import { apiRequest } from "./api"
import type { PreferenceCategory } from "./types"

export const categoryApi = {
  getCategories() {
    return apiRequest<PreferenceCategory[]>("/api/categories", {
      method: "GET",
    })
  },
}