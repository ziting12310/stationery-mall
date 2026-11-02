# 数据库设计文档（MySQL 8 · utf8mb4 · InnoDB）

> 版本：v1.0 · 日期：2026-10-29
> 依赖文档：[PRD.md](../01-requirements/PRD.md) · [GLOSSARY.md](../01-requirements/GLOSSARY.md) · [architecture.md](./architecture.md)
> **表数量**：7 张业务表 + 1 张辅助表（revoked\_tokens，用于登出使旧 token 失效）

***

## 1. ER 图（Mermaid erDiagram）

```MariaDB&#x20;SQL
erDiagram
    %% ===== 业务表 =====

    USERS {
        BIGINT_UNSIGNED id PK
        VARCHAR username UK
        VARCHAR password
        TINYINT role
        DATETIME created_at
        DATETIME updated_at
    }

    CATEGORIES {
        BIGINT_UNSIGNED id PK
        VARCHAR name
        BIGINT_UNSIGNED parent_id FK_NULL
        INT sort_order
    }

    PRODUCTS {
        BIGINT_UNSIGNED id PK
        VARCHAR name
        DECIMAL price
        INT_UNSIGNED stock
        BIGINT_UNSIGNED category_id FK
        TEXT description
        VARCHAR image_url
        TINYINT on_shelf
        DATETIME created_at
        DATETIME updated_at
    }

    CART_ITEMS {
        BIGINT_UNSIGNED id PK
        BIGINT_UNSIGNED user_id FK
        BIGINT_UNSIGNED product_id FK
        INT_UNSIGNED quantity
    }

    ORDERS {
        BIGINT_UNSIGNED id PK
        VARCHAR order_no UK
        BIGINT_UNSIGNED user_id FK
        TINYINT status
        DECIMAL total_amount
        VARCHAR address
        VARCHAR remark
        DATETIME created_at
        DATETIME paid_at
        DATETIME shipped_at
        DATETIME completed_at
        DATETIME cancelled_at
    }

    ORDER_ITEMS {
        BIGINT_UNSIGNED id PK
        BIGINT_UNSIGNED order_id FK
        BIGINT_UNSIGNED product_id FK
        VARCHAR product_name
        DECIMAL unit_price
        INT_UNSIGNED quantity
        DECIMAL subtotal
    }

    EVALUATIONS {
        BIGINT_UNSIGNED id PK
        BIGINT_UNSIGNED order_id FK
        BIGINT_UNSIGNED product_id FK
        BIGINT_UNSIGNED user_id FK
        TINYINT rating
        VARCHAR content
        DATETIME created_at
    }

    %% ===== 辅助表 =====
    REVOKED_TOKENS {
        VARCHAR jti PK
        DATETIME revoked_at
        DATETIME expires_at
    }

    %% ===== 关系 =====

    CATEGORIES ||--o{ CATEGORIES : "自关联 parent_id（一级/二级）"

    USERS ||--o{ CART_ITEMS : "1:N 购物车项"
    USERS ||--o{ ORDERS : "1:N 订单"
    USERS ||--o{ EVALUATIONS : "1:N 评价"

    CATEGORIES ||--o{ PRODUCTS : "二级分类挂商品"

    PRODUCTS ||--o{ CART_ITEMS : "1:N"
    PRODUCTS ||--o{ ORDER_ITEMS : "1:N（历史引用，不依赖 FK 业务逻辑）"
    PRODUCTS ||--o{ EVALUATIONS : "1:N"

    ORDERS ||--o{ ORDER_ITEMS : "1:N 订单明细"
    ORDERS ||--o{ EVALUATIONS : "1:N"

    %% ===== 外键注释（ER 语法不支持）=====
    %% order_items.product_name / unit_price 是下单快照字段，不做实时 JOIN
    %% categories.parent_id NULL = 一级分类，非 NULL = 二级分类
    %% cart_items UNIQUE(user_id, product_id) —— 同一商品重复加购只叠加
    %% evaluations UNIQUE(order_id, product_id, user_id) —— 同一订单同一商品只能评一次
```

