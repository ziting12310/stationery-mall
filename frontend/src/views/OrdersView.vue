/**
 * 我的订单页（新增文件，不改动任何原有页面）
 *
 * 业务规则：
 * - 普通用户：只看自己的订单；待付款可取消；已发货可确认收货；已完成可评价
 * - 管理员：看全部订单；仅待发货可标记发货
 *
 * 数据来源：
 * - USE_MOCK = true  → 渲染本地 mock 假订单，操作只打印日志 + 页面提示（答辩演示用）
 * - USE_MOCK = false → 走 api/order.ts 的真实后端接口（联调完成后改成 false 即可）
 */
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { USER_INFO_KEY } from '../utils/request'
import type { UserInfo } from '../api/auth'
// 真实接口（mock 阶段不执行，保留用于替换）
import {
  cancelOrder,
  confirmReceipt,
  getEvaluationStatus,
  listMyOrders,
  payOrder,
  shipOrder,
  submitEvaluation,
  type OrderItemVO,
  type OrderVO
} from '../api/order'

const router = useRouter()

/** 是否使用本地 mock 数据兜底：false = 调用真实后端订单接口（GET /orders 或 GET /admin/orders） */
const USE_MOCK = false

/** 当前登录用户（localStorage 由登录页写入） */
const user = ref<UserInfo | null>(null)
/** 订单列表 */
const orders = ref<OrderVO[]>([])
const loading = ref(false)

/** 评价草稿（key = 订单明细 id）：每个商品独立输入文字评价 */
const evalDraft = reactive<Record<number, string>>({})
/** 评价提交中（key = 订单明细 id）：控制提交按钮 loading */
const evalSubmitting = reactive<Record<number, boolean>>({})

/** 当前用户 id 与是否为管理员 */
const userId = computed<number>(() => Number(user.value?.id ?? 0))
const isAdmin = computed<boolean>(() => user.value?.role === 'ADMIN')

/** 取订单归属用户 id（兼容后端 camelCase：userId；若无则回退 snake_case：user_id） */
function orderOwnerId(order: OrderVO): number {
  const raw = order.userId ?? (order as unknown as { user_id?: number }).user_id
  return Number(raw ?? 0)
}

/**
 * 按角色过滤订单。
 * - 普通用户：只保留 order.user_id === 当前登录用户 id 的订单
 * - 管理员：不过滤，返回全部订单
 */
function filterOrdersForCurrentUser(list: OrderVO[]): OrderVO[] {
  if (isAdmin.value) {
    return list
  }
  return list.filter((order) => orderOwnerId(order) === userId.value)
}

/** 可见订单：管理员看全部，普通用户只看自己 */
const visibleOrders = computed<OrderVO[]>(() => filterOrdersForCurrentUser(orders.value))

/* ============================== 状态映射 ============================== */

/** 状态文案与配色（待付款蓝 / 待发货橙 / 已发货黑 / 已完成绿 / 已取消灰） */
const STATUS_MAP: Record<number, { text: string; className: string }> = {
  0: { text: '待付款', className: 'status-unpaid' },
  1: { text: '待发货', className: 'status-paid' },
  2: { text: '已发货', className: 'status-shipped' },
  3: { text: '已完成', className: 'status-completed' },
  4: { text: '已取消', className: 'status-cancelled' }
}

function statusText(status: number): string {
  return STATUS_MAP[status]?.text ?? '未知状态'
}

function statusClass(status: number): string {
  return STATUS_MAP[status]?.className ?? 'status-cancelled'
}

/** 后端 OrderVO.status 序列化为枚举名（UNPAID/PAID/SHIPPED/COMPLETED/CANCELLED），这里统一转成 0~4 数字码 */
const STATUS_CODE: Record<string, number> = {
  UNPAID: 0,
  PAID: 1,
  SHIPPED: 2,
  COMPLETED: 3,
  CANCELLED: 4
}

/** 状态归一化：兼容后端枚举名与 mock 数字码 */
function normalizeStatus(raw: number | string): number {
  return typeof raw === 'number' ? raw : STATUS_CODE[String(raw)] ?? -1
}

