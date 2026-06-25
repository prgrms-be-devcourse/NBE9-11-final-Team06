"use client"

import Image from "next/image"
import Link from "next/link"
import { useEffect, useState } from "react"
import {
  ArrowRight,
  CalendarDays,
  MapPin,
  Users,
  Sparkles,
  Activity,
  Route,
} from "lucide-react"
import { Card } from "@/components/ui/card"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { PlaceCard } from "@/components/place-card"
import { CrowdBadge } from "@/components/crowd-badge"
import { CATEGORIES, PLACES, SEOUL_AREAS } from "@/lib/data"

const STEPS = [
  {
    icon: CalendarDays,
    title: "조건을 골라요",
    desc: "날짜, 위치, 동행 유형, 취향 카테고리를 선택해요.",
  },
  {
    icon: Sparkles,
    title: "AI가 추천해요",
    desc: "운영 중인 행사와 주변 장소를 점수화해 코스를 만들어요.",
  },
  {
    icon: Route,
    title: "코스를 받아요",
    desc: "이동 동선과 추천 이유까지 담긴 하루 코스를 지도로 확인해요.",
  },
]

const FEATURES = [
  {
    icon: Activity,
    title: "실시간 혼잡도 반영",
    desc: "서울 실시간 도시데이터를 바탕으로 한산한 장소를 우선 추천해요.",
  },
  {
    icon: Users,
    title: "동행 맞춤 추천",
    desc: "혼자, 친구, 가족, 커플 등 동행 유형에 맞는 코스를 제안해요.",
  },
  {
    icon: MapPin,
    title: "지도 동선 안내",
    desc: "방문 순서와 이동 거리를 지도 경로로 한눈에 볼 수 있어요.",
  },
]

type SeoulAreaWithCrowd = (typeof SEOUL_AREAS)[number] & {
  populationMin: number | null
  populationMax: number | null
}

type CrowdApiResponse = {
  areaName: string
  congestionText: string
  populationMin: number | null
  populationMax: number | null
}

const CROWD_API_AREA_NAMES: Record<string, string> = {
  성수: "성수카페거리",
  연남동: "연남동",
  익선동: "익선동",
  삼청동: "삼청동",
  여의도: "여의도한강공원",
  잠실: "잠실 관광특구",
  홍대: "홍대 관광특구",
  이태원: "이태원 관광특구",
}

const CROWD_LEVELS = new Set(["여유", "보통", "혼잡", "매우혼잡"])

const DEFAULT_SEOUL_AREAS: SeoulAreaWithCrowd[] = SEOUL_AREAS.map((area) => ({
  ...area,
  populationMin: null,
  populationMax: null,
}))

