import Image from "next/image"
import Link from "next/link"
import {
  ArrowRight,
  CalendarDays,
  MapPin,
  Users,
  Sparkles,
  Route,
  Clock,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { PlaceCard } from "@/components/place-card"
import { CrowdBadge } from "@/components/crowd-badge"
import {
  EVENTS,
  PLACES,
  SAMPLE_COURSE,
  SEOUL_AREAS,
} from "@/lib/data"

export default async function RecommendPage({
  searchParams,
}: {
  searchParams: Promise<{ [key: string]: string | undefined }>
}) {
  const sp = await searchParams
  const area = sp.area ?? "성수"
  const locationName = sp.locationName ?? area
  const locationSource = sp.locationSource ?? "preset"
  const locationAddress = sp.locationAddress
  const latitude = sp.lat
  const longitude = sp.lng
  const companion = sp.companion ?? "커플"
  const dateStr = sp.date
  const cats = sp.cats?.split(",").filter(Boolean) ?? []
  const areaMeta = SEOUL_AREAS.find((a) => a.name === area) ?? SEOUL_AREAS[0]

  const formattedDate = dateStr
    ? new Date(dateStr).toLocaleDateString("ko-KR", {
        month: "long",
        day: "numeric",
        weekday: "short",
      })
    : "오늘"

  return (
    <div className="flex min-h-screen flex-col">
      <SiteHeader />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6">
        {/* condition summary */}
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
          {cats.map((c) => (
            <Badge key={c} variant="secondary" className="px-3 py-1.5 text-sm">
              {c}
            </Badge>
          ))}
          <Button asChild variant="ghost" size="sm" className="ml-auto">
            <Link href="/plan">조건 수정</Link>
          </Button>
        </div>

        <h1 className="mt-6 text-balance text-3xl font-extrabold tracking-tight sm:text-4xl">
          {locationName}에서 즐기는 {companion} 하루 코스예요
        </h1>

        <Card className="mt-6 flex-row flex-wrap items-start gap-4 bg-background p-5">
          <span className="flex size-11 items-center justify-center rounded-xl bg-secondary text-primary shadow-sm">
            <MapPin className="size-5" />
          </span>
          <div className="flex-1">
            <p className="font-semibold">선택된 위치</p>
            <p className="mt-1 text-lg font-bold">{locationName}</p>
            <p className="text-sm text-muted-foreground">
              {locationSource === "kakao" ? "카카오맵에서 선택한 위치" : "기본 지역 선택"}
            </p>
            {locationAddress && (
              <p className="mt-2 text-sm text-muted-foreground">{locationAddress}</p>
            )}
            {latitude && longitude && (
              <p className="mt-1 text-xs text-muted-foreground">
                위도 {latitude}, 경도 {longitude}
              </p>
            )}
          </div>
        </Card>

        {/* real-time crowd banner */}
        <Card className="mt-6 flex-row flex-wrap items-center gap-4 bg-secondary/40 p-5">
          <span className="flex size-11 items-center justify-center rounded-xl bg-background text-primary shadow-sm">
            <Sparkles className="size-5" />
          </span>
          <div className="flex-1">
            <p className="font-semibold">
              지금 {locationName} 주변의 실시간 혼잡도는{" "}
              <span className="align-middle">
                <CrowdBadge level={areaMeta.crowd} showRange />
              </span>
            </p>
            <p className="text-sm text-muted-foreground">
              데이터 갱신: 방금 전 · 서울 실시간 도시데이터 기준
            </p>
          </div>
        </Card>

        {/* recommended course highlight */}
        <section className="mt-10">
          <h2 className="text-2xl font-bold tracking-tight">추천 하루 코스</h2>
          <Card className="mt-4 overflow-hidden p-0 md:flex-row">
            <div className="relative aspect-[16/10] md:aspect-auto md:w-2/5">
              <Image
                src={SAMPLE_COURSE.cover || "/placeholder.svg"}
                alt={SAMPLE_COURSE.title}
                fill
                className="object-cover"
                sizes="(max-width: 768px) 100vw, 40vw"
              />
            </div>
            <div className="flex flex-1 flex-col gap-4 p-6">
              <div>
                <h3 className="text-xl font-bold">{SAMPLE_COURSE.title}</h3>
                <p className="mt-1 leading-relaxed text-muted-foreground">
                  {SAMPLE_COURSE.description}
                </p>
              </div>
              <div className="flex flex-wrap gap-4 text-sm">
                <span className="flex items-center gap-1.5">
                  <Route className="size-4 text-primary" />
                  {SAMPLE_COURSE.stops.length}개 장소
                </span>
                <span className="flex items-center gap-1.5">
                  <Clock className="size-4 text-primary" />
                  {SAMPLE_COURSE.totalDuration}
                </span>
                <span className="flex items-center gap-1.5">
                  <MapPin className="size-4 text-primary" />
                  총 {SAMPLE_COURSE.totalDistance}
                </span>
              </div>
              <div className="rounded-2xl bg-secondary/50 p-4">
                <p className="mb-2 flex items-center gap-1.5 text-sm font-semibold">
                  <Sparkles className="size-4 text-accent" />
                  이 코스를 추천하는 이유
                </p>
                <ul className="space-y-1.5">
                  {SAMPLE_COURSE.summaryReasons.slice(0, 3).map((r) => (
                    <li
                      key={r}
                      className="flex gap-2 text-sm leading-relaxed text-muted-foreground"
                    >
                      <span className="mt-1.5 size-1.5 shrink-0 rounded-full bg-primary" />
                      {r}
                    </li>
                  ))}
                </ul>
              </div>
              <Button asChild className="mt-auto w-fit gap-2">
                <Link href={`/course/${SAMPLE_COURSE.id}`}>
                  코스 상세 · 지도 보기
                  <ArrowRight className="size-4" />
                </Link>
              </Button>
            </div>
          </Card>
        </section>

        {/* recommended events */}
        <section className="mt-12">
          <h2 className="text-2xl font-bold tracking-tight">
            {formattedDate}에 열리는 행사
          </h2>
          <p className="mt-1 text-muted-foreground">
            선택한 날짜에 운영 중인 행사를 우선 정렬했어요.
          </p>
          <div className="mt-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {EVENTS.map((e) => (
              <Card key={e.id} className="group overflow-hidden p-0">
                <div className="relative aspect-[16/10] overflow-hidden">
                  <Image
                    src={e.image || "/placeholder.svg"}
                    alt={e.name}
                    fill
                    className="object-cover transition-transform duration-300 group-hover:scale-105"
                    sizes="(max-width: 768px) 100vw, 33vw"
                  />
                  <span className="absolute left-3 top-3 rounded-full bg-accent px-2.5 py-1 text-xs font-semibold text-accent-foreground">
                    {e.category}
                  </span>
                </div>
                <div className="flex flex-col gap-2 p-4">
                  <h3 className="font-bold leading-tight">{e.name}</h3>
                  <p className="flex items-center gap-1 text-sm text-muted-foreground">
                    <MapPin className="size-3.5" />
                    {e.place}
                  </p>
                  <p className="flex items-center gap-1 text-sm text-muted-foreground">
                    <CalendarDays className="size-3.5" />
                    {e.period}
                  </p>
                  <div className="mt-1 flex items-center justify-between">
                    <span className="text-sm font-semibold text-foreground">
                      {e.fee}
                    </span>
                    <Button asChild variant="ghost" size="sm" className="gap-1">
                      <a href={e.url}>
                        자세히
                        <ArrowRight className="size-3.5" />
                      </a>
                    </Button>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        </section>

        {/* recommended places with crowd */}
        <section className="mt-12">
          <div className="flex items-end justify-between gap-4">
            <div>
              <h2 className="text-2xl font-bold tracking-tight">
                {locationName} 주변 추천 장소
              </h2>
              <p className="mt-1 text-muted-foreground">
                혼잡도가 낮은 곳에 가산점을 부여해 정렬했어요.
              </p>
            </div>
          </div>
          <div className="mt-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {PLACES.map((p) => (
              <PlaceCard key={p.id} place={p} />
            ))}
          </div>
        </section>

        {/* alternative suggestion */}
        <Card className="mt-12 flex-row flex-wrap items-center gap-4 border-amber-200 bg-amber-50 p-5">
          <span className="flex size-11 items-center justify-center rounded-xl bg-amber-100 text-amber-700">
            <Users className="size-5" />
          </span>
          <div className="flex-1">
            <p className="font-semibold text-amber-900">
              대림창고가 지금 혼잡해요
            </p>
            <p className="text-sm text-amber-800/80">
              대신 혼잡도가 낮은 &lsquo;어니언 성수&rsquo;를 추천드려요. 같은 카테고리에
              도보 5분 거리예요.
            </p>
          </div>
          <Button
            variant="outline"
            size="sm"
            className="border-amber-300 bg-background text-amber-800 hover:bg-amber-100"
          >
            대체 장소 보기
          </Button>
        </Card>
      </main>
      <SiteFooter />
    </div>
  )
}
