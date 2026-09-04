package com.supermarket.controller.admin;

import com.supermarket.dto.PromotionCreateRequest;
import com.supermarket.dto.PromotionUpdateRequest;
import com.supermarket.config.OpenApiConfig;
import com.supermarket.service.PromotionService;
import com.supermarket.vo.PromotionView;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;

@RestController @RequestMapping("/api/admin/promotions")
@SecurityRequirement(name = OpenApiConfig.ADMIN_BASIC_SCHEME)
public class PromotionAdminController {
    private final PromotionService service;
    public PromotionAdminController(PromotionService service) { this.service = service; }
    @PostMapping public PromotionView create(@Valid @RequestBody PromotionCreateRequest request) { return service.create(request); }
    @GetMapping("/{id}") public PromotionView find(@PathVariable Long id) { return service.find(id); }
    @GetMapping public List<PromotionView> list() { return service.list(); }
    @PutMapping("/{id}") public PromotionView update(@PathVariable Long id, @Valid @RequestBody PromotionUpdateRequest request) { return service.update(id, request); }
    @PatchMapping("/{id}/enabled") public PromotionView setEnabled(@PathVariable Long id, @RequestParam boolean enabled) { return service.setEnabled(id, enabled); }
}
