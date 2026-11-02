# RESTful 接口规范文档（API Spec）

> 版本：v1.0 · 日期：2026-10-29
> 依赖文档：[PRD.md](../01-requirements/PRD.md) · [GLOSSARY.md](../01-requirements/GLOSSARY.md) · [architecture.md](./architecture.md)
> 前后端：Vue 3 + Axios（前端） → Spring Boot 3 + MyBatis（后端） → MySQL 8
> 所有接口 JSON 序列化：`snake_case（DB） → camelCase（Java/JSON）`，由 MyBatis `map-underscore-to-camel-case: true` + Jackson 默认行为自动完成

---

## 1. 接口总约定

### 1.1 基础 URL

| 环境 | Base URL |
|------|----------|
| 开发环境（Vite proxy） | 前端通过 `/api/**` 代理到 `http://localhost:8080` |
| 后端直连 | `http://localhost:8080/api` |
| Nginx 反代（验收） | 同域 `/api` 前缀，无跨域 |

### 1.2 请求头与 Token 鉴权规则

```http
Content-Type: application/json
Authorization: Bearer <jwt_token>    ← 需鉴权的接口必须携带；游客接口可省略
```

| 场景 | 后端行为 |
|------|----------|
| 请求无 `Authorization` 头，但接口**需要登录** | 返回 `401 UNAUTHORIZED` |
| token 无效（签名 / 过期 / 黑名单） | 返回 `401 UNAUTHORIZED` |
| token 有效但角色不匹配 | 返回 `403 FORBIDDEN` |
| token 有效 + 角色匹配 | 放行，将 `userId` + `role` 注入 request attribute |

### 1.3 全局统一响应结构体

**后端 Java 类定义（DTO）：**

```java
public record ApiResponse<T>(
    int code,
    String message,
    T data
) {}
```

