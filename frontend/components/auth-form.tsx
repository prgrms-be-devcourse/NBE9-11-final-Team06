"use client"

import type React from "react"

import { useState } from "react"
import { useRouter } from "next/navigation"
import Link from "next/link"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Card, CardContent } from "@/components/ui/card"
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs"
import { Badge } from "@/components/ui/badge"
import { memberApi } from "@/lib/member-api"
import { preferenceApi } from "@/lib/preference-api"
import type { CompanionType, MobilityLevel } from "@/lib/types"
import { toast } from "sonner"
import { MapPin } from "lucide-react"

const CATEGORY_OPTIONS = [
  { id: 1, label: "전시" },
  { id: 2, label: "카페" },
  { id: 3, label: "산책" },
  { id: 4, label: "맛집" },
  { id: 5, label: "공연" },
]

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

export function AuthForm() {
  const router = useRouter()

  const [mode, setMode] = useState("login")

  const [loginEmail, setLoginEmail] = useState("")
  const [loginPassword, setLoginPassword] = useState("")

  const [signupNickname, setSignupNickname] = useState("")
  const [signupEmail, setSignupEmail] = useState("")
  const [signupPassword, setSignupPassword] = useState("")

  const [preferredArea, setPreferredArea] = useState("")
  const [selectedCategoryIds, setSelectedCategoryIds] = useState<number[]>([])
  const [selectedCompanion, setSelectedCompanion] = useState<CompanionType | null>(null)
  const [selectedMobilityLevel, setSelectedMobilityLevel] = useState<MobilityLevel | null>(null)
  const [avoidCrowded, setAvoidCrowded] = useState<boolean | null>(null)

  const [isSignupLoading, setIsSignupLoading] = useState(false)
  const [isLoginLoading, setIsLoginLoading] = useState(false)

  function toggleCategory(categoryId: number) {
    setSelectedCategoryIds((prev) =>
      prev.includes(categoryId)
        ? prev.filter((id) => id !== categoryId)
        : [...prev, categoryId],
    )
  }

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault()

    setIsLoginLoading(true)

    try {
      const response = await memberApi.login({
        email: loginEmail,
        password: loginPassword,
      })

      if (!response.success || !response.data) {
        toast.error(response.message ?? "로그인에 실패했습니다.")
        return
      }

      toast.success("로그인되었습니다. 환영해요!")
      router.push("/")
      router.refresh()
    } catch {
      toast.error("서버와 통신 중 오류가 발생했습니다.")
    } finally {
      setIsLoginLoading(false)
    }
  }

  async function handleSignup(e: React.FormEvent) {
    e.preventDefault()

    if (!preferredArea.trim()) {
      toast.error("선호 지역을 입력해주세요.")
      return
    }

    if (selectedCategoryIds.length === 0) {
      toast.error("관심사를 1개 이상 선택해주세요.")
      return
    }

    if (!selectedCompanion) {
      toast.error("주로 함께하는 동행을 선택해주세요.")
      return
    }

    if (!selectedMobilityLevel) {
      toast.error("이동 강도를 선택해주세요.")
      return
    }

    if (avoidCrowded === null) {
      toast.error("혼잡도 선호를 선택해주세요.")
      return
    }

    setIsSignupLoading(true)

    try {
      const signupResponse = await memberApi.signup({
        email: signupEmail,
        password: signupPassword,
        nickname: signupNickname,
      })

      if (!signupResponse.success) {
        toast.error(signupResponse.message ?? "회원가입에 실패했습니다.")
        return
      }

      const loginResponse = await memberApi.login({
        email: signupEmail,
        password: signupPassword,
      })

      if (!loginResponse.success || !loginResponse.data) {
        toast.success("회원가입이 완료되었어요. 로그인해주세요!")
        setMode("login")
        setLoginEmail(signupEmail)
        setLoginPassword("")
        resetSignupForm()
        return
      }

      const preferenceResponse = await preferenceApi.createMyPreference({
        preferredArea: preferredArea.trim(),
        categoryIds: selectedCategoryIds,
        companionType: selectedCompanion,
        mobilityLevel: selectedMobilityLevel,
        avoidCrowded,
      })

      if (!preferenceResponse.success) {
        toast.warning(
          preferenceResponse.message ??
            "회원가입은 완료되었지만 선호 정보 등록에 실패했습니다. 마이페이지에서 다시 등록해주세요.",
        )

        resetSignupForm()

        router.push("/mypage")
        router.refresh()
        return
      }

      toast.success("회원가입과 선호 정보 등록이 완료되었습니다.")

      resetSignupForm()

      router.push("/")
      router.refresh()
    } catch {
      toast.error("서버와 통신 중 오류가 발생했습니다.")
    } finally {
      setIsSignupLoading(false)
    }
  }

  function resetSignupForm() {
    setSignupNickname("")
    setSignupEmail("")
    setSignupPassword("")
    setPreferredArea("")
    setSelectedCategoryIds([])
    setSelectedCompanion(null)
    setSelectedMobilityLevel(null)
    setAvoidCrowded(null)
  }

  return (
    <div className="mx-auto w-full max-w-md">
      <div className="mb-8 flex flex-col items-center text-center">
        <div className="flex size-12 items-center justify-center rounded-2xl bg-primary text-primary-foreground">
          <MapPin className="size-6" />
        </div>

        <h1 className="mt-4 font-heading text-2xl font-bold tracking-tight">하루서울</h1>
        <p className="mt-1 text-sm text-muted-foreground">서울의 하루를 가장 나답게</p>
      </div>

      <Card className="border-border/60 shadow-sm">
        <CardContent className="pt-6">
          <Tabs value={mode} onValueChange={setMode}>
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="login">로그인</TabsTrigger>
              <TabsTrigger value="signup">회원가입</TabsTrigger>
            </TabsList>

            <TabsContent value="login" className="mt-6">
              <form onSubmit={handleLogin} className="flex flex-col gap-4">
                <div className="flex flex-col gap-2">
                  <Label htmlFor="login-email">이메일</Label>
                  <Input
                    id="login-email"
                    type="email"
                    placeholder="you@example.com"
                    value={loginEmail}
                    onChange={(e) => setLoginEmail(e.target.value)}
                    required
                  />
                </div>

                <div className="flex flex-col gap-2">
                  <Label htmlFor="login-password">비밀번호</Label>
                  <Input
                    id="login-password"
                    type="password"
                    placeholder="••••••••"
                    value={loginPassword}
                    onChange={(e) => setLoginPassword(e.target.value)}
                    required
                  />
                </div>

                <Button type="submit" className="mt-2 w-full" disabled={isLoginLoading}>
                  {isLoginLoading ? "로그인 중..." : "로그인"}
                </Button>
              </form>
            </TabsContent>

            <TabsContent value="signup" className="mt-6">
              <form onSubmit={handleSignup} className="flex flex-col gap-4">
                <div className="flex flex-col gap-2">
                  <Label htmlFor="signup-name">닉네임</Label>
                  <Input
                    id="signup-name"
                    placeholder="여행자"
                    value={signupNickname}
                    onChange={(e) => setSignupNickname(e.target.value)}
                    required
                  />
                </div>

                <div className="flex flex-col gap-2">
                  <Label htmlFor="signup-email">이메일</Label>
                  <Input
                    id="signup-email"
                    type="email"
                    placeholder="you@example.com"
                    value={signupEmail}
                    onChange={(e) => setSignupEmail(e.target.value)}
                    required
                  />
                </div>

                <div className="flex flex-col gap-2">
                  <Label htmlFor="signup-password">비밀번호</Label>
                  <Input
                    id="signup-password"
                    type="password"
                    placeholder="영문, 숫자 포함 8자 이상"
                    value={signupPassword}
                    onChange={(e) => setSignupPassword(e.target.value)}
                    required
                  />
                </div>

                <div className="flex flex-col gap-2">
                  <Label htmlFor="preferred-area">선호 지역</Label>
                  <Input
                    id="preferred-area"
                    placeholder="예: 홍대, 성수, 강남"
                    value={preferredArea}
                    onChange={(e) => setPreferredArea(e.target.value)}
                    required
                  />
                </div>

                <div className="flex flex-col gap-2">
                  <Label>관심사</Label>

                  <div className="flex flex-wrap gap-2">
                    {CATEGORY_OPTIONS.map((category) => {
                      const active = selectedCategoryIds.includes(category.id)

                      return (
                        <button
                          key={category.id}
                          type="button"
                          onClick={() => toggleCategory(category.id)}
                          className={`rounded-full border px-3 py-1.5 text-sm transition-colors ${
                            active
                              ? "border-primary bg-primary text-primary-foreground"
                              : "border-border bg-background text-foreground hover:border-primary/40"
                          }`}
                        >
                          {category.label}
                        </button>
                      )
                    })}
                  </div>
                </div>

                <div className="flex flex-col gap-2">
                  <Label>주로 함께하는 동행</Label>

                  <div className="flex flex-wrap gap-2">
                    {COMPANION_OPTIONS.map((option) => {
                      const active = selectedCompanion === option.value

                      return (
                        <button
                          key={option.value}
                          type="button"
                          onClick={() => setSelectedCompanion(option.value)}
                          className={`rounded-full border px-3 py-1.5 text-sm transition-colors ${
                            active
                              ? "border-accent bg-accent text-accent-foreground"
                              : "border-border bg-background text-foreground hover:border-accent/40"
                          }`}
                        >
                          {option.label}
                        </button>
                      )
                    })}
                  </div>
                </div>

                <div className="flex flex-col gap-2">
                  <Label>이동 강도</Label>

                  <div className="flex flex-wrap gap-2">
                    {MOBILITY_OPTIONS.map((option) => {
                      const active = selectedMobilityLevel === option.value

                      return (
                        <button
                          key={option.value}
                          type="button"
                          onClick={() => setSelectedMobilityLevel(option.value)}
                          className={`rounded-full border px-3 py-1.5 text-sm transition-colors ${
                            active
                              ? "border-primary bg-primary text-primary-foreground"
                              : "border-border bg-background text-foreground hover:border-primary/40"
                          }`}
                        >
                          {option.label}
                        </button>
                      )
                    })}
                  </div>
                </div>

                <div className="flex flex-col gap-2">
                  <Label>혼잡도 선호</Label>

                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      onClick={() => setAvoidCrowded(true)}
                      className={`rounded-full border px-3 py-1.5 text-sm transition-colors ${
                        avoidCrowded === true
                          ? "border-primary bg-primary text-primary-foreground"
                          : "border-border bg-background text-foreground hover:border-primary/40"
                      }`}
                    >
                      혼잡한 곳 피하기
                    </button>

                    <button
                      type="button"
                      onClick={() => setAvoidCrowded(false)}
                      className={`rounded-full border px-3 py-1.5 text-sm transition-colors ${
                        avoidCrowded === false
                          ? "border-primary bg-primary text-primary-foreground"
                          : "border-border bg-background text-foreground hover:border-primary/40"
                      }`}
                    >
                      상관없음
                    </button>
                  </div>
                </div>

                {selectedCategoryIds.length > 0 && (
                  <Badge variant="secondary" className="w-fit">
                    {selectedCategoryIds.length}개 관심사 선택됨
                  </Badge>
                )}

                <Button type="submit" className="mt-2 w-full" disabled={isSignupLoading}>
                  {isSignupLoading ? "가입 중..." : "가입하고 시작하기"}
                </Button>
              </form>
            </TabsContent>
          </Tabs>
        </CardContent>
      </Card>

      <p className="mt-6 text-center text-xs text-muted-foreground">
        둘러보기만 할게요?{" "}
        <Link href="/" className="font-medium text-primary hover:underline">
          홈으로 돌아가기
        </Link>
      </p>
    </div>
  )
}
