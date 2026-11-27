import { apiRequest } from './client'

export const POST_CATEGORIES = [
  { value: '', label: '전체' },
  { value: 'INFO', label: '정보·팁', badge: 'bg-emerald-50 text-emerald-700' },
  { value: 'QUESTION', label: '질문', badge: 'bg-sky-50 text-sky-700' },
  { value: 'REVIEW', label: '후기', badge: 'bg-amber-50 text-amber-700' },
  { value: 'FREE', label: '자유', badge: 'bg-slate-100 text-slate-700' },
]

export function getCategoryMeta(category) {
  return (
    POST_CATEGORIES.find((item) => item.value === category) || {
      value: category,
      label: '자유',
      badge: 'bg-slate-100 text-slate-700',
    }
  )
}

export function listPosts({ category = '', keyword = '', page = 0, size = 15 } = {}) {
  const params = new URLSearchParams()
  if (category) params.set('category', category)
  if (keyword.trim()) params.set('keyword', keyword.trim())
  params.set('page', String(page))
  params.set('size', String(size))
  return apiRequest(`/api/v1/posts?${params.toString()}`)
}

export function getPost(postId) {
  return apiRequest(`/api/v1/posts/${postId}`)
}

export function createPost(payload) {
  return apiRequest('/api/v1/posts', {
    method: 'POST',
    body: payload,
  })
}

export function updatePost(postId, payload) {
  return apiRequest(`/api/v1/posts/${postId}`, {
    method: 'PUT',
    body: payload,
  })
}

export function deletePost(postId) {
  return apiRequest(`/api/v1/posts/${postId}`, {
    method: 'DELETE',
  })
}

export function addComment(postId, content) {
  return apiRequest(`/api/v1/posts/${postId}/comments`, {
    method: 'POST',
    body: { content },
  })
}

export function deleteComment(postId, commentId) {
  return apiRequest(`/api/v1/posts/${postId}/comments/${commentId}`, {
    method: 'DELETE',
  })
}
