<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SelectButton from 'primevue/selectbutton'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Message from 'primevue/message'
import Button from 'primevue/button'
import { signup } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/stores/toast'

const route = useRoute()
const router = useRouter()
const { setSession } = useAuthStore()
const { showToast } = useToast()

const SIGNUP_MODES = [
  { label: '새 가족 만들기', value: 'CREATE_FAMILY' },
  { label: '가족 코드로 참여', value: 'JOIN_FAMILY' },
]

const mode = ref(route.query.mode === 'join' ? 'JOIN_FAMILY' : 'CREATE_FAMILY')
const email = ref('')
const password = ref('')
const nickname = ref('')
const familyName = ref('')
const inviteCode = ref('')
const submitting = ref(false)
const errorMessage = ref('')
const fieldErrors = ref({})

watch(
  () => route.query.mode,
  (nextMode) => {
    if (nextMode === 'join') mode.value = 'JOIN_FAMILY'
    else if (nextMode === 'create') mode.value = 'CREATE_FAMILY'
  },
)

const isCreateMode = computed(() => mode.value === 'CREATE_FAMILY')

async function handleSubmit() {
  errorMessage.value = ''
  fieldErrors.value = {}
  submitting.value = true

  try {
    const payload = {
      email: email.value.trim(),
      password: password.value,
      nickname: nickname.value.trim(),
      mode: mode.value,
      familyName: isCreateMode.value ? familyName.value.trim() || null : null,
      inviteCode: !isCreateMode.value ? inviteCode.value.trim().toUpperCase() : null,
    }

    const res = await signup(payload)
    setSession(res)
    if (isCreateMode.value) {
      showToast(`가족이 생성되었어요! 초대 코드: ${res.inviteCode}`, 'success', 4000)
    } else {
      showToast(`${res.familyName || '가족'} 그룹에 합류했어요!`, 'success')
    }
    router.replace('/')
  } catch (err) {
    errorMessage.value = err.message || '회원가입에 실패했습니다.'
    fieldErrors.value = err.fieldErrors || {}
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="space-y-4 pt-1">
    <!-- 가입 방식 세그먼트 토글 -->
    <SelectButton
      v-model="mode"
      :options="SIGNUP_MODES"
      optionLabel="label"
      optionValue="value"
      :allowEmpty="false"
      fluid
      :pt="{
        root: { class: '!grid !grid-cols-2 !gap-1.5' },
        pcToggleButton: { root: { class: '!h-11 !rounded-xl !text-xs !font-bold' } },
      }"
    />

    <Message severity="success" :closable="false">
      <span class="text-xs leading-relaxed">
        <template v-if="isCreateMode">
          우리 집 대표로 가입하면 <strong>가족 고유 번호(예: PET-7K4M9Q)</strong>가 자동 발급됩니다. 이 번호를 가족에게 공유해 주세요.
        </template>
        <template v-else>
          먼저 가입한 가족에게 받은 <strong>가족 고유 번호</strong>를 입력하면 같은 반려동물과 일정을 바로 공유할 수 있어요.
        </template>
      </span>
    </Message>

    <form class="bg-white rounded-3xl p-5 border border-slate-200/80 space-y-4" @submit.prevent="handleSubmit">
      <div v-if="isCreateMode" class="space-y-1.5">
        <label for="signup-family-name" class="block text-xs font-bold text-slate-700">
          가족 별칭 <span class="text-slate-400 font-normal">(선택)</span>
        </label>
        <InputText
          id="signup-family-name"
          v-model="familyName"
          fluid
          maxlength="50"
          placeholder="예: 콩이네 가족"
          class="!h-12 !rounded-2xl !text-sm"
        />
      </div>

      <div v-else class="space-y-1.5">
        <label for="signup-invite-code" class="block text-xs font-bold text-slate-700">
          가족 초대 코드 <span class="text-rose-500">*</span>
        </label>
        <InputText
          id="signup-invite-code"
          v-model="inviteCode"
          fluid
          required
          maxlength="20"
          placeholder="예: PET-7K4M9Q"
          class="!h-12 !rounded-2xl !text-sm uppercase tracking-wider font-semibold"
        />
      </div>

      <div class="space-y-1.5">
        <label for="signup-nickname" class="block text-xs font-bold text-slate-700">
          가족 내 호칭(닉네임) <span class="text-rose-500">*</span>
        </label>
        <InputText
          id="signup-nickname"
          v-model="nickname"
          fluid
          required
          maxlength="30"
          placeholder="예: 엄마, 아빠, 첫째"
          :invalid="Boolean(fieldErrors.nickname)"
          class="!h-12 !rounded-2xl !text-sm"
        />
        <p v-if="fieldErrors.nickname" class="text-xs text-rose-600">{{ fieldErrors.nickname }}</p>
      </div>

      <div class="space-y-1.5">
        <label for="signup-email" class="block text-xs font-bold text-slate-700">
          이메일 <span class="text-rose-500">*</span>
        </label>
        <InputText
          id="signup-email"
          v-model="email"
          type="email"
          fluid
          required
          autocomplete="email"
          placeholder="example@email.com"
          :invalid="Boolean(fieldErrors.email)"
          class="!h-12 !rounded-2xl !text-sm"
        />
        <p v-if="fieldErrors.email" class="text-xs text-rose-600">{{ fieldErrors.email }}</p>
      </div>

      <div class="space-y-1.5">
        <label for="signup-password" class="block text-xs font-bold text-slate-700">
          비밀번호 <span class="text-rose-500">*</span>
        </label>
        <Password
          inputId="signup-password"
          v-model="password"
          :feedback="false"
          toggleMask
          fluid
          required
          placeholder="4자 이상 입력"
          :invalid="Boolean(fieldErrors.password)"
          :inputProps="{ autocomplete: 'new-password', minlength: 4 }"
          inputClass="!h-12 !rounded-2xl !text-sm"
        />
        <p v-if="fieldErrors.password" class="text-xs text-rose-600">{{ fieldErrors.password }}</p>
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
        <span>{{ submitting ? '가입 처리 중...' : isCreateMode ? '가족 만들고 가입하기' : '가족 코드로 합류하기' }}</span>
      </Button>
    </form>

    <div class="text-center">
      <RouterLink to="/login" class="inline-flex items-center h-11 px-4 text-xs font-semibold text-slate-600">
        이미 계정이 있으신가요? <span class="text-emerald-700 ml-1">로그인</span>
      </RouterLink>
    </div>
  </section>
</template>
