"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
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

/* ---------------- PlaceList ---------------- */

function PlaceList({
  title,
  icon,
  items,
  selectedId,
  onSelect,
  color,
}: any) {
  return (
    <section className="mt-10">
      <h2 className="text-2xl font-bold">{icon} {title}</h2>

      <div className="mt-5 space-y-4">
        {items.map((place: any) => (
          <Card
            key={place.id}
            onClick={() => onSelect(place.id)}
            className={`p-5 cursor-pointer transition border
              ${selectedId === place.id ? color : ""}`}
          >
            <h3 className="text-xl font-bold">{place.name}</h3>

            <p className="mt-2 flex items-center gap-2 text-sm text-muted-foreground">
              <MapPin className="size-3.5" />
              {place.address}
            </p>
          </Card>
        ))}
      </div>
    </section>
  )
}

/* ---------------- API ---------------- */

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080"

/* ---------------- types ---------------- */

type PlaceItem = {
  id: number
  name: string
  address: string
  latitude: number
  longitude: number
  url?: string
}

type EventNearbyPlaceResponse = {
  eventId: number
  eventTitle: string
  restaurants: PlaceItem[]
  cafes: PlaceItem[]
}

type CoursePreviewResponse = {
  eventIds: number[]
  events: EventNearbyPlaceResponse[]
}

/* ---------------- token ---------------- */

