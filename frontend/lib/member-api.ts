import { apiRequest } from "./api"
import type {
  LoginRequest,
  LoginResponse,
  Member,
  MemberCreateRequest,
  MemberUpdateRequest,
} from "./types"

export const memberApi = {
  signup(request: MemberCreateRequest) {
    return apiRequest<Member>("/api/members", {
      method: "POST",
      body: request,
    })
  },

  login(request: LoginRequest) {
    return apiRequest<LoginResponse>("/api/auth/login", {
      method: "POST",
      body: request,
    })
  },

  logout() {
    return apiRequest<void>("/api/auth/logout", {
      method: "POST",
    })
  },

  reissue() {
    return apiRequest<void>("/api/auth/reissue", {
      method: "POST",
    })
  },

  getMyInfo() {
    return apiRequest<Member>("/api/members/me", {
      method: "GET",
    })
  },

  updateMyInfo(request: MemberUpdateRequest) {
    return apiRequest<Member>("/api/members/me", {
      method: "PATCH",
      body: request,
    })
  },

  withdrawMyAccount() {
    return apiRequest<void>("/api/members/me", {
      method: "DELETE",
    })
  },
}