### 1.1 实体关系说明

| 关系           | 基数      | 关键说明                                          |
| ------------ | ------- | --------------------------------------------- |
| 分类 → 分类（自关联） | 1:N（自指） | `parent_id NULL` = 一级分类；`parent_id` 非空 = 二级分类 |
| 用户 → 购物车项    | 1:N     | 同一用户可有多行购物车项                                  |
| 用户 → 订单      | 1:N     | 同一用户可有多张订单                                    |
| 用户 → 评价      | 1:N     | 同一用户可写多条评价                                    |
| 分类 → 商品      | 1:N     | **商品只能挂二级分类**（Service 校验）                     |
| 商品 → 购物车项    | 1:N     | —                                             |
| 商品 → 订单明细    | 1:N     | FK 仅用于数据完整性；业务语义上 order\_items 存的是**快照**      |
| 商品 → 评价      | 1:N     | —                                             |
| 订单 → 订单明细    | 1:N     | 订单明细存**快照**，后续商品改名/改价不影响历史                    |
| 订单 → 评价      | 1:N     | 同一订单中的每件商品可单独评价                               |

***

## 2. 数据表详细定义

> 字符集统一 `utf8mb4`，引擎统一 `InnoDB`，字段名 `snake_case`，MyBatis `map-underscore-to-camel-case: true` 自动映射

### 2.1 users（用户表）

**中文说明**：系统认证主体。包含普通用户 + 管理员；**游客无 DB 记录**（VISITOR 是未登录态概念）

| 字段           | 类型              |   PK   | NOT NULL |   唯一   | 注释                                   | 默认值                          |
| ------------ | --------------- | :----: | :------: | :----: | ------------------------------------ | ---------------------------- |
| `id`         | BIGINT UNSIGNED |    ✅   |     ✅    |    ✅   | 自增主键                                 | AUTO\_INCREMENT              |
| `username`   | VARCHAR(32)     | <br /> |     ✅    |    ✅   | 登录用户名，全局唯一                           | —                            |
| `password`   | VARCHAR(128)    | <br /> |     ✅    | <br /> | BCrypt 哈希值，**永不存明文**                 | —                            |
| `role`       | TINYINT         | <br /> |     ✅    | <br /> | 1=NORMAL\_USER, 2=ADMIN（VISITOR 无记录） | 1                            |
| `created_at` | DATETIME        | <br /> |     ✅    | <br /> | 注册时间                                 | CURRENT\_TIMESTAMP           |
| `updated_at` | DATETIME        | <br /> |  <br />  | <br /> | 资料更新时间                               | ON UPDATE CURRENT\_TIMESTAMP |

**role 枚举值**：

| DB 值 | 枚举常量         | 含义           |
| :--: | ------------ | ------------ |
|   —  | VISITOR      | 游客态，DB 无记录   |
|   1  | NORMAL\_USER | 普通用户（注册默认值）  |
|   2  | ADMIN        | 管理员（DB 脚本预置） |

***

### 2.2 categories（商品分类表 · 两级自关联）

**中文说明**：严格两级分类结构；`parent_id IS NULL` = 一级分类；`parent_id IS NOT NULL` = 二级分类。**商品只能挂在二级分类下**

| 字段           | 类型              |   PK   | NOT NULL |   唯一   | 注释                               | 默认值             |
| ------------ | --------------- | :----: | :------: | :----: | -------------------------------- | --------------- |
| `id`         | BIGINT UNSIGNED |    ✅   |     ✅    |    ✅   | 自增主键                             | AUTO\_INCREMENT |
| `name`       | VARCHAR(32)     | <br /> |     ✅    | <br /> | 分类名                              | —               |
| `parent_id`  | BIGINT UNSIGNED | <br /> |  <br />  | <br /> | 父分类 ID；NULL = 一级分类；非 NULL = 二级分类 | NULL            |
| `sort_order` | INT             | <br /> |     ✅    | <br /> | 同级排序，小数字靠前                       | 0               |

