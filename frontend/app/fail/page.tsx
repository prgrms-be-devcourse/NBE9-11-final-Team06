"use client"

import { Suspense } from "react"
import { useSearchParams, useRouter } from "next/navigation"
import { AlertTriangle, XCircle } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"

function FailContent() {
  const searchParams = useSearchParams()
  const router = useRouter()

  // 토스페이먼츠가 실패 시 자동으로 붙여주는 에러 코드와 메시지
  const errorCode = searchParams.get("code")
  const errorMessage = searchParams.get("message")
  
  // 우리가 주소 뒤에 심어둔 유입 경로 식별자
  const fromPage = searchParams.get("from")

  // 유입 경로에 따라 이전 페이지로 돌려보내는 함수
  const handleRetryRedirect = () => {
    if (fromPage === "mypage") {
      router.push("/mypage")
    } else {
      router.push("/pricing")
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-secondary/10 p-4">
      <Card className="w-full max-w-md p-6 shadow-xl bg-card">
        <div className="flex flex-col items-center justify-center py-6 text-center">
          <XCircle className="size-14 text-destructive mb-4" />
          <h2 className="text-2xl font-bold text-destructive">결제 수단 등록 실패</h2>
          
          <p className="mt-2 text-sm text-muted-foreground">
            {errorMessage || "사용자가 결제를 취소했거나 인증에 실패했습니다."}
          </p>

          {errorCode && (
            <div className="mt-4 rounded-lg bg-destructive/10 px-3 py-1.5 text-xs font-mono text-destructive">
              에러 코드: {errorCode}
            </div>
          )}

          <div className="mt-8 flex w-full gap-3">
            {/* 유입 경로에 따라 동적으로 작동하는 다시 시도 버튼 */}
            <Button 
              variant="outline" 
              onClick={handleRetryRedirect} 
              className="flex-1 font-semibold"
            >
              다시 시도
            </Button>
            <Button 
              onClick={() => router.push("/")} 
              className="flex-1 font-semibold"
            >
              홈으로 가기
            </Button>
          </div>
        </div>
      </Card>
    </div>
  )
}

export default function FailPage() {
  return (
    <Suspense fallback={
      <div className="flex min-h-screen items-center justify-center bg-secondary/10 p-4">
        <Card className="w-full max-w-md p-6 shadow-xl bg-card flex flex-col items-center justify-center py-10 text-center gap-4">
          <AlertTriangle className="size-12 text-muted-foreground animate-pulse" />
          <h2 className="text-xl font-bold">오류 정보 분석 중...</h2>
        </Card>
      </div>
    }>
      <FailContent />
    </Suspense>
  )
}