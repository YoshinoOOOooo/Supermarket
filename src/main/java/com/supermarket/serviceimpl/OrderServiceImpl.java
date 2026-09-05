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
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {
    private final PricingQuoteService pricingQuoteService;
    private final CustomerOrderMapper orderMapper;
    private final OrderItemMapper itemMapper;

    public OrderServiceImpl(PricingQuoteService pricingQuoteService, CustomerOrderMapper orderMapper,
                            OrderItemMapper itemMapper) {
        this.pricingQuoteService = pricingQuoteService;
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
    }

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

        List<OrderItem> snapshots = new ArrayList<OrderItem>();
        for (PricingQuote.Line line : quote.getLines()) {
            OrderItem snapshot = snapshot(order.getId(), line);
            itemMapper.insert(snapshot);
            snapshots.add(snapshot);
        }
        return view(order, snapshots);
    }

    @Override
    public OrderView findByOrderNo(UUID orderNo) {
        CustomerOrder order = required(orderNo);
        return view(order, snapshots(order.getId()));
    }

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
        int changed = orderMapper.update(changedOrder, new LambdaUpdateWrapper<CustomerOrder>()
                .eq(CustomerOrder::getId, order.getId()).eq(CustomerOrder::getStatus, OrderStatus.UNPAID));
        if (changed != 1) throw invalidState("Order state changed concurrently");
        itemMapper.delete(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        List<OrderItem> replacement = new ArrayList<OrderItem>();
        for (PricingQuote.Line line : quote.getLines()) {
            OrderItem item = snapshot(order.getId(), line); itemMapper.insert(item); replacement.add(item);
        }
        order.setOriginalAmount(quote.getOriginalAmount()); order.setDiscountAmount(quote.getDiscountAmount());
        order.setPayableAmount(quote.getPayableAmount()); order.setUpdatedAt(now);
        return view(order, replacement);
    }

    @Override
    @Transactional
    public OrderView complete(UUID orderNo) {
        return transition(orderNo, OrderStatus.COMPLETED);
    }

    @Override
    @Transactional
    public OrderView cancel(UUID orderNo) {
        return transition(orderNo, OrderStatus.CANCELLED);
    }

    @Override
    public IPage<OrderView> list(long page, long size) {
        if (page < 1 || size < 1) throw invalid("Page and size must be positive");
        IPage<CustomerOrder> result = orderMapper.selectPage(new Page<CustomerOrder>(page, size),
                new LambdaQueryWrapper<CustomerOrder>().orderByDesc(CustomerOrder::getCreatedAt));
        return result.convert(order -> view(order, snapshots(order.getId())));
    }

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

    private CustomerOrder required(UUID orderNo) {
        CustomerOrder order = orderMapper.selectOne(new LambdaQueryWrapper<CustomerOrder>()
                .eq(CustomerOrder::getOrderNo, orderNo.toString()));
        if (order == null) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Order not found");
        return order;
    }

    private List<OrderItem> snapshots(Long orderId) {
        return itemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId).orderByAsc(OrderItem::getId));
    }

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

    private OrderView view(CustomerOrder order, List<OrderItem> snapshots) {
        List<OrderItemView> itemViews = new ArrayList<OrderItemView>();
        for (OrderItem item : snapshots) {
            itemViews.add(new OrderItemView(item.getProductCode(), item.getProductName(), item.getQuantity(),
                    item.getUnitPrice(), item.getOriginalAmount(), item.getDiscountAmount(), item.getPayableAmount()));
        }
        return new OrderView(order.getOrderNo(), order.getStatus(), order.getOriginalAmount(),
                order.getDiscountAmount(), order.getPayableAmount(), itemViews,
                order.getCreatedAt(), order.getUpdatedAt());
    }

    private void requirePositiveQuantity(CheckoutRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw invalid("Order items must not be empty");
        }
        for (CheckoutItemRequest item : request.getItems()) {
            if (item != null && item.getQuantity() != null && item.getQuantity() > 0) return;
        }
        throw invalid("Order must contain a positive quantity");
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_REQUEST, message);
    }

    private BusinessException invalidState(String message) {
        return new BusinessException(ErrorCode.INVALID_ORDER_STATE, message);
    }
}