export default function HomePage() {
  const [seoulAreas, setSeoulAreas] = useState<SeoulAreaWithCrowd[]>(DEFAULT_SEOUL_AREAS)

  useEffect(() => {
    const controller = new AbortController()

    const loadRealtimeCrowds = async () => {
      const updatedAreas = await Promise.all(
        DEFAULT_SEOUL_AREAS.map(async (area) => {
          const areaName = CROWD_API_AREA_NAMES[area.name]

          if (!areaName) {
            return area
          }

          try {
            const response = await fetch(
              `/api/crowds?areaName=${encodeURIComponent(areaName)}`,
              { signal: controller.signal },
            )

            if (!response.ok) {
              return area
            }

            const crowdData = (await response.json()) as CrowdApiResponse

            return {
              ...area,
              crowd: CROWD_LEVELS.has(crowdData.congestionText)
                ? crowdData.congestionText as SeoulAreaWithCrowd["crowd"]
                : area.crowd,
              populationMin: crowdData.populationMin,
              populationMax: crowdData.populationMax,
            }
          } catch (error) {
            if ((error as DOMException).name !== "AbortError") {
              console.warn(`혼잡도 조회 실패: ${area.name}`, error)
            }
            return area
          }
        }),
      )

      if (!controller.signal.aborted) {
        setSeoulAreas(updatedAreas)
      }
    }

    void loadRealtimeCrowds()

    return () => controller.abort()
  }, [])

  const seongsuArea = seoulAreas.find((area) => area.name === "성수")

  return (
    <div className="flex min-h-screen flex-col">
      <SiteHeader />

      <main className="flex-1">
        {/* Hero */}
        <section className="relative overflow-hidden">
          <div className="mx-auto grid max-w-6xl items-center gap-10 px-4 py-12 sm:px-6 md:grid-cols-2 md:py-20">
            <div className="flex flex-col gap-6">
              <span className="inline-flex w-fit items-center gap-2 rounded-full bg-secondary px-3 py-1 text-sm font-medium text-secondary-foreground">
                <Sparkles className="size-4 text-primary" />
                서울 하루 여행, 고민은 그만
              </span>

              <h1 className="text-balance text-4xl font-extrabold leading-tight tracking-tight sm:text-5xl">
                오늘 서울, 어디로 갈지{" "}
                <span className="text-primary">3초 만에</span> 정해드려요
              </h1>

              <p className="text-pretty text-lg leading-relaxed text-muted-foreground">
                날짜와 위치, 동행, 취향만 고르면 실시간 혼잡도까지 반영한 맞춤
                하루 코스를 만들어 드려요.
              </p>

              <div className="flex flex-wrap gap-3">
                <Link
                  href="/plan"
                  className="inline-flex h-11 items-center justify-center gap-2 rounded-md bg-primary px-8 text-sm font-medium text-primary-foreground shadow transition-colors hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
                >
                  코스 추천받기
                  <ArrowRight className="size-4" />
                </Link>

                <Link
                  href="/events"
                  className="inline-flex h-11 items-center justify-center rounded-md border border-input bg-background px-8 text-sm font-medium shadow-sm transition-colors hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
                >
                  행사 둘러보기
                </Link>
              </div>

              <div className="flex flex-wrap items-center gap-x-6 gap-y-2 pt-2 text-sm text-muted-foreground">
                <span className="flex items-center gap-1.5">
                  <MapPin className="size-4 text-primary" />
                  서울 8개 인기 지역
                </span>

                <span className="flex items-center gap-1.5">
                  <Activity className="size-4 text-primary" />
                  실시간 혼잡도 연동
                </span>
              </div>
            </div>

            <div className="relative">
              <div className="relative aspect-[4/3] overflow-hidden rounded-3xl shadow-xl">
                <Image
                  src="/seoul-hero-cityscape.png"
                  alt="노을빛 서울 도심 풍경"
                  fill
                  priority
                  className="object-cover"
                  sizes="(max-width: 768px) 100vw, 50vw"
                />
              </div>

            </div>
          </div>
        </section>

        {/* Areas quick chips */}
        <section className="mx-auto max-w-6xl px-4 sm:px-6">
          <div className="flex flex-wrap gap-2">
            {seoulAreas.map((area) => (
              <Link
                key={area.name}
                href="/plan"
                className="flex items-center gap-2 rounded-full border border-border bg-card px-3.5 py-2 text-sm font-medium transition-colors hover:border-primary hover:text-primary"
              >
                <MapPin className="size-3.5 text-primary" />
                {area.name}
                <CrowdBadge
                  level={area.crowd}
                  populationMin={area.populationMin}
                  populationMax={area.populationMax}
                  showRange
                  className="px-1.5 py-0.5 text-[10px]"
                />
              </Link>
            ))}
          </div>
        </section>

        {/* How it works */}
        <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
          <div className="mb-10 text-center">
            <h2 className="text-balance text-3xl font-bold tracking-tight">
              3단계면 끝나는 코스 추천
            </h2>
            <p className="mt-2 text-muted-foreground">
              복잡한 검색 없이, 고르기만 하면 돼요.
            </p>
          </div>

          <div className="grid gap-5 md:grid-cols-3">
            {STEPS.map((step, index) => (
              <Card key={step.title} className="gap-3 p-6">
                <div className="flex items-center gap-3">
                  <span className="flex size-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
                    <step.icon className="size-5" />
                  </span>
                  <span className="text-sm font-semibold text-muted-foreground">
                    STEP {index + 1}
                  </span>
                </div>
                <h3 className="text-lg font-bold">{step.title}</h3>
                <p className="leading-relaxed text-muted-foreground">
                  {step.desc}
                </p>
              </Card>
            ))}
          </div>
        </section>

        {/* Categories */}
        <section className="bg-secondary/40 py-16">
        <div className="mx-auto max-w-6xl px-4 sm:px-6">
          <h2 className="text-balance text-3xl font-bold tracking-tight">
            취향대로 골라보세요
          </h2>
          <p className="mt-2 text-muted-foreground">
            선택한 카테고리에 가산점을 부여해 코스를 구성해요.
          </p>

          <div className="mt-8 grid grid-cols-2 gap-3 sm:grid-cols-4">
            {CATEGORIES.map((category) => (
              <div
                key={category.value}
                className="flex items-center gap-3 rounded-2xl border border-border bg-card p-4"
              >
                <span className="text-2xl" aria-hidden>
                  {category.emoji}
                </span>
                <span className="font-semibold">{category.label}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

        {/* Features */}
        <section className="mx-auto max-w-6xl px-4 pb-16 sm:px-6">
          <div className="grid gap-5 md:grid-cols-3">
            {FEATURES.map((feature) => (
              <Card
                key={feature.title}
                className="gap-3 border-0 bg-secondary/50 p-6"
              >
                <span className="flex size-11 items-center justify-center rounded-xl bg-background text-primary shadow-sm">
                  <feature.icon className="size-5" />
                </span>
                <h3 className="text-lg font-bold">{feature.title}</h3>
                <p className="leading-relaxed text-muted-foreground">
                  {feature.desc}
                </p>
              </Card>
            ))}
          </div>
        </section>

        {/* CTA */}
        <section className="mx-auto max-w-6xl px-4 pb-8 sm:px-6">
          <div className="relative overflow-hidden rounded-3xl bg-primary px-6 py-12 text-center text-primary-foreground sm:px-12 sm:py-16">
            <h2 className="text-balance text-3xl font-extrabold sm:text-4xl">
              오늘 하루, 서울에서 어떻게 보낼까요?
            </h2>
            <p className="mx-auto mt-3 max-w-xl text-pretty leading-relaxed text-primary-foreground/85">
              지금 바로 조건을 골라 나만의 하루 코스를 추천받아 보세요.
            </p>

            <Link
              href="/plan"
              className="mt-6 inline-flex h-11 items-center justify-center gap-2 rounded-md bg-secondary px-8 text-sm font-medium text-secondary-foreground shadow-sm transition-colors hover:bg-secondary/80 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
            >
              무료로 코스 추천받기
              <ArrowRight className="size-4" />
            </Link>
          </div>
        </section>
      </main>

      <SiteFooter />
    </div>
  )
}