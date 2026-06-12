"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import Image from "next/image"
import { useRouter } from "next/navigation"
import { toast } from "sonner"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Avatar, AvatarImage, AvatarFallback } from "@/components/ui/avatar"
import { Separator } from "@/components/ui/separator"
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { SAVED_COURSES, CATEGORIES, COMPANIONS } from "@/lib/data"
import { memberApi } from "@/lib/member-api"
import type { Member } from "@/lib/types"
import { MapPin, Clock, Route, Heart, Settings, Bookmark } from "lucide-react"

const myInterests = ["전시", "카페", "산책"]
const myCompanion = "커플"

export default function MyPage() {
  const router = useRouter()

  const [activeTab, setActiveTab] = useState("saved")
  const [member, setMember] = useState<Member | null>(null)
  const [nickname, setNickname] = useState("")
  const [profileImageUrl, setProfileImageUrl] = useState("")

  const [isLoading, setIsLoading] = useState(true)
  const [isUpdating, setIsUpdating] = useState(false)
  const [isWithdrawing, setIsWithdrawing] = useState(false)

  useEffect(() => {
    async function fetchMyInfo() {
      try {
        const response = await memberApi.getMyInfo()

        if (!response.success || !response.data) {
          toast.error(response.message ?? "로그인이 필요합니다.")
          router.push("/login")
          return
        }

        setMember(response.data)
        setNickname(response.data.nickname)
        setProfileImageUrl(response.data.profileImageUrl ?? "")
      } catch {
        toast.error("회원 정보를 불러오는 중 오류가 발생했습니다.")
        router.push("/login")
      } finally {
        setIsLoading(false)
      }
    }

    fetchMyInfo()
  }, [router])

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
        profileImageUrl: profileImageUrl.trim(),
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

      
      try {
        await memberApi.logout()
      } catch (error) {
        console.error("회원 탈퇴 후 로그아웃 처리 중 오류 발생:", error)
      }

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

  if (isLoading) {
    return (
      <div className="flex min-h-screen flex-col bg-background">
        <SiteHeader />
        <main className="mx-auto flex w-full max-w-5xl flex-1 items-center justify-center px-4 py-10">
          <p className="text-sm text-muted-foreground">회원 정보를 불러오는 중...</p>
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
      <SiteHeader />

      <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-10">
        {/* Profile header */}
        <div className="flex flex-col gap-5 rounded-3xl border border-border/60 bg-card p-6 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-4">
            <Avatar className="size-16 overflow-hidden">
              {member.profileImageUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
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
                  <Heart className="size-3" /> {SAVED_COURSES.length}개 코스 저장
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
          </TabsList>

          <TabsContent value="saved" className="mt-6">
            <div className="grid gap-5 sm:grid-cols-2">
              {SAVED_COURSES.map((course) => (
                <Link key={course.id} href={`/course/${course.id}`}>
                  <Card className="group h-full overflow-hidden border-border/60 pt-0 transition-shadow hover:shadow-md">
                    <div className="relative aspect-[16/9] overflow-hidden">
                      <Image
                        src={course.cover || "/placeholder.svg"}
                        alt={course.title}
                        fill
                        className="object-cover transition-transform duration-300 group-hover:scale-105"
                      />

                      <Badge className="absolute left-3 top-3 bg-background/90 text-foreground hover:bg-background/90">
                        {course.area}
                      </Badge>
                    </div>

                    <CardContent className="px-5 pb-5">
                      <h3 className="font-heading font-semibold">{course.title}</h3>

                      <p className="mt-1 line-clamp-2 text-sm text-muted-foreground">
                        {course.description}
                      </p>

                      <div className="mt-3 flex flex-wrap gap-3 text-xs text-muted-foreground">
                        <span className="flex items-center gap-1">
                          <Clock className="size-3.5" /> {course.totalDuration}
                        </span>

                        <span className="flex items-center gap-1">
                          <Route className="size-3.5" /> {course.totalDistance}
                        </span>

                        <span className="flex items-center gap-1">
                          <MapPin className="size-3.5" /> {course.stops.length}개 장소
                        </span>
                      </div>
                    </CardContent>
                  </Card>
                </Link>
              ))}
            </div>
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

                <div>
                  <p className="mb-3 text-sm font-medium">관심 카테고리</p>

                  <div className="flex flex-wrap gap-2">
                    {CATEGORIES.map((c) => {
                      const active = myInterests.includes(c.value)

                      return (
                        <span
                          key={c.value}
                          className={`rounded-full border px-3 py-1.5 text-sm ${
                            active
                              ? "border-primary bg-primary text-primary-foreground"
                              : "border-border bg-background text-muted-foreground"
                          }`}
                        >
                          {c.label}
                        </span>
                      )
                    })}
                  </div>
                </div>

                <Separator />

                <div>
                  <p className="mb-3 text-sm font-medium">주 동행 유형</p>

                  <div className="flex flex-wrap gap-2">
                    {COMPANIONS.map((c) => {
                      const active = myCompanion === c.value

                      return (
                        <span
                          key={c.value}
                          className={`rounded-full border px-3 py-1.5 text-sm ${
                            active
                              ? "border-accent bg-accent text-accent-foreground"
                              : "border-border bg-background text-muted-foreground"
                          }`}
                        >
                          {c.value}
                        </span>
                      )
                    })}
                  </div>
                </div>

                <Button className="w-fit">선호 정보 수정</Button>

                <Separator />

                <div>
                  <p className="mb-2 text-sm font-medium text-destructive">회원 탈퇴</p>

                  <p className="mb-3 text-sm text-muted-foreground">
                    탈퇴하면 현재 계정으로 다시 로그인할 수 없습니다.
                  </p>

                  <Button
                    type="button"
                    variant="destructive"
                    onClick={handleWithdraw}
                    disabled={isWithdrawing}
                  >
                    {isWithdrawing ? "탈퇴 처리 중..." : "탈퇴하기"}
                  </Button>
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </main>

      <SiteFooter />
    </div>
  )
}