<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import { getCategoryMeta, listPosts, POST_CATEGORIES } from '@/api/posts'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const { isAuthenticated } = useAuthStore()

const CATEGORY_SEVERITY = {
  INFO: 'success',
  QUESTION: 'info',
  REVIEW: 'warn',
  FREE: 'secondary',
}

const selectedCategory = ref('')
const keyword = ref('')
const posts = ref([])
const page = ref(0)
const hasNext = ref(false)
const totalElements = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const errorMessage = ref('')

function formatDateTime(isoString) {
  if (!isoString) return ''
  const dt = new Date(isoString)
  if (Number.isNaN(dt.getTime())) return ''
  const m = dt.getMonth() + 1
  const d = dt.getDate()
  const hh = String(dt.getHours()).padStart(2, '0')
  const mm = String(dt.getMinutes()).padStart(2, '0')
  return `${m}/${d} ${hh}:${mm}`
}

async function fetchPosts(reset = true) {
  if (reset) {
    loading.value = true
    page.value = 0
  } else {
    loadingMore.value = true
  }
  errorMessage.value = ''

  try {
    const res = await listPosts({
      category: selectedCategory.value,
      keyword: keyword.value,
      page: reset ? 0 : page.value + 1,
      size: 15,
    })
    if (reset) {
      posts.value = res.content
    } else {
      posts.value = [...posts.value, ...res.content]
    }
    page.value = res.page
    hasNext.value = res.hasNext
    totalElements.value = res.totalElements
  } catch (err) {
    errorMessage.value = err.message || '게시글을 불러오지 못했습니다.'
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function selectCategory(cat) {
  selectedCategory.value = cat
  fetchPosts(true)
}

function goWrite() {
  if (!isAuthenticated.value) {
    router.push({ name: 'login', query: { redirect: '/community/new' } })
    return
  }
  router.push('/community/new')
}

onMounted(() => fetchPosts(true))
</script>

<template>
  <section class="space-y-3.5">
    <!-- 상단 헤더 + 글쓰기 -->
    <div class="flex items-center justify-between gap-2">
      <div>
        <h2 class="text-base font-bold text-slate-900">반려동물 커뮤니티</h2>
        <p class="text-xs text-slate-500 mt-0.5">사료·병원 후기·건강 관리 팁을 나눠보세요.</p>
      </div>
      <Button
        size="small"
        class="!h-11 !px-4 !rounded-2xl !text-xs !font-bold shrink-0"
        @click="goWrite"
      >
        <i class="pi pi-pencil text-xs" />
        <span>글쓰기</span>
      </Button>
    </div>

    <!-- 검색 바 -->
    <form class="flex gap-1.5" @submit.prevent="fetchPosts(true)">
      <IconField class="flex-1">
        <InputIcon class="pi pi-search text-xs" />
        <InputText
          v-model="keyword"
          type="search"
          fluid
          placeholder="제목·내용 검색 (예: 슬개골, 사료, 병원)"
          class="!h-11 !rounded-2xl !text-sm !bg-white"
        />
      </IconField>
      <Button
        type="submit"
        severity="contrast"
        class="!h-11 !px-4 !rounded-2xl !text-xs !font-bold shrink-0"
      >
        검색
      </Button>
    </form>

    <!-- 카테고리 칩 -->
    <div class="flex gap-1.5 overflow-x-auto pb-0.5">
      <Button
        v-for="cat in POST_CATEGORIES"
        :key="cat.value || 'ALL'"
        size="small"
        :severity="selectedCategory === cat.value ? undefined : 'secondary'"
        :outlined="selectedCategory !== cat.value"
        class="!h-9 !px-3.5 !rounded-xl !text-xs !font-bold whitespace-nowrap shrink-0"
        @click="selectCategory(cat.value)"
      >
        {{ cat.label }}
      </Button>
    </div>

    <!-- 목록 영역 -->
    <div
      v-if="loading"
      class="bg-white rounded-2xl p-6 border border-slate-200/80 flex flex-col items-center gap-2 text-sm text-slate-500"
    >
      <ProgressSpinner style="width: 32px; height: 32px" strokeWidth="4" />
      <span>게시글을 불러오는 중...</span>
    </div>

    <div v-else-if="errorMessage" class="space-y-2.5">
      <Message severity="error" :closable="false">{{ errorMessage }}</Message>
      <Button
        severity="danger"
        outlined
        size="small"
        class="!h-10 !rounded-xl !text-xs !font-semibold"
        @click="fetchPosts(true)"
      >
        다시 시도
      </Button>
    </div>

    <div
      v-else-if="posts.length === 0"
      class="bg-white rounded-3xl p-7 border border-dashed border-slate-300 text-center space-y-2.5"
    >
      <div class="text-3xl">💬</div>
      <p class="text-sm font-bold text-slate-800">아직 등록된 글이 없어요</p>
      <p class="text-xs text-slate-500">우리 아이 돌봄 팁이나 궁금한 점을 첫 글로 남겨보세요!</p>
      <Button
        size="small"
        class="!h-11 !px-5 !rounded-2xl !text-xs !font-bold"
        @click="goWrite"
      >
        첫 글 작성하기
      </Button>
    </div>

    <div v-else class="space-y-2.5">
      <p class="text-[11px] font-semibold text-slate-500 px-1">총 {{ totalElements }}개의 글</p>

      <RouterLink
        v-for="post in posts"
        :key="post.id"
        :to="`/community/${post.id}`"
        class="block bg-white rounded-2xl p-4 border border-slate-200/80 space-y-2 active:scale-[0.99] transition"
      >
        <div class="flex items-center justify-between gap-2">
          <Tag
            :value="getCategoryMeta(post.category).label"
            :severity="CATEGORY_SEVERITY[post.category] || 'secondary'"
            rounded
            class="!text-[10px] !py-0.5 !px-2"
          />
          <span class="text-[11px] text-slate-400">{{ formatDateTime(post.createdAt) }}</span>
        </div>

        <div class="space-y-1">
          <h3 class="text-sm font-bold text-slate-900 line-clamp-1">{{ post.title }}</h3>
          <p class="text-xs text-slate-600 line-clamp-2 leading-relaxed">{{ post.excerpt }}</p>
        </div>

        <div class="flex items-center justify-between text-[11px] text-slate-500 pt-1 border-t border-slate-100">
          <span class="font-semibold text-slate-700">{{ post.authorNickname }}</span>
          <div class="flex items-center gap-3">
            <span>조회 {{ post.viewCount }}</span>
            <span class="text-emerald-700 font-semibold">댓글 {{ post.commentCount }}</span>
          </div>
        </div>
      </RouterLink>

      <Button
        v-if="hasNext"
        severity="secondary"
        outlined
        fluid
        :disabled="loadingMore"
        class="!h-11 !rounded-2xl !text-xs !font-bold !bg-white"
        @click="fetchPosts(false)"
      >
        <i v-if="loadingMore" class="pi pi-spin pi-spinner text-xs" />
        <span>{{ loadingMore ? '불러오는 중...' : '더 보기' }}</span>
      </Button>
    </div>
  </section>
</template>
