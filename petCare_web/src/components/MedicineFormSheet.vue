<script setup>
import { computed, reactive, ref, watch } from 'vue'
import Drawer from 'primevue/drawer'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import InputNumber from 'primevue/inputnumber'
import SelectButton from 'primevue/selectbutton'
import DatePicker from 'primevue/datepicker'
import ToggleSwitch from 'primevue/toggleswitch'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { searchMedicineCatalog } from '@/api/medicines'
import { formatIsoDate, parseIsoDate } from '@/utils/petFormat'

const props = defineProps({
  open: { type: Boolean, default: false },
  petName: { type: String, default: '' },
  editingMedicine: { type: Object, default: null },
  submitting: { type: Boolean, default: false },
})

const emit = defineEmits(['submit', 'close'])

const DAILY_DOSE_OPTIONS = [
  { label: '하루 1회', value: 1 },
  { label: '하루 2회', value: 2 },
  { label: '하루 3회', value: 3 },
]

function getTodayString() {
  return formatIsoDate(new Date())
}

const isEditMode = computed(() => Boolean(props.editingMedicine))

const searchKeyword = ref('')
const catalogResults = ref([])
const searching = ref(false)
const hasSearched = ref(false)
const selectedCatalogName = ref('')

const form = reactive({
  sourceType: 'CUSTOM',
  catalogId: null,
  name: '',
  dosageMethod: '',
  dailyDoseCount: 1,
  totalDoses: 10,
  remainingDoses: 10,
  startDate: getTodayString(),
  active: true,
})

const startDateModel = computed({
  get: () => parseIsoDate(form.startDate),
  set: (val) => {
    form.startDate = formatIsoDate(val) || getTodayString()
  },
})

const estimatedDays = computed(() => {
  const rem = isEditMode.value ? Number(form.remainingDoses) : Number(form.totalDoses)
  const daily = Math.max(1, Number(form.dailyDoseCount) || 1)
  if (!Number.isFinite(rem) || rem <= 0) return 0
  return Math.ceil(rem / daily)
})

watch(
  () => [props.open, props.editingMedicine],
  async ([isOpen, med]) => {
    if (!isOpen) return
    searchKeyword.value = ''
    hasSearched.value = false
    selectedCatalogName.value = ''

    if (med) {
      form.sourceType = med.sourceType || 'CUSTOM'
      form.catalogId = med.catalogId ?? null
      form.name = med.name || ''
      form.dosageMethod = med.dosageMethod || ''
      form.dailyDoseCount = med.dailyDoseCount || 1
      form.totalDoses = med.totalDoses || 1
      form.remainingDoses = med.remainingDoses ?? 0
      form.startDate = med.startDate || getTodayString()
      form.active = med.active !== false
    } else {
      form.sourceType = 'CUSTOM'
      form.catalogId = null
      form.name = ''
      form.dosageMethod = ''
      form.dailyDoseCount = 1
      form.totalDoses = 10
      form.remainingDoses = 10
      form.startDate = getTodayString()
      form.active = true
      await runCatalogSearch('')
    }
  },
)

async function runCatalogSearch(keyword = searchKeyword.value) {
  searching.value = true
  try {
    catalogResults.value = await searchMedicineCatalog(keyword)
    hasSearched.value = true
  } catch {
    catalogResults.value = []
  } finally {
    searching.value = false
  }
}

function selectCatalogItem(item) {
  form.sourceType = 'API'
  form.catalogId = item.id
  form.name = item.name
  selectedCatalogName.value = item.name
  if (item.dosageGuide) {
    form.dosageMethod = item.dosageGuide
  }
}

function switchToCustom() {
  form.sourceType = 'CUSTOM'
  form.catalogId = null
  selectedCatalogName.value = ''
}

function handleSubmit() {
  const daily = Math.max(1, Number(form.dailyDoseCount) || 1)
  const total = Math.max(1, Number(form.totalDoses) || 1)
  const remaining = isEditMode.value
    ? Math.max(0, Number(form.remainingDoses) || 0)
    : total

  if (isEditMode.value) {
    emit('submit', {
      name: form.name.trim(),
      dosageMethod: form.dosageMethod.trim(),
      dailyDoseCount: daily,
      totalDoses: total,
      remainingDoses: remaining,
      startDate: form.startDate || getTodayString(),
      active: form.active,
    })
  } else {
    emit('submit', {
      sourceType: form.sourceType,
      catalogId: form.sourceType === 'API' ? form.catalogId : null,
      name: form.name.trim(),
      dosageMethod: form.dosageMethod.trim(),
      dailyDoseCount: daily,
      totalDoses: total,
      remainingDoses: remaining,
      startDate: form.startDate || getTodayString(),
    })
  }
}

