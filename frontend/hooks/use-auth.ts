"use client"

import { useEffect, useState } from "react"
import { usePathname, useRouter } from "next/navigation"
import { toast } from "sonner"
import { authStorage } from "@/lib/auth"
import { memberApi } from "@/lib/member-api"

export function useAuth() {
  const router = useRouter()
  const pathname = usePathname()

  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [isAuthLoading, setIsAuthLoading] = useState(true)
  const [isLogoutLoading, setIsLogoutLoading] = useState(false)

  useEffect(() => {
    const accessToken = authStorage.getAccessToken()

    setIsLoggedIn(Boolean(accessToken))
    setIsAuthLoading(false)
  }, [pathname])

  async function logout() {
    setIsLogoutLoading(true)

    try {
      const response = await memberApi.logout()

      if (!response.success) {
        toast.error(response.message ?? "로그아웃 처리 중 오류가 발생했습니다.")
      } else {
        toast.success("로그아웃되었습니다.")
      }
    } catch {
      toast.error("서버와 통신 중 오류가 발생했습니다.")
    } finally {
      authStorage.removeAccessToken()
      setIsLoggedIn(false)
      setIsLogoutLoading(false)
      router.push("/")
      router.refresh()
    }
  }

  return {
    isLoggedIn,
    isAuthLoading,
    isLogoutLoading,
    logout,
  }
}