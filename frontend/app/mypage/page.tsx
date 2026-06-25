"use client"

import type React from "react"
import { useCallback, useEffect, useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import Script from "next/script" 
import { toast } from "sonner"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { CategoryMultiSelect } from "@/components/category-multi-select"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Separator } from "@/components/ui/separator"
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { memberApi } from "@/lib/member-api"
import { preferenceApi } from "@/lib/preference-api"
import { courseBookmarkApi } from "@/lib/course-bookmark-api"
import { billingApi, type BillingCardResponse } from "@/lib/billing-api"
import { PaymentHistoryList } from "@/components/billing/payment-history-list"

import { subscriptionApi } from "@/lib/subscription-api"
import { useSubscription } from "@/hooks/use-subscription"
import { Calendar, ShieldCheck, XCircle } from "lucide-react"
import { useAuth } from "@/hooks/use-auth" 

import type {
  CompanionType,
  Member,
  MobilityLevel,
  SavedCourseResponse,
  UserPreference,
} from "@/lib/types"
import { MapPin, Clock, Route, Heart, Settings, Bookmark, CreditCard, Plus, Trash2,Loader2 } from "lucide-react"

declare global {
  interface Window {
    TossPayments: any
  }
}

const CLIENT_KEY = process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY;

const COMPANION_OPTIONS: { value: CompanionType; label: string }[] = [
  { value: "SOLO", label: "혼자" },
  { value: "COUPLE", label: "커플" },
  { value: "FRIEND", label: "친구" },
  { value: "FAMILY", label: "가족" },
  { value: "PARENT", label: "부모님" },
]

const MOBILITY_OPTIONS: { value: MobilityLevel; label: string }[] = [
  { value: "LOW", label: "낮음" },
  { value: "NORMAL", label: "보통" },
  { value: "HIGH", label: "높음" },
]

function formatDate(date?: string | null) {
  if (!date) {
    return "날짜 미정"
  }

  return new Date(date).toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "short",
    day: "numeric",
  })
}

