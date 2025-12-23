package com.example.accounting.web.controller;

import com.example.accounting.domain.Product;
import com.example.accounting.repository.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@CrossOrigin
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<ProductDto> list(@RequestParam(required = false) Boolean enabled) {
        return productRepository.findAll().stream()
                .filter(p -> enabled == null || p.isEnabled() == enabled)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> get(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(p -> ResponseEntity.ok(toDto(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ProductDto> create(@Valid @RequestBody ProductDto dto) {
        Product product = new Product();
        product.setCode(dto.getCode());
        product.setName(dto.getName());
        product.setSpecification(dto.getSpecification());
        product.setUnit(dto.getUnit());
        product.setEnabled(dto.isEnabled() != null ? dto.isEnabled() : true);
        Product saved = productRepository.save(product);
        return ResponseEntity.ok(toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> update(@PathVariable Long id, @Valid @RequestBody ProductDto dto) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setCode(dto.getCode());
                    product.setName(dto.getName());
                    product.setSpecification(dto.getSpecification());
                    product.setUnit(dto.getUnit());
                    if (dto.isEnabled() != null) {
                        product.setEnabled(dto.isEnabled());
                    }
                    Product saved = productRepository.save(product);
                    return ResponseEntity.ok(toDto(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private ProductDto toDto(Product p) {
        ProductDto dto = new ProductDto();
        dto.setId(p.getId());
        dto.setCode(p.getCode());
        dto.setName(p.getName());
        dto.setSpecification(p.getSpecification());
        dto.setUnit(p.getUnit());
        dto.setLastPurchasePrice(p.getLastPurchasePrice());
        dto.setEnabled(p.isEnabled());
        return dto;
    }

    public static class ProductDto {
        private Long id;
        private String code;
        private String name;
        private String specification;
        private String unit;
        private java.math.BigDecimal lastPurchasePrice;
        private Boolean enabled;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSpecification() { return specification; }
        public void setSpecification(String specification) { this.specification = specification; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public java.math.BigDecimal getLastPurchasePrice() { return lastPurchasePrice; }
        public void setLastPurchasePrice(java.math.BigDecimal lastPurchasePrice) { this.lastPurchasePrice = lastPurchasePrice; }
        public Boolean isEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }
}

