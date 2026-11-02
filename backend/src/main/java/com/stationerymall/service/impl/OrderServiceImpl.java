package com.stationerymall.service.impl;

import com.stationerymall.dto.OrderCreateDTO;
import com.stationerymall.entity.CartItem;
import com.stationerymall.entity.Comment;
import com.stationerymall.entity.Goods;
import com.stationerymall.entity.Order;
import com.stationerymall.entity.OrderItem;
import com.stationerymall.enums.BizCodeEnum;
import com.stationerymall.enums.GoodsStatusEnum;
import com.stationerymall.enums.OrderStatusEnum;
import com.stationerymall.enums.UserRoleEnum;
import com.stationerymall.exception.BusinessStateException;
import com.stationerymall.exception.ForbiddenException;
import com.stationerymall.exception.StockNotEnoughException;
import com.stationerymall.exception.UnauthException;
import com.stationerymall.mapper.CartItemMapper;
import com.stationerymall.mapper.CommentMapper;
import com.stationerymall.mapper.GoodsMapper;
import com.stationerymall.mapper.OrderItemMapper;
import com.stationerymall.mapper.OrderMapper;
import com.stationerymall.service.OrderItemService;
import com.stationerymall.service.OrderService;
import com.stationerymall.util.UserContext;
import com.stationerymall.vo.OrderItemVO;
import com.stationerymall.vo.OrderVO;
import com.stationerymall.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 订单服务实现。
 * <p>
 * 订单状态机（Prompt 中的 PENDING_PAY 即本系统 {@link OrderStatusEnum#UNPAID}）：
 * 仅 UNPAID 可取消（取消回补库存）；仅 PAID 可发货（管理员）；
 * 仅 SHIPPED 可确认收货（用户）；COMPLETED / CANCELLED 为终态不可再变更。
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /** 订单号时间部分格式 */
    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 默认页大小 */
    private static final int DEFAULT_PAGE_SIZE = 10;

    /** 页大小上限（对齐 api-spec 1.5） */
    private static final int MAX_PAGE_SIZE = 50;

    private final OrderMapper orderMapper;

    private final OrderItemMapper orderItemMapper;

    private final CartItemMapper cartItemMapper;

    private final GoodsMapper goodsMapper;

    private final CommentMapper commentMapper;

    private final OrderItemService orderItemService;

    /** 订单日志（SLF4J）：查询订单前打印当前登录身份，便于排查数据可见范围 */
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    /**
     * 下单：扣库存、写订单、写明细、清购物车在同一事务中完成。
     * <p>
     * 金额完全由服务端按商品当前价格重算，DTO 中不存在 totalAmount 字段（不采信前端金额）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(OrderCreateDTO dto) {
        // 保留原有业务规则：管理员禁止下单（requireNormalUser 内对 ADMIN 抛 403）
        // userId 取自 JWT 登录态，不接收前端传值
        Long userId = requireNormalUser();
        log.info("【下单】当前登录 userId={}，userRole={}", userId, UserContext.getRole());

        // 1. 勾选的购物车行必须全部属于当前用户（带 user_id 条件查询，数量不一致即越权）
        List<CartItem> cartItems = cartItemMapper.selectByIdsAndUserId(dto.getCartItemIds(), userId);
        if (cartItems.size() != new HashSet<>(dto.getCartItemIds()).size()) {
            throw new ForbiddenException("存在不属于当前用户的购物车项");
        }
        if (cartItems.isEmpty()) {
            throw new BusinessStateException("请至少勾选一件购物车商品");
        }

        // 2. 逐行校验上架 + 库存，并准备「下单快照」明细
        List<OrderItem> orderItems = new ArrayList<>(cartItems.size());
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Goods goods = goodsMapper.selectById(cartItem.getProductId());
            if (goods == null) {
                throw new BusinessStateException(BizCodeEnum.NOT_FOUND, "商品不存在，请刷新购物车");
            }
            if (goods.getOnShelf() != GoodsStatusEnum.ON_SHELF) {
                throw new BusinessStateException("商品【" + goods.getName() + "】已下架，请刷新购物车后重新结算");
            }
            if (goods.getStock() < cartItem.getQuantity()) {
                throw new StockNotEnoughException(goods.getName(), goods.getStock());
            }

            OrderItem item = new OrderItem();
            item.setProductId(goods.getId());
            // 下单快照：此刻的名称与单价，后续商品改名/改价不影响历史订单
            item.setProductName(goods.getName());
            item.setUnitPrice(goods.getPrice());
            item.setQuantity(cartItem.getQuantity());
            item.setSubtotal(goods.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            orderItems.add(item);

            totalAmount = totalAmount.add(item.getSubtotal());
        }

        // 3. 写订单主单（初始状态 UNPAID，等待用户模拟付款）
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setStatus(OrderStatusEnum.UNPAID);
        order.setTotalAmount(totalAmount);
        order.setAddress(dto.getAddress());
        order.setRemark(dto.getRemark());
        order.setCreatedAt(LocalDateTime.now());
        orderMapper.insert(order);

        // 4. 批量写明细（明细 id 由数据库生成，无需回填）
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
        }
        orderItemMapper.batchInsert(orderItems);

        // 5. 行级扣减库存：影响行数为 0 说明被并发抢光，抛出异常使整单回滚
        for (CartItem cartItem : cartItems) {
            int rows = goodsMapper.decreaseStock(cartItem.getProductId(), cartItem.getQuantity());
            if (rows == 0) {
                throw new StockNotEnoughException("商品库存不足，下单失败");
            }
        }

        // 6. 删除已下单的购物车行
        cartItemMapper.deleteByIdsAndUserId(dto.getCartItemIds(), userId);

        return toOrderVO(order, orderItems.stream().map(this::toItemVO).toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO payOrder(Long orderId) {
        Long userId = requireNormalUser();
        // 支付操作日志：记录操作人与订单，便于排查（管理员下单拦截仍由 requireNormalUser 负责）
        log.info("【订单付款】userId={}，role={}，orderId={}", userId, UserContext.getRole(), orderId);
        Order order = getOwnedOrder(orderId, userId);
        // 状态机：仅 UNPAID 可付款
        if (!order.getStatus().canPay()) {
            throw new BusinessStateException("仅待支付订单可付款，当前状态：" + order.getStatus().getDesc());
        }
        Order update = new Order();
        update.setId(orderId);
        update.setStatus(OrderStatusEnum.PAID);
        update.setPaidAt(LocalDateTime.now());
        orderMapper.updateById(update);
        log.info("【订单付款】成功：orderId={}，状态 UNPAID → PAID", orderId);
        return loadDetailVO(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO cancelOrder(Long orderId) {
        Long userId = requireNormalUser();
        Order order = getOwnedOrder(orderId, userId);
        // 状态机：仅 UNPAID 可取消；COMPLETED / CANCELLED 为终态
        if (!order.getStatus().canCancel()) {
            throw new BusinessStateException("仅待支付订单可取消，当前状态：" + order.getStatus().getDesc());
        }

        // 取消需要把已扣的库存逐行回补（与下单扣减对称）
        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        for (OrderItem item : items) {
            goodsMapper.increaseStock(item.getProductId(), item.getQuantity());
        }

        Order update = new Order();
        update.setId(orderId);
        update.setStatus(OrderStatusEnum.CANCELLED);
        update.setCancelledAt(LocalDateTime.now());
        orderMapper.updateById(update);
        return loadDetailVO(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO confirmReceipt(Long orderId) {
        Long userId = requireNormalUser();
        Order order = getOwnedOrder(orderId, userId);
        // 状态机：仅 SHIPPED 可确认收货（PAID 必须先由管理员发货）
        if (!order.getStatus().canConfirmReceipt()) {
            throw new BusinessStateException("仅已发货订单可确认收货，当前状态：" + order.getStatus().getDesc());
        }
        Order update = new Order();
        update.setId(orderId);
        update.setStatus(OrderStatusEnum.COMPLETED);
        update.setCompletedAt(LocalDateTime.now());
        orderMapper.updateById(update);
        return loadDetailVO(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO adminShip(Long orderId) {
        requireAdmin();
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessStateException(BizCodeEnum.NOT_FOUND, "订单不存在");
        }
        // 状态机：仅 PAID 可发货（管理员只能做这一步）
        if (!order.getStatus().canShip()) {
            throw new BusinessStateException("仅已支付订单可发货，当前状态：" + order.getStatus().getDesc());
        }
        Order update = new Order();
        update.setId(orderId);
        update.setStatus(OrderStatusEnum.SHIPPED);
        update.setShippedAt(LocalDateTime.now());
        orderMapper.updateById(update);
        return loadDetailVO(orderId);
    }

    @Override
    public PageVO<OrderVO> listMyOrders(OrderStatusEnum status, Long cursor, Integer size) {
        Long userId = requireNormalUser();
        // 查询前打印当前登录身份（默认查询仅本人订单）
        log.info("【我的订单】查询前：当前登录 userId={}，role={}，status={}", userId, UserContext.getRole(), status);
        int pageSize = normalizeSize(size);
        // 多查 1 条判断 hasMore
        List<Order> orders = orderMapper.selectByUserId(userId, status, cursor, pageSize + 1);
        return toOrderPage(orders, pageSize);
    }

    @Override
    public PageVO<OrderVO> listOrdersByRole(OrderStatusEnum status, Long cursor, Integer size) {
        // 未登录直接 401；身份信息全部来自 JWT 登录态，前端不传 userId
        requireLogin();

        UserRoleEnum userRole = UserContext.getRole();
        // 严格校验：非管理员必须能取到登录 userId，否则视为登录态异常
        Long userId = userRole.isAdmin() ? null : UserContext.getUserId();
        if (!userRole.isAdmin() && userId == null) {
            throw new UnauthException("未获取到登录用户 id，请重新登录");
        }

        // 查询前日志：入参与角色、最终 SQL 条件
        log.info("【订单列表】查询入参：userId={}，userRole={}，status={}，cursor={}，size={}",
                userId, userRole, status, cursor, size);
        log.info("【订单列表】最终执行 SQL 条件：{}",
                userId == null
                        ? "userRole=ADMIN → 不追加 user_id 条件，查询全部订单"
                        : "userRole=NORMAL_USER → WHERE o.user_id = " + userId);

        int pageSize = normalizeSize(size);
        // 多查 1 条判断 hasMore
        List<Order> orders = orderMapper.selectList(userId, status, cursor, pageSize + 1);
        return toOrderPage(orders, pageSize);
    }

    @Override
    public OrderVO getMyOrderDetail(Long orderId) {
        Long userId = requireLogin();
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessStateException(BizCodeEnum.NOT_FOUND, "订单不存在");
        }
        // 归属权校验：普通用户只能看自己的订单，管理员可看全部
        if (!UserContext.isAdmin() && !userId.equals(order.getUserId())) {
            throw new ForbiddenException("无权查看他人订单");
        }
        OrderVO vo = toOrderVO(order, orderItemService.listByOrderId(orderId));
        markEvaluated(vo);
        return vo;
    }

    @Override
    public PageVO<OrderVO> adminListOrders(OrderStatusEnum status, Long cursor, Integer size) {
        requireAdmin();
        int pageSize = normalizeSize(size);
        List<Order> orders = orderMapper.selectAll(status, cursor, pageSize + 1);
        return toOrderPage(orders, pageSize);
    }

    /* ==================== 私有辅助 ==================== */

    /** 当前登录普通用户 id：未登录 401；管理员禁止下单 403 */
    private Long requireNormalUser() {
        Long userId = requireLogin();
        if (!UserContext.getRole().canPlaceOrder()) {
            throw new ForbiddenException("管理员不能下单");
        }
        return userId;
    }

    private Long requireLogin() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new UnauthException();
        }
        return userId;
    }

    private void requireAdmin() {
        if (!UserContext.isAdmin()) {
            throw new ForbiddenException("仅管理员可执行该操作");
        }
    }

    /** 查询订单并校验归属权 */
    private Order getOwnedOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessStateException(BizCodeEnum.NOT_FOUND, "订单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            throw new ForbiddenException("无权操作他人订单");
        }
        return order;
    }

    /** 状态变更后重新查询并组装详情 VO（同时标记每条明细是否已评价） */
    private OrderVO loadDetailVO(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        OrderVO vo = toOrderVO(order, orderItemService.listByOrderId(orderId));
        markEvaluated(vo);
        return vo;
    }

    /** 订单号：SO + yyyyMMddHHmmss + 3 位随机数（唯一索引兜底） */
    private String generateOrderNo() {
        return "SO" + LocalDateTime.now().format(ORDER_NO_FORMATTER)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    /** 列表分页：一次性批量查明细汇总件数（避免 N+1），并随订单返回明细（含商品介绍），再裁剪成 PageVO */
    private PageVO<OrderVO> toOrderPage(List<Order> orders, int pageSize) {
        if (orders.isEmpty()) {
            return PageVO.empty();
        }
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        List<OrderItem> items = orderItemMapper.selectByOrderIds(orderIds);
        Map<Long, Integer> countMap = new HashMap<>();
        // 按订单分组明细：订单列表接口也要返回订单项（含商品介绍），供前端逐条渲染
        Map<Long, List<OrderItemVO>> itemMap = new HashMap<>();
        for (OrderItem item : items) {
            countMap.merge(item.getOrderId(), item.getQuantity(), Integer::sum);
            itemMap.computeIfAbsent(item.getOrderId(), key -> new ArrayList<>()).add(toItemVO(item));
        }
        // 批量查评价（一次 SQL），列表页也能回填「是否已评价 + 评价文字」
        Map<Long, List<Comment>> commentMap = commentMapper.selectByOrderIds(orderIds).stream()
                .collect(Collectors.groupingBy(Comment::getOrderId));

        List<OrderVO> result = new ArrayList<>(orders.size());
        for (Order order : orders) {
            OrderVO vo = baseVO(order);
            vo.setItemCount(countMap.getOrDefault(order.getId(), 0));
            vo.setItems(itemMap.getOrDefault(order.getId(), List.of()));
            markEvaluated(vo, commentMap.getOrDefault(order.getId(), List.of()));
            result.add(vo);
        }
        return PageVO.of(result, pageSize, OrderVO::getId);
    }

    /**
     * 标记明细是否已评价：按订单查一次评价，构建 productId 集合后回填。
     * 前端据此隐藏「去评价」按钮，无需自行判断。
     */
    private void markEvaluated(OrderVO vo) {
        markEvaluated(vo, commentMapper.selectByOrderId(vo.getId()));
    }

    /** 回填评价状态与评价文字（列表页使用批量查询结果，避免 N+1） */
    private void markEvaluated(OrderVO vo, List<Comment> comments) {
        if (vo.getItems() == null || vo.getItems().isEmpty()) {
            return;
        }
        Map<Long, String> contentMap = comments.stream()
                .collect(Collectors.toMap(Comment::getProductId, Comment::getContent, (a, b) -> a));
        for (OrderItemVO item : vo.getItems()) {
            item.setEvaluated(contentMap.containsKey(item.getProductId()));
            // 评价文字：已评价返回原文，未评价为 null（前端只做只读展示）
            item.setEvaluationContent(contentMap.get(item.getProductId()));
        }
    }

    /** 页大小归一：null / 非正数取默认 10，超过上限按 50 处理 */
    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private OrderVO toOrderVO(Order order, List<OrderItemVO> items) {
        OrderVO vo = baseVO(order);
        vo.setItems(items);
        vo.setItemCount(items == null ? 0 : items.stream().mapToInt(OrderItemVO::getQuantity).sum());
        return vo;
    }

    /** 订单公共字段转换 */
    private OrderVO baseVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        // 用户名由 Mapper LEFT JOIN users 回填（后台订单列表 / 详情展示）
        vo.setUsername(order.getUsername());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(order.getStatus() == null ? null : order.getStatus().getDesc());
        // 三个操作布尔值由服务端按状态机预计算，前端直接读
        vo.setCanCancel(order.getStatus() != null && order.getStatus().canCancel());
        vo.setCanConfirmReceipt(order.getStatus() != null && order.getStatus().canConfirmReceipt());
        vo.setCanEvaluate(order.getStatus() != null && order.getStatus().canEvaluate());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setAddress(order.getAddress());
        vo.setRemark(order.getRemark());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setPaidAt(order.getPaidAt());
        vo.setShippedAt(order.getShippedAt());
        vo.setCompletedAt(order.getCompletedAt());
        vo.setCancelledAt(order.getCancelledAt());
        return vo;
    }

    private OrderItemVO toItemVO(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        vo.setId(item.getId());
        vo.setProductId(item.getProductId());
        vo.setProductName(item.getProductName());
        vo.setUnitPrice(item.getUnitPrice());
        vo.setQuantity(item.getQuantity());
        vo.setSubtotal(item.getSubtotal());
        // 商品介绍：由明细查询 LEFT JOIN products 取得（别名 product_intro → productIntro）
        vo.setProductIntro(item.getProductIntro());
        // 默认未评价，详情场景由 {@link #markEvaluated} 覆盖
        vo.setEvaluated(false);
        return vo;
    }
}
