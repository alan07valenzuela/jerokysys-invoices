package com.jerokysys.invoices.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jerokysys.commons.common.PageResult;
import com.jerokysys.commons.common.PagedFilter;
import com.jerokysys.commons.domain.entity.Invoice;
import com.jerokysys.commons.domain.entity.InvoiceDetail;
import com.jerokysys.commons.domain.entity.Student;
import com.jerokysys.commons.domain.enums.InvoiceStatus;
import com.jerokysys.commons.domain.enums.SaleCondition;
import com.jerokysys.commons.exception.BusinessException;
import com.jerokysys.commons.service.AbstractBaseService;
import com.jerokysys.dto.InvoiceDTO;
import com.jerokysys.dto.InvoiceDetailDTO;
import com.jerokysys.invoices.mappers.InvoiceDetailMapper;
import com.jerokysys.invoices.mappers.InvoiceMapper;
import com.jerokysys.invoices.repo.InvoiceRepo;

import jakarta.persistence.EntityManager;
import lombok.Getter;

@Service
@Transactional(readOnly = true)
public class InvoiceService extends AbstractBaseService<Invoice, InvoiceDTO, PagedFilter> {

    private static final Pattern INVOICE_NUMBER = Pattern.compile("^\\d{3}-\\d{3}-\\d{7}$");
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    @Getter
    private final InvoiceRepo invoiceRepo;
    private final InvoiceMapper mapper;
    private final InvoiceDetailMapper detailMapper;
    private final EntityManager entityManager;

    public InvoiceService(InvoiceRepo invoiceRepo, InvoiceMapper invoiceMapper,
            InvoiceDetailMapper invoiceDetailMapper, EntityManager entityManager) {
        super(invoiceRepo);
        this.invoiceRepo = invoiceRepo;
        this.mapper = invoiceMapper;
        this.detailMapper = invoiceDetailMapper;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public InvoiceDTO create(InvoiceDTO dto) {
        assertInvoiceNumberAvailable(dto.getInvoiceNumber());
        Student student = loadStudent(dto.getStudentId());

        Invoice entity = toEntity(dto);
        entity.setStudent(student);
        replaceDetails(entity, buildDetails(dto.getDetails()));
        recalculateTotals(entity);

        try {
            return toDto(invoiceRepo.save(entity));
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("STUDENT_NOT_FOUND",
                    "Student not found with id: " + dto.getStudentId());
        }
    }

    @Override
    @Transactional
    public InvoiceDTO update(Long id, InvoiceDTO dto) {
        Invoice entity = invoiceRepo.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "ENTITY_NOT_FOUND",
                        "Invoice not found with id: " + id));

        if (dto.getInvoiceNumber() != null && !dto.getInvoiceNumber().equals(entity.getInvoiceNumber())) {
            assertInvoiceNumberAvailable(dto.getInvoiceNumber());
        }
        if (dto.getStudentId() != null) {
            entity.setStudent(loadStudent(dto.getStudentId()));
        }

        mapper.updateEntity(dto, entity);

        if (dto.getDetails() != null) {
            replaceDetails(entity, buildDetails(dto.getDetails()));
        }
        recalculateTotals(entity);

        return toDto(invoiceRepo.save(entity));
    }

    @Transactional
    public InvoiceDTO changeStatus(Long id, String status) {
        Invoice entity = invoiceRepo.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "ENTITY_NOT_FOUND",
                        "Invoice not found with id: " + id));

        InvoiceStatus newStatus = parseStatus(status);
        if (entity.getPaymentStatus() != InvoiceStatus.PENDING) {
            throw new BusinessException("CONFLICT",
                    "Only PENDING invoices change status, this one is "
                            + entity.getPaymentStatus().name());
        }

        entity.setPaymentStatus(newStatus);
        return toDto(invoiceRepo.save(entity));
    }

    public List<InvoiceDTO> getByStudent(Long studentId) {
        return mapper.toDtoList(invoiceRepo.findByStudentId(studentId));
    }

    public List<InvoiceDTO> getByStatus(String status) {
        return mapper.toDtoList(invoiceRepo.findByPaymentStatus(parseStatus(status)));
    }

    public List<InvoiceDTO> getBySaleCondition(String saleCondition) {
        return mapper.toDtoList(invoiceRepo.findBySaleCondition(parseSaleCondition(saleCondition)));
    }

    public List<InvoiceDTO> getByInvoiceNumber(String invoiceNumber) {
        return mapper.toDtoList(invoiceRepo.findByInvoiceNumber(invoiceNumber));
    }

    public List<InvoiceDTO> getByTimbrado(String timbrado) {
        return mapper.toDtoList(invoiceRepo.findByTimbrado(timbrado));
    }

    public List<InvoiceDTO> getByTaxId(String taxId) {
        return mapper.toDtoList(invoiceRepo.findByTaxId(taxId));
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

    public List<InvoiceDTO> searchByBusinessName(String businessName) {
        return mapper.toDtoList(invoiceRepo.findByBusinessNameContainingIgnoreCase(businessName));
    }

    @Override
    public PageResult<InvoiceDTO> search(PagedFilter filter) {
        Pageable pageable = buildPageable(filter);
        Page<Invoice> page = invoiceRepo.findAll(pageable);
        return toPageResult(toPageDto(page));
    }

    @Override
    public List<InvoiceDTO> getAll() {
        return super.getAll();
    }

    @Override
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

    private void assertInvoiceNumberAvailable(String invoiceNumber) {
        if (invoiceNumber == null || !INVOICE_NUMBER.matcher(invoiceNumber).matches()) {
            throw new BusinessException("VALIDATION_ERROR",
                    "invoiceNumber must match the format 001-001-0000123");
        }
        if (invoiceRepo.existsByInvoiceNumber(invoiceNumber)) {
            throw new BusinessException("CONFLICT", "invoiceNumber already used: " + invoiceNumber);
        }
    }

    private Student loadStudent(Long studentId) {
        if (studentId == null) {
            throw new BusinessException("VALIDATION_ERROR", "studentId is required");
        }
        return entityManager.getReference(Student.class, studentId);
    }

    private List<InvoiceDetail> buildDetails(List<InvoiceDetailDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR",
                    "An invoice must have at least one detail line");
        }
        return dtos.stream().map(detailMapper::toEntity).toList();
    }

    private void replaceDetails(Invoice entity, List<InvoiceDetail> details) {
        details.forEach(detail -> detail.setInvoice(entity));
        entity.getDetails().clear();
        entity.getDetails().addAll(details);
    }

    private void recalculateTotals(Invoice entity) {
        BigDecimal base = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;

        for (InvoiceDetail detail : entity.getDetails()) {
            BigDecimal amount = detail.getAmount() == null ? BigDecimal.ZERO : detail.getAmount();
            int rate = detail.getVatRate() == null ? 0 : detail.getVatRate();
            base = base.add(amount);
            iva = iva.add(amount.multiply(BigDecimal.valueOf(rate))
                    .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP));
        }

        entity.setTotalAmount(base);
        entity.setTotalIva(iva);
    }

    private InvoiceStatus parseStatus(String value) {
        final InvoiceStatus status;
        try {
            status = InvoiceStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException("VALIDATION_ERROR", "Invalid status: " + value);
        }
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