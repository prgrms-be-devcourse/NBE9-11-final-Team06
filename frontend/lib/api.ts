import type { ApiResponse } from "./types"

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

type HttpMethod = "GET" | "POST" | "PATCH" | "DELETE"

interface ApiRequestOptions {
  method: HttpMethod
  body?: unknown
  headers?: Record<string, string>
}

export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions,
): Promise<ApiResponse<T>> {
  const requestInit = createRequestInit(options)

  let response = await fetch(`${API_BASE_URL}${path}`, requestInit)

  if (response.status === 401 && shouldTryReissue(path)) {
    const reissueSuccess = await reissueAccessToken()

    if (reissueSuccess) {
      response = await fetch(`${API_BASE_URL}${path}`, createRequestInit(options))
    }
  }

  return parseResponse<T>(response)
}

function createRequestInit(options: ApiRequestOptions): RequestInit {
  const customHeaders = options.headers ?? {}
  const headers: HeadersInit = {
    "Content-Type": "application/json",
    ...customHeaders,
  }

  const accessToken = getAccessToken()

  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`
  }

  return {
    method: options.method,
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
    credentials: "include",
  }
}

function getAccessToken() {
  if (typeof window === "undefined") {
    return null
  }

  const storages = [localStorage, sessionStorage]
  const tokenKeys = ["accessToken", "access_token", "token", "jwt"]

  for (const storage of storages) {
    for (const tokenKey of tokenKeys) {
      const token = normalizeAccessToken(storage.getItem(tokenKey))

      if (token) {
        return token
      }
    }
  }

  return null
}

function normalizeAccessToken(value: string | null | undefined) {
  if (!value) {
    return null
  }

  const token = value.trim().replace(/^Bearer\s+/i, "")

  if (!token || token === "undefined" || token === "null") {
    return null
  }

  return token
}

function shouldTryReissue(path: string): boolean {
  return path !== "/api/auth/login" && path !== "/api/auth/reissue"
}

async function reissueAccessToken(): Promise<boolean> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/auth/reissue`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include",
    })

    return response.ok
  } catch {
    return false
  }
}

async function parseResponse<T>(response: Response): Promise<ApiResponse<T>> {
  const contentType = response.headers.get("content-type")

  if (!contentType?.includes("application/json")) {
    return {
      success: false,
      code: String(response.status),
      message:
        response.status === 401
          ? "인증이 필요합니다."
          : "요청 처리 중 오류가 발생했습니다.",
    }
  }

  return (await response.json()) as ApiResponse<T>
}