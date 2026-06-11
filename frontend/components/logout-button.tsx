"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { memberApi } from "@/lib/member-api"
import { authStorage } from "@/lib/auth"

export function LogoutButton() {
  const router = useRouter()

  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    const accessToken = authStorage.getAccessToken()
    setIsLoggedIn(Boolean(accessToken))
  }, [])

  async function handleLogout() {
    setIsLoading(true)

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
      setIsLoading(false)
      router.push("/")
      router.refresh()
    }
  }

  if (!isLoggedIn) {
    return null
  }

  return (
    <Button type="button" variant="outline" onClick={handleLogout} disabled={isLoading}>
      {isLoading ? "로그아웃 중..." : "로그아웃"}
    </Button>
  )
}