"use client"

import { useEffect, useState } from "react"
import {
  ArrowLeft,
  MapPin,
  Route,
  Users,
  ExternalLink,
} from "lucide-react"

import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080"

type PlaceItem = {
  id: number
  name: string
  address: string | null
  latitude: number | null
  longitude: number | null
  url?: string | null
}

type CoursePreviewResponse = {
  eventIds: number[]
  restaurants: PlaceItem[]
  cafes: PlaceItem[]
  startLatitude?: number | null
  startLongitude?: number | null
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

  if (directToken) {
    return directToken
  }

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
      if (rawToken?.startsWith("eyJ")) {
        return rawToken
      }

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

function extractCourse(result: any): CoursePreviewResponse | null {
  return (
    result?.data ??
    result?.result ??
    result?.body ??
    result?.content ??
    result?.response ??
    result ??
    null
  )
}

function extractCreatedCourseId(result: any): number | string | null {
  if (result === null || result === undefined) {
    return null
  }

  if (typeof result === "number" || typeof result === "string") {
    return result
  }

  if (typeof result !== "object") {
    return null
  }

  if (result.data !== undefined && result.data !== null) {
    return extractCreatedCourseId(result.data)
  }

  if (result.courseId !== undefined && result.courseId !== null) {
    return result.courseId
  }

  if (result.id !== undefined && result.id !== null) {
    return result.id
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

function readRecommendationCategoriesFromStorage(): string[] {
  try {
    const categories = JSON.parse(
      localStorage.getItem("recommendationCategories") ?? "[]",
    )

    return Array.isArray(categories)
      ? categories.filter((category) => typeof category === "string")
      : []
  } catch {
    return []
  }
}

export default function CoursePreviewPage() {
  const [course, setCourse] = useState<CoursePreviewResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [selectedRestaurantId, setSelectedRestaurantId] = useState<number | null>(null)
  const [selectedCafeId, setSelectedCafeId] = useState<number | null>(null)
  const [request, setRequest] = useState<any>(null)

  function toggleRestaurant(id: number) {
    setSelectedRestaurantId((prev) => (prev === id ? null : id))
  }

  function toggleCafe(id: number) {
    setSelectedCafeId((prev) => (prev === id ? null : id))
  }

  useEffect(() => {
    let isMounted = true

    async function fetchPreview() {
      setIsLoading(true)
      setErrorMessage(null)

      try {
        const search = typeof window !== "undefined" ? window.location.search : ""
        const params = new URLSearchParams(search)

        const requestFromUrl = params.get("request")
        const localRequest = localStorage.getItem("coursePreviewRequest")

        const rawRequest = requestFromUrl || localRequest

        if (!rawRequest) {
          setErrorMessage("추천 조건 정보를 찾을 수 없습니다.")
          return
        }

        let requestBody: any

        try {
          requestBody = JSON.parse(rawRequest)
        } catch {
          console.error("request JSON 깨짐:", rawRequest)
          setErrorMessage("추천 조건 정보가 깨졌습니다.")
          return
        }

        const categoriesFromStorage = readRecommendationCategoriesFromStorage()

        requestBody = {
          ...requestBody,
          categories:
            Array.isArray(requestBody.categories) && requestBody.categories.length > 0
              ? requestBody.categories
              : categoriesFromStorage,
        }

        setRequest(requestBody)

        const accessToken = getAccessToken()

        const headers: HeadersInit = {
          "Content-Type": "application/json; charset=UTF-8",
          ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
        }

        const previewResponse = await fetch(`${API_BASE_URL}/api/courses/preview`, {
          method: "POST",
          credentials: "include",
          headers,
          body: JSON.stringify(requestBody),
        })

        const previewResult = await readJsonSafely(previewResponse)

        console.log("course preview requestBody:", requestBody)
        console.log("course preview status:", previewResponse.status)
        console.log("course preview response:", previewResult)

        if (!previewResponse.ok) {
          setErrorMessage(
            previewResult?.message ?? "코스 프리뷰 정보를 불러오지 못했습니다.",
          )
          return
        }

        const fetchedCourse = extractCourse(previewResult)

        if (isMounted) {
          setCourse(fetchedCourse)
        }
      } catch (error) {
        console.error(error)
        setErrorMessage("코스 프리뷰 정보를 불러오지 못했습니다.")
      } finally {
        if (isMounted) {
          setIsLoading(false)
        }
      }
    }

    fetchPreview()

    return () => {
      isMounted = false
    }
  }, [])

  const restaurants = course?.restaurants ?? []
  const cafes = course?.cafes ?? []
  const eventCount = course?.eventIds?.length ?? 0

  return (
    <div className="flex min-h-screen flex-col">
      <SiteHeader />

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6">
        <Button
          variant="ghost"
          size="sm"
          className="mb-6 gap-2"
          onClick={() => {
            window.location.href = "/recommend"
          }}
        >
          <ArrowLeft className="size-4" />
          추천 결과로 돌아가기
        </Button>

        {isLoading && !course && (
          <Card className="p-6 text-sm text-muted-foreground">
            코스 프리뷰 정보를 불러오는 중입니다.
          </Card>
        )}

        {errorMessage && !course && (
          <Card className="border-destructive/30 bg-destructive/5 p-6">
            <p className="font-semibold text-destructive">
              {errorMessage}
            </p>
          </Card>
        )}

        {course && (
          <>
            <div className="flex flex-wrap items-center gap-2">
              <Badge variant="secondary" className="gap-1.5 px-3 py-1.5 text-sm">
                <Route className="size-3.5" />
                맛집 {restaurants.length}개 · 카페 {cafes.length}개
              </Badge>

              <Badge variant="secondary" className="gap-1.5 px-3 py-1.5 text-sm">
                <Users className="size-3.5" />
                추천 코스
              </Badge>

              <Badge variant="secondary" className="gap-1.5 px-3 py-1.5 text-sm">
                행사 {eventCount}개
              </Badge>
            </div>

            <h1 className="mt-6 text-3xl font-extrabold">
              코스 프리뷰
            </h1>

            <p className="mt-3 text-muted-foreground">
              선택한 조건에 맞는 행사, 맛집, 카페를 확인해보세요.
            </p>

            <section className="mt-12">
              <h2 className="text-2xl font-bold">🍽️ 추천 맛집</h2>

              {restaurants.length > 0 ? (
                <div className="mt-5 space-y-4">
                  {restaurants.map((place) => (
                    <Card
                      key={place.id}
                      onClick={() => toggleRestaurant(place.id)}
                      className={`cursor-pointer border p-5 transition ${
                        selectedRestaurantId === place.id
                          ? "border-blue-500 bg-blue-50"
                          : ""
                      }`}
                    >
                      <h3 className="text-xl font-bold">{place.name}</h3>

                      {place.address && (
                        <p className="mt-2 flex items-center gap-2 text-sm text-muted-foreground">
                          <MapPin className="size-3.5" />
                          {place.address}
                        </p>
                      )}

                      {place.latitude !== null && place.longitude !== null && (
                        <p className="mt-1 text-xs text-muted-foreground">
                          위도 {place.latitude}, 경도 {place.longitude}
                        </p>
                      )}

                      {place.url && (
                        <a
                          href={place.url}
                          target="_blank"
                          rel="noreferrer"
                          onClick={(event) => event.stopPropagation()}
                          className="mt-3 inline-flex items-center gap-1 text-sm text-blue-500"
                        >
                          카카오맵 보기
                          <ExternalLink className="size-3" />
                        </a>
                      )}
                    </Card>
                  ))}
                </div>
              ) : (
                <Card className="mt-5 p-6 text-sm text-muted-foreground">
                  추천 맛집이 없습니다.
                </Card>
              )}
            </section>

            <section className="mt-12">
              <h2 className="text-2xl font-bold">☕ 추천 카페</h2>

              {cafes.length > 0 ? (
                <div className="mt-5 space-y-4">
                  {cafes.map((place) => (
                    <Card
                      key={place.id}
                      onClick={() => toggleCafe(place.id)}
                      className={`cursor-pointer border p-5 transition ${
                        selectedCafeId === place.id
                          ? "border-green-500 bg-green-50"
                          : ""
                      }`}
                    >
                      <h3 className="text-xl font-bold">{place.name}</h3>

                      {place.address && (
                        <p className="mt-2 flex items-center gap-2 text-sm text-muted-foreground">
                          <MapPin className="size-3.5" />
                          {place.address}
                        </p>
                      )}

                      {place.latitude !== null && place.longitude !== null && (
                        <p className="mt-1 text-xs text-muted-foreground">
                          위도 {place.latitude}, 경도 {place.longitude}
                        </p>
                      )}

                      {place.url && (
                        <a
                          href={place.url}
                          target="_blank"
                          rel="noreferrer"
                          onClick={(event) => event.stopPropagation()}
                          className="mt-3 inline-flex items-center gap-1 text-sm text-blue-500"
                        >
                          카카오맵 보기
                          <ExternalLink className="size-3" />
                        </a>
                      )}
                    </Card>
                  ))}
                </div>
              ) : (
                <Card className="mt-5 p-6 text-sm text-muted-foreground">
                  추천 카페가 없습니다.
                </Card>
              )}
            </section>

            <Button
              className="mt-10 w-full"
              disabled={selectedRestaurantId === null || selectedCafeId === null}
              onClick={async () => {
                const selectedRestaurant = restaurants.find(
                  (restaurant) => restaurant.id === selectedRestaurantId,
                )

                const selectedCafe = cafes.find(
                  (cafe) => cafe.id === selectedCafeId,
                )

                if (!selectedRestaurant || !selectedCafe || !course || !request) {
                  return
                }

                const eventIds = (course.eventIds ?? [])
                  .map(Number)
                  .filter(Number.isFinite)

                const payload = {
                  title: "추천 코스",
                  description: "맛집과 카페를 함께 즐기는 코스",
                  courseType: request.courseType,
                  startDate: request.startDate,
                  endDate: request.endDate,
                  baseArea: request.baseArea,
                  companionType: request.companionType,
                  eventIds,
                  restaurantId: selectedRestaurant.id,
                  cafeId: selectedCafe.id,
                  startLatitude: request.startLatitude,
                  startLongitude: request.startLongitude,
                }

                try {
                  const accessToken = getAccessToken()

                  console.log("course create payload:", payload)

                  const response = await fetch(`${API_BASE_URL}/api/courses`, {
                    method: "POST",
                    credentials: "include",
                    headers: {
                      "Content-Type": "application/json; charset=UTF-8",
                      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
                    },
                    body: JSON.stringify(payload),
                  })

                  const result = await readJsonSafely(response)

                  console.log("course create status:", response.status)
                  console.log("course create response:", result)

                  if (
                    response.status === 0 ||
                    response.status === 302 ||
                    response.type === "opaqueredirect"
                  ) {
                    throw new Error(
                      "로그인 인증이 만료되었거나 토큰이 전달되지 않았습니다. 다시 로그인해주세요.",
                    )
                  }

                  if (!response.ok) {
                    throw new Error(
                      result?.message ??
                        result?.error ??
                        "코스 생성에 실패했습니다.",
                    )
                  }

                  const courseId = extractCreatedCourseId(result)

                  if (!courseId) {
                    alert("courseId 없음")
                    return
                  }

                  window.location.href = `/course/${courseId}`
                } catch (error) {
                  console.error(error)
                  alert(
                    error instanceof Error
                      ? error.message
                      : "코스 생성 중 에러가 발생했습니다.",
                  )
                }
              }}
            >
              선택 완료
            </Button>
          </>
        )}
      </main>

      <SiteFooter />
    </div>
  )
}