<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SelectButton from 'primevue/selectbutton'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Message from 'primevue/message'
import Button from 'primevue/button'
import ProgressSpinner from 'primevue/progressspinner'
import { createPost, getPost, POST_CATEGORIES, updatePost } from '@/api/posts'
import { useToast } from '@/stores/toast'

const route = useRoute()
const router = useRouter()
const { showToast } = useToast()

const postId = computed(() => (route.params.postId ? Number(route.params.postId) : null))
const isEditMode = computed(() => Boolean(postId.value))
const selectableCategories = POST_CATEGORIES.filter((item) => item.value)

const form = reactive({
  category: 'INFO',
  title: '',
  content: '',
})

const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')

async function loadPost() {
  if (!isEditMode.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await getPost(postId.value)
    form.category = data.category || 'INFO'
    form.title = data.title || ''
    form.content = data.content || ''
  } catch (err) {
    errorMessage.value = err.message || '게시글을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  errorMessage.value = ''
  submitting.value = true
  const payload = {
    category: form.category,
    title: form.title.trim(),
    content: form.content.trim(),
  }

  try {
    if (isEditMode.value) {
      await updatePost(postId.value, payload)
      showToast('게시글을 수정했어요.', 'success')
      router.replace(`/community/${postId.value}`)
    } else {
      const created = await createPost(payload)
      showToast('게시글을 등록했어요!', 'success')
      router.replace(`/community/${created.id}`)
    }
  } catch (err) {
    errorMessage.value = err.message || '저장에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}

onMounted(loadPost)
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

    <form
      v-else
      class="bg-white rounded-3xl p-5 border border-slate-200/80 space-y-4"
      @submit.prevent="handleSubmit"
    >
      <div class="space-y-1.5">
        <span class="block text-xs font-bold text-slate-700">카테고리</span>
        <SelectButton
          v-model="form.category"
          :options="selectableCategories"
          optionLabel="label"
          optionValue="value"
          :allowEmpty="false"
          fluid
          :pt="{
            root: { class: '!grid !grid-cols-4 !gap-1.5' },
            pcToggleButton: { root: { class: '!h-11 !rounded-xl !text-xs !font-bold' } },
          }"
        />
      </div>

      <div class="space-y-1.5">
        <label for="post-title" class="block text-xs font-bold text-slate-700">
          제목 <span class="text-rose-500">*</span>
        </label>
        <InputText
          id="post-title"
          v-model="form.title"
          fluid
          required
          maxlength="150"
          placeholder="제목을 입력해주세요"
          class="!h-12 !rounded-2xl !text-sm"
        />
      </div>

      <div class="space-y-1.5">
        <label for="post-content" class="block text-xs font-bold text-slate-700">
          내용 <span class="text-rose-500">*</span>
        </label>
        <Textarea
          id="post-content"
          v-model="form.content"
          fluid
          autoResize
          rows="8"
          required
          maxlength="10000"
          placeholder="반려동물 돌봄 경험, 추천 용품, 궁금한 점을 자유롭게 적어주세요."
          class="!rounded-2xl !text-sm"
        />
      </div>

      <Message v-if="errorMessage" severity="error" :closable="false">
        {{ errorMessage }}
      </Message>

      <Button
        type="submit"
        fluid
        :disabled="submitting"
        class="!h-12 !rounded-2xl !font-bold !text-sm"
      >
        <i v-if="submitting" class="pi pi-spin pi-spinner text-sm" />
        <span>{{ submitting ? '저장 중...' : isEditMode ? '수정 완료' : '게시글 등록하기' }}</span>
      </Button>
    </form>
  </section>
</template>