export default function MyPage() {
  const router = useRouter()

  const { isLoggedIn } = useAuth() // 이미 구현된 훅 주입
  const { subscription, isSubLoading, isSubscribed, refetchSubscription } = useSubscription(isLoggedIn)
  const [isCanceling, setIsCanceling] = useState(false)

  const [activeTab, setActiveTab] = useState("saved")
  const [member, setMember] = useState<Member | null>(null)
  const [preference, setPreference] = useState<UserPreference | null>(null)

  const [savedCourses, setSavedCourses] = useState<SavedCourseResponse[]>([])
  const [savedCoursePage, setSavedCoursePage] = useState(0)
  const [totalSavedCoursePages, setTotalSavedCoursePages] = useState(0)
  const [totalSavedCourseCount, setTotalSavedCourseCount] = useState(0)

  // 결제 수단 관련 상태 관리
  const [billingCards, setBillingCards] = useState<BillingCardResponse[]>([])
  const [isBillingLoading, setIsBillingLoading] = useState(false)
  const [deletingCardId, setDeletingCardId] = useState<number | null>(null)
  const [isRegistering, setIsRegistering] = useState(false)

  const [histories, setHistories] = useState<PaymentHistoryResponse[]>([])
  const [isHistoriesLoading, setIsHistoriesLoading] = useState(false)

  const [nickname, setNickname] = useState("")
  const [profileImageUrl, setProfileImageUrl] = useState("")

  const [preferredArea, setPreferredArea] = useState("")
  const [selectedCategoryIds, setSelectedCategoryIds] = useState<number[]>([])
  const [companionType, setCompanionType] = useState<CompanionType | null>(null)
  const [mobilityLevel, setMobilityLevel] = useState<MobilityLevel | null>(null)
  const [avoidCrowded, setAvoidCrowded] = useState<boolean | null>(null)

  const [isLoading, setIsLoading] = useState(true)
  const [isSavedCoursesLoading, setIsSavedCoursesLoading] = useState(false)
  const [isUpdating, setIsUpdating] = useState(false)
  const [isWithdrawing, setIsWithdrawing] = useState(false)
  const [isPreferenceSaving, setIsPreferenceSaving] = useState(false)
  const [isPreferenceDeleting, setIsPreferenceDeleting] = useState(false)

  const fetchPaymentHistories = useCallback(async () => {
    setIsHistoriesLoading(true)
    try {
      const response = await billingApi.getMyPaymentHistories()
      if (response.success && response.data) {
        setHistories(response.data)
      }
    } catch (error) {
      console.error("결제 내역 조회 실패:", error)
    } finally {
      setIsHistoriesLoading(false)
    }
  }, [])

  const fetchSavedCourses = useCallback(
    async (page = 0, showErrorToast = false) => {
      setIsSavedCoursesLoading(true)

      try {
        const response = await courseBookmarkApi.getSavedCourses(page)

        if (!response.success || !response.data) {
          if (page === 0) {
            setSavedCourses([])
            setSavedCoursePage(0)
            setTotalSavedCoursePages(0)
            setTotalSavedCourseCount(0)
          }

          if (showErrorToast) {
            toast.error(response.message ?? "북마크한 코스를 불러오지 못했습니다.")
          }

          console.warn("북마크한 코스 조회 실패:", response)
          return
        }

        setSavedCourses(response.data.content)
        setSavedCoursePage(response.data.page)
        setTotalSavedCoursePages(response.data.totalPages)
        setTotalSavedCourseCount(response.data.totalElements)
      } catch (error) {
        if (page === 0) {
          setSavedCourses([])
          setSavedCoursePage(0)
          setTotalSavedCoursePages(0)
          setTotalSavedCourseCount(0)
        }

        if (showErrorToast) {
          toast.error("북마크한 코스를 불러오는 중 오류가 발생했습니다.")
        }

        console.warn("북마크한 코스 조회 중 오류:", error)
      } finally {
        setIsSavedCoursesLoading(false)
      }
    },
    [],
  )

  // 등록된 카드 목록 호출 함수
  const fetchBillingCards = useCallback(async () => {
    setIsBillingLoading(true)
    try {
      const response = await billingApi.getBillingKeys()
      if (response.success && response.data) {
        setBillingCards(response.data)
      } else {
        console.warn("카드 목록 조회 실패:", response.message)
      }
    } catch (error) {
      console.error("카드 목록 조회 중 에러:", error)
    } finally {
      setIsBillingLoading(false)
    }
  }, [])

  const handleCancelSubscription = async () => {
    if (!subscription) return

    const confirmed = window.confirm(
      `정말 ${subscription.planName} 해지를 진행하시겠습니까?\n해지 완료 후 다음 결제일(${subscription.nextBillingDate})부터 서비스 정기 결제가 안전하게 중단됩니다.`
    )
    if (!confirmed) return

    setIsCanceling(true)
    try {
      const response = await subscriptionApi.cancelSubscription(subscription.subscriptionId)
      if (response.success) {
        toast.success("정기 구독 해지 처리가 정상적으로 접수되었습니다.")
        
        await refetchSubscription()
        router.refresh()
      } else {
        toast.error(response.message ?? "구독 해지 중 오류가 발생했습니다.")
      }
    } catch (error) {
      toast.error("서버 내부 통신 실패로 구독 취소를 처리하지 못했습니다.")
    } finally {
      setIsCanceling(false)
    }
  }


  // 회원 정보 조회 및 선호 정보, 북마크 코스, 결제 수단 초기 데이터 로드
  useEffect(() => {
    let ignore = false

    async function fetchInitialData() {
      try {
        const memberResponse = await memberApi.getMyInfo()

        if (ignore) {
          return
        }

        if (!memberResponse.success || !memberResponse.data) {
          toast.error(memberResponse.message ?? "로그인이 필요합니다.")
          router.push("/login")
          return
        }

        setMember(memberResponse.data)
        setNickname(memberResponse.data.nickname)
        setProfileImageUrl(memberResponse.data.profileImageUrl ?? "")

        const preferenceResponse = await preferenceApi.getMyPreference()

        if (ignore) {
          return
        }

        if (preferenceResponse.success && preferenceResponse.data) {
          applyPreference(preferenceResponse.data)
        } else if (
          !preferenceResponse.success &&
          preferenceResponse.code !== "PREFERENCE_NOT_FOUND"
        ) {
          toast.error(
            preferenceResponse.message ??
              "선호 정보를 불러오는 중 오류가 발생했습니다.",
          )
        }

        await fetchSavedCourses(0)
        await fetchBillingCards()
        await fetchPaymentHistories()
      } catch {
        if (!ignore) {
          toast.error("회원 정보를 불러오는 중 오류가 발생했습니다.")
          router.push("/login")
        }
      } finally {
        if (!ignore) {
          setIsLoading(false)
        }
      }
    }

    fetchInitialData()

    return () => {
      ignore = true
    }
  }, [router, fetchSavedCourses, fetchBillingCards])

  
  const handleRegisterCard = async () => {
    if (!member) return

    if (!window.TossPayments) {
      toast.error("토스페이먼츠 라이브러리가 아직 로드되지 않았습니다. 잠시 후 다시 시도해주세요.")
      return
    }

    try {
      setIsRegistering(true)
      
      const tossPayments = window.TossPayments(CLIENT_KEY)
      
      // 사용자 고유 키 매핑 (기존처럼 랜덤으로 하거나 회원 고유식별값 조합 가능)
      const customerKey = "USER_" + member.id + "_" + Math.random().toString(36).substring(2, 7)
      const payment = tossPayments.payment({ customerKey })

      // 빌링키 인증 요청 (v2 규격 반영)
      await payment.requestBillingAuth({
        method: "CARD",
        successUrl: `${window.location.origin}/success?customerKey=${customerKey}&from=mypage`, 
        failUrl: `${window.location.origin}/fail?from=mypage`,
        customerEmail: member.email || "customer@example.com",
        customerName: member.nickname || "고객",
      })
    } catch (error) {
      console.error("토스페이먼츠 인증 창 호출 실패:", error)
      toast.error("결제창을 여는 중 오류가 발생했습니다.")
    } finally {
      setIsRegistering(false)
    }
  }

  // 카드 해지 및 삭제 핸들러
  const handleDeleteCard = async (cardId: number) => {
    const confirmed = window.confirm("등록된 카드를 삭제하시겠습니까?\n삭제 후 정기 결제 서비스 이용 시 재등록이 필요합니다.")
    if (!confirmed) return

    setDeletingCardId(cardId)
    try {
      const response = await billingApi.deleteBillingKey(cardId)
      if (response.success) {
        toast.success("카드가 안전하게 삭제되었습니다.")
        await fetchBillingCards()
      } else {
        toast.error(response.message ?? "카드 삭제에 실패했습니다.")
      }
    } catch (error) {
      toast.error("카드 삭제 중 시스템 오류가 발생했습니다.")
    } finally {
      setDeletingCardId(null)
    }
  }

  function applyPreference(nextPreference: UserPreference) {
    setPreference(nextPreference)
    setPreferredArea(nextPreference.preferredArea)
    setSelectedCategoryIds(
      nextPreference.categories.map((category) => category.id),
    )
    setCompanionType(nextPreference.companionType)
    setMobilityLevel(nextPreference.mobilityLevel)
    setAvoidCrowded(nextPreference.avoidCrowded)
  }

  function resetPreferenceForm() {
    setPreference(null)
    setPreferredArea("")
    setSelectedCategoryIds([])
    setCompanionType(null)
    setMobilityLevel(null)
    setAvoidCrowded(null)
  }

  async function handleUpdate(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()

    if (!nickname.trim()) {
      toast.error("닉네임을 입력해주세요.")
      return
    }

    setIsUpdating(true)

    try {
      const response = await memberApi.updateMyInfo({
        nickname: nickname.trim(),
        profileImageUrl: profileImageUrl.trim() || undefined,
      })

      if (!response.success || !response.data) {
        toast.error(response.message ?? "회원 정보 수정에 실패했습니다.")
        return
      }

      setMember(response.data)
      setNickname(response.data.nickname)
      setProfileImageUrl(response.data.profileImageUrl ?? "")

      toast.success("회원 정보가 수정되었습니다.")
      router.push("/")
      router.refresh()
    } catch {
      toast.error("회원 정보 수정 중 오류가 발생했습니다.")
    } finally {
      setIsUpdating(false)
    }
  }

  async function handleSavePreference(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()

    if (!preferredArea.trim()) {
      toast.error("선호 지역을 입력해주세요.")
      return
    }

    if (selectedCategoryIds.length === 0) {
      toast.error("선호 카테고리를 1개 이상 선택해주세요.")
      return
    }

    if (!companionType) {
      toast.error("동행 유형을 선택해주세요.")
      return
    }

    if (!mobilityLevel) {
      toast.error("이동 강도를 선택해주세요.")
      return
    }

    if (avoidCrowded === null) {
      toast.error("혼잡도 선호를 선택해주세요.")
      return
    }

    setIsPreferenceSaving(true)

    try {
      const request = {
        preferredArea: preferredArea.trim(),
        categoryIds: selectedCategoryIds,
        companionType,
        mobilityLevel,
        avoidCrowded,
      }

      const response = preference
        ? await preferenceApi.updateMyPreference(request)
        : await preferenceApi.createMyPreference(request)

      if (!response.success || !response.data) {
        toast.error(response.message ?? "선호 정보 저장에 실패했습니다.")
        return
      }

      applyPreference(response.data)
      toast.success(
        preference ? "선호 정보가 수정되었습니다." : "선호 정보가 등록되었습니다.",
      )
    } catch {
      toast.error("선호 정보 저장 중 오류가 발생했습니다.")
    } finally {
      setIsPreferenceSaving(false)
    }
  }

  async function handleDeletePreference() {
    const confirmed = window.confirm("저장된 선호 정보를 삭제하시겠습니까?")

    if (!confirmed) {
      return
    }

    setIsPreferenceDeleting(true)

    try {
      const response = await preferenceApi.deleteMyPreference()

      if (!response.success) {
        toast.error(response.message ?? "선호 정보 삭제에 실패했습니다.")
        return
      }

      resetPreferenceForm()
      toast.success("선호 정보가 삭제되었습니다.")
    } catch {
      toast.error("선호 정보 삭제 중 오류가 발생했습니다.")
    } finally {
      setIsPreferenceDeleting(false)
    }
  }

  async function handleWithdraw() {
    const confirmed = window.confirm(
      "정말 탈퇴하시겠습니까?\n탈퇴 후 현재 계정으로 다시 로그인할 수 없습니다.",
    )

    if (!confirmed) {
      return
    }

    setIsWithdrawing(true)

    try {
      const response = await memberApi.withdrawMyAccount()

      if (!response.success) {
        toast.error(response.message ?? "회원 탈퇴에 실패했습니다.")
        return
      }

      await memberApi.logout()

      toast.success("회원 탈퇴가 완료되었습니다.")
      router.push("/")
      router.refresh()
    } catch {
      toast.error("회원 탈퇴 중 오류가 발생했습니다.")
    } finally {
      setIsWithdrawing(false)
    }
  }

  function getAvatarText() {
    if (!member?.nickname) {
      return "여행"
    }

    return member.nickname.slice(0, 2)
  }

  if (isLoading || isSubLoading) {
    return (
      <div className="flex min-h-screen flex-col bg-background">
        <SiteHeader />
        <main className="mx-auto flex w-full max-w-5xl flex-1 items-center justify-center px-4 py-10">
          <Loader2 className="size-6 animate-spin text-primary" />
          <p className="ml-2 text-sm text-muted-foreground">마이페이지 데이터를 동기화 중...</p>
        </main>
        <SiteFooter />
      </div>
    )
  }

  if (!member) {
    return null
  }

  return (
    <div className="flex min-h-screen flex-col bg-background">
      {/* 👈 5. 토스페이먼츠 SDK v2 스크립트 비동기 로더 심어두기 */}
      <Script 
        src="https://js.tosspayments.com/v2/standard" 
        strategy="afterInteractive"
      />

      <SiteHeader />

      <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-10">
        <div className="flex flex-col gap-5 rounded-3xl border border-border/60 bg-card p-6 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-4">
            <Avatar className="size-16 overflow-hidden">
              {member.profileImageUrl ? (
                <img
                  src={member.profileImageUrl}
                  alt={`${member.nickname} 프로필 이미지`}
                  className="size-full object-cover"
                />
              ) : (
                <AvatarFallback className="bg-primary text-lg text-primary-foreground">
                  {getAvatarText()}
                </AvatarFallback>
              )}
            </Avatar>

            <div>
              <h1 className="font-heading text-xl font-bold">{member.nickname}님</h1>
              <p className="text-sm text-muted-foreground">{member.email}</p>
              <div className="mt-2 flex flex-wrap gap-1.5">
                <Badge variant="secondary" className="gap-1">
                  <Heart className="size-3" /> {totalSavedCourseCount}개 코스 북마크
                </Badge>
              </div>
            </div>
          </div>

          <Button
            type="button"
            variant="outline"
            className="gap-2 bg-transparent"
            onClick={() => setActiveTab("prefs")}
          >
            <Settings className="size-4" /> 프로필 설정
          </Button>
        </div>

        <Tabs value={activeTab} onValueChange={setActiveTab} className="mt-8">
          <TabsList>
            <TabsTrigger value="saved" className="gap-1.5">
              <Bookmark className="size-4" /> 저장한 코스
            </TabsTrigger>
            <TabsTrigger value="prefs" className="gap-1.5">
              <Settings className="size-4" /> 선호 정보
            </TabsTrigger>
            <TabsTrigger value="billing" className="gap-1.5">
              <CreditCard className="size-4" /> 결제 관리
            </TabsTrigger>
            <TabsTrigger value="subscription" className="gap-1.5">
              <ShieldCheck className="size-4" /> 멤버십 관리
            </TabsTrigger>
            <TabsTrigger value="history" className="gap-1.5">
              <CreditCard className="size-4" /> 결제 내역
            </TabsTrigger>
          </TabsList>

          <TabsContent value="saved" className="mt-6">
            {isSavedCoursesLoading ? (
              <Card className="border-border/60 p-8 text-center text-sm text-muted-foreground">
                북마크한 코스를 불러오는 중입니다.
              </Card>
            ) : savedCourses.length === 0 ? (
              <Card className="border-border/60 p-8 text-center">
                <p className="font-medium">아직 북마크한 코스가 없습니다.</p>
                <p className="mt-2 text-sm text-muted-foreground">
                  마음에 드는 코스를 북마크하면 이곳에서 다시 확인할 수 있어요.
                </p>
                <Button asChild className="mt-5">
                  <Link href="/plan">코스 추천 받기</Link>
                </Button>
              </Card>
            ) : (
              <>
                <div className="grid gap-5 sm:grid-cols-2">
                  {savedCourses.map((course) => (
                    <Link key={course.savedCourseId} href={`/course/${course.courseId}`}>
                      <Card className="group h-full border-border/60 p-5 transition-shadow hover:shadow-md">
                        <div className="flex items-start justify-between gap-3">
                          <div>
                            <Badge variant="secondary">{course.baseArea || "지역 미정"}</Badge>
                            <h3 className="mt-3 font-heading text-lg font-semibold">{course.title}</h3>
                          </div>
                          <Bookmark className="size-5 text-primary" />
                        </div>
                        <div className="mt-4 flex flex-wrap gap-3 text-xs text-muted-foreground">
                          <span className="flex items-center gap-1">
                            <Route className="size-3.5" /> {course.courseType || "코스 유형 미정"}
                          </span>
                          <span className="flex items-center gap-1">
                            <MapPin className="size-3.5" /> {course.baseArea || "지역 미정"}
                          </span>
                          <span className="flex items-center gap-1">
                            <Clock className="size-3.5" /> {formatDate(course.startDate)}
                          </span>
                        </div>
                        <p className="mt-4 text-xs text-muted-foreground">북마크한 날짜: {formatDate(course.savedAt)}</p>
                      </Card>
                    </Link>
                  ))}
                </div>
              </>
            )}
          </TabsContent>

          <TabsContent value="prefs" className="mt-6">
            <Card className="border-border/60">
              <CardHeader>
                <CardTitle className="text-base">나의 선호 정보</CardTitle>
              </CardHeader>
              <CardContent className="flex flex-col gap-6">
                <form onSubmit={handleUpdate} className="flex flex-col gap-4">
                  <div>
                    <p className="mb-3 text-sm font-medium">프로필 정보</p>
                    <div className="grid gap-4 sm:grid-cols-2">
                      <div className="grid gap-2">
                        <Label htmlFor="email">이메일</Label>
                        <Input id="email" value={member.email} disabled />
                      </div>
                      <div className="grid gap-2">
                        <Label htmlFor="nickname">닉네임</Label>
                        <Input
                          id="nickname"
                          value={nickname}
                          onChange={(e) => setNickname(e.target.value)}
                          placeholder="닉네임을 입력하세요"
                          required
                        />
                      </div>
                    </div>
                  </div>
                  <div className="grid gap-2">
                    <Label htmlFor="profileImageUrl">프로필 이미지 URL</Label>
                    <Input
                      id="profileImageUrl"
                      value={profileImageUrl}
                      onChange={(e) => setProfileImageUrl(e.target.value)}
                      placeholder="https://example.com/profile.png"
                    />
                  </div>
                  <Button type="submit" className="w-fit" disabled={isUpdating}>
                    {isUpdating ? "수정 중..." : "프로필 수정"}
                  </Button>
                </form>

                <Separator />

                <form onSubmit={handleSavePreference} className="flex flex-col gap-6">
                  <div>
                    <p className="mb-3 text-sm font-medium">선호 지역</p>
                    <Input value={preferredArea} onChange={(e) => setPreferredArea(e.target.value)} placeholder="예: 홍대, 성수, 강남" />
                  </div>
                  <div>
                    <p className="mb-3 text-sm font-medium">관심 카테고리</p>
                    <CategoryMultiSelect selectedCategoryIds={selectedCategoryIds} onChange={setSelectedCategoryIds} disabled={isPreferenceSaving} />
                  </div>

                  <Separator />

                  <div>
                    <p className="mb-3 text-sm font-medium">주 동행 유형</p>
                    <div className="flex flex-wrap gap-2">
                      {COMPANION_OPTIONS.map((option) => {
                        const active = companionType === option.value
                        return (
                          <button
                            key={option.value}
                            type="button"
                            disabled={isPreferenceSaving}
                            onClick={() => setCompanionType(option.value)}
                            className={`rounded-full border px-3 py-1.5 text-sm ${active ? "border-accent bg-accent text-accent-foreground" : "border-border bg-background text-muted-foreground"}`}
                          >
                            {option.label}
                          </button>
                        )
                      })}
                    </div>
                  </div>

                  <div className="flex flex-wrap gap-2">
                    <Button type="submit" className="w-fit" disabled={isPreferenceSaving}>
                      {isPreferenceSaving ? "저장 중..." : preference ? "선호 정보 수정" : "선호 정보 등록"}
                    </Button>
                  </div>
                </form>
              </CardContent>
            </Card>
          </TabsContent>

          {/* 💳 결제 관리 탭 콘텐츠 파트 */}
          <TabsContent value="billing" className="mt-6">
            <Card className="border-border/60">
              <CardHeader>
                <CardTitle className="text-base">등록된 결제 수단</CardTitle>
              </CardHeader>
              <CardContent>
                {isBillingLoading ? (
                  <p className="text-center text-sm text-muted-foreground py-6">
                    결제 카드 정보를 불러오는 중입니다...
                  </p>
                ) : (
                  <div className="grid gap-5 sm:grid-cols-2">
                    {/* 등록된 카드 리스트 루프 */}
                    {billingCards.map((card) => (
                      <Card key={card.id} className="relative overflow-hidden bg-gradient-to-br from-slate-900 to-slate-800 text-white p-6 border-0 shadow-sm min-h-[160px] flex flex-col justify-between">
                        <div>
                          <div className="flex items-center justify-between">
                            <span className="font-semibold text-sm tracking-wide text-slate-200">
                              {card.cardCompany}
                            </span>
                            <CreditCard className="size-5 text-slate-400" />
                          </div>
                          <p className="mt-4 text-lg font-mono tracking-widest text-slate-100">
                            {card.cardNumber}
                          </p>
                        </div>

                        <div className="flex items-end justify-between mt-4">
                          <span className="text-[10px] text-slate-400">
                            등록일: {formatDate(card.createdAt)}
                          </span>
                          <Button
                            type="button"
                            size="sm"
                            variant="destructive"
                            className="h-8 gap-1 bg-red-600/90 hover:bg-red-600 text-white border-0"
                            disabled={deletingCardId === card.id}
                            onClick={() => handleDeleteCard(card.id)}
                          >
                            <Trash2 className="size-3.5" />
                            {deletingCardId === card.id ? "삭제중" : "삭제"}
                          </Button>
                        </div>
                      </Card>
                    ))}

                    {/* 카드 추가 (+) 디자인 버튼 */}
                    <button
                      type="button"
                      onClick={handleRegisterCard}
                      disabled={isRegistering}
                      className="flex flex-col items-center justify-center gap-2.5 rounded-xl border-2 border-dashed border-border/80 hover:border-primary/50 bg-transparent p-6 min-h-[160px] text-muted-foreground hover:text-primary transition-all group disabled:opacity-50"
                    >
                      <div className="flex size-10 items-center justify-center rounded-full bg-muted group-hover:bg-primary/10 transition-colors">
                        <Plus className="size-5" />
                      </div>
                      <div className="text-center">
                        <p className="text-sm font-medium">
                          {isRegistering ? "인증창 준비 중..." : "새 결제 수단 추가"}
                        </p>
                        <p className="text-xs text-muted-foreground mt-1">정기 구독에 사용할 카드를 등록하세요</p>
                      </div>
                    </button>
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>
          <TabsContent value="subscription" className="mt-6">
            <Card className="border-border/60">
              <CardHeader>
                <CardTitle className="text-base">현재 이용 중인 구독 멤버십</CardTitle>
              </CardHeader>
              <CardContent>
                {/* 기존: isSubscribed && subscription 
                  변경: 결제 취소(CANCELED)가 되었거나 아예 없는 경우를 제외하고 
                        ACTIVE 또는 CANCELED_RESERVED 상태일 때 멤버십 카드 UI를 노출합니다.
                */}
                {subscription && subscription.status !== "CANCELED" ? (
                  <div className="rounded-2xl border border-border/80 p-6 bg-secondary/10 flex flex-col gap-6 sm:flex-row sm:items-center sm:justify-between">
                    <div className="space-y-2">
                      <div className="flex items-center gap-2">
                        <span className="font-heading text-lg font-bold text-foreground">
                          {subscription.planName}
                        </span>
                        
                        {/* 상태에 따른 동적 배지 분기 */}
                        {subscription.status === "CANCELED_RESERVED" ? (
                          <Badge className="bg-yellow-500/10 text-yellow-600 border-0 hover:bg-yellow-500/10">
                            해지 예약됨
                          </Badge>
                        ) : (
                          <Badge className="bg-green-500/10 text-green-600 border-0 hover:bg-green-500/10">
                            이용 중
                          </Badge>
                        )}
                      </div>

                      {/* 상태에 따른 텍스트 설명 분기 */}
                      <p className="text-sm text-muted-foreground">
                        {subscription.status === "CANCELED_RESERVED" ? (
                          <span className="text-amber-600 font-medium">
                            구독 해지 신청이 완료되었습니다. 남은 기간 동안 서비스를 정상적으로 이용하실 수 있습니다.
                          </span>
                        ) : (
                          <>
                            매달 정기 정산 금액:{" "}
                            <span className="font-semibold text-foreground">
                              ₩{subscription.amount.toLocaleString()}
                            </span>
                          </>
                        )}
                      </p>
                      
                      <div className="flex items-center gap-1.5 text-xs text-muted-foreground pt-1">
                        <Calendar className="size-3.5" />
                        <span>
                          {subscription.status === "CANCELED_RESERVED" ? (
                            <>
                              멤버십 서비스 최종 종료일:{" "}
                              <strong className="text-foreground">{subscription.nextBillingDate}</strong>
                            </>
                          ) : (
                            <>
                              다음 자동 정기 결제 예정일:{" "}
                              <strong className="text-foreground">{subscription.nextBillingDate}</strong>
                            </>
                          )}
                        </span>
                      </div>
                    </div>

                    {/* ★ 핵심 비즈니스 연동: 
                      이미 해지 예약(CANCELED_RESERVED)된 상태라면 중복 해지 요청을 막기 위해 버튼을 보여주지 않습니다.
                      오직 활성화(ACTIVE) 상태일 때만 '멤버십 구독 해지' 버튼을 노출합니다.
                    */}
                    {subscription.status === "ACTIVE" && (
                      <Button
                        type="button"
                        variant="outline"
                        className="h-9 gap-1.5 text-destructive border-destructive/30 hover:bg-destructive/5 hover:text-destructive shrink-0"
                        disabled={isCanceling}
                        onClick={handleCancelSubscription}
                      >
                        {isCanceling ? (
                          <Loader2 className="size-3.5 animate-spin" />
                        ) : (
                          <XCircle className="size-3.5" />
                        )}
                        멤버십 구독 해지
                      </Button>
                    )}
                  </div>
                ) : (
                  /* 구독 멤버십 요금제가 아예 없거나 CANCELED 일 때 (기존 UI 유지) */
                  <div className="py-10 text-center">
                    <p className="font-medium text-muted-foreground text-sm">
                      현재 활성화된 구독 멤버십 요금제가 없습니다.
                    </p>
                    <p className="mt-1 text-xs text-muted-foreground">
                      구독 멤버십에 가입하시면 최적화된 고퀄리티 여행 루트 추천 패키지를 이용하실 수 있어요.
                    </p>
                    <Button asChild className="mt-5 size-sm text-xs">
                      <Link href="/pricing">멤버십 요금제 둘러보기</Link>
                    </Button>
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="history" className="mt-6">
            <Card className="border-border/60">
              <CardHeader>
                <CardTitle className="text-base">최근 결제 내역</CardTitle>
              </CardHeader>
              <CardContent>
                {isHistoriesLoading ? (
                  <div className="py-10 text-center text-sm text-muted-foreground">내역을 불러오는 중...</div>
                ) : histories.length === 0 ? (
                  <div className="py-10 text-center text-sm text-muted-foreground">결제 내역이 없습니다.</div>
                ) : (
                  <PaymentHistoryList 
                    histories={histories} 
                    refetch={fetchPaymentHistories} 
                    refetchSubscription={refetchSubscription}
                  />
                )}
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </main>

      <SiteFooter />
    </div>
  )
}