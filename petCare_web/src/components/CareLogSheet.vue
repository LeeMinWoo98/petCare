<script setup>
import { computed, ref, watch } from 'vue'
import Drawer from 'primevue/drawer'
import SelectButton from 'primevue/selectbutton'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'

const props = defineProps({
  open: { type: Boolean, default: false },
  petName: { type: String, default: '' },
  initialType: { type: String, default: 'WALK' },
  submitting: { type: Boolean, default: false },
})

const emit = defineEmits(['submit', 'close'])

const careType = ref(props.initialType)
const memo = ref('')

const CARE_TYPES = [
  { value: 'WALK', label: '산책', emoji: '🚶', presets: ['산책 20분', '산책 30분', '배변 완료'] },
  { value: 'MEAL', label: '식사', emoji: '🍚', presets: ['아침 사료 완밥', '저녁 사료 완밥', '습식 급여'] },
  { value: 'SNACK', label: '간식', emoji: '🦴', presets: ['덴탈껌 1개', '트릿 소량', '츄르 1개'] },
]

watch(
  () => [props.open, props.initialType],
  ([isOpen, nextType]) => {
    if (isOpen) {
      careType.value = nextType || 'WALK'
      memo.value = ''
    }
  },
)

const currentOption = computed(
  () => CARE_TYPES.find((item) => item.value === careType.value) || CARE_TYPES[0],
)

function applyPreset(text) {
  memo.value = text
}

function handleSave() {
  emit('submit', {
    careType: careType.value,
    memo: memo.value.trim() || null,
  })
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
      root: { class: '!w-full !max-w-[430px] !mx-auto !rounded-t-3xl !h-auto !border-0' },
      header: { class: '!hidden' },
      content: { class: '!px-5 !pt-3 !pb-6 safe-pb' },
    }"
    @update:visible="handleVisibleChange"
  >
    <div class="space-y-4">
      <div class="w-10 h-1 rounded-full bg-slate-300 mx-auto" />

      <div class="flex items-center justify-between">
        <div>
          <p class="text-xs text-emerald-700 font-semibold">{{ petName }} 케어 기록</p>
          <h2 class="text-lg font-bold text-slate-900">방금 챙겨주셨나요?</h2>
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

      <!-- 활동 종류 3탭 -->
      <SelectButton
        v-model="careType"
        :options="CARE_TYPES"
        optionValue="value"
        optionLabel="label"
        :allowEmpty="false"
        fluid
        :pt="{
          root: { class: '!grid !grid-cols-3 !gap-2' },
          pcToggleButton: { root: { class: '!h-14 !rounded-2xl' } },
        }"
      >
        <template #option="{ option }">
          <div class="flex flex-col items-center justify-center gap-0.5 py-1">
            <span class="text-lg leading-none">{{ option.emoji }}</span>
            <span class="text-xs font-bold">{{ option.label }}</span>
          </div>
        </template>
      </SelectButton>

      <!-- 추천 메모 칩 -->
      <div class="space-y-1.5">
        <p class="text-[11px] font-bold text-slate-500">빠른 메모 선택</p>
        <div class="flex flex-wrap gap-1.5">
          <Button
            v-for="preset in currentOption.presets"
            :key="preset"
            size="small"
            :severity="memo === preset ? undefined : 'secondary'"
            :outlined="memo !== preset"
            class="!h-9 !px-3 !rounded-xl !text-xs !font-semibold"
            @click="applyPreset(preset)"
          >
            {{ preset }}
          </Button>
        </div>
      </div>

      <!-- 자유 메모 -->
      <div class="space-y-1.5">
        <label for="care-memo" class="block text-xs font-bold text-slate-700">
          간단 메모 <span class="text-slate-400 font-normal">(선택)</span>
        </label>
        <InputText
          id="care-memo"
          v-model="memo"
          fluid
          maxlength="200"
          placeholder="예: 공원 30분 산책, 사료 50g"
          class="!h-12 !rounded-2xl !text-sm"
        />
      </div>

      <Button
        fluid
        :disabled="submitting"
        class="!h-12 !rounded-2xl !font-bold !text-sm"
        @click="handleSave"
      >
        <i v-if="submitting" class="pi pi-spin pi-spinner text-sm" />
        <span>{{ submitting ? '기록 저장 중...' : `${currentOption.label} 완료 기록하기` }}</span>
      </Button>
    </div>
  </Drawer>
</template>