/** 接口数据归一化：把枚举名状态改写为数字码，后续按钮判断与 mock 分支保持一致 */
function normalizeOrders(list: OrderVO[]): OrderVO[] {
  list.forEach((order) => {
    order.status = normalizeStatus(order.status as unknown as number | string)
  })
  return list
}

/* ============================== mock 数据 ============================== */

/** 构造 mock 订单（含当前用户与他人订单，便于验证 admin / 普通用户的可见范围） */
function buildMockOrders(currentUserId: number): OrderVO[] {
  const others = currentUserId === 1 ? 2 : 1
  return [
    {
      id: 1,
      orderNo: 'SO202610300001',
      userId: currentUserId,
      username: user.value?.username || 'alice',
      status: 0,
      totalAmount: 22.5,
      address: '上海市浦东新区世纪大道 100 号',
      createdAt: '2026-10-30 10:20:31',
      items: [
        { id: 101, productId: 1, productName: '晨光黑色圆珠笔 0.5mm', unitPrice: 3.5, quantity: 2, subtotal: 7 }
      ]
    },
    {
      id: 2,
      orderNo: 'SO202610300002',
      userId: currentUserId,
      username: user.value?.username || 'alice',
      status: 1,
      totalAmount: 32,
      address: '上海市浦东新区世纪大道 100 号',
      createdAt: '2026-10-29 09:05:12',
      items: [{ id: 102, productId: 2, productName: '晨光 A5 硬壳笔记本', unitPrice: 16, quantity: 2, subtotal: 32 }]
    },
    {
      id: 3,
      orderNo: 'SO202610280003',
      userId: currentUserId,
      username: user.value?.username || 'alice',
      status: 2,
      totalAmount: 15,
      address: '上海市徐汇区漕溪北路 88 号',
      createdAt: '2026-10-28 15:42:03',
      items: [{ id: 103, productId: 4, productName: '得力中性笔替芯 0.5mm', unitPrice: 1.5, quantity: 10, subtotal: 15 }]
    },
    {
      id: 4,
      orderNo: 'SO202610260004',
      userId: currentUserId,
      username: user.value?.username || 'alice',
      status: 3,
      totalAmount: 58.8,
      address: '上海市徐汇区漕溪北路 88 号',
      createdAt: '2026-10-26 20:11:45',
      items: [{ id: 104, productId: 6, productName: '晨光 4B 橡皮擦（2 块装）', unitPrice: 29.4, quantity: 2, subtotal: 58.8 }]
    },
    {
      id: 5,
      orderNo: 'SO202610250005',
      userId: others,
      username: 'bob',
      status: 1,
      totalAmount: 45,
      address: '北京市海淀区中关村大街 1 号',
      createdAt: '2026-10-25 11:30:00',
      items: [{ id: 105, productId: 8, productName: '得力订书机 标准型', unitPrice: 22.5, quantity: 2, subtotal: 45 }]
    },
    {
      id: 6,
      orderNo: 'SO202610240006',
      userId: others,
      username: 'bob',
      status: 4,
      totalAmount: 9.9,
      address: '北京市海淀区中关村大街 1 号',
      createdAt: '2026-10-24 08:15:20',
      items: [{ id: 106, productId: 9, productName: '真彩荧光笔（单支）', unitPrice: 3.3, quantity: 3, subtotal: 9.9 }]
    }
  ]
}

/* ============================== 加载 ============================== */

/** 读取本地登录用户信息 */
function loadUserInfo(): void {
  try {
    const raw = localStorage.getItem(USER_INFO_KEY)
    user.value = raw ? (JSON.parse(raw) as UserInfo) : null
  } catch {
    user.value = null
  }
}

/** 加载订单（mock / 真实接口两条路径） */
async function loadOrders(): Promise<void> {
  loading.value = true
  try {
    if (USE_MOCK) {
      // ---------- mock 分支：本地假数据，答辩演示用 ----------
      orders.value = buildMockOrders(userId.value || 1)
    } else {
      // ---------- 真实接口分支：统一走 GET /api/orders ----------
      // 后端按 JWT 角色判定：管理员返回全部订单，普通用户只返回本人订单，前端不传 userId
      const list = await listMyOrders()
      orders.value = normalizeOrders(list)
    }

    // 调试日志：当前登录用户 id + 原始数组 + 过滤后的数组
    console.log('[Orders] 当前登录用户 id：', userId.value, '是否管理员：', isAdmin.value)
    console.log('[Orders] 原始订单数组：', orders.value)
    console.log('[Orders] 过滤之后的订单数组：', filterOrdersForCurrentUser(orders.value))

    // 加载后逐订单读取评价状态：已评价商品直接展示评价文字并隐藏输入区
    await syncEvaluationsForOrders()
  } catch (err) {
    // 接口异常已由响应拦截器统一提示，这里仅控制台留档
    console.error('[Orders] 加载订单失败：', err)
  } finally {
    loading.value = false
  }
}

