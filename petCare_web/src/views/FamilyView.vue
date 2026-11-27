<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Avatar from 'primevue/avatar'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import { getMyFamily } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/stores/toast'
import { copyToClipboard } from '@/utils/petFormat'
import ConfirmSheet from '@/components/ConfirmSheet.vue'

const router = useRouter()
const { state, clearSession, updateFamilyMeta } = useAuthStore()
const { showToast } = useToast()

const family = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const logoutSheetOpen = ref(false)

async function fetchFamily() {
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await getMyFamily()
    family.value = data
    updateFamilyMeta(data)
  } catch (err) {
    errorMessage.value = err.message || '가족 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function handleCopyCode() {
  const code = family.value?.inviteCode || state.user?.inviteCode
  if (!code) return
  const ok = await copyToClipboard(code)
  showToast(ok ? `초대 코드 ${code}가 복사되었어요` : '복사에 실패했어요', ok ? 'success' : 'error')
}

function confirmLogout() {
  logoutSheetOpen.value = false
  clearSession()
  showToast('로그아웃되었습니다.', 'info')
  router.replace('/')
}

onMounted(fetchFamily)
</script>

<template>
  <section class="space-y-4">
    <div
      v-if="loading"
      class="bg-white rounded-2xl p-6 border border-slate-200/80 flex flex-col items-center gap-2 text-sm text-slate-500"
    >
      <ProgressSpinner style="width: 32px; height: 32px" strokeWidth="4" />
      <span>가족 정보를 불러오는 중...</span>
    </div>

    <div v-else-if="errorMessage" class="space-y-2.5">
      <Message severity="error" :closable="false">{{ errorMessage }}</Message>
      <Button
        severity="danger"
        outlined
        size="small"
        class="!h-10 !rounded-xl !text-xs !font-semibold"
        @click="fetchFamily"
      >
        다시 불러오기
      </Button>
    </div>

    <template v-else-if="family">
      <!-- 가족 초대 코드 카드 -->
      <div class="rounded-3xl bg-gradient-to-br from-emerald-600 to-teal-600 p-5 text-white space-y-4 shadow-sm">
        <div>
          <p class="text-xs text-emerald-100">우리 가족 그룹</p>
          <h2 class="text-xl font-bold mt-0.5">{{ family.name }}</h2>
        </div>

        <div class="rounded-2xl bg-white/15 backdrop-blur p-4 flex items-center justify-between gap-3">
          <div>
            <p class="text-[11px] text-emerald-100">가족 초대 고유 번호</p>
            <p class="text-xl font-extrabold tracking-widest mt-0.5">{{ family.inviteCode }}</p>
          </div>
          <Button
            severity="secondary"
            size="small"
            class="!bg-white !text-emerald-700 !border-0 !h-11 !px-4 !rounded-xl !text-xs !font-bold shrink-0"
            @click="handleCopyCode"
          >
            <i class="pi pi-copy text-xs" />
            <span>코드 복사</span>
          </Button>
        </div>

        <p class="text-xs text-emerald-50/90 leading-relaxed">
          다른 가족이 회원가입할 때 <strong>“가족 코드로 참여”</strong>에서 위 번호를 입력하면 같은 가족으로 자동 연결됩니다.
        </p>
      </div>

      <!-- 구성원 목록 -->
      <div class="bg-white rounded-3xl p-4 border border-slate-200/80 space-y-3">
        <div class="flex items-center justify-between">
          <h3 class="text-sm font-bold text-slate-900">가족 구성원</h3>
          <Tag severity="success" rounded :value="`${family.members.length}명`" class="!text-xs" />
        </div>

        <ul class="divide-y divide-slate-100">
          <li
            v-for="member in family.members"
            :key="member.memberId"
            class="py-3 first:pt-1 last:pb-1 flex items-center justify-between gap-3"
          >
            <div class="flex items-center gap-3 min-w-0">
              <Avatar
                :label="member.nickname?.slice(0, 1) || '?'"
                shape="circle"
                class="!w-10 !h-10 !bg-slate-100 !text-slate-700 !font-bold !text-sm shrink-0"
              />
              <div class="min-w-0">
                <div class="flex items-center gap-1.5">
                  <p class="text-sm font-bold text-slate-900 truncate">{{ member.nickname }}</p>
                  <Tag
                    v-if="member.familyOwner"
                    value="대표"
                    severity="success"
                    rounded
                    class="!text-[10px] !py-0.5 !px-2"
                  />
                  <Tag
                    v-if="member.memberId === state.user?.memberId"
                    value="나"
                    severity="secondary"
                    rounded
                    class="!text-[10px] !py-0.5 !px-2"
                  />
                </div>
                <p class="text-xs text-slate-500 truncate">{{ member.email }}</p>
              </div>
            </div>
          </li>
        </ul>
      </div>

      <!-- 계정 액션 -->
      <div class="bg-white rounded-2xl p-2 border border-slate-200/80">
        <Button
          text
          severity="danger"
          fluid
          class="!h-12 !rounded-xl !font-semibold !text-sm"
          @click="logoutSheetOpen = true"
        >
          <i class="pi pi-sign-out text-sm" />
          <span>로그아웃</span>
        </Button>
      </div>
    </template>

    <ConfirmSheet
      :open="logoutSheetOpen"
      title="로그아웃할까요?"
      description="다시 로그인하면 우리 가족의 반려동물과 기록을 그대로 이어서 볼 수 있어요."
      confirm-text="로그아웃"
      danger
      @confirm="confirmLogout"
      @cancel="logoutSheetOpen = false"
    />
  </section>
</template>
