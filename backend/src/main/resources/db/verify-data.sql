-- =============================================================================
-- 文具商城 MVP · 初始化数据校验脚本（配合 init-data.sql 使用）
-- 适用：MySQL 8.0+ / InnoDB / utf8mb4
--
-- 使用方式：
--   1) 命令行： mysql -u root -p stationery_mall < verify-data.sql
--   2) 客户端： SOURCE D:/.../backend/src/main/resources/db/verify-data.sql;
--
-- 结果阅读：
--   * 【第一部分】断言总表：result 列全为 PASS 即数据合格，任一 FAIL 即定位到具体检查项
--   * 【第二部分】明细排查：每条查询「应返回 0 行」，返回任何行即为异常数据
--   * 【第三部分】业务场景抽查：确认测试用例依赖的数据确实存在
-- =============================================================================

SET NAMES utf8mb4;

-- =============================================================================
-- 第一部分：断言总表（expected 与 actual 相等 → PASS）
-- =============================================================================
SELECT check_name, expected, actual,
       CASE WHEN expected = actual THEN 'PASS' ELSE 'FAIL' END AS result
FROM (
    SELECT '① 用户总数（期望 3）' AS check_name,
           '3' AS expected,
           CAST((SELECT COUNT(*) FROM users) AS CHAR) AS actual
    UNION ALL
    SELECT '② NORMAL_USER 数（期望 2）',
           '2',
           CAST((SELECT COUNT(*) FROM users WHERE role = 1) AS CHAR)
    UNION ALL
    SELECT '③ ADMIN 数（期望 1）',
           '1',
           CAST((SELECT COUNT(*) FROM users WHERE role = 2) AS CHAR)
    UNION ALL
    SELECT '④ admin 账号存在且为管理员（期望 1）',
           '1',
           CAST((SELECT COUNT(*) FROM users WHERE username = 'admin' AND role = 2) AS CHAR)
    UNION ALL
    SELECT '⑤ 密码哈希长度=60 的用户数（期望 3）',
           '3',
           CAST((SELECT COUNT(*) FROM users WHERE CHAR_LENGTH(password) = 60) AS CHAR)
    UNION ALL
    SELECT '⑥ 一级分类数（期望 3，≥3）',
           '3',
           CAST((SELECT COUNT(*) FROM categories WHERE parent_id IS NULL) AS CHAR)
    UNION ALL
    SELECT '⑦ 二级分类数（期望 7）',
           '7',
           CAST((SELECT COUNT(*) FROM categories WHERE parent_id IS NOT NULL) AS CHAR)
    UNION ALL
    SELECT '⑧ 商品总数（期望 13，≥12）',
           '13',
           CAST((SELECT COUNT(*) FROM products) AS CHAR)
    UNION ALL
    SELECT '⑨ 下架商品数（期望 2）',
           '2',
           CAST((SELECT COUNT(*) FROM products WHERE on_shelf = 0) AS CHAR)
    UNION ALL
    SELECT '⑩ 库存为 0 的商品数（期望 1）',
           '1',
           CAST((SELECT COUNT(*) FROM products WHERE stock = 0) AS CHAR)
    UNION ALL
    SELECT '⑪ 商品挂在二级分类的数量（期望 13 = 总数）',
           '13',
           CAST((SELECT COUNT(*)
                   FROM products p
                   JOIN categories c ON c.id = p.category_id
                  WHERE c.parent_id IS NOT NULL) AS CHAR)
    UNION ALL
    SELECT '⑫ 订单总数（期望 6）',
           '6',
           CAST((SELECT COUNT(*) FROM orders) AS CHAR)
    UNION ALL
    SELECT '⑬ 覆盖的订单状态数（期望 5：0/1/2/3/4）',
           '5',
           CAST((SELECT COUNT(DISTINCT status) FROM orders) AS CHAR)
    UNION ALL
    SELECT '⑭ 终态订单数（COMPLETED+CANCELLED，期望 3）',
           '3',
           CAST((SELECT COUNT(*) FROM orders WHERE status IN (3, 4)) AS CHAR)
    UNION ALL
    SELECT '⑮ 订单明细总数（期望 7）',
           '7',
           CAST((SELECT COUNT(*) FROM order_items) AS CHAR)
    UNION ALL
    SELECT '⑯ 购物车项总数（期望 6）',
           '6',
           CAST((SELECT COUNT(*) FROM cart_items) AS CHAR)
    UNION ALL
    SELECT '⑰ 评价总数（期望 2）',
           '2',
           CAST((SELECT COUNT(*) FROM evaluations) AS CHAR)
    UNION ALL
    SELECT '⑱ 订单金额与明细合计不一致的订单数（期望 0）',
           '0',
           CAST((SELECT COUNT(*)
                   FROM (SELECT o.id
                           FROM orders o
                           LEFT JOIN order_items oi ON oi.order_id = o.id
                          GROUP BY o.id, o.total_amount
                         HAVING o.total_amount <> COALESCE(SUM(oi.subtotal), 0)) t) AS CHAR)
    UNION ALL
    SELECT '⑲ 明细小计 ≠ 单价 × 数量 的行数（期望 0）',
           '0',
           CAST((SELECT COUNT(*)
                   FROM order_items
                  WHERE subtotal <> unit_price * quantity) AS CHAR)
    UNION ALL
    SELECT '⑳ 购物车重复 (user_id, product_id) 组数（期望 0）',
           '0',
           CAST((SELECT COUNT(*)
                   FROM (SELECT user_id, product_id
                           FROM cart_items
                          GROUP BY user_id, product_id
                         HAVING COUNT(*) > 1) t) AS CHAR)
    UNION ALL
    SELECT '㉑ 评价重复 (order_id, product_id, user_id) 组数（期望 0）',
           '0',
           CAST((SELECT COUNT(*)
                   FROM (SELECT order_id, product_id, user_id
                           FROM evaluations
                          GROUP BY order_id, product_id, user_id
                         HAVING COUNT(*) > 1) t) AS CHAR)
    UNION ALL
    SELECT '㉒ 订单状态越界（非 0~4）的行数（期望 0）',
           '0',
           CAST((SELECT COUNT(*) FROM orders WHERE status NOT BETWEEN 0 AND 4) AS CHAR)
    UNION ALL
    SELECT '㉓ 评分越界（非 1~5）的评价数（期望 0）',
           '0',
           CAST((SELECT COUNT(*) FROM evaluations WHERE rating NOT BETWEEN 1 AND 5) AS CHAR)
    UNION ALL
    SELECT '㉔ 订单状态与时间戳不一致的订单数（期望 0）',
           '0',
           CAST((SELECT COUNT(*)
                   FROM orders
                  WHERE (status = 0 AND paid_at IS NOT NULL)
                     OR (status = 1 AND (paid_at IS NULL OR shipped_at IS NOT NULL))
                     OR (status = 2 AND (paid_at IS NULL OR shipped_at IS NULL OR completed_at IS NOT NULL))
                     OR (status = 3 AND (paid_at IS NULL OR shipped_at IS NULL OR completed_at IS NULL))
                     OR (status = 4 AND cancelled_at IS NULL)) AS CHAR)
    UNION ALL
    SELECT '㉕ 评价所属订单非 COMPLETED 或与订单用户不一致的评价数（期望 0）',
           '0',
           CAST((SELECT COUNT(*)
                   FROM evaluations e
                   JOIN orders o ON o.id = e.order_id
                  WHERE o.status <> 3 OR o.user_id <> e.user_id) AS CHAR)
) checks;

