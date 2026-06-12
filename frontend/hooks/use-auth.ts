"use client"

import { useEffect, useState } from "react"
import { usePathname, useRouter } from "next/navigation"
import { toast } from "sonner"
import { memberApi } from "@/lib/member-api"

export function useAuth() {
  const router = useRouter()
  const pathname = usePathname()

  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [isAuthLoading, setIsAuthLoading] = useState(true)
  const [isLogoutLoading, setIsLogoutLoading] = useState(false)

  useEffect(() => {
    let ignore = false

    async function checkAuth() {
      setIsAuthLoading(true)

      try {
        const response = await memberApi.getMyInfo()

        if (!ignore) {
          setIsLoggedIn(response.success && Boolean(response.data))
        }
      } catch {
        if (!ignore) {
          setIsLoggedIn(false)
        }
      } finally {
        if (!ignore) {
          setIsAuthLoading(false)
        }
      }
    }

    checkAuth()

    return () => {
      ignore = true
    }
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