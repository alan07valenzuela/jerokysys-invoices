package com.jerokysys.invoices.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jerokysys.commons.common.PageResult;
import com.jerokysys.commons.common.PagedFilter;
import com.jerokysys.commons.domain.entity.Invoice;
import com.jerokysys.commons.domain.entity.InvoiceDetail;
import com.jerokysys.commons.domain.entity.Student;
import com.jerokysys.commons.domain.entity.TaxStamp;
import com.jerokysys.commons.domain.enums.InvoiceStatus;
import com.jerokysys.commons.domain.enums.SaleCondition;
import com.jerokysys.commons.exception.BusinessException;
import com.jerokysys.commons.service.AbstractBaseService;
import com.jerokysys.dto.InvoiceDTO;
import com.jerokysys.dto.InvoiceDetailDTO;
import com.jerokysys.invoices.config.DomainCache;
import com.jerokysys.invoices.config.RedisCacheConfig;
import com.jerokysys.invoices.mappers.InvoiceDetailMapper;
import com.jerokysys.invoices.mappers.InvoiceMapper;
import com.jerokysys.invoices.repo.InvoiceRepo;
import com.jerokysys.invoices.repo.TaxStampRepo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.Getter;

@Service
@Transactional(readOnly = true)
public class InvoiceService extends AbstractBaseService<Invoice, InvoiceDTO, PagedFilter> {

    private static final int MAX_SEQUENCE = 9_999_999;
    private static final ZoneId PARAGUAY_ZONE = ZoneId.of("America/Asuncion");

    @Getter
    private final InvoiceRepo invoiceRepo;
    private final TaxStampRepo taxStampRepo;
    private final InvoiceMapper mapper;
    private final InvoiceDetailMapper detailMapper;
    private final EntityManager entityManager;
    private final DomainCache domainCache;

    public InvoiceService(InvoiceRepo invoiceRepo, TaxStampRepo taxStampRepo,
            InvoiceMapper invoiceMapper, InvoiceDetailMapper invoiceDetailMapper,
            EntityManager entityManager, DomainCache domainCache) {
        super(invoiceRepo);
        this.invoiceRepo = invoiceRepo;
        this.taxStampRepo = taxStampRepo;
        this.mapper = invoiceMapper;
        this.detailMapper = invoiceDetailMapper;
        this.entityManager = entityManager;
        this.domainCache = domainCache;
    }

    @Override
    @Transactional
    @CacheEvict(value = {RedisCacheConfig.CACHE_INVOICE,
            RedisCacheConfig.CACHE_INVOICE_DETAIL}, allEntries = true)
    public InvoiceDTO create(InvoiceDTO dto) {
        validateCreate(dto);
        TaxStamp stamp = lockStamp(dto.getTaxStampId());
        validateStamp(stamp, dto.getIssueDate(), dto.getIssuerRuc());

        int sequence = nextSequence(stamp);
        Invoice entity = toEntity(dto);
        if (dto.getStudentId() != null) {
            Student student = entityManager.find(Student.class, dto.getStudentId());
            if (student == null || Boolean.TRUE.equals(student.getDeleted())) {
                throw new BusinessException("STUDENT_NOT_FOUND",
                        "Student not found with id: " + dto.getStudentId());
            }
            entity.setStudent(student);
        }
        entity.setTaxStamp(stamp);
        entity.setEstablishmentCode(stamp.getEstablishmentCode());
        entity.setExpeditionPoint(stamp.getExpeditionPoint());
        entity.setSequenceNumber(sequence);
        entity.setInvoiceNumber(formatInvoiceNumber(stamp, sequence));
        entity.setStampNumber(stamp.getStampNumber());
        entity.setStampValidFrom(stamp.getValidFrom());
        entity.setStampValidUntil(stamp.getValidUntil());
        entity.setPaymentStatus(InvoiceStatus.PENDING);
        if (entity.getSaleCondition() == null) {
            entity.setSaleCondition(SaleCondition.CONTADO);
        }

        replaceDetails(entity, buildDetails(dto.getDetails()));
        recalculateTotals(entity);

        stamp.setNextSequence(sequence + 1);
        taxStampRepo.save(stamp);

        return toDto(invoiceRepo.save(entity));
    }

