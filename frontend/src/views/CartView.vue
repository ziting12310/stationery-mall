<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCartList, removeCartItem, updateCartQuantity, type CartItem } from '../api/auth'

const router = useRouter()

/** 页面统一使用的购物车行结构（兼容后端两种字段命名） */
interface CartRow {
  id: number
  goodsId: number
  name: string
  price: number
  num: number
  stock: number
  imageUrl?: string
}

const rows = ref<CartRow[]>([])
const loading = ref(false)

/** 购物车合计金额 */
const totalAmount = computed<number>(() =>
  rows.value.reduce((sum, row) => sum + row.price * row.num, 0)
)

/** 把接口返回的购物车项归一化为页面结构 */
function normalize(item: CartItem): CartRow {
  return {
    id: item.id,
    goodsId: Number(item.goodsId ?? item.productId ?? 0),
    name: item.goodsName ?? item.productName ?? '未命名商品',
    price: Number(item.price ?? 0),
    num: Number(item.num ?? item.quantity ?? 1),
    stock: Number(item.stock ?? 0),
    imageUrl: item.imageUrl
  }
}

/** 加载购物车列表 */
async function loadCart(): Promise<void> {
  loading.value = true
  try {
    const list = await getCartList()
    rows.value = (list || []).map(normalize)
  } finally {
    loading.value = false
  }
}

/**
 * 修改数量：PUT /cart/items/{id}
 * 数量由 el-input-number 直接绑定 row.num，变更后调用后端持久化；
 * 失败时重新拉取购物车回滚为服务端数据。
 */
async function handleQuantityChange(row: CartRow): Promise<void> {
  if (row.stock > 0 && row.num > row.stock) {
    ElMessage.warning(`「${row.name}」库存仅剩 ${row.stock} 件`)
    row.num = row.stock
  }
  try {
    await updateCartQuantity(row.id, row.num)
  } catch {
    await loadCart()
  }
}

/** 删除商品：DELETE /cart/items/{id} */
async function handleRemove(row: CartRow): Promise<void> {
  await removeCartItem(row.id)
  rows.value = rows.value.filter((item) => item.id !== row.id)
  ElMessage.success('已移除该商品')
}

/** 去结算：跳转订单确认页 */
function goCheckout(): void {
  if (rows.value.length === 0) {
    ElMessage.warning('购物车为空，无法结算')
    return
  }
  router.push('/order')
}

onMounted(loadCart)
</script>

<template>
  <div class="cart-page jp-page" v-loading="loading">
    <el-card shadow="never" class="jp-card">
      <template #header>
        <div class="card-header">
          <span class="title jp-title">我的购物车</span>
          <span class="count jp-sub">共 {{ rows.length }} 种商品</span>
        </div>
      </template>

      <el-empty v-if="!loading && rows.length === 0" description="购物车还是空的">
        <el-button type="primary" class="jp-btn jp-btn-primary" @click="router.push('/')">去逛逛</el-button>
      </el-empty>

      <template v-else>
        <el-table :data="rows" style="width: 100%">
          <el-table-column label="商品" min-width="240">
            <template #default="{ row }">
              <div class="goods-cell">
                <el-image v-if="row.imageUrl" :src="row.imageUrl" fit="cover" class="thumb">
                  <template #error>
                    <img src="../assets/images/doodle/default-cart.svg" alt="商品占位图" class="thumb" />
                  </template>
                </el-image>
                <img v-else src="../assets/images/doodle/default-cart.svg" alt="商品占位图" class="thumb" />
                <span class="goods-name">{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="单价" width="120">
            <template #default="{ row }">¥{{ row.price.toFixed(2) }}</template>
          </el-table-column>

          <el-table-column label="数量" width="170">
            <template #default="{ row }">
              <el-input-number
                v-model="row.num"
                :min="1"
                :max="Math.max(row.stock, 1)"
                size="small"
                @change="handleQuantityChange(row)"
              />
            </template>
          </el-table-column>

          <el-table-column label="小计" width="130">
            <template #default="{ row }">
              <span class="subtotal">¥{{ (row.price * row.num).toFixed(2) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="danger" @click="handleRemove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 结算栏 -->
        <div class="checkout-bar">
          <div class="total">
            合计：<span class="total-price">¥{{ totalAmount.toFixed(2) }}</span>
          </div>
          <el-button type="primary" size="large" class="jp-btn jp-btn-primary" @click="goCheckout">去结算</el-button>
        </div>
      </template>
    </el-card>
  </div>
</template>

<style scoped>
.cart-page {
  max-width: 72rem;
  margin: 0 auto;
  padding: 8px 0 48px;
}

.cart-page :deep(.el-card__body) {
  padding: 8px 24px 24px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-size: 17px;
  font-weight: 500;
  letter-spacing: 0.12em;
  color: #2c2c2c;
}

.count {
  color: #5a5a5a;
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.06em;
}

.goods-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.thumb {
  width: 56px;
  height: 56px;
  border-radius: 4px;
  flex-shrink: 0;
}

.thumb-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fffef5;
  color: #5a5a5a;
  font-size: 12px;
  font-weight: 400;
}

.goods-name {
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 0.02em;
  color: #2c2c2c;
}

.subtotal {
  color: #2c2c2c;
  font-weight: 400;
  letter-spacing: 0.04em;
}

.checkout-bar {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px dashed rgba(44, 44, 44, 0.35);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 24px;
}

.total {
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.06em;
  color: #5a5a5a;
}

.total-price {
  color: #ff6b6b;
  font-size: 20px;
  font-weight: 500;
  letter-spacing: 0.04em;
  text-decoration: underline wavy rgba(255, 217, 61, 0.9) 1px;
  text-underline-offset: 4px;
}
</style>
