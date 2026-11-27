<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import { deletePet, listPets } from '@/api/pets'
import {
  cancelMedicationLog,
  createPetMedicine,
  deletePetMedicine,
  listPetMedicines,
  recordMedicationDose,
  updatePetMedicine,
} from '@/api/medicines'
import { useToast } from '@/stores/toast'
import {
  formatPetAge,
  getGenderLabel,
  getSpeciesEmoji,
  getSpeciesLabel,
} from '@/utils/petFormat'
import ConfirmSheet from '@/components/ConfirmSheet.vue'
import MedicineFormSheet from '@/components/MedicineFormSheet.vue'

const { showToast } = useToast()

const pets = ref([])
const selectedPetId = ref(null)
const loadingPets = ref(false)
const petsError = ref('')

const medicines = ref([])
const loadingMedicines = ref(false)
const medicinesError = ref('')

const deletePetTarget = ref(null)
const deletingPet = ref(false)

const medicineSheetOpen = ref(false)
const editingMedicine = ref(null)
const submittingMedicine = ref(false)

const deleteMedicineTarget = ref(null)
const deletingMedicine = ref(false)

const extraDoseTarget = ref(null)
const recordingDoseId = ref(null)

const selectedPet = computed(
  () => pets.value.find((p) => p.id === selectedPetId.value) || pets.value[0] || null,
)

function formatTime(isoString) {
  if (!isoString) return ''
  const dt = new Date(isoString)
  if (Number.isNaN(dt.getTime())) return ''
  return dt.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
}

function getStockTagSeverity(med) {
  if (med.depleted) return 'danger'
  if (med.lowStock) return 'warn'
  return 'success'
}

function getStockTagLabel(med) {
  if (med.depleted) return '소진됨'
  if (med.lowStock) return `소진 임박 · ${med.remainingDays}일치`
  return `${med.remainingDays}일치 남음`
}

async function fetchPets() {
  loadingPets.value = true
  petsError.value = ''
  try {
    const data = await listPets()
    pets.value = data
    if (data.length > 0 && !data.some((p) => p.id === selectedPetId.value)) {
      selectedPetId.value = data[0].id
    }
  } catch (err) {
    petsError.value = err.message || '반려동물 목록을 불러오지 못했습니다.'
  } finally {
    loadingPets.value = false
  }
}

async function fetchMedicines() {
  if (!selectedPet.value) {
    medicines.value = []
    return
  }
  loadingMedicines.value = true
  medicinesError.value = ''
  try {
    medicines.value = await listPetMedicines(selectedPet.value.id)
  } catch (err) {
    medicinesError.value = err.message || '복용 약 목록을 불러오지 못했습니다.'
  } finally {
    loadingMedicines.value = false
  }
}

function openCreateMedicine() {
  editingMedicine.value = null
  medicineSheetOpen.value = true
}

function openEditMedicine(med) {
  editingMedicine.value = med
  medicineSheetOpen.value = true
}

async function handleSaveMedicine(payload) {
  if (!selectedPet.value) return
  submittingMedicine.value = true
  try {
    if (editingMedicine.value) {
      await updatePetMedicine(selectedPet.value.id, editingMedicine.value.id, payload)
      showToast(`${payload.name} 정보를 수정했어요.`, 'success')
    } else {
      await createPetMedicine(selectedPet.value.id, payload)
      showToast(`${payload.name}을(를) 등록했어요!`, 'success')
    }
    medicineSheetOpen.value = false
    await fetchMedicines()
  } catch (err) {
    showToast(err.message || '약 저장에 실패했습니다.', 'error')
  } finally {
    submittingMedicine.value = false
  }
}

async function handleDoseClick(med) {
  if (med.depleted) {
    showToast('남은 횟수가 0회입니다. 약 수정에서 잔여량을 갱신해주세요.', 'error')
    return
  }
  if (med.todayCompleted) {
    extraDoseTarget.value = med
    return
  }
  await executeDoseRecord(med)
}

async function executeDoseRecord(med) {
  if (!selectedPet.value) return
  recordingDoseId.value = med.id
  try {
    await recordMedicationDose(selectedPet.value.id, med.id)
    extraDoseTarget.value = null
    showToast(`${med.name} 투약 완료! 잔여량이 자동 차감되었어요.`, 'success')
    await fetchMedicines()
  } catch (err) {
    showToast(err.message || '투약 체크에 실패했습니다.', 'error')
  } finally {
    recordingDoseId.value = null
  }
}

async function handleCancelDoseLog(logId) {
  try {
    await cancelMedicationLog(logId)
    showToast('투약 기록을 취소하고 잔여량을 복구했어요.', 'info')
    await fetchMedicines()
  } catch (err) {
    showToast(err.message || '투약 취소에 실패했습니다.', 'error')
  }
}

