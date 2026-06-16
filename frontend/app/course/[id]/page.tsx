"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useParams } from "next/navigation"
import {
  ArrowLeft,
  CalendarDays,
  MapPin,
  Route,
  Sparkles,
  Users,
} from "lucide-react"
import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

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
  return place.recommendationReason ?? place.reason ?? place.memo ?? "사용자 선호 정보와 행사 유사도를 기반으로 추천되었습니다."
}

function getVisitOrder(place: CoursePlace, index: number) {
  return place.visitOrder ?? place.sequence ?? place.order ?? index + 1
}

export default function CourseDetailPage() {
  const params = useParams<{ id: string }>()
  const courseId = params.id
  const [course, setCourse] = useState<CourseDetail | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  useEffect(() => {
    const savedCourse = getSavedRecommendedCourse(courseId)
    if (savedCourse) {
      setCourse(savedCourse)
    }

    async function fetchCourse() {
      setIsLoading(true)
      setErrorMessage(null)

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

        const result = await response.json().catch(() => null)

        if (!response.ok) {
          if (!savedCourse) {
            setErrorMessage("코스 상세 정보를 불러오지 못했습니다.")
          }
          return
        }

        const fetchedCourse = extractCourse(result)
        setCourse(fetchedCourse)
      } catch {
        if (!savedCourse) {
          setErrorMessage("코스 상세 정보를 불러오지 못했습니다.")
        }
      } finally {
        setIsLoading(false)
      }
    }

    if (courseId) {
      fetchCourse()
    }
  }, [courseId])

  const coursePlaces = getCoursePlaces(course)
  const title = course?.title ?? "코스 상세"
  const description = course?.description ?? "추천 알고리즘으로 생성된 코스입니다."
  const startDate = formatDate(course?.startDate)
  const endDate = formatDate(course?.endDate)
  const baseArea = course?.baseArea ?? "지역 미정"
  const companion = course?.companionType ?? "동행 미정"
  const recommendationReason = course?.recommendationReason ?? course?.reason ?? "사용자 선호 지역, 카테고리, 행사 유사도를 함께 고려해 추천했어요."

  return (
    <div className="flex min-h-screen flex-col">
      <SiteHeader />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6">
        <Button asChild variant="ghost" size="sm" className="mb-6 gap-2">
          <Link href="/recommend">
            <ArrowLeft className="size-4" />
            추천 결과로 돌아가기
          </Link>
        </Button>

        {isLoading && !course && (
          <Card className="p-6 text-sm text-muted-foreground">
            코스 상세 정보를 불러오는 중입니다.
          </Card>
        )}

        {errorMessage && !course && (
          <Card className="border-destructive/30 bg-destructive/5 p-6">
            <p className="font-semibold text-destructive">{errorMessage}</p>
            <p className="mt-2 text-sm text-muted-foreground">
              courseId={courseId} 조회에 실패했습니다. 백엔드 서버 실행 상태와 API 주소를 확인해주세요.
            </p>
          </Card>
        )}

        {course && (
          <>
            <div className="flex flex-wrap items-center gap-2">
              <Badge variant="secondary" className="gap-1.5 px-3 py-1.5 text-sm">
                <CalendarDays className="size-3.5" />
                {course?.startDate && course?.endDate ? `${startDate} ~ ${endDate}` : startDate}
              </Badge>
              <Badge variant="secondary" className="gap-1.5 px-3 py-1.5 text-sm">
                <MapPin className="size-3.5" />
                {baseArea}
              </Badge>
              <Badge variant="secondary" className="gap-1.5 px-3 py-1.5 text-sm">
                <Users className="size-3.5" />
                {companion}
              </Badge>
              <Badge variant="secondary" className="gap-1.5 px-3 py-1.5 text-sm">
                <Route className="size-3.5" />
                {coursePlaces.length}개 장소
              </Badge>
            </div>

            <h1 className="mt-6 text-balance text-3xl font-extrabold tracking-tight sm:text-4xl">
              {title}
            </h1>
            <p className="mt-3 max-w-3xl leading-relaxed text-muted-foreground">
              {description}
            </p>

            <Card className="mt-6 p-6">
              <p className="mb-2 flex items-center gap-1.5 font-semibold">
                <Sparkles className="size-4 text-accent" />
                이 코스를 추천하는 이유
              </p>
              <p className="text-sm leading-relaxed text-muted-foreground">
                {recommendationReason}
              </p>
            </Card>

            <section className="mt-10">
              <h2 className="text-2xl font-bold tracking-tight">방문 순서</h2>
              <p className="mt-1 text-muted-foreground">
                실제 생성된 코스의 CoursePlace 목록입니다.
              </p>

              {coursePlaces.length > 0 ? (
                <div className="mt-5 grid gap-5 lg:grid-cols-[1fr_360px]">
                  <div className="space-y-4">
                    {coursePlaces
                      .slice()
                      .sort((a, b) => getVisitOrder(a, 0) - getVisitOrder(b, 0))
                      .map((place, index) => (
                        <Card key={`${place.placeId ?? place.eventId ?? index}`} className="p-5">
                          <div className="flex flex-wrap items-start justify-between gap-3">
                            <div>
                              <Badge>{getVisitOrder(place, index)}번째</Badge>
                              <h3 className="mt-3 text-xl font-bold leading-tight">
                                {getPlaceTitle(place)}
                              </h3>
                            </div>
                            <Badge variant="secondary">{getPlaceCategory(place)}</Badge>
                          </div>

                          <div className="mt-4 grid gap-2 text-sm text-muted-foreground sm:grid-cols-2">
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
                              <p className="flex items-center gap-1.5">
                                <MapPin className="size-3.5" />
                                위도 {place.latitude}, 경도 {place.longitude}
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

                  <Card className="h-fit p-5">
                    <p className="mb-3 flex items-center gap-1.5 font-semibold">
                      <MapPin className="size-4 text-primary" />
                      지도 보기
                    </p>
                    <div className="flex aspect-square items-center justify-center rounded-2xl bg-secondary/60 text-center text-sm text-muted-foreground">
                      지도 영역입니다.
                      <br />
                      추후 Naver Map 마커 연동 가능
                    </div>
                  </Card>
                </div>
              ) : (
                <Card className="mt-5 p-6 text-center text-muted-foreground">
                  아직 표시할 코스 장소가 없습니다.
                </Card>
              )}
            </section>
          </>
        )}
      </main>
      <SiteFooter />
    </div>
  )
}
