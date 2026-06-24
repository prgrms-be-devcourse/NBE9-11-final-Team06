"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import Link from "next/link"
import { toast } from "sonner"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Check, CreditCard, Loader2, Sparkles, AlertCircle, X, ChevronLeft, ChevronRight } from "lucide-react"
import { useAuth } from "@/hooks/use-auth"
import { useSubscription } from "@/hooks/use-subscription"
import { billingApi, type BillingCardResponse } from "@/lib/billing-api"
import { subscriptionApi, type PlanResponse } from "@/lib/subscription-api"

export default function PricingPage() {
  const router = useRouter()
  const { isLoggedIn, isAuthLoading } = useAuth()
  const { subscription, isSubLoading, isSubscribed, refetchSubscription } = useSubscription(isLoggedIn)

  const [plans, setPlans] = useState<PlanResponse[]>([])
  const [billingCards, setBillingCards] = useState<BillingCardResponse[]>([])
  const [isDataLoading, setIsDataLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState<number | null>(null)

  // 커스텀 팝업 및 슬라이드 상태 제어
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [activePlan, setActivePlan] = useState<PlanResponse | null>(null)
  const [currentSlideIndex, setCurrentSlideIndex] = useState(0)

  // 기본 데이터 로드
  useEffect(() => {
    let ignore = false

    async function loadPricingData() {
      try {
        const planResponse = await subscriptionApi.getPlans()
        if (ignore) return

        if (planResponse.success && planResponse.data) {
          setPlans(planResponse.data)
        }

        if (isLoggedIn) {
          const cardResponse = await billingApi.getBillingKeys()
          if (!ignore && cardResponse.success && cardResponse.data) {
            setBillingCards(cardResponse.data)
          }
        }
      } catch (error) {
        console.error("요금제 정보 로드 실패:", error)
        toast.error("요금제 정보를 불러오지 못했습니다.")
      } finally {
        if (!ignore) setIsDataLoading(false)
      }
    }

    loadPricingData()
    return () => { ignore = true }
  }, [isLoggedIn])

  // [구독 시작하기] 버튼 클릭 핸들러
  const handleOpenPaymentModal = (plan: PlanResponse) => {
    if (!isLoggedIn) {
      toast.error("로그인이 필요한 서비스입니다.")
      router.push("/login")
      return
    }

    if (billingCards.length === 0) {
      toast.error("등록된 결제 수단이 없습니다. 마이페이지에서 카드를 먼저 등록해 주세요.")
      return
    }

    setActivePlan(plan)
    setCurrentSlideIndex(0) 
    setIsModalOpen(true)
  }

  // 슬라이드 제어 핸들러
  const handlePrevSlide = () => {
    setCurrentSlideIndex((prev) => (prev === 0 ? billingCards.length - 1 : prev - 1))
  }

  const handleNextSlide = () => {
    setCurrentSlideIndex((prev) => (prev === billingCards.length - 1 ? 0 : prev + 1))
  }

  // 최종 결제 승인
  const handleConfirmPayment = async () => {
    if (!activePlan || billingCards.length === 0) return

    const targetCard = billingCards[currentSlideIndex]
    if (!targetCard) {
      toast.error("선택한 카드 정보를 식별할 수 없습니다.")
      return
    }

    const confirmed = window.confirm(
      `선택하신 [${targetCard.cardCompany}] 카드로 정기 구독을 바로 시작하시겠습니까?`
    )
    if (!confirmed) return

    setIsSubmitting(activePlan.id)
    setIsModalOpen(false) 

    // 결제 직전 고유한 멱등키 UUID 생성
    const clientIdempotencyKey = `SUB_REQ_${crypto.randomUUID().replace(/-/g, "")}`

    try {
      const response = await subscriptionApi.startSubscription({
        billingInfoId: targetCard.id,
        planId: activePlan.id,
      }, 
      clientIdempotencyKey)
    

      if (response.success) {
        toast.success(response.message ?? "정기 구독 멤버십 가입이 성공적으로 완료되었습니다!")
        await refetchSubscription()
        router.push("/mypage")
      } else {
        toast.error(response.message ?? "구독 승인 처리 중 에러가 발생했습니다.")
      }
    } catch (error) {
      toast.error("서버와 통신 중 정기 결제 승인 오류가 발생했습니다.")
    } finally {
      setIsSubmitting(null)
      setActivePlan(null)
    }
  }

  if (isAuthLoading || isSubLoading || isDataLoading) {
    return (
      <div className="flex min-h-screen flex-col bg-background">
        <SiteHeader />
        <main className="flex flex-1 items-center justify-center">
          <Loader2 className="size-8 animate-spin text-primary" />
        </main>
        <SiteFooter />
      </div>
    )
  }

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <SiteHeader />
      
      {/* 🌟 수정 포인트 1: 전체 패딩 상단 간격을 늘려 상단 바(Header)와 타이틀 텍스트 사이 규격 조정 */}
      <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-24 text-center">
        <div className="mb-16">
          <h1 className="font-heading text-3xl font-bold tracking-tight sm:text-4xl">오늘 어디가? 멤버십 요금제</h1>
          <p className="mt-4 text-muted-foreground">합리적인 비용으로 특별한 코스 추천과 전용 혜택을 누려보세요.</p>
        </div>

        {/* 🌟 수정 포인트 2: pt-12를 주어 요금제 그리드 카드 윗공간 여백을 완벽하게 확보, 배지가 올라와도 짤릴 공간을 없앰 */}
        <div className="grid gap-10 sm:grid-cols-2 max-w-3xl mx-auto pt-12">
          {plans.map((plan) => {
            const isCurrentPlan = subscription?.planName === plan.displayName && isSubscribed

            return (
              /* 🌟 수정 포인트 3: 복잡한 내부 overflow 속성이 짤림을 만들지 못하도록 isolate와 overflow-visible을 명시적으로 부여 */
              <Card 
                key={plan.id} 
                className={`relative flex flex-col justify-between border-border/60 text-left transition-all overflow-visible isolate ${
                  isCurrentPlan ? 'border-primary ring-2 ring-primary/20' : ''
                }`}
              >
                {plan.name.includes("PREMIUM") && (
                  /* 🌟 수정 포인트 4: -top-5 위치 조정 및 z-50 레이어 우선순위를 높여 그 어떤 요소보다 위에 띄워 온전하게 노출 */
                  <div className="absolute -top-5 left-1/2 -translate-x-1/2 z-50 whitespace-nowrap rounded-full bg-primary px-3.5 py-1 text-xs font-semibold text-primary-foreground flex items-center gap-1 shadow-md">
                    <Sparkles className="size-3" /> 인기 멤버십
                  </div>
                )}
                
                <div>
                  <CardHeader className="pt-8"> {/* 패딩 상단 미세 조정 */}
                    <CardTitle className="text-xl">{plan.displayName}</CardTitle>
                    <CardDescription>{plan.name.includes("PREMIUM") ? "고급 정밀 맞춤 추천 및 한정 혜택 제공" : "가성비 핵심 추천 코스 올인원 패키지"}</CardDescription>
                    <div className="mt-4 flex items-baseline gap-1">
                      <span className="text-3xl font-bold tracking-tight text-foreground">₩{plan.amount.toLocaleString()}</span>
                      <span className="text-sm font-normal text-muted-foreground">/ 월</span>
                    </div>
                  </CardHeader>
                  
                  <CardContent className="grid gap-3 text-sm text-muted-foreground pb-6">
                    <div className="flex items-center gap-2">
                      <Check className="size-4 text-primary" /> <span>인공지능 기반 맞춤 스케줄러 무제한</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Check className="size-4 text-primary" /> <span>프리미엄 한정 제휴 행사 할인권 제공</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Check className="size-4 text-primary text-opacity-75" /> <span>실시간 혼잡도 회피 경로 매핑 지원</span>
                    </div>
                  </CardContent>
                </div>

                <div className="w-full">
                  <CardFooter className="pt-4 border-t border-border/40">
                    {!isLoggedIn ? (
                      <Button render={<Link href="/login" />} className="w-full">
                        로그인 후 구독하기
                      </Button>
                    ) : isSubscribed ? (
                      <Button disabled className="w-full bg-muted text-muted-foreground">
                        {isCurrentPlan ? "현재 이용 중인 멤버십" : "이미 다른 플랜 구독 중"}
                      </Button>
                    ) : (
                      <Button
                        onClick={() => handleOpenPaymentModal(plan)}
                        disabled={isSubmitting !== null}
                        className="w-full gap-2 font-semibold"
                      >
                        {isSubmitting === plan.id ? (
                          <>
                            <Loader2 className="size-4 animate-spin" />
                            멤버십 가입 처리 중...
                          </>
                        ) : (
                          <>
                            <CreditCard className="size-4" />
                            구독 시작하기
                          </>
                        )}
                      </Button>
                    )}
                  </CardFooter>
                </div>
              </Card>
            )
          })}
        </div>
      </main>

      {/* 수제 커스텀 결제 슬라이드 팝업 화면 (동일하게 유지) */}
      {isModalOpen && activePlan && billingCards.length > 0 && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
          <div className="relative w-full max-w-md rounded-2xl border border-border/60 p-6 shadow-2xl text-left flex flex-col gap-5 bg-white">
            <button 
              onClick={() => setIsModalOpen(false)}
              className="absolute right-4 top-4 rounded-md p-1 opacity-70 hover:opacity-100 transition-colors"
            >
              <X className="size-4 text-foreground" />
            </button>

            <div className="text-center space-y-1">
              <h3 className="font-heading text-lg font-bold text-foreground">결제 카드 선택</h3>
              <p className="text-xs text-muted-foreground">좌우 버튼을 눌러 정기 결제에 사용할 카드를 지정해 주세요.</p>
            </div>

            <div className="relative w-full flex items-center justify-between px-2 py-3">
              {billingCards.length > 1 && (
                <button
                  onClick={handlePrevSlide}
                  className="absolute left-0 z-30 size-8 rounded-full border border-border/80 bg-background flex items-center justify-center shadow-sm hover:bg-secondary text-foreground"
                >
                  <ChevronLeft className="size-4" />
                </button>
              )}

              <div className="w-full max-w-[280px] mx-auto overflow-hidden">
                <Card className="bg-gradient-to-br from-slate-900 to-slate-800 text-white p-5 border-0 shadow-md h-36 flex flex-col justify-between">
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-xs tracking-wide text-slate-200">
                      {billingCards[currentSlideIndex].cardCompany}
                    </span>
                    <CreditCard className="size-4 text-slate-400" />
                  </div>
                  <p className="text-base font-mono tracking-widest text-slate-100 text-center my-2">
                    {billingCards[currentSlideIndex].cardNumber}
                  </p>
                  <div className="text-right">
                    <span className="text-[10px] text-slate-400 font-medium">연동 카드의 {currentSlideIndex + 1}번째 슬롯</span>
                  </div>
                </Card>
              </div>

              {billingCards.length > 1 && (
                <button
                  onClick={handleNextSlide}
                  className="absolute right-0 z-30 size-8 rounded-full border border-border/80 bg-background flex items-center justify-center shadow-sm hover:bg-secondary text-foreground"
                >
                  <ChevronRight className="size-4" />
                </button>
              )}
            </div>

            {billingCards.length > 1 && (
              <div className="flex gap-1.5 justify-center -mt-2">
                {billingCards.map((_, idx) => (
                  <button
                    key={idx}
                    onClick={() => setCurrentSlideIndex(idx)}
                    className={`size-1.5 rounded-full transition-all ${
                      idx === currentSlideIndex ? "bg-primary w-3.5" : "bg-muted"
                    }`}
                  />
                ))}
              </div>
            )}

            <div className="w-full rounded-xl bg-secondary/40 p-4 text-xs space-y-2 border border-border/40">
              <div className="flex justify-between">
                <span className="text-muted-foreground">선택 요금제</span>
                <span className="font-bold text-foreground">{activePlan.displayName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">결제 예정 금액</span>
                <span className="font-bold text-primary">₩{activePlan.amount.toLocaleString()} / 월</span>
              </div>
              <div className="flex justify-between items-center pt-1 border-t border-border/30 text-[11px] text-muted-foreground">
                <span className="flex items-center gap-1">
                  <AlertCircle className="size-3 text-primary" /> 매달 자동 정산되는 정기 결제 플랜입니다.
                </span>
              </div>
            </div>

            <Button onClick={handleConfirmPayment} className="w-full font-semibold">
              선택한 카드로 정기 결제 승인하기
            </Button>
          </div>
        </div>
      )}

      <SiteFooter />
    </div>
  )
}