-- =============================================================================
-- 第二部分：明细排查（以下每条查询都应返回 0 行；有行即异常）
-- =============================================================================

-- 2.1 订单合计金额与明细合计不一致
SELECT o.id AS order_id, o.order_no, o.total_amount,
       COALESCE(SUM(oi.subtotal), 0) AS items_sum
FROM orders o
LEFT JOIN order_items oi ON oi.order_id = o.id
GROUP BY o.id, o.order_no, o.total_amount
HAVING o.total_amount <> COALESCE(SUM(oi.subtotal), 0);

-- 2.2 明细小计与「单价 × 数量」不一致
SELECT id AS order_item_id, order_id, unit_price, quantity, subtotal
FROM order_items
WHERE subtotal <> unit_price * quantity;

-- 2.3 孤儿数据：orders.user_id 指向不存在的用户
SELECT o.id AS order_id, o.user_id
FROM orders o
LEFT JOIN users u ON u.id = o.user_id
WHERE u.id IS NULL;

-- 2.4 孤儿数据：order_items 指向不存在的订单 / 商品
SELECT oi.id AS order_item_id, oi.order_id, oi.product_id
FROM order_items oi
LEFT JOIN orders o ON o.id = oi.order_id
LEFT JOIN products p ON p.id = oi.product_id
WHERE o.id IS NULL OR p.id IS NULL;

-- 2.5 孤儿数据：cart_items 指向不存在的用户 / 商品
SELECT ci.id AS cart_item_id, ci.user_id, ci.product_id
FROM cart_items ci
LEFT JOIN users u ON u.id = ci.user_id
LEFT JOIN products p ON p.id = ci.product_id
WHERE u.id IS NULL OR p.id IS NULL;

-- 2.6 孤儿数据：evaluations 指向不存在的订单 / 商品 / 用户
SELECT e.id AS evaluation_id, e.order_id, e.product_id, e.user_id
FROM evaluations e
LEFT JOIN orders o ON o.id = e.order_id
LEFT JOIN products p ON p.id = e.product_id
LEFT JOIN users u ON u.id = e.user_id
WHERE o.id IS NULL OR p.id IS NULL OR u.id IS NULL;

-- 2.7 孤儿数据：二级分类的 parent_id 指向不存在或非一级分类
SELECT c.id AS category_id, c.name, c.parent_id
FROM categories c
LEFT JOIN categories p ON p.id = c.parent_id
WHERE c.parent_id IS NOT NULL
  AND (p.id IS NULL OR p.parent_id IS NOT NULL);

