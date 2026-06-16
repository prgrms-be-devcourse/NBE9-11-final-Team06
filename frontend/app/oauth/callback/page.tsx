"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { toast } from "sonner"
import { memberApi } from "@/lib/member-api"
import { preferenceApi } from "@/lib/preference-api"
import { MapPin } from "lucide-react"

export default function OAuthCallbackPage() {
  const router = useRouter()
  const [message, setMessage] = useState("로그인 정보를 확인하고 있어요.")

  useEffect(() => {
    let ignore = false

    async function handleOAuthCallback() {
      try {
        const memberResponse = await memberApi.getMyInfo()

        if (ignore) {
          return
        }

        if (!memberResponse.success || !memberResponse.data) {
          router.replace("/login?error=oauth")
          return
        }

        setMessage("선호 정보를 확인하고 있어요.")

        const preferenceResponse = await preferenceApi.getMyPreference()

        if (ignore) {
          return
        }

        if (preferenceResponse.success && preferenceResponse.data) {
          router.replace("/")
          return
        }

        if (preferenceResponse.success && !preferenceResponse.data) {
          router.replace("/onboarding")
          return
        }

        if (preferenceResponse.code === "PREFERENCE_NOT_FOUND") {
          router.replace("/onboarding")
          return
        }

        toast.error(
          preferenceResponse.message ?? "선호 정보 조회 중 오류가 발생했습니다.",
        )
        router.replace("/login?error=preference")
      } catch {
        if (!ignore) {
          toast.error("서버와 통신 중 오류가 발생했습니다.")
          router.replace("/login?error=oauth")
        }
      }
    }

    handleOAuthCallback()

    return () => {
      ignore = true
    }
  }, [router])

  return (
    <main className="flex min-h-screen items-center justify-center bg-secondary/30 px-4">
      <div className="flex flex-col items-center text-center">
        <div className="flex size-14 items-center justify-center rounded-2xl bg-primary text-primary-foreground">
          <MapPin className="size-7" />
        </div>

        <h1 className="mt-5 font-heading text-2xl font-bold tracking-tight">
          오늘 어디가?
        </h1>

        <p className="mt-3 text-sm text-muted-foreground">{message}</p>
      </div>
    </main>
  )
}