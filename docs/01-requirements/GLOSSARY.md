# 统一领域语言词汇表（GLOSSARY）

> 版本：v1.0 · 日期：2026-10-29
> 冻结来源：[PRD.md](./PRD.md) + [USER_STORIES.md](./USER_STORIES.md)
> **使用规则**：任何数据库 DDL、Java 实体、Vue 组件、API 路由的命名**必须**以本词汇表为准，禁止 AI 自行发明同义词（如 `buyer`、`shopItem`、`delivered` 等一律视为禁用名）

---

## 0. 三端命名约定速查

| 维度 | 数据库（MySQL 8） | Java 17（Spring Boot 3） | Vue 3 + TypeScript |
|------|-------------------|--------------------------|--------------------|
| **实体类 / 表名** | `snake_case` 复数（例：`order_items`） | `PascalCase` 单数（例：`OrderItem`） | 接口类型 `PascalCase`（例：`OrderItem`） |
| **字段 / 属性** | `snake_case`（例：`created_at`） | `camelCase`（例：`createdAt`） | `camelCase`（例：`createdAt`） |
| **枚举值** | `TINYINT` + 注释（例：0='UNPAID'） | `UPPER_SNAKE_CASE`（例：`UNPAID`） | `UPPER_SNAKE_CASE`（例：`'UNPAID'`） |
| **主键** | `id BIGINT UNSIGNED AUTO_INCREMENT` | `private Long id;` | `id: number` |
| **布尔字段** | `TINYINT(1)`（0=false, 1=true） | `Boolean` | `boolean` |
| **时间戳** | `DATETIME` | `LocalDateTime` | `string`（ISO 8601） |
| **金额** | `DECIMAL(10,2)` | `BigDecimal` | `number` |
| **外键** | `xxx_id BIGINT UNSIGNED` | 关联对象 or `Long xxxId` | `xxxId: number` |
| **删除方式** | 物理删除（MVP）；下架用 `on_shelf` 标志 | — | — |

---

## 1. 核心领域实体（Domain Entities）

> 共 **7 张表**，严格对齐作业文档第三节"数据"

### 1.1 用户 User

