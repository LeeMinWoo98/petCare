import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView,
    meta: { title: 'PetCare' },
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '로그인', showBack: true, hideBottomNav: true },
  },
  {
    path: '/signup',
    name: 'signup',
    component: () => import('@/views/SignupView.vue'),
    meta: { title: '회원가입', showBack: true, hideBottomNav: true },
  },
  {
    path: '/pets',
    name: 'pets',
    component: () => import('@/views/PetListView.vue'),
    meta: { title: '반려동물 · 약', requiresAuth: true },
  },
  {
    path: '/pets/new',
    name: 'pet-new',
    component: () => import('@/views/PetFormView.vue'),
    meta: { title: '반려동물 등록', showBack: true, hideBottomNav: true, requiresAuth: true },
  },
  {
    path: '/pets/:petId/edit',
    name: 'pet-edit',
    component: () => import('@/views/PetFormView.vue'),
    meta: { title: '반려동물 수정', showBack: true, hideBottomNav: true, requiresAuth: true },
  },
  {
    path: '/community',
    name: 'community',
    component: () => import('@/views/CommunityView.vue'),
    meta: { title: '커뮤니티' },
  },
  {
    path: '/community/new',
    name: 'community-new',
    component: () => import('@/views/PostFormView.vue'),
    meta: { title: '글쓰기', showBack: true, hideBottomNav: true, requiresAuth: true },
  },
  {
    path: '/community/:postId',
    name: 'community-detail',
    component: () => import('@/views/PostDetailView.vue'),
    meta: { title: '게시글 상세', showBack: true, hideBottomNav: true },
  },
  {
    path: '/community/:postId/edit',
    name: 'community-edit',
    component: () => import('@/views/PostFormView.vue'),
    meta: { title: '게시글 수정', showBack: true, hideBottomNav: true, requiresAuth: true },
  },
  {
    path: '/health-news',
    name: 'health-news',
    component: () => import('@/views/HealthNewsView.vue'),
    meta: { title: '건강정보' },
  },
  {
    path: '/family',
    name: 'family',
    component: () => import('@/views/FamilyView.vue'),
    meta: { title: '내 가족', requiresAuth: true },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  const { isAuthenticated } = useAuthStore()
  if (to.meta?.requiresAuth && !isAuthenticated.value) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