    @Override
    @Transactional
    @CacheEvict(value = {RedisCacheConfig.CACHE_INVOICE,
            RedisCacheConfig.CACHE_INVOICE_DETAIL}, allEntries = true)
    public InvoiceDTO update(Long id, InvoiceDTO dto) {
        Invoice entity = invoiceRepo.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "ENTITY_NOT_FOUND",
                        "Invoice not found with id: " + id));

        if (entity.getPaymentStatus() != InvoiceStatus.PENDING) {
            throw new BusinessException("INVOICE_NOT_EDITABLE",
                    "Only PENDING invoices can be edited, this one is "
                            + entity.getPaymentStatus().name());
        }

        validateCustomer(dto);
        if (dto.getDueDate() != null && entity.getIssueDate() != null
                && dto.getDueDate().isBefore(entity.getIssueDate())) {
            throw new BusinessException("VALIDATION_ERROR",
                    "dueDate cannot be before issueDate");
        }

        mapper.updateEntity(dto, entity);

        if (dto.getDetails() != null) {
            replaceDetails(entity, buildDetails(dto.getDetails()));
        }
        recalculateTotals(entity);

        return toDto(invoiceRepo.save(entity));
    }

    @Override
    @Transactional
    @CacheEvict(value = {RedisCacheConfig.CACHE_INVOICE,
            RedisCacheConfig.CACHE_INVOICE_DETAIL}, allEntries = true)
    public void delete(Long id) {
        Invoice entity = invoiceRepo.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "ENTITY_NOT_FOUND",
                        "Invoice not found with id: " + id));

        if (entity.getPaymentStatus() != InvoiceStatus.PENDING) {
            throw new BusinessException("INVOICE_NOT_EDITABLE",
                    "Only PENDING invoices can be deleted, this one is "
                            + entity.getPaymentStatus().name());
        }

        entity.setDeleted(true);
        entity.setActive(false);
        entity.getDetails().forEach(detail -> {
            detail.setDeleted(true);
            detail.setActive(false);
        });
        invoiceRepo.save(entity);
    }

    @Transactional
    @CacheEvict(value = RedisCacheConfig.CACHE_INVOICE, allEntries = true)
    public InvoiceDTO changeStatus(Long id, String status) {
        Invoice entity = invoiceRepo.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "ENTITY_NOT_FOUND",
                        "Invoice not found with id: " + id));

        InvoiceStatus newStatus = parseTargetStatus(status);
        if (entity.getPaymentStatus() != InvoiceStatus.PENDING) {
            throw new BusinessException("CONFLICT",
                    "Only PENDING invoices change status, this one is "
                            + entity.getPaymentStatus().name());
        }

        entity.setPaymentStatus(newStatus);
        return toDto(invoiceRepo.save(entity));
    }

    public List<InvoiceDTO> getByStatus(String status) {
        return mapper.toDtoList(invoiceRepo.findByPaymentStatus(parseFilterStatus(status)));
    }

    public List<InvoiceDTO> getByStudent(Long studentId) {
        return mapper.toDtoList(invoiceRepo.findByStudentId(studentId));
    }

    public List<InvoiceDTO> getBySaleCondition(String saleCondition) {
        return mapper.toDtoList(invoiceRepo.findBySaleCondition(parseSaleCondition(saleCondition)));
    }

    public List<InvoiceDTO> getByInvoiceNumber(String invoiceNumber) {
        return mapper.toDtoList(invoiceRepo.findByInvoiceNumber(invoiceNumber));
    }

    public List<InvoiceDTO> getByStampNumber(String stampNumber) {
        return mapper.toDtoList(invoiceRepo.findByStampNumber(stampNumber));
    }

    public List<InvoiceDTO> getByCustomerRuc(String customerRuc) {
        return mapper.toDtoList(invoiceRepo.findByCustomerRuc(customerRuc));
    }

    public List<InvoiceDTO> getByIssueDate(LocalDate date) {
        return mapper.toDtoList(invoiceRepo.findByIssueDate(date));
    }

    public List<InvoiceDTO> getByIssueDateRange(LocalDate from, LocalDate to) {
        return mapper.toDtoList(invoiceRepo.findByIssueDateBetween(from, to));
    }

    public List<InvoiceDTO> getByAmountRange(BigDecimal min, BigDecimal max) {
        return mapper.toDtoList(invoiceRepo.findByTotalAmountBetween(min, max));
    }

    public List<InvoiceDTO> searchByCustomerName(String customerName) {
        return mapper.toDtoList(invoiceRepo.findByCustomerNameContainingIgnoreCase(customerName));
    }

    @Override
    public PageResult<InvoiceDTO> search(PagedFilter filter) {
        Pageable pageable = buildPageable(filter);
        Page<Invoice> page = invoiceRepo.findAll(pageable);
        return toPageResult(toPageDto(page));
    }

    @Override
    public List<InvoiceDTO> getAll() {
        List<InvoiceDTO> list = super.getAll();
        domainCache.putAll(RedisCacheConfig.CACHE_INVOICE, list, InvoiceDTO::getId);
        return list;
    }

    @Override
    @Cacheable(value = RedisCacheConfig.CACHE_INVOICE, key = "#id")
    public InvoiceDTO getById(Long id) {
        return super.getById(id);
    }

    @Override
    public PageResult<InvoiceDTO> getByFilter(PagedFilter filter) {
        return super.getByFilter(filter);
    }

    @Override
    public InvoiceDTO toDto(Invoice entity) {
        return mapper.toDto(entity);
    }

    @Override
    public Invoice toEntity(InvoiceDTO dto) {
        return mapper.toEntity(dto);
    }

    private TaxStamp lockStamp(Long taxStampId) {
        TaxStamp stamp = entityManager.find(TaxStamp.class, taxStampId, LockModeType.PESSIMISTIC_WRITE);
        if (stamp == null || Boolean.TRUE.equals(stamp.getDeleted())
                || Boolean.FALSE.equals(stamp.getActive())) {
            throw new BusinessException("STAMP_NOT_FOUND",
                    "Tax stamp not found with id: " + taxStampId);
        }
        return stamp;
    }

    private void validateStamp(TaxStamp stamp, LocalDate issueDate, String issuerRuc) {
        if (issueDate.isBefore(stamp.getValidFrom()) || issueDate.isAfter(stamp.getValidUntil())) {
            throw new BusinessException("STAMP_EXPIRED",
                    "Tax stamp " + stamp.getStampNumber() + " is not valid on " + issueDate);
        }
        if (!normalizeRuc(stamp.getBusinessRuc()).equals(normalizeRuc(issuerRuc))) {
            throw new BusinessException("STAMP_RUC_MISMATCH",
                    "issuerRuc does not match the RUC of the selected tax stamp");
        }
    }

    private int nextSequence(TaxStamp stamp) {
        int sequence = stamp.getNextSequence() == null ? 1 : stamp.getNextSequence();
        if (sequence < 1 || sequence > MAX_SEQUENCE) {
            throw new BusinessException("SEQUENCE_EXHAUSTED",
                    "Tax stamp " + stamp.getStampNumber() + " has no sequence numbers left");
        }
        return sequence;
    }

    private String formatInvoiceNumber(TaxStamp stamp, int sequence) {
        return String.format("%s-%s-%07d",
                stamp.getEstablishmentCode(), stamp.getExpeditionPoint(), sequence);
    }

    private void validateCreate(InvoiceDTO dto) {
        if (dto.getTaxStampId() == null) {
            throw new BusinessException("VALIDATION_ERROR", "taxStampId is required");
        }
        if (dto.getIssueDate() == null) {
            throw new BusinessException("VALIDATION_ERROR", "issueDate is required");
        }
        if (dto.getIssueDate().isAfter(LocalDate.now(PARAGUAY_ZONE))) {
            throw new BusinessException("VALIDATION_ERROR", "issueDate cannot be in the future");
        }
        validateCustomer(dto);
        requireText(dto.getIssuerName(), "issuerName");
        requireText(dto.getIssuerRuc(), "issuerRuc");
        requireText(dto.getIssuerActivity(), "issuerActivity");
        requireText(dto.getIssuerAddress(), "issuerAddress");

        if (dto.getDueDate() != null && dto.getDueDate().isBefore(dto.getIssueDate())) {
            throw new BusinessException("VALIDATION_ERROR",
                    "dueDate cannot be before issueDate");
        }
        if (dto.getDetails() == null || dto.getDetails().isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR",
                    "An invoice must have at least one detail line");
        }
    }

    private void validateCustomer(InvoiceDTO dto) {
        requireText(dto.getCustomerName(), "customerName");
        requireText(dto.getCustomerRuc(), "customerRuc");
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", field + " is required");
        }
    }

    private List<InvoiceDetail> buildDetails(List<InvoiceDetailDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR",
                    "An invoice must have at least one detail line");
        }

        return dtos.stream().map(dto -> {
            if (dto.getDescription() == null || dto.getDescription().isBlank()) {
                throw new BusinessException("VALIDATION_ERROR",
                        "Every detail line needs a description");
            }
            if (dto.getQuantity() == null || dto.getQuantity().signum() <= 0) {
                throw new BusinessException("VALIDATION_ERROR",
                        "Every detail line needs a quantity greater than zero");
            }
            if (dto.getUnitPrice() == null || dto.getUnitPrice().signum() < 0) {
                throw new BusinessException("VALIDATION_ERROR",
                        "Every detail line needs a unit price greater than or equal to zero");
            }
            if (dto.getVatType() == null) {
                throw new BusinessException("VALIDATION_ERROR",
                        "Every detail line needs a vatType");
            }

            InvoiceDetail detail = detailMapper.toEntity(dto);
            detail.setSubtotal(round(dto.getQuantity().multiply(dto.getUnitPrice())));
            return detail;
        }).toList();
    }

    private void replaceDetails(Invoice entity, List<InvoiceDetail> details) {
        details.forEach(detail -> detail.setInvoice(entity));
        entity.getDetails().clear();
        entity.getDetails().addAll(details);
    }

    private void recalculateTotals(Invoice entity) {
        InvoiceTotalsCalculator.Totals totals = InvoiceTotalsCalculator.calculate(entity.getDetails());
        entity.setExemptAmount(totals.exempt());
        entity.setSales5Amount(totals.sales5());
        entity.setSales10Amount(totals.sales10());
        entity.setIva5Amount(totals.iva5());
        entity.setIva10Amount(totals.iva10());
        entity.setTotalAmount(totals.total());
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeRuc(String ruc) {
        return ruc == null ? "" : ruc.replaceAll("\\D", "");
    }

    /** Estados válidos para consultar/filtrar: incluye PENDING. */
    private InvoiceStatus parseFilterStatus(String value) {
        try {
            return InvoiceStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException("VALIDATION_ERROR", "Invalid status: " + value);
        }
    }

    /** Estados alcanzables por una transición: solo PAID y CANCELLED. */
    private InvoiceStatus parseTargetStatus(String value) {
        InvoiceStatus status = parseFilterStatus(value);
        if (status == InvoiceStatus.PENDING) {
            throw new BusinessException("VALIDATION_ERROR",
                    "PENDING is not a reachable target, only PAID or CANCELLED are");
        }
        return status;
    }

    private SaleCondition parseSaleCondition(String value) {
        try {
            return SaleCondition.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException("VALIDATION_ERROR", "Invalid saleCondition: " + value);
        }
    }
}
