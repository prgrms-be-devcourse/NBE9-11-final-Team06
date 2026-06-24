"use client"

import { Suspense, useEffect, useMemo, useState } from "react"
import Link from "next/link"
import { useSearchParams } from "next/navigation"
import {
  ArrowRight,
  CalendarDays,
  Clock,
  ExternalLink,
  Landmark,
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

type CoursePlace = {
  id?: number
  placeId?: number
  eventId?: number
  title?: string
  name?: string
  placeName?: string
  eventTitle?: string
  category?: string
  categoryName?: string
  area?: string
  address?: string
  latitude?: number | string
  longitude?: number | string
  visitOrder?: number
  sequence?: number
  order?: number
  memo?: string
  recommendationReason?: string
  reason?: string
}

type TourPlaceItem = {
  placeId: number
  title: string
  categoryName: string
  address: string | null
  latitude: number | null
  longitude: number | null
  url: string | null
  recommendationReason: string | null
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

function extractTourPlaces(result: any): TourPlaceItem[] {
  const data =
    result?.data ??
    result?.result ??
    result?.body ??
    result?.content ??
    result?.response ??
    result

  return Array.isArray(data) ? data : []
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
  return place.title ?? place.placeName ?? place.eventTitle ?? place.name ?? "추천 장소"
}

function getPlaceCategory(place: CoursePlace) {
  return place.categoryName ?? place.category ?? "추천"
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
  const [course, setCourse] = useState<CourseDetail | null>(null)
  const [isLoadingCourse, setIsLoadingCourse] = useState(false)
  const [courseLoadError, setCourseLoadError] = useState<string | null>(null)

  const [tourPlaces, setTourPlaces] = useState<TourPlaceItem[]>([])
  const [isTourLoading, setIsTourLoading] = useState(false)
  const [tourErrorMessage, setTourErrorMessage] = useState<string | null>(null)

  const courseId = searchParams.get("courseId") ?? undefined
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

    if (savedCourse) {
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

        const response = await fetch(`/api/courses/${courseId}`, {
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
  }, [courseId])

  useEffect(() => {
    const baseArea = course?.baseArea ?? areaParam ?? locationNameParam

    if (!hasRecommendContext || !baseArea) {
      return
    }

    let isMounted = true

    async function fetchTourPlaces() {
      setIsTourLoading(true)
      setTourErrorMessage(null)

      try {
        const accessToken = getAccessToken()

        const startLatitude = parseNullableNumber(latitude)
        const startLongitude = parseNullableNumber(longitude)

        const tourPlaceRequest = {
          area: baseArea,
          baseArea,
          topK: 3,
          categories: ["TOUR"],
          startLatitude,
          startLongitude,
        }

        const response = await fetch(
          `${API_BASE_URL}/api/recommendations/tour-places/preview`,
          {
            method: "POST",
            credentials: "include",
            headers: {
              "Content-Type": "application/json; charset=UTF-8",
              ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
            },
            body: JSON.stringify(tourPlaceRequest),
          },
        )

        const result = await readJsonSafely(response)

        console.log("recommend tour places requestBody:", tourPlaceRequest)
        console.log("recommend tour places status:", response.status)
        console.log("recommend tour places response:", result)

        if (!response.ok) {
          if (isMounted) {
            setTourErrorMessage(
              result?.message ?? "관광지 추천 정보를 불러오지 못했습니다.",
            )
          }
          return
        }

        const fetchedTourPlaces = extractTourPlaces(result)

        if (isMounted) {
          setTourPlaces(fetchedTourPlaces)
        }
      } catch (error) {
        console.error(error)

        if (isMounted) {
          setTourErrorMessage("관광지 추천 정보를 불러오지 못했습니다.")
        }
      } finally {
        if (isMounted) {
          setIsTourLoading(false)
        }
      }
    }

    fetchTourPlaces()

    return () => {
      isMounted = false
    }
  }, [
    course?.baseArea,
    areaParam,
    locationNameParam,
    latitude,
    longitude,
    hasRecommendContext,
  ])

  if (!course && !courseId && !hasRecommendContext) {
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
        {course
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
                {courseDescription}
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

            {displayCourseId ? (
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
        <h2 className="text-2xl font-bold tracking-tight">생성된 코스 장소</h2>
        <p className="mt-1 text-muted-foreground">
          추천 알고리즘으로 생성된 코스의 방문 순서입니다.
        </p>

        {coursePlaces.length > 0 ? (
          <div className="mt-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {coursePlaces
              .slice()
              .sort((a, b) => getVisitOrder(a, 0) - getVisitOrder(b, 0))
              .map((place, index) => (
                <Card
                  key={`${place.placeId ?? place.eventId ?? index}`}
                  className="p-5"
                >
                  <div className="flex items-start justify-between gap-3">
                    <Badge className="shrink-0">
                      {getVisitOrder(place, index)}번째
                    </Badge>
                    <Badge variant="secondary">{getPlaceCategory(place)}</Badge>
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
                        행사 좌표: 위도 {place.latitude}, 경도 {place.longitude}
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

      <section className="mt-12">
        <div className="flex items-center justify-between gap-3">
          <div>
            <h2 className="text-2xl font-bold tracking-tight">추천 관광지</h2>
            <p className="mt-1 text-muted-foreground">
              선택한 지역의 TOUR API 관광지 데이터를 기반으로 추천합니다.
            </p>
          </div>
        </div>

        {isTourLoading && (
          <Card className="mt-5 p-6 text-sm text-muted-foreground">
            관광지 추천 정보를 불러오는 중입니다.
          </Card>
        )}

        {tourErrorMessage && !isTourLoading && (
          <Card className="mt-5 border-destructive/30 bg-destructive/5 p-6">
            <p className="font-semibold text-destructive">
              {tourErrorMessage}
            </p>
          </Card>
        )}

        {!isTourLoading && !tourErrorMessage && tourPlaces.length > 0 && (
          <div className="mt-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {tourPlaces.map((place) => (
              <Card key={place.placeId} className="p-5">
                <div className="flex items-start justify-between gap-3">
                  <Badge variant="secondary">
                    {place.categoryName || "관광지"}
                  </Badge>

                  <Landmark className="size-5 shrink-0 text-primary" />
                </div>

                <h3 className="mt-4 text-lg font-bold leading-tight">
                  {place.title}
                </h3>

                <div className="mt-3 space-y-1.5 text-sm text-muted-foreground">
                  {place.address && (
                    <p className="flex items-start gap-1.5">
                      <MapPin className="mt-0.5 size-3.5 shrink-0" />
                      {place.address}
                    </p>
                  )}

                  {place.latitude !== null && place.longitude !== null && (
                    <p className="text-xs">
                      관광지 좌표: 위도 {place.latitude}, 경도 {place.longitude}
                    </p>
                  )}
                </div>

                <div className="mt-4 rounded-2xl bg-secondary/50 p-4">
                  <p className="mb-1 flex items-center gap-1.5 text-sm font-semibold">
                    <Sparkles className="size-4 text-accent" />
                    추천 이유
                  </p>
                  <p className="text-sm leading-relaxed text-muted-foreground">
                    {place.recommendationReason ??
                      "선택한 지역에 맞는 관광지입니다."}
                  </p>
                </div>

                {place.url && (
                  <a
                    href={place.url}
                    target="_blank"
                    rel="noreferrer"
                    className="mt-4 inline-flex items-center gap-1 text-sm text-blue-500"
                  >
                    상세 보기
                    <ExternalLink className="size-3" />
                  </a>
                )}
              </Card>
            ))}
          </div>
        )}

        {!isTourLoading && !tourErrorMessage && tourPlaces.length === 0 && (
          <Card className="mt-5 p-6 text-center text-muted-foreground">
            추천 관광지가 없습니다.
          </Card>
        )}
      </section>
    </main>
  )
}