import { useToast as usePrimeToast } from 'primevue/usetoast'

const SEVERITY_MAP = {
  success: 'success',
  error: 'error',
  warn: 'warn',
  info: 'info',
}

export function useToast() {
  const primeToast = usePrimeToast()

  function showToast(message, type = 'success', duration = 2600) {
    primeToast.add({
      severity: SEVERITY_MAP[type] || 'info',
      summary: message,
      life: duration,
    })
  }

  function hideToast() {
    primeToast.removeAllGroups()
  }

  return {
    showToast,
    hideToast,
  }
}
