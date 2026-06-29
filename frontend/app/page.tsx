"use client"

import Image from "next/image"
import Link from "next/link"
import { useEffect, useState, type MouseEvent } from "react"
import { useAuth } from "@/hooks/use-auth"
import { useRouter } from "next/navigation"
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
    desc: "추천 코스의 방문 순서를 지도에서 한눈에 확인할 수 있어요.",
  },
]


type CrowdApiResponse = {
  areaName: string
  congestionText: string
  congestionLevel?: string
  populationMin: number | null
  populationMax: number | null
}

type CrowdLevel = (typeof SEOUL_AREAS)[number]["crowd"]

const CROWD_LEVEL_LABELS: Record<string, CrowdLevel> = {
  RELAXED: "여유",
  NORMAL: "보통",
  CROWDED: "혼잡",
  VERY_CROWDED: "매우혼잡",
  여유: "여유",
  보통: "보통",
  혼잡: "혼잡",
  매우혼잡: "매우혼잡",
}


export default function HomePage() {
  const router = useRouter()
  const { isLoggedIn, isAuthLoading } = useAuth()

  function handleRecommendationStart(event: MouseEvent<HTMLAnchorElement>) {
    if (isAuthLoading || isLoggedIn) {
      return
    }

    event.preventDefault()

    const shouldMoveToLogin = window.confirm(
      "코스 추천은 로그인 후 이용할 수 있어요.\n로그인 페이지로 이동할까요?"
    )

    if (shouldMoveToLogin) {
      router.push("/login")
    }
  }
  const [topCrowdAreas, setTopCrowdAreas] = useState<CrowdApiResponse[]>([])
  const [isTopCrowdLoading, setIsTopCrowdLoading] = useState(true)

  useEffect(() => {
    const controller = new AbortController()

    const loadTopCrowdAreas = async () => {
      try {
        const response = await fetch("/api/crowds/top?limit=10", {
          signal: controller.signal,
        })

        if (!response.ok) {
          throw new Error("혼잡도 상위 지역을 불러오지 못했습니다.")
        }

        const crowdData = (await response.json()) as CrowdApiResponse[]

        if (!controller.signal.aborted) {
          setTopCrowdAreas(crowdData)
        }
      } catch (error) {
        if ((error as DOMException).name !== "AbortError") {
          console.warn("혼잡도 상위 지역 조회 실패", error)
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsTopCrowdLoading(false)
        }
      }
    }

    void loadTopCrowdAreas()

    return () => controller.abort()
  }, [])

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
                <span className="text-primary">손쉽게</span> 정해드려요
              </h1>

              <p className="text-pretty text-lg leading-relaxed text-muted-foreground">
                날짜와 위치, 동행, 취향만 고르면 실시간 혼잡도까지 반영한 맞춤
                하루 코스를 만들어 드려요.
              </p>

              <div className="flex flex-wrap gap-3">
                <Link
                  href="/plan"
                  onClick={handleRecommendationStart}
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

        {/* Current crowd top 10 */}
        <section className="mx-auto max-w-6xl px-4 sm:px-6">
          <div className="mb-4 flex items-end justify-between gap-4">
            <div>
              <p className="text-sm font-semibold text-primary">실시간 서울 혼잡도</p>
              <h2 className="mt-1 text-2xl font-bold tracking-tight">
                지금 서울에서 혼잡한 지역 TOP 10
              </h2>
            </div>
            <span className="flex shrink-0 items-center gap-1.5 text-sm text-muted-foreground">
              <Activity className="size-4 text-primary" />
              최신 수집 데이터 기준
            </span>
          </div>

          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
            {isTopCrowdLoading
              ? Array.from({ length: 10 }, (_, index) => (
                  <div
                    key={index}
                    className="flex min-h-24 animate-pulse flex-col justify-between rounded-2xl border border-border bg-card p-4"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <span className="h-4 w-24 rounded bg-muted" />
                      <span className="size-6 rounded-full bg-muted" />
                    </div>
                    <span className="mt-3 h-4 w-20 rounded bg-muted" />
                  </div>
                ))
              : topCrowdAreas.map((area, index) => (
                  <div
                    key={area.areaName}
                    className="flex min-h-24 flex-col justify-between rounded-2xl border border-border bg-card p-4"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <span className="text-sm font-semibold leading-snug">{area.areaName}</span>
                      <span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary">
                        {index + 1}
                      </span>
                    </div>
                    <CrowdBadge
                      level={CROWD_LEVEL_LABELS[area.congestionLevel ?? area.congestionText] ?? "보통"}
                      populationMin={area.populationMin}
                      populationMax={area.populationMax}
                      showRange
                      className="mt-3 w-fit px-1.5 py-0.5 text-[10px]"
                    />
                  </div>
                ))}
          </div>
        </section>

        {/* How it works */}
        <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
          <div className="mb-10">
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

          <div className="mt-8 grid grid-cols-2 gap-3 sm:grid-cols-3">
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
              onClick={handleRecommendationStart}
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