/* ============================== 订单操作（mock + 真实接口预留） ============================== */

/** 去付款（待付款） */
async function handlePay(order: OrderVO): Promise<void> {
  if (USE_MOCK) {
    console.log('[mock] 付款订单：', order.orderNo)
    order.status = 1
    ElMessage.success('付款成功（mock）')
    return
  }
  await payOrder(order.id)
  ElMessage.success('付款成功')
  // 付款成功后重新拉取订单列表，状态以后端返回为准
  await loadOrders()
}

/** 取消订单（待付款） */
async function handleCancel(order: OrderVO): Promise<void> {
  if (USE_MOCK) {
    console.log('[mock] 取消订单：', order.orderNo)
    order.status = 4
    ElMessage.success('订单已取消（mock）')
    return
  }
  await cancelOrder(order.id)
  ElMessage.success('订单已取消')
  await loadOrders()
}

/** 确认收货（已发货） */
async function handleConfirmReceipt(order: OrderVO): Promise<void> {
  if (USE_MOCK) {
    console.log('[mock] 确认收货：', order.orderNo)
    order.status = 3
    ElMessage.success('已确认收货（mock）')
    return
  }
  await confirmReceipt(order.id)
  ElMessage.success('已确认收货')
  await loadOrders()
}

/** 标记发货（管理员，待发货） */
async function handleShip(order: OrderVO): Promise<void> {
  if (USE_MOCK) {
    console.log('[mock] 标记发货：', order.orderNo)
    order.status = 2
    ElMessage.success('已标记发货（mock）')
    return
  }
  await shipOrder(order.id)
  ElMessage.success('已标记发货')
  await loadOrders()
}

/** 回读单个订单的评价状态：逐商品调用状态接口，已评价则回填评价文字 */
async function syncOrderEvaluations(order: OrderVO): Promise<void> {
  await Promise.all(
    (order.items ?? []).map(async (orderItem) => {
      try {
        // 管理员：只带 orderId + productId（查看该订单商品对应的用户评价）；
        // 普通用户：额外带 userId，只查自己的评价
        const status = isAdmin.value
          ? await getEvaluationStatus(order.id, orderItem.productId)
          : await getEvaluationStatus(order.id, orderItem.productId, userId.value)
        orderItem.evaluated = !!status?.hasComment
        orderItem.evaluationContent = status?.content || ''
      } catch {
        // 状态接口异常时静默处理（请求已带 silent）：不提示、不锁定，保留输入框
      }
    })
  )
}

/**
 * 页面加载订单后循环调用评价状态接口，
 * 已评价的商品直接展示评价文字并隐藏输入区（管理员只读、普通用户可提交）。
 */
async function syncEvaluationsForOrders(): Promise<void> {
  if (USE_MOCK) {
    return
  }
  // 仅已完成订单可评价/可查看评价，待付款、待发货不展示评价
  const targets = visibleOrders.value.filter((order) => order.status === 3 && order.items?.length)
  await Promise.all(targets.map((order) => syncOrderEvaluations(order)))
}

