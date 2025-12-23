package com.example.accounting.web.controller;

import com.example.accounting.domain.Warehouse;
import com.example.accounting.repository.WarehouseRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/warehouses")
@CrossOrigin
public class WarehouseController {

    private final WarehouseRepository warehouseRepository;

    public WarehouseController(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    @GetMapping
    public List<WarehouseDto> list(@RequestParam(required = false) Boolean enabled) {
        return warehouseRepository.findAll().stream()
                .filter(w -> enabled == null || w.isEnabled() == enabled)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseDto> get(@PathVariable Long id) {
        return warehouseRepository.findById(id)
                .map(w -> ResponseEntity.ok(toDto(w)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<WarehouseDto> create(@Valid @RequestBody WarehouseDto dto) {
        Warehouse warehouse = new Warehouse();
        warehouse.setCode(dto.getCode());
        warehouse.setName(dto.getName());
        warehouse.setAddress(dto.getAddress());
        warehouse.setEnabled(dto.isEnabled() != null ? dto.isEnabled() : true);
        Warehouse saved = warehouseRepository.save(warehouse);
        return ResponseEntity.ok(toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WarehouseDto> update(@PathVariable Long id, @Valid @RequestBody WarehouseDto dto) {
        return warehouseRepository.findById(id)
                .map(warehouse -> {
                    warehouse.setCode(dto.getCode());
                    warehouse.setName(dto.getName());
                    warehouse.setAddress(dto.getAddress());
                    if (dto.isEnabled() != null) {
                        warehouse.setEnabled(dto.isEnabled());
                    }
                    Warehouse saved = warehouseRepository.save(warehouse);
                    return ResponseEntity.ok(toDto(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private WarehouseDto toDto(Warehouse w) {
        WarehouseDto dto = new WarehouseDto();
        dto.setId(w.getId());
        dto.setCode(w.getCode());
        dto.setName(w.getName());
        dto.setAddress(w.getAddress());
        dto.setEnabled(w.isEnabled());
        return dto;
    }

    public static class WarehouseDto {
        private Long id;
        private String code;
        private String name;
        private String address;
        private Boolean enabled;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public Boolean isEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }
}

