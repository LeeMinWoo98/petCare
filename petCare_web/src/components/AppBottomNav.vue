<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const tabs = [
  { name: 'home', path: '/', label: '홈', icon: 'pi pi-home' },
  { name: 'pets', path: '/pets', label: '반려동물·약', icon: 'pi pi-heart' },
  { name: 'community', path: '/community', label: '커뮤니티', icon: 'pi pi-comments' },
  { name: 'health-news', path: '/health-news', label: '건강정보', icon: 'pi pi-book' },
  { name: 'family', path: '/family', label: '내 가족', icon: 'pi pi-users' },
]

function isActive(tab) {
  if (tab.path === '/') {
    return route.path === '/'
  }
  return route.path.startsWith(tab.path)
}
</script>

<template>
  <nav
    aria-label="하단 탭 내비게이션"
    class="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[430px] z-30 bg-white/95 backdrop-blur border-t border-slate-200/90 safe-pb"
  >
    <div class="grid grid-cols-5 h-[60px]">
      <RouterLink
        v-for="tab in tabs"
        :key="tab.name"
        :to="tab.path"
        class="flex flex-col items-center justify-center gap-1 min-h-[44px] active:scale-95 transition"
        :class="isActive(tab) ? 'text-emerald-600' : 'text-slate-400'"
      >
        <i :class="[tab.icon, 'text-lg leading-none']" />
        <span class="text-[11px] font-semibold leading-none tracking-tight">
          {{ tab.label }}
        </span>
      </RouterLink>
    </div>
  </nav>
</template>