/** 评价（已完成订单）：逐商品提交文字评价，提交成功后切换为「已评价」只读态 */
async function handleEvaluate(order: OrderVO, item: OrderItemVO): Promise<void> {
  const content = (evalDraft[item.id] || '').trim()
  if (!content) {
    ElMessage.warning('请先写下评价内容')
    return
  }
  if (USE_MOCK) {
    console.log('[mock] 提交评价：', { orderId: order.id, productId: item.productId, rating: 5, content })
    item.evaluated = true
    item.evaluationContent = content
    ElMessage.success('评价成功（mock）')
    return
  }
  evalSubmitting[item.id] = true
  try {
    await submitEvaluation({ orderId: order.id, productId: item.productId, rating: 5, content })
    // 保存成功：回读服务端评价状态，输入区随 evaluated 切换为只读展示
    await syncOrderEvaluations(order)
    ElMessage.success('评价成功')
  } catch (err) {
    // 后端业务异常（HTTP 409「您已评价过该商品」）：提示已由响应拦截器弹出，这里同步状态并锁定评价区
    if ((err as { response?: { status?: number } }).response?.status === 409) {
      await syncOrderEvaluations(order).catch(() => {
        // 状态接口异常时兜底：直接锁定当前商品，避免重复提交
      })
      item.evaluated = true
      item.evaluationContent = item.evaluationContent || content
    }
  } finally {
    evalSubmitting[item.id] = false
  }
}

/* ============================== 初始化 ============================== */

onMounted(async () => {
  loadUserInfo()
  await loadOrders()
  console.log('[Orders] 当前用户：', user.value, '是否管理员：', isAdmin.value)
})
</script>

<template>
  <div class="orders-page jp-page" v-loading="loading">
    <!-- 页头 -->
    <div class="page-head">
      <div>
        <h2 class="page-title jp-title">我的订单</h2>
        <p class="page-sub jp-sub">{{ isAdmin ? '管理员视角 · 全部用户订单' : '仅展示当前账号的订单' }}</p>
      </div>
      <el-button class="back-cart-btn jp-btn" round @click="router.push('/cart')">去购物车</el-button>
    </div>

    <!-- 空状态 -->
    <el-empty v-if="!loading && visibleOrders.length === 0" description="暂无属于你的订单，请前往购物车下单">
      <el-button type="primary" class="jp-btn jp-btn-primary" round @click="router.push('/cart')">去购物车</el-button>
    </el-empty>

    <!-- 订单卡片列表 -->
    <div v-else class="order-list">
      <el-card v-for="order in visibleOrders" :key="order.id" class="order-card" shadow="never">
        <!-- 卡片头部：订单号 / 时间 / 状态标签 -->
        <div class="order-head">
          <div class="order-meta">
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <span class="order-time">{{ order.createdAt }}</span>
            <span v-if="isAdmin" class="order-user">下单用户：{{ order.username || order.userId }}</span>
          </div>
          <span class="status-tag" :class="statusClass(order.status)">{{ statusText(order.status) }}</span>
        </div>

        <!-- 商品明细 -->
        <div class="order-items">
          <div v-for="item in order.items" :key="item.id" class="order-item">
            <div class="item-main">
              <span class="item-name">{{ item.productName }}</span>
              <!-- 商品介绍：后端由商品表关联返回，为空时不渲染 -->
              <span v-if="item.productIntro" class="item-desc">{{ item.productIntro }}</span>
            </div>
            <span class="item-qty">×{{ item.quantity }}</span>
            <span class="item-price">¥{{ Number(item.unitPrice).toFixed(2) }}</span>
          </div>
        </div>

        <!-- 评价区（已完成订单）：普通用户可提交，管理员只读查看该订单商品的用户评价 -->
        <div v-if="order.status === 3" class="order-eval">
          <div v-for="item in order.items" :key="'eval-' + item.id" class="eval-item">
            <div class="eval-head">
              <span class="eval-name">{{ item.productName }}</span>
              <span class="eval-badge" :class="item.evaluated ? 'eval-badge-done' : 'eval-badge-todo'">
                {{ item.evaluated ? '已评价' : (isAdmin ? '暂无评价' : '待评价') }}
              </span>
            </div>

            <!-- 已评价：只读展示用户写下的评价文字，不再提供输入框 -->
            <p v-if="item.evaluated" class="eval-text">{{ item.evaluationContent || '未填写文字评价' }}</p>

            <!-- 管理员：未评价时只提示暂无评价，不提供任何提交入口 -->
            <p v-else-if="isAdmin" class="eval-text eval-empty">该商品暂无评价</p>

            <!-- 普通用户·未评价：文本输入框 + 提交按钮 -->
            <template v-else>
              <el-input
                v-model="evalDraft[item.id]"
                class="eval-input"
                type="textarea"
                :rows="2"
                :maxlength="500"
                show-word-limit
                placeholder="写下你的使用感受（最多 500 字）"
              />
              <div class="eval-actions">
                <el-button
                  class="eval-submit jp-btn jp-btn-primary"
                  type="primary"
                  round
                  size="small"
                  :loading="!!evalSubmitting[item.id]"
                  @click="handleEvaluate(order, item)"
                >
                  提交评价
                </el-button>
              </div>
            </template>
          </div>
        </div>

        <!-- 卡片底部：合计 + 操作按钮 -->
        <div class="order-foot">
          <div class="order-total">
            合计 <strong>¥{{ Number(order.totalAmount).toFixed(2) }}</strong>
          </div>

          <div class="order-actions">
            <!-- 普通用户：待付款 → 去付款 -->
            <el-button
              v-if="!isAdmin && order.status === 0"
              class="act-btn jp-btn jp-btn-primary"
              type="primary"
              round
              size="small"
              @click="handlePay(order)"
            >
              去付款
            </el-button>

            <!-- 普通用户：待付款 → 取消订单 -->
            <el-button
              v-if="!isAdmin && order.status === 0"
              class="act-btn jp-btn"
              round
              size="small"
              @click="handleCancel(order)"
            >
              取消订单
            </el-button>

            <!-- 普通用户：已发货 → 确认收货 -->
            <el-button
              v-if="!isAdmin && order.status === 2"
              class="act-btn jp-btn"
              type="success"
              round
              size="small"
              @click="handleConfirmReceipt(order)"
            >
              确认收货
            </el-button>

            <!-- 普通用户：已完成订单的评价入口已上移到商品明细下方（逐商品文字评价） -->

            <!-- 管理员：待发货 → 标记发货 -->
            <el-button
              v-if="isAdmin && order.status === 1"
              class="act-btn jp-btn"
              type="warning"
              round
              size="small"
              @click="handleShip(order)"
            >
              标记发货
            </el-button>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.orders-page {
  max-width: 72rem;
  margin: 0 auto;
  padding: 8px 0 48px;
}

