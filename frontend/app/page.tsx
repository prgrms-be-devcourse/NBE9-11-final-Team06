import Image from "next/image"
import Link from "next/link"
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
import { CATEGORIES, PLACES, SEOUL_AREAS, type CrowdLevel } from "@/lib/data"

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

type CrowdResponse = {
  areaName: string
  areaCode: string
  congestionLevel: "RELAXED" | "NORMAL" | "CROWDED" | "VERY_CROWDED"
  congestionText: string
  message: string
  populationMin: number | null
  populationMax: number | null
  measuredAt: string | null
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

const AREA_NAME_MAP: Record<string, string> = {
  성수: "성수카페거리",
  연남동: "연남동",
  익선동: "익선동",
  삼청동: "북촌한옥마을",
  여의도: "여의도한강공원",
  잠실: "잠실 관광특구",
  홍대: "홍대 관광특구",
  이태원: "이태원 관광특구",
}

const CONGESTION_LEVEL_MAP: Record<CrowdResponse["congestionLevel"], CrowdLevel> = {
  RELAXED: "여유",
  NORMAL: "보통",
  CROWDED: "혼잡",
  VERY_CROWDED: "매우혼잡",
}

async function getCrowdStatus(areaName: string): Promise<CrowdResponse | null> {
  try {
    const response = await fetch(
      `${API_BASE_URL}/api/crowds?areaName=${encodeURIComponent(areaName)}`,
      {
        next: { revalidate: 300 },
      },
    )

    if (!response.ok) {
      return null
    }

    return response.json()
  } catch {
    return null
  }
}

async function getSeoulAreasWithCrowd() {
  return Promise.all(
    SEOUL_AREAS.map(async (area) => {
      const apiAreaName = AREA_NAME_MAP[area.name] ?? area.name
      const crowdStatus = await getCrowdStatus(apiAreaName)

      return {
        ...area,
        crowd: crowdStatus ? CONGESTION_LEVEL_MAP[crowdStatus.congestionLevel] : area.crowd,
        populationMin: crowdStatus?.populationMin ?? null,
        populationMax: crowdStatus?.populationMax ?? null,
      }
    }),
  )
}

export default async function HomePage() {
  const seoulAreas = await getSeoulAreasWithCrowd()
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
                  href="/explore"
                  className="inline-flex h-11 items-center justify-center rounded-md border border-input bg-background px-8 text-sm font-medium shadow-sm transition-colors hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
                >
                  장소 둘러보기
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
              <Card className="absolute -bottom-5 -left-3 w-fit min-w-44 gap-1 p-4 shadow-lg sm:-left-6">
                <span className="text-xs text-muted-foreground">지금 성수동은</span>
                <CrowdBadge
                  level={seongsuArea?.crowd ?? "보통"}
                  populationMin={seongsuArea?.populationMin}
                  populationMax={seongsuArea?.populationMax}
                  showRange
                  className="w-fit"
                />
              </Card>
              <Card className="absolute -right-3 top-6 flex-row items-center gap-2 p-3 shadow-lg sm:-right-6">
                <span className="flex size-9 items-center justify-center rounded-lg bg-accent/15 text-accent">
                  <Route className="size-5" />
                </span>
                <div className="leading-tight">
                  <p className="text-sm font-bold">4곳 · 6시간</p>
                  <p className="text-xs text-muted-foreground">추천 코스 완성</p>
                </div>
              </Card>
            </div>
          </div>
        </section>

        {/* Areas quick chips */}
        <section className="mx-auto max-w-6xl px-4 sm:px-6">
          <div className="flex flex-wrap gap-2">
            {seoulAreas.map((a) => (
              <Link
                key={a.name}
                href="/plan"
                className="flex items-center gap-2 rounded-full border border-border bg-card px-3.5 py-2 text-sm font-medium transition-colors hover:border-primary hover:text-primary"
              >
                <MapPin className="size-3.5 text-primary" />
                {a.name}
                <CrowdBadge
                  level={a.crowd}
                  populationMin={a.populationMin}
                  populationMax={a.populationMax}
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
            {STEPS.map((step, i) => (
              <Card key={step.title} className="gap-3 p-6">
                <div className="flex items-center gap-3">
                  <span className="flex size-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
                    <step.icon className="size-5" />
                  </span>
                  <span className="text-sm font-semibold text-muted-foreground">
                    STEP {i + 1}
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
              {CATEGORIES.map((c) => (
                <Link
                  key={c.value}
                  href="/plan"
                  className="flex items-center gap-3 rounded-2xl border border-border bg-card p-4 transition-all hover:-translate-y-0.5 hover:border-primary hover:shadow-md"
                >
                  <span className="text-2xl" aria-hidden>
                    {c.emoji}
                  </span>
                  <span className="font-semibold">{c.label}</span>
                </Link>
              ))}
            </div>
          </div>
        </section>

        {/* Popular places */}
        <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
          <div className="mb-8 flex items-end justify-between gap-4">
            <div>
              <h2 className="text-balance text-3xl font-bold tracking-tight">
                지금 뜨는 성수 장소
              </h2>
              <p className="mt-2 text-muted-foreground">
                실시간 혼잡도와 함께 확인해보세요.
              </p>
            </div>
            <Link
              href="/explore"
              className="inline-flex h-9 items-center justify-center gap-1 rounded-md px-3 text-sm font-medium transition-colors hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
            >
              전체보기
              <ArrowRight className="size-4" />
            </Link>
          </div>
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {PLACES.slice(0, 3).map((p) => (
              <PlaceCard key={p.id} place={p} />
            ))}
          </div>
        </section>

        {/* Features */}
        <section className="mx-auto max-w-6xl px-4 pb-16 sm:px-6">
          <div className="grid gap-5 md:grid-cols-3">
            {FEATURES.map((f) => (
              <Card key={f.title} className="gap-3 border-0 bg-secondary/50 p-6">
                <span className="flex size-11 items-center justify-center rounded-xl bg-background text-primary shadow-sm">
                  <f.icon className="size-5" />
                </span>
                <h3 className="text-lg font-bold">{f.title}</h3>
                <p className="leading-relaxed text-muted-foreground">{f.desc}</p>
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
