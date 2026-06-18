import type { ApiResponse } from "./types"

type HttpMethod = "GET" | "POST" | "PATCH" | "DELETE"

interface ApiRequestOptions {
  method: HttpMethod
  body?: unknown
}

export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions,
): Promise<ApiResponse<T>> {
  const requestInit = createRequestInit(options)

  let response = await fetch(path, requestInit)

  if (response.status === 401 && shouldTryReissue(path)) {
    const reissueSuccess = await reissueAccessToken()

    if (reissueSuccess) {
      response = await fetch(path, requestInit)
    }
  }

  return parseResponse<T>(response)
}

function createRequestInit(options: ApiRequestOptions): RequestInit {
  return {
    method: options.method,
    headers: {
      "Content-Type": "application/json",
    },
    body: options.body ? JSON.stringify(options.body) : undefined,
    credentials: "include",
  }
}

function shouldTryReissue(path: string): boolean {
  return path !== "/api/auth/login" && path !== "/api/auth/reissue"
}

async function reissueAccessToken(): Promise<boolean> {
  try {
    const response = await fetch(`/api/auth/reissue`, {
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