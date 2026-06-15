import { apiRequest } from "./api"
import type {
  UserPreference,
  UserPreferenceCreateRequest,
  UserPreferenceUpdateRequest,
} from "./types"

export const preferenceApi = {
  createMyPreference(request: UserPreferenceCreateRequest) {
    return apiRequest<UserPreference>("/api/preferences/me", {
      method: "POST",
      body: request,
    })
  },

  getMyPreference() {
    return apiRequest<UserPreference | null>("/api/preferences/me", {
      method: "GET",
    })
  },

  updateMyPreference(request: UserPreferenceUpdateRequest) {
    return apiRequest<UserPreference>("/api/preferences/me", {
      method: "PATCH",
      body: request,
    })
  },

  deleteMyPreference() {
    return apiRequest<void>("/api/preferences/me", {
      method: "DELETE",
    })
  },
}