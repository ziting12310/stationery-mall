-- =============================================================================
-- 文具商城 MVP · 初始化测试数据（仅 INSERT，不含建表）
-- 适用：MySQL 8.0+ / InnoDB / utf8mb4
-- 依据：docs/02-design/database-design.md 4. 初始化数据 SQL 样例
-- 表结构：users / categories / products / cart_items / orders / order_items / evaluations
--
-- 使用方式（任选其一）：
--   1) 命令行导入： mysql -u root -p stationery_mall < init-data.sql
--   2) 客户端内导入：SOURCE D:/.../backend/src/main/resources/db/init-data.sql;
-- 说明：
--   * 脚本可重复执行：先按外键反序清空 7 张表，再按正序插入（分类/用户 → 商品 → 订单 → 明细/购物车/评价）
--   * 所有主键显式指定，便于测试用例直接引用固定 id
--   * 订单状态数值：0=UNPAID(待支付)、1=PAID、2=SHIPPED、3=COMPLETED、4=CANCELLED
--     （作业描述中的 PENDING_PAY 即本项目枚举 UNPAID）
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 0. 清理旧数据（按外键依赖反序，保证可重复导入）
-- -----------------------------------------------------------------------------
DELETE FROM evaluations;
DELETE FROM cart_items;
DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM products;
DELETE FROM categories;
DELETE FROM users;

-- -----------------------------------------------------------------------------
-- 1. 用户（2 普通用户 + 1 管理员）
--    alice / bob 明文密码：123456     admin 明文密码：admin123
--    下列哈希由项目依赖 at.favre.lib:bcrypt 10 rounds 实时生成并校验通过，
--    可直接登录（BCryptUtil.matches 校验通过）。
-- -----------------------------------------------------------------------------
INSERT INTO users (id, username, password, role) VALUES
(1, 'alice', '$2a$10$o.sqWzKrClKdZmiU/.etvumtBeCY.m0mn7T1HImy/0W73UNYwWOZi', 1),  -- NORMAL_USER / 123456
(2, 'bob',   '$2a$10$o.sqWzKrClKdZmiU/.etvumtBeCY.m0mn7T1HImy/0W73UNYwWOZi', 1),  -- NORMAL_USER / 123456
(3, 'admin', '$2a$10$LP2k9JliOwa56A17xVNnluWYXUYTzfUZCccZtgzp.cM3I5XEJOvaS', 2);  -- ADMIN / admin123

-- -----------------------------------------------------------------------------
-- 2. 分类（3 个一级 + 7 个二级，两级自关联：parent_id 为 NULL 即一级）
-- -----------------------------------------------------------------------------
INSERT INTO categories (id, name, parent_id, sort_order) VALUES
(1,  '文具用品', NULL, 1),
(2,  '办公用品', NULL, 2),
(3,  '学生用品', NULL, 3),
(10, '圆珠笔',    1, 1),
(11, '笔记本',    1, 2),
(12, '直尺',      1, 3),
(20, '文件夹',    2, 1),
(21, '胶带',      2, 2),
(30, '书包',      3, 1),
(31, '剪刀',      3, 2);

-- -----------------------------------------------------------------------------
-- 3. 商品（13 件：10 件正常上架 + 1 件库存 0 + 2 件下架）
--    id 11：stock = 0（上架但缺货，用于测试「库存不足」）
--    id 12、13：on_shelf = 0（下架，用于测试「下架不可加购/下单/详情提示」）
-- -----------------------------------------------------------------------------
INSERT INTO products (id, name, price, stock, category_id, description, image_url, on_shelf) VALUES
(1,  '晨光黑色圆珠笔 0.5mm',   3.50, 120, 10, '标准书写用笔',           '/images/pen-black.webp',    1),
(2,  '晨光蓝色圆珠笔 0.5mm',   3.50,  80, 10, '标准书写用笔（蓝色）',   '/images/pen-blue.webp',     1),
(3,  '得力中性笔芯（盒）',     12.00, 60, 10, '黑色 20 支装',           '/images/refill.webp',       1),
(4,  'A5 横线笔记本 100 页',    8.00, 200, 11, '办公学习通用',           '/images/notebook-a5.webp',  1),
(5,  'B5 空白绘画本 80 页',    15.00,  45, 11, '加厚纸张',               '/images/notebook-b5.webp',  1),
(6,  '得力透明直尺 30cm',       6.50,  90, 12, '带波浪边',               '/images/ruler.webp',        1),
(7,  '得力双孔文件夹（蓝）',    5.00, 150, 20, 'A4 纸适用',              '/images/folder-blue.webp',  1),
(8,  '得力双孔文件夹（绿）',    5.00, 150, 20, 'A4 纸适用',              '/images/folder-green.webp', 1),
(9,  '晨光透明胶带 2cm×20m',    2.50, 300, 21, '日常修补',               '/images/tape.webp',         1),
(10, '晨光书包 藏青色',        89.00,  25, 30, '15 寸电脑仓',            '/images/bag.webp',          1),
(11, '得力订书器 统一型',      18.00,   0, 20, '送订书钉一盒（无现货）', '/images/stapler.webp',      1),  -- 库存 0
(12, '百乐钢笔（已下架）',      59.00,  50, 10, '停产清仓',               '/images/pen-pilot.webp',    0),  -- 下架
(13, '儿童剪刀套装（已下架）',  25.00,  30, 31, '库存清完下架',           '/images/scissors.webp',     0);  -- 下架

