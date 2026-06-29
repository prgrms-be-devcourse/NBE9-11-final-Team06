// @/lib/billing-api.ts
import { ApiResponse } from "./types" // 프로젝트 내 공통 ApiResponse 타입 위치에 맞게 수정해주세요.

export interface BillingCardResponse {
  id: number
  cardCompany: string
  cardNumber: string
  createdAt: string
}

export interface PaymentHistoryResponse {
  [key: string]: any
}

export const billingApi = {
  // 등록된 결제 카드 목록 조회
  getBillingKeys: async (): Promise<ApiResponse<BillingCardResponse[]>> => {
    const response = await fetch("/api/v1/billing", {
      method: "GET",
      headers: { "Content-Type": "application/json" },
    })
    return response.json()
  },

  // 등록된 결제 카드 삭제
  deleteBillingKey: async (billingInfoId: number): Promise<ApiResponse<void>> => {
    const response = await fetch(`/api/v1/billing/${billingInfoId}`, {
      method: "DELETE",
      headers: { "Content-Type": "application/json" },
    })
    return response.json()
  },

  // [신규] 결제 내역 조회
  getMyPaymentHistories: async (): Promise<ApiResponse<PaymentHistoryResponse[]>> => {
    const response = await fetch("/api/v1/subscriptions/payments/me")
    return response.json()
  },

  // [신규] 결제 취소
  cancelPayment: async (paymentHistoryId: number, cancelReason: string): Promise<ApiResponse<void>> => {
    const response = await fetch(`/api/v1/subscriptions/payments/${paymentHistoryId}/cancel`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ cancelReason }),
    })
    return response.json()
  },

  issueBillingKey: async (idempotencyKey: string, authKey: string, customerKey: string): Promise<ApiResponse<BillingCardResponse>> => {
    const response = await fetch("/api/v1/billing/issue", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Idempotency-Key": idempotencyKey, 
      },
      body: JSON.stringify({
        authKey,
        customerKey,
      }),
    })
    return response.json()
  },
}