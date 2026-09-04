package com.supermarket.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supermarket.dto.ProductCreateRequest;
import com.supermarket.dto.ProductUpdateRequest;
import com.supermarket.entity.Product;
import com.supermarket.exception.BusinessException;
import com.supermarket.exception.ErrorCode;
import com.supermarket.mapper.ProductMapper;
import com.supermarket.service.ProductService;
import com.supermarket.vo.ProductView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductMapper mapper;
    public ProductServiceImpl(ProductMapper mapper) { this.mapper = mapper; }

    @Override @Transactional
    public ProductView create(ProductCreateRequest request) {
        String code = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (mapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getCode, code)) > 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Product code already exists");
        }
        Product product = new Product(); product.setCode(code); product.setName(request.getName());
        product.setUnitPrice(request.getUnitPrice()); product.setEnabled(true); mapper.insert(product); return view(product);
    }
    @Override @Transactional public ProductView update(Long id, ProductUpdateRequest request) {
        Product product = required(id); product.setName(request.getName()); product.setUnitPrice(request.getUnitPrice()); mapper.updateById(product); return view(product);
    }
    @Override @Transactional public ProductView setEnabled(Long id, boolean enabled) {
        Product product = required(id); product.setEnabled(enabled); mapper.updateById(product); return view(product);
    }
    @Override public ProductView find(Long id) { return view(required(id)); }
    @Override public List<ProductView> list() { return mapper.selectList(null).stream().map(this::view).collect(Collectors.toList()); }
    private Product required(Long id) { Product p = mapper.selectById(id); if (p == null) throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found"); return p; }
    private ProductView view(Product p) { return new ProductView(p.getId(), p.getCode(), p.getName(), p.getUnitPrice(), p.getEnabled(), p.getCreatedAt(), p.getUpdatedAt()); }
}
