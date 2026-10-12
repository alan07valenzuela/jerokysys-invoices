package com.jerokysys.invoices.mappers;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.jerokysys.commons.domain.entity.InvoiceDetail;
import com.jerokysys.dto.InvoiceDetailDTO;

@Mapper(componentModel = "spring")
public interface InvoiceDetailMapper {

    @Mapping(target = "invoiceId", source = "invoice.id")
    InvoiceDetailDTO toDto(InvoiceDetail entity);

    List<InvoiceDetailDTO> toDtoList(List<InvoiceDetail> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    InvoiceDetail toEntity(InvoiceDetailDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntity(InvoiceDetailDTO dto, @MappingTarget InvoiceDetail entity);
}