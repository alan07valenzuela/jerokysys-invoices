package com.jerokysys.invoices.mappers;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.jerokysys.commons.domain.entity.TaxStamp;
import com.jerokysys.dto.TaxStampDTO;

@Mapper(componentModel = "spring")
public interface TaxStampMapper {

    TaxStampDTO toDto(TaxStamp entity);

    List<TaxStampDTO> toDtoList(List<TaxStamp> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nextSequence", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    TaxStamp toEntity(TaxStampDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nextSequence", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntity(TaxStampDTO dto, @MappingTarget TaxStamp entity);
}