**CHECK 约束建议**（InnoDB 可选）：

```sql
-- 二级分类的 parent_id 必须指向存在的一级分类
-- （由 Service 层保证；DB 外键约束可选）
```

***

### 2.3 products（商品表）

**中文说明**：MVP 单规格商品（一个价格一个库存）；上架/下架状态；关联二级分类

| 字段            | 类型              |   PK   | NOT NULL |   唯一   | 注释                        | 默认值                          |
| ------------- | --------------- | :----: | :------: | :----: | ------------------------- | ---------------------------- |
| `id`          | BIGINT UNSIGNED |    ✅   |     ✅    |    ✅   | 自增主键                      | AUTO\_INCREMENT              |
| `name`        | VARCHAR(128)    | <br /> |     ✅    | <br /> | 商品展示名                     | —                            |
| `price`       | DECIMAL(10,2)   | <br /> |     ✅    | <br /> | 当前单价，DECIMAL 避免浮点精度丢失     | —                            |
| `stock`       | INT UNSIGNED    | <br /> |     ✅    | <br /> | 库存数量，**≥ 0，不允许负数**        | 0                            |
| `category_id` | BIGINT UNSIGNED | <br /> |     ✅    | <br /> | 必须指向**二级分类**（Service 层校验） | —                            |
| `description` | TEXT            | <br /> |  <br />  | <br /> | 商品描述（纯文本，前端展示需 XSS 清洗）    | NULL                         |
| `image_url`   | VARCHAR(512)    | <br /> |  <br />  | <br /> | 单张图片 URL；MVP 单图           | NULL                         |
| `on_shelf`    | TINYINT(1)      | <br /> |     ✅    | <br /> | 1=上架（前台可见）；0=下架（前台不可见）    | 1                            |
| `created_at`  | DATETIME        | <br /> |     ✅    | <br /> | —                         | CURRENT\_TIMESTAMP           |
| `updated_at`  | DATETIME        | <br /> |  <br />  | <br /> | —                         | ON UPDATE CURRENT\_TIMESTAMP |

***

### 2.4 cart\_items（购物车项）

**中文说明**：用户 × 商品 的唯一行；同一商品重复加购只叠加数量；价格用**当前** price（非快照）

| 字段           | 类型              |   PK   | NOT NULL |   唯一   | 注释                     | 默认值             |
| ------------ | --------------- | :----: | :------: | :----: | ---------------------- | --------------- |
| `id`         | BIGINT UNSIGNED |    ✅   |     ✅    |    ✅   | 自增主键                   | AUTO\_INCREMENT |
| `user_id`    | BIGINT UNSIGNED | <br /> |     ✅    |   联合   | 外键 → users.id          | —               |
| `product_id` | BIGINT UNSIGNED | <br /> |     ✅    |   联合   | 外键 → products.id       | —               |
| `quantity`   | INT UNSIGNED    | <br /> |     ✅    | <br /> | ≥ 1；不得超过 product.stock | 1               |

**唯一约束**：`UNIQUE(user_id, product_id)` — 同一用户对同一商品只有一行

***

### 2.5 orders（订单表 · 主单）

**中文说明**：一次勾选结算 = 一张订单；**5 状态固定**；下单即生成并扣库存