| 中文概念 | 统一英文标识符 | 数据库（snake_case） | Java（Pascal + camel） | Vue TS（camel） | 业务含义与边界 |
|----------|---------------|----------------------|------------------------|-----------------|----------------|
| 用户 | `User` | 表：`users` | 实体：`User` | 接口：`User` | 系统认证主体，包含游客以外的已注册账户 |
| 用户 ID | `id` | `id` BIGINT UNSIGNED PK | `private Long id;` | `id: number` | 自增主键 |
| 用户名 | `username` | `username` VARCHAR(32) UNIQUE NOT NULL | `private String username;` | `username: string` | 全局唯一；登录凭证之一 |
| 密码（加密后） | `password` | `password` VARCHAR(128) NOT NULL | `private String password;` | `password: string`（仅前端表单） | BCrypt 哈希 + 盐；**禁止明文存库** |
| 角色 | `role` | `role` TINYINT NOT NULL | `private UserRole role;` | `role: UserRole` | 枚举 `UserRole`；普通用户注册固定 `NORMAL_USER` |
| 创建时间 | `createdAt` | `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP | `private LocalDateTime createdAt;` | `createdAt: string` | 注册时间 |
| 更新时间 | `updatedAt` | `updated_at` DATETIME ON UPDATE CURRENT_TIMESTAMP | `private LocalDateTime updatedAt;` | `updatedAt: string` | 资料更新时间 |

**禁用命名**：`account`、`member`、`customer`、`buyer`、`pwd`、`passwd`

---

### 1.2 商品分类 Category（两级）

| 中文概念 | 统一英文标识符 | 数据库 | Java | Vue TS | 业务含义与边界 |
|----------|---------------|--------|------|--------|----------------|
| 分类 | `Category` | 表：`categories` | 实体：`Category` | 接口：`Category` | 严格两级结构；`parent_id IS NULL` = 一级分类；非空 = 二级分类 |
| 分类 ID | `id` | `id` BIGINT UNSIGNED PK | `private Long id;` | `id: number` | 自增主键 |
| 分类名称 | `name` | `name` VARCHAR(32) NOT NULL | `private String name;` | `name: string` | 同层级唯一 |
| 父分类 ID | `parentId` | `parent_id` BIGINT UNSIGNED NULLABLE | `private Long parentId;` | `parentId: number \| null` | NULL 表示一级分类；**商品只能挂二级分类** |
| 排序序号 | `sortOrder` | `sort_order` INT NOT NULL DEFAULT 0 | `private Integer sortOrder;` | `sortOrder: number` | 同级展示顺序，数字越小越靠前 |

**禁用命名**：`catalog`、`type`、`kind`、`parent_category_id`（用 `parent_id`）

---

### 1.3 商品 Product

| 中文概念 | 统一英文标识符 | 数据库 | Java | Vue TS | 业务含义与边界 |
|----------|---------------|--------|------|--------|----------------|
| 商品 | `Product` | 表：`products` | 实体：`Product` | 接口：`Product` | MVP 单规格：一个商品一个价格一个库存 |
| 商品 ID | `id` | `id` BIGINT UNSIGNED PK | `private Long id;` | `id: number` | 自增主键 |
| 商品名称 | `name` | `name` VARCHAR(128) NOT NULL | `private String name;` | `name: string` | 商品展示名 |
| 单价 | `price` | `price` DECIMAL(10,2) NOT NULL | `private BigDecimal price;` | `price: number` | 当前售价；历史价格快照存 `order_items.unit_price` |
| 库存 | `stock` | `stock` INT UNSIGNED NOT NULL DEFAULT 0 | `private Integer stock;` | `stock: number` | ≥ 0；不允许负数 |
| 分类 ID | `categoryId` | `category_id` BIGINT UNSIGNED NOT NULL | `private Long categoryId;` | `categoryId: number` | 必须指向**二级分类**（Service 层校验） |
| 描述 | `description` | `description` TEXT NULL | `private String description;` | `description: string \| null` | 富文本或纯文本，前端展示需 XSS 清洗 |
| 图片 URL | `imageUrl` | `image_url` VARCHAR(512) NULL | `private String imageUrl;` | `imageUrl: string \| null` | MVP 单图；WebP 格式，≤ 5 MB |
| 是否上架 | `onShelf` | `on_shelf` TINYINT(1) NOT NULL DEFAULT 1 | `private Boolean onShelf;` | `onShelf: boolean` | 1=上架（前台可见）；0=下架（前台不可见） |
| 创建时间 | `createdAt` | `created_at` DATETIME | `private LocalDateTime createdAt;` | `createdAt: string` | — |
| 更新时间 | `updatedAt` | `updated_at` DATETIME ON UPDATE | `private LocalDateTime updatedAt;` | `updatedAt: string` | — |

**禁用命名**：`item`、`goods`、`sku`（MVP 不做多规格）、`available`、`enabled`（用 `on_shelf`）

---

### 1.4 购物车项 CartItem

| 中文概念 | 统一英文标识符 | 数据库 | Java | Vue TS | 业务含义与边界 |
|----------|---------------|--------|------|--------|----------------|
| 购物车项 | `CartItem` | 表：`cart_items` | 实体：`CartItem` | 接口：`CartItem` | 用户 × 商品 的唯一行；**不做价格快照**（购物车用当前价） |
| 购物车项 ID | `id` | `id` BIGINT UNSIGNED PK | `private Long id;` | `id: number` | 自增主键 |
| 用户 ID | `userId` | `user_id` BIGINT UNSIGNED NOT NULL FK → users.id | `private Long userId;` | `userId: number` | 归属权校验基础 |
| 商品 ID | `productId` | `product_id` BIGINT UNSIGNED NOT NULL FK → products.id | `private Long productId;` | `productId: number` | — |
| 数量 | `quantity` | `quantity` INT UNSIGNED NOT NULL DEFAULT 1 | `private Integer quantity;` | `quantity: number` | 加购时叠加；不得超过 `products.stock` |

**唯一约束**：`UNIQUE(user_id, product_id)` — 同一商品重复加购只累加数量

**禁用命名**：`cart_row`、`shopping_cart_item`、`count`（用 `quantity`）

---

### 1.5 订单 Order

| 中文概念 | 统一英文标识符 | 数据库 | Java | Vue TS | 业务含义与边界 |
|----------|---------------|--------|------|--------|----------------|
| 订单 | `Order` | 表：`orders` | 实体：`Order` | 接口：`Order` | 一次结算 = 一张订单；由购物车中勾选的若干行生成 |
| 订单 ID | `id` | `id` BIGINT UNSIGNED PK | `private Long id;` | `id: number` | 自增主键；对外展示可用雪花 ID 或 NO 前缀 |
| 订单号（业务） | `orderNo` | `order_no` VARCHAR(32) UNIQUE | `private String orderNo;` | `orderNo: string` | 对用户展示的可读编号，如 `SO20261029143052001` |
| 用户 ID | `userId` | `user_id` BIGINT UNSIGNED NOT NULL FK → users.id | `private Long userId;` | `userId: number` | 归属权校验 |
| 订单状态 | `status` | `status` TINYINT NOT NULL | `private OrderStatus status;` | `status: OrderStatus` | 枚举 `OrderStatus`（5 态） |
| 总金额 | `totalAmount` | `total_amount` DECIMAL(10,2) NOT NULL | `private BigDecimal totalAmount;` | `totalAmount: number` | 服务端重算，前端传值不采信 |
| 收货地址 | `address` | `address` VARCHAR(512) NOT NULL | `private String address;` | `address: string` | MVP 纯文本框，不做地址管理 |
| 备注 | `remark` | `remark` VARCHAR(256) NULL | `private String remark;` | `remark: string \| null` | 可选，用户下单时填写 |
| 付款时间 | `paidAt` | `paid_at` DATETIME NULL | `private LocalDateTime paidAt;` | `paidAt: string \| null` | 模拟付款时写入 |
| 发货时间 | `shippedAt` | `shipped_at` DATETIME NULL | `private LocalDateTime shippedAt;` | `shippedAt: string \| null` | 管理员标记发货时写入 |
| 完成时间 | `completedAt` | `completed_at` DATETIME NULL | `private LocalDateTime completedAt;` | `completedAt: string \| null` | 用户确认收货时写入 |
| 取消时间 | `cancelledAt` | `cancelled_at` DATETIME NULL | `private LocalDateTime cancelledAt;` | `cancelledAt: string \| null` | 订单取消时写入 |
| 创建时间 | `createdAt` | `created_at` DATETIME NOT NULL | `private LocalDateTime createdAt;` | `createdAt: string` | 下单即写入 |

**禁用命名**：`purchase`、`checkout`、`amount`（用 `total_amount` 避免歧义）、`status_code`（用 `status`）

---

### 1.6 订单明细 OrderItem

| 中文概念 | 统一英文标识符 | 数据库 | Java | Vue TS | 业务含义与边界 |
|----------|---------------|--------|------|--------|----------------|
| 订单明细项 | `OrderItem` | 表：`order_items` | 实体：`OrderItem` | 接口：`OrderItem` | 订单 ↔ 商品 的一对多；**存下单当时的价格快照** |
| 明细 ID | `id` | `id` BIGINT UNSIGNED PK | `private Long id;` | `id: number` | 自增主键 |
| 订单 ID | `orderId` | `order_id` BIGINT UNSIGNED NOT NULL FK → orders.id | `private Long orderId;` | `orderId: number` | — |
| 商品 ID | `productId` | `product_id` BIGINT UNSIGNED NOT NULL FK → products.id | `private Long productId;` | `productId: number` | 关联当前商品（历史数据完整性） |
| **商品名称（快照）** | `productName` | `product_name` VARCHAR(128) NOT NULL | `private String productName;` | `productName: string` | **下单当时的名称**，后续改名不影响历史 |
| **单价（快照）** | `unitPrice` | `unit_price` DECIMAL(10,2) NOT NULL | `private BigDecimal unitPrice;` | `unitPrice: number` | **下单当时的单价**，后续改价不影响历史 |
| 数量 | `quantity` | `quantity` INT UNSIGNED NOT NULL | `private Integer quantity;` | `quantity: number` | 下单数量 |
| 小计 | `subtotal` | `subtotal` DECIMAL(10,2) NOT NULL | `private BigDecimal subtotal;` | `subtotal: number` | `unit_price × quantity`，事务内写入 |

**关键业务规则**：`product_name` + `unit_price` 是**价格快照**，必须在下单事务内从 `products` 表 SELECT 后写入，**禁止**事后通过 JOIN 实时计算。

**禁用命名**：`order_detail`、`order_line`、`snapshot_price`（直接用 `unit_price` + `product_name` 即可，无需附加 `_snapshot` 后缀）

---

### 1.7 评价 Evaluation

| 中文概念 | 统一英文标识符 | 数据库 | Java | Vue TS | 业务含义与边界 |
|----------|---------------|--------|------|--------|----------------|
| 评价 | `Evaluation` | 表：`evaluations` | 实体：`Evaluation` | 接口：`Evaluation` | 用户对已完成订单中商品的评分 + 文字 |
| 评价 ID | `id` | `id` BIGINT UNSIGNED PK | `private Long id;` | `id: number` | 自增主键 |
| 订单 ID | `orderId` | `order_id` BIGINT UNSIGNED NOT NULL FK → orders.id | `private Long orderId;` | `orderId: number` | — |
| 商品 ID | `productId` | `product_id` BIGINT UNSIGNED NOT NULL FK → products.id | `private Long productId;` | `productId: number` | — |
| 用户 ID | `userId` | `user_id` BIGINT UNSIGNED NOT NULL FK → users.id | `private Long userId;` | `userId: number` | 评价归属；评价人必须 = 订单所属人 |
| 评分 | `rating` | `rating` TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5) | `private Integer rating;` | `rating: number` | 1~5 星，必填 |
| 评论内容 | `content` | `content` VARCHAR(500) NULL | `private String content;` | `content: string \| null` | 选填纯文字；**不支持图片**；前端展示需 XSS 清洗 |
| 创建时间 | `createdAt` | `created_at` DATETIME NOT NULL | `private LocalDateTime createdAt;` | `createdAt: string` | — |

**唯一约束**：`UNIQUE(order_id, product_id, user_id)` — 同一订单同一商品同一用户只能评一次

**禁用命名**：`review`、`comment`（会与商品评论混淆；作业明确叫"评价"）、`rate`（作动词，不作名词）、`image_url`（MVP 不做图片）

---

## 2. 核心领域枚举（Domain Enums）

> 枚举统一用 `TINYINT` 存数据库，Java / TypeScript 用 `enum` 或联合字符串类型

### 2.1 用户角色 UserRole

| 中文 | 统一英文标识符 | DB 值 | Java 枚举 | Vue TS 类型 | 业务含义 |
|------|---------------|-------|-----------|-------------|----------|
| 游客 | `VISITOR` | —（未登录无 role） | `UserRole.VISITOR` | `'VISITOR'` | 仅浏览级权限，不入库 |
| 普通用户 | `NORMAL_USER` | 1 | `UserRole.NORMAL_USER` | `'NORMAL_USER'` | 注册默认值 |
| 管理员 | `ADMIN` | 2 | `UserRole.ADMIN` | `'ADMIN'` | DB 脚本预置，禁止自助注册 |

**禁用命名**：`CUSTOMER`、`BUYER`、`MEMBER`、`SUPER_ADMIN`（只有一种管理员）

---

### 2.2 订单状态 OrderStatus（**5 态固定**）

| 中文 | 统一英文标识符 | DB 值 | Java 枚举 | Vue TS 类型 | 状态流转规则 |
|------|---------------|-------|-----------|-------------|-------------|
| 待支付 | `UNPAID` | 0 | `OrderStatus.UNPAID` | `'UNPAID'` | 订单刚创建的初始状态；**可取消** |
| 已支付 | `PAID` | 1 | `OrderStatus.PAID` | `'PAID'` | 用户模拟付款后进入；等待管理员发货 |
| 已发货 | `SHIPPED` | 2 | `OrderStatus.SHIPPED` | `'SHIPPED'` | 管理员标记发货后进入；等待用户确认收货 |
| 已完成 | `COMPLETED` | 3 | `OrderStatus.COMPLETED` | `'COMPLETED'` | 用户确认收货后进入；**终态**；可评价 |
| 已取消 | `CANCELLED` | 4 | `OrderStatus.CANCELLED` | `'CANCELLED'` | 用户取消（仅限 UNPAID）后进入；**终态**；库存回补 |

**合法状态流转**：

```
UNPAID ──付款──► PAID ──发货──► SHIPPED ──确认收货──► COMPLETED（终态）
   │
   └──取消──► CANCELLED（终态）
