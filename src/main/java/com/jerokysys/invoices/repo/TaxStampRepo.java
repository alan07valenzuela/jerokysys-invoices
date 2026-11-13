package com.jerokysys.invoices.repo;

import java.time.LocalDate;
import java.util.List;

import com.jerokysys.commons.domain.entity.TaxStamp;
import com.jerokysys.commons.repository.BaseRepository;

public interface TaxStampRepo extends BaseRepository<TaxStamp, Long> {

    List<TaxStamp> findByValidFromLessThanEqualAndValidUntilGreaterThanEqual(LocalDate from, LocalDate until);

    List<TaxStamp> findByActiveTrueAndValidFromLessThanEqualAndValidUntilGreaterThanEqual(
            LocalDate from, LocalDate until);

    List<TaxStamp> findByBusinessRucAndEstablishmentCodeAndExpeditionPoint(
            String businessRuc, String establishmentCode, String expeditionPoint);
}
