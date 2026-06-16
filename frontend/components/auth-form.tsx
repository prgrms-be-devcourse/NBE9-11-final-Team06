"use client"

import type React from "react"

import { useEffect, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import Link from "next/link"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Card, CardContent } from "@/components/ui/card"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Badge } from "@/components/ui/badge"
import { memberApi } from "@/lib/member-api"
import { preferenceApi } from "@/lib/preference-api"
import type { CompanionType, MobilityLevel } from "@/lib/types"
import { toast } from "sonner"
import { MapPin } from "lucide-react"

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

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
  const searchParams = useSearchParams()

  const googleOAuthLoginUrl = `${API_BASE_URL}/oauth2/authorization/google`
  const kakaoOAuthLoginUrl = `${API_BASE_URL}/oauth2/authorization/kakao`

  const [mode, setMode] = useState("login")

  const [loginEmail, setLoginEmail] = useState("")
  const [loginPassword, setLoginPassword] = useState("")

  const [signupNickname, setSignupNickname] = useState("")
  const [signupEmail, setSignupEmail] = useState("")
  const [signupPassword, setSignupPassword] = useState("")

  const [preferredArea, setPreferredArea] = useState("")
  const [selectedCategoryIds, setSelectedCategoryIds] = useState<number[]>([])
  const [selectedCompanion, setSelectedCompanion] =
    useState<CompanionType | null>(null)
  const [selectedMobilityLevel, setSelectedMobilityLevel] =
    useState<MobilityLevel | null>(null)
  const [avoidCrowded, setAvoidCrowded] = useState<boolean | null>(null)

  const [isSignupLoading, setIsSignupLoading] = useState(false)
  const [isLoginLoading, setIsLoginLoading] = useState(false)

  useEffect(() => {
    if (searchParams.get("error") === "oauth") {
      toast.error("소셜 로그인에 실패했습니다. 다시 시도해주세요.")
      router.replace("/login")
    }
  }, [router, searchParams])

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

        <h1 className="mt-4 font-heading text-2xl font-bold tracking-tight">
          하루서울
        </h1>

        <p className="mt-1 text-sm text-muted-foreground">
          서울의 하루를 가장 나답게
        </p>
      </div>

      <Card className="border-border/60 shadow-sm">
        <CardContent className="pt-6">
          <Tabs value={mode} onValueChange={setMode}>
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="login">로그인</TabsTrigger>
              <TabsTrigger value="signup">회원가입</TabsTrigger>
            </TabsList>

            <div className="mt-6 flex flex-col gap-2">
              <a
                href={googleOAuthLoginUrl}
                className="inline-flex h-11 w-full items-center justify-center gap-3 rounded-md border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-800 shadow-sm transition-colors hover:bg-slate-50"
              >
                <svg className="size-5" viewBox="0 0 24 24" aria-hidden="true">
                  <path
                    fill="#4285F4"
                    d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                  />
                  <path
                    fill="#34A853"
                    d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                  />
                  <path
                    fill="#FBBC05"
                    d="M5.84 14.1c-.22-.66-.35-1.36-.35-2.1s.13-1.44.35-2.1V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l3.66-2.84z"
                  />
                  <path
                    fill="#EA4335"
                    d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06L5.84 9.9C6.71 7.3 9.14 5.38 12 5.38z"
                  />
                </svg>
                Google로 계속하기
              </a>

              <a
                href={kakaoOAuthLoginUrl}
                className="inline-flex h-11 w-full items-center justify-center gap-3 rounded-md bg-[#FEE500] px-4 py-2 text-sm font-semibold text-[#191919] shadow-sm transition-colors hover:bg-[#FADA0A]"
              >
                <svg className="size-5" viewBox="0 0 24 24" aria-hidden="true">
                  <path
                    fill="#191919"
                    d="M12 3C6.48 3 2 6.58 2 11c0 2.83 1.84 5.32 4.62 6.74l-.74 2.72c-.07.27.24.49.47.33l3.24-2.16c.78.15 1.59.23 2.41.23 5.52 0 10-3.58 10-8S17.52 3 12 3z"
                  />
                </svg>
                Kakao로 계속하기
              </a>
            </div>

            <div className="my-5 flex items-center gap-3">
              <div className="h-px flex-1 bg-border" />
              <span className="text-xs text-muted-foreground">
                또는 이메일로 계속하기
              </span>
              <div className="h-px flex-1 bg-border" />
            </div>

            <TabsContent value="login">
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

                <Button
                  type="submit"
                  className="mt-2 w-full"
                  disabled={isLoginLoading}
                >
                  {isLoginLoading ? "로그인 중..." : "로그인"}
                </Button>
              </form>
            </TabsContent>

            <TabsContent value="signup">
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

                <Button
                  type="submit"
                  className="mt-2 w-full"
                  disabled={isSignupLoading}
                >
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