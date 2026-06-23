"use client"

import { useEffect, useState, useRef, Suspense } from "react"
import { useSearchParams, useRouter } from "next/navigation"
import { CheckCircle2, Loader2, AlertTriangle } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"

type BillingIssueResponse = {
  billingInfoId: number
  cardCompany: string
  cardNumber: string
}

type ApiResponse<T> = {
  success: boolean
  data: T | null
  code: string | null
  message: string | null
}

function SuccessContent() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const requestSent = useRef(false)

  const [status, setStatus] = useState<"loading" | "success" | "error">("loading")
  const [resultData, setResultData] = useState<BillingIssueResponse | null>(null)
  const [errorMessage, setErrorMessage] = useState<string>("")

  const authKey = searchParams.get("authKey")
  const customerKey = searchParams.get("customerKey")
  const plan = searchParams.get("plan")
  
  // 1. 어느 페이지에서 진입했는지 쿼리 스트링 값을 명확히 읽어옵니다.
  const fromPage = searchParams.get("from")

  useEffect(() => {
    if (!authKey || !customerKey) {
      setStatus("error")
      setErrorMessage("결제 인증 정보가 누락되었습니다.")
      return
    }

    if (requestSent.current) return
    requestSent.current = true

    const issueBillingKey = async () => {
      try {
        const response = await fetch("/api/v1/billing/issue", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            authKey: authKey,
            customerKey: customerKey,
          }),
        })

        const result: ApiResponse<BillingIssueResponse> = await response.json()

        if (response.ok && result.success && result.data) {
          setResultData(result.data)
          setStatus("success")
        } else {
          setStatus("error")
          setErrorMessage(result.message || "빌링키 생성 중 서버 오류가 발생했습니다.")
        }
      } catch (error) {
        console.error("백엔드 통신 에러:", error)
        setStatus("error")
        setErrorMessage("서버와 통신에 실패했습니다.")
      }
    }

    void issueBillingKey()
  }, [authKey, customerKey, plan])

  // 2. 유입된 위치에 따라 동적으로 리다이렉트 주소를 계산하는 함수
  const handleRedirect = () => {
    if (fromPage === "mypage") {
      // 마이페이지에서 왔다면 마이페이지 메인으로 복귀시킵니다.
      // (MyPage 컴포넌트가 마운트될 때 결제 관리 탭을 열고 싶다면 가이드 메시지를 인지하게 하거나 유연하게 활용 가능합니다)
      router.push("/mypage")
    } else {
      // 요금제 페이지 등 기타 영역에서 왔다면 이전 요금제 리스트로 이동시킵니다.
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
              토스페이먼츠 인증을 확인하고 안전하게 빌링 정보를 생성하고 있습니다. 잠시만 기다려주세요.
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

            {/* 성공 시 유입 경로에 맞춤화된 동적 경로 이동 */}
            <Button onClick={handleRedirect} className="mt-8 w-full font-semibold">
              {fromPage === "mypage" ? "마이페이지로 돌아가기" : "구독 정보 확인하기"}
            </Button>
          </div>
        )}

        {status === "error" && (
          <div className="flex flex-col items-center justify-center py-6 text-center">
            <AlertTriangle className="size-14 text-destructive mb-4" />
            <h2 className="text-2xl font-bold text-destructive">결제 등록 실패</h2>
            <p className="mt-2 text-sm text-muted-foreground">
              {errorMessage || "알 수 없는 이유로 등록에 실패했습니다."}
            </p>

            <div className="mt-8 flex w-full gap-3">
              {/* 실패 시 다시 시도하는 버튼의 대상 경로 동적 매핑 */}
              <Button 
                variant="outline" 
                onClick={handleRedirect} 
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