| 字段             | 类型              |   PK   | NOT NULL |   唯一   | 注释                                                    | 默认值                |
| -------------- | --------------- | :----: | :------: | :----: | ----------------------------------------------------- | ------------------ |
| `id`           | BIGINT UNSIGNED |    ✅   |     ✅    |    ✅   | 自增主键                                                  | AUTO\_INCREMENT    |
| `order_no`     | VARCHAR(32)     | <br /> |     ✅    |    ✅   | 业务编号，对用户展示（如 `SO20261029143052001`）                   | —                  |
| `user_id`      | BIGINT UNSIGNED | <br /> |     ✅    | <br /> | 外键 → users.id；归属权校验基础                                 | —                  |
| `status`       | TINYINT         | <br /> |     ✅    | <br /> | 0=UNPAID, 1=PAID, 2=SHIPPED, 3=COMPLETED, 4=CANCELLED | —                  |
| `total_amount` | DECIMAL(10,2)   | <br /> |     ✅    | <br /> | **服务端重算**；前端传来的金额不采信                                  | —                  |
| `address`      | VARCHAR(512)    | <br /> |     ✅    | <br /> | MVP 收货地址纯文本框                                          | —                  |
| `remark`       | VARCHAR(256)    | <br /> |  <br />  | <br /> | 用户下单备注选填                                              | NULL               |
| `created_at`   | DATETIME        | <br /> |     ✅    | <br /> | 订单创建时间（扣库存事务内写入）                                      | CURRENT\_TIMESTAMP |
| `paid_at`      | DATETIME        | <br /> |  <br />  | <br /> | 模拟付款时间（PAID 时写入）                                      | NULL               |
| `shipped_at`   | DATETIME        | <br /> |  <br />  | <br /> | 管理员标记发货时间                                             | NULL               |
| `completed_at` | DATETIME        | <br /> |  <br />  | <br /> | 用户确认收货时间                                              | NULL               |
| `cancelled_at` | DATETIME        | <br /> |  <br />  | <br /> | 用户取消时间                                                | NULL               |

**order\_no 生成规则**（Java Service 层）：

```java
"SO" + LocalDateTime.now().format("yyyyMMddHHmmss") + String.format("%03d", orderId)
```

**status 枚举值**（5 状态固定）：

| DB 值 | 枚举常量      | 含义  | 可流转至           |  终态 |
| :--: | --------- | --- | -------------- | :-: |
|   0  | UNPAID    | 待支付 | PAID、CANCELLED |  ❌  |
|   1  | PAID      | 已支付 | SHIPPED        |  ❌  |
|   2  | SHIPPED   | 已发货 | COMPLETED      |  ❌  |
|   3  | COMPLETED | 已完成 | —              |  ✅  |
|   4  | CANCELLED | 已取消 | —              |  ✅  |

***

### 2.6 order\_items（订单明细表 · 快照）

**中文说明**：orders ↔ products 的一对多关联；**存下单当时的商品名称和单价**（价格快照），后续改商品信息不影响历史

| 字段             | 类型              |   PK   | NOT NULL |   唯一   | 注释                            | 默认值             |
| -------------- | --------------- | :----: | :------: | :----: | ----------------------------- | --------------- |
| `id`           | BIGINT UNSIGNED |    ✅   |     ✅    |    ✅   | 自增主键                          | AUTO\_INCREMENT |
| `order_id`     | BIGINT UNSIGNED | <br /> |     ✅    | <br /> | 外键 → orders.id                | —               |
| `product_id`   | BIGINT UNSIGNED | <br /> |     ✅    | <br /> | 外键 → products.id（数据完整性用）      | —               |
| `product_name` | VARCHAR(128)    | <br /> |     ✅    | <br /> | **下单当时**的商品名称（快照）             | —               |
| `unit_price`   | DECIMAL(10,2)   | <br /> |     ✅    | <br /> | **下单当时**的商品单价（快照）             | —               |
| `quantity`     | INT UNSIGNED    | <br /> |     ✅    | <br /> | 下单数量，≥ 1                      | —               |
| `subtotal`     | DECIMAL(10,2)   | <br /> |     ✅    | <br /> | `unit_price × quantity`，事务内写入 | —               |

**关键**：`product_name` + `unit_price` 是**快照字段**，在下单事务内先 SELECT 商品再写入；后续 `products` 表改名/改价**绝不影响**历史 order\_items。

***

### 2.7 evaluations（评价表）

**中文说明**：用户对已完成订单中的商品写评价；**同一订单 + 商品 + 用户 唯一约束**防重复评价

