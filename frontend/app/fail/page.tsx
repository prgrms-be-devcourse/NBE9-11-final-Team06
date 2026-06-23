"use client"

import { useSearchParams, useRouter } from "next/navigation"
import { AlertCircle } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"

export const dynamic = 'force-dynamic'

export default function FailPage() {
  const searchParams = useSearchParams()
  const router = useRouter()

  const code = searchParams.get("code")
  const message = searchParams.get("message")

  return (
    <div className="flex min-h-screen items-center justify-center bg-secondary/10 p-4">
      <Card className="w-full max-w-md p-6 shadow-xl bg-card text-center">
        <AlertCircle className="size-14 text-destructive mx-auto mb-4" />
        <h2 className="text-2xl font-bold text-foreground">카드 인증에 실패했습니다</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          결제 진행 도중 에러가 발생했거나 사용자에 의해 취소되었습니다.
        </p>

        <div className="mt-6 w-full rounded-xl bg-destructive/5 p-4 text-left text-sm space-y-1">
          <p className="text-xs text-muted-foreground font-semibold">에러 코드: {code || "UNKNOWN_ERROR"}</p>
          <p className="text-sm text-destructive font-medium">{message || "인증이 거부되었습니다."}</p>
        </div>

        <div className="mt-8 flex gap-3">
          <Button variant="outline" onClick={() => router.push("/pricing")} className="flex-1 font-semibold">
            이전 요금제 화면
          </Button>
          <Button onClick={() => router.push("/")} className="flex-1 font-semibold">
            메인 홈으로
          </Button>
        </div>
      </Card>
    </div>
  )
}