"use client"

import { Suspense, useEffect, useMemo, useState } from "react"
import Link from "next/link"
import { useRouter, useSearchParams } from "next/navigation"
import {
  ArrowRight,
  CalendarDays,
  Check,
  Clock,
  MapPin,
  Route,
  Sparkles,
  Users,
} from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { SiteFooter } from "@/components/site-footer"
import { SiteHeader } from "@/components/site-header"

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080"

type CourseItemType = "PLACE" | "EVENT" | "TOUR"

type CoursePlace = {
  id?: number
  itemType?: CourseItemType
  placeId?: number | null
  eventId?: number | null
  tourId?: number | null
  title?: string
  itemName?: string
  name?: string
  placeName?: string
  eventTitle?: string
  category?: string
  categoryName?: string
  detailCategoryName?: string | null
  area?: string
  address?: string | null
  latitude?: number | string | null
  longitude?: number | string | null
  visitOrder?: number
  sequence?: number
  order?: number
  memo?: string
  recommendationReason?: string
  reason?: string
}


type CourseDetail = {
  id?: number
  courseId?: number
  title?: string
  description?: string
  courseType?: string
  startDate?: string
  endDate?: string
  baseArea?: string
  companionType?: string
  recommendationReason?: string
  reason?: string
  places?: CoursePlace[]
  coursePlaces?: CoursePlace[]
}

type RecommendationCandidate = {
  type?: "EVENT" | "TOUR"
  eventId?: number | null
  tourId?: number | null
  title?: string
  categoryName?: string | null
  detailCategoryName?: string | null
  address?: string | null
  latitude?: number | string | null
  longitude?: number | string | null
  score?: number
  recommendationReason?: string | null
}

type RecommendationCandidateDraft = {
  startDate?: string
  endDate?: string
  baseArea?: string
  companionType?: string
  candidates?: RecommendationCandidate[]
}

function normalizeAccessToken(value: string | null | undefined) {
  if (!value) return null

  const token = value.trim().replace(/^Bearer\s+/i, "")

  if (!token || token === "undefined" || token === "null") {
    return null
  }

  return token
}

function findAccessToken(value: any): string | null {
  if (!value) return null

  if (typeof value === "string") {
    const token = normalizeAccessToken(value)
    return token?.startsWith("eyJ") ? token : null
  }

  if (typeof value !== "object") return null

  const directToken =
    normalizeAccessToken(value.accessToken) ??
    normalizeAccessToken(value.access_token) ??
    normalizeAccessToken(value.token) ??
    normalizeAccessToken(value.jwt)

  if (directToken) return directToken

  for (const nestedValue of Object.values(value)) {
    const token = findAccessToken(nestedValue)
    if (token) return token
  }

  return null
}

function getAccessToken() {
  if (typeof window === "undefined") return null

  const storages = [localStorage, sessionStorage]
  const tokenKeys = ["accessToken", "access_token", "token", "jwt"]

  for (const storage of storages) {
    for (const tokenKey of tokenKeys) {
      const token = normalizeAccessToken(storage.getItem(tokenKey))
      if (token) return token
    }
  }

  for (const storage of storages) {
    for (let i = 0; i < storage.length; i++) {
      const key = storage.key(i)
      if (!key) continue

      const value = storage.getItem(key)
      if (!value) continue

      const rawToken = normalizeAccessToken(value)
      if (rawToken?.startsWith("eyJ")) return rawToken

      try {
        const parsed = JSON.parse(value)
        const token = findAccessToken(parsed)
        if (token) return token
      } catch {
        // JSON이 아닌 값은 건너뜁니다.
      }
    }
  }

  return null
}

