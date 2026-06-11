import { apiRequest } from './api'
import type {
  LoginRequest,
  LoginResponse,
  Member,
  MemberCreateRequest,
  MemberUpdateRequest,
} from './types'

export const memberApi = {
  signup(request: MemberCreateRequest) {
    return apiRequest<Member>('/api/members', {
      method: 'POST',
      body: request,
    })
  },

  login(request: LoginRequest) {
    return apiRequest<LoginResponse>('/api/auth/login', {
      method: 'POST',
      body: request,
    })
  },

  logout() {
    return apiRequest<void>('/api/auth/logout', {
      method: 'POST',
      withAuth: true,
    })
  },

  getMyInfo() {
    return apiRequest<Member>('/api/members/me', {
      method: 'GET',
      withAuth: true,
    })
  },

  updateMyInfo(request: MemberUpdateRequest) {
    return apiRequest<Member>('/api/members/me', {
      method: 'PATCH',
      body: request,
      withAuth: true,
    })
  },

  withdrawMyAccount() {
    return apiRequest<void>('/api/members/me', {
      method: 'DELETE',
      withAuth: true,
    })
  },
}