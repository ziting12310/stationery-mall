<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addCart, getAdminGoodsList, getCategoryList, getGoodsList, searchGoods, toggleGoodsOnShelf, type Category, type Goods, type UserInfo } from '../api/auth'
import { getToken, USER_INFO_KEY } from '../utils/request'

const router = useRouter()
const route = useRoute()

/** 搜索关键词（导航栏搜索跳转 /?keyword=xxx），为空表示普通浏览 */
const keyword = computed<string>(() => String(route.query.keyword ?? '').trim())

/** 分类列表（一级分类含 children 二级分类） */
const categories = ref<Category[]>([])
/** 上架商品列表 */
const goods = ref<Goods[]>([])
/** 当前选中的分类 id，0 表示全部商品 */
const activeCategory = ref<number>(0)
const loading = ref(false)
/** 正在加购的商品 id（用于按卡片显示 loading，避免重复点击） */
const addingId = ref<number | null>(null)
/** 当前登录用户（localStorage 由登录页写入），用于判断是否管理员 */
const user = ref<UserInfo | null>(null)
/** 是否管理员：仅 ADMIN 展示上架 / 下架按钮 */
const isAdmin = computed<boolean>(() => user.value?.role === 'ADMIN')
/** 正在上架 / 下架的商品 id（按卡片显示 loading） */
const shelfLoadingId = ref<number | null>(null)

/** 读取本地登录用户信息（游客访问时可能为空，按普通用户处理） */
function loadUserInfo(): void {
  try {
    const raw = localStorage.getItem(USER_INFO_KEY)
    user.value = raw ? (JSON.parse(raw) as UserInfo) : null
  } catch {
    user.value = null
  }
}

/** 选中的分类下允许的二级分类 id 集合（未选中一级分类时返回 null） */
function categoryIdSet(categoryId: number): Set<number> | null {
  if (categoryId === 0) return null
  const parent = categories.value.find((item) => item.id === categoryId)
  if (parent?.children?.length) {
    return new Set(parent.children.map((child) => child.id))
  }
  return new Set([categoryId])
}

/** 按分类过滤后的商品（纯前端筛选，接口 /goods/list 无分类参数） */
const filteredGoods = computed<Goods[]>(() => {
  const idSet = categoryIdSet(activeCategory.value)
  if (!idSet) return goods.value
  return goods.value.filter((item) => idSet.has(Number(item.categoryId)))
})

/** 含二级子分类的一级分类（渲染为 el-sub-menu） */
const parentsWithChildren = computed(() =>
  categories.value.filter((item) => item.children && item.children.length)
)

/** 不含子分类的一级分类（直接渲染为菜单项） */
const parentsWithoutChildren = computed(() =>
  categories.value.filter((item) => !item.children || item.children.length === 0)
)

/** 分类菜单点击：index 即分类 id 字符串 */
function handleCategorySelect(index: string): void {
  activeCategory.value = Number(index)
}

/** 加载分类与商品（游客可访问，失败时 request 已统一提示） */
async function loadData(): Promise<void> {
  loading.value = true
  try {
    const [categoryList, goodsList] = await Promise.all([
      getCategoryList(),
      // 有搜索关键词：走后端名称模糊搜索；否则按角色加载商品列表
      keyword.value
        ? searchGoods(keyword.value)
        : (isAdmin.value ? getAdminGoodsList() : getGoodsList())
    ])
    categories.value = categoryList
    goods.value = goodsList
  } finally {
    loading.value = false
  }
}

/** 点击商品卡片进入详情页 */
function goDetail(id: number): void {
  router.push(`/goods/${id}`)
}

/**
 * 卡片内「加入购物车」：未登录先跳登录页，缺货直接提示。
 * 与商品详情页逻辑保持一致：默认加购 1 件，具体数量在详情页可调。
 */
async function handleAddCart(item: Goods): Promise<void> {
  if (!getToken()) {
    ElMessage.warning('请先登录后再加入购物车')
    router.push({ path: '/login', query: { redirect: '/' } })
    return
  }
  if (item.stock <= 0) {
    ElMessage.warning('该商品暂时缺货')
    return
  }

  addingId.value = item.id
  try {
    await addCart({ goodsId: item.id, num: 1 })
    ElMessage.success('已加入购物车')
  } catch (err) {
    // 接口异常已由响应拦截器统一提示，这里仅控制台留档
    console.error('[Home] 加入购物车失败：', err)
  } finally {
    addingId.value = null
  }
}