-- -----------------------------------------------------------------------------
-- 4. 订单（5 种状态各 1 条 + 额外 1 条 bob 的 COMPLETED 用于评价测试）
--    id 1 alice UNPAID     → 测试：模拟付款 / 取消订单
--    id 2 alice PAID       → 测试：管理员标记发货
--    id 3 bob   SHIPPED    → 测试：用户确认收货
--    id 4 alice COMPLETED  → 测试：提交评价（已评价 1 件）
--    id 5 bob   CANCELLED  → 测试：终态不可流转
--    id 6 bob   COMPLETED  → 测试：bob 侧评价 + 商品详情评价列表
-- -----------------------------------------------------------------------------
INSERT INTO orders (id, order_no, user_id, status, total_amount, address, remark, created_at, paid_at, shipped_at, completed_at, cancelled_at) VALUES
(1, 'SO20261029100000001', 1, 0,  7.00, '北京市朝阳区某某路 1 号', '请尽快发货', NOW(), NULL, NULL, NULL, NULL),
(2, 'SO20261029110000002', 1, 1, 22.50, '北京市朝阳区某某路 1 号', NULL, NOW(), NOW(), NULL, NULL, NULL),
(3, 'SO20261029120000003', 2, 2, 15.00, '上海市浦东新区某某路 2 号', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), NULL, NULL),
(4, 'SO20261028100000004', 1, 3, 89.00, '北京市朝阳区某某路 1 号', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
(5, 'SO20261029130000005', 2, 4,  3.50, '上海市浦东新区某某路 2 号', '不想要了', NOW(), NULL, NULL, NULL, NOW()),
(6, 'SO20261027100000006', 2, 3,  7.00, '上海市浦东新区某某路 2 号', NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL);

-- -----------------------------------------------------------------------------
-- 5. 订单明细（product_name / unit_price 为下单快照，与当前商品解耦）
-- -----------------------------------------------------------------------------
INSERT INTO order_items (id, order_id, product_id, product_name, unit_price, quantity, subtotal) VALUES
(1, 1, 1,  '晨光黑色圆珠笔 0.5mm', 3.50, 2,  7.00),
(2, 2, 4,  'A5 横线笔记本 100 页', 8.00, 2, 16.00),
(3, 2, 6,  '得力透明直尺 30cm',     6.50, 1,  6.50),
(4, 3, 7,  '得力双孔文件夹（蓝）',  5.00, 3, 15.00),
(5, 4, 10, '晨光书包 藏青色',      89.00, 1, 89.00),
(6, 5, 2,  '晨光蓝色圆珠笔 0.5mm',  3.50, 1,  3.50),
(7, 6, 1,  '晨光黑色圆珠笔 0.5mm',  3.50, 2,  7.00);

-- -----------------------------------------------------------------------------
-- 6. 购物车（UNIQUE(user_id, product_id)，同一商品只一行）
--    alice：可结算商品 + 1 条库存 0（id 11，测试下单库存不足）+ 1 条下架（id 12，测试自动剔除）
--    bob  ：可结算商品 + 1 条下架（id 13，测试自动剔除）
-- -----------------------------------------------------------------------------
INSERT INTO cart_items (id, user_id, product_id, quantity) VALUES
(1, 1, 3,  1),
(2, 1, 5,  2),
(3, 1, 11, 1),
(4, 1, 12, 1),
(5, 2, 9,  3),
(6, 2, 13, 1);

-- -----------------------------------------------------------------------------
-- 7. 评价（UNIQUE(order_id, product_id, user_id)，仅 COMPLETED 订单可评价）
-- -----------------------------------------------------------------------------
INSERT INTO evaluations (id, order_id, product_id, user_id, rating, content, created_at) VALUES
(1, 4, 10, 1, 5, '容量很大，15 寸电脑能放下，孩子很喜欢', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 6, 1,  2, 4, '书写顺滑，一支能用很久，性价比高',       DATE_SUB(NOW(), INTERVAL 1 DAY));

-- =============================================================================
-- 数据自检（可选，导入后可在客户端执行 SELECT 验证）
--   SELECT COUNT(*) FROM products;            -- 期望 13（其中 on_shelf=0 两条、stock=0 一条）
--   SELECT status, COUNT(*) FROM orders GROUP BY status;  -- 期望覆盖 0/1/2/3/4
--   SELECT COUNT(*) FROM cart_items;          -- 期望 6
-- =============================================================================
