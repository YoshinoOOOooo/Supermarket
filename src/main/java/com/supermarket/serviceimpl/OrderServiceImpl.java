package com.supermarket.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.dto.CheckoutItemRequest;
import com.supermarket.dto.CheckoutRequest;
import com.supermarket.entity.CustomerOrder;
import com.supermarket.entity.OrderItem;
import com.supermarket.enums.OrderStatus;
import com.supermarket.exception.BusinessException;
import com.supermarket.exception.ErrorCode;
import com.supermarket.mapper.CustomerOrderMapper;
import com.supermarket.mapper.OrderItemMapper;
import com.supermarket.pricing.PricingQuote;
import com.supermarket.service.OrderService;
import com.supermarket.service.PricingQuoteService;
import com.supermarket.vo.OrderItemView;
import com.supermarket.vo.OrderView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.UUID;
import javax.annotation.Resource;

/** 订单服务实现，负责重新计价、保存快照以及订单状态流转。 */
@Service
public class OrderServiceImpl implements OrderService {
    /** 统一报价服务，订单金额始终由服务端重新计算。 */
    @Resource
    private PricingQuoteService pricingQuoteService;
    /** 订单主表持久化访问对象。 */
    @Resource
    private CustomerOrderMapper orderMapper;
    /** 订单商品快照持久化访问对象。 */
    @Resource
    private OrderItemMapper itemMapper;

    /** 在同一事务中重新计价并创建未支付订单及明细快照。 */
    @Override
    @Transactional
    public OrderView create(CheckoutRequest request) {
        requirePositiveQuantity(request);
        PricingQuote quote = pricingQuoteService.quote(request);
        LocalDateTime now = LocalDateTime.now();
        CustomerOrder order = new CustomerOrder();
        order.setOrderNo(UUID.randomUUID().toString());
        order.setStatus(OrderStatus.UNPAID);
        order.setOriginalAmount(quote.getOriginalAmount());
        order.setDiscountAmount(quote.getDiscountAmount());
        order.setPayableAmount(quote.getPayableAmount());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        orderMapper.insert(order);

        List<OrderItem> snapshots = new ArrayList<>();
        for (PricingQuote.Line line : quote.getLines()) {
            OrderItem snapshot = snapshot(order.getId(), line);
            itemMapper.insert(snapshot);
            snapshots.add(snapshot);
        }
        return view(order, snapshots);
    }

    /** 使用一致性只读事务查询订单头和对应商品快照。 */
    @Override
    @Transactional(readOnly = true)
    public OrderView findByOrderNo(UUID orderNo) {
        CustomerOrder order = required(orderNo);
        return view(order, snapshots(order.getId()));
    }

    /** 仅允许未支付订单重新计价并原子替换金额与明细快照。 */
    @Override
    @Transactional
    public OrderView update(UUID orderNo, CheckoutRequest request) {
        requirePositiveQuantity(request);
        CustomerOrder order = required(orderNo);
        if (order.getStatus() != OrderStatus.UNPAID) {
            throw invalidState("Only unpaid orders can be modified");
        }
        PricingQuote quote = pricingQuoteService.quote(request);
        LocalDateTime now = LocalDateTime.now();
        CustomerOrder changedOrder = new CustomerOrder();
        changedOrder.setOriginalAmount(quote.getOriginalAmount());
        changedOrder.setDiscountAmount(quote.getDiscountAmount());
        changedOrder.setPayableAmount(quote.getPayableAmount());
        changedOrder.setUpdatedAt(now);
        changedOrder.setVersion(order.getVersion());
        int changed = orderMapper.update(changedOrder, new LambdaUpdateWrapper<CustomerOrder>()
                .eq(CustomerOrder::getId, order.getId()).eq(CustomerOrder::getStatus, OrderStatus.UNPAID));
        if (changed != 1) throw invalidState("Order state changed concurrently");
        itemMapper.delete(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        List<OrderItem> replacement = new ArrayList<>();
        for (PricingQuote.Line line : quote.getLines()) {
            OrderItem item = snapshot(order.getId(), line); itemMapper.insert(item); replacement.add(item);
        }
        order.setOriginalAmount(quote.getOriginalAmount());
        order.setDiscountAmount(quote.getDiscountAmount());
        order.setPayableAmount(quote.getPayableAmount());
        order.setUpdatedAt(now);
        return view(order, replacement);
    }

    /** 将未支付订单转换为已完成状态。 */
    @Override
    @Transactional
    public OrderView complete(UUID orderNo) {
        return transition(orderNo, OrderStatus.COMPLETED);
    }

    /** 将未支付订单转换为已取消状态。 */
    @Override
    @Transactional
    public OrderView cancel(UUID orderNo) {
        return transition(orderNo, OrderStatus.CANCELLED);
    }

    /** 分页查询订单，并在同一只读事务中组装每张订单的明细。 */
    @Override
    @Transactional(readOnly = true)
    public IPage<OrderView> list(long page, long size) {
        if (page < 1 || size < 1 || size > 100)
            throw invalid("Page must be positive and size must be between 1 and 100");
        IPage<CustomerOrder> result = orderMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<CustomerOrder>().orderByDesc(CustomerOrder::getCreatedAt));
        List<Long> orderIds = new ArrayList<>();
        for (CustomerOrder order : result.getRecords()) orderIds.add(order.getId());
        Map<Long, List<OrderItem>> grouped = snapshotsByOrderIds(orderIds);
        return result.convert(order -> view(order,
                grouped.containsKey(order.getId()) ? grouped.get(order.getId()) : Collections.emptyList()));
    }

