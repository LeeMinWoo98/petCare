<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Toast from 'primevue/toast'
import AppTopBar from '@/components/AppTopBar.vue'
import AppBottomNav from '@/components/AppBottomNav.vue'

const route = useRoute()
const showBottomNav = computed(() => !route.meta?.hideBottomNav)
</script>

<template>
  <div class="min-h-dvh bg-slate-100 flex justify-center">
    <div
      class="w-full max-w-[430px] min-h-dvh bg-slate-50 border-x border-slate-200/80 shadow-sm flex flex-col relative"
    >
      <AppTopBar />
      <Toast
        position="top-center"
        :pt="{ root: { class: '!w-[calc(100%-2rem)] !max-w-[398px] !top-16' } }"
      />

      <main
        class="flex-1 px-4 pt-4"
        :class="showBottomNav ? 'pb-24' : 'pb-8'"
      >
        <RouterView />
      </main>

      <AppBottomNav v-if="showBottomNav" />
    </div>
  </div>
</template>
