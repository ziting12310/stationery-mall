<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addCart, getGoodsDetail, type Goods } from '../api/auth'
import { getToken } from '../utils/request'

const route = useRoute()
const router = useRouter()

/** 商品详情数据 */
const goods = ref<Goods | null>(null)
/** 购买数量 */
const num = ref<number>(1)
const loading = ref(false)
const adding = ref(false)

/** 是否可购买（有库存） */
const canBuy = computed<boolean>(() => !!goods.value && goods.value.stock > 0)

/** 加载商品详情（游客可直接访问） */
async function loadDetail(): Promise<void> {
  console.log("loadDetail执行，拿到路由id =", route.params.id)
  loading.value = true
  try {
    goods.value = await getGoodsDetail(route.params.id as string)
    console.log("后端接口返回商品数据：", goods.value)
    // 切换商品后购买数量重置为 1
    num.value = 1
  } catch (err) {
    // 接口异常已由响应拦截器统一提示，这里仅控制台留档，避免未捕获的 Promise 报错
    console.error('[GoodsDetail] 加载商品详情失败：', err)
  } finally {
    loading.value = false
  }
}

// 监听路由 id 变化（如详情页之间互相跳转、组件被复用），id 变更时重新拉取商品详情
watch(
  () => route.params.id,
  (newId, oldId) => {
    if (newId && newId !== oldId) {
      loadDetail()
    }
  }
)

/** 加入购物车：未登录先跳登录页，登录后回到本页 */
async function handleAddCart(): Promise<void> {
  if (!goods.value) return

  if (!getToken()) {
    ElMessage.warning('请先登录后再加入购物车')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (goods.value.stock <= 0) {
    ElMessage.warning('该商品暂时缺货')
    return
  }
  if (num.value > goods.value.stock) {
    ElMessage.warning(`库存不足，当前仅剩 ${goods.value.stock} 件`)
    return
  }

  adding.value = true
  try {
    await addCart({ goodsId: goods.value.id, num: num.value })
    ElMessage.success('已加入购物车')
  } finally {
    adding.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <div v-loading="loading" class="detail-page jp-page">
    <el-page-header content="商品详情" @back="router.push('/')" />

    <el-card v-if="goods" shadow="never" class="detail-card jp-card">
      <div class="detail-body">
        <!-- 商品大图 -->
        <div class="image-area">
          <el-image v-if="goods.imageUrl" :src="goods.imageUrl" fit="cover" class="big-image">
            <template #error>
              <img src="../assets/images/doodle/default-cart.svg" alt="商品占位图" class="big-image" />
            </template>
          </el-image>
          <img v-else src="../assets/images/doodle/default-cart.svg" alt="商品占位图" class="big-image" />
        </div>

        <!-- 商品信息 -->
        <div class="info-area">
          <h2 class="goods-name jp-title">{{ goods.name }}</h2>
          <div class="price-line">
            <span class="label">单价</span>
            <span class="price">¥{{ Number(goods.price).toFixed(2) }}</span>
          </div>
          <div class="stock-line">
            <span class="label">库存</span>
            <span :class="['stock', canBuy ? '' : 'stock-empty']">
              {{ canBuy ? `${goods.stock} 件` : '暂时缺货' }}
            </span>
          </div>

          <div v-if="goods.description" class="desc-line">
            <span class="label">描述</span>
            <span class="desc">{{ goods.description }}</span>
          </div>

          <div class="num-line">
            <span class="label">数量</span>
            <el-input-number
              v-model="num"
              :min="1"
              :max="Math.max(goods.stock, 1)"
              :disabled="!canBuy"
            />
          </div>

          <el-button
            type="primary"
            size="large"
            class="add-btn jp-btn jp-btn-primary"
            :disabled="!canBuy"
            :loading="adding"
            @click="handleAddCart"
          >
            加入购物车
          </el-button>
        </div>
      </div>
    </el-card>

    <el-empty v-else-if="!loading" description="商品不存在或已下架" />
  </div>
</template>

<style scoped>
.detail-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 8px 0 48px;
}

.detail-card {
  margin-top: 16px;
  background-color: #ffffff;
}

.detail-card :deep(.el-card__body) {
  padding: 40px;
}

.detail-body {
  display: flex;
  gap: 40px;
  flex-wrap: wrap;
}

.image-area {
  width: 360px;
  flex-shrink: 0;
}

.big-image {
  width: 360px;
  height: 360px;
  border-radius: 4px;
  overflow: hidden;
}

.image-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fffef5;
  color: #5a5a5a;
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.06em;
  border-radius: 4px;
}

.info-area {
  flex: 1;
  min-width: 280px;
}

.goods-name {
  margin: 0 0 24px;
  font-size: 22px;
  font-weight: 500;
  letter-spacing: 0.08em;
  color: #2c2c2c;
  line-height: 1.6;
}

.price-line,
.stock-line,
.desc-line,
.num-line {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
  padding-bottom: 12px;
  border-bottom: 1px dashed rgba(44, 44, 44, 0.35);
}

.label {
  width: 48px;
  flex-shrink: 0;
  color: #5a5a5a;
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.06em;
}

.price {
  color: #ff6b6b;
  font-size: 24px;
  font-weight: 500;
  letter-spacing: 0.04em;
  text-decoration: underline wavy rgba(255, 217, 61, 0.9) 1px;
  text-underline-offset: 4px;
}

.stock {
  color: #4f4f4f;
  font-weight: 400;
}

.stock-empty {
  color: #b57b86;
}

.desc {
  color: #4f4f4f;
  font-size: 14px;
  font-weight: 400;
  line-height: 1.8;
}

.add-btn {
  width: 200px;
  height: 42px;
  margin-top: 8px;
}

@media (max-width: 767px) {
  .detail-card :deep(.el-card__body) {
    padding: 24px;
  }

  .image-area {
    width: 100%;
  }

  .big-image {
    width: 100%;
    height: auto;
    aspect-ratio: 1 / 1;
  }
}
</style>