/**
 * 管理员上架 / 下架：调用后端 PATCH /admin/products/{id}/on-shelf，
 * 成功后重新拉取商品列表，保证状态与后端一致。
 */
async function handleToggleShelf(item: Goods): Promise<void> {
  if (!isAdmin.value) {
    ElMessage.warning('仅管理员可执行上下架操作')
    return
  }
  const nextOnShelf = !item.onShelf
  shelfLoadingId.value = item.id
  try {
    await toggleGoodsOnShelf(item.id, nextOnShelf)
    ElMessage.success(nextOnShelf ? '商品已上架' : '商品已下架')
    // 操作成功后刷新商品列表
    await loadData()
  } catch (err) {
    // 接口异常已由响应拦截器统一提示，这里仅控制台留档
    console.error('[Home] 上下架失败：', err)
  } finally {
    shelfLoadingId.value = null
  }
}

/** 清除搜索：回到无关键词的首页浏览态 */
function clearSearch(): void {
  router.push('/')
}

/* ==================== 商品插画（内联 SVG，按商品名称匹配） ==================== */

/** 统一纸底：奶白纸 + 横线纹理 + 左上角小红星，body 为居中主图案 */
function doodleSvg(body: string): string {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300">
    <rect width="400" height="300" fill="#fffef5"/>
    <g stroke="#2c2c2c" stroke-opacity="0.08" stroke-width="1">
      <line x1="0" y1="60" x2="400" y2="60"/>
      <line x1="0" y1="120" x2="400" y2="120"/>
      <line x1="0" y1="180" x2="400" y2="180"/>
      <line x1="0" y1="240" x2="400" y2="240"/>
    </g>
    <g transform="rotate(-6 40 42)">
      <path d="M40 24 L46 37 L60 38 L50 48 L53 62 L40 55 L27 62 L30 48 L20 38 L34 37 Z"
        fill="#ff6b6b" stroke="#2c2c2c" stroke-width="1.5" stroke-dasharray="4 3"/>
    </g>
    <g transform="rotate(-2 200 165)">${body}</g>
  </svg>`
}

/** 订书机 */
const STAPLER_SVG = doodleSvg(`
    <path d="M118 158 C140 108 260 108 282 158" fill="#ffd93d" stroke="#2c2c2c"
      stroke-width="2" stroke-dasharray="6 4"/>
    <rect x="112" y="152" width="176" height="44" rx="6" fill="#4ecdc4"
      stroke="#2c2c2c" stroke-width="2" stroke-dasharray="6 4"/>
    <rect x="124" y="196" width="152" height="14" rx="4" fill="#ff6b6b"
      stroke="#2c2c2c" stroke-width="1.5" stroke-dasharray="5 4"/>
    <circle cx="146" cy="174" r="5" fill="#2c2c2c"/>
    <line x1="220" y1="156" x2="220" y2="192" stroke="#2c2c2c" stroke-width="1.5" stroke-dasharray="4 4"/>
`)

/** 书包 */
const BACKPACK_SVG = doodleSvg(`
    <path d="M150 126 C150 88 250 88 250 126" fill="none" stroke="#2c2c2c"
      stroke-width="2" stroke-dasharray="6 4"/>
    <rect x="128" y="118" width="144" height="118" rx="16" fill="#4ecdc4"
      stroke="#2c2c2c" stroke-width="2" stroke-dasharray="7 5"/>
    <path d="M136 148 L264 148 L248 110 Q200 94 152 110 Z" fill="#ff6b6b"
      stroke="#2c2c2c" stroke-width="2" stroke-dasharray="6 4"/>
    <rect x="168" y="164" width="64" height="46" rx="8" fill="#ffd93d"
      stroke="#2c2c2c" stroke-width="1.5" stroke-dasharray="5 4"/>
    <line x1="176" y1="146" x2="224" y2="146" stroke="#2c2c2c" stroke-width="2"/>
    <circle cx="232" cy="146" r="4" fill="#2c2c2c"/>
    <rect x="112" y="138" width="12" height="72" rx="6" fill="#ff6b6b"
      stroke="#2c2c2c" stroke-width="1.5" stroke-dasharray="4 3"/>
    <rect x="276" y="138" width="12" height="72" rx="6" fill="#ff6b6b"
      stroke="#2c2c2c" stroke-width="1.5" stroke-dasharray="4 3"/>
`)

/** 胶带 */
const TAPE_SVG = doodleSvg(`
    <circle cx="184" cy="164" r="56" fill="#ffd93d" stroke="#2c2c2c"
      stroke-width="2" stroke-dasharray="7 5"/>
    <circle cx="184" cy="164" r="20" fill="#fffef5" stroke="#2c2c2c"
      stroke-width="2" stroke-dasharray="5 4"/>
    <path d="M240 166 Q286 156 292 202 L262 210 Q258 178 236 190 Z" fill="#4ecdc4"
      stroke="#2c2c2c" stroke-width="2" stroke-dasharray="6 4"/>
    <path d="M140 140 a56 56 0 0 1 34 -24" fill="none" stroke="#ff6b6b" stroke-width="4"/>
    <line x1="252" y1="186" x2="288" y2="196" stroke="#2c2c2c" stroke-opacity="0.45"
      stroke-width="1.5" stroke-dasharray="5 4"/>
`)

/** 文件夹 */
const FOLDER_SVG = doodleSvg(`
    <path d="M112 128 h72 l20 24 h84 v96 h-176 z" fill="#ffd93d"
      stroke="#2c2c2c" stroke-width="2" stroke-dasharray="7 5"/>
    <path d="M112 168 h176" stroke="#2c2c2c" stroke-width="1.5" stroke-dasharray="6 4"/>
    <rect x="136" y="186" width="128" height="30" rx="3" fill="#fffef5"
      stroke="#2c2c2c" stroke-width="1.5" stroke-dasharray="4 4"/>
    <line x1="146" y1="198" x2="254" y2="198" stroke="#4ecdc4" stroke-width="3"/>
    <line x1="146" y1="208" x2="232" y2="208" stroke="#2c2c2c" stroke-opacity="0.45" stroke-width="2"/>
`)

/** 直尺 */
const RULER_SVG = doodleSvg(`
    <rect x="88" y="140" width="224" height="52" rx="5" fill="#4ecdc4"
      stroke="#2c2c2c" stroke-width="2" stroke-dasharray="7 5"/>
    <g stroke="#2c2c2c" stroke-width="1.5">
      <line x1="108" y1="140" x2="108" y2="152"/>
      <line x1="128" y1="140" x2="128" y2="148"/>
      <line x1="148" y1="140" x2="148" y2="152"/>
      <line x1="168" y1="140" x2="168" y2="148"/>
      <line x1="188" y1="140" x2="188" y2="152"/>
      <line x1="208" y1="140" x2="208" y2="148"/>
      <line x1="228" y1="140" x2="228" y2="152"/>
      <line x1="248" y1="140" x2="248" y2="148"/>
      <line x1="268" y1="140" x2="268" y2="152"/>
      <line x1="288" y1="140" x2="288" y2="148"/>
    </g>
    <line x1="100" y1="176" x2="300" y2="176" stroke="#2c2c2c" stroke-opacity="0.35"
      stroke-width="1.5" stroke-dasharray="6 4"/>
`)

/** 绘画本（线圈素描本 + 小风景） */
const SKETCHBOOK_SVG = doodleSvg(`
    <rect x="116" y="104" width="168" height="152" fill="#fffef5"
      stroke="#2c2c2c" stroke-width="2" stroke-dasharray="7 5"/>
    <g fill="#ffd93d" stroke="#2c2c2c" stroke-width="1.5">
      <circle cx="116" cy="120" r="7"/>
      <circle cx="116" cy="144" r="7"/>
      <circle cx="116" cy="168" r="7"/>
      <circle cx="116" cy="192" r="7"/>
      <circle cx="116" cy="216" r="7"/>
      <circle cx="116" cy="240" r="7"/>
    </g>
    <circle cx="200" cy="152" r="16" fill="#ffd93d" stroke="#2c2c2c"
      stroke-width="1.5" stroke-dasharray="5 4"/>
    <path d="M138 238 L178 190 L206 224 L222 206 L266 238 Z" fill="#4ecdc4"
      stroke="#2c2c2c" stroke-width="2" stroke-dasharray="6 4"/>
    <path d="M146 246 q26 -12 52 0" fill="none" stroke="#ff6b6b" stroke-width="2.5"/>
`)

/** 笔记本（横线本 + 红色页边线） */
const NOTEBOOK_SVG = doodleSvg(`
    <rect x="120" y="104" width="160" height="152" rx="4" fill="#fffef5"
      stroke="#2c2c2c" stroke-width="2" stroke-dasharray="7 5"/>
    <path d="M280 104 L254 104 L280 130 Z" fill="#ffd93d" stroke="#2c2c2c"
      stroke-width="1.5" stroke-dasharray="4 3"/>
    <line x1="152" y1="112" x2="152" y2="248" stroke="#ff6b6b" stroke-width="2.5"/>
    <g stroke="#2c2c2c" stroke-opacity="0.45" stroke-width="2" stroke-dasharray="8 5">
      <line x1="136" y1="136" x2="268" y2="136"/>
      <line x1="136" y1="162" x2="268" y2="162"/>
      <line x1="136" y1="188" x2="268" y2="188"/>
      <line x1="136" y1="214" x2="268" y2="214"/>
      <line x1="136" y1="240" x2="240" y2="240"/>
    </g>
`)

/** 默认文具（笔记本 + 铅笔 + 橡皮） */
const DEFAULT_SVG = doodleSvg(`
    <rect x="96" y="110" width="118" height="142" rx="6" fill="#fffef5"
      stroke="#2c2c2c" stroke-width="2" stroke-dasharray="7 5"/>
    <g stroke="#2c2c2c" stroke-opacity="0.4" stroke-width="2" stroke-dasharray="7 5">
      <line x1="112" y1="138" x2="198" y2="138"/>
      <line x1="112" y1="164" x2="198" y2="164"/>
      <line x1="112" y1="190" x2="180" y2="190"/>
    </g>
    <line x1="121" y1="116" x2="121" y2="246" stroke="#ff6b6b" stroke-width="2.5"/>
    <path d="M236 236 L292 180 L304 192 L248 248 Z" fill="#ffd93d"
      stroke="#2c2c2c" stroke-width="2" stroke-dasharray="6 4"/>
    <polygon points="292,180 304,192 296,198" fill="#2c2c2c"/>
    <rect x="240" y="238" width="26" height="16" rx="4" fill="#ff6b6b"
      stroke="#2c2c2c" stroke-width="1.5" stroke-dasharray="4 3"/>
    <circle cx="318" cy="204" r="10" fill="#4ecdc4" stroke="#2c2c2c"
      stroke-width="1.5" stroke-dasharray="4 3"/>
`)

/** 商品名称 → 手绘卡通插图：按关键词顺序匹配，未命中使用默认文具插图 */
function productIllustration(name?: string): string {
  const text = name || ''
  if (text.includes('订书器') || text.includes('订书机')) return STAPLER_SVG
  if (text.includes('书包')) return BACKPACK_SVG
  if (text.includes('胶带')) return TAPE_SVG
  if (text.includes('文件夹')) return FOLDER_SVG
  if (text.includes('直尺')) return RULER_SVG
  if (text.includes('绘画本')) return SKETCHBOOK_SVG
  if (text.includes('笔记本')) return NOTEBOOK_SVG
  return DEFAULT_SVG
}

onMounted(async () => {
  loadUserInfo()
  await loadData()
})

// 搜索关键词变化时重新加载（已在首页时再次搜索也能生效），并重置分类筛选
watch(keyword, async () => {
  activeCategory.value = 0
  await loadData()
})
</script>

<template>
  <div class="home-page jp-page" v-loading="loading">
    <!-- 顶部 Banner：网站名称 + slogan + 装饰图形（纯展示，无交互） -->
    <section class="banner">
      <div class="banner-text">
        <h1 class="banner-title">文具商城</h1>
        <p class="banner-slogan">好文具，陪你把日常写好</p>
      </div>
      <div class="banner-deco" aria-hidden="true">
        <span class="deco deco-1"></span>
        <span class="deco deco-2"></span>
        <span class="deco deco-3"></span>
      </div>
    </section>

    <!-- 按分类逛：横向分类卡片（点击仍调用原有 handleCategorySelect） -->
    <section id="category-section" class="section">
      <div class="section-head">
        <h2 class="section-title">按分类逛</h2>
        <span class="section-more" @click="handleCategorySelect('0')">查看全部</span>
      </div>
      <div class="category-cards">
        <div
          class="category-card"
          :class="{ active: activeCategory === 0 }"
          @click="handleCategorySelect('0')"
        >
          <span class="cat-name">全部商品</span>
          <span class="cat-count">{{ goods.length }} 件商品</span>
        </div>
        <div
          v-for="first in categories"
          :key="first.id"
          class="category-card"
          :class="{ active: activeCategory === Number(first.id) }"
          @click="handleCategorySelect(String(first.id))"
        >
          <span class="cat-name">{{ first.name }}</span>
          <span class="cat-count">{{ first.children?.length || 0 }} 个子分类</span>
        </div>
      </div>
    </section>

    <!-- 精选推荐：商品卡片列表（卡片结构与事件绑定全部保留） -->
    <section class="section">
      <div class="section-head">
        <h2 class="section-title">{{ keyword ? '搜索「' + keyword + '」' : '精选推荐' }}</h2>
        <span v-if="keyword" class="section-more" @click="clearSearch">清除搜索</span>
        <span v-else class="section-more" @click="handleCategorySelect('0')">查看全部</span>
      </div>
      <el-empty
        v-if="!loading && filteredGoods.length === 0"
        :description="keyword ? '没有找到相关商品' : '该分类下暂无商品'"
      />
      <div v-else class="goods-grid">
        <el-card
          v-for="item in filteredGoods"
          :key="item.id"
          shadow="hover"
          class="goods-card"
          @click="goDetail(item.id)"
        >
          <!-- 商品插图：按商品名称匹配手绘卡通内联 SVG；真实图片加载失败同样回退到匹配插图 -->
          <el-image v-if="item.imageUrl" :src="item.imageUrl" fit="cover" class="goods-image">
            <template #error>
              <div class="goods-svg" v-html="productIllustration(item.name)"></div>
            </template>
          </el-image>
          <div v-else class="goods-image" v-html="productIllustration(item.name)"></div>

          <div class="goods-name" :title="item.name">{{ item.name }}</div>
          <div class="goods-bottom">
            <span class="goods-price">¥{{ Number(item.price).toFixed(2) }}</span>
            <span class="goods-stock">库存 {{ item.stock }}</span>
          </div>

          <!-- 加入购物车：@click.stop 阻止冒泡到卡片，避免同时跳转详情页 -->
          <el-button
            type="primary"
            class="add-cart-btn"
            :loading="addingId === item.id"
            :disabled="item.stock <= 0"
            @click.stop="handleAddCart(item)"
          >
            {{ item.stock > 0 ? '加入购物车' : '暂时缺货' }}
          </el-button>

          <!-- 管理员：上架 / 下架（普通用户不渲染） -->
          <el-button
            v-if="isAdmin"
            :type="item.onShelf ? 'warning' : 'success'"
            class="shelf-btn"
            :loading="shelfLoadingId === item.id"
            @click.stop="handleToggleShelf(item)"
          >
            {{ item.onShelf ? '下架' : '上架' }}
          </el-button>
        </el-card>
      </div>
    </section>

    <!-- 底部导航（视觉层，跳转沿用现有路由，不新增页面） -->
    <nav class="bottom-nav">
      <router-link class="bottom-item" to="/">首页</router-link>
      <a class="bottom-item" href="#category-section">分类</a>
      <router-link class="bottom-item" to="/cart">购物车</router-link>
      <router-link class="bottom-item" to="/user">我的</router-link>
    </nav>
  </div>
</template>

<style scoped>
/* ============================================================
   手绘涂鸦风 hand-drawn-doodle（唯一保留的主题层）
   纸张 #fffef5 · 墨黑 #2c2c2c · 马克笔 红 #ff6b6b / 青 #4ecdc4 / 黄 #ffd93d
   只有虚线边框 · 硬边偏移阴影 · 微歪斜 · 300ms ease-out
   ============================================================ */
.home-page {
  --jp-paper: #fffef5;
  --jp-line: #2c2c2c;
  --jp-text: #2c2c2c;
  --jp-text-sub: #4f4f4f;
  --jp-text-weak: #5a5a5a;
  --jp-sky: #4ecdc4;
  --jp-mint: #4ecdc4;
  --jp-pink: #ff6b6b;
  --jp-powder: #ffd93d;
  max-width: 72rem;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 0;
  background: transparent;
  color: var(--jp-text);
  font-family: inherit;
  font-weight: 400;
  letter-spacing: 0.02em;
}

/* 区块密度：py-10 md:py-14，模块间距 mb-10 md:mb-14（收紧整体留白） */
.section {
  display: flex;
  flex-direction: column;
  padding: 40px 0 0;
  margin-bottom: 40px;
}

/* 标题：手写便签标题 + 红色波浪下划线；标题与内容间距 mb-5 md:mb-6 */
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  margin-bottom: 20px;
  border-bottom: 1px dashed var(--jp-line);
}

@media (min-width: 768px) {
  .section {
    padding: 56px 0 0;
    margin-bottom: 56px;
  }

  .section-head {
    margin-bottom: 24px;
  }
}

.section-title {
  margin: 0;
  font-family: inherit;
  font-size: 18px;
  font-weight: 500;
  letter-spacing: 0.04em;
  color: var(--jp-text);
  text-decoration: underline wavy var(--jp-pink) 1px;
  text-underline-offset: 6px;
}

.section-more {
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.02em;
  color: var(--jp-text);
  text-decoration: underline wavy var(--jp-sky) 1px;
  text-underline-offset: 4px;
  cursor: pointer;
  transition: all 300ms ease-out;
}

.section-more:hover {
  color: var(--jp-pink);
  text-decoration-color: var(--jp-pink);
  transform: rotate(-1deg);
}

/* ---------- 顶部 Banner：白纸卡片 + 胶带 + 硬边黄阴影 ---------- */
.banner {
  position: relative;
  overflow: visible;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
  padding: 32px 28px;
  border-radius: 4px;
  background: #ffffff;
  border: 1px dashed var(--jp-line);
  box-shadow: 2px 2px 0 var(--jp-powder);
  transform: rotate(-0.5deg);
}

/* 胶带：两条黄色纸胶带贴在 banner 上边沿 */
.banner::before,
.banner::after {
  content: '';
  position: absolute;
  top: -10px;
  width: 86px;
  height: 20px;
  background: rgba(255, 217, 61, 0.55);
  border-left: 1px dashed rgba(44, 44, 44, 0.35);
  border-right: 1px dashed rgba(44, 44, 44, 0.35);
  pointer-events: none;
}

.banner::before {
  left: 32px;
  transform: rotate(-3deg);
}

.banner::after {
  right: 32px;
  transform: rotate(3deg);
}

.banner-text {
  position: relative;
  z-index: 1;
}

@media (min-width: 768px) {
  .banner {
    padding: 36px 32px;
  }
}

.banner-title {
  margin: 0;
  font-family: inherit;
  font-size: 26px;
  font-weight: 500;
  letter-spacing: 0.06em;
  color: var(--jp-text);
}

.banner-slogan {
  margin: 12px 0 0;
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.02em;
  color: var(--jp-text-weak);
}

/* 装饰图形：手绘涂鸦的马克笔圆点与方块 */
.banner-deco {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.deco {
  position: absolute;
  display: block;
  border-radius: 2px;
}

.deco-1 {
  width: 120px;
  height: 120px;
  top: -34px;
  right: 56px;
  border: 1px dashed rgba(255, 107, 107, 0.45);
  border-radius: 50%;
  background: rgba(255, 107, 107, 0.08);
}

.deco-2 {
  width: 64px;
  height: 64px;
  right: 168px;
  bottom: -16px;
  border: 1px dashed rgba(78, 205, 196, 0.5);
  transform: rotate(12deg);
  background: rgba(78, 205, 196, 0.1);
}

.deco-3 {
  width: 24px;
  height: 24px;
  right: 104px;
  bottom: 40px;
  border-radius: 3px;
  transform: rotate(18deg);
  border: 1px dashed rgba(255, 217, 61, 0.7);
  background: rgba(255, 217, 61, 0.2);
}

/* ---------- 按分类逛：大尺寸分类卡片（移动端 2 列 45%，桌面 4 列 23%） ---------- */
.category-cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  padding: 2px 2px 4px;
}

@media (min-width: 768px) {
  .category-cards {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 20px;
  }
}

.category-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  min-height: 96px;
  padding: 24px;
  border-radius: 4px;
  background: #ffffff;
  border: 1px dashed var(--jp-line);
  box-shadow: 2px 2px 0 var(--jp-powder);
  cursor: pointer;
  transform: rotate(-0.5deg);
  transition: all 300ms ease-out;
}

/* 交替微歪斜，模拟手绘不规则 */
.category-card:nth-child(even) {
  transform: rotate(0.5deg);
}

@media (min-width: 768px) {
  .category-card {
    min-height: 112px;
    padding: 28px;
  }
}

.category-card:hover {
  background: #ffffff;
  border-color: var(--jp-line);
  box-shadow: 3px 3px 0 var(--jp-sky);
  transform: rotate(0deg) translate(-1px, -1px);
}

.category-card:active {
  box-shadow: 1px 1px 0 var(--jp-pink);
  transform: scale(0.98);
}

.category-card.active {
  border-color: var(--jp-line);
  background: rgba(255, 217, 61, 0.2);
  box-shadow: 2px 2px 0 var(--jp-pink);
}

.cat-name {
  font-family: inherit;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 0.02em;
  color: var(--jp-text);
}

@media (min-width: 768px) {
  .cat-name {
    font-size: 18px;
  }
}

.cat-count {
  font-size: 12px;
  font-weight: 400;
  letter-spacing: 0.02em;
  color: var(--jp-text-weak);
}

@media (min-width: 768px) {
  .cat-count {
    font-size: 14px;
  }
}

/* ---------- 精选推荐：商品卡片 ---------- */
.goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 20px;
}

@media (min-width: 768px) {
  .goods-grid {
    gap: 24px;
  }
}

.goods-card {
  cursor: pointer;
  background: #ffffff;
  border: 1px dashed var(--jp-line);
  border-radius: 4px;
  box-shadow: 2px 2px 0 var(--jp-powder);
  transform: rotate(-0.5deg);
  transition: all 300ms ease-out;
}

.goods-card:nth-child(even) {
  transform: rotate(0.5deg);
}

.goods-card:hover {
  box-shadow: 3px 3px 0 var(--jp-sky);
  transform: rotate(0deg) translate(-1px, -1px);
}

.goods-card:active {
  box-shadow: 1px 1px 0 var(--jp-pink);
  transform: scale(0.98);
}

.goods-card :deep(.el-card__body) {
  padding: 18px;
}

@media (min-width: 768px) {
  .goods-card :deep(.el-card__body) {
    padding: 24px;
  }
}

.goods-image {
  width: 100%;
  aspect-ratio: 4 / 3;
  height: auto;
  border-radius: 4px;
  border: 1px dashed rgba(44, 44, 44, 0.35);
  overflow: hidden;
}

/* 内联商品插图铺满图片区域 */
.goods-svg,
.goods-image svg {
  display: block;
  width: 100%;
  height: 100%;
}

.image-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  aspect-ratio: 4 / 3;
  height: auto;
  background: var(--jp-paper);
  color: var(--jp-text-weak);
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.02em;
  border: 1px dashed rgba(44, 44, 44, 0.35);
  border-radius: 4px;
}

.goods-name {
  margin: 12px 2px 10px;
  font-size: 15px;
  font-weight: 400;
  letter-spacing: 0.02em;
  color: var(--jp-text);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  transition: color 300ms ease-out;
}

.goods-card:hover .goods-name {
  color: var(--jp-text);
}

.goods-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2px 2px;
}

.goods-price {
  font-size: 18px;
  font-weight: 500;
  letter-spacing: 0.02em;
  color: var(--jp-pink);
  text-decoration: underline wavy rgba(255, 217, 61, 0.9) 1px;
  text-underline-offset: 4px;
}

.goods-stock {
  padding: 0;
  background: transparent;
  font-size: 12px;
  font-weight: 400;
  letter-spacing: 0.02em;
  color: var(--jp-text-weak);
}

/* 加入购物车：白底 + 虚线墨边 + 硬边黄阴影，300ms ease-out */
.add-cart-btn {
  width: 100%;
  height: 36px;
  margin-top: 10px;
  border-radius: 4px;
  border: 1px dashed var(--jp-line) !important;
  background: #ffffff !important;
  color: var(--jp-text) !important;
  font-family: inherit;
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 0.04em;
  box-shadow: 2px 2px 0 var(--jp-powder) !important;
  transition: all 300ms ease-out;
}

.add-cart-btn:hover:not(.is-disabled) {
  background: #ffffff !important;
  border-color: var(--jp-line) !important;
  color: var(--jp-text) !important;
  box-shadow: 3px 3px 0 var(--jp-sky) !important;
  transform: rotate(-0.5deg) translate(-1px, -1px);
}

.add-cart-btn:active:not(.is-disabled) {
  box-shadow: 1px 1px 0 var(--jp-pink) !important;
  transform: scale(0.98);
}

.add-cart-btn.is-disabled {
  background: var(--jp-paper) !important;
  color: #7a7a7a !important;
  border-color: #7a7a7a !important;
  box-shadow: none !important;
}

/* 上下架按钮：第二行、小尺寸、白底虚线墨边 */
.shelf-btn {
  display: flex;
  width: 88px;
  height: 26px;
  margin: 8px auto 0;
  padding: 0;
  border-radius: 4px;
  border: 1px dashed var(--jp-line) !important;
  background: #ffffff !important;
  font-family: inherit;
  font-size: 12px;
  font-weight: 400;
  letter-spacing: 0.04em;
  box-shadow: 1px 1px 0 var(--jp-powder) !important;
  transition: all 300ms ease-out;
}

.shelf-btn:hover {
  background: #ffffff !important;
  box-shadow: 2px 2px 0 var(--jp-sky) !important;
  transform: rotate(-1deg);
}

.shelf-btn:active {
  box-shadow: 1px 1px 0 var(--jp-pink) !important;
  transform: scale(0.98);
}

/* 上架：青色马克笔文字；下架：红色马克笔文字 */
.shelf-btn.el-button--success,
.shelf-btn.el-button--success:hover {
  color: var(--jp-sky) !important;
  background: #ffffff !important;
  border-color: var(--jp-line) !important;
}

.shelf-btn.el-button--warning,
.shelf-btn.el-button--warning:hover {
  color: var(--jp-pink) !important;
  background: #ffffff !important;
  border-color: var(--jp-line) !important;
}

/* ---------- 底部导航：纸底、虚线墨线、紧凑高度 ---------- */
.bottom-nav {
  position: sticky;
  bottom: 0;
  z-index: 10;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 4px;
  padding: 10px 14px;
  background: var(--jp-paper);
  border-top: 1px dashed var(--jp-line);
  border-radius: 4px 4px 0 0;
  box-shadow: none;
}

.bottom-item {
  padding: 4px 0;
  border: 1px dashed transparent;
  border-radius: 4px;
  text-align: center;
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.04em;
  color: var(--jp-text);
  text-decoration: none;
  transition: all 300ms ease-out;
}

.bottom-item:hover {
  color: var(--jp-text);
  background: rgba(255, 217, 61, 0.35);
  border-color: var(--jp-line);
  transform: rotate(-1deg);
}

.bottom-item.router-link-active {
  color: var(--jp-text);
  font-weight: 500;
  background: var(--jp-powder);
  border: 1px dashed var(--jp-line);
  box-shadow: 2px 2px 0 var(--jp-line);
  text-decoration: underline wavy var(--jp-pink) 1px;
  text-underline-offset: 4px;
}

/* ---------- 移动端：留白收敛，商品单列 ---------- */
@media (max-width: 767px) {
  .banner {
    margin-top: 12px;
    padding: 24px 20px;
  }

  /* 移动端胶带收窄，避免压住标题 */
  .banner::before,
  .banner::after {
    width: 56px;
  }

  .banner-title {
    font-size: 21px;
  }

  .goods-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .goods-card :deep(.el-card__body) {
    padding: 18px;
  }
}

/* 动效降级：用户偏好减少动态时关闭位移/缩放/旋转 */
@media (prefers-reduced-motion: reduce) {
  .category-card,
  .goods-card,
  .add-cart-btn,
  .shelf-btn,
  .bottom-item,
  .section-more {
    transition: none !important;
  }

  .banner,
  .category-card,
  .category-card:nth-child(even),
  .goods-card,
  .goods-card:nth-child(even),
  .category-card:hover,
  .category-card:active,
  .goods-card:hover,
  .goods-card:active,
  .add-cart-btn:hover:not(.is-disabled),
  .add-cart-btn:active:not(.is-disabled),
  .shelf-btn:hover,
  .shelf-btn:active,
  .bottom-item:hover,
  .section-more:hover {
    transform: none !important;
  }
}
</style>
