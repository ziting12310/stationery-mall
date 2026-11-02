import { request } from '../utils/request'

/* ============================== 类型定义 ============================== */

/** 登录 / 注册请求体 */
export interface AuthPayload {
  username: string
  password: string
}

/** 当前登录用户信息（role 为后端枚举名：NORMAL_USER=普通用户，ADMIN=管理员） */
export interface UserInfo {
  id: number
  username: string
  role: string
}

/** 登录返回（后端 LoginVO 为扁平结构） */
export interface LoginResult {
  token: string
  userId: number
  username: string
  role: string
  roleDesc?: string
}

/** 分类（一级分类 children 嵌套二级分类） */
export interface Category {
  id: number
  name: string
  parentId?: number | null
  children?: Category[]
}

/** 商品（列表与详情共用字段，详情可能多出 description） */
export interface Goods {
  id: number
  name: string
  price: number
  imageUrl?: string
  stock: number
  categoryId?: number
  description?: string
  /** 上架状态：前台列表均为 true；后台列表（/admin/products）可能为 false */
  onShelf?: boolean
}

/** 加入购物车请求体 */
export interface CartAddPayload {
  goodsId: number
  num: number
}

/** 购物车项（兼容 productId/goodsId、quantity/num 两种命名） */
export interface CartItem {
  id: number
  goodsId?: number
  productId?: number
  goodsName?: string
  productName?: string
  price: number
  num?: number
  quantity?: number
  stock?: number
  imageUrl?: string
}

/** 创建订单请求体（地址必填，备注选填） */
export interface OrderCreatePayload {
  cartItemIds: number[]
  address: string
  remark?: string
}

/* ============================== 账号接口 ============================== */

/** 登录：POST /auth/login */
export function login(data: AuthPayload): Promise<LoginResult> {
  return request<LoginResult>({ url: '/auth/login', method: 'post', data })
}

/** 注册：POST /auth/register */
export function register(data: AuthPayload): Promise<void> {
  return request<void>({ url: '/auth/register', method: 'post', data })
}

/** 登出：POST /auth/logout（后端使该用户全部 token 失效） */
export function logout(): Promise<void> {
  return request<void>({ url: '/auth/logout', method: 'post' })
}

/* ============================== 分类 & 商品 ============================== */

/**
 * 分类列表（一级嵌套二级）。
 * 后端拆分为两个接口：GET /categories/top 一级分类、GET /categories/{parentId}/sub 二级分类，
 * 此处聚合为树，供首页左侧菜单直接使用。
 */
export async function getCategoryList(): Promise<Category[]> {
  const tops = await request<Category[]>({ url: '/categories/top', method: 'get' })
  return Promise.all(
    (tops || []).map(async (top) => ({
      ...top,
      children: await request<Category[]>({ url: `/categories/${top.id}/sub`, method: 'get' })
    }))
  )
}

/** 上架商品列表：GET /products（后端返回游标分页 {items,nextCursor,hasMore}，首页取 items） */
export async function getGoodsList(categoryId?: number): Promise<Goods[]> {
  const page = await request<{ items: Goods[] }>({
    url: '/products',
    method: 'get',
    params: { categoryId: categoryId || undefined, size: 50 }
  })
  return page?.items || []
}

/** 后台商品列表（含已下架）：GET /admin/products（仅管理员，后端会二次校验角色） */
export async function getAdminGoodsList(): Promise<Goods[]> {
  const page = await request<{ items: Goods[] }>({
    url: '/admin/products',
    method: 'get',
    params: { size: 50 }
  })
  return page?.items || []
}

/** 商品名称模糊搜索：GET /product/search?keyword=（仅上架，分页结构同商品列表） */
export async function searchGoods(keyword: string): Promise<Goods[]> {
  const page = await request<{ items: Goods[] }>({
    url: '/product/search',
    method: 'get',
    params: { keyword, size: 50 }
  })
  return page?.items || []
}

/** 管理员上架 / 下架：POST /products/{id}/on-shelf 或 /products/{id}/off-shelf（后端校验 ADMIN，普通用户 403） */
export function toggleGoodsOnShelf(id: number, onShelf: boolean): Promise<unknown> {
  return request({
    url: `/products/${id}/${onShelf ? 'on-shelf' : 'off-shelf'}`,
    method: 'post'
  })
}

/** 商品详情：GET /products/{id}（后端返回 {message, data} 结构，下架商品也在 data 中返回） */
export async function getGoodsDetail(id: number | string): Promise<Goods> {
  const detail = await request<{ message: string; data: Goods }>({
    url: `/products/${id}`,
    method: 'get'
  })
  console.log("api拿到的原始detail：", detail) //新增打印
  return detail
}

/* ============================== 购物车 ============================== */

/** 加入购物车：POST /cart/items（后端字段为 productId / quantity，这里做键名映射） */
export function addCart(data: CartAddPayload): Promise<unknown> {
  return request({
    url: '/cart/items',
    method: 'post',
    data: { productId: data.goodsId, quantity: data.num }
  })
}

/** 购物车列表：GET /cart/items（后端返回 {items, totalAmount}） */
export async function getCartList(): Promise<CartItem[]> {
  const summary = await request<{ items: CartItem[]; totalAmount: number }>({
    url: '/cart/items',
    method: 'get'
  })
  return summary?.items || []
}

/** 修改购物车项数量：PUT /cart/items/{id} */
export function updateCartQuantity(id: number, quantity: number): Promise<unknown> {
  return request({ url: `/cart/items/${id}`, method: 'put', data: { quantity } })
}

/** 删除购物车项：DELETE /cart/items/{id} */
export function removeCartItem(id: number): Promise<void> {
  return request<void>({ url: `/cart/items/${id}`, method: 'delete' })
}

/* ============================== 订单 ============================== */

/** 创建订单：POST /orders（后端扣减库存、服务端重算金额） */
export function createOrder(data: OrderCreatePayload): Promise<unknown> {
  return request({ url: '/orders', method: 'post', data })
}
