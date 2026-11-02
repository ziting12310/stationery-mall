<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createOrder, getCartList, type CartItem } from '../api/auth'
// 订单列表接口（即需求中的 getOrderList：GET /orders）
import { listMyOrders } from '../api/order'
import { USER_INFO_KEY } from '../utils/request'

const router = useRouter()

/** 待下单商品行 */
interface OrderRow {
  id: number
  name: string
  price: number
  num: number
}

const rows = ref<OrderRow[]>([])
const loading = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

/** 订单表单：收货地址必填，备注选填 */
const form = reactive({
  address: '',
  remark: ''
})

const rules: FormRules = {
  address: [{ required: true, message: '请输入收货地址', trigger: 'blur' }]
}

const totalAmount = computed<number>(() =>
  rows.value.reduce((sum, row) => sum + row.price * row.num, 0)
)

/** 把购物车项归一化为订单行 */
function normalize(item: CartItem): OrderRow {
  return {
    id: item.id,
    name: item.goodsName ?? item.productName ?? '未命名商品',
    price: Number(item.price ?? 0),
    num: Number(item.num ?? item.quantity ?? 1)
  }
}

/** 拉取购物车作为待结算清单 */
async function loadCart(): Promise<void> {
  loading.value = true
  try {
    const list = await getCartList()
    rows.value = (list || []).map(normalize)
  } finally {
    loading.value = false
  }
}

/** 读取当前登录用户 id（登录页写入 localStorage 的 userInfo） */
function getCurrentUserId(): number | undefined {
  try {
    const raw = localStorage.getItem(USER_INFO_KEY)
    if (!raw) return undefined
    const info = JSON.parse(raw) as { id?: number; userId?: number }
    const id = Number(info.id ?? info.userId ?? 0)
    return id > 0 ? id : undefined
  } catch {
    return undefined
  }
}

/** 提交订单：校验地址 → 调用创建订单接口（后端扣减库存） */
async function handleSubmit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (rows.value.length === 0) {
    ElMessage.warning('没有可结算的商品')
    return
  }

  submitting.value = true
  try {
    // 本次结算的购物车项 id（后端据此写订单明细并清空这些购物车行）
    const cartItemIds = rows.value.map((row) => row.id)
    // 当前登录用户 id，随请求体一并提交
    const userId = getCurrentUserId()

    // 说明：后端 orders.user_id 实际取自 JWT 中的登录用户（更安全），请求体中的 userId 仅作冗余透传
    const payload = {
      cartItemIds,
      userId,
      address: form.address.trim(),
      remark: form.remark.trim() || undefined
    }

    await createOrder(payload)

    // 后端已在同一事务内：写入 orders.user_id、复制商品名称/单价快照到 order_items、清空已结算购物车项
    // 前端同步清空本地待结算清单，避免返回后重复展示
    rows.value = []

    ElMessage.success('下单成功')

    // 跳转订单页之前，重新调用订单列表接口刷新订单数据（确保新订单已落库再展示）
    // 刷新失败不阻塞跳转，避免因网络抖动导致用户停留在确认页
    try {
      await listMyOrders()
    } catch (err) {
      console.error('[Order] 刷新订单列表失败：', err)
    }

    // 跳转【我的订单】列表页（/order 为确认订单页，订单列表在 /orders）
    router.push('/orders')
  } finally {
    submitting.value = false
  }
}

onMounted(loadCart)
</script>

<template>
  <div class="order-page jp-page" v-loading="loading">
    <el-card shadow="never" class="jp-card">
      <template #header>
        <span class="title jp-title">确认订单</span>
      </template>

      <el-empty v-if="!loading && rows.length === 0" description="没有待结算的商品">
        <el-button type="primary" class="jp-btn jp-btn-primary" @click="router.push('/cart')">返回购物车</el-button>
      </el-empty>

      <template v-else>
        <!-- 商品清单 -->
        <el-table :data="rows" style="width: 100%">
          <el-table-column prop="name" label="商品" min-width="220" />
          <el-table-column label="单价" width="120">
            <template #default="{ row }">¥{{ row.price.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="num" label="数量" width="100" />
          <el-table-column label="小计" width="130">
            <template #default="{ row }">¥{{ (row.price * row.num).toFixed(2) }}</template>
          </el-table-column>
        </el-table>

        <div class="total-line">
          合计：<span class="total-price">¥{{ totalAmount.toFixed(2) }}</span>
        </div>

        <!-- 收货信息 -->
        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="order-form">
          <el-form-item label="收货地址" prop="address">
            <el-input
              v-model="form.address"
              type="textarea"
              :rows="2"
              maxlength="256"
              show-word-limit
              placeholder="请输入XX省XX市XX区详细收货地址"
            />
          </el-form-item>
          <el-form-item label="备注">
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="2"
              maxlength="200"
              show-word-limit
              placeholder="选填，如配送要求"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="jp-btn jp-btn-primary" :loading="submitting" @click="handleSubmit">提交订单</el-button>
            <el-button class="jp-btn" @click="router.push('/cart')">返回购物车</el-button>
          </el-form-item>
        </el-form>
      </template>
    </el-card>
  </div>
</template>

<style scoped>
.order-page {
  max-width: 72rem;
  margin: 0 auto;
  padding: 8px 0 48px;
}

.order-page :deep(.el-card__body) {
  padding: 8px 24px 24px;
}

.title {
  font-size: 17px;
  font-weight: 500;
  letter-spacing: 0.12em;
  color: #2c2c2c;
}

.total-line {
  margin: 16px 0 24px;
  padding-top: 16px;
  border-top: 1px dashed rgba(44, 44, 44, 0.35);
  text-align: right;
  color: #5a5a5a;
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.06em;
}

.total-price {
  color: #ff6b6b;
  font-size: 20px;
  font-weight: 500;
  letter-spacing: 0.04em;
  text-decoration: underline wavy rgba(255, 217, 61, 0.9) 1px;
  text-underline-offset: 4px;
}

.order-form {
  max-width: 640px;
  margin-top: 8px;
}

.order-form :deep(.el-form-item) {
  margin-bottom: 28px;
}

.order-form :deep(.el-textarea__inner) {
  min-height: 56px;
  padding-bottom: 8px;
}

.order-form :deep(.el-input__count) {
  background: transparent;
  color: #5a5a5a;
  font-weight: 400;
  bottom: -18px;
}
</style>