```

**禁用命名**：`PENDING`（不够明确）、`DELIVERED`（用 `SHIPPED`）、`FINISHED`（用 `COMPLETED`）、`REFUNDED`（MVP 不做退款）

---

### 2.3 商品上架状态 OnShelfFlag

| 中文 | 统一英文标识符 | DB 值 | Java | Vue TS | 业务含义 |
|------|---------------|-------|------|--------|----------|
| 上架 | `ON_SHELF` | 1 | `Boolean.TRUE` / `onShelf = true` | `true` | 前台可见、可加购 |
| 下架 | `OFF_SHELF` | 0 | `Boolean.FALSE` / `onShelf = false` | `false` | 前台不可见；后台仍可见 |

**说明**：MVP 阶段用 `TINYINT(1)` + 布尔语义即可，无需单独建枚举类（避免过度设计）。但代码中绝不允许出现 `"status = 1"` 之类的魔法数字，必须用 `ProductStatus.ON_SHELF` 或常量。

---

### 2.4 统一响应错误码 ApiCode

| code | 英文标识符 | HTTP 状态 | 业务含义 |
|------|-----------|----------|----------|
| 0 | `SUCCESS` | 200 | 成功 |
| 400 | `BAD_REQUEST` | 400 | 业务不允许：库存不足 / 状态越权 / 下架商品加购 |
| 401 | `UNAUTHORIZED` | 401 | 未登录 / token 失效 |
| 403 | `FORBIDDEN` | 403 | 已登录但无权限（角色或归属权拦截） |
| 404 | `NOT_FOUND` | 404 | 资源不存在 |
| 409 | `CONFLICT` | 409 | 业务冲突：重复用户名 / 重复评价 |
| 413 | `PAYLOAD_TOO_LARGE` | 413 | 图片单图 > 5 MB |
| 500 | `INTERNAL_ERROR` | 500 | 服务器内部异常（事务回滚等） |

---

## 3. 业务操作动词（Domain Actions）

> 动词用于 Service 方法名、Controller 路由、API endpoint、前端 action/function 名。**同一项操作在三端命名必须一致**

### 3.1 身份域

| 中文操作 | 统一英文动词 | Java Service 方法 | API Endpoint | Vue 方法名 | 说明 |
|----------|-------------|-------------------|--------------|-----------|------|
| 注册 | `register` | `userService.register(RegisterDTO)` | `POST /api/auth/register` | `register()` | 只产生 `NORMAL_USER` |
| 登录 | `login` | `authService.login(LoginDTO)` | `POST /api/auth/login` | `login()` | 返回 token |
| 登出 | `logout` | `authService.logout(String token)` | `POST /api/auth/logout` | `logout()` | 后端使 token 失效 |
| 获取当前用户 | `getCurrentUser` | `userService.getCurrentUser(Long userId)` | `GET /api/users/me` | `fetchCurrentUser()` | — |

**禁用命名**：`signIn` / `signUp`（用 `login` / `register`，保持 REST 风格统一）、`logIn` / `logOut`（大小写不统一）

---

### 3.2 分类域

| 中文操作 | 统一英文动词 | Java Service 方法 | API Endpoint | Vue 方法名 |
|----------|-------------|-------------------|--------------|-----------|
| 列出一级分类 | `listTopCategories` | `categoryService.listTopCategories()` | `GET /api/categories/top` | `fetchTopCategories()` |
| 列出二级分类 | `listSubCategories` | `categoryService.listSubCategories(Long parentId)` | `GET /api/categories/{parentId}/sub` | `fetchSubCategories()` |

---

### 3.3 商品域

| 中文操作 | 统一英文动词 | Java Service 方法 | API Endpoint | Vue 方法名 | 说明 |
|----------|-------------|-------------------|--------------|-----------|------|
| 列出商品（前台，上架） | `listOnShelfProducts` | `productService.listOnShelfProducts(Pageable)` | `GET /api/products` | `fetchProducts()` | SQL 自动 `WHERE on_shelf = 1` |
| 列出推荐商品 | `listRecommendedProducts` | `productService.listRecommendedProducts()` | `GET /api/products/recommended` | `fetchRecommendedProducts()` | 首页 3~8 件 |
| 按分类列商品 | `listProductsByCategory` | `productService.listProductsByCategory(Long categoryId, Pageable)` | `GET /api/categories/{categoryId}/products` | `fetchProductsByCategory()` | 二级分类下 |
| 商品详情 | `getProductDetail` | `productService.getProductDetail(Long productId)` | `GET /api/products/{productId}` | `fetchProductDetail()` | 下架商品返回 404 + 明确提示 |
| 新增商品 | `createProduct` | `adminProductService.createProduct(CreateProductDTO)` | `POST /api/admin/products` | `createProduct()` | 管理员 |
| 编辑商品 | `updateProduct` | `adminProductService.updateProduct(Long id, UpdateProductDTO)` | `PUT /api/admin/products/{id}` | `updateProduct()` | 管理员 |
| 上架 / 下架 | `changeOnShelf` | `adminProductService.changeOnShelf(Long id, Boolean onShelf)` | `PATCH /api/admin/products/{id}/on-shelf` | `toggleOnShelf()` | 管理员；**方法名带语义**，不用模糊的 `update()` |

**禁用命名**：`getItem`、`fetchGoods`、`setAvailable` / `setStatus`（用 `changeOnShelf`）、`addProduct`（REST 用 `create`）

---

### 3.4 购物车域

| 中文操作 | 统一英文动词 | Java Service 方法 | API Endpoint | Vue 方法名 | 说明 |
|----------|-------------|-------------------|--------------|-----------|------|
| 加购 | `addCartItem` | `cartService.addCartItem(Long userId, Long productId, Integer qty)` | `POST /api/cart/items` | `addToCart()` | 重复加购自动叠加数量；校验上架 + 库存 |
| 列出购物车 | `listCartItems` | `cartService.listCartItems(Long userId)` | `GET /api/cart/items` | `fetchCartItems()` | 实时计算当前小计和合计 |
| 修改数量 | `updateCartItemQuantity` | `cartService.updateCartItemQuantity(Long userId, Long cartItemId, Integer qty)` | `PUT /api/cart/items/{id}` | `updateCartItemQty()` | 不得超过库存 |
| 删除购物车项 | `removeCartItem` | `cartService.removeCartItem(Long userId, Long cartItemId)` | `DELETE /api/cart/items/{id}` | `removeFromCart()` | — |
| 清空购物车 | `clearCart` | `cartService.clearCart(Long userId)` | `DELETE /api/cart` | `clearCart()` | — |

**禁用命名**：`addToShoppingCart`（太长，用 `addToCart` / `addCartItem`）、`modifyQty`（用完整 `updateCartItemQuantity`）

---

### 3.5 订单域

| 中文操作 | 统一英文动词 | Java Service 方法 | API Endpoint | Vue 方法名 | 说明 |
|----------|-------------|-------------------|--------------|-----------|------|
| 创建订单（结算） | `createOrder` | `orderService.createOrder(Long userId, CreateOrderDTO)` | `POST /api/orders` | `submitOrder()` | **事务内**：扣库存 + 写 orders + 写 order_items；自动剔除下架购物车项 |
| 模拟付款 | `simulatePay` | `orderService.simulatePay(Long userId, Long orderId)` | `POST /api/orders/{id}/pay` | `payOrder()` | UNPAID → PAID；不接真实支付 |
| 列出自己订单 | `listMyOrders` | `orderService.listMyOrders(Long userId, Pageable)` | `GET /api/orders` | `fetchMyOrders()` | 按创建时间倒序 |
| 订单详情 | `getOrderDetail` | `orderService.getOrderDetail(Long userId, Long orderId)` | `GET /api/orders/{id}` | `fetchOrderDetail()` | 归属权校验 |
| 取消订单 | `cancelOrder` | `orderService.cancelOrder(Long userId, Long orderId)` | `POST /api/orders/{id}/cancel` | `cancelOrder()` | 仅限 UNPAID；**库存回补** |
| 确认收货 | `confirmReceipt` | `orderService.confirmReceipt(Long userId, Long orderId)` | `POST /api/orders/{id}/confirm-receipt` | `confirmReceipt()` | SHIPPED → COMPLETED |
| 列出全部订单（后台） | `listAllOrders` | `adminOrderService.listAllOrders(OrderStatus status, Pageable)` | `GET /api/admin/orders` | `fetchAllOrders()` | 管理员；可按状态筛选 |
| 标记发货 | `markShipped` | `adminOrderService.markShipped(Long orderId)` | `POST /api/admin/orders/{id}/ship` | `markOrderShipped()` | PAID → SHIPPED |

**禁用命名**：`placeOrder`（用 `createOrder` 与 REST 对齐）、`buy` / `purchase`、`confirmPay`（用 `simulatePay` 强调模拟性质）、`deliver`（用 `markShipped`）、`receiveOrder`（用 `confirmReceipt`）

---

### 3.6 评价域

| 中文操作 | 统一英文动词 | Java Service 方法 | API Endpoint | Vue 方法名 | 说明 |
|----------|-------------|-------------------|--------------|-----------|------|
| 提交评价 | `submitEvaluation` | `evaluationService.submitEvaluation(Long userId, SubmitEvaluationDTO)` | `POST /api/evaluations` | `submitEvaluation()` | 仅限 COMPLETED 订单；唯一键防重复 |
| 列出商品评价 | `listEvaluationsByProduct` | `evaluationService.listEvaluationsByProduct(Long productId)` | `GET /api/products/{productId}/evaluations` | `fetchProductEvaluations()` | 商品详情页使用 |

**禁用命名**：`writeReview` / `addReview`（用 `submitEvaluation`）、`rateProduct`（评价不止评分还有文字）

---

## 4. 命名约定总结

### 4.1 强制定死的命名规则（红线）

| 规则 | 说明 |
|------|------|
| ✅ 表名一律复数 | `users`、`products`、`order_items`（不是 `order_item`） |
| ✅ 外键字段一律 `xxx_id` | 例：`user_id`、`product_id`（**禁止** `fk_user`、`userid`、`userId` 混合） |
| ✅ 布尔字段一律 `TINYINT(1)` | 0 = false / 1 = true；Java 用 `Boolean`，不用 `boolean`（支持 NULL） |
| ✅ 时间戳字段一律 `_at` 后缀 | `created_at`、`paid_at`、`shipped_at`（**禁止** `create_time`、`gmt_create`） |
| ✅ 金额一律 `DECIMAL(10,2)` + Java `BigDecimal` | **禁止**用 `FLOAT` / `DOUBLE`（精度丢失） |
| ✅ 枚举值一律 `UPPER_SNAKE_CASE` | Java enum、TypeScript union、DB CHECK 注释必须一致 |
| ✅ REST API 用资源名 + HTTP 动词 | `POST /api/orders`（不是 `POST /api/createOrder`） |

### 4.2 禁用命名黑名单

| 禁用词 | 原因 | 统一替换为 |
|--------|------|-----------|
| `account` / `member` / `customer` / `buyer` | 与 PRD 定义的 `User` 混淆 | `User` |
| `item` / `goods` / `sku` | `sku` 暗示多规格（MVP 禁止）；`item` 太泛 | `Product` |
| `review` / `comment` | 作业明确叫"评价"，避免与商品评论混淆 | `Evaluation` |
| `purchase` / `checkout` | 不够准确；`checkout` 语义是"离开柜台" | `Order`（创建动作用 `createOrder`） |
| `signIn` / `signUp` | 与 REST 风格的 `login` / `register` 不统一 | `login` / `register` |
| `setStatus` / `setAvailable` | 语义模糊，不知道设什么状态 | 具体动词：`changeOnShelf` |
| `getItemById` / `fetchGoods` | 不统一 | `getProductDetail` |
| `finalized` / `finished` | 状态机中用 `COMPLETED` | `COMPLETED` |
| `pending` | 不够明确，不知道"待"什么 | `UNPAID` |
| `onSale` | 与实体类名 `Product` 冲突（语义重叠） | `onShelf`（强调货架/展示） |
| `gmt_create` / `gmt_modified` | MyBatis-Plus 风格，作业未指定该框架 | `created_at` / `updated_at` |

### 4.3 AI 命名幻觉提醒

> 在后续任何 AI 辅助开发环节（DDL、实体、API 路由、Vue 组件）中，如果 AI 输出了以下词，**必须立即纠正**：
> - ❌ 把 `User` 写成 `Customer` / `Buyer`
> - ❌ 把 `Evaluation` 写成 `Review` / `Comment`
> - ❌ 把 `OrderItem.unit_price` 写成 `snapshot_price`（多余后缀）
> - ❌ 把 `on_shelf` 写成 `status`（语义丢失）
> - ❌ 把 `UNPAID` 写成 `PENDING_PAYMENT`
> - ❌ 把 `createOrder` 写成 `placeOrder` / `buyNow`

---

*—— END OF GLOSSARY ——*
