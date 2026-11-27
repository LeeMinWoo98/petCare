<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import InputText from 'primevue/inputtext'
import SelectButton from 'primevue/selectbutton'
import DatePicker from 'primevue/datepicker'
import InputNumber from 'primevue/inputnumber'
import ToggleSwitch from 'primevue/toggleswitch'
import Textarea from 'primevue/textarea'
import Message from 'primevue/message'
import Button from 'primevue/button'
import ProgressSpinner from 'primevue/progressspinner'
import { createPet, getPet, updatePet } from '@/api/pets'
import { useToast } from '@/stores/toast'
import {
  formatIsoDate,
  GENDER_OPTIONS,
  parseIsoDate,
  SPECIES_OPTIONS,
} from '@/utils/petFormat'

const route = useRoute()
const router = useRouter()
const { showToast } = useToast()

const petId = computed(() => (route.params.petId ? Number(route.params.petId) : null))
const isEditMode = computed(() => Boolean(petId.value))

const form = reactive({
  name: '',
  species: 'DOG',
  breed: '',
  birthDate: '',
  gender: 'UNKNOWN',
  neutered: false,
  weightKg: null,
  notes: '',
})

const birthDateModel = computed({
  get: () => parseIsoDate(form.birthDate),
  set: (val) => {
    form.birthDate = formatIsoDate(val) || ''
  },
})

const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const fieldErrors = ref({})

async function loadPet() {
  if (!isEditMode.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await getPet(petId.value)
    form.name = data.name || ''
    form.species = data.species || 'DOG'
    form.breed = data.breed || ''
    form.birthDate = data.birthDate || ''
    form.gender = data.gender || 'UNKNOWN'
    form.neutered = Boolean(data.neutered)
    form.weightKg = data.weightKg != null ? Number(data.weightKg) : null
    form.notes = data.notes || ''
  } catch (err) {
    errorMessage.value = err.message || '반려동물 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  errorMessage.value = ''
  fieldErrors.value = {}
  submitting.value = true

  const parsedWeight = form.weightKg == null || form.weightKg === '' ? null : Number(form.weightKg)
  const payload = {
    name: form.name.trim(),
    species: form.species,
    breed: form.breed.trim() || null,
    birthDate: form.birthDate || null,
    gender: form.gender,
    neutered: form.neutered,
    weightKg: Number.isFinite(parsedWeight) ? parsedWeight : null,
    notes: form.notes.trim() || null,
  }

  try {
    if (isEditMode.value) {
      await updatePet(petId.value, payload)
      showToast(`${payload.name} 프로필을 수정했어요.`, 'success')
    } else {
      await createPet(payload)
      showToast(`${payload.name} 프로필을 등록했어요!`, 'success')
    }
    router.replace('/pets')
  } catch (err) {
    errorMessage.value = err.message || '저장에 실패했습니다.'
    fieldErrors.value = err.fieldErrors || {}
  } finally {
    submitting.value = false
  }
}

onMounted(loadPet)
</script>

<template>
  <section class="space-y-4">
    <div
      v-if="loading"
      class="bg-white rounded-2xl p-6 border border-slate-200/80 flex flex-col items-center gap-2 text-sm text-slate-500"
    >
      <ProgressSpinner style="width: 32px; height: 32px" strokeWidth="4" />
      <span>반려동물 정보를 불러오는 중...</span>
    </div>

    <form
      v-else
      class="bg-white rounded-3xl p-5 border border-slate-200/80 space-y-5"
      @submit.prevent="handleSubmit"
    >
      <!-- 이름 -->
      <div class="space-y-1.5">
        <label for="pet-name" class="block text-xs font-bold text-slate-700">
          이름 <span class="text-rose-500">*</span>
        </label>
        <InputText
          id="pet-name"
          v-model="form.name"
          fluid
          required
          maxlength="50"
          placeholder="예: 콩이"
          :invalid="Boolean(fieldErrors.name)"
          class="!h-12 !rounded-2xl !text-sm"
        />
        <p v-if="fieldErrors.name" class="text-xs text-rose-600">{{ fieldErrors.name }}</p>
      </div>

      <!-- 종 선택 -->
      <div class="space-y-1.5">
        <span class="block text-xs font-bold text-slate-700">
          종 <span class="text-rose-500">*</span>
        </span>
        <SelectButton
          v-model="form.species"
          :options="SPECIES_OPTIONS"
          optionLabel="label"
          optionValue="value"
          :allowEmpty="false"
          fluid
          :pt="{
            root: { class: '!grid !grid-cols-3 !gap-2' },
            pcToggleButton: { root: { class: '!h-14 !rounded-2xl' } },
          }"
        >
          <template #option="{ option }">
            <div class="flex flex-col items-center justify-center gap-0.5 py-0.5">
              <span class="text-lg leading-none">{{ option.emoji }}</span>
              <span class="text-xs font-bold">{{ option.label }}</span>
            </div>
          </template>
        </SelectButton>
      </div>

      <!-- 품종 -->
      <div class="space-y-1.5">
        <label for="pet-breed" class="block text-xs font-bold text-slate-700">품종</label>
        <InputText
          id="pet-breed"
          v-model="form.breed"
          fluid
          maxlength="50"
          placeholder="예: 말티즈, 코리안숏헤어"
          class="!h-12 !rounded-2xl !text-sm"
        />
      </div>

      <!-- 생년월일 & 몸무게 -->
      <div class="grid grid-cols-2 gap-3">
        <div class="space-y-1.5">
          <label for="pet-birth" class="block text-xs font-bold text-slate-700">생년월일</label>
          <DatePicker
            inputId="pet-birth"
            v-model="birthDateModel"
            fluid
            dateFormat="yy-mm-dd"
            showIcon
            iconDisplay="input"
            showClear
            placeholder="YYYY-MM-DD"
            inputClass="!h-12 !rounded-2xl !text-sm"
          />
        </div>

        <div class="space-y-1.5">
          <label for="pet-weight" class="block text-xs font-bold text-slate-700">몸무게 (kg)</label>
          <InputNumber
            inputId="pet-weight"
            v-model="form.weightKg"
            fluid
            :min="0.01"
            :max="999.99"
            :minFractionDigits="0"
            :maxFractionDigits="2"
            suffix=" kg"
            placeholder="예: 3.45"
            :invalid="Boolean(fieldErrors.weightKg)"
            inputClass="!h-12 !rounded-2xl !text-sm"
          />
          <p v-if="fieldErrors.weightKg" class="text-xs text-rose-600">{{ fieldErrors.weightKg }}</p>
        </div>
      </div>

      <!-- 성별 -->
      <div class="space-y-1.5">
        <span class="block text-xs font-bold text-slate-700">성별</span>
        <SelectButton
          v-model="form.gender"
          :options="GENDER_OPTIONS"
          optionLabel="label"
          optionValue="value"
          :allowEmpty="false"
          fluid
          :pt="{
            root: { class: '!grid !grid-cols-3 !gap-2' },
            pcToggleButton: { root: { class: '!h-11 !rounded-2xl !text-xs !font-bold' } },
          }"
        />
      </div>

      <!-- 중성화 여부 -->
      <div class="flex items-center justify-between rounded-2xl bg-slate-50 px-4 py-3">
        <div>
          <p class="text-xs font-bold text-slate-800">중성화 수술 여부</p>
          <p class="text-[11px] text-slate-500 mt-0.5">중성화 완료 시 켜주세요</p>
        </div>
        <ToggleSwitch v-model="form.neutered" aria-label="중성화 수술 여부" />
      </div>

      <!-- 특이사항 -->
      <div class="space-y-1.5">
        <label for="pet-notes" class="block text-xs font-bold text-slate-700">
          특이사항 · 알러지 메모
        </label>
        <Textarea
          id="pet-notes"
          v-model="form.notes"
          fluid
          autoResize
          rows="3"
          maxlength="2000"
          placeholder="예: 닭고기 알러지 있음, 슬개골 주의, 낯선 강아지 무서워함"
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
        <span>{{ submitting ? '저장 중...' : isEditMode ? '수정 내용 저장' : '반려동물 등록하기' }}</span>
      </Button>
    </form>
  </section>
</template>