function normalizeAccessToken(value: string | null | undefined) {
  if (!value) return null
  const token = value.trim().replace(/^Bearer\s+/i, "")
  if (!token || token === "undefined" || token === "null") return null
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

/* ---------------- api helper ---------------- */

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
/* ---------------- page ---------------- */

export default function CourseDetailPage() {
  const [course, setCourse] = useState<CoursePreviewResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [selectedRestaurantId, setSelectedRestaurantId] = useState<number | null>(null)
  const [selectedCafeId, setSelectedCafeId] = useState<number | null>(null)
  const [request, setRequest] = useState<any>(null)
  const [mapLoaded, setMapLoaded] = useState(false)
  

  


  function toggleRestaurant(id: number) {
    setSelectedRestaurantId(prev => (prev === id ? null : id))
  }
  
  function toggleCafe(id: number) {
    setSelectedCafeId(prev => (prev === id ? null : id))
  }


  
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

        if (!rawRequest) {
          setErrorMessage("추천 조건 정보를 찾을 수 없습니다.")
          return
        }

        let requestBody: any

        try {
          requestBody = JSON.parse(rawRequest)
        } catch (e) {
          console.error("request JSON 깨짐:", rawRequest)
          setErrorMessage("추천 조건 정보가 깨졌습니다.")
          return
        }

        console.log("requestFromUrl =", requestFromUrl)
        
        setRequest(requestBody)


        const accessToken = getAccessToken()

        const headers: HeadersInit = {
          "Content-Type": "application/json",
          ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
        }

        const res = await fetch(`${API_BASE_URL}/api/courses/preview`, {
          method: "POST",
          credentials: "include",
          headers,
          body: JSON.stringify(requestBody),
        })
  
        const text = await res.text()
        console.log("status:", res.status)
        console.log("content-type:", res.headers.get("content-type"))
        console.log("raw body:", text.substring(0, 500))
        console.log("requestBody:", requestBody)
        console.log("status:", res.status)
        console.log("raw body:", text.substring(0, 500))


  
        let result = null
  
        try {
          result = text ? JSON.parse(text) : null
        } catch (e) {
          console.error("json parse 실패", e)
        }
  
        if (!res.ok) {
          setErrorMessage("코스 상세 정보를 불러오지 못했습니다.")
          return
        }
  
        const fetched = extractCourse(result)
  
        console.log("fetched:", fetched)
        console.log("fetched:", fetched)

        // 🔥 여기다 찍어
        console.log("events:", fetched?.events)
        console.log(
          "event points:",
          fetched?.events?.map(e => ({
            id: e.eventId,
            title: e.eventTitle,
            lat: e.restaurants?.[0]?.latitude,
            lng: e.restaurants?.[0]?.longitude
          }))
        )

        console.log("event coords:", points.filter(p => p.type === "event"))
        if (isMounted) {
          setCourse(fetched)
        }
      } catch (e) {
        console.error(e)
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
    const script = document.createElement("script")
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${process.env.NEXT_PUBLIC_NAVER_MAP_CLIENT_ID}`
    script.async = true
  
    script.onload = () => {
      console.log("NAVER MAP LOADED")
      setMapLoaded(true) // ⭐ 이거 필수
    }
  
    script.onerror = () => {
      console.error("NAVER MAP LOAD FAILED")
    }
  
    document.head.appendChild(script)
  }, [])



  const restaurants =
  Array.from(
    new Map(
      course?.events
        ?.flatMap(e => e.restaurants)
        .map(r => [r.id, r]) // 중복 제거
    ).values()
  ) ?? []

  const cafes =
  Array.from(
    new Map(
      course?.events
        ?.flatMap(e => e.cafes)
        .map(c => [c.id, c])
    ).values()
  ) ?? []

  const points = [
    ...(course?.events?.map((e, idx) => ({
      id: e.eventId ?? idx,
      title: e.eventTitle,
      latitude: e.restaurants?.[0]?.latitude ?? e.cafes?.[0]?.latitude,
      longitude: e.restaurants?.[0]?.longitude ?? e.cafes?.[0]?.longitude,
      order: idx,
      type: "event",
    })) ?? []),
  
    ...restaurants.map((r, i) => ({
      id : r.id,
      title: r.name,
      latitude: r.latitude,
      longitude: r.longitude,
      order: i + 1,
      type: "restaurant",
    })),
  
    ...cafes.map((c, i) => ({
      id : c.id,
      title: c.name,
      latitude: c.latitude,
      longitude: c.longitude,
      order: restaurants.length + i + 1,
      type: "cafe",
    })),
  ]

  return (
    <div className="flex min-h-screen flex-col">
      <SiteHeader />

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6">

        {/* ✅ FIX: nativeButton 충돌 제거 (Button + Link 분리) */}
        <Button
          variant="ghost"
          size="sm"
          className="mb-6 gap-2"
          onClick={() => window.location.href = "/recommend"}
        >
          <ArrowLeft className="size-4" />
          추천 결과로 돌아가기
        </Button>

        {/* loading */}
        {isLoading && !course && (
          <Card className="p-6 text-sm text-muted-foreground">
            코스 상세 정보를 불러오는 중입니다.
          </Card>
        )}

        {/* error */}
        {errorMessage && !course && (
          <Card className="border-destructive/30 bg-destructive/5 p-6">
            <p className="font-semibold text-destructive">
              {errorMessage}
            </p>
          </Card>
        )}

        {/* content */}
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
            </div>

            <h1 className="mt-6 text-3xl font-extrabold">
              추천 코스 상세
            </h1>

            {mapLoaded ? (
              <SimpleNaverMap
              points={points}
              selectedRestaurantId={selectedRestaurantId}
              selectedCafeId={selectedCafeId}
              onSelect={(p) => {
                if (p.type === "restaurant") {
                  setSelectedRestaurantId(p.id)
                }
            
                if (p.type === "cafe") {
                  setSelectedCafeId(p.id)
                }
              }}
            />
            ) : (
              <Card className="p-4 text-sm text-muted-foreground">
                지도 로딩 중...
              </Card>
            )}

            <p className="mt-3 text-muted-foreground">
              맛집과 카페가 함께 구성된 추천 일정입니다.
            </p>

            {/* restaurants */}
            <section className="mt-10">
              <h2 className="text-2xl font-bold">🍽️ 추천 맛집</h2>

              <div className="mt-5 space-y-4">
              {restaurants.map((place) => (
              <Card
                key={place.id}
                onClick={() => toggleRestaurant(place.id)}
                className={`p-5 cursor-pointer transition border
                  ${selectedRestaurantId === place.id ? "border-blue-500 bg-blue-50" : ""}`}
              >
                <h3 className="text-xl font-bold">{place.name}</h3>

                <p className="mt-2 flex items-center gap-2 text-sm text-muted-foreground">
                  <MapPin className="size-3.5" />
                  {place.address}
                </p>

                {place.url && (
                  <a
                    href={place.url}
                    target="_blank"
                    rel="noreferrer"
                    className="mt-3 inline-flex items-center gap-1 text-sm text-blue-500"
                  >
                    카카오맵 보기
                    <ExternalLink className="size-3" />
                  </a>
                )}
              </Card>
            ))}
              </div>
            </section>

            {/* cafes */}
            <section className="mt-12">
              <h2 className="text-2xl font-bold">☕ 추천 카페</h2>

              <div className="mt-5 space-y-4">
                {cafes.map((place) => (
                  <Card
                  key={place.id}
                  onClick={() => toggleCafe(place.id)}
                  className={`p-5 cursor-pointer transition border
                    ${selectedCafeId === place.id ? "border-green-500 bg-green-50" : ""}`}
                >
                    <h3 className="text-xl font-bold">{place.name}</h3>

                    <p className="mt-2 flex items-center gap-2 text-sm text-muted-foreground">
                      <MapPin className="size-3.5" />
                      {place.address}
                    </p>

                    {place.url && (
                      <a
                        href={place.url}
                        target="_blank"
                        rel="noreferrer"
                        className="mt-3 inline-flex items-center gap-1 text-sm text-blue-500"
                      >
                        카카오맵 보기
                        <ExternalLink className="size-3" />
                      </a>
                    )}
                  </Card>
                ))}
              </div>
            </section>

            <Button
            className="mt-10 w-full"
            disabled={!selectedRestaurantId || !selectedCafeId}
            onClick={async () => {
              const selectedRestaurant = restaurants.find(
                r => r.id === selectedRestaurantId
              )
            
              const selectedCafe = cafes.find(
                c => c.id === selectedCafeId
              )
            
              if (!selectedRestaurant || !selectedCafe || !course) return
            
              const eventIds = (() => {
                try {
                  return JSON.parse(
                    localStorage.getItem("recommendedEventIds") ?? "[]"
                  )
                } catch {
                  return []
                }
              })()

              console.log("recommendedEventIds:", eventIds)


              // 🔥 핵심: preview 요청 기반 + 선택값 합치기
              const payload = {
                title: "추천 코스",
                description: "맛집과 카페를 함께 즐기는 코스",
              
                courseType: request.courseType,
                startDate: request.startDate,
                endDate: request.endDate,
                baseArea: request.baseArea,
                companionType: request.companionType,
              
                eventIds: eventIds,
              
                restaurantId: selectedRestaurant.id,
                cafeId: selectedCafe.id,

                startLatitude: request.startLatitude,
                startLongitude: request.startLongitude,
              }
            
              try {
                const accessToken = getAccessToken()
            
                console.log("payload:", payload)
                const response = await fetch(`${API_BASE_URL}/api/courses`, {
                  method: "POST",
                  credentials: "include",
                  headers: {
                    "Content-Type": "application/json",
                    ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
                  },
                  body: JSON.stringify(payload),
                })
                
                const result = await response.json().catch(() => null)
                
                console.log("status:", result.status)
                console.log("raw response:", result)
                console.log("RAW RESULT:", result)
                
                if (response.status === 0 || response.status === 302 || response.type === "opaqueredirect") {
                  throw new Error("로그인 인증이 만료되었거나 토큰이 전달되지 않았습니다. 다시 로그인해주세요.")
                }

                if (!response.ok) {
                  throw new Error(result?.message ?? result?.error ?? "코스 추천 생성에 실패했습니다.")
                }
                
                
                const courseId = result?.data
                
                if (!courseId) {
                  alert("courseId 없음")
                  return
                }
                
                window.location.href = `/course/${courseId}`
              } catch (e) {
                console.error(e)
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