async function handleConfirmDeleteMedicine() {
  if (!selectedPet.value || !deleteMedicineTarget.value) return
  deletingMedicine.value = true
  try {
    const name = deleteMedicineTarget.value.name
    await deletePetMedicine(selectedPet.value.id, deleteMedicineTarget.value.id)
    deleteMedicineTarget.value = null
    showToast(`${name}을(를) 삭제했어요.`, 'info')
    await fetchMedicines()
  } catch (err) {
    showToast(err.message || '약 삭제에 실패했습니다.', 'error')
  } finally {
    deletingMedicine.value = false
  }
}

async function handleConfirmDeletePet() {
  if (!deletePetTarget.value) return
  deletingPet.value = true
  try {
    const targetName = deletePetTarget.value.name
    await deletePet(deletePetTarget.value.id)
    pets.value = pets.value.filter((pet) => pet.id !== deletePetTarget.value.id)
    deletePetTarget.value = null
    selectedPetId.value = pets.value[0]?.id ?? null
    showToast(`${targetName} 프로필을 삭제했어요.`, 'info')
  } catch (err) {
    showToast(err.message || '삭제에 실패했습니다.', 'error')
  } finally {
    deletingPet.value = false
  }
}

onMounted(fetchPets)
watch(selectedPet, fetchMedicines)
</script>

