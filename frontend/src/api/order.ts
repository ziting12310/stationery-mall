import { request } from '../utils/request'

/* ============================== 类型定义 ============================== */

/** 订单明细（商品快照） */
export interface OrderItemVO {
  id: number
  productId: number
  productName: string
  unitPrice: number
  quantity: number
  subtotal: number
  /** 商品介绍（后端关联商品表返回，可能为空） */
  productIntro?: string
  /** 该商品是否已评价（后端按评价表回填） */
  evaluated?: boolean
  /** 已评价时的评价文字（未评价为空） */
  evaluationContent?: string
}

/** 订单（status 与后端 OrderStatusEnum 一致：0 待付款 1 待发货 2 已发货 3 已完成 4 已取消） */
export interface OrderVO {
  id: number
  orderNo: string
  userId: number
  username?: string
  status: number
  totalAmount: number
  address?: string
  remark?: string
  createdAt: string
  items?: OrderItemVO[]
}

/** 评价状态（GET /evaluations/status 返回：hasComment 是否已评价，content 评价文字） */
export interface EvaluationStatusVO {
  hasComment: boolean
  content?: string
}

/** 评价请求体 */
export interface EvaluationPayload {
  orderId: number
  productId: number
  rating: number
  content?: string
}

/* ============================== 订单接口（真实后端） ============================== */

/** 我的订单列表：GET /orders（后端返回游标分页 {items,nextCursor,hasMore}，这里取 items） */
export async function listMyOrders(): Promise<OrderVO[]> {
  const page = await request<{ items: OrderVO[] }>({
    url: '/orders',
    method: 'get',
    params: { size: 50 }
  })
  return page?.items || []
}

/** 全部订单列表（管理员）：GET /admin/orders */
export async function listAllOrders(): Promise<OrderVO[]> {
  const page = await request<{ items: OrderVO[] }>({
    url: '/admin/orders',
    method: 'get',
    params: { size: 50 }
  })
  return page?.items || []
}

/** 模拟付款：POST /orders/pay?orderId=xxx（仅待付款订单可付款，归属权由后端校验） */
export function payOrder(id: number): Promise<unknown> {
  return request({ url: '/orders/pay', method: 'post', params: { orderId: id } })
}

/** 取消订单：POST /orders/{id}/cancel（仅待付款可取消） */
export function cancelOrder(id: number): Promise<unknown> {
  return request({ url: `/orders/${id}/cancel`, method: 'post' })
}

/** 确认收货：POST /orders/{id}/confirm-receipt（仅已发货可确认） */
export function confirmReceipt(id: number): Promise<unknown> {
  return request({ url: `/orders/${id}/confirm-receipt`, method: 'post' })
}

/** 标记发货（管理员）：POST /admin/orders/{id}/ship（仅待发货可发货） */
export function shipOrder(id: number): Promise<unknown> {
  return request({ url: `/admin/orders/${id}/ship`, method: 'post' })
}

/** 提交评价：POST /evaluations */
export function submitEvaluation(data: EvaluationPayload): Promise<unknown> {
  return request({ url: '/evaluations', method: 'post', data })
}

/** 查询评价状态：GET /evaluations/status?orderId=&productId=&userId=
 *  （静默请求，失败不弹提示；管理员不传 userId，后端仅按 orderId + productId 查询） */
export function getEvaluationStatus(
  orderId: number,
  productId: number,
  userId?: number
): Promise<EvaluationStatusVO> {
  return request({
    url: '/evaluations/status',
    method: 'get',
    params: { orderId, productId, userId },
    silent: true
  })
}
