-- =============================================================================
-- 文具商城 MVP · MySQL 8 建表脚本（schema.sql）
-- 依据：docs/02-design/database-design.md（2. 数据表详细定义 / 3. 索引设计）
-- 引擎：InnoDB    字符集：utf8mb4    排序规则：utf8mb4_0900_ai_ci（MySQL 8 默认）
--
-- 表名说明：与 7 份 Mapper XML、init-data.sql、verify-data.sql 完全一致，
--   采用复数命名：users / categories / products / cart_items / orders / order_items / evaluations
--   （orders 为关键字，脚本内统一使用反引号包裹）
--
-- 使用方式：
--   1) 先建库：CREATE DATABASE IF NOT EXISTS stationery_mall DEFAULT CHARSET utf8mb4;
--   2) 命令行导入：mysql -u root -p stationery_mall < schema.sql
--   3) 客户端内：SOURCE D:/.../backend/src/main/resources/db/schema.sql;
--   4) 再导入测试数据：SOURCE .../db/init-data.sql;  校验：SOURCE .../db/verify-data.sql;
--
-- 执行顺序：先删后建（DROP 按外键反序、关闭外键检查，便于反复重建）
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 0. 重建：关闭外键检查后按依赖反序删除（重复执行不会报错）
-- -----------------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS evaluations;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 1. users（用户表）
--    role: 1=NORMAL_USER（注册默认）, 2=ADMIN（脚本预置）；游客 VISITOR 无 DB 记录
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    username   VARCHAR(32)     NOT NULL                COMMENT '登录用户名，全局唯一',
    password   VARCHAR(128)    NOT NULL                COMMENT 'BCrypt 哈希，永不存明文',
    role       TINYINT         NOT NULL DEFAULT 1      COMMENT '角色：1=NORMAL_USER，2=ADMIN（VISITOR 无记录）',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    updated_at DATETIME        NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '资料更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_role (role)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表（普通用户 + 管理员）';

