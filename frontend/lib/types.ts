export interface ApiResponse<T> {
  success: boolean
  data?: T
  code?: string
  message?: string
}

export interface Member {
  id: number
  email: string
  nickname: string
  profileImageUrl: string | null
  role: string
  status: string
}

export interface MemberCreateRequest {
  email: string
  password: string
  nickname: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  member: Member
}

export interface MemberUpdateRequest {
  nickname?: string
  profileImageUrl?: string
}