package com.jerokysys.invoices.services;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jerokysys.commons.common.PageResult;
import com.jerokysys.commons.common.PagedFilter;
import com.jerokysys.commons.domain.entity.TaxStamp;
import com.jerokysys.commons.exception.BusinessException;
import com.jerokysys.commons.service.AbstractBaseService;
import com.jerokysys.dto.TaxStampDTO;
import com.jerokysys.invoices.config.DomainCache;
import com.jerokysys.invoices.config.RedisCacheConfig;
import com.jerokysys.invoices.mappers.TaxStampMapper;
import com.jerokysys.invoices.repo.TaxStampRepo;

import lombok.Getter;

@Service
@Transactional(readOnly = true)
public class TaxStampService extends AbstractBaseService<TaxStamp, TaxStampDTO, PagedFilter> {

    private static final Pattern THREE_DIGITS = Pattern.compile("^\\d{3}$");
    private static final ZoneId PARAGUAY_ZONE = ZoneId.of("America/Asuncion");

    @Getter
    private final TaxStampRepo taxStampRepo;
    private final TaxStampMapper mapper;
    private final DomainCache domainCache;

    public TaxStampService(TaxStampRepo taxStampRepo, TaxStampMapper mapper, DomainCache domainCache) {
        super(taxStampRepo);
        this.taxStampRepo = taxStampRepo;
        this.mapper = mapper;
        this.domainCache = domainCache;
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisCacheConfig.CACHE_TAX_STAMP, allEntries = true)
    public TaxStampDTO create(TaxStampDTO dto) {
        validate(dto, null);
        TaxStamp entity = mapper.toEntity(dto);
        entity.setBusinessRuc(dto.getBusinessRuc().trim());
        entity.setStampNumber(dto.getStampNumber().trim());
        entity.setEstablishmentCode(dto.getEstablishmentCode().trim());
        entity.setExpeditionPoint(dto.getExpeditionPoint().trim());
        entity.setNextSequence(1);
        entity.setActive(true);
        return mapper.toDto(taxStampRepo.save(entity));
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisCacheConfig.CACHE_TAX_STAMP, allEntries = true)
    public TaxStampDTO update(Long id, TaxStampDTO dto) {
        TaxStamp entity = taxStampRepo.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "ENTITY_NOT_FOUND",
                        "Tax stamp not found with id: " + id));

        validate(dto, id);
        mapper.updateEntity(dto, entity);
        entity.setBusinessRuc(dto.getBusinessRuc().trim());
        entity.setStampNumber(dto.getStampNumber().trim());
        entity.setEstablishmentCode(dto.getEstablishmentCode().trim());
        entity.setExpeditionPoint(dto.getExpeditionPoint().trim());
        return mapper.toDto(taxStampRepo.save(entity));
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisCacheConfig.CACHE_TAX_STAMP, allEntries = true)
    public void delete(Long id) {
        super.delete(id);
    }

    public List<TaxStampDTO> getValid(LocalDate date) {
        LocalDate target = date == null ? LocalDate.now(PARAGUAY_ZONE) : date;
        return mapper.toDtoList(taxStampRepo
                .findByActiveTrueAndValidFromLessThanEqualAndValidUntilGreaterThanEqual(target, target));
    }

    @Override
    public PageResult<TaxStampDTO> search(PagedFilter filter) {
        Pageable pageable = buildPageable(filter);
        Page<TaxStamp> page = taxStampRepo.findAll(pageable);
        return toPageResult(toPageDto(page));
    }

    @Override
    public PageResult<TaxStampDTO> getByFilter(PagedFilter filter) {
        return super.getByFilter(filter);
    }

    @Override
    public List<TaxStampDTO> getAll() {
        List<TaxStampDTO> list = super.getAll();
        domainCache.putAll(RedisCacheConfig.CACHE_TAX_STAMP, list, TaxStampDTO::getId);
        return list;
    }

    @Override
    @Cacheable(value = RedisCacheConfig.CACHE_TAX_STAMP, key = "#id")
    public TaxStampDTO getById(Long id) {
        return super.getById(id);
    }

    @Override
    public TaxStampDTO toDto(TaxStamp entity) {
        return mapper.toDto(entity);
    }

    @Override
    public TaxStamp toEntity(TaxStampDTO dto) {
        return mapper.toEntity(dto);
    }

    private void validate(TaxStampDTO dto, Long currentId) {
        requireText(dto.getBusinessRuc(), "businessRuc");
        requireText(dto.getStampNumber(), "stampNumber");
        requireText(dto.getEstablishmentCode(), "establishmentCode");
        requireText(dto.getExpeditionPoint(), "expeditionPoint");

        if (!THREE_DIGITS.matcher(dto.getEstablishmentCode().trim()).matches()) {
            throw new BusinessException("VALIDATION_ERROR",
                    "establishmentCode must have exactly three digits");
        }
        if (!THREE_DIGITS.matcher(dto.getExpeditionPoint().trim()).matches()) {
            throw new BusinessException("VALIDATION_ERROR",
                    "expeditionPoint must have exactly three digits");
        }
        if (dto.getValidFrom() == null || dto.getValidUntil() == null) {
            throw new BusinessException("VALIDATION_ERROR",
                    "validFrom and validUntil are required");
        }
        if (dto.getValidUntil().isBefore(dto.getValidFrom())) {
            throw new BusinessException("TAX_STAMP_INVALID_RANGE",
                    "validUntil cannot be before validFrom");
        }

        List<TaxStamp> existing = taxStampRepo
                .findByBusinessRucAndEstablishmentCodeAndExpeditionPoint(
                        dto.getBusinessRuc().trim(),
                        dto.getEstablishmentCode().trim(),
                        dto.getExpeditionPoint().trim());

        boolean overlaps = existing.stream()
                .filter(stamp -> currentId == null || !currentId.equals(stamp.getId()))
                .anyMatch(stamp -> !dto.getValidUntil().isBefore(stamp.getValidFrom())
                        && !dto.getValidFrom().isAfter(stamp.getValidUntil()));

        if (overlaps) {
            throw new BusinessException("TAX_STAMP_OVERLAP",
                    "Another tax stamp already covers that establishment, expedition point and date range");
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", field + " is required");
        }
    }
}
