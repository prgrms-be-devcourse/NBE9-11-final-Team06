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
  url?: string // 👈 placeUrl -> url로 통일
}

type CoursePreviewResponse = {
  eventIds: number[]
  restaurants: PlaceItem[]
  cafes: PlaceItem[]
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
  return result?.data ?? null
}

/* ---------------- page ---------------- */

export default function CourseDetailPage() {
  const [course, setCourse] = useState<CoursePreviewResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  useEffect(() => {
    let isMounted = true

    async function fetchCourse() {
      setIsLoading(true)
      setErrorMessage(null)

      try {
        const accessToken = getAccessToken()

        const res = await fetch(
          `${API_BASE_URL}/api/courses/preview`,
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              ...(accessToken
                ? { Authorization: `Bearer ${accessToken}` }
                : {}),
            },
            body: JSON.stringify({
              courseType: "DAILY",
              startDate: "2026-06-15",
              endDate: "2026-06-20",
              baseArea: "강동구",
              companionType: "FRIEND",
              restaurantType: "JAPANESE",
            }),
          }
        )

        
        const result = await res.json().catch(() => null)
        console.log("status:", res.status)
        console.log("ok:", res.ok)
        console.log("result:", result)

        if (!res.ok) {
          setErrorMessage("코스 상세 정보를 불러오지 못했습니다.")
          return
        }

        const fetched = extractCourse(result)

        if (isMounted) setCourse(fetched)
      } catch {
        setErrorMessage("코스 상세 정보를 불러오지 못했습니다.")
      } finally {
        if (isMounted) setIsLoading(false)
      }


    }

    fetchCourse()

    return () => {
      isMounted = false
    }
  }, [])

  const restaurants = course?.restaurants ?? []
  const cafes = course?.cafes ?? []

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

            <p className="mt-3 text-muted-foreground">
              맛집과 카페가 함께 구성된 추천 일정입니다.
            </p>

            {/* restaurants */}
            <section className="mt-10">
              <h2 className="text-2xl font-bold">🍽️ 추천 맛집</h2>

              <div className="mt-5 space-y-4">
                {restaurants.map((place) => (
                  <Card key={place.id} className="p-5">
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
                  <Card key={place.id} className="p-5">
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
          </>
        )}
      </main>

      <SiteFooter />
    </div>
  )
}