/* 页头：发丝底边分隔 */
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px dashed rgba(44, 44, 44, 0.35);
}

.page-title {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 500;
  letter-spacing: 0.14em;
  color: #2c2c2c;
}

.page-sub {
  margin: 0;
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.06em;
  color: #5a5a5a;
}

/* 卡片列表 */
.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  border: 1px dashed #2c2c2c;
  border-radius: 4px;
  box-shadow: 2px 2px 0 #ffd93d;
  transition: all 300ms ease-out;
}

/* hover 微歪斜 + 硬边青阴影，无弹跳 */
.order-card:hover {
  transform: rotate(-0.5deg) translate(-1px, -1px);
  box-shadow: 3px 3px 0 #4ecdc4;
}

.order-card:active {
  transform: scale(0.98);
  box-shadow: 1px 1px 0 #ff6b6b;
}

.order-card :deep(.el-card__body) {
  padding: 24px 28px;
}

/* 卡片头部 */
.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px dashed rgba(44, 44, 44, 0.35);
}

.order-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.04em;
  color: #5a5a5a;
}

.order-no {
  color: #2c2c2c;
  font-weight: 400;
}

.order-user {
  color: #4f4f4f;
}

/* 状态标签：保留语义，改用低透明度马克笔三色 */
.status-tag {
  flex-shrink: 0;
  padding: 4px 14px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.06em;
  line-height: 1.4;
  background: #fffef5;
  color: #5a5a5a;
}

.status-unpaid {
  background: rgba(255, 217, 61, 0.25);
  color: #2c2c2c;
}

.status-paid {
  background: rgba(255, 107, 107, 0.2);
  color: #2c2c2c;
}

.status-shipped {
  background: rgba(78, 205, 196, 0.2);
  color: #2c2c2c;
}

.status-completed {
  background: rgba(78, 205, 196, 0.3);
  color: #2c2c2c;
}

.status-cancelled {
  background: #fffef5;
  color: #5a5a5a;
}

/* 商品明细 */
.order-items {
  padding: 16px 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  font-weight: 400;
}

