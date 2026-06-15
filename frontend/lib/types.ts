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

export type CompanionType = "SOLO" | "COUPLE" | "FRIEND" | "FAMILY" | "PARENT"

export type MobilityLevel = "LOW" | "NORMAL" | "HIGH"

export type CategoryType = string

export interface PreferenceCategory {
  id: number
  name: string
  type: CategoryType
}

export interface UserPreference {
  id: number
  preferredArea: string
  categories: PreferenceCategory[]
  companionType: CompanionType
  mobilityLevel: MobilityLevel
  avoidCrowded: boolean
}

export interface UserPreferenceCreateRequest {
  preferredArea: string
  categoryIds: number[]
  companionType: CompanionType
  mobilityLevel: MobilityLevel
  avoidCrowded: boolean
}

export interface UserPreferenceUpdateRequest {
  preferredArea?: string
  categoryIds?: number[]
  companionType?: CompanionType
  mobilityLevel?: MobilityLevel
  avoidCrowded?: boolean
}