-- -----------------------------------------------------------------------------
-- 2. categories（商品分类表 · 两级自关联）
--    parent_id IS NULL = 一级分类；非 NULL = 二级分类；商品只能挂二级分类（Service 校验）
-- -----------------------------------------------------------------------------
CREATE TABLE categories (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name       VARCHAR(32)     NOT NULL                COMMENT '分类名',
    parent_id  BIGINT UNSIGNED NULL     DEFAULT NULL   COMMENT '父分类 ID：NULL=一级分类，非 NULL=二级分类',
    sort_order INT             NOT NULL DEFAULT 0      COMMENT '同级排序，小的靠前',
    PRIMARY KEY (id),
    UNIQUE KEY uk_parent_name (parent_id, name),
    KEY idx_parent_id (parent_id),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品分类表（两级自关联）';

-- -----------------------------------------------------------------------------
-- 3. products（商品表）
--    on_shelf: 1=上架（前台可见），0=下架（前台不可见）
--    stock: INT UNSIGNED 天然不允许负数
-- -----------------------------------------------------------------------------
CREATE TABLE products (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(128)    NOT NULL                COMMENT '商品展示名',
    price       DECIMAL(10, 2)  NOT NULL                COMMENT '当前单价，DECIMAL 避免浮点误差',
    stock       INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '库存数量，≥0',
    category_id BIGINT UNSIGNED NOT NULL                COMMENT '所属分类，必须为二级分类（Service 校验）',
    description TEXT            NULL                    COMMENT '商品描述（前端展示需 XSS 清洗）',
    image_url   VARCHAR(512)    NULL                    COMMENT '单张图片 URL（MVP 单图）',
    on_shelf    TINYINT(1)      NOT NULL DEFAULT 1      COMMENT '上下架：1=上架（可见），0=下架（不可见）',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME        NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_category_on_shelf (category_id, on_shelf),
    KEY idx_on_shelf (on_shelf),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品表（单规格）';

-- -----------------------------------------------------------------------------
-- 4. cart_items（购物车项）
--    UNIQUE(user_id, product_id)：同一商品重复加购只累加数量
-- -----------------------------------------------------------------------------
CREATE TABLE cart_items (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT UNSIGNED NOT NULL                COMMENT '所属用户 → users.id',
    product_id BIGINT UNSIGNED NOT NULL                COMMENT '商品 → products.id',
    quantity   INT UNSIGNED    NOT NULL DEFAULT 1      COMMENT '数量，≥1 且不得超过 product.stock',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_product (user_id, product_id),
    KEY idx_user_id (user_id),
    CONSTRAINT fk_cart_items_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '购物车项（用户×商品唯一）';

-- -----------------------------------------------------------------------------
-- 5. orders（订单主表）
--    status: 0=UNPAID, 1=PAID, 2=SHIPPED, 3=COMPLETED, 4=CANCELLED
--    时间字段与状态对应：PAID→paid_at，SHIPPED→shipped_at，COMPLETED→completed_at，CANCELLED→cancelled_at
-- -----------------------------------------------------------------------------
CREATE TABLE `orders` (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no     VARCHAR(32)     NOT NULL                COMMENT '业务订单号，如 SO20261029143052001',
    user_id      BIGINT UNSIGNED NOT NULL                COMMENT '下单用户 → users.id',
    status       TINYINT         NOT NULL                COMMENT '订单状态：0=UNPAID 待支付，1=PAID 已支付，2=SHIPPED 已发货，3=COMPLETED 已完成，4=CANCELLED 已取消',
    total_amount DECIMAL(10, 2)  NOT NULL                COMMENT '订单总额，服务端重算，不采信前端',
    address      VARCHAR(512)    NOT NULL                COMMENT '收货地址（MVP 纯文本）',
    remark       VARCHAR(256)    NULL                    COMMENT '下单备注（选填）',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（扣库存事务内写入）',
    paid_at      DATETIME        NULL     DEFAULT NULL   COMMENT '模拟付款时间',
    shipped_at   DATETIME        NULL     DEFAULT NULL   COMMENT '管理员发货时间',
    completed_at DATETIME        NULL     DEFAULT NULL   COMMENT '确认收货时间',
    cancelled_at DATETIME        NULL     DEFAULT NULL   COMMENT '取消时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_created (user_id, created_at DESC),
    KEY idx_status (status),
    KEY idx_user_status (user_id, status),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单主表（5 状态）';

-- -----------------------------------------------------------------------------
-- 6. order_items（订单明细 · 快照）
--    product_name / unit_price 为下单当时快照，后续商品改名/改价不影响历史
-- -----------------------------------------------------------------------------
CREATE TABLE order_items (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id     BIGINT UNSIGNED NOT NULL                COMMENT '所属订单 → orders.id',
    product_id   BIGINT UNSIGNED NOT NULL                COMMENT '商品 → products.id（仅数据完整性）',
    product_name VARCHAR(128)    NOT NULL                COMMENT '下单当时的商品名称（快照）',
    unit_price   DECIMAL(10, 2)  NOT NULL                COMMENT '下单当时的商品单价（快照）',
    quantity     INT UNSIGNED    NOT NULL                COMMENT '下单数量，≥1',
    subtotal     DECIMAL(10, 2)  NOT NULL                COMMENT '小计 = unit_price × quantity',
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_product_id (product_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES `orders` (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单明细表（商品快照）';

-- -----------------------------------------------------------------------------
-- 7. evaluations（评价表）
--    UNIQUE(order_id, product_id, user_id)：同一订单同一商品同一用户只能评一次
--    CHECK(rating BETWEEN 1 AND 5)：MySQL 8 生效，Service 层另有校验
-- -----------------------------------------------------------------------------
CREATE TABLE evaluations (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id   BIGINT UNSIGNED NOT NULL                COMMENT '订单 → orders.id（仅 COMPLETED 可评）',
    product_id BIGINT UNSIGNED NOT NULL                COMMENT '商品 → products.id',
    user_id    BIGINT UNSIGNED NOT NULL                COMMENT '评价人 → users.id，必须等于订单所属用户',
    rating     TINYINT         NOT NULL                COMMENT '评分：1~5 星',
    content    VARCHAR(500)    NULL                    COMMENT '评价内容（纯文本，选填）',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_product_user (order_id, product_id, user_id),
    KEY idx_product_id (product_id),
    KEY idx_user_id (user_id),
    CONSTRAINT chk_evaluations_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT fk_evaluations_order FOREIGN KEY (order_id) REFERENCES `orders` (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_evaluations_product FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_evaluations_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '评价表（订单×商品×用户唯一）';

-- =============================================================================
-- 建表完成：7 张表 + 外键 + 索引
-- 可执行以下 SQL 确认：
--   SHOW TABLES;
--   SELECT TABLE_NAME, TABLE_COMMENT
--     FROM information_schema.TABLES
--    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN
--          ('users','categories','products','cart_items','orders','order_items','evaluations');
-- =============================================================================