    /**
     * 执行带旧状态条件的状态更新。
     * 并发更新失败后重新读取，同目标状态按幂等成功处理。
     */
    private OrderView transition(UUID orderNo, OrderStatus target) {
        CustomerOrder order = required(orderNo);
        if (order.getStatus() == target) return view(order, snapshots(order.getId()));
        if (order.getStatus() != OrderStatus.UNPAID) {
            throw invalidState("Order cannot transition from " + order.getStatus() + " to " + target);
        }
        LocalDateTime now = LocalDateTime.now();
        CustomerOrder update = new CustomerOrder();
        update.setStatus(target);
        update.setUpdatedAt(now);
        update.setVersion(order.getVersion());
        int changed = orderMapper.update(update, new LambdaUpdateWrapper<CustomerOrder>()
                .eq(CustomerOrder::getId, order.getId())
                .eq(CustomerOrder::getStatus, OrderStatus.UNPAID));
        if (changed != 1) {
            CustomerOrder latest = required(orderNo);
            if (latest.getStatus() == target) return view(latest, snapshots(latest.getId()));
            throw invalidState("Order state changed concurrently");
        }
        order.setStatus(target);
        order.setUpdatedAt(now);
        return view(order, snapshots(order.getId()));
    }

    /** 根据对外订单号读取必须存在的订单。 */
    private CustomerOrder required(UUID orderNo) {
        CustomerOrder order = orderMapper.selectOne(new LambdaQueryWrapper<CustomerOrder>()
                .eq(CustomerOrder::getOrderNo, orderNo.toString()));
        if (order == null) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Order not found");
        return order;
    }

    /** 按明细主键顺序查询订单保存的商品快照。 */
    private List<OrderItem> snapshots(Long orderId) {
        return itemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId).orderByAsc(OrderItem::getId));
    }

    /** 一次查询整页订单明细并按订单主键分组，避免逐订单查询。 */
    private Map<Long, List<OrderItem>> snapshotsByOrderIds(List<Long> orderIds) {
        Map<Long, List<OrderItem>> grouped = new LinkedHashMap<>();
        if (orderIds.isEmpty()) return grouped;
        List<OrderItem> all = itemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderId, orderIds).orderByAsc(OrderItem::getId));
        for (OrderItem item : all) {
            List<OrderItem> orderItems = grouped.get(item.getOrderId());
            if (orderItems == null) {
                orderItems = new ArrayList<OrderItem>();
                grouped.put(item.getOrderId(), orderItems);
            }
            orderItems.add(item);
        }
        return grouped;
    }

    /** 根据报价行构造可持久化的订单明细快照。 */
    private OrderItem snapshot(Long orderId, PricingQuote.Line line) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setProductId(line.getProductId());
        item.setProductCode(line.getProductCode());
        item.setProductName(line.getProductName());
        item.setQuantity(line.getQuantity());
        item.setUnitPrice(line.getUnitPrice());
        item.setOriginalAmount(line.getOriginalAmount());
        item.setDiscountAmount(line.getDiscountAmount());
        item.setPayableAmount(line.getPayableAmount());
        return item;
    }

    /** 将订单实体和明细快照组装为对外订单视图。 */
    private OrderView view(CustomerOrder order, List<OrderItem> snapshots) {
        List<OrderItemView> itemViews = new ArrayList<>();
        for (OrderItem item : snapshots) {
            itemViews.add(new OrderItemView(item.getProductCode(), item.getProductName(), item.getQuantity(),
                    item.getUnitPrice(), item.getOriginalAmount(), item.getDiscountAmount(), item.getPayableAmount()));
        }
        return new OrderView(order.getOrderNo(), order.getStatus(), order.getOriginalAmount(),
                order.getDiscountAmount(), order.getPayableAmount(), itemViews,
                order.getCreatedAt(), order.getUpdatedAt());
    }

    /** 正式订单至少需要一个购买数量大于零的商品。 */
    private void requirePositiveQuantity(CheckoutRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw invalid("Order items must not be empty");
        }
        for (CheckoutItemRequest item : request.getItems()) {
            if (item != null && item.getQuantity() != null && item.getQuantity() > 0) return;
        }
        throw invalid("Order must contain a positive quantity");
    }

    /** 创建普通请求参数错误。 */
    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_REQUEST, message);
    }

    /** 创建订单状态冲突错误。 */
    private BusinessException invalidState(String message) {
        return new BusinessException(ErrorCode.INVALID_ORDER_STATE, message);
    }
}
