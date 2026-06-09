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
import { toast } from "sonner"
import { MapPin } from "lucide-react"

export function AuthForm() {
  const router = useRouter()
  const [mode, setMode] = useState("login")
  const [selectedInterests, setSelectedInterests] = useState<string[]>([])
  const [selectedCompanion, setSelectedCompanion] = useState<string>("")

  function toggleInterest(id: string) {
    setSelectedInterests((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]))
  }

  function handleLogin(e: React.FormEvent) {
    e.preventDefault()
    toast.success("로그인되었습니다. 환영해요!")
    router.push("/mypage")
  }

  function handleSignup(e: React.FormEvent) {
    e.preventDefault()
    toast.success("회원가입이 완료되었어요. 맞춤 추천을 시작해보세요!")
    router.push("/mypage")
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
                  <Input id="login-email" type="email" placeholder="you@example.com" required />
                </div>
                <div className="flex flex-col gap-2">
                  <Label htmlFor="login-password">비밀번호</Label>
                  <Input id="login-password" type="password" placeholder="••••••••" required />
                </div>
                <Button type="submit" className="mt-2 w-full">
                  로그인
                </Button>
              </form>
            </TabsContent>

            <TabsContent value="signup" className="mt-6">
              <form onSubmit={handleSignup} className="flex flex-col gap-4">
                <div className="flex flex-col gap-2">
                  <Label htmlFor="signup-name">닉네임</Label>
                  <Input id="signup-name" placeholder="여행자" required />
                </div>
                <div className="flex flex-col gap-2">
                  <Label htmlFor="signup-email">이메일</Label>
                  <Input id="signup-email" type="email" placeholder="you@example.com" required />
                </div>
                <div className="flex flex-col gap-2">
                  <Label htmlFor="signup-password">비밀번호</Label>
                  <Input id="signup-password" type="password" placeholder="••••••••" required />
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

                <Button type="submit" className="mt-2 w-full">
                  가입하고 시작하기
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