.item-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.item-name {
  color: #2c2c2c;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* 商品介绍：弱化样式，单行省略，避免撑高卡片 */
.item-desc {
  color: #5a5a5a;
  font-size: 12px;
  font-weight: 400;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.item-qty {
  color: #5a5a5a;
  font-size: 13px;
  font-weight: 400;
}

.item-price {
  color: #2c2c2c;
  font-weight: 400;
  letter-spacing: 0.04em;
}

/* 卡片底部 */
.order-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px dashed rgba(44, 44, 44, 0.35);
}

.order-total {
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.06em;
  color: #5a5a5a;
}

.order-total strong {
  margin-left: 4px;
  font-size: 18px;
  font-weight: 500;
  letter-spacing: 0.04em;
  color: #ff6b6b;
  text-decoration: underline wavy rgba(255, 217, 61, 0.9) 1px;
  text-underline-offset: 4px;
}

.order-actions {
  display: flex;
  gap: 8px;
}

.act-btn {
  transition: all 300ms ease-out;
}

/* ===== 评价区（手绘涂鸦风：虚线边框 + 纸张底色 + 硬偏移阴影） ===== */
.order-eval {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 0 0 16px;
  padding: 16px 18px;
  background: #fffef5;
  border: 1px dashed #2c2c2c;
  border-radius: 4px;
  box-shadow: 2px 2px 0 #ffd93d;
}

.eval-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 12px;
  background: #fffef5;
  border: 1px dashed rgba(44, 44, 44, 0.5);
  border-radius: 4px;
  box-shadow: 2px 2px 0 rgba(78, 205, 196, 0.9);
  transition: all 300ms ease-out;
}

.eval-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.eval-name {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 13px;
  font-weight: 400;
  color: #2c2c2c;
}

/* 状态贴纸：黄 = 待评价，青 = 已评价，轻微歪斜 */
.eval-badge {
  flex-shrink: 0;
  padding: 2px 10px;
  border: 1px dashed #2c2c2c;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 400;
  letter-spacing: 0.06em;
  line-height: 1.4;
  color: #2c2c2c;
  transform: rotate(-1deg);
  box-shadow: 2px 2px 0 #2c2c2c;
  transition: all 300ms ease-out;
}

.eval-badge-todo {
  background: rgba(255, 217, 61, 0.45);
}

.eval-badge-done {
  background: rgba(78, 205, 196, 0.35);
}

/* 已评价文字：手写便签，红色波浪下划线强调 */
.eval-text {
  margin: 0;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.7;
  color: #2c2c2c;
  white-space: pre-wrap;
  word-break: break-word;
  text-decoration: underline wavy rgba(255, 107, 107, 0.65) 1px;
  text-underline-offset: 4px;
}

/* 管理员只读：该商品暂无评价（与已评价文字区分，不加波浪线） */
.eval-empty {
  color: #5a5a5a;
  text-decoration: none;
}

/* 输入框：虚线边框 + 黄硬阴影，聚焦转红、hover 转青 */
.eval-input :deep(.el-textarea__inner) {
  background: #fffef5;
  border: 1px dashed #2c2c2c;
  border-radius: 4px;
  box-shadow: 2px 2px 0 #ffd93d;
  color: #2c2c2c;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.7;
  transition: all 300ms ease-out;
}

.eval-input :deep(.el-textarea__inner):hover {
  box-shadow: 3px 3px 0 #4ecdc4;
}

.eval-input :deep(.el-textarea__inner):focus {
  border-color: #ff6b6b;
  box-shadow: 3px 3px 0 #4ecdc4;
}

.eval-input :deep(.el-input__count) {
  background: transparent;
  color: #5a5a5a;
  font-size: 12px;
}

.eval-actions {
  display: flex;
  justify-content: flex-end;
}

.eval-submit {
  transition: all 300ms ease-out;
}

@media (max-width: 767px) {
  .order-card :deep(.el-card__body) {
    padding: 20px;
  }

  .page-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .order-head,
  .order-foot {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}

/* 动效降级 */
@media (prefers-reduced-motion: reduce) {
  .order-card,
  .act-btn,
  .eval-item,
  .eval-badge,
  .eval-submit,
  .eval-input :deep(.el-textarea__inner) {
    transition: none !important;
  }

  .order-card:hover,
  .order-card:active {
    transform: none !important;
  }
}
</style>
