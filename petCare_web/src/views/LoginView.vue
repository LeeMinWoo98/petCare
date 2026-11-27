<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Message from 'primevue/message'
import Button from 'primevue/button'
import { login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/stores/toast'

const route = useRoute()
const router = useRouter()
const { setSession } = useAuthStore()
const { showToast } = useToast()

const email = ref('')
const password = ref('')
const submitting = ref(false)
const errorMessage = ref('')

async function handleSubmit() {
  errorMessage.value = ''
  submitting.value = true
  try {
    const res = await login({
      email: email.value.trim(),
      password: password.value,
    })
    setSession(res)
    showToast(`${res.nickname}님, 환영합니다!`, 'success')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect)
  } catch (err) {
    errorMessage.value = err.message || '로그인에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="space-y-5 pt-2">
    <div class="space-y-1">
      <h2 class="text-xl font-bold text-slate-900">다시 만나서 반가워요</h2>
      <p class="text-sm text-slate-600">가입한 이메일과 비밀번호로 로그인해주세요.</p>
    </div>

    <form class="bg-white rounded-3xl p-5 border border-slate-200/80 space-y-4" @submit.prevent="handleSubmit">
      <div class="space-y-1.5">
        <label for="login-email" class="block text-xs font-bold text-slate-700">이메일</label>
        <InputText
          id="login-email"
          v-model="email"
          type="email"
          fluid
          required
          autocomplete="email"
          placeholder="mom@example.com"
          class="!h-12 !rounded-2xl !text-sm"
        />
      </div>

      <div class="space-y-1.5">
        <label for="login-password" class="block text-xs font-bold text-slate-700">비밀번호</label>
        <Password
          inputId="login-password"
          v-model="password"
          :feedback="false"
          toggleMask
          fluid
          required
          placeholder="비밀번호 입력"
          :inputProps="{ autocomplete: 'current-password' }"
          inputClass="!h-12 !rounded-2xl !text-sm"
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
        <span>{{ submitting ? '로그인 중...' : '로그인' }}</span>
      </Button>
    </form>

    <div class="bg-white rounded-2xl p-4 border border-slate-200/80 space-y-2.5 text-center">
      <p class="text-xs text-slate-600">아직 PetCare 가족 계정이 없으신가요?</p>
      <div class="grid grid-cols-2 gap-2">
        <RouterLink to="/signup?mode=create" custom v-slot="{ href, navigate }">
          <Button
            as="a"
            :href="href"
            severity="success"
            outlined
            size="small"
            class="!h-11 !rounded-xl !font-semibold !text-xs no-underline justify-center"
            @click="navigate"
          >
            새 가족 만들기
          </Button>
        </RouterLink>
        <RouterLink to="/signup?mode=join" custom v-slot="{ href, navigate }">
          <Button
            as="a"
            :href="href"
            severity="secondary"
            size="small"
            class="!h-11 !rounded-xl !font-semibold !text-xs no-underline justify-center"
            @click="navigate"
          >
            초대 코드 합류
          </Button>
        </RouterLink>
      </div>
    </div>
  </section>
</template>
