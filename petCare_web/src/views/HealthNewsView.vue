<script setup>
import Tag from 'primevue/tag'
</script>

<template>
  <section class="space-y-4">
    <div class="bg-white rounded-3xl p-6 border border-slate-200/80 text-center space-y-3">
      <div class="w-14 h-14 mx-auto rounded-2xl bg-sky-50 text-2xl flex items-center justify-center">
        🩺
      </div>
      <div class="space-y-1.5">
        <Tag value="Step 5 구현 예정" severity="info" rounded class="!text-[11px] !font-bold" />
        <h2 class="text-lg font-bold text-slate-900">질병 · 신약 · 건강정보 피드</h2>
        <p class="text-xs text-slate-600 leading-relaxed">
          자동 수집 후 관리자 검수를 통과한 신뢰할 수 있는 반려동물 질병·신약·건강관리 소식이 이곳에 제공됩니다.
        </p>
      </div>
    </div>
  </section>
</template>
