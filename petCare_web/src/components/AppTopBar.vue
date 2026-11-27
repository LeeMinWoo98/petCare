<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/stores/toast'
import { copyToClipboard } from '@/utils/petFormat'

const route = useRoute()
const router = useRouter()
const { state, isAuthenticated } = useAuthStore()
const { showToast } = useToast()

const showBack = computed(() => Boolean(route.meta?.showBack))
const pageTitle = computed(() => route.meta?.title || 'PetCare')
const isHome = computed(() => route.name === 'home')

function handleBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

async function handleCopyInvite() {
  const code = state.user?.inviteCode
  if (!code) return
  const copied = await copyToClipboard(code)
  if (copied) {
    showToast(`가족 코드 ${code}를 복사했어요`, 'success')
  } else {
    showToast('복사에 실패했어요. 코드를 직접 선택해 복사해주세요.', 'error')
  }
}
</script>

<template>
  <header
    class="sticky top-0 z-30 h-14 safe-pt bg-white/95 backdrop-blur border-b border-slate-200/80 px-4 flex items-center justify-between gap-2"
  >
    <div class="flex items-center gap-2 min-w-0">
      <Button
        v-if="showBack"
        iconOnly
        text
        severity="secondary"
        aria-label="뒤로 가기"
        class="-ml-2 !w-11 !h-11 !rounded-xl shrink-0"
        @click="handleBack"
      >
        <i class="pi pi-arrow-left text-base" />
      </Button>

      <RouterLink v-else-if="isHome" to="/" class="flex items-center gap-2 min-w-0">
        <span
          class="w-8 h-8 rounded-xl bg-emerald-600 text-white font-bold text-sm flex items-center justify-center shrink-0"
        >
          🐾
        </span>
        <div class="leading-tight truncate">
          <p class="text-base font-bold tracking-tight text-slate-900">PetCare</p>
          <p v-if="isAuthenticated && state.user?.familyName" class="text-[11px] text-emerald-700 font-medium truncate">
            {{ state.user.familyName }}
          </p>
        </div>
      </RouterLink>

      <h1 v-if="showBack || !isHome" class="text-base font-bold text-slate-900 truncate">
        {{ pageTitle }}
      </h1>
    </div>

    <div class="flex items-center gap-1.5 shrink-0">
      <Button
        v-if="isAuthenticated && state.user?.inviteCode && !showBack"
        size="small"
        outlined
        severity="success"
        class="!h-9 !px-2.5 !rounded-xl !text-xs !font-semibold shrink-0"
        @click="handleCopyInvite"
      >
        <span>{{ state.user.inviteCode }}</span>
        <i class="pi pi-copy text-xs" />
      </Button>

      <RouterLink v-else-if="!isAuthenticated && !showBack" to="/login" custom v-slot="{ href, navigate }">
        <Button
          as="a"
          :href="href"
          size="small"
          class="!h-9 !px-3 !rounded-xl !text-xs !font-semibold no-underline"
          @click="navigate"
        >
          로그인
        </Button>
      </RouterLink>
    </div>
  </header>
</template>
