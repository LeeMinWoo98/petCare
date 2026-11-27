<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/stores/toast'
import { listPets } from '@/api/pets'
import {
  createCareLog,
  deleteCareLog,
  getDailyCareLogs,
  openFamilyEventStream,
} from '@/api/care'
import {
  cancelMedicationLog,
  listPetMedicines,
  recordMedicationDose,
} from '@/api/medicines'
import {
  copyToClipboard,
  formatPetAge,
  getGenderLabel,
  getSpeciesEmoji,
  getSpeciesLabel,
} from '@/utils/petFormat'
import CareLogSheet from '@/components/CareLogSheet.vue'
import ConfirmSheet from '@/components/ConfirmSheet.vue'

const { state, isAuthenticated } = useAuthStore()
const { showToast } = useToast()

const CARE_META = {
  WALK: { label: '산책', emoji: '🚶', badge: 'bg-emerald-50 text-emerald-700', severity: 'success' },
  MEAL: { label: '식사', emoji: '🍚', badge: 'bg-amber-50 text-amber-700', severity: 'warn' },
  SNACK: { label: '간식', emoji: '🦴', badge: 'bg-sky-50 text-sky-700', severity: 'info' },
}

function getTodayDateString() {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function shiftDate(dateStr, deltaDays) {
  const [y, m, d] = dateStr.split('-').map(Number)
  const dt = new Date(y, m - 1, d + deltaDays)
  const ny = dt.getFullYear()
  const nm = String(dt.getMonth() + 1).padStart(2, '0')
  const nd = String(dt.getDate()).padStart(2, '0')
  return `${ny}-${nm}-${nd}`
}

function formatKoreanDate(dateStr) {
  const [y, m, d] = dateStr.split('-').map(Number)
  const dt = new Date(y, m - 1, d)
  const weekdays = ['일', '월', '화', '수', '목', '금', '토']
  const today = getTodayDateString()
  const prefix = dateStr === today ? '오늘 · ' : ''
  return `${prefix}${m}월 ${d}일 (${weekdays[dt.getDay()]})`
}

function formatTime(isoString) {
  if (!isoString) return ''
  const dt = new Date(isoString)
  if (Number.isNaN(dt.getTime())) return ''
  return dt.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
}

const pets = ref([])
const selectedPetId = ref(null)
const selectedDate = ref(getTodayDateString())
const loadingPets = ref(false)
const petsError = ref('')

const careSummary = ref(null)
const loadingCare = ref(false)
const careError = ref('')

const medicines = ref([])
const loadingMedicines = ref(false)
const recordingMedId = ref(null)
const extraDoseTarget = ref(null)

const careSheetOpen = ref(false)
const careSheetType = ref('WALK')
const submittingCare = ref(false)

const deleteLogTarget = ref(null)
const deletingLog = ref(false)

const sseStatus = ref('disconnected')
let eventSource = null

const isToday = computed(() => selectedDate.value === getTodayDateString())
const selectedPet = computed(
  () => pets.value.find((p) => p.id === selectedPetId.value) || pets.value[0] || null,
)
const activeMedicines = computed(() => medicines.value.filter((med) => med.active))

async function fetchPets() {
  if (!isAuthenticated.value) {
    pets.value = []
    selectedPetId.value = null
    careSummary.value = null
    medicines.value = []
    return
  }
  loadingPets.value = true
  petsError.value = ''
  try {
    const data = await listPets()
    pets.value = data
    if (data.length > 0 && !data.some((p) => p.id === selectedPetId.value)) {
      selectedPetId.value = data[0].id
    }
  } catch (err) {
    petsError.value = err.message || '반려동물 정보를 불러오지 못했습니다.'
  } finally {
    loadingPets.value = false
  }
}

async function fetchCareLogs() {
  if (!isAuthenticated.value || !selectedPet.value) {
    careSummary.value = null
    return
  }
  loadingCare.value = true
  careError.value = ''
  try {
    careSummary.value = await getDailyCareLogs(selectedPet.value.id, selectedDate.value)
  } catch (err) {
    careError.value = err.message || '케어 기록을 불러오지 못했습니다.'
  } finally {
    loadingCare.value = false
  }
}

async function fetchMedicines() {
  if (!isAuthenticated.value || !selectedPet.value) {
    medicines.value = []
    return
  }
  loadingMedicines.value = true
  try {
    medicines.value = await listPetMedicines(selectedPet.value.id, selectedDate.value)
  } catch {
    medicines.value = []
  } finally {
    loadingMedicines.value = false
  }
}

function openCareSheet(type) {
  careSheetType.value = type
  careSheetOpen.value = true
}

function buildTimestampForSelectedDate() {
  if (isToday.value) return null
  return `${selectedDate.value}T12:00:00`
}

async function handleCreateCareLog({ careType, memo }) {
  if (!selectedPet.value) return
  submittingCare.value = true
  try {
    await createCareLog(selectedPet.value.id, {
      careType,
      performedAt: buildTimestampForSelectedDate(),
      memo,
    })
    careSheetOpen.value = false
    const label = CARE_META[careType]?.label || '케어'
    showToast(`${selectedPet.value.name} ${label} 기록을 가족과 공유했어요!`, 'success')
    await fetchCareLogs()
  } catch (err) {
    showToast(err.message || '기록 저장에 실패했습니다.', 'error')
  } finally {
    submittingCare.value = false
  }
}

async function handleConfirmDeleteLog() {
  if (!deleteLogTarget.value) return
  deletingLog.value = true
  try {
    await deleteCareLog(deleteLogTarget.value.id)
    deleteLogTarget.value = null
    showToast('케어 기록을 취소했어요.', 'info')
    await fetchCareLogs()
  } catch (err) {
    showToast(err.message || '기록 취소에 실패했습니다.', 'error')
  } finally {
    deletingLog.value = false
  }
}

async function handleMedicationClick(med) {
  if (med.depleted) {
    showToast('남은 약이 없습니다. 반려동물·약 탭에서 잔여량을 갱신해주세요.', 'error')
    return
  }
  if (med.todayCompleted) {
    extraDoseTarget.value = med
    return
  }
  await executeMedicationRecord(med)
}

async function executeMedicationRecord(med) {
  if (!selectedPet.value) return
  recordingMedId.value = med.id
  try {
    await recordMedicationDose(selectedPet.value.id, med.id, {
      medicatedAt: buildTimestampForSelectedDate(),
    })
    extraDoseTarget.value = null
    showToast(`${med.name} 투약을 가족과 공유했어요!`, 'success')
    await fetchMedicines()
  } catch (err) {
    showToast(err.message || '투약 체크에 실패했습니다.', 'error')
  } finally {
    recordingMedId.value = null
  }
}

async function handleCancelMedicationLog(logId) {
  try {
    await cancelMedicationLog(logId)
    showToast('투약 기록을 취소하고 잔여량을 복구했어요.', 'info')
    await fetchMedicines()
  } catch (err) {
    showToast(err.message || '투약 취소에 실패했습니다.', 'error')
  }
}

function connectSse() {
  disconnectSse()
  const memberId = state.user?.memberId
  if (!memberId) return

  sseStatus.value = 'reconnecting'
  const source = openFamilyEventStream(memberId)
  eventSource = source

  source.addEventListener('connected', () => {
    sseStatus.value = 'connected'
  })

  source.addEventListener('family-event', (event) => {
    try {
      const payload = JSON.parse(event.data)
      const isOtherMember = payload.actorMemberId !== state.user?.memberId

      if (payload.eventType === 'MEDICATION_LOG_CREATED' || payload.eventType === 'MEDICATION_LOG_DELETED') {
        const medLabel = payload.medicineName || '약'
        if (isOtherMember) {
          showToast(
            payload.eventType === 'MEDICATION_LOG_CREATED'
              ? `${payload.actorNickname}님이 ${medLabel} 투약을 완료했어요!`
              : `${payload.actorNickname}님이 ${medLabel} 투약을 취소했어요.`,
            'info',
          )
        }
        if (selectedPet.value && payload.petId === selectedPet.value.id) {
          fetchMedicines()
        }
        return
      }

      const careLabel = CARE_META[payload.careType]?.label || '케어'
      if (isOtherMember) {
        if (payload.eventType === 'CARE_LOG_CREATED') {
          showToast(`${payload.actorNickname}님이 ${careLabel} 기록을 남겼어요!`, 'info')
        } else if (payload.eventType === 'CARE_LOG_DELETED') {
          showToast(`${payload.actorNickname}님이 ${careLabel} 기록을 취소했어요.`, 'info')
        }
      }

      if (
        selectedPet.value &&
        payload.petId === selectedPet.value.id &&
        payload.careDate === selectedDate.value
      ) {
        fetchCareLogs()
      }
    } catch {
      // ignore malformed payload
    }
  })

  source.onerror = () => {
    sseStatus.value = 'reconnecting'
  }
}

function disconnectSse() {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
  sseStatus.value = 'disconnected'
}

async function handleCopyCode() {
  const code = state.user?.inviteCode
  if (!code) return
  const ok = await copyToClipboard(code)
  showToast(ok ? `초대 코드 ${code}가 복사되었어요` : '복사에 실패했어요', ok ? 'success' : 'error')
}

onMounted(async () => {
  await fetchPets()
  if (isAuthenticated.value) {
    connectSse()
  }
})

onBeforeUnmount(disconnectSse)

watch(isAuthenticated, async (authed) => {
  await fetchPets()
  if (authed) {
    connectSse()
  } else {
    disconnectSse()
  }
})

watch([selectedPet, selectedDate], () => {
  fetchCareLogs()
  fetchMedicines()
})
</script>

<template>
  <!-- 비로그인 온보딩 화면 -->
  <section v-if="!isAuthenticated" class="space-y-4">
    <div class="rounded-3xl bg-gradient-to-br from-emerald-600 to-teal-600 p-6 text-white shadow-sm space-y-4">
      <div class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/15 text-xs font-semibold">
        <span>🐾</span>
        <span>우리 가족 반려동물 케어 허브</span>
      </div>
      <div class="space-y-2">
        <h1 class="text-2xl font-bold leading-snug">
          산책·식사·투약 기록을<br />온 가족이 한눈에 공유해요
        </h1>
        <p class="text-sm text-emerald-50/95 leading-relaxed">
          대표 가족이 가입하면 발급되는 고유 번호로 온 가족이 같은 반려동물 일정을 실시간으로 확인할 수 있어요.
        </p>
      </div>
      <div class="grid grid-cols-1 gap-2.5 pt-1">
        <RouterLink to="/signup?mode=create" custom v-slot="{ href, navigate }">
          <Button
            as="a"
            :href="href"
            severity="secondary"
            class="!bg-white !text-emerald-700 !border-0 !h-12 !rounded-2xl !font-bold !text-sm shadow-sm no-underline justify-center"
            @click="navigate"
          >
            새 가족 만들고 시작하기
          </Button>
        </RouterLink>
        <RouterLink to="/signup?mode=join" custom v-slot="{ href, navigate }">
          <Button
            as="a"
            :href="href"
            outlined
            class="!bg-emerald-700/60 !text-white !border-white/25 !h-12 !rounded-2xl !font-semibold !text-sm no-underline justify-center"
            @click="navigate"
          >
            가족 초대 코드로 참여하기
          </Button>
        </RouterLink>
      </div>
    </div>

    <div class="space-y-2.5">
      <h2 class="text-sm font-bold text-slate-700 px-1">PetCare 주요 기능</h2>
      <div class="bg-white rounded-2xl p-4 border border-slate-200/80 flex items-start gap-3">
        <span class="w-10 h-10 rounded-xl bg-emerald-50 text-lg flex items-center justify-center shrink-0">
          🚶
        </span>
        <div>
          <p class="text-sm font-bold text-slate-900">당일 산책 · 식사 · 간식 실시간 공유</p>
          <p class="text-xs text-slate-600 mt-0.5 leading-relaxed">
            “오늘 밥 줬나?” 고민 없이 누가 언제 챙겼는지 가족 모두 실시간으로 확인해요.
          </p>
        </div>
      </div>

      <div class="bg-white rounded-2xl p-4 border border-slate-200/80 flex items-start gap-3">
        <span class="w-10 h-10 rounded-xl bg-amber-50 text-lg flex items-center justify-center shrink-0">
          💊
        </span>
        <div>
          <p class="text-sm font-bold text-slate-900">복용 약 · 투약 체크 · 잔여 일수</p>
          <p class="text-xs text-slate-600 mt-0.5 leading-relaxed">
            동물 의약품 검색과 급여 방법 기록, 오늘 투약 여부와 남은 약 일수를 자동 계산해요.
          </p>
        </div>
      </div>

      <div class="bg-white rounded-2xl p-4 border border-slate-200/80 flex items-start gap-3">
        <span class="w-10 h-10 rounded-xl bg-sky-50 text-lg flex items-center justify-center shrink-0">
          🩺
        </span>
        <div>
          <p class="text-sm font-bold text-slate-900">반려동물 커뮤니티 & 건강정보</p>
          <p class="text-xs text-slate-600 mt-0.5 leading-relaxed">
            검수된 최신 질병·신약·건강관리 소식과 반려인 정보 공유 게시판을 제공해요.
          </p>
        </div>
      </div>
    </div>

    <div class="text-center pt-1">
      <RouterLink to="/login" custom v-slot="{ href, navigate }">
        <Button
          as="a"
          :href="href"
          link
          class="!h-11 !px-4 !text-sm !font-semibold no-underline"
          @click="navigate"
        >
          이미 계정이 있으신가요? 로그인하기 →
        </Button>
      </RouterLink>
    </div>
  </section>

  <!-- 로그인 후 실시간 케어 & 투약 대시보드 -->
  <section v-else class="space-y-4">
    <!-- 상단 인사 + 실시간 SSE 배지 -->
    <div class="bg-white rounded-2xl p-4 border border-slate-200/80 flex items-center justify-between gap-2">
      <div class="min-w-0 space-y-1">
        <Tag
          :severity="sseStatus === 'connected' ? 'success' : 'warn'"
          rounded
          class="!text-[11px] !py-0.5 !px-2.5"
        >
          <span class="inline-flex items-center gap-1.5">
            <span
              class="w-2 h-2 rounded-full shrink-0"
              :class="sseStatus === 'connected' ? 'bg-emerald-500' : 'bg-amber-500 animate-pulse'"
            />
            <span>{{ sseStatus === 'connected' ? '가족 실시간 동기화 연결됨' : '실시간 동기화 연결 중...' }}</span>
          </span>
        </Tag>
        <h2 class="text-base font-bold text-slate-900 truncate">
          {{ state.user?.familyName || '우리 가족' }} 케어 보드
        </h2>
      </div>
      <Button
        severity="success"
        outlined
        size="small"
        class="!h-10 !px-3 !rounded-xl !text-xs !font-bold shrink-0"
        @click="handleCopyCode"
      >
        <i class="pi pi-copy text-xs" />
        <span>초대코드</span>
      </Button>
    </div>

    <!-- 반려동물 선택 칩 -->
    <div class="space-y-2">
      <div class="flex items-center justify-between px-1">
        <h3 class="text-xs font-bold text-slate-500">우리 가족 반려동물</h3>
        <RouterLink to="/pets/new" class="text-xs font-semibold text-emerald-700">
          + 추가하기
        </RouterLink>
      </div>

      <div
        v-if="loadingPets"
        class="bg-white rounded-2xl p-6 border border-slate-200/80 flex flex-col items-center gap-2 text-sm text-slate-500"
      >
        <ProgressSpinner style="width: 32px; height: 32px" strokeWidth="4" />
        <span>반려동물 정보를 불러오는 중...</span>
      </div>

      <div v-else-if="petsError" class="space-y-2.5">
        <Message severity="error" :closable="false">{{ petsError }}</Message>
        <Button
          severity="danger"
          outlined
          size="small"
          class="!h-10 !rounded-xl !text-xs !font-semibold"
          @click="fetchPets"
        >
          다시 시도
        </Button>
      </div>

      <div
        v-else-if="pets.length === 0"
        class="bg-white rounded-3xl p-6 border border-dashed border-emerald-300 text-center space-y-3"
      >
        <div class="text-3xl">🐶🐱</div>
        <div class="space-y-1">
          <p class="text-base font-bold text-slate-900">아직 등록된 반려동물이 없어요</p>
          <p class="text-xs text-slate-600">
            첫 반려동물을 등록하면 가족 모두와 산책·식사·간식·투약 기록을 공유할 수 있어요.
          </p>
        </div>
        <RouterLink to="/pets/new" custom v-slot="{ href, navigate }">
          <Button
            as="a"
            :href="href"
            class="!h-12 !px-5 !rounded-2xl !text-sm !font-bold no-underline"
            @click="navigate"
          >
            첫 반려동물 등록하기
          </Button>
        </RouterLink>
      </div>

      <template v-else>
        <div class="flex gap-2 overflow-x-auto pb-1">
          <Button
            v-for="pet in pets"
            :key="pet.id"
            :severity="selectedPet?.id === pet.id ? undefined : 'secondary'"
            :outlined="selectedPet?.id !== pet.id"
            class="!h-11 !px-4 !rounded-2xl !text-sm !font-semibold whitespace-nowrap shrink-0"
            @click="selectedPetId = pet.id"
          >
            <span>{{ getSpeciesEmoji(pet.species) }}</span>
            <span>{{ pet.name }}</span>
          </Button>
        </div>

        <!-- 선택 반려동물 요약 바 -->
        <div
          v-if="selectedPet"
          class="bg-white rounded-2xl px-4 py-3 border border-slate-200/80 flex items-center justify-between gap-3"
        >
          <div class="min-w-0">
            <p class="text-sm font-bold text-slate-900 truncate">
              {{ selectedPet.name }}
              <span class="text-xs font-medium text-slate-500 ml-1">
                {{ getSpeciesLabel(selectedPet.species) }} · {{ formatPetAge(selectedPet.birthDate) }} ·
                {{ getGenderLabel(selectedPet.gender) }}
              </span>
            </p>
            <p v-if="selectedPet.notes" class="text-[11px] text-amber-800 truncate mt-0.5">
              주의: {{ selectedPet.notes }}
            </p>
          </div>
          <RouterLink :to="`/pets/${selectedPet.id}/edit`" custom v-slot="{ href, navigate }">
            <Button
              as="a"
              :href="href"
              severity="secondary"
              size="small"
              class="!h-9 !px-3 !rounded-xl !text-xs !font-semibold shrink-0 no-underline"
              @click="navigate"
            >
              프로필
            </Button>
          </RouterLink>
        </div>

        <!-- 날짜 선택기 -->
        <div class="bg-white rounded-2xl px-2 py-1.5 border border-slate-200/80 flex items-center justify-between">
          <Button
            iconOnly
            text
            severity="secondary"
            aria-label="이전 날짜"
            class="!w-11 !h-11 !rounded-xl"
            @click="selectedDate = shiftDate(selectedDate, -1)"
          >
            <i class="pi pi-chevron-left text-sm" />
          </Button>

          <div class="flex items-center gap-2">
            <span class="text-sm font-bold text-slate-900">{{ formatKoreanDate(selectedDate) }}</span>
            <Button
              v-if="!isToday"
              severity="success"
              text
              size="small"
              class="!h-7 !px-2 !rounded-lg !text-[11px] !font-bold"
              @click="selectedDate = getTodayDateString()"
            >
              오늘
            </Button>
          </div>

          <Button
            iconOnly
            text
            severity="secondary"
            aria-label="다음 날짜"
            class="!w-11 !h-11 !rounded-xl"
            @click="selectedDate = shiftDate(selectedDate, 1)"
          >
            <i class="pi pi-chevron-right text-sm" />
          </Button>
        </div>

        <!-- 산책 · 식사 · 간식 3대 원터치 카드 -->
        <div class="grid grid-cols-3 gap-2.5">
          <button
            v-for="typeKey in ['WALK', 'MEAL', 'SNACK']"
            :key="typeKey"
            type="button"
            class="bg-white rounded-3xl p-3.5 border border-slate-200/80 flex flex-col items-center gap-1.5 active:scale-[0.97] transition shadow-2xs"
            @click="openCareSheet(typeKey)"
          >
            <span class="text-2xl">{{ CARE_META[typeKey].emoji }}</span>
            <span class="text-xs font-bold text-slate-900">{{ CARE_META[typeKey].label }}</span>
            <Tag
              :severity="(careSummary?.[`${typeKey.toLowerCase()}Count`] || 0) > 0 ? CARE_META[typeKey].severity : 'secondary'"
              rounded
              :value="
                (careSummary?.[`${typeKey.toLowerCase()}Count`] || 0) > 0
                  ? `${careSummary[`${typeKey.toLowerCase()}Count`]}회 완료`
                  : '기록하기'
              "
              class="!text-[11px]"
            />
          </button>
        </div>

        <!-- 오늘의 투약 체크 & 남은 약 -->
        <div class="bg-white rounded-3xl p-4 border border-slate-200/80 space-y-3">
          <div class="flex items-center justify-between">
            <h3 class="text-sm font-bold text-slate-900">💊 오늘의 투약 체크</h3>
            <RouterLink to="/pets" class="text-xs font-semibold text-emerald-700">
              약 관리 →
            </RouterLink>
          </div>

          <div v-if="loadingMedicines" class="py-4 flex flex-col items-center gap-1.5 text-xs text-slate-500">
            <ProgressSpinner style="width: 24px; height: 24px" strokeWidth="4" />
            <span>복용 약 정보를 불러오는 중...</span>
          </div>

          <div v-else-if="activeMedicines.length === 0" class="py-3 text-center space-y-1">
            <p class="text-xs text-slate-500">현재 복용 중인 약이 등록되어 있지 않아요.</p>
            <RouterLink to="/pets" class="inline-block text-xs font-bold text-emerald-700">
              반려동물·약 탭에서 약 등록하기 →
            </RouterLink>
          </div>

          <div v-else class="space-y-2.5">
            <div
              v-for="med in activeMedicines"
              :key="med.id"
              class="rounded-2xl border p-3 space-y-2"
              :class="
                med.depleted
                  ? 'border-rose-200 bg-rose-50/30'
                  : med.lowStock
                    ? 'border-amber-200 bg-amber-50/30'
                    : 'border-slate-200/80 bg-slate-50/40'
              "
            >
              <div class="flex items-start justify-between gap-2">
                <div class="min-w-0">
                  <div class="flex items-center gap-1.5 flex-wrap">
                    <p class="text-sm font-bold text-slate-900 truncate">{{ med.name }}</p>
                    <Tag
                      :severity="med.depleted ? 'danger' : med.lowStock ? 'warn' : 'success'"
                      rounded
                      :value="med.depleted ? '소진됨' : `${med.remainingDays}일치 남음`"
                      class="!text-[10px] !py-0.5 !px-2"
                    />
                  </div>
                  <p class="text-[11px] text-slate-500 mt-0.5 line-clamp-1">{{ med.dosageMethod }}</p>
                </div>

                <Button
                  size="small"
                  :disabled="recordingMedId === med.id || med.depleted"
                  :outlined="med.todayCompleted"
                  :severity="med.todayCompleted ? 'success' : undefined"
                  class="!h-10 !px-3 !rounded-xl !text-xs !font-bold shrink-0"
                  @click="handleMedicationClick(med)"
                >
                  <i v-if="recordingMedId === med.id" class="pi pi-spin pi-spinner text-xs" />
                  <span>
                    {{
                      recordingMedId === med.id
                        ? '처리중'
                        : med.todayCompleted
                          ? `완료 (${med.todayDoseCount}/${med.dailyDoseCount})`
                          : `투약 (${med.todayDoseCount}/${med.dailyDoseCount})`
                    }}
                  </span>
                </Button>
              </div>

              <div v-if="med.todayLogs?.length > 0" class="flex flex-wrap gap-1.5 pt-0.5">
                <span
                  v-for="log in med.todayLogs"
                  :key="log.id"
                  class="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg bg-white border border-slate-200 text-[11px] text-slate-700"
                >
                  <span>{{ log.memberNickname }} {{ formatTime(log.medicatedAt) }}</span>
                  <button
                    type="button"
                    aria-label="투약 기록 취소"
                    class="text-slate-400 hover:text-rose-600 font-bold px-0.5"
                    @click="handleCancelMedicationLog(log.id)"
                  >
                    ×
                  </button>
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 당일 케어 타임라인 -->
        <div class="bg-white rounded-3xl p-4 border border-slate-200/80 space-y-3">
          <div class="flex items-center justify-between">
            <h3 class="text-sm font-bold text-slate-900">가족 케어 타임라인</h3>
            <Tag
              severity="secondary"
              rounded
              :value="`총 ${careSummary?.logs?.length || 0}건`"
              class="!text-xs"
            />
          </div>

          <div v-if="loadingCare" class="py-6 flex flex-col items-center gap-2 text-xs text-slate-500">
            <ProgressSpinner style="width: 28px; height: 28px" strokeWidth="4" />
            <span>케어 기록을 불러오는 중...</span>
          </div>

          <div v-else-if="careError" class="space-y-2">
            <Message severity="error" :closable="false">{{ careError }}</Message>
            <Button
              severity="danger"
              outlined
              size="small"
              class="!h-9 !rounded-xl !text-xs !font-semibold"
              @click="fetchCareLogs"
            >
              다시 불러오기
            </Button>
          </div>

          <div
            v-else-if="!careSummary || careSummary.logs.length === 0"
            class="py-7 text-center space-y-1.5"
          >
            <p class="text-sm font-semibold text-slate-700">아직 등록된 케어 기록이 없어요</p>
            <p class="text-xs text-slate-500">위 산책 · 식사 · 간식 카드를 눌러 첫 기록을 남겨보세요!</p>
          </div>

          <ul v-else class="divide-y divide-slate-100">
            <li
              v-for="item in careSummary.logs"
              :key="item.id"
              class="py-3 first:pt-1 last:pb-1 flex items-start justify-between gap-3"
            >
              <div class="flex items-start gap-3 min-w-0">
                <div
                  class="w-10 h-10 rounded-2xl flex items-center justify-center text-lg shrink-0"
                  :class="CARE_META[item.careType]?.badge || 'bg-slate-100'"
                >
                  {{ CARE_META[item.careType]?.emoji || '🐾' }}
                </div>
                <div class="min-w-0">
                  <div class="flex items-center gap-1.5 flex-wrap">
                    <span class="text-sm font-bold text-slate-900">
                      {{ CARE_META[item.careType]?.label || item.careType }}
                    </span>
                    <span class="text-xs font-semibold text-emerald-700">
                      {{ item.memberNickname }}
                    </span>
                    <span class="text-[11px] text-slate-400">
                      {{ formatTime(item.performedAt) }}
                    </span>
                  </div>
                  <p v-if="item.memo" class="text-xs text-slate-600 mt-0.5 break-words">
                    {{ item.memo }}
                  </p>
                </div>
              </div>

              <Button
                text
                severity="danger"
                size="small"
                class="!h-9 !px-2.5 !rounded-xl !text-xs !font-semibold shrink-0"
                @click="deleteLogTarget = item"
              >
                취소
              </Button>
            </li>
          </ul>
        </div>
      </template>
    </div>

    <CareLogSheet
      :open="careSheetOpen"
      :pet-name="selectedPet?.name || ''"
      :initial-type="careSheetType"
      :submitting="submittingCare"
      @submit="handleCreateCareLog"
      @close="careSheetOpen = false"
    />

    <ConfirmSheet
      :open="Boolean(extraDoseTarget)"
      title="오늘 이미 권장 횟수를 모두 급여했어요"
      :description="`${extraDoseTarget?.name}은(는) 하루 ${extraDoseTarget?.dailyDoseCount}회 급여 약입니다. 중복 급여가 아닌지 확인 후 진행할까요?`"
      confirm-text="추가 투약 체크"
      @confirm="executeMedicationRecord(extraDoseTarget)"
      @cancel="extraDoseTarget = null"
    />

    <ConfirmSheet
      :open="Boolean(deleteLogTarget)"
      title="이 케어 기록을 취소할까요?"
      :description="`${CARE_META[deleteLogTarget?.careType]?.label || '케어'} 기록(${deleteLogTarget?.memberNickname})이 가족 모두의 화면에서 즉시 삭제됩니다.`"
      confirm-text="기록 취소"
      danger
      :loading="deletingLog"
      @confirm="handleConfirmDeleteLog"
      @cancel="deleteLogTarget = null"
    />
  </section>
</template>
