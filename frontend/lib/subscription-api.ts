import { apiRequest } from "./api"

// 백엔드 Record DTO 스펙 반영
export type PlanResponse = {
  id: number
  name: string
  displayName: string
  amount: number
}

export type SubscriptionStatus = "PENDING" | "ACTIVE" | "PAUSED" | "CANCELED_RESERVED" | "CANCELED" | "MANUAL_CHECK" | "EXPIRED_PAYMENT_PENDING"

export type SubscriptionResponse = {
  subscriptionId: number
  planName: string
  amount: number
  nextBillingDate: string // LocalDate 매핑
  status: SubscriptionStatus
}

export type SubscriptionRequest = {
  billingInfoId: number
  planId: number
}

export const subscriptionApi = {
  // 1. 활성화된 플랜 목록 조회
  getPlans() {
    return apiRequest<PlanResponse[]>("/api/v1/subscriptions/plans", {
      method: "GET",
    })
  },

  // 2. 현재 로그인한 회원의 활성화된 구독 정보 조회
  getMySubscription() {
    return apiRequest<SubscriptionResponse | null>("/api/v1/subscriptions/me", {
      method: "GET",
    })
  },

  // 3. 정기 구독 신청 (최초 결제 포함)
  startSubscription(request: SubscriptionRequest, idempotencyKey: string) {
    return apiRequest<SubscriptionResponse>("/api/v1/subscriptions", {
      method: "POST",
      headers: {
        "Idempotency-Key": idempotencyKey 
      },
      body: request,
    })
  },

  // 4. 정기 구독 해지
  cancelSubscription(subscriptionId: number) {
    return apiRequest<void>(`/api/v1/subscriptions/${subscriptionId}`, {
      method: "DELETE",
    })
  },
}