import { computed, reactive } from 'vue'

const SESSION_KEY = 'petcare_session_v1'

function loadSession() {
  try {
    const raw = localStorage.getItem(SESSION_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

const state = reactive({
  user: loadSession(),
})

export function useAuthStore() {
  const isAuthenticated = computed(() => Boolean(state.user?.memberId))

  function setSession(authResponse) {
    state.user = authResponse
    localStorage.setItem(SESSION_KEY, JSON.stringify(authResponse))
  }

  function clearSession() {
    state.user = null
    localStorage.removeItem(SESSION_KEY)
  }

  function updateFamilyMeta(family) {
    if (!state.user || !family) return
    const updated = {
      ...state.user,
      familyId: family.familyId ?? state.user.familyId,
      familyName: family.name ?? state.user.familyName,
      inviteCode: family.inviteCode ?? state.user.inviteCode,
    }
    setSession(updated)
  }

  return {
    state,
    isAuthenticated,
    setSession,
    clearSession,
    updateFamilyMeta,
  }
}
