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
import { CATEGORIES, COMPANIONS } from "@/lib/data"
import { memberApi } from "@/lib/member-api"
import { authStorage } from "@/lib/auth"
import { toast } from "sonner"
import { MapPin } from "lucide-react"

export function AuthForm() {
  const router = useRouter()

  const [mode, setMode] = useState("login")

  const [loginEmail, setLoginEmail] = useState("")
  const [loginPassword, setLoginPassword] = useState("")

  const [signupNickname, setSignupNickname] = useState("")
  const [signupEmail, setSignupEmail] = useState("")
  const [signupPassword, setSignupPassword] = useState("")

  const [selectedInterests, setSelectedInterests] = useState<string[]>([])
  const [selectedCompanion, setSelectedCompanion] = useState<string>("")

  const [isSignupLoading, setIsSignupLoading] = useState(false)
  const [isLoginLoading, setIsLoginLoading] = useState(false)

  function toggleInterest(id: string) {
    setSelectedInterests((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
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

      authStorage.setAccessToken(response.data.accessToken)

      toast.success("로그인되었습니다. 환영해요!")
      router.push("/")
    } catch {
      toast.error("서버와 통신 중 오류가 발생했습니다.")
    } finally {
      setIsLoginLoading(false)
    }
  }

  async function handleSignup(e: React.FormEvent) {
    e.preventDefault()

    setIsSignupLoading(true)

    try {
      const response = await memberApi.signup({
        email: signupEmail,
        password: signupPassword,
        nickname: signupNickname,
      })

      if (!response.success) {
        toast.error(response.message ?? "회원가입에 실패했습니다.")
        return
      }

      toast.success("회원가입이 완료되었어요. 로그인해주세요!")

      setMode("login")
      setLoginEmail(signupEmail)
      setLoginPassword("")

      setSignupNickname("")
      setSignupEmail("")
      setSignupPassword("")
      setSelectedInterests([])
      setSelectedCompanion("")
    } catch {
      toast.error("서버와 통신 중 오류가 발생했습니다.")
    } finally {
      setIsSignupLoading(false)
    }
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
                  <Label>관심사 (선호 정보)</Label>
                  <div className="flex flex-wrap gap-2">
                    {CATEGORIES.map((it) => {
                      const active = selectedInterests.includes(it.value)

                      return (
                        <button
                          key={it.value}
                          type="button"
                          onClick={() => toggleInterest(it.value)}
                          className={`rounded-full border px-3 py-1.5 text-sm transition-colors ${
                            active
                              ? "border-primary bg-primary text-primary-foreground"
                              : "border-border bg-background text-foreground hover:border-primary/40"
                          }`}
                        >
                          {it.label}
                        </button>
                      )
                    })}
                  </div>
                </div>

                <div className="flex flex-col gap-2">
                  <Label>주로 함께하는 동행</Label>
                  <div className="flex flex-wrap gap-2">
                    {COMPANIONS.map((c) => {
                      const active = selectedCompanion === c.value

                      return (
                        <button
                          key={c.value}
                          type="button"
                          onClick={() => setSelectedCompanion(c.value)}
                          className={`rounded-full border px-3 py-1.5 text-sm transition-colors ${
                            active
                              ? "border-accent bg-accent text-accent-foreground"
                              : "border-border bg-background text-foreground hover:border-accent/40"
                          }`}
                        >
                          {c.value}
                        </button>
                      )
                    })}
                  </div>
                </div>

                {selectedInterests.length > 0 && (
                  <Badge variant="secondary" className="w-fit">
                    {selectedInterests.length}개 관심사 선택됨
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