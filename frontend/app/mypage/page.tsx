"use client"

import type React from "react"

import { useEffect, useState } from "react"
import Link from "next/link"
import Image from "next/image"
import { useRouter } from "next/navigation"
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
import { SAVED_COURSES } from "@/lib/data"
import { memberApi } from "@/lib/member-api"
import { preferenceApi } from "@/lib/preference-api"
import type {
  CompanionType,
  Member,
  MobilityLevel,
  UserPreference,
} from "@/lib/types"
import { MapPin, Clock, Route, Heart, Settings, Bookmark } from "lucide-react"

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

export default function MyPage() {
  const router = useRouter()

  const [activeTab, setActiveTab] = useState("saved")
  const [member, setMember] = useState<Member | null>(null)
  const [preference, setPreference] = useState<UserPreference | null>(null)

  const [nickname, setNickname] = useState("")
  const [profileImageUrl, setProfileImageUrl] = useState("")

  const [preferredArea, setPreferredArea] = useState("")
  const [selectedCategoryIds, setSelectedCategoryIds] = useState<number[]>([])
  const [companionType, setCompanionType] = useState<CompanionType | null>(null)
  const [mobilityLevel, setMobilityLevel] = useState<MobilityLevel | null>(null)
  const [avoidCrowded, setAvoidCrowded] = useState<boolean | null>(null)

  const [isLoading, setIsLoading] = useState(true)
  const [isUpdating, setIsUpdating] = useState(false)
  const [isWithdrawing, setIsWithdrawing] = useState(false)
  const [isPreferenceSaving, setIsPreferenceSaving] = useState(false)
  const [isPreferenceDeleting, setIsPreferenceDeleting] = useState(false)

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
          return
        }

        if (!preferenceResponse.success && preferenceResponse.code !== "PREFERENCE_NOT_FOUND") {
          toast.error(
            preferenceResponse.message ?? "선호 정보를 불러오는 중 오류가 발생했습니다.",
          )
        }
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
  }, [router])

  function applyPreference(nextPreference: UserPreference) {
    setPreference(nextPreference)
    setPreferredArea(nextPreference.preferredArea)
    setSelectedCategoryIds(nextPreference.categories.map((category) => category.id))
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

  if (isLoading) {
    return (
      <div className="flex min-h-screen flex-col bg-background">
        <SiteHeader />
        <main className="mx-auto flex w-full max-w-5xl flex-1 items-center justify-center px-4 py-10">
          <p className="text-sm text-muted-foreground">
            회원 정보를 불러오는 중...
          </p>
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
              <h1 className="font-heading text-xl font-bold">
                {member.nickname}님
              </h1>
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
                      <h3 className="font-heading font-semibold">
                        {course.title}
                      </h3>

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
                          <MapPin className="size-3.5" />{" "}
                          {course.stops.length}개 장소
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

                <form
                  onSubmit={handleSavePreference}
                  className="flex flex-col gap-6"
                >
                  <div>
                    <p className="mb-3 text-sm font-medium">선호 지역</p>
                    <Input
                      value={preferredArea}
                      onChange={(e) => setPreferredArea(e.target.value)}
                      placeholder="예: 홍대, 성수, 강남"
                    />
                  </div>

                  <div>
                    <p className="mb-3 text-sm font-medium">관심 카테고리</p>
                    <CategoryMultiSelect
                      selectedCategoryIds={selectedCategoryIds}
                      onChange={setSelectedCategoryIds}
                      disabled={isPreferenceSaving}
                    />
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
                            className={`rounded-full border px-3 py-1.5 text-sm disabled:cursor-not-allowed disabled:opacity-60 ${
                              active
                                ? "border-accent bg-accent text-accent-foreground"
                                : "border-border bg-background text-muted-foreground"
                            }`}
                          >
                            {option.label}
                          </button>
                        )
                      })}
                    </div>
                  </div>

                  <div>
                    <p className="mb-3 text-sm font-medium">이동 강도</p>

                    <div className="flex flex-wrap gap-2">
                      {MOBILITY_OPTIONS.map((option) => {
                        const active = mobilityLevel === option.value

                        return (
                          <button
                            key={option.value}
                            type="button"
                            disabled={isPreferenceSaving}
                            onClick={() => setMobilityLevel(option.value)}
                            className={`rounded-full border px-3 py-1.5 text-sm disabled:cursor-not-allowed disabled:opacity-60 ${
                              active
                                ? "border-primary bg-primary text-primary-foreground"
                                : "border-border bg-background text-muted-foreground"
                            }`}
                          >
                            {option.label}
                          </button>
                        )
                      })}
                    </div>
                  </div>

                  <div>
                    <p className="mb-3 text-sm font-medium">혼잡도 선호</p>

                    <div className="flex flex-wrap gap-2">
                      <button
                        type="button"
                        disabled={isPreferenceSaving}
                        onClick={() => setAvoidCrowded(true)}
                        className={`rounded-full border px-3 py-1.5 text-sm disabled:cursor-not-allowed disabled:opacity-60 ${
                          avoidCrowded === true
                            ? "border-primary bg-primary text-primary-foreground"
                            : "border-border bg-background text-muted-foreground"
                        }`}
                      >
                        혼잡한 곳 피하기
                      </button>

                      <button
                        type="button"
                        disabled={isPreferenceSaving}
                        onClick={() => setAvoidCrowded(false)}
                        className={`rounded-full border px-3 py-1.5 text-sm disabled:cursor-not-allowed disabled:opacity-60 ${
                          avoidCrowded === false
                            ? "border-primary bg-primary text-primary-foreground"
                            : "border-border bg-background text-muted-foreground"
                        }`}
                      >
                        상관없음
                      </button>
                    </div>
                  </div>

                  <div className="flex flex-wrap gap-2">
                    <Button
                      type="submit"
                      className="w-fit"
                      disabled={isPreferenceSaving}
                    >
                      {isPreferenceSaving
                        ? "저장 중..."
                        : preference
                          ? "선호 정보 수정"
                          : "선호 정보 등록"}
                    </Button>

                    {preference && (
                      <Button
                        type="button"
                        variant="outline"
                        className="w-fit bg-transparent"
                        onClick={handleDeletePreference}
                        disabled={isPreferenceDeleting}
                      >
                        {isPreferenceDeleting ? "삭제 중..." : "선호 정보 삭제"}
                      </Button>
                    )}
                  </div>
                </form>

                <Separator />

                <div>
                  <p className="mb-2 text-sm font-medium text-destructive">
                    회원 탈퇴
                  </p>

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