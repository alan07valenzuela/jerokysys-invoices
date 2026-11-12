package com.jerokysys.invoices.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.jerokysys.commons.common.PagedFilter;
import com.jerokysys.commons.common.PageResult;
import com.jerokysys.commons.domain.entity.InvoiceDetail;
import com.jerokysys.commons.exception.BusinessException;
import com.jerokysys.commons.service.AbstractBaseService;
import com.jerokysys.dto.InvoiceDetailDTO;
import com.jerokysys.invoices.config.DomainCache;
import com.jerokysys.invoices.config.RedisCacheConfig;
import com.jerokysys.invoices.mappers.InvoiceDetailMapper;
import com.jerokysys.invoices.repo.InvoiceDetailRepo;

import lombok.Getter;

@Service
public class InvoiceDetailService extends AbstractBaseService<InvoiceDetail, InvoiceDetailDTO, PagedFilter> {

    private static final Set<String> CONCEPT_TYPES = Set.of("MATRICULA", "EXAMEN");

    @Getter
    private final InvoiceDetailRepo invoiceDetailRepo;
    private final InvoiceDetailMapper mapper;
    private final DomainCache domainCache;

    public InvoiceDetailService(InvoiceDetailRepo invoiceDetailRepo,
            InvoiceDetailMapper invoiceDetailMapper, DomainCache domainCache) {
        super(invoiceDetailRepo);
        this.invoiceDetailRepo = invoiceDetailRepo;
        this.mapper = invoiceDetailMapper;
        this.domainCache = domainCache;
    }

    @Override
    public InvoiceDetailDTO create(InvoiceDetailDTO dto) {
        throw new BusinessException(
                "DETAIL_NOT_WRITABLE",
                "Invoice details are written only through their invoice");
    }

    @Override
    public InvoiceDetailDTO update(Long id, InvoiceDetailDTO dto) {
        throw new BusinessException(
                "DETAIL_NOT_WRITABLE",
                "Invoice details are written only through their invoice");
    }

    @Override
    @Cacheable(value = RedisCacheConfig.CACHE_INVOICE_DETAIL, key = "#id")
    public InvoiceDetailDTO getById(Long id) {
        return super.getById(id);
    }

    @Override
    public List<InvoiceDetailDTO> getAll() {
        List<InvoiceDetailDTO> list = super.getAll();
        domainCache.putAll(RedisCacheConfig.CACHE_INVOICE_DETAIL, list, InvoiceDetailDTO::getId);
        return list;
    }

    @Override
    @CacheEvict(value = RedisCacheConfig.CACHE_INVOICE_DETAIL, allEntries = true)
    public void delete(Long id) {
        super.delete(id);
    }

    public List<InvoiceDetailDTO> getByInvoice(Long invoiceId) {
        return mapper.toDtoList(invoiceDetailRepo.findByInvoiceId(invoiceId));
    }

    public List<InvoiceDetailDTO> getByCorrespondingMonth(String correspondingMonth) {
        return mapper.toDtoList(invoiceDetailRepo.findByCorrespondingMonth(correspondingMonth));
    }

    public List<InvoiceDetailDTO> getByVatRate(Integer vatRate) {
        return mapper.toDtoList(invoiceDetailRepo.findByVatRate(vatRate));
    }

    public List<InvoiceDetailDTO> searchByDescription(String description) {
        return mapper.toDtoList(invoiceDetailRepo.findByDescriptionContaining(description));
    }

    public List<InvoiceDetailDTO> getByAmountRange(BigDecimal min, BigDecimal max) {
        return mapper.toDtoList(invoiceDetailRepo.findByAmountBetween(min, max));
    }

    /**
     * El concepto facturado viaja dentro de description con un token entre corchetes
     * al final, por ejemplo `[MATRICULA#12]`, porque el esquema compartido no tiene
     * columnas enrollment_id ni exam_id. Se busca por ese substring.
     */
    public List<InvoiceDetailDTO> getByConcept(String conceptType, Long conceptId) {
        return mapper.toDtoList(invoiceDetailRepo.findByDescriptionContaining(buildConceptToken(conceptType, conceptId)));
    }

    @Override
    public PageResult<InvoiceDetailDTO> search(PagedFilter filter) {
        Pageable pageable = buildPageable(filter);
        Page<InvoiceDetail> page = invoiceDetailRepo.findAll(pageable);
        return toPageResult(toPageDto(page));
    }

    @Override
    public InvoiceDetailDTO toDto(InvoiceDetail entity) {
        return mapper.toDto(entity);
    }

    @Override
    public InvoiceDetail toEntity(InvoiceDetailDTO dto) {
        return mapper.toEntity(dto);
    }

    private String buildConceptToken(String conceptType, Long conceptId) {
        if (conceptType == null || conceptId == null) {
            throw new BusinessException("VALIDATION_ERROR",
                    "Both conceptType and conceptId are required");
        }
        String normalized = conceptType.trim().toUpperCase(Locale.ROOT);
        if (!CONCEPT_TYPES.contains(normalized)) {
            throw new BusinessException("VALIDATION_ERROR", "Invalid conceptType: " + conceptType);
        }
        return "[" + normalized + "#" + conceptId + "]";
    }
}