async function readJsonSafely(response: Response) {
  const text = await response.text()

  if (!text) {
    return null
  }

  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

function extractCourse(result: any) {
  return (
    result?.data ??
    result?.result ??
    result?.body ??
    result?.content ??
    result?.response ??
    result
  ) as CourseDetail | null
}


function getSavedRecommendedCourse(courseId?: string) {
  if (typeof window === "undefined") return null

  const saved = sessionStorage.getItem("recommendedCourse")
  if (!saved) return null

  try {
    const parsed = JSON.parse(saved) as CourseDetail
    const savedCourseId = parsed.id ?? parsed.courseId

    if (!courseId || String(savedCourseId) === courseId) {
      return parsed
    }

    return null
  } catch {
    return null
  }
}

function getSavedRecommendationCandidates() {
  if (typeof window === "undefined") return null

  const saved = sessionStorage.getItem("recommendationCandidates")
  if (!saved) return null

  try {
    return JSON.parse(saved) as RecommendationCandidateDraft
  } catch {
    return null
  }
}

function createCandidateCourse(draft: RecommendationCandidateDraft): CourseDetail {
  const places: CoursePlace[] = (draft.candidates ?? []).map((candidate, index) => ({
    itemType: candidate.type,
    eventId: candidate.eventId ?? null,
    tourId: candidate.tourId ?? null,
    title: candidate.title,
    categoryName: candidate.categoryName ?? undefined,
    detailCategoryName: candidate.detailCategoryName ?? undefined,
    address: candidate.address ?? null,
    latitude: candidate.latitude ?? null,
    longitude: candidate.longitude ?? null,
    visitOrder: index + 1,
    recommendationReason:
      candidate.recommendationReason ??
      "선택한 조건과 출발 위치를 바탕으로 추천된 장소입니다.",
  }))

  return {
    title: `${draft.baseArea ?? "선택한 지역"} 추천 후보`,
    description: "행사와 관광지 후보를 확인한 뒤 원하는 장소를 선택해 코스로 저장하세요.",
    startDate: draft.startDate,
    endDate: draft.endDate,
    baseArea: draft.baseArea,
    companionType: draft.companionType,
    recommendationReason: "선택한 취향, 동행 유형, 출발 위치를 함께 고려해 행사와 관광지를 추천했어요.",
    places,
  }
}


function formatDate(dateStr?: string) {
  return dateStr
    ? new Date(dateStr).toLocaleDateString("ko-KR", {
        year: "numeric",
        month: "long",
        day: "numeric",
        weekday: "short",
      })
    : "날짜 미정"
}

function getCoursePlaces(course: CourseDetail | null) {
  return course?.places ?? course?.coursePlaces ?? []
}

function getPlaceTitle(place: CoursePlace) {
  return (
    place.itemName ??
    place.title ??
    place.placeName ??
    place.eventTitle ??
    place.name ??
    "추천 장소"
  )
}

function getPlaceCategory(place: CoursePlace) {
  if (place.itemType === "TOUR") {
    return place.detailCategoryName ?? place.categoryName ?? "관광지"
  }
  if (place.itemType === "EVENT") {
    return place.categoryName ?? place.category ?? "행사"
  }
  if (place.itemType === "PLACE") {
    return place.categoryName ?? place.category ?? "장소"
  }

  return place.detailCategoryName ?? place.categoryName ?? place.category ?? "추천"
}

function getPlaceReason(place: CoursePlace) {
  return (
    place.recommendationReason ??
    place.reason ??
    place.memo ??
    "사용자 선호 정보와 행사 유사도를 기반으로 추천되었습니다."
  )
}

function getVisitOrder(place: CoursePlace, index: number) {
  return place.visitOrder ?? place.sequence ?? place.order ?? index + 1
}

function getPlaceKey(place: CoursePlace, index: number) {
  return `${place.itemType ?? "UNKNOWN"}-${
    place.tourId ?? place.eventId ?? place.placeId ?? place.id ?? index
  }`
}

function getCoordinateLabel(place: CoursePlace) {
  if (place.itemType === "TOUR") return "관광지 좌표"
  if (place.itemType === "EVENT") return "행사 좌표"
  return "장소 좌표"
}

function parseNullableNumber(value?: string) {
  if (!value) return null

  const numberValue = Number(value)

  return Number.isFinite(numberValue) ? numberValue : null
}

export default function RecommendPage() {
  return (
    <div className="flex min-h-screen flex-col">
      <SiteHeader />
      <Suspense fallback={<RecommendPageFallback />}>
        <RecommendContent />
      </Suspense>
      <SiteFooter />
    </div>
  )
}

function RecommendPageFallback() {
  return (
    <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6">
      <Card className="p-6 text-sm text-muted-foreground">
        추천 코스를 불러오는 중입니다.
      </Card>
    </main>
  )
}

function RecommendEmptyState() {
  return (
    <main className="mx-auto flex w-full max-w-6xl flex-1 items-center justify-center px-4 py-16 sm:px-6">
      <Card className="w-full max-w-xl p-8 text-center">
        <span className="mx-auto flex size-12 items-center justify-center rounded-2xl bg-secondary text-primary">
          <Sparkles className="size-6" />
        </span>

        <h1 className="mt-5 text-2xl font-bold tracking-tight">
          아직 생성된 추천 코스가 없습니다.
        </h1>

        <p className="mt-3 leading-relaxed text-muted-foreground">
          날짜, 지역, 동행 유형, 취향을 선택하면 맞춤 코스를 생성할 수 있어요.
        </p>

        <Button asChild className="mt-6 gap-2">
          <Link href="/plan">
            코스 추천 받기
            <ArrowRight className="size-4" />
          </Link>
        </Button>
      </Card>
    </main>
  )
}

function RecommendContent() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const [course, setCourse] = useState<CourseDetail | null>(null)
  const [isLoadingCourse, setIsLoadingCourse] = useState(false)
  const [courseLoadError, setCourseLoadError] = useState<string | null>(null)
  const [selectedEventIds, setSelectedEventIds] = useState<number[]>([])
  const [selectedTourIds, setSelectedTourIds] = useState<number[]>([])
  const [selectionMessage, setSelectionMessage] = useState<string | null>(null)

  const courseId = searchParams.get("courseId") ?? undefined
  const candidateMode = searchParams.get("candidateMode") === "true"
  const areaParam = searchParams.get("area") ?? undefined
  const locationNameParam = searchParams.get("locationName") ?? undefined
  const locationSource = searchParams.get("locationSource") ?? "preset"
  const locationAddress = searchParams.get("locationAddress") ?? undefined
  const latitude = searchParams.get("lat") ?? undefined
  const longitude = searchParams.get("lng") ?? undefined
  const companionParam = searchParams.get("companion") ?? undefined
  const dateParam = searchParams.get("date") ?? undefined
  const cats = useMemo(
    () => searchParams.get("cats")?.split(",").filter(Boolean) ?? [],
    [searchParams],
  )

  const hasRecommendContext = Boolean(
    courseId ||
      areaParam ||
      locationNameParam ||
      locationAddress ||
      latitude ||
      longitude ||
      companionParam ||
      dateParam ||
      cats.length > 0,
  )

  useEffect(() => {
    const savedCourse = getSavedRecommendedCourse(courseId)
    const savedCandidateDraft = getSavedRecommendationCandidates()

    if (candidateMode && savedCandidateDraft) {
      setCourse(createCandidateCourse(savedCandidateDraft))
    } else if (savedCourse) {
      setCourse(savedCourse)
    }

    if (!courseId) return

    async function fetchCourse() {
      setIsLoadingCourse(true)
      setCourseLoadError(null)

      try {
        const accessToken = getAccessToken()
        const headers: HeadersInit = {}

        if (accessToken) {
          headers.Authorization = `Bearer ${accessToken}`
        }

        const response = await fetch(`${API_BASE_URL}/api/courses/${courseId}`, {
          method: "GET",
          credentials: "include",
          headers,
        })

        const result = await readJsonSafely(response)

        if (!response.ok) {
          if (!savedCourse) {
            setCourseLoadError(`courseId=${courseId} 조회에 실패했습니다.`)
          }
          return
        }

        const fetchedCourse = extractCourse(result)
        setCourse(fetchedCourse)
      } catch {
        if (!savedCourse) {
          setCourseLoadError(`courseId=${courseId} 조회에 실패했습니다.`)
        }
      } finally {
        setIsLoadingCourse(false)
      }
    }

    fetchCourse()
  }, [candidateMode, courseId])

  function toggleCandidateSelection(place: CoursePlace) {
    if (place.itemType === "EVENT" && place.eventId) {
      setSelectedEventIds((currentIds) =>
        currentIds.includes(place.eventId!)
          ? currentIds.filter((id) => id !== place.eventId)
          : [...currentIds, place.eventId!],
      )
      setSelectionMessage(null)
      return
    }

    if (place.itemType === "TOUR" && place.tourId) {
      setSelectedTourIds((currentIds) =>
        currentIds.includes(place.tourId!)
          ? currentIds.filter((id) => id !== place.tourId)
          : [...currentIds, place.tourId!],
      )
      setSelectionMessage(null)
    }
  }

  function isCandidateSelected(place: CoursePlace) {
    if (place.itemType === "EVENT") {
      return Boolean(place.eventId && selectedEventIds.includes(place.eventId))
    }

    if (place.itemType === "TOUR") {
      return Boolean(place.tourId && selectedTourIds.includes(place.tourId))
    }

    return false
  }

  function saveSelectedCandidates() {
    const selectedCount = selectedEventIds.length + selectedTourIds.length

    if (selectedCount === 0) {
      setSelectionMessage("최소 한 곳 이상의 행사 또는 관광지를 선택해주세요.")
      return
    }

    sessionStorage.setItem(
      "selectedRecommendationItems",
      JSON.stringify({
        eventIds: selectedEventIds,
        tourIds: selectedTourIds,
        startDate: course?.startDate ?? dateParam ?? null,
        endDate: course?.endDate ?? dateParam ?? null,
        baseArea: course?.baseArea ?? areaParam ?? null,
        companionType: course?.companionType ?? companionParam ?? null,
        startLatitude: parseNullableNumber(latitude),
        startLongitude: parseNullableNumber(longitude),
      }),
    )

    setSelectionMessage(
      `행사 ${selectedEventIds.length}개, 관광지 ${selectedTourIds.length}개를 선택했어요.`,
    )

    router.push("/course/preview")
  }

  if (!course && !courseId && !candidateMode && !hasRecommendContext) {
    return <RecommendEmptyState />
  }

  const coursePlaces = getCoursePlaces(course)
  const area = course?.baseArea ?? areaParam ?? "지역 미정"
  const locationName = locationNameParam ?? area
  const companion = course?.companionType ?? companionParam ?? "동행 미정"
  const dateStr = course?.startDate ?? dateParam
  const formattedDate = formatDate(dateStr)
  const courseTitle = course?.title ?? `${locationName} 추천 코스`
  const courseDescription =
    course?.description ??
    "선택한 조건과 사용자 선호 정보를 기반으로 생성된 추천 코스입니다."
  const courseReason =
    course?.recommendationReason ??
    course?.reason ??
    "사용자 선호 지역, 카테고리, 행사 유사도를 함께 고려해 추천했어요."
  const displayCourseId = course?.id ?? course?.courseId ?? courseId

  return (
    <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6">
      <div className="flex flex-wrap items-center gap-2">
        <Badge variant="secondary" className="gap-1.5 px-3 py-1.5 text-sm">
          <CalendarDays className="size-3.5" />
          {formattedDate}
        </Badge>

        <Badge variant="secondary" className="gap-1.5 px-3 py-1.5 text-sm">
          <MapPin className="size-3.5" />
          {locationName}
        </Badge>

        <Badge variant="secondary" className="gap-1.5 px-3 py-1.5 text-sm">
          <Users className="size-3.5" />
          {companion}
        </Badge>

        {cats.map((category) => (
          <Badge
            key={category}
            variant="secondary"
            className="px-3 py-1.5 text-sm"
          >
            {category}
          </Badge>
        ))}

        <Button asChild variant="ghost" size="sm" className="ml-auto">
          <Link href="/plan">조건 수정</Link>
        </Button>
      </div>

      <h1 className="mt-6 text-balance text-3xl font-extrabold tracking-tight sm:text-4xl">
        {candidateMode
          ? "행사와 관광지 추천 후보를 확인해보세요"
          : course
            ? `${courseTitle}가 생성됐어요`
            : "선택한 조건으로 추천 코스를 확인해보세요"}
      </h1>

      <Card className="mt-6 flex-row flex-wrap items-start gap-4 bg-background p-5">
        <span className="flex size-11 items-center justify-center rounded-xl bg-secondary text-primary shadow-sm">
          <MapPin className="size-5" />
        </span>

        <div className="flex-1">
          <p className="font-semibold">선택된 위치</p>
          <p className="mt-1 text-lg font-bold">{locationName}</p>
          <p className="text-sm text-muted-foreground">
            {locationSource === "kakao" || locationSource === "naver"
              ? "지도 검색으로 선택한 출발 위치"
              : "기본 지역 선택"}
          </p>

          {locationAddress && (
            <p className="mt-2 text-sm text-muted-foreground">
              {locationAddress}
            </p>
          )}

          {latitude && longitude && (
            <p className="mt-1 text-xs text-muted-foreground">
              출발지 좌표: 위도 {latitude}, 경도 {longitude}
            </p>
          )}
        </div>
      </Card>

      {isLoadingCourse && !course && (
        <Card className="mt-6 p-5 text-sm text-muted-foreground">
          생성된 코스를 불러오는 중입니다.
        </Card>
      )}

      {courseLoadError && !course && (
        <Card className="mt-6 border-destructive/30 bg-destructive/5 p-5">
          <p className="font-semibold text-destructive">
            생성된 코스를 불러오지 못했어요.
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            {courseLoadError} 백엔드 서버 실행 상태와 API 주소를 확인해주세요.
          </p>
        </Card>
      )}

      <section className="mt-10">
        <h2 className="text-2xl font-bold tracking-tight">추천 하루 코스</h2>

        <Card className="mt-4 p-6">
          <div className="flex flex-col gap-5">
            <div>
              <h3 className="text-xl font-bold">{courseTitle}</h3>
              <p className="mt-1 leading-relaxed text-muted-foreground">
                {candidateMode
                  ? "마음에 드는 행사와 관광지를 선택한 뒤 최종 코스로 저장할 수 있어요."
                  : courseDescription}
              </p>
            </div>

            <div className="flex flex-wrap gap-4 text-sm">
              <span className="flex items-center gap-1.5">
                <Route className="size-4 text-primary" />
                {coursePlaces.length}개 장소
              </span>

              <span className="flex items-center gap-1.5">
                <Clock className="size-4 text-primary" />
                {course?.startDate && course?.endDate
                  ? `${course.startDate} ~ ${course.endDate}`
                  : formattedDate}
              </span>

              <span className="flex items-center gap-1.5">
                <MapPin className="size-4 text-primary" />
                {area}
              </span>
            </div>

            <div className="rounded-2xl bg-secondary/50 p-4">
              <p className="mb-2 flex items-center gap-1.5 text-sm font-semibold">
                <Sparkles className="size-4 text-accent" />
                이 코스를 추천하는 이유
              </p>
              <p className="text-sm leading-relaxed text-muted-foreground">
                {courseReason}
              </p>
            </div>
            {candidateMode ? (
              <div className="flex flex-wrap items-center gap-3">
                <Button
                  type="button"
                  className="gap-2"
                  onClick={saveSelectedCandidates}
                >
                  선택한 장소 확정
                  <Check className="size-4" />
                </Button>

                <Button asChild variant="outline" className="gap-2">
                  <Link href="/plan">
                    조건 다시 선택하기
                    <ArrowRight className="size-4" />
                  </Link>
                </Button>

                {selectionMessage && (
                  <p className="w-full text-sm text-muted-foreground">
                    {selectionMessage}
                  </p>
                )}
              </div>
            ) : displayCourseId ? (
              <div className="flex flex-wrap gap-3">
                <Button asChild className="gap-2">
                  <Link href={`/course/${displayCourseId}`}>
                    코스 상세 · 지도 보기
                    <ArrowRight className="size-4" />
                  </Link>
                </Button>

                <Button asChild variant="outline" className="gap-2">
                  <Link href="/course/preview">
                    식당 · 카페 추천
                    <ArrowRight className="size-4" />
                  </Link>
                </Button>
              </div>
            ) : (
              <Button asChild className="w-fit gap-2">
                <Link href="/plan">
                  코스 다시 생성하기
                  <ArrowRight className="size-4" />
                </Link>
              </Button>
            )}
          </div>
        </Card>
      </section>

      <section className="mt-12">
        <h2 className="text-2xl font-bold tracking-tight">
          {candidateMode ? "추천 후보 장소" : "생성된 코스 장소"}
        </h2>
        <p className="mt-1 text-muted-foreground">
          {candidateMode
            ? "행사와 관광지 후보를 함께 비교해 원하는 장소를 선택하세요."
            : "추천 알고리즘으로 생성된 코스의 방문 순서입니다."}
        </p>
        {candidateMode && (
          <p className="mt-2 text-sm font-medium text-primary">
            현재 선택: 행사 {selectedEventIds.length}개 · 관광지 {selectedTourIds.length}개
          </p>
        )}

        {coursePlaces.length > 0 ? (
          <div className="mt-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {coursePlaces
              .slice()
              .sort((a, b) => getVisitOrder(a, 0) - getVisitOrder(b, 0))
              .map((place, index) => (
                <Card
                  key={getPlaceKey(place, index)}
                  className={`p-5 transition-colors ${
                    candidateMode && isCandidateSelected(place)
                      ? "border-primary bg-primary/5"
                      : ""
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <Badge className="shrink-0">
                      {getVisitOrder(place, index)}번째
                    </Badge>
                    <div className="flex items-center gap-2">
                      <Badge variant="secondary">{getPlaceCategory(place)}</Badge>
                      {candidateMode && (
                        <Button
                          type="button"
                          size="sm"
                          variant={isCandidateSelected(place) ? "default" : "outline"}
                          className="shrink-0"
                          onClick={() => toggleCandidateSelection(place)}
                        >
                          {isCandidateSelected(place) ? "선택됨" : "선택"}
                        </Button>
                      )}
                    </div>
                  </div>

                  <h3 className="mt-4 text-lg font-bold leading-tight">
                    {getPlaceTitle(place)}
                  </h3>

                  <div className="mt-3 space-y-1.5 text-sm text-muted-foreground">
                    {place.area && (
                      <p className="flex items-center gap-1.5">
                        <MapPin className="size-3.5" />
                        {place.area}
                      </p>
                    )}

                    {place.address && (
                      <p className="flex items-center gap-1.5">
                        <MapPin className="size-3.5" />
                        {place.address}
                      </p>
                    )}

                    {place.latitude && place.longitude && (
                      <p className="text-xs">
                        {getCoordinateLabel(place)}: 위도 {place.latitude}, 경도{" "}
                        {place.longitude}
                      </p>
                    )}
                  </div>

                  <div className="mt-4 rounded-2xl bg-secondary/50 p-4">
                    <p className="mb-1 flex items-center gap-1.5 text-sm font-semibold">
                      <Sparkles className="size-4 text-accent" />
                      추천 이유
                    </p>
                    <p className="text-sm leading-relaxed text-muted-foreground">
                      {getPlaceReason(place)}
                    </p>
                  </div>
                </Card>
              ))}
          </div>
        ) : (
          <Card className="mt-5 p-6 text-center text-muted-foreground">
            아직 표시할 코스 장소가 없습니다. 조건을 다시 선택해 코스를
            생성해주세요.
          </Card>
        )}
      </section>
    </main>
  )
}