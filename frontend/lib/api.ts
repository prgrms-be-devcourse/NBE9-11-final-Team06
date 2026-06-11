import { authStorage } from './auth'
import type { ApiResponse } from './types'

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080'

type HttpMethod = 'GET' | 'POST' | 'PATCH' | 'DELETE'

interface ApiRequestOptions {
  method: HttpMethod
  body?: unknown
  withAuth?: boolean
}

export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions
): Promise<ApiResponse<T>> {
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  }

  if (options.withAuth) {
    const accessToken = authStorage.getAccessToken()

    if (accessToken) {
      headers.Authorization = `Bearer ${accessToken}`
    }
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: options.method,
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  })

  const contentType = response.headers.get('content-type')

  if (!contentType?.includes('application/json')) {
    return {
      success: false,
      code: String(response.status),
      message: response.status === 401 ? '인증이 필요합니다.' : '요청 처리 중 오류가 발생했습니다.',
    }
  }

  const data = (await response.json()) as ApiResponse<T>

  return data
}