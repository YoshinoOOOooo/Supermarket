package com.supermarket.controller.admin;

import com.supermarket.dto.ProductCreateRequest;
import com.supermarket.dto.ProductUpdateRequest;
import com.supermarket.config.OpenApiConfig;
import com.supermarket.service.ProductService;
import com.supermarket.vo.ProductView;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import javax.annotation.Resource;
import java.util.List;

@RestController @RequestMapping("/api/admin/products")
@SecurityRequirement(name = OpenApiConfig.ADMIN_BASIC_SCHEME)
public class ProductAdminController {
    @Resource
    private ProductService service;
    @PostMapping public ProductView create(@Valid @RequestBody ProductCreateRequest request) { return service.create(request); }

    @GetMapping("/{id}") public ProductView find(@PathVariable Long id) { return service.find(id); }

    @GetMapping public List<ProductView> list() { return service.list(); }

    @PutMapping("/{id}") public ProductView update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        return service.update(id, request); }

    @PatchMapping("/{id}/enabled") public ProductView setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        return service.setEnabled(id, enabled); }
}
