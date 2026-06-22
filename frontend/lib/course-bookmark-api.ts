import { apiRequest } from "@/lib/api"
import type {
  ApiResponse,
  CourseBookmarkResponse,
  SavedCoursePageResponse,
} from "@/lib/types"

export const courseBookmarkApi = {
  toggleBookmark(courseId: number): Promise<ApiResponse<CourseBookmarkResponse>> {
    return apiRequest<CourseBookmarkResponse>(`/api/courses/${courseId}/bookmark`, {
      method: "POST",
    })
  },

  getBookmarkStatus(courseId: number): Promise<ApiResponse<boolean>> {
    return apiRequest<boolean>(`/api/courses/${courseId}/bookmark`, {
      method: "GET",
    })
  },

  getSavedCourses(page = 0): Promise<ApiResponse<SavedCoursePageResponse>> {
    return apiRequest<SavedCoursePageResponse>(
      `/api/courses/bookmarks?page=${page}`,
      {
        method: "GET",
      },
    )
  },
}