| 字段           | 类型              |   PK   | NOT NULL |   唯一   | 注释                         | 默认值                |
| ------------ | --------------- | :----: | :------: | :----: | -------------------------- | ------------------ |
| `id`         | BIGINT UNSIGNED |    ✅   |     ✅    |    ✅   | 自增主键                       | AUTO\_INCREMENT    |
| `order_id`   | BIGINT UNSIGNED | <br /> |     ✅    |   三元   | 外键 → orders.id             | —                  |
| `product_id` | BIGINT UNSIGNED | <br /> |     ✅    |   三元   | 外键 → products.id           | —                  |
| `user_id`    | BIGINT UNSIGNED | <br /> |     ✅    |   三元   | 外键 → users.id（必须 = 订单所属用户） | —                  |
| `rating`     | TINYINT         | <br /> |     ✅    | <br /> | 1\~5 星整数                   | —                  |
| `content`    | VARCHAR(500)    | <br /> |  <br />  | <br /> | 纯文字评论选填；**不支持图片**          | NULL               |
| `created_at` | DATETIME        | <br /> |     ✅    | <br /> | —                          | CURRENT\_TIMESTAMP |

**唯一约束**：`UNIQUE(order_id, product_id, user_id)` — 同一订单同一商品同一用户只能评一次

**CHECK 约束建议**：`CHECK(rating BETWEEN 1 AND 5)` — MySQL 8 支持 CHECK 约束，但某些版本不严格执行，Service 层再校验一次

***

### 2.8 revoked\_tokens（辅助表 · 登出黑名单）

**中文说明**：登出使旧 token 失效（不引入 Redis 的轻量替代）；定时清理过期条目

| 字段           | 类型          |   PK   | NOT NULL | 注释                       | 默认值                |
| ------------ | ----------- | :----: | :------: | ------------------------ | ------------------ |
| `jti`        | VARCHAR(64) |    ✅   |     ✅    | JWT 的 `jti`（唯一 token ID） | —                  |
| `revoked_at` | DATETIME    | <br /> |     ✅    | 被加入黑名单时间                 | CURRENT\_TIMESTAMP |
| `expires_at` | DATETIME    | <br /> |     ✅    | token 原始过期时间，用于清理        | —                  |

***

## 3. 关键索引说明

### 3.1 必须建立的索引

| 表                | 索引名                     | 类型     | 字段                                  | 原因                                    |
| ---------------- | ----------------------- | ------ | ----------------------------------- | ------------------------------------- |
| `users`          | `uk_username`           | UNIQUE | `username`                          | 注册校验登录唯一索引，已有                         |
| `users`          | `idx_role`              | INDEX  | `role`                              | 管理员登录查 role，低频查询，可选                   |
| `categories`     | `idx_parent_id`         | INDEX  | `parent_id`                         | 按一级分类筛二级分类，高频查询                       |
| `categories`     | `uk_parent_name`        | UNIQUE | `parent_id`, `name`                 | 同级分类名唯一（业务约束）                         |
| `products`       | `idx_category_on_shelf` | INDEX  | `category_id`, `on_shelf`           | 前台按二级分类筛上架商品，**最高频查询路径**              |
| `products`       | `idx_on_shelf`          | INDEX  | `on_shelf`                          | 首页全部上架商品查询                            |
| `cart_items`     | `uk_user_product`       | UNIQUE | `user_id`, `product_id`             | 同一用户同一商品唯一行；同时覆盖加购校验路径                |
| `cart_items`     | `idx_user_id`           | INDEX  | `user_id`                           | 列出当前用户购物车                             |
| `orders`         | `uk_order_no`           | UNIQUE | `order_no`                          | 业务编号唯一                                |
| `orders`         | `idx_user_created`      | INDEX  | `user_id`, `created_at DESC`        | **前台"我的订单"列表倒序查询** — 高频路径             |
| `orders`         | `idx_status`            | INDEX  | `status`                            | **后台全部订单按状态筛选** — 高频路径                |
| `orders`         | `idx_user_status`       | INDEX  | `user_id`, `status`                 | 前台按状态筛自己订单（可选，`idx_user_created` 也够用） |
| `order_items`    | `idx_order_id`          | INDEX  | `order_id`                          | 订单详情页一次性查所有明细                         |
| `order_items`    | `idx_product_id`        | INDEX  | `product_id`                        | 商品详情页查历史订单引用（低频，可选）                   |
| `evaluations`    | `uk_order_product_user` | UNIQUE | `order_id`, `product_id`, `user_id` | **防重复评价的关键唯一约束**                      |
| `evaluations`    | `idx_product_id`        | INDEX  | `product_id`                        | 商品详情页展示评价列表 — 高频路径                    |
| `evaluations`    | `idx_user_id`           | INDEX  | `user_id`                           | 用户查看自己的评价历史（低频）                       |
| `revoked_tokens` | `idx_expires_at`        | INDEX  | `expires_at`                        | 清理过期黑名单条目                             |

### 3.2 索引总览（SQL 片段）

```sql
-- ===== users =====
ALTER TABLE users ADD UNIQUE INDEX uk_username (username);

-- ===== categories =====
ALTER TABLE categories ADD INDEX idx_parent_id (parent_id);
ALTER TABLE categories ADD UNIQUE INDEX uk_parent_name (parent_id, name);

-- ===== products =====
ALTER TABLE products ADD INDEX idx_category_on_shelf (category_id, on_shelf);
ALTER TABLE products ADD INDEX idx_on_shelf (on_shelf);

-- ===== cart_items =====
ALTER TABLE cart_items ADD UNIQUE INDEX uk_user_product (user_id, product_id);
ALTER TABLE cart_items ADD INDEX idx_user_id (user_id);

-- ===== orders =====
ALTER TABLE orders ADD UNIQUE INDEX uk_order_no (order_no);
ALTER TABLE orders ADD INDEX idx_user_created (user_id, created_at DESC);
ALTER TABLE orders ADD INDEX idx_status (status);

-- ===== order_items =====
ALTER TABLE order_items ADD INDEX idx_order_id (order_id);

-- ===== evaluations =====
ALTER TABLE evaluations ADD UNIQUE INDEX uk_order_product_user (order_id, product_id, user_id);
ALTER TABLE evaluations ADD INDEX idx_product_id (product_id);

-- ===== revoked_tokens =====
ALTER TABLE revoked_tokens ADD INDEX idx_expires_at (expires_at);
```

***

## 4. 初始化数据 SQL 样例

> **作业硬性要求**：商品 ≥ 12 件（含 2 件下架、1 件库存为 0）；普通用户 2 个；管理员 1 个；5 种状态的订单各 1 条

### 4.1 分类（3 个一级 + 至少 5 个二级）

```sql
INSERT INTO categories (id, name, parent_id, sort_order) VALUES
(1,  '文具用品', NULL, 1),
(2,  '办公用品', NULL, 2),
(3,  '学生用品', NULL, 3),
-- 二级分类（父 文具用品）
(10, '圆珠笔',    1, 1),
(11, '笔记本',    1, 2),
(12, '直尺',      1, 3),
-- 二级分类（父 办公用品）
(20, '文件夹',    2, 1),
(21, '胶带',      2, 2),
-- 二级分类（父 学生用品）
(30, '书包',      3, 1);
```

### 4.2 用户（2 普通 + 1 管理员）

```sql
-- BCrypt 哈希密码示例：原始密码 = "123456"
-- BCrypt.encode("123456", 10) 结果形如 "$2a$10$..." 共 60 字符
INSERT INTO users (username, password, role) VALUES
('alice', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 1),  -- 普通用户
('bob',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 1),  -- 普通用户
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 2);  -- 管理员
```

> ⚠️ 实际演示时密码哈希必须在**运行时**通过 BCrypt 生成后再 INSERT；上面的占位哈希是示例，直接插入将无法登录。

### 4.3 商品（12 件，含 2 下架 + 1 库存 0）

```sql
INSERT INTO products (name, price, stock, category_id, description, image_url, on_shelf) VALUES
-- 上架商品（10 件）
('晨光黑色圆珠笔 0.5mm',   3.50, 120, 10, '标准书写用笔',          '/images/pen-black.webp',    1),
('晨光蓝色圆珠笔 0.5mm',   3.50,  80, 10, '标准书写用笔（蓝色）',  '/images/pen-blue.webp',     1),
('得力中性笔芯（盒）',     12.00, 60, 10, '黑色 20 支装',          '/images/refill.webp',       1),
('A5 横线笔记本 100 页',   8.00, 200, 11, '办公学习通用',          '/images/notebook-a5.webp',  1),
('B5 空白绘画本 80 页',   15.00,  45, 11, '加厚纸张',              '/images/notebook-b5.webp',  1),
('得力透明直尺 30cm',      6.50,  90, 12, '带波浪边',              '/images/ruler.webp',        1),
('得力双孔文件夹（蓝）',   5.00, 150, 20, 'A4 纸适用',             '/images/folder-blue.webp',  1),
('得力双孔文件夹（绿）',   5.00, 150, 20, 'A4 纸适用',             '/images/folder-green.webp', 1),
('晨光透明胶带 2cm×20m',   2.50, 300, 21, '日常修补',              '/images/tape.webp',         1),
('晨光书包 藏青色',       89.00,  25, 30, '15 寸电脑仓',           '/images/bag.webp',          1),

-- 库存 = 0 的上架商品（1 件）
('得力订书器 统一型',     18.00,   0, 20, '送订书钉一盒',          '/images/stapler.webp',      1),

-- 下架商品（2 件）
('百乐钢笔（已下架）',    59.00,  50, 10, '停产清仓',              '/images/pen-pilot.webp',    0),
('儿童剪刀套装（已下架）',25.00,  30, 30, '库存清完下架',          '/images/scissors.webp',     0);
```

### 4.4 订单（5 种状态各 1 条）

```sql
-- 前置：假设商品 id=1 的 name="晨光黑色圆珠笔"、price=3.50（与上文插入顺序一致）
--      用户 alice 的 id=1

-- UNPAID 状态（1 条）
INSERT INTO orders (order_no, user_id, status, total_amount, address, created_at) VALUES
('SO20261029100000001', 1, 0, 7.00, '北京市朝阳区某某路 1 号', NOW());
INSERT INTO order_items (order_id, product_id, product_name, unit_price, quantity, subtotal) VALUES
(LAST_INSERT_ID(), 1, '晨光黑色圆珠笔 0.5mm', 3.50, 2, 7.00);

-- PAID 状态（1 条）
INSERT INTO orders (order_no, user_id, status, total_amount, address, created_at, paid_at) VALUES
('SO20261029110000002', 1, 1, 20.00, '北京市朝阳区某某路 1 号', NOW(), NOW());
INSERT INTO order_items (order_id, product_id, product_name, unit_price, quantity, subtotal) VALUES
(LAST_INSERT_ID(), 4, 'A5 横线笔记本 100 页', 8.00, 2, 16.00),
(LAST_INSERT_ID(), 6, '得力透明直尺 30cm',     6.50, 1,  6.50);

-- SHIPPED 状态（1 条）
INSERT INTO orders (order_no, user_id, status, total_amount, address, created_at, paid_at, shipped_at) VALUES
('SO20261029120000003', 2, 2, 15.50, '上海市浦东新区某某路 2 号', NOW(), NOW(), NOW());
INSERT INTO order_items (order_id, product_id, product_name, unit_price, quantity, subtotal) VALUES
(LAST_INSERT_ID(), 7, '得力双孔文件夹（蓝）', 5.00, 3, 15.00);

-- COMPLETED 状态（1 条）
INSERT INTO orders (order_no, user_id, status, total_amount, address, created_at, paid_at, shipped_at, completed_at) VALUES
('SO20261028100000004', 1, 3, 89.00, '北京市朝阳区某某路 1 号', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));
INSERT INTO order_items (order_id, product_id, product_name, unit_price, quantity, subtotal) VALUES
(LAST_INSERT_ID(), 10, '晨光书包 藏青色', 89.00, 1, 89.00);

-- CANCELLED 状态（1 条）
INSERT INTO orders (order_no, user_id, status, total_amount, address, created_at, cancelled_at) VALUES
('SO20261029130000005', 2, 4, 3.50, '上海市浦东新区某某路 2 号', NOW(), NOW());
INSERT INTO order_items (order_id, product_id, product_name, unit_price, quantity, subtotal) VALUES
(LAST_INSERT_ID(), 2, '晨光蓝色圆珠笔 0.5mm', 3.50, 1, 3.50);
```

