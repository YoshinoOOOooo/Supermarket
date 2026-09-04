package com.supermarket.service;

import com.supermarket.dto.ProductCreateRequest;
import com.supermarket.dto.ProductUpdateRequest;
import com.supermarket.vo.ProductView;
import java.util.List;

public interface ProductService {
    ProductView create(ProductCreateRequest request);
    ProductView update(Long id, ProductUpdateRequest request);
    ProductView setEnabled(Long id, boolean enabled);
    ProductView find(Long id);
    List<ProductView> list();
}
