export const SPECIES_OPTIONS = [
  { value: 'DOG', label: '강아지', emoji: '🐶' },
  { value: 'CAT', label: '고양이', emoji: '🐱' },
  { value: 'OTHER', label: '기타', emoji: '🐾' },
]

export const GENDER_OPTIONS = [
  { value: 'MALE', label: '남아' },
  { value: 'FEMALE', label: '여아' },
  { value: 'UNKNOWN', label: '미상' },
]

export function getSpeciesLabel(species) {
  return SPECIES_OPTIONS.find((item) => item.value === species)?.label || '기타'
}

export function getSpeciesEmoji(species) {
  return SPECIES_OPTIONS.find((item) => item.value === species)?.emoji || '🐾'
}

export function getGenderLabel(gender) {
  return GENDER_OPTIONS.find((item) => item.value === gender)?.label || '미상'
}

export function parseIsoDate(dateStr) {
  if (!dateStr) return null
  if (dateObjIsValidDate(dateStr)) return dateStr
  const parts = String(dateStr).split('-').map(Number)
  if (parts.length !== 3 || parts.some((n) => !Number.isFinite(n))) {
    return null
  }
  const dt = new Date(parts[0], parts[1] - 1, parts[2])
  if (
    Number.isNaN(dt.getTime()) ||
    dt.getFullYear() !== parts[0] ||
    dt.getMonth() !== parts[1] - 1 ||
    dt.getDate() !== parts[2]
  ) {
    return null
  }
  return dt
}

export function formatIsoDate(dateObj) {
  if (!dateObj) return null
  if (typeof dateObj === 'string') return dateObj
  if (!dateObjIsValidDate(dateObj)) return null
  const y = dateObj.getFullYear()
  const m = String(dateObj.getMonth() + 1).padStart(2, '0')
  const d = String(dateObj.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function dateObjIsValidDate(value) {
  return value instanceof Date && !Number.isNaN(value.getTime())
}

export function formatPetAge(birthDate) {
  if (!birthDate) return '나이 미등록'
  const birth = birthDate instanceof Date ? birthDate : parseIsoDate(birthDate) || new Date(birthDate)
  if (!birth || Number.isNaN(birth.getTime())) return '나이 미등록'

  const now = new Date()
  let months = (now.getFullYear() - birth.getFullYear()) * 12 + (now.getMonth() - birth.getMonth())
  if (now.getDate() < birth.getDate()) {
    months -= 1
  }
  if (months < 0) return '출생 예정'
  if (months < 12) return `${months}개월`

  const years = Math.floor(months / 12)
  const remMonths = months % 12
  return remMonths > 0 ? `${years}살 ${remMonths}개월` : `${years}살`
}

export async function copyToClipboard(text) {
  if (!text) return false
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
      return true
    }
  } catch {
    // fallback below
  }

  try {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.focus()
    textarea.select()
    const ok = document.execCommand('copy')
    document.body.removeChild(textarea)
    return ok
  } catch {
    return false
  }
}