-- 2.8 商品挂在一级分类（业务禁止）
SELECT p.id AS product_id, p.name, p.category_id
FROM products p
JOIN categories c ON c.id = p.category_id
WHERE c.parent_id IS NULL;

-- 2.9 购物车数量 < 1
SELECT id AS cart_item_id, user_id, product_id, quantity
FROM cart_items
WHERE quantity < 1;

-- 2.10 购物车数量超过当前库存（注：cart_item_id=3 指向 product_id=11（库存 0），属刻意样例）
SELECT ci.id AS cart_item_id, ci.product_id, ci.quantity, p.stock
FROM cart_items ci
JOIN products p ON p.id = ci.product_id
WHERE ci.quantity > p.stock;

-- 2.11 订单状态时间戳不一致（例：PAID 缺 paid_at、CANCELLED 缺 cancelled_at）
SELECT id AS order_id, order_no, status, paid_at, shipped_at, completed_at, cancelled_at
FROM orders
WHERE (status = 0 AND paid_at IS NOT NULL)
   OR (status = 1 AND (paid_at IS NULL OR shipped_at IS NOT NULL))
   OR (status = 2 AND (paid_at IS NULL OR shipped_at IS NULL OR completed_at IS NOT NULL))
   OR (status = 3 AND (paid_at IS NULL OR shipped_at IS NULL OR completed_at IS NULL))
   OR (status = 4 AND cancelled_at IS NULL);

-- =========================================================================
-- 第三部分：业务场景抽查（有数据即说明测试用例可执行）
-- =========================================================================

-- 3.1 各订单状态分布 + 归属用户（应覆盖 0~4）
SELECT o.status,
       CASE o.status WHEN 0 THEN 'UNPAID' WHEN 1 THEN 'PAID' WHEN 2 THEN 'SHIPPED'
                     WHEN 3 THEN 'COMPLETED' WHEN 4 THEN 'CANCELLED' ELSE 'UNKNOWN' END AS status_desc,
       COUNT(*) AS order_count,
       GROUP_CONCAT(CONCAT(u.username, '#', o.id) ORDER BY o.id) AS orders
FROM orders o
JOIN users u ON u.id = o.user_id
GROUP BY o.status
ORDER BY o.status;

-- 3.2 商品上架 / 缺货 / 下架分布
SELECT CASE
         WHEN on_shelf = 0 THEN 'OFF_SHELF(下架)'
         WHEN stock = 0    THEN 'ON_SHELF(缺货 stock=0)'
         ELSE 'ON_SHELF(正常)'
       END AS goods_state,
       COUNT(*) AS goods_count,
       GROUP_CONCAT(CONCAT(id, ':', name) ORDER BY id SEPARATOR ' | ') AS goods
FROM products
GROUP BY goods_state;

-- 3.3 购物车明细（含被 listMyCart 自动剔除的下架行，属预期）
SELECT ci.id AS cart_item_id, u.username, p.name AS product_name, ci.quantity,
       p.on_shelf, p.stock,
       CASE
         WHEN p.on_shelf = 0 THEN '下架 → 购物车列表自动剔除'
         WHEN ci.quantity > p.stock THEN '库存不足 → 下单时报错'
         ELSE '可正常结算'
       END AS scene
FROM cart_items ci
JOIN users u ON u.id = ci.user_id
JOIN products p ON p.id = ci.product_id
ORDER BY ci.id;

-- 3.4 已完成订单中的商品（可评价素材）及是否已评价
SELECT o.id AS order_id, u.username, p.id AS product_id, p.name AS product_name,
       CASE WHEN e.id IS NULL THEN '未评价（可提交）' ELSE '已评价（重复提交应 409）' END AS eval_state,
       e.rating, e.content
FROM orders o
JOIN users u ON u.id = o.user_id
JOIN order_items oi ON oi.order_id = o.id
LEFT JOIN products p ON p.id = oi.product_id
LEFT JOIN evaluations e
       ON e.order_id = o.id AND e.product_id = oi.product_id AND e.user_id = o.user_id
WHERE o.status = 3
ORDER BY o.id, oi.id;

-- 3.5 可执行动作清单（下单 / 付款 / 取消 / 发货 / 收货 / 评价）
SELECT o.id AS order_id, o.order_no, u.username, o.status,
       CASE o.status WHEN 0 THEN '可付款、可取消'
                     WHEN 1 THEN '可标记发货（管理员）'
                     WHEN 2 THEN '可确认收货（用户）'
                     WHEN 3 THEN '可评价（未评价商品）'
                     WHEN 4 THEN '终态，不可操作' END AS available_action
FROM orders o
JOIN users u ON u.id = o.user_id
ORDER BY o.id;

-- =============================================================================
-- 判定原则：第一部分全部 PASS + 第二部分全部返回 0 行 → 初始化数据可用
-- 例外：2.10 会返回 cart_item_id=3（product_id=11，stock=0），这是刻意用于「下单库存不足」测试的预期数据
-- =============================================================================
