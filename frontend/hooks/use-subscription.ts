"use client"

import { useEffect, useState, useCallback } from "react"
import { usePathname } from "next/navigation"
import { toast } from "sonner"
import { subscriptionApi, type SubscriptionResponse } from "@/lib/subscription-api"

export function useSubscription(isLoggedIn: boolean) {
  const pathname = usePathname()
  const [subscription, setSubscription] = useState<SubscriptionResponse | null>(null)
  const [isSubLoading, setIsSubLoading] = useState(true)

  const fetchSubscription = useCallback(async () => {
    if (!isLoggedIn) {
      setSubscription(null)
      setIsSubLoading(false)
      return
    }

    setIsSubLoading(true)
    try {
      const response = await subscriptionApi.getMySubscription()
      
      // 백엔드 ApiResponse 구조 분기 처리 (data가 null이거나 success가 false인 경우 방어)
      if (response.success && response.data) {
        setSubscription(response.data)
      } else {
        setSubscription(null)
      }
    } catch (error) {
      console.error("구독 정보 조회 중 오류:", error)
      setSubscription(null)
    } finally {
      setIsSubLoading(false)
    }
  }, [isLoggedIn])

  useEffect(() => {
    let ignore = false

    if (isLoggedIn) {
      fetchSubscription()
    } else {
      setSubscription(null)
      setIsSubLoading(false)
    }

    return () => {
      ignore = true
    }
  }, [pathname, isLoggedIn, fetchSubscription])

  return {
    subscription,
    isSubLoading,
    refetchSubscription: fetchSubscription,
    isSubscribed: subscription?.status === "ACTIVE",
  }
}