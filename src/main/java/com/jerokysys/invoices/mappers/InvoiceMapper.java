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
    @Mapping(target = "taxStampId", source = "taxStamp.id")
    InvoiceDTO toDto(Invoice entity);

    List<InvoiceDTO> toDtoList(List<Invoice> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "taxStamp", ignore = true)
    @Mapping(target = "details", ignore = true)
    @Mapping(target = "establishmentCode", ignore = true)
    @Mapping(target = "expeditionPoint", ignore = true)
    @Mapping(target = "sequenceNumber", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "stampNumber", ignore = true)
    @Mapping(target = "stampValidFrom", ignore = true)
    @Mapping(target = "stampValidUntil", ignore = true)
    @Mapping(target = "exemptAmount", ignore = true)
    @Mapping(target = "sales5Amount", ignore = true)
    @Mapping(target = "sales10Amount", ignore = true)
    @Mapping(target = "iva5Amount", ignore = true)
    @Mapping(target = "iva10Amount", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "paymentStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Invoice toEntity(InvoiceDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "taxStamp", ignore = true)
    @Mapping(target = "details", ignore = true)
    @Mapping(target = "establishmentCode", ignore = true)
    @Mapping(target = "expeditionPoint", ignore = true)
    @Mapping(target = "sequenceNumber", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "issueDate", ignore = true)
    @Mapping(target = "customerAddress", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "dueDate", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "issuerName", ignore = true)
    @Mapping(target = "issuerRuc", ignore = true)
    @Mapping(target = "issuerActivity", ignore = true)
    @Mapping(target = "issuerPhone", ignore = true)
    @Mapping(target = "issuerAddress", ignore = true)
    @Mapping(target = "issuerEmail", ignore = true)
    @Mapping(target = "stampNumber", ignore = true)
    @Mapping(target = "stampValidFrom", ignore = true)
    @Mapping(target = "stampValidUntil", ignore = true)
    @Mapping(target = "exemptAmount", ignore = true)
    @Mapping(target = "sales5Amount", ignore = true)
    @Mapping(target = "sales10Amount", ignore = true)
    @Mapping(target = "iva5Amount", ignore = true)
    @Mapping(target = "iva10Amount", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "paymentStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntity(InvoiceDTO dto, @MappingTarget Invoice entity);
}