<template>
  <section class="space-y-4">
    <div class="flex items-center justify-between gap-2">
      <div>
        <h2 class="text-base font-bold text-slate-900">반려동물 & 복용 약</h2>
        <p class="text-xs text-slate-500 mt-0.5">프로필과 처방약·잔여 일수를 가족이 함께 관리해요.</p>
      </div>
      <RouterLink to="/pets/new" custom v-slot="{ href, navigate }">
        <Button
          as="a"
          :href="href"
          size="small"
          class="!h-11 !px-3.5 !rounded-2xl !text-xs !font-bold shrink-0 no-underline"
          @click="navigate"
        >
          <i class="pi pi-plus text-xs" />
          <span>반려동물</span>
        </Button>
      </RouterLink>
    </div>

    <div
      v-if="loadingPets"
      class="bg-white rounded-2xl p-6 border border-slate-200/80 flex flex-col items-center gap-2 text-sm text-slate-500"
    >
      <ProgressSpinner style="width: 32px; height: 32px" strokeWidth="4" />
      <span>목록을 불러오는 중...</span>
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
      class="bg-white rounded-3xl p-7 border border-dashed border-emerald-300 text-center space-y-3"
    >
      <div class="text-4xl">🐾</div>
      <div class="space-y-1">
        <p class="text-base font-bold text-slate-900">등록된 반려동물이 없어요</p>
        <p class="text-xs text-slate-600">먼저 반려동물을 등록하면 복용 약도 함께 관리할 수 있어요.</p>
      </div>
      <RouterLink to="/pets/new" custom v-slot="{ href, navigate }">
        <Button
          as="a"
          :href="href"
          class="!h-12 !px-5 !rounded-2xl !text-sm !font-bold no-underline"
          @click="navigate"
        >
          반려동물 등록하기
        </Button>
      </RouterLink>
    </div>

    <template v-else>
      <!-- 반려동물 선택 칩 -->
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

      <!-- 선택 반려동물 프로필 카드 -->
      <article v-if="selectedPet" class="bg-white rounded-3xl p-4 border border-slate-200/80 space-y-3">
        <div class="flex items-start justify-between gap-3">
          <div class="flex items-center gap-3 min-w-0">
            <div class="w-12 h-12 rounded-2xl bg-emerald-50 text-2xl flex items-center justify-center shrink-0">
              {{ getSpeciesEmoji(selectedPet.species) }}
            </div>
            <div class="min-w-0">
              <div class="flex items-center gap-1.5 flex-wrap">
                <h3 class="text-base font-bold text-slate-900 truncate">{{ selectedPet.name }}</h3>
                <Tag
                  :value="getSpeciesLabel(selectedPet.species)"
                  severity="success"
                  rounded
                  class="!text-[11px] !py-0.5 !px-2"
                />
                <Tag
                  v-if="selectedPet.neutered"
                  value="중성화"
                  severity="secondary"
                  rounded
                  class="!text-[11px] !py-0.5 !px-2"
                />
              </div>
              <p class="text-xs text-slate-500 mt-0.5">
                {{ selectedPet.breed || '품종 미등록' }} · {{ formatPetAge(selectedPet.birthDate) }} ·
                {{ getGenderLabel(selectedPet.gender) }}
                <span v-if="selectedPet.weightKg != null"> · {{ selectedPet.weightKg }}kg</span>
              </p>
            </div>
          </div>
        </div>

        <div
          v-if="selectedPet.notes"
          class="rounded-2xl bg-amber-50/70 border border-amber-200/60 px-3 py-2 text-xs text-amber-900"
        >
          <span class="font-bold">특이사항:</span> {{ selectedPet.notes }}
        </div>

        <div class="grid grid-cols-2 gap-2">
          <RouterLink :to="`/pets/${selectedPet.id}/edit`" custom v-slot="{ href, navigate }">
            <Button
              as="a"
              :href="href"
              severity="secondary"
              class="!h-11 !rounded-2xl !text-xs !font-bold no-underline justify-center"
              @click="navigate"
            >
              프로필 수정
            </Button>
          </RouterLink>
          <Button
            severity="danger"
            outlined
            class="!h-11 !rounded-2xl !text-xs !font-bold"
            @click="deletePetTarget = selectedPet"
          >
            프로필 삭제
          </Button>
        </div>
      </article>

      <!-- 선택 반려동물 복용 약 섹션 -->
      <div v-if="selectedPet" class="space-y-3 pt-1">
        <div class="flex items-center justify-between">
          <div>
            <h3 class="text-sm font-bold text-slate-900">{{ selectedPet.name }} 복용 약 · 잔여 일수</h3>
            <p class="text-[11px] text-slate-500">동물 의약품 검색 또는 직접 입력으로 등록하세요.</p>
          </div>
          <Button
            severity="success"
            outlined
            size="small"
            class="!h-10 !px-3.5 !rounded-xl !text-xs !font-bold shrink-0"
            @click="openCreateMedicine"
          >
            <i class="pi pi-plus text-xs" />
            <span>약 등록</span>
          </Button>
        </div>

        <div
          v-if="loadingMedicines"
          class="bg-white rounded-2xl p-5 border border-slate-200/80 flex flex-col items-center gap-2 text-xs text-slate-500"
        >
          <ProgressSpinner style="width: 28px; height: 28px" strokeWidth="4" />
          <span>복용 약 목록을 불러오는 중...</span>
        </div>

        <div v-else-if="medicinesError" class="space-y-2">
          <Message severity="error" :closable="false">{{ medicinesError }}</Message>
          <Button
            severity="danger"
            outlined
            size="small"
            class="!h-9 !rounded-xl !text-xs !font-semibold"
            @click="fetchMedicines"
          >
            다시 시도
          </Button>
        </div>

        <div
          v-else-if="medicines.length === 0"
          class="bg-white rounded-3xl p-6 border border-dashed border-slate-300 text-center space-y-2.5"
        >
          <div class="text-3xl">💊</div>
          <p class="text-sm font-bold text-slate-800">등록된 약이 없어요</p>
          <p class="text-xs text-slate-500">
            심장사상충약, 처방약, 영양제와 급여 방법을 등록해 가족과 공유해보세요.
          </p>
          <Button
            size="small"
            class="!h-11 !px-4 !rounded-2xl !text-xs !font-bold"
            @click="openCreateMedicine"
          >
            첫 약 등록하기
          </Button>
        </div>

        <div v-else class="space-y-3">
          <article
            v-for="med in medicines"
            :key="med.id"
            class="bg-white rounded-3xl p-4 border space-y-3"
            :class="
              med.depleted
                ? 'border-rose-200'
                : med.lowStock
                  ? 'border-amber-300'
                  : 'border-slate-200/80'
            "
          >
            <div class="flex items-start justify-between gap-2">
              <div class="min-w-0">
                <div class="flex items-center gap-1.5 flex-wrap">
                  <h4 class="text-sm font-bold text-slate-900">{{ med.name }}</h4>
                  <Tag
                    :value="med.sourceType === 'API' ? '의약품 DB' : '직접 등록'"
                    :severity="med.sourceType === 'API' ? 'info' : 'secondary'"
                    rounded
                    class="!text-[10px] !py-0.5 !px-2"
                  />
                  <Tag
                    v-if="!med.active"
                    value="복용 종료"
                    severity="secondary"
                    rounded
                    class="!text-[10px] !py-0.5 !px-2"
                  />
                </div>
                <p class="text-xs text-slate-600 mt-1 leading-relaxed">{{ med.dosageMethod }}</p>
              </div>

              <Tag
                :value="getStockTagLabel(med)"
                :severity="getStockTagSeverity(med)"
                rounded
                class="!text-[11px] !font-bold shrink-0"
              />
            </div>

            <div class="grid grid-cols-2 gap-2 text-xs">
              <div class="rounded-2xl bg-slate-50 px-3 py-2">
                <span class="text-slate-500">남은 횟수</span>
                <p class="font-bold text-slate-900 mt-0.5">
                  {{ med.remainingDoses }}회 / 총 {{ med.totalDoses }}회
                </p>
              </div>
              <div class="rounded-2xl bg-slate-50 px-3 py-2">
                <span class="text-slate-500">오늘 투약</span>
                <p
                  class="font-bold mt-0.5"
                  :class="med.todayCompleted ? 'text-emerald-700' : 'text-slate-900'"
                >
                  {{ med.todayDoseCount }} / {{ med.dailyDoseCount }}회
                  <span v-if="med.todayCompleted"> (완료)</span>
                </p>
              </div>
            </div>

            <!-- 오늘 투약 이력 -->
            <div v-if="med.todayLogs?.length > 0" class="rounded-2xl bg-emerald-50/50 px-3 py-2 space-y-1.5">
              <div
                v-for="log in med.todayLogs"
                :key="log.id"
                class="flex items-center justify-between text-xs"
              >
                <span class="text-emerald-900">
                  ✓ <strong>{{ log.memberNickname }}</strong> · {{ formatTime(log.medicatedAt) }}
                </span>
                <Button
                  link
                  size="small"
                  class="!p-0 !text-[11px] !font-semibold !text-slate-500"
                  @click="handleCancelDoseLog(log.id)"
                >
                  취소
                </Button>
              </div>
            </div>

            <!-- 액션 버튼 -->
            <div class="flex gap-2">
              <Button
                :disabled="recordingDoseId === med.id || med.depleted || !med.active"
                :outlined="med.todayCompleted"
                :severity="med.todayCompleted ? 'success' : undefined"
                class="flex-1 !h-11 !rounded-2xl !text-xs !font-bold"
                @click="handleDoseClick(med)"
              >
                <i v-if="recordingDoseId === med.id" class="pi pi-spin pi-spinner text-xs" />
                <span>
                  {{
                    recordingDoseId === med.id
                      ? '처리 중...'
                      : med.depleted
                        ? '남은 약 없음'
                        : med.todayCompleted
                          ? '오늘 급여 완료 (추가 체크)'
                          : '오늘 투약 완료 체크'
                  }}
                </span>
              </Button>
              <Button
                severity="secondary"
                class="!h-11 !px-3.5 !rounded-2xl !text-xs !font-bold"
                @click="openEditMedicine(med)"
              >
                수정
              </Button>
              <Button
                severity="danger"
                outlined
                class="!h-11 !px-3 !rounded-2xl !text-xs !font-bold"
                @click="deleteMedicineTarget = med"
              >
                삭제
              </Button>
            </div>
          </article>
        </div>
      </div>
    </template>

    <MedicineFormSheet
      :open="medicineSheetOpen"
      :pet-name="selectedPet?.name || ''"
      :editing-medicine="editingMedicine"
      :submitting="submittingMedicine"
      @submit="handleSaveMedicine"
      @close="medicineSheetOpen = false"
    />

    <ConfirmSheet
      :open="Boolean(extraDoseTarget)"
      title="오늘 이미 권장 횟수를 모두 급여했어요"
      :description="`${extraDoseTarget?.name}은(는) 하루 ${extraDoseTarget?.dailyDoseCount}회 급여 약입니다. 중복 급여가 아닌지 확인 후 진행할까요?`"
      confirm-text="추가 투약 체크"
      @confirm="executeDoseRecord(extraDoseTarget)"
      @cancel="extraDoseTarget = null"
    />

    <ConfirmSheet
      :open="Boolean(deleteMedicineTarget)"
      :title="`${deleteMedicineTarget?.name || '약'}을(를) 삭제할까요?`"
      description="삭제된 약은 목록에서 숨겨집니다."
      confirm-text="삭제하기"
      danger
      :loading="deletingMedicine"
      @confirm="handleConfirmDeleteMedicine"
      @cancel="deleteMedicineTarget = null"
    />

    <ConfirmSheet
      :open="Boolean(deletePetTarget)"
      :title="`${deletePetTarget?.name || '반려동물'} 프로필을 삭제할까요?`"
      description="삭제된 반려동물은 가족 목록에서 숨겨지며, 복구가 필요하면 관리자에게 문의해야 합니다."
      confirm-text="삭제하기"
      danger
      :loading="deletingPet"
      @confirm="handleConfirmDeletePet"
      @cancel="deletePetTarget = null"
    />
  </section>
</template>
