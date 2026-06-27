// @/app/success/page.tsx
"use client"

import { useEffect, useState, useRef, Suspense, useCallback } from "react"
import { useSearchParams, useRouter } from "next/navigation"
import { CheckCircle2, Loader2, AlertTriangle, RefreshCw } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { billingApi, type BillingCardResponse } from "@/lib/billing-api" // 수정된 api 임포트
import { type ApiResponse } from "@/lib/types"

function SuccessContent() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const requestSent = useRef(false)

  //  상태 정의 세분화 ("loading" | "success" | "error" | "timeout_error")
  const [status, setStatus] = useState<"loading" | "success" | "error" | "timeout_error">("loading")
  const [resultData, setResultData] = useState<BillingCardResponse | null>(null)
  const [errorMessage, setErrorMessage] = useState<string>("")

  const authKey = searchParams.get("authKey")
  const customerKey = searchParams.get("customerKey")
  const idempotencyKey = searchParams.get("idempotencyKey") // 💡 쿼리 스트링에서 추출
  const plan = searchParams.get("plan")
  const fromPage = searchParams.get("from")

  //  구출 및 재시도를 위해 함수화 처리
  const requestIssue = useCallback(async () => {
    if (!authKey || !customerKey || !idempotencyKey) {
      setStatus("error")
      setErrorMessage("결제 인증 및 멱등성 검증 정보가 누락되었습니다.")
      return
    }

    setStatus("loading")
    try {
      //  공통 billingApi 엔진을 호출하며 멱등키 전달
      const result = await billingApi.issueBillingKey(idempotencyKey, authKey, customerKey)

      if (result.success && result.data) {
        setResultData(result.data)
        setStatus("success")
      } else {
        // 💡 백엔드에서 지정한 최종 타임아웃 에러 코드 매핑 판단
        if (result.code === "NETWORK_ERROR_FINAL_FAILED") {
          setStatus("timeout_error")
          setErrorMessage("토스페이먼츠 서버 연결이 지연되고 있습니다. 기존 요청 수단으로 다시 연결을 시도할 수 있습니다.")
        } else {
          setStatus("error")
          setErrorMessage(result.message || "빌링키 생성 중 오류가 발생했습니다.")
        }
      }
    } catch (error) {
      console.error("백엔드 통신 에러:", error)
      setStatus("error")
      setErrorMessage("네트워크 연결에 실패했습니다.")
    }
  }, [authKey, customerKey, idempotencyKey])

  // 최초 컴포넌트 로드 시 실행
  useEffect(() => {
    if (requestSent.current) return
    requestSent.current = true

    void requestIssue()
  }, [requestIssue])

  // 유저가 [네트워크 재시도] 버튼을 직접 눌렀을 때 작동하는 구출 핸들러
  const handleRetryBilling = () => {
    requestSent.current = false // 락 해제
    void requestIssue() // 아까 그 멱등키 그대로 재호출!
  }

  const handleRedirect = () => {
    if (fromPage === "mypage") {
      router.push("/mypage")
    } else {
      router.push("/pricing")
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-secondary/10 p-4">
      <Card className="w-full max-w-md p-6 shadow-xl bg-card">
        {status === "loading" && (
          <div className="flex flex-col items-center justify-center py-10 text-center gap-4">
            <Loader2 className="size-12 animate-spin text-primary" />
            <h2 className="text-xl font-bold">결제 카드 등록 처리 중</h2>
            <p className="text-sm text-muted-foreground">
              안전하게 빌링 정보를 생성하고 있습니다. 잠시만 기다려주세요.
            </p>
          </div>
        )}

        {status === "success" && (
          <div className="flex flex-col items-center justify-center py-6 text-center">
            <CheckCircle2 className="size-14 text-green-500 mb-4" />
            <h2 className="text-2xl font-bold text-foreground">카드 등록 완료!</h2>
            {fromPage === "mypage" ? (
              <p className="mt-2 text-sm text-muted-foreground">
                마이페이지 정기 결제 관리에 사용할 수단이 안전하게 추가되었습니다.
              </p>
            ) : (
              <p className="mt-2 text-sm text-muted-foreground">
                선택하신 <span className="font-semibold text-primary">[{plan === "premium" ? "프리미엄 요금제" : "베이직 요금제"}]</span> 정기 결제 카드가 성공적으로 등록되었습니다.
              </p>
            )}
            {resultData && (
              <div className="mt-6 w-full rounded-xl bg-secondary/50 p-4 text-left text-sm space-y-2">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">등록 카드사</span>
                  <span className="font-semibold text-foreground">{resultData.cardCompany}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">카드 번호</span>
                  <span className="font-semibold text-foreground">{resultData.cardNumber}</span>
                </div>
              </div>
            )}
            <Button onClick={handleRedirect} className="mt-8 w-full font-semibold">
              마이페이지로 돌아가기
            </Button>
          </div>
        )}

        {/* 💡 4. 타임아웃 에러 전용 UI 화면 분기 (구출 기회 제공) */}
        {status === "timeout_error" && (
          <div className="flex flex-col items-center justify-center py-6 text-center">
            <Loader2 className="size-14 text-amber-500 animate-pulse mb-4" />
            <h2 className="text-2xl font-bold text-amber-600">연결 지연 발생</h2>
            <p className="mt-2 text-sm text-muted-foreground px-2">
              {errorMessage}
            </p>
            <div className="mt-8 flex w-full flex-col gap-3">
              <Button 
                onClick={handleRetryBilling} 
                className="w-full font-semibold bg-amber-600 hover:bg-amber-700 text-white gap-2"
              >
                <RefreshCw className="size-4" />
                안전하게 다시 연결 시도
              </Button>
              <Button 
                variant="outline"
                onClick={handleRedirect} 
                className="w-full font-semibold"
              >
                나중에 확인하기 (마이페이지 이동)
              </Button>
            </div>
          </div>
        )}

        {status === "error" && (
          <div className="flex flex-col items-center justify-center py-6 text-center">
            <AlertTriangle className="size-14 text-destructive mb-4" />
            <h2 className="text-2xl font-bold text-destructive">결제 등록 실패</h2>
            <p className="mt-2 text-sm text-muted-foreground">
              {errorMessage}
            </p>
            <div className="mt-8 flex w-full gap-3">
              <Button variant="outline" onClick={handleRedirect} className="flex-1 font-semibold">
                돌아가기
              </Button>
              <Button onClick={() => router.push("/")} className="flex-1 font-semibold">
                홈으로 가기
              </Button>
            </div>
          </div>
        )}
      </Card>
    </div>
  )
}

export default function SuccessPage() {
  return (
    <Suspense fallback={
      <div className="flex min-h-screen items-center justify-center bg-secondary/10 p-4">
        <Card className="w-full max-w-md p-6 shadow-xl bg-card flex flex-col items-center justify-center py-10 text-center gap-4">
          <Loader2 className="size-12 animate-spin text-primary" />
          <h2 className="text-xl font-bold">결제 정보 읽는 중...</h2>
        </Card>
      </div>
    }>
      <SuccessContent />
    </Suspense>
  )
}