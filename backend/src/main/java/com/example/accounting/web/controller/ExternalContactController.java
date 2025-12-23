package com.example.accounting.web.controller;

import com.example.accounting.domain.ExternalContact;
import com.example.accounting.repository.ExternalContactRepository;
import com.example.accounting.web.dto.ExternalContactDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/external-contacts")
@CrossOrigin
public class ExternalContactController {

    private final ExternalContactRepository repository;

    public ExternalContactController(ExternalContactRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ExternalContactDto> list(@RequestParam(required = false) String type,
                                         @RequestParam(required = false) String status) {
        String statusFilter = status != null ? status : null;
        return repository.findAll().stream()
                .filter(c -> type == null || type.equals(c.getType()))
                .filter(c -> statusFilter == null || statusFilter.equals(c.getStatus()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<ExternalContactDto> create(@Valid @RequestBody ExternalContactDto dto) {
        if (dto.getTaxId() != null) {
            repository.findByTaxIdAndStatus(dto.getTaxId(), "活跃").ifPresent(existing -> {
                throw new IllegalArgumentException("该纳税人识别号已存在于系统，对应联系人：" + existing.getName());
            });
        }
        ExternalContact contact = new ExternalContact();
        applyDto(dto, contact);
        if (contact.getStatus() == null) {
            contact.setStatus("活跃");
        }
        ExternalContact saved = repository.save(contact);
        return ResponseEntity.ok(toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExternalContactDto> update(@PathVariable Long id,
                                                     @Valid @RequestBody ExternalContactDto dto) {
        Optional<ExternalContact> optional = repository.findById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ExternalContact contact = optional.get();
        if (dto.getTaxId() != null && !dto.getTaxId().equals(contact.getTaxId())) {
            repository.findByTaxIdAndStatus(dto.getTaxId(), "活跃").ifPresent(existing -> {
                throw new IllegalArgumentException("该纳税人识别号已存在于系统，对应联系人：" + existing.getName());
            });
        }
        applyDto(dto, contact);
        ExternalContact saved = repository.save(contact);
        return ResponseEntity.ok(toDto(saved));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        Optional<ExternalContact> optional = repository.findById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ExternalContact contact = optional.get();
        contact.setStatus("停用");
        repository.save(contact);
        return ResponseEntity.noContent().build();
    }

    private ExternalContactDto toDto(ExternalContact c) {
        ExternalContactDto dto = new ExternalContactDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setType(c.getType());
        dto.setTaxId(c.getTaxId());
        dto.setAddress(c.getAddress());
        dto.setPhone(c.getPhone());
        dto.setEmail(c.getEmail());
        dto.setMainContact(c.getMainContact());
        dto.setBankName(c.getBankName());
        dto.setBankAccount(c.getBankAccount());
        dto.setStatus(c.getStatus());
        return dto;
    }

    private void applyDto(ExternalContactDto dto, ExternalContact c) {
        c.setName(dto.getName());
        c.setType(dto.getType());
        c.setTaxId(dto.getTaxId());
        c.setAddress(dto.getAddress());
        c.setPhone(dto.getPhone());
        c.setEmail(dto.getEmail());
        c.setMainContact(dto.getMainContact());
        c.setBankName(dto.getBankName());
        c.setBankAccount(dto.getBankAccount());
        if (dto.getStatus() != null) {
            c.setStatus(dto.getStatus());
        }
    }
}


