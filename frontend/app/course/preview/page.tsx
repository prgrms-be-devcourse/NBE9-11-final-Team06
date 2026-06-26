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
import { SimpleNaverMap } from "@/components/simple-naver-map"

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080"

type PlaceItem = {
  id: number
  name: string
  address: string
  latitude: number | null
  longitude: number | null
  url?: string | null
  placeUrl?: string | null
}

type NearbyPlaceResponse = {
  itemType: "EVENT" | "TOUR"
  itemId: number
  title: string
  latitude: number | null
  longitude: number | null
  restaurants: PlaceItem[]
  cafes: PlaceItem[]
}

type CoursePreviewResponse = {
  eventIds: number[]
  tourIds: number[]
  events: NearbyPlaceResponse[]
}

type SelectedRecommendationItems = {
  eventIds?: number[]
  tourIds?: number[]
  startDate?: string | null
  endDate?: string | null
  baseArea?: string | null
  companionType?: string | null
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

function getAccessToken() {
  if (typeof window === "undefined") return null

  const storages = [localStorage, sessionStorage]
  const keys = ["accessToken", "access_token", "token", "jwt"]

  for (const storage of storages) {
    for (const key of keys) {
      const token = normalizeAccessToken(storage.getItem(key))
      if (token) return token
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

function getPlaceUrl(place: PlaceItem) {
  return place.url ?? place.placeUrl ?? null
}

function readSelectedRecommendationItems(): SelectedRecommendationItems | null {
  if (typeof window === "undefined") return null

  const saved = sessionStorage.getItem("selectedRecommendationItems")
  if (!saved) return null

  try {
    const parsed = JSON.parse(saved) as SelectedRecommendationItems

    return {
      eventIds: Array.isArray(parsed.eventIds)
        ? parsed.eventIds
            .map((id) => Number(id))
            .filter((id) => Number.isFinite(id) && id > 0)
        : [],
      tourIds: Array.isArray(parsed.tourIds)
        ? parsed.tourIds
            .map((id) => Number(id))
            .filter((id) => Number.isFinite(id) && id > 0)
        : [],
      startDate: parsed.startDate ?? null,
      endDate: parsed.endDate ?? null,
      baseArea: parsed.baseArea ?? null,
      companionType: parsed.companionType ?? null,
      startLatitude: parsed.startLatitude ?? null,
      startLongitude: parsed.startLongitude ?? null,
    }
  } catch {
    return null
  }
}

export default function CoursePreviewPage() {
  const [course, setCourse] = useState<CoursePreviewResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [selectedRestaurantId, setSelectedRestaurantId] = useState<number | null>(null)
  const [selectedCafeId, setSelectedCafeId] = useState<number | null>(null)
  const [request, setRequest] = useState<any>(null)
  const [mapLoaded, setMapLoaded] = useState(false)
  const [selectedEventIds, setSelectedEventIds] = useState<number[]>([])
  const [selectedTourIds, setSelectedTourIds] = useState<number[]>([])
  const [selectedItems, setSelectedItems] =
    useState<SelectedRecommendationItems | null>(null)

  function toggleRestaurant(id: number) {
    setSelectedRestaurantId((prev) => (prev === id ? null : id))
  }

  function toggleCafe(id: number) {
    setSelectedCafeId((prev) => (prev === id ? null : id))
  }

  useEffect(() => {
    const savedItems = readSelectedRecommendationItems()

    if (!savedItems) return

    setSelectedItems(savedItems)
    setSelectedEventIds(savedItems.eventIds ?? [])
    setSelectedTourIds(savedItems.tourIds ?? [])
  }, [])

  useEffect(() => {
    let isMounted = true

    async function fetchCourse() {
      setIsLoading(true)
      setErrorMessage(null)

      try {
        const search = typeof window !== "undefined" ? window.location.search : ""
        const params = new URLSearchParams(search)

        const requestFromUrl = params.get("request")
        const localRequest = localStorage.getItem("coursePreviewRequest")
        const rawRequest = requestFromUrl || localRequest
        const savedItems = readSelectedRecommendationItems()

        let legacyRequest: any = {}

        if (rawRequest) {
          try {
            legacyRequest = JSON.parse(rawRequest)
          } catch (error) {
            console.error("request JSON 깨짐:", rawRequest, error)
            setErrorMessage("추천 조건 정보가 깨졌습니다.")
            return
          }
        }

        if (!rawRequest && !savedItems) {
          setErrorMessage("선택한 추천 장소 정보를 찾을 수 없습니다.")
          return
        }

        const {
          categories: _categories,
          categoryIds: _categoryIds,
          ...requestWithoutRecommendationFilters
        } = legacyRequest

        const requestBody = {
          ...requestWithoutRecommendationFilters,
          eventIds: savedItems?.eventIds ?? legacyRequest.eventIds ?? [],
          tourIds: savedItems?.tourIds ?? legacyRequest.tourIds ?? [],
          startDate: savedItems?.startDate ?? legacyRequest.startDate ?? null,
          endDate: savedItems?.endDate ?? legacyRequest.endDate ?? null,
          baseArea: savedItems?.baseArea ?? legacyRequest.baseArea ?? null,
          companionType:
            savedItems?.companionType ?? legacyRequest.companionType ?? null,
          startLatitude:
            savedItems?.startLatitude ?? legacyRequest.startLatitude ?? null,
          startLongitude:
            savedItems?.startLongitude ?? legacyRequest.startLongitude ?? null,
        }

        setRequest(requestBody)

        const accessToken = getAccessToken()

        const headers: HeadersInit = {
          "Content-Type": "application/json",
          ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
        }

        const response = await fetch(`${API_BASE_URL}/api/courses/preview`, {
          method: "POST",
          credentials: "include",
          headers,
          body: JSON.stringify(requestBody),
        })

        const text = await response.text()

        console.log("preview requestBody:", requestBody)
        console.log("preview status:", response.status)
        console.log("preview raw body:", text.substring(0, 500))

        let result = null

        try {
          result = text ? JSON.parse(text) : null
        } catch (error) {
          console.error("json parse 실패", error)
        }

        if (!response.ok) {
          setErrorMessage("코스 상세 정보를 불러오지 못했습니다.")
          return
        }

        const fetched = extractCourse(result)

        console.log("preview fetched:", fetched)
        console.log("preview nearby places:", fetched?.events)

        if (isMounted) {
          setCourse(fetched)
        }
      } catch (error) {
        console.error(error)
        setErrorMessage("코스 상세 정보를 불러오지 못했습니다.")
      } finally {
        if (isMounted) {
          setIsLoading(false)
        }
      }
    }

    fetchCourse()

    return () => {
      isMounted = false
    }
  }, [])

  useEffect(() => {
    const scriptId = "naver-map-script"

    if (document.getElementById(scriptId)) {
      setMapLoaded(true)
      return
    }

    const script = document.createElement("script")
    script.id = scriptId
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${process.env.NEXT_PUBLIC_NAVER_MAP_CLIENT_ID}`
    script.async = true

    script.onload = () => {
      console.log("NAVER MAP LOADED")
      setMapLoaded(true)
    }

    script.onerror = () => {
      console.error("NAVER MAP LOAD FAILED")
    }

    document.head.appendChild(script)
  }, [])

  const restaurants = Array.from(
    new Map(
      (course?.events ?? [])
        .flatMap((event) => event.restaurants ?? [])
        .map((restaurant) => [restaurant.id, restaurant]),
    ).values(),
  )

  const cafes = Array.from(
    new Map(
      (course?.events ?? [])
        .flatMap((event) => event.cafes ?? [])
        .map((cafe) => [cafe.id, cafe]),
    ).values(),
  )

  const points = [
    ...((course?.events ?? []).map((place, index) => ({
      id: `${place.itemType}-${place.itemId}`,
      title: place.title,
      latitude: place.latitude,
      longitude: place.longitude,
      order: index,
      type: place.itemType === "TOUR" ? "tour" : "event",
    })) ?? []),

    ...restaurants.map((restaurant, index) => ({
      id: restaurant.id,
      title: restaurant.name,
      latitude: restaurant.latitude,
      longitude: restaurant.longitude,
      order: index + 1,
      type: "restaurant",
    })),

    ...cafes.map((cafe, index) => ({
      id: cafe.id,
      title: cafe.name,
      latitude: cafe.latitude,
      longitude: cafe.longitude,
      order: restaurants.length + index + 1,
      type: "cafe",
    })),
  ].filter((point) => point.latitude !== null && point.longitude !== null)

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
            코스 상세 정보를 불러오는 중입니다.
          </Card>
        )}

        {errorMessage && !course && (
          <Card className="border-destructive/30 bg-destructive/5 p-6">
            <p className="font-semibold text-destructive">{errorMessage}</p>
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

              {selectedEventIds.length > 0 && (
                <Badge variant="secondary" className="gap-1.5 px-3 py-1.5 text-sm">
                  행사 {selectedEventIds.length}개 선택됨
                </Badge>
              )}

              {selectedTourIds.length > 0 && (
                <Badge variant="secondary" className="gap-1.5 px-3 py-1.5 text-sm">
                  관광지 {selectedTourIds.length}개 선택됨
                </Badge>
              )}
            </div>

            <h1 className="mt-6 text-3xl font-extrabold">추천 코스 상세</h1>


            <div className="mt-6">
              {mapLoaded ? (
                <SimpleNaverMap
                  points={points}
                  selectedRestaurantId={selectedRestaurantId}
                  selectedCafeId={selectedCafeId}
                  onSelect={(point) => {
                    if (point.type === "restaurant") {
                      setSelectedRestaurantId(point.id)
                    }

                    if (point.type === "cafe") {
                      setSelectedCafeId(point.id)
                    }
                  }}
                />
              ) : (
                <Card className="p-4 text-sm text-muted-foreground">
                  지도 로딩 중...
                </Card>
              )}
            </div>

            <p className="mt-3 text-muted-foreground">
              선택한 행사·관광지 주변의 맛집과 카페를 골라 최종 코스를 완성하세요.
            </p>

            <section className="mt-10">
              <h2 className="text-2xl font-bold">🍽️ 추천 맛집</h2>

              <div className="mt-5 space-y-4">
                {restaurants.map((place) => {
                  const placeUrl = getPlaceUrl(place)

                  return (
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

                      <p className="mt-2 flex items-center gap-2 text-sm text-muted-foreground">
                        <MapPin className="size-3.5" />
                        {place.address}
                      </p>

                      {placeUrl && (
                        <a
                          href={placeUrl}
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
                  )
                })}
              </div>
            </section>

            <section className="mt-12">
              <h2 className="text-2xl font-bold">☕ 추천 카페</h2>

              <div className="mt-5 space-y-4">
                {cafes.map((place) => {
                  const placeUrl = getPlaceUrl(place)

                  return (
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

                      <p className="mt-2 flex items-center gap-2 text-sm text-muted-foreground">
                        <MapPin className="size-3.5" />
                        {place.address}
                      </p>

                      {placeUrl && (
                        <a
                          href={placeUrl}
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
                  )
                })}
              </div>
            </section>

            <Button
              className="mt-10 w-full"
              disabled={!selectedRestaurantId || !selectedCafeId}
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

                const eventIds = selectedEventIds.length > 0
                  ? selectedEventIds
                  : course.eventIds ?? []

                const tourIds = selectedTourIds.length > 0
                  ? selectedTourIds
                  : course.tourIds ?? []

                const payload = {
                  title: "추천 코스",
                  description: "선택한 행사와 관광지, 맛집과 카페를 함께 즐기는 코스",

                  courseType: request.courseType ?? "RECOMMENDED",
                  startDate: request.startDate,
                  endDate: request.endDate,
                  baseArea: request.baseArea,
                  companionType: request.companionType,

                  eventIds,
                  tourIds,

                  restaurantId: selectedRestaurant.id,
                  cafeId: selectedCafe.id,

                  startLatitude: request.startLatitude ?? selectedItems?.startLatitude ?? null,
                  startLongitude: request.startLongitude ?? selectedItems?.startLongitude ?? null,
                }

                try {
                  const accessToken = getAccessToken()

                  console.log("create course payload:", payload)

                  const response = await fetch(`${API_BASE_URL}/api/courses`, {
                    method: "POST",
                    credentials: "include",
                    headers: {
                      "Content-Type": "application/json",
                      ...(accessToken
                        ? { Authorization: `Bearer ${accessToken}` }
                        : {}),
                    },
                    body: JSON.stringify(payload),
                  })

                  const result = await response.json().catch(() => null)

                  console.log("create course status:", response.status)
                  console.log("create course response:", result)

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
                        "코스 추천 생성에 실패했습니다.",
                    )
                  }

                  const courseId = result?.data

                  if (!courseId) {
                    alert("courseId 없음")
                    return
                  }

                  sessionStorage.removeItem("selectedRecommendationItems")
                  sessionStorage.removeItem("recommendationCandidates")

                  window.location.href = `/course/${courseId}`
                } catch (error) {
                  console.error(error)
                  alert("코스 생성 중 에러")
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