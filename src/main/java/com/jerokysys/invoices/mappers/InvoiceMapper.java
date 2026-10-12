package com.jerokysys.invoices.mappers;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.jerokysys.commons.domain.entity.Invoice;
import com.jerokysys.dto.InvoiceDTO;

@Mapper(componentModel = "spring", uses = InvoiceDetailMapper.class)
public interface InvoiceMapper {

    @Mapping(target = "studentId", source = "student.id")
    InvoiceDTO toDto(Invoice entity);

    List<InvoiceDTO> toDtoList(List<Invoice> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "details", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Invoice toEntity(InvoiceDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "details", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "totalIva", ignore = true)
    @Mapping(target = "paymentStatus", ignore = true)
    void updateEntity(InvoiceDTO dto, @MappingTarget Invoice entity);
}