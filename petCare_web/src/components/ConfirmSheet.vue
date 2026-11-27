<script setup>
import Drawer from 'primevue/drawer'
import Button from 'primevue/button'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, required: true },
  description: { type: String, default: '' },
  confirmText: { type: String, default: '확인' },
  cancelText: { type: String, default: '취소' },
  danger: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
})

const emit = defineEmits(['confirm', 'cancel'])

function handleVisibleChange(visible) {
  if (!visible && !props.loading) {
    emit('cancel')
  }
}
</script>

<template>
  <Drawer
    :visible="open"
    position="bottom"
    :showCloseIcon="false"
    :dismissable="!loading"
    :closeOnEscape="!loading"
    :pt="{
      root: { class: '!w-full !max-w-[430px] !mx-auto !rounded-t-3xl !h-auto !border-0' },
      header: { class: '!hidden' },
      content: { class: '!px-5 !pt-3 !pb-6 safe-pb' },
    }"
    @update:visible="handleVisibleChange"
  >
    <div class="space-y-4">
      <div class="w-10 h-1 rounded-full bg-slate-300 mx-auto" />

      <div class="space-y-1.5 text-center">
        <h2 class="text-lg font-bold text-slate-900">{{ title }}</h2>
        <p v-if="description" class="text-sm text-slate-600 leading-relaxed">
          {{ description }}
        </p>
      </div>

      <div class="grid grid-cols-2 gap-2.5 pt-2">
        <Button
          severity="secondary"
          :disabled="loading"
          class="!h-12 !rounded-2xl !font-semibold !text-sm"
          @click="emit('cancel')"
        >
          {{ cancelText }}
        </Button>
        <Button
          :severity="danger ? 'danger' : undefined"
          :disabled="loading"
          class="!h-12 !rounded-2xl !font-semibold !text-sm"
          @click="emit('confirm')"
        >
          <i v-if="loading" class="pi pi-spin pi-spinner text-sm" />
          <span>{{ loading ? '처리 중...' : confirmText }}</span>
        </Button>
      </div>
    </div>
  </Drawer>
</template>