function handleVisibleChange(visible) {
  if (!visible && !props.submitting) {
    emit('close')
  }
}
</script>

<template>
  <Drawer
    :visible="open"
    position="bottom"
    :showCloseIcon="false"
    :dismissable="!submitting"
    :closeOnEscape="!submitting"
    :pt="{
      root: { class: '!w-full !max-w-[430px] !mx-auto !rounded-t-3xl !h-auto !max-h-[88dvh] !border-0' },
      header: { class: '!hidden' },
      content: { class: '!px-5 !pt-3 !pb-6 safe-pb overflow-y-auto' },
    }"
    @update:visible="handleVisibleChange"
  >
    <div class="space-y-4">
      <div class="w-10 h-1 rounded-full bg-slate-300 mx-auto" />

      <div class="flex items-center justify-between">
        <div>
          <p class="text-xs text-emerald-700 font-semibold">{{ petName }} 약 관리</p>
          <h2 class="text-lg font-bold text-slate-900">
            {{ isEditMode ? '복용 약 정보 수정' : '새 복용 약 등록' }}
          </h2>
        </div>
        <Button
          text
          severity="secondary"
          size="small"
          :disabled="submitting"
          class="!h-10 !px-3 !rounded-xl !text-xs !font-semibold"
          @click="emit('close')"
        >
          닫기
        </Button>
      </div>

      <!-- 의약품 카탈로그 검색 (신규 등록 시) -->
      <div v-if="!isEditMode" class="rounded-2xl bg-slate-50 border border-slate-200/80 p-3.5 space-y-2.5">
        <div class="flex items-center justify-between">
          <p class="text-xs font-bold text-slate-800">1. 동물 의약품 검색 (선택)</p>
          <Button
            v-if="form.sourceType === 'API'"
            link
            size="small"
            class="!p-0 !text-[11px] !font-semibold"
            @click="switchToCustom"
          >
            직접 입력으로 전환
          </Button>
        </div>

        <div class="flex gap-1.5">
          <IconField class="flex-1">
            <InputIcon class="pi pi-search text-xs" />
            <InputText
              v-model="searchKeyword"
              type="search"
              fluid
              placeholder="약 이름·제조사 검색 (예: 하트가드, 아포퀠)"
              class="!h-11 !rounded-xl !text-xs"
              @keyup.enter="runCatalogSearch()"
            />
          </IconField>
          <Button
            size="small"
            class="!h-11 !px-3.5 !rounded-xl !text-xs !font-bold shrink-0"
            @click="runCatalogSearch()"
          >
            검색
          </Button>
        </div>

        <div v-if="searching" class="text-center py-2 text-xs text-slate-500">검색 중...</div>

        <div v-else-if="catalogResults.length > 0" class="max-h-36 overflow-y-auto space-y-1.5 pr-0.5">
          <button
            v-for="item in catalogResults"
            :key="item.id"
            type="button"
            class="w-full text-left p-2.5 rounded-xl border transition active:scale-[0.99]"
            :class="
              form.catalogId === item.id
                ? 'bg-emerald-50 border-emerald-500'
                : 'bg-white border-slate-200'
            "
            @click="selectCatalogItem(item)"
          >
            <div class="flex items-center justify-between gap-2">
              <p class="text-xs font-bold text-slate-900 truncate">{{ item.name }}</p>
              <span class="text-[10px] text-slate-500 shrink-0">{{ item.manufacturer }}</span>
            </div>
            <p v-if="item.efficacy" class="text-[11px] text-slate-600 line-clamp-1 mt-0.5">
              {{ item.efficacy }}
            </p>
          </button>
        </div>

        <p v-else-if="hasSearched" class="text-[11px] text-slate-500 text-center py-1">
          검색 결과가 없어요. 아래 입력란에 약 정보를 직접 입력해 등록할 수 있습니다.
        </p>

        <div v-if="selectedCatalogName" class="pt-0.5">
          <Tag severity="success" rounded :value="`선택됨: ${selectedCatalogName}`" class="!text-[11px]" />
        </div>
      </div>

      <form class="space-y-4" @submit.prevent="handleSubmit">
        <div class="space-y-1.5">
          <label for="med-name" class="block text-xs font-bold text-slate-700">
            약 이름 <span class="text-rose-500">*</span>
          </label>
          <InputText
            id="med-name"
            v-model="form.name"
            fluid
            required
            maxlength="150"
            placeholder="예: 아포퀠정, 처방 항생제"
            class="!h-12 !rounded-2xl !text-sm"
          />
        </div>

        <div class="space-y-1.5">
          <label for="med-dosage" class="block text-xs font-bold text-slate-700">
            급여 방법 <span class="text-rose-500">*</span>
          </label>
          <Textarea
            id="med-dosage"
            v-model="form.dosageMethod"
            fluid
            autoResize
            rows="2"
            required
            maxlength="2000"
            placeholder="예: 아침·저녁 식후 반 알씩 간식에 싸서 급여"
            class="!rounded-2xl !text-sm"
          />
        </div>

        <!-- 1일 급여 횟수 -->
        <div class="space-y-1.5">
          <span class="block text-xs font-bold text-slate-700">1일 급여 횟수</span>
          <div class="grid grid-cols-4 gap-2 items-center">
            <SelectButton
              v-model="form.dailyDoseCount"
              :options="DAILY_DOSE_OPTIONS"
              optionLabel="label"
              optionValue="value"
              :allowEmpty="false"
              fluid
              class="col-span-3"
              :pt="{
                root: { class: '!grid !grid-cols-3 !gap-1.5' },
                pcToggleButton: { root: { class: '!h-11 !rounded-xl !text-xs !font-bold' } },
              }"
            />
            <InputNumber
              v-model="form.dailyDoseCount"
              fluid
              :min="1"
              :max="10"
              aria-label="1일 급여 횟수 직접 입력"
              inputClass="!h-11 !rounded-xl !text-center !text-xs !font-bold"
            />
          </div>
        </div>

        <!-- 총 처방 횟수 & 남은 횟수 / 시작일 -->
        <div class="grid grid-cols-2 gap-3">
          <div class="space-y-1.5">
            <label for="med-total" class="block text-xs font-bold text-slate-700">
              총 처방 횟수 <span class="text-rose-500">*</span>
            </label>
            <InputNumber
              inputId="med-total"
              v-model="form.totalDoses"
              fluid
              :min="1"
              required
              inputClass="!h-12 !rounded-2xl !text-sm"
            />
          </div>

          <div v-if="isEditMode" class="space-y-1.5">
            <label for="med-remaining" class="block text-xs font-bold text-slate-700">
              현재 남은 횟수 <span class="text-rose-500">*</span>
            </label>
            <InputNumber
              inputId="med-remaining"
              v-model="form.remainingDoses"
              fluid
              :min="0"
              required
              inputClass="!h-12 !rounded-2xl !text-sm"
            />
          </div>

          <div v-else class="space-y-1.5">
            <label for="med-start" class="block text-xs font-bold text-slate-700">복용 시작일</label>
            <DatePicker
              inputId="med-start"
              v-model="startDateModel"
              fluid
              dateFormat="yy-mm-dd"
              showIcon
              iconDisplay="input"
              inputClass="!h-12 !rounded-2xl !text-sm"
            />
          </div>
        </div>

        <!-- 예상 잔여 일수 미리보기 -->
        <div class="rounded-2xl bg-emerald-50/70 border border-emerald-200/70 px-4 py-2.5 flex items-center justify-between text-xs">
          <span class="text-emerald-900 font-medium">예상 남은 복용 기간</span>
          <Tag
            :severity="estimatedDays === 0 ? 'danger' : estimatedDays <= 3 ? 'warn' : 'success'"
            rounded
            :value="estimatedDays > 0 ? `약 ${estimatedDays}일치` : '소진됨 (0일)'"
          />
        </div>

        <div v-if="isEditMode" class="flex items-center justify-between rounded-2xl bg-slate-50 px-4 py-3">
          <div>
            <p class="text-xs font-bold text-slate-800">현재 복용 중</p>
            <p class="text-[11px] text-slate-500 mt-0.5">복용 완료된 약은 끌 수 있어요</p>
          </div>
          <ToggleSwitch v-model="form.active" aria-label="현재 복용 중 여부" />
        </div>

        <Button
          type="submit"
          fluid
          :disabled="submitting"
          class="!h-12 !rounded-2xl !font-bold !text-sm"
        >
          <i v-if="submitting" class="pi pi-spin pi-spinner text-sm" />
          <span>{{ submitting ? '저장 중...' : isEditMode ? '약 정보 수정 저장' : '복용 약 등록하기' }}</span>
        </Button>
      </form>
    </div>
  </Drawer>
</template>
