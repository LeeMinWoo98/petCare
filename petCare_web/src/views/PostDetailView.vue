<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Tag from 'primevue/tag'
import Divider from 'primevue/divider'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import {
  addComment,
  deleteComment,
  deletePost,
  getCategoryMeta,
  getPost,
} from '@/api/posts'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/stores/toast'
import ConfirmSheet from '@/components/ConfirmSheet.vue'

const route = useRoute()
const router = useRouter()
const { state, isAuthenticated } = useAuthStore()
const { showToast } = useToast()

const CATEGORY_SEVERITY = {
  INFO: 'success',
  QUESTION: 'info',
  REVIEW: 'warn',
  FREE: 'secondary',
}

const postId = computed(() => Number(route.params.postId))
const post = ref(null)
const loading = ref(false)
const errorMessage = ref('')

const commentText = ref('')
const submittingComment = ref(false)

const deletePostSheetOpen = ref(false)
const deletingPost = ref(false)
const deleteCommentTarget = ref(null)
const deletingComment = ref(false)

const isAuthor = computed(
  () => Boolean(post.value && state.user?.memberId && post.value.authorId === state.user.memberId),
)

function formatDateTime(isoString) {
  if (!isoString) return ''
  const dt = new Date(isoString)
  if (Number.isNaN(dt.getTime())) return ''
  return dt.toLocaleString('ko-KR', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

async function fetchDetail() {
  loading.value = true
  errorMessage.value = ''
  try {
    post.value = await getPost(postId.value)
  } catch (err) {
    errorMessage.value = err.message || '게시글을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function handleAddComment() {
  const trimmed = commentText.value.trim()
  if (!trimmed) return
  submittingComment.value = true
  try {
    const created = await addComment(postId.value, trimmed)
    post.value.comments = [...(post.value.comments || []), created]
    commentText.value = ''
    showToast('댓글을 등록했어요.', 'success')
  } catch (err) {
    showToast(err.message || '댓글 등록에 실패했습니다.', 'error')
  } finally {
    submittingComment.value = false
  }
}

async function handleConfirmDeleteComment() {
  if (!deleteCommentTarget.value) return
  deletingComment.value = true
  try {
    const targetId = deleteCommentTarget.value.id
    await deleteComment(postId.value, targetId)
    post.value.comments = post.value.comments.filter((c) => c.id !== targetId)
    deleteCommentTarget.value = null
    showToast('댓글을 삭제했어요.', 'info')
  } catch (err) {
    showToast(err.message || '댓글 삭제에 실패했습니다.', 'error')
  } finally {
    deletingComment.value = false
  }
}

async function handleConfirmDeletePost() {
  deletingPost.value = true
  try {
    await deletePost(postId.value)
    deletePostSheetOpen.value = false
    showToast('게시글을 삭제했어요.', 'info')
    router.replace('/community')
  } catch (err) {
    showToast(err.message || '게시글 삭제에 실패했습니다.', 'error')
  } finally {
    deletingPost.value = false
  }
}

onMounted(fetchDetail)
</script>

<template>
  <section class="space-y-4">
    <div
      v-if="loading"
      class="bg-white rounded-2xl p-6 border border-slate-200/80 flex flex-col items-center gap-2 text-sm text-slate-500"
    >
      <ProgressSpinner style="width: 32px; height: 32px" strokeWidth="4" />
      <span>게시글을 불러오는 중...</span>
    </div>

    <div v-else-if="errorMessage" class="space-y-2.5">
      <Message severity="error" :closable="false">{{ errorMessage }}</Message>
      <RouterLink to="/community" custom v-slot="{ href, navigate }">
        <Button
          as="a"
          :href="href"
          severity="secondary"
          outlined
          size="small"
          class="!h-10 !rounded-xl !text-xs !font-semibold no-underline"
          @click="navigate"
        >
          목록으로 돌아가기
        </Button>
      </RouterLink>
    </div>

    <template v-else-if="post">
      <!-- 게시글 본문 카드 -->
      <article class="bg-white rounded-3xl p-5 border border-slate-200/80 space-y-4">
        <div class="space-y-2">
          <Tag
            :value="getCategoryMeta(post.category).label"
            :severity="CATEGORY_SEVERITY[post.category] || 'secondary'"
            rounded
            class="!text-[11px]"
          />
          <h2 class="text-lg font-bold text-slate-900 leading-snug">{{ post.title }}</h2>
          <div class="flex items-center justify-between text-xs text-slate-500">
            <span>
              <strong class="text-slate-800">{{ post.authorNickname }}</strong> ·
              {{ formatDateTime(post.createdAt) }}
            </span>
            <span>조회 {{ post.viewCount }}</span>
          </div>
        </div>

        <Divider class="!my-1" />

        <div class="text-sm text-slate-800 leading-relaxed whitespace-pre-line break-words min-h-[80px]">
          {{ post.content }}
        </div>

        <div v-if="isAuthor" class="grid grid-cols-2 gap-2 pt-2 border-t border-slate-100">
          <RouterLink :to="`/community/${post.id}/edit`" custom v-slot="{ href, navigate }">
            <Button
              as="a"
              :href="href"
              severity="secondary"
              class="!h-11 !rounded-2xl !text-xs !font-bold no-underline justify-center"
              @click="navigate"
            >
              수정하기
            </Button>
          </RouterLink>
          <Button
            severity="danger"
            outlined
            class="!h-11 !rounded-2xl !text-xs !font-bold"
            @click="deletePostSheetOpen = true"
          >
            삭제하기
          </Button>
        </div>
      </article>

      <!-- 댓글 섹션 -->
      <div class="bg-white rounded-3xl p-4 border border-slate-200/80 space-y-3">
        <div class="flex items-center justify-between">
          <h3 class="text-sm font-bold text-slate-900">댓글</h3>
          <Tag severity="success" rounded :value="`${post.comments?.length || 0}개`" class="!text-xs" />
        </div>

        <p
          v-if="!post.comments || post.comments.length === 0"
          class="py-4 text-center text-xs text-slate-500"
        >
          첫 댓글을 남겨보세요!
        </p>

        <ul v-else class="divide-y divide-slate-100">
          <li
            v-for="comment in post.comments"
            :key="comment.id"
            class="py-3 first:pt-1 last:pb-1 space-y-1"
          >
            <div class="flex items-center justify-between gap-2">
              <div class="flex items-center gap-1.5 text-xs">
                <span class="font-bold text-slate-800">{{ comment.authorNickname }}</span>
                <span class="text-[11px] text-slate-400">{{ formatDateTime(comment.createdAt) }}</span>
              </div>
              <Button
                v-if="state.user?.memberId && comment.authorId === state.user.memberId"
                text
                severity="danger"
                size="small"
                class="!p-1 !h-7 !text-[11px] !font-semibold"
                @click="deleteCommentTarget = comment"
              >
                삭제
              </Button>
            </div>
            <p class="text-xs text-slate-700 leading-relaxed whitespace-pre-line break-words">
              {{ comment.content }}
            </p>
          </li>
        </ul>

        <!-- 댓글 작성 폼 -->
        <form v-if="isAuthenticated" class="flex gap-2 pt-2 border-t border-slate-100" @submit.prevent="handleAddComment">
          <InputText
            v-model="commentText"
            fluid
            required
            maxlength="2000"
            placeholder="따뜻한 댓글을 남겨주세요"
            class="flex-1 !h-11 !rounded-2xl !text-xs"
          />
          <Button
            type="submit"
            :disabled="submittingComment || !commentText.trim()"
            class="!h-11 !px-4 !rounded-2xl !text-xs !font-bold shrink-0"
          >
            <i v-if="submittingComment" class="pi pi-spin pi-spinner text-xs" />
            <span>등록</span>
          </Button>
        </form>

        <div v-else class="pt-2 border-t border-slate-100 text-center">
          <RouterLink
            :to="{ name: 'login', query: { redirect: `/community/${post.id}` } }"
            custom
            v-slot="{ href, navigate }"
          >
            <Button
              as="a"
              :href="href"
              severity="secondary"
              size="small"
              class="!h-10 !px-4 !rounded-xl !text-xs !font-semibold no-underline"
              @click="navigate"
            >
              로그인하고 댓글 작성하기 →
            </Button>
          </RouterLink>
        </div>
      </div>
    </template>

    <ConfirmSheet
      :open="deletePostSheetOpen"
      title="게시글을 삭제할까요?"
      description="삭제된 게시글은 커뮤니티 목록에서 더 이상 보이지 않습니다."
      confirm-text="삭제하기"
      danger
      :loading="deletingPost"
      @confirm="handleConfirmDeletePost"
      @cancel="deletePostSheetOpen = false"
    />

    <ConfirmSheet
      :open="Boolean(deleteCommentTarget)"
      title="댓글을 삭제할까요?"
      description="삭제된 댓글은 복구할 수 없습니다."
      confirm-text="삭제하기"
      danger
      :loading="deletingComment"
      @confirm="handleConfirmDeleteComment"
      @cancel="deleteCommentTarget = null"
    />
  </section>
</template>
