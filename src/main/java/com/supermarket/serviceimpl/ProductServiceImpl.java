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
import org.springframework.dao.DataIntegrityViolationException;
import javax.annotation.Resource;

/** 商品管理服务实现，负责商品新增、改价、启停和查询。 */
@Service
public class ProductServiceImpl implements ProductService {
    /** 商品持久化访问对象。 */
    @Resource
    private ProductMapper mapper;

    /** 创建商品，统一规范化编码并处理唯一键并发冲突。 */
    @Override @Transactional
    public ProductView create(ProductCreateRequest request) {
        String code = request.getCode().trim().toUpperCase(Locale.ROOT);

        if (mapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getCode, code)) > 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Product code already exists");
        }
        Product product = new Product();
        product.setCode(code);
        product.setName(request.getName());
        product.setUnitPrice(request.getUnitPrice());
        product.setEnabled(true);

        try {
            if (mapper.insert(product) != 1) throw conflict("Product was not created");
        }
        catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "Product code already exists");
        }
        return view(required(product.getId()));
    }
    /** 更新指定商品的名称和每斤单价。 */
    @Override @Transactional public ProductView update(Long id, ProductUpdateRequest request) {
        Product product = required(id);
        product.setName(request.getName());

        product.setUnitPrice(request.getUnitPrice());
        if (mapper.updateById(product) != 1) throw conflict("Product changed concurrently");
        return view(required(id));
    }
    /** 启用或停用指定商品。 */
    @Override @Transactional public ProductView setEnabled(Long id, boolean enabled) {
        Product product = required(id);

        product.setEnabled(enabled);
        if (mapper.updateById(product) != 1) throw conflict("Product changed concurrently");
        return view(required(id));

    }
    /** 根据主键查询商品，不存在时抛出业务异常。 */
    @Override public ProductView find(Long id) { return view(required(id)); }

    /** 查询全部商品并转换为接口视图。 */
    @Override public List<ProductView> list() {
        return mapper.selectList(null).stream().map(this::view).collect(Collectors.toList());
    }

    /** 获取必须存在的商品实体。 */
    private Product required(Long id) {
        Product p = mapper.selectById(id);
        if (p == null) throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found"); return p; }

    /** 创建统一的商品写入冲突异常。 */
    private BusinessException conflict(String message) {
        return new BusinessException(ErrorCode.RESOURCE_CONFLICT, message);
    }

    /** 将商品实体转换为对外视图，避免控制器暴露持久化对象。 */
    private ProductView view(Product p) {
        return new ProductView(p.getId(), p.getCode(), p.getName(), p.getUnitPrice(),
                p.getEnabled(), p.getCreatedAt(), p.getUpdatedAt()); }
}