**JSON 示例（成功）：**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 42,
    "username": "alice",
    "role": "NORMAL_USER",
    "createdAt": "2026-10-29T14:30:52"
  }
}
```

**JSON 示例（失败）：**

```json
{
  "code": 400,
  "message": "商品库存不足，仅剩 3 件",
  "data": null
}
```

**Axios 前端响应拦截器约定：**

```typescript
axios.interceptors.response.use(
  resp => {
    const { code, message, data } = resp.data
    if (code === 0) return data                  // 成功：直接把 data 解包返回
    if (code === 401) router.push('/login')      // 未登录：跳登录
    if (code === 403) ElMessage.error('无权限操作')
    ElMessage.error(message)                     // 其他：Toast 提示
    return Promise.reject(new Error(message))
  }
)
```

### 1.4 公共业务状态码枚举

| code | HTTP | 常量名 | 含义 | 典型触发场景 |
|------|------|--------|------|--------------|
| **0** | 200 | `SUCCESS` | 成功 | — |
| **400** | 400 | `BAD_REQUEST` | 业务不允许 | 库存不足、订单状态越权、下架商品加购、参数校验失败 |
| **401** | 401 | `UNAUTHORIZED` | 未登录 / token 失效 | 游客访问购物车 / 订单接口、登出后旧 token |
| **403** | 403 | `FORBIDDEN` | 已登录但无权限 | 普通用户访问 `/api/admin/**`、访问他人订单、管理员尝试下单 |
| **404** | 404 | `NOT_FOUND` | 资源不存在 | 直链访问不存在/已删除商品 |
| **409** | 409 | `CONFLICT` | 业务冲突 | 重复用户名注册、重复评价（唯一键拦截） |
| **413** | 413 | `PAYLOAD_TOO_LARGE` | 请求体过大 | 商品图片单图 > 5 MB |
| **500** | 500 | `INTERNAL_ERROR` | 服务器内部异常 | DB 连接异常、事务执行失败（兜底） |

### 1.5 分页参数约定（Cursor 游标分页）

所有列表型接口统一使用 **Cursor 游标分页**（基于 `id`），避免 OFFSET 性能问题：

```json
// 请求（query params）
GET /api/products?cursor=24&size=10

// 响应 data
{
  "items": [ ... ],          // 当前页数据
  "nextCursor": 35,          // 下一页游标；为 null 表示最后一页
  "hasMore": true
}
```

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `cursor` | number | 否 | 0 | 上一页最后一条的 `id`；首页不传或传 0 |
| `size` | number | 否 | 10 | 每页条数；最大 50 |

---

## 2. 接口分组与清单总览

| 分组 | 路径前缀 | 主要方法 |
|------|----------|----------|
| **① 账号接口** | `/api/auth` | 注册、登录、登出 |
| **② 分类接口** | `/api/categories` | 一级分类列表、二级分类列表 |
| **③ 商品接口** | `/api/products` | 上架商品列表、推荐商品、商品详情、商品评价列表 |
| **④ 购物车接口** | `/api/cart` | 列出、加购、修改数量、删除项、清空 |
| **⑤ 订单接口** | `/api/orders` | 创建订单、模拟付款、我的订单列表、订单详情、取消订单、确认收货 |
| **⑥ 评价接口** | `/api/evaluations` | 提交评价 |
| **⑦ 管理员后台接口** | `/api/admin` | 商品 CRUD + 上下架、全部订单列表 + 详情 + 标记发货 |

### 权限速查表

| 接口分组 | Visitor | NormalUser | Admin |
|----------|:-------:|:----------:|:-----:|
| ① 注册 | ✓ | ✗ | ✗ |
| ① 登录 | ✓ | ✓ | ✓* |
| ① 登出 | ✗ | ✓ | ✓ |
| ② 分类 | ✓ | ✓ | ✗ |
| ③ 商品 | ✓ | ✓ | ✗ |
| ④ 购物车 | ✗ | ✓ | ✗ |
| ⑤ 订单（前台） | ✗ | ✓ | ✗ |
| ⑥ 评价 | ✗ | ✓ | ✗ |
| ⑦ 管理员后台 | ✗ | ✗ | ✓ |

> *管理员登录走相同接口，但在**前台 UI 上不暴露管理员登录入口**；若直接请求 API 可以成功登录并获得 ADMIN token。

---

## 3. 接口详细规范

---

### 3.1 账号接口 `/api/auth`

#### 3.1.1 注册

```
POST /api/auth/register
```

**请求体（JSON）：**

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|:----:|------|------|
| `username` | string | ✅ | 3~32 字符，唯一 | 全局唯一用户名 |
| `password` | string | ✅ | ≥ 8 字符 | 后端 BCrypt 哈希；**前端传明文** |

**权限**：Visitor

**业务前置校验：**
1. `username` 格式校验（非空、长度、唯一）
2. `password` 长度校验
3. **强制**写入 `role = 'NORMAL_USER'`，请求 payload 中的 role 字段被忽略

**成功响应（code=0）：**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 101,
    "username": "alice",
    "role": "NORMAL_USER",
    "createdAt": "2026-10-29T14:30:52"
  }
}
```

**失败场景：**

| code | message 示例 | 触发 |
|------|--------------|------|
| 409 | 该用户名已被占用 | 重复用户名 |
| 400 | 用户名长度需 3~32 字符 | 校验失败 |
| 400 | 密码长度至少 8 字符 | 校验失败 |

---

#### 3.1.2 登录

```
POST /api/auth/login
```

**请求体（JSON）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| `username` | string | ✅ | — |
| `password` | string | ✅ | 明文 |

**权限**：Visitor

**业务前置校验：**
1. `username` 存在 → BCrypt 校验密码 → 生成 JWT（`jti`, `userId`, `username`, `role`, `exp`）
2. 不存在或密码错误 → 401
3. 管理员账号**允许通过此接口登录**（返回 role=ADMIN）

**成功响应（code=0）：**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 101,
    "username": "alice",
    "role": "NORMAL_USER",
    "expiresAt": "2026-11-05T14:30:52"
  }
}
```

**失败场景：**

| code | message 示例 |
|------|--------------|
| 401 | 用户名或密码错误 |

---

#### 3.1.3 登出

```
POST /api/auth/logout
```

**请求头**：`Authorization: Bearer <token>`

**请求体**：无

**权限**：NormalUser / Admin

**业务前置校验：**
1. JWT 解析 → 取 `jti` 写入 `revoked_tokens` 黑名单表
2. token 已失效（黑名单中）→ 仍返回 200（幂等）

**成功响应（code=0）：**

```json
{ "code": 0, "message": "ok", "data": null }
```

---

### 3.2 分类接口 `/api/categories`

#### 3.2.1 列出一级分类

```
GET /api/categories/top
```

**权限**：Visitor / NormalUser

**响应（code=0）：**

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    { "id": 1, "name": "文具用品", "sortOrder": 1 },
    { "id": 2, "name": "办公用品", "sortOrder": 2 },
    { "id": 3, "name": "学生用品", "sortOrder": 3 }
  ]
}
```

#### 3.2.2 列出指定一级分类下的二级分类

```
GET /api/categories/{parentId}/sub
```

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `parentId` | number | 一级分类 id |

**权限**：Visitor / NormalUser

**业务前置校验**：`parentId` 对应的分类必须是一级分类（`parent_id IS NULL`），否则返回 404

**响应（code=0）：**

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    { "id": 10, "name": "圆珠笔", "parentId": 1, "sortOrder": 1 },
    { "id": 11, "name": "笔记本", "parentId": 1, "sortOrder": 2 }
  ]
}
```

---

### 3.3 商品接口 `/api/products`

#### 3.3.1 列出上架商品（前台）

```
GET /api/products
```

**Query 参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| `categoryId` | number | 否 | 二级分类 id；仅展示该分类下上架商品 |
| `cursor` | number | 否 | 游标分页 |
| `size` | number | 否 | 每页条数，默认 10，最大 50 |

**权限**：Visitor / NormalUser

**业务前置校验：**
1. **SQL 强制 `WHERE on_shelf = 1`**（Mapper XML 层硬编码）
2. 指定 `categoryId` 时校验分类存在性；查不到商品返回空列表（不是 404）

**响应（code=0）：**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "items": [
      {
        "id": 5,
        "name": "晨光黑色圆珠笔 0.5mm",
        "price": 3.50,
        "stock": 120,
        "categoryId": 10,
        "imageUrl": "/images/pen-black.webp",
        "onShelf": true
      }
    ],
    "nextCursor": 42,
    "hasMore": true
  }
}
```

#### 3.3.2 推荐商品（首页）

```
GET /api/products/recommended
```

**权限**：Visitor / NormalUser

**业务前置校验**：
1. SQL `WHERE on_shelf = 1 ORDER BY RAND() LIMIT 8`（或按固定 seed），返回 3~8 件

**响应**：同 3.3.1 items 数组，外层直接是数组（无 cursor/hasMore）

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    { "id": 5, "name": "...", "price": 3.50, "stock": 120, "imageUrl": "...", "onShelf": true }
  ]
}
```

#### 3.3.3 商品详情

```
GET /api/products/{id}
```

**路径参数**：`id` — 商品 id

**权限**：Visitor / NormalUser

**业务前置校验：**
1. 商品不存在 → 404
2. 商品下架（`on_shelf = 0`）→ **200 但返回 `onShelf: false` + 明确提示 message**（方便前端展示"该商品已下架"）
3. 同时返回该商品的评价列表（嵌套）

**响应（code=0）：**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 5,
    "name": "晨光黑色圆珠笔 0.5mm",
    "price": 3.50,
    "stock": 120,
    "categoryId": 10,
    "categoryName": "圆珠笔",
    "description": "标准书写用笔，0.5mm 笔尖",
    "imageUrl": "/images/pen-black.webp",
    "onShelf": true,
    "evaluations": [
      {
        "id": 301,
        "rating": 5,
        "content": "很好用！",
        "username": "alice",
        "createdAt": "2026-10-29T16:00:00"
      }
    ],
    "evaluationCount": 42,
    "averageRating": 4.6
  }
}
```

#### 3.3.4 下架商品直链响应示例

```json
{
  "code": 0,
  "message": "该商品已下架，暂不可购买",
  "data": {
    "id": 99,
    "name": "已下架钢笔",
    "price": 29.90,
    "stock": 50,
    "categoryId": 11,
    "onShelf": false,
    "description": null,
    "imageUrl": null,
    "evaluations": [],
    "evaluationCount": 0,
    "averageRating": 0
  }
}
```

---

### 3.4 购物车接口 `/api/cart`

所有接口需 **Authorization Bearer token**。

#### 3.4.1 列出购物车

```
GET /api/cart/items
```

**权限**：NormalUser（强制 `userId = token.userId` 校验）

**业务前置校验**：
1. 自动过滤已下架商品（`cart_items JOIN products WHERE on_shelf = 1`）
2. 在响应中附带每条的当前小计和购物车合计金额（服务端实时计算，不用前端 total）

**响应（code=0）：**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "items": [
      {
        "id": 201,
        "productId": 5,
        "productName": "晨光黑色圆珠笔 0.5mm",
        "price": 3.50,
        "stock": 120,
        "quantity": 2,
        "subtotal": 7.00
      }
    ],
    "totalAmount": 7.00
  }
}
```

#### 3.4.2 加入购物车

```
POST /api/cart/items
```

**请求体：**

| 字段 | 类型 | 必填 | 约束 |
|------|------|:----:|------|
| `productId` | number | ✅ | 商品必须存在、上架 |
| `quantity` | number | ✅ | ≥ 1，INT |

**权限**：NormalUser

**业务前置校验：**
1. 商品存在 + `on_shelf = 1` + `stock >= quantity`
2. 下架 → 400 "该商品已下架"；库存不足 → 400 "库存不足，仅剩 N 件"
3. 同一商品已在购物车 → **叠加数量**（INSERT ... ON DUPLICATE KEY UPDATE 或先 SELECT 再 INSERT/UPDATE）

**成功响应**：同 3.4.1 的单条 item

**失败示例：**

```json
{ "code": 400, "message": "商品 5 库存不足，仅剩 3 件", "data": null }
{ "code": 400, "message": "该商品已下架，无法加入购物车", "data": null }
```

#### 3.4.3 修改购物车项数量

```
PUT /api/cart/items/{id}
```

**路径参数**：`id` — cart_item id

**请求体：**

| 字段 | 类型 | 必填 | 约束 |
|------|------|:----:|------|
| `quantity` | number | ✅ | ≥ 1，≤ product.stock |

**权限**：NormalUser（校验 `cart_item.user_id = token.userId`）

**业务前置校验：**
1. cart_item 存在且属于当前用户 → 否则 404 / 403
2. 新数量 ≤ product.stock → 否则 400
3. 关联商品仍上架 → 否则 400

**成功响应**：同 3.4.2

#### 3.4.4 删除购物车项

```
DELETE /api/cart/items/{id}
```

**权限**：NormalUser（归属权校验）

**成功响应：** `{ "code": 0, "message": "ok", "data": null }`

#### 3.4.5 清空购物车

```
DELETE /api/cart
```

**权限**：NormalUser

**成功响应：** `{ "code": 0, "message": "ok", "data": null }`

---

### 3.5 订单接口 `/api/orders`（前台）

所有接口需 **Authorization Bearer token** + **NormalUser 权限** + **Service 层归属权校验**。

#### 3.5.1 创建订单（结算，初始状态 UNPAID）

```
POST /api/orders
```

**请求体：**

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|:----:|------|------|
| `cartItemIds` | number[] | ✅ | ≥ 1 项 | 购物车中勾选的行 id |
| `address` | string | ✅ | ≤ 256 字符 | 收货地址文本 |
| `remark` | string | 否 | ≤ 256 字符 | 备注选填 |
| ~~`totalAmount`~~ | — | — | — | **前端传来忽略**，后端重算 |

**权限**：NormalUser（Admin 调用 → 403）

**业务前置校验（** `@Transactional(rollbackFor = Exception.class)` 内 **）：**
1. 所有 `cartItemIds` 必须属于当前用户，且关联商品 `on_shelf = 1`
2. 每行 `quantity <= product.stock`（二次校验，防并发）
3. 扣库存（`UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?`，检查影响行数）
4. INSERT `orders`：`status = 'UNPAID'`（模拟付款为独立动作，见 3.5.2）；`total_amount` 服务端重算
5. INSERT `order_items`：**事务内快照** product.name + product.price
6. DELETE 已勾选的 `cart_items`
7. 失败任一 → 全部回滚

**成功响应（code=0）：**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 5001,
    "orderNo": "SO20261029143052001",
    "status": "UNPAID",
    "totalAmount": 20.00,
    "address": "北京市朝阳区某某路 1 号",
    "createdAt": "2026-10-29T14:30:52",
    "paidAt": null,
    "items": [
      {
        "id": 8001,
        "productId": 5,
        "productName": "晨光黑色圆珠笔 0.5mm",
        "unitPrice": 3.50,
        "quantity": 2,
        "subtotal": 7.00
      }
    ]
  }
}
```

**失败示例：**

```json
{
  "code": 400,
  "message": "以下商品库存不足：[商品A 仅剩 3 件]",
  "data": null
}
```

#### 3.5.2 模拟付款（UNPAID → PAID）

```
POST /api/orders/{id}/pay
```

**路径参数**：`id` — 订单 id

**权限**：NormalUser（归属权校验）

**业务前置校验：**
1. 订单存在 + `user_id = token.userId` → 否则 403
2. 订单状态必须是 `UNPAID`（已付款不可重复付款，SHIPPED / COMPLETED / CANCELLED 不可付款）→ 否则 400 "当前状态不允许付款"
3. `UPDATE orders SET status = 'PAID', paid_at = NOW()`

**成功响应：** 同 3.5.4（更新后的详情）

#### 3.5.3 我的订单列表

```
GET /api/orders
```

**Query 参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| `status` | string | 否 | 按订单状态筛选：UNPAID / PAID / SHIPPED / COMPLETED / CANCELLED |
| `cursor` | number | 否 | 游标，按 `created_at DESC, id DESC` |
| `size` | number | 否 | 默认 10，最大 50 |

**权限**：NormalUser（强制 `WHERE user_id = token.userId`）

**业务前置校验**：不允许通过此接口查他人订单；`status` 值在枚举范围内

**响应：**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "items": [
      {
        "id": 5001,
        "orderNo": "SO20261029143052001",
        "status": "PAID",
        "totalAmount": 20.00,
        "itemCount": 3,
        "createdAt": "2026-10-29T14:30:52",
        "canCancel": false,
        "canConfirmReceipt": false,
        "canEvaluate": false
      }
    ],
    "nextCursor": 5000,
    "hasMore": true
  }
}
```

> `canCancel` / `canConfirmReceipt` / `canEvaluate` 是**服务端计算好的布尔值**，前端直接读即可，不要再自己判断状态

#### 3.5.4 订单详情

```
GET /api/orders/{id}
```

**路径参数**：`id` — 订单 id

**权限**：NormalUser

**业务前置校验**：Service 层必须 `WHERE id = ? AND user_id = ?` → 查不到返回 403（防止越权枚举）

**响应（code=0）：**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 5001,
    "orderNo": "SO20261029143052001",
    "status": "SHIPPED",
    "totalAmount": 20.00,
    "address": "北京市朝阳区某某路 1 号",
    "remark": null,
    "createdAt": "2026-10-29T14:30:52",
    "paidAt": "2026-10-29T14:30:52",
    "shippedAt": "2026-10-29T16:00:00",
    "completedAt": null,
    "cancelledAt": null,
    "items": [
      {
        "id": 8001,
        "productId": 5,
        "productName": "晨光黑色圆珠笔 0.5mm",
        "unitPrice": 3.50,
        "quantity": 2,
        "subtotal": 7.00,
        "evaluated": false
      }
    ],
    "canCancel": false,
    "canConfirmReceipt": true,
    "canEvaluate": false
  }
}
```

#### 3.5.5 取消订单（仅限 UNPAID）

```
POST /api/orders/{id}/cancel
```

**路径参数**：`id` — 订单 id

**权限**：NormalUser（归属权校验）

**业务前置校验（** `@Transactional` **）：**
1. 订单存在 + `user_id = token.userId`
2. 订单状态必须是 `UNPAID` → 否则 400 "当前状态不允许取消"
3. **库存回补**：`UPDATE products SET stock = stock + #{qty} WHERE id = #{productId}`
4. `UPDATE orders SET status = 'CANCELLED', cancelled_at = NOW()`

**成功响应：** 同 3.5.4（更新后的详情）

#### 3.5.6 确认收货（仅限 SHIPPED → COMPLETED）

```
POST /api/orders/{id}/confirm-receipt
```

**路径参数**：`id` — 订单 id

**权限**：NormalUser（归属权校验）

**业务前置校验：**
1. 订单存在 + `user_id = token.userId`
2. 订单状态必须是 `SHIPPED` → 否则 400 "订单尚未发货，无法确认收货"
3. `UPDATE orders SET status = 'COMPLETED', completed_at = NOW()`

**成功响应：** 同 3.5.4

---

### 3.6 评价接口 `/api/evaluations`

#### 3.6.1 提交评价

```
POST /api/evaluations
```

**请求体：**

| 字段 | 类型 | 必填 | 约束 |
|------|------|:----:|------|
| `orderId` | number | ✅ | 订单必须属于当前用户 + 状态 = COMPLETED |
| `productId` | number | ✅ | 必须存在于该订单的 order_items 中 |
| `rating` | number | ✅ | 1~5 整数 |
| `content` | string | 否 | ≤ 500 字符，纯文字，**不支持图片** |

**权限**：NormalUser

**业务前置校验：**
1. 订单存在 + `user_id = token.userId` + `status = COMPLETED`
2. productId 必须在 order_items 中
3. **唯一键拦截**：`(order_id, product_id, user_id)` 已存在 → 409 "您已评价过该商品"
4. 未完成订单 → 400 "订单未完成，无法评价"

**成功响应（code=0）：**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 301,
    "orderId": 5001,
    "productId": 5,
    "userId": 101,
    "rating": 5,
    "content": "很好用！",
    "createdAt": "2026-10-29T16:00:00"
  }
}
```

---

### 3.7 管理员后台接口 `/api/admin`

所有接口需 **Authorization Bearer token + `role = 'ADMIN'`**。

#### 3.7.1 管理员：商品列表（含已下架）

```
GET /api/admin/products
```

**Query 参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| `categoryId` | number | 否 | 按二级分类筛选 |
| `onShelf` | boolean | 否 | `true`=只看上架 / `false`=只看下架 / 不传=全部 |
| `cursor` / `size` | number | 否 | 游标分页 |

**权限**：Admin（NormalUser 调用 → 403）

**响应：** 同 3.3.1 items 结构，但包含已下架商品，额外返回 `onShelf` 状态列

#### 3.7.2 管理员：新增商品

```
POST /api/admin/products
```

**请求体：**

| 字段 | 类型 | 必填 | 约束 |
|------|------|:----:|------|
| `name` | string | ✅ | ≤ 128 字符 |
| `price` | number | ✅ | `DECIMAL(10,2)`，> 0 |
| `stock` | number | ✅ | ≥ 0 整数 |
| `categoryId` | number | ✅ | **必须指向二级分类**（Service 校验 `parent_id IS NOT NULL`） |
| `description` | string | 否 | TEXT |
| `imageUrl` | string | 否 | ≤ 512 字符；或用 multipart 文件上传后回填 |
| `onShelf` | boolean | 否 | 默认 `true` |

**权限**：Admin

**业务前置校验：**
1. categoryId 存在且是二级分类 → 否则 400
2. price > 0, stock ≥ 0

**成功响应：** 返回创建后的完整商品对象

#### 3.7.3 管理员：编辑商品

```
PUT /api/admin/products/{id}
```

**请求体**：同 3.7.2（所有字段可选，传了才更新）

**权限**：Admin

**业务前置校验：**
1. 商品必须存在 → 404
2. 编辑后库存不能 < 0
3. categoryId 变更时仍必须是二级分类

#### 3.7.4 管理员：上架 / 下架切换

```
PATCH /api/admin/products/{id}/on-shelf
```

**请求体：**

```json
{ "onShelf": false }
```

**权限**：Admin

**业务前置校验：** 商品必须存在

**成功响应：** `{ "code": 0, "message": "ok", "data": { "id": 5, "onShelf": false } }`

#### 3.7.5 管理员：全部订单列表

```
GET /api/admin/orders
```

**Query 参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| `status` | string | 否 | 按订单状态筛选 |
| `cursor` / `size` | number | 否 | 游标分页 |

**权限**：Admin

**业务前置校验**：无归属权限制（Admin 看全部）

**响应：**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "items": [
      {
        "id": 5001,
        "orderNo": "SO20261029143052001",
        "userId": 101,
        "username": "alice",
        "status": "PAID",
        "totalAmount": 20.00,
        "itemCount": 3,
        "createdAt": "2026-10-29T14:30:52"
      }
    ],
    "nextCursor": 5000,
    "hasMore": true
  }
}
```

#### 3.7.6 管理员：订单详情

```
GET /api/admin/orders/{id}
```

**权限**：Admin

**业务前置校验**：无归属权限制；订单不存在 → 404

**响应**：同 3.5.4 订单详情结构，额外返回 `username`

#### 3.7.7 管理员：标记发货

```
POST /api/admin/orders/{id}/ship
```

**路径参数**：`id` — 订单 id

**权限**：Admin

**业务前置校验：**
1. 订单存在
2. 订单状态必须是 `PAID` → 否则 400 "当前状态不允许标记发货"
3. 终态订单（COMPLETED / CANCELLED）→ 400 "终态订单不可修改"
4. `UPDATE orders SET status = 'SHIPPED', shipped_at = NOW()`

**成功响应：** 返回更新后的订单详情

**失败示例：**

```json
{ "code": 400, "message": "订单当前状态为 SHIPPED，不允许标记发货", "data": null }
{ "code": 400, "message": "终态订单不可修改", "data": null }
```

---

## 4. 接口 × 权限速查表（一页纸）

| 分组 | 接口 | Method | Path | Visitor | NormalUser | Admin |
|------|------|--------|------|:-------:|:----------:|:-----:|
| 账号 | 注册 | POST | `/api/auth/register` | ✅ | ❌ | ❌ |
| 账号 | 登录 | POST | `/api/auth/login` | ✅ | ✅ | ✅ |
| 账号 | 登出 | POST | `/api/auth/logout` | ❌ | ✅ | ✅ |
| 分类 | 一级列表 | GET | `/api/categories/top` | ✅ | ✅ | ✗ |
| 分类 | 二级列表 | GET | `/api/categories/{parentId}/sub` | ✅ | ✅ | ✗ |
| 商品 | 上架列表 | GET | `/api/products` | ✅ | ✅ | ✗ |
| 商品 | 推荐商品 | GET | `/api/products/recommended` | ✅ | ✅ | ✗ |
| 商品 | 商品详情 | GET | `/api/products/{id}` | ✅ | ✅ | ✗ |
| 购物车 | 列出 | GET | `/api/cart/items` | ❌ | ✅ | ❌ |
| 购物车 | 加购 | POST | `/api/cart/items` | ❌ | ✅ | ❌ |
| 购物车 | 改数量 | PUT | `/api/cart/items/{id}` | ❌ | ✅ | ❌ |
| 购物车 | 删除项 | DELETE | `/api/cart/items/{id}` | ❌ | ✅ | ❌ |
| 购物车 | 清空 | DELETE | `/api/cart` | ❌ | ✅ | ❌ |
| 订单 | 创建订单 | POST | `/api/orders` | ❌ | ✅ | ❌ |
| 订单 | 模拟付款 | POST | `/api/orders/{id}/pay` | ❌ | ✅* | ❌ |
| 订单 | 我的订单列表 | GET | `/api/orders` | ❌ | ✅ | ❌ |
| 订单 | 订单详情 | GET | `/api/orders/{id}` | ❌ | ✅* | ❌ |
| 订单 | 取消订单 | POST | `/api/orders/{id}/cancel` | ❌ | ✅* | ❌ |
| 订单 | 确认收货 | POST | `/api/orders/{id}/confirm-receipt` | ❌ | ✅* | ❌ |
| 评价 | 提交评价 | POST | `/api/evaluations` | ❌ | ✅ | ❌ |
| 后台 | 商品列表 | GET | `/api/admin/products` | ❌ | ❌ | ✅ |
| 后台 | 新增商品 | POST | `/api/admin/products` | ❌ | ❌ | ✅ |
| 后台 | 编辑商品 | PUT | `/api/admin/products/{id}` | ❌ | ❌ | ✅ |
| 后台 | 上下架切换 | PATCH | `/api/admin/products/{id}/on-shelf` | ❌ | ❌ | ✅ |
| 后台 | 全部订单列表 | GET | `/api/admin/orders` | ❌ | ❌ | ✅ |
| 后台 | 订单详情 | GET | `/api/admin/orders/{id}` | ❌ | ❌ | ✅ |
| 后台 | 标记发货 | POST | `/api/admin/orders/{id}/ship` | ❌ | ❌ | ✅ |

> *`✅*` 表示除登录态 + 角色外，**Service 层还做归属权校验**（`user_id = token.userId`），越权返回 403

---

*—— END OF API SPEC ——*
