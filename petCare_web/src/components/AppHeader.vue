<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <header class="border-b border-slate-200 bg-white">
    <div class="mx-auto flex h-16 max-w-5xl items-center justify-between px-4 sm:px-6">
      <RouterLink to="/" class="flex items-center gap-2 text-lg font-bold text-emerald-600">
        <span>🐾</span>
        <span>PetCare</span>
      </RouterLink>

      <nav class="flex items-center gap-6 text-sm font-medium">
        <RouterLink
          to="/"
          class="text-slate-600 transition hover:text-emerald-600"
          active-class="text-emerald-600 font-semibold"
        >
          홈
        </RouterLink>
        <RouterLink
          to="/about"
          class="text-slate-600 transition hover:text-emerald-600"
          active-class="text-emerald-600 font-semibold"
        >
          소개
        </RouterLink>
      </nav>
    </div>
  </header>
</template>