***

## 5. 数据库业务约束说明

### 5.1 库存不能为负数

| 层级        | 约束方式                                                                | 说明                                                      |
| --------- | ------------------------------------------------------------------- | ------------------------------------------------------- |
| DB 字段     | `stock INT UNSIGNED`                                                | INT UNSIGNED 本身禁止负数（从 DDL 层面）                           |
| DB SQL    | `UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?` | **更新时带** **`stock >= delta`** **前置条件**，影响行数 = 0 则视为超卖失败 |
| Service 层 | 前置 SELECT 校验 + 事务内 UPDATE 二次校验                                      | 双重保障                                                    |
| DTO 校验    | `@Min(0)` / `@PositiveOrZero`                                       | 管理员修改库存时先做参数校验                                          |

### 5.2 事务使用场景

| 场景          | 事务范围                                                       | 必须原子性的操作                                                           |
| ----------- | ---------------------------------------------------------- | ------------------------------------------------------------------ |
| **下单**      | Service 方法 `@Transactional(rollbackFor = Exception.class)` | 扣库存 + INSERT orders + INSERT order\_items + DELETE cart\_items 勾选行 |
| **取消订单**    | 同上                                                         | 库存回补 + UPDATE orders → CANCELLED                                   |
| **管理员标记发货** | 简单 `@Transactional`                                        | UPDATE orders → SHIPPED（单 SQL，事务可选，保持统一风格）                         |
| **评价提交**    | 简单 `@Transactional`                                        | INSERT evaluations（单 SQL；唯一键被违反则自动回滚）                              |
| 商品 CRUD     | 可选                                                         | 单表操作，事务非必需                                                         |

**事务红线**：**下单事务必须全部成功或全部失败**；出现部分成功（如库存已扣但订单没写）属于严重 Bug

### 5.3 外键 / 逻辑删除策略

| 问题                                           | 策略                           | 理由                                             |
| -------------------------------------------- | ---------------------------- | ---------------------------------------------- |
| 外键约束                                         | **DB 层可建可不见**（建议显式建）         | 防止脏数据；性能影响可忽略（ID 关联 + 有索引）                     |
| 逻辑删除                                         | **MVP 不做**，下架用 `on_shelf` 标志 | 作业明确"下架=前台不可见"；物理删除商品会影响历史 order\_items 的快照完整性 |
| `categories.parent_id` 允许 NULL               | 自关联一级分类                      | NULL 表示一级分类；非 NULL 表示二级分类                      |
| `order_items.product_name` / `unit_price` 冗余 | **有意的快照字段**                  | 不依赖 product FK 实时查询，确保历史订单独立                   |

### 5.4 事务回滚测试清单（BDD 场景映射）

| 测试场景                          | 覆盖 BDD         |
| ----------------------------- | -------------- |
| 正常下单全部成功                      | US04-01        |
| 库存不足 → 整单失败 → 事务回滚            | US04-03        |
| 商品被并发下架 + 超卖（UPDATE 影响行数 = 0） | US03-03 + 并发测试 |
| 取消订单 → 库存正确回补                 | US05-02        |

***

*—— END OF DATABASE DESIGN ——*
