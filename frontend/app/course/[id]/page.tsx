import Image from "next/image"
import { notFound } from "next/navigation"
import {
  Clock,
  MapPin,
  Route,
  Sparkles,
  Footprints,
  Star,
} from "lucide-react"
import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { CourseMap } from "@/components/course-map"
import { CourseActions } from "@/components/course-actions"
import { CrowdBadge } from "@/components/crowd-badge"
import { SAMPLE_COURSE, SAVED_COURSES } from "@/lib/data"

export default async function CourseDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  const course =
    [SAMPLE_COURSE, ...SAVED_COURSES].find((c) => c.id === id) ?? SAMPLE_COURSE
  if (!course) notFound()

  return (
    <div className="flex min-h-screen flex-col">
      <SiteHeader />
      <main className="flex-1">
        {/* hero */}
        <div className="relative h-56 w-full overflow-hidden sm:h-72">
          <Image
            src={course.cover || "/placeholder.svg"}
            alt={course.title}
            fill
            priority
            className="object-cover"
            sizes="100vw"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-black/10" />
          <div className="absolute inset-x-0 bottom-0 mx-auto max-w-6xl px-4 pb-6 sm:px-6">
            <div className="flex flex-wrap gap-2">
              <Badge className="bg-background/90 text-foreground hover:bg-background">
                {course.area}
              </Badge>
              <Badge className="bg-background/90 text-foreground hover:bg-background">
                {course.companion}
              </Badge>
            </div>
            <h1 className="mt-2 text-balance text-3xl font-extrabold tracking-tight text-white sm:text-4xl">
              {course.title}
            </h1>
          </div>
        </div>

        <div className="mx-auto grid max-w-6xl gap-8 px-4 py-8 sm:px-6 lg:grid-cols-[1fr_380px]">
          {/* left: itinerary */}
          <div>
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div className="flex flex-wrap gap-4 text-sm">
                <span className="flex items-center gap-1.5">
                  <Route className="size-4 text-primary" />
                  {course.stops.length}개 장소
                </span>
                <span className="flex items-center gap-1.5">
                  <Clock className="size-4 text-primary" />
                  {course.totalDuration}
                </span>
                <span className="flex items-center gap-1.5">
                  <Footprints className="size-4 text-primary" />
                  총 {course.totalDistance}
                </span>
              </div>
              <CourseActions title={course.title} />
            </div>

            <p className="mt-4 leading-relaxed text-muted-foreground">
              {course.description}
            </p>

            {/* overall reasons */}
            <Card className="mt-6 gap-3 bg-secondary/40 p-5">
              <p className="flex items-center gap-1.5 font-semibold">
                <Sparkles className="size-4 text-accent" />
                코스 전체 추천 이유
              </p>
              <ul className="space-y-2">
                {course.summaryReasons.map((r) => (
                  <li
                    key={r}
                    className="flex gap-2 text-sm leading-relaxed text-muted-foreground"
                  >
                    <span className="mt-1.5 size-1.5 shrink-0 rounded-full bg-primary" />
                    {r}
                  </li>
                ))}
              </ul>
            </Card>

            {/* timeline */}
            <h2 className="mt-8 text-xl font-bold tracking-tight">방문 순서</h2>
            <ol className="mt-4">
              {course.stops.map((stop, i) => (
                <li key={stop.place.id} className="relative flex gap-4">
                  {/* line + number */}
                  <div className="flex flex-col items-center">
                    <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-primary text-sm font-bold text-primary-foreground">
                      {stop.order}
                    </span>
                    {i < course.stops.length - 1 && (
                      <span className="my-1 w-0.5 flex-1 bg-border" />
                    )}
                  </div>

                  {/* content */}
                  <div className="flex-1 pb-8">
                    <Card className="overflow-hidden p-0 sm:flex-row">
                      <div className="relative h-40 sm:h-auto sm:w-40 sm:shrink-0">
                        <Image
                          src={stop.place.image || "/placeholder.svg"}
                          alt={stop.place.name}
                          fill
                          className="object-cover"
                          sizes="(max-width: 640px) 100vw, 160px"
                        />
                      </div>
                      <div className="flex flex-1 flex-col gap-2 p-4">
                        <div className="flex items-start justify-between gap-2">
                          <div>
                            <span className="text-xs font-semibold text-primary">
                              {stop.arrive} 도착 · {stop.place.category}
                            </span>
                            <h3 className="text-lg font-bold leading-tight">
                              {stop.place.name}
                            </h3>
                          </div>
                          <CrowdBadge level={stop.place.crowd} />
                        </div>
                        <p className="flex items-center gap-1 text-sm text-muted-foreground">
                          <MapPin className="size-3.5" />
                          {stop.place.address}
                        </p>
                        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
                          <span className="flex items-center gap-1 font-semibold text-amber-600">
                            <Star className="size-3 fill-current" />
                            {stop.place.rating}
                          </span>
                          <span className="flex items-center gap-1">
                            <Clock className="size-3" />
                            {stop.place.duration}분 소요
                          </span>
                          <span className="font-medium text-foreground">
                            {stop.place.priceLabel}
                          </span>
                        </div>
                        <div className="mt-1 flex flex-col gap-1">
                          {stop.reasons.map((r) => (
                            <span
                              key={r}
                              className="flex items-start gap-1.5 text-sm text-muted-foreground"
                            >
                              <Sparkles className="mt-0.5 size-3.5 shrink-0 text-accent" />
                              {r}
                            </span>
                          ))}
                        </div>
                      </div>
                    </Card>

                    {stop.travelToNext && (
                      <div className="mt-3 flex items-center gap-2 pl-1 text-xs text-muted-foreground">
                        <Footprints className="size-3.5" />
                        {stop.travelToNext.mode} {stop.travelToNext.minutes}분 ·{" "}
                        {stop.travelToNext.distance}
                      </div>
                    )}
                  </div>
                </li>
              ))}
            </ol>
          </div>

          {/* right: sticky map */}
          <aside className="lg:sticky lg:top-20 lg:self-start">
            <Card className="gap-3 p-4">
              <div className="flex items-center justify-between">
                <h2 className="flex items-center gap-1.5 font-bold">
                  <MapPin className="size-4 text-primary" />
                  코스 경로
                </h2>
                <span className="text-xs text-muted-foreground">
                  마커를 눌러보세요
                </span>
              </div>
              <CourseMap stops={course.stops} />
              <div className="flex items-center justify-between rounded-xl bg-secondary/50 px-4 py-3 text-sm">
                <span className="text-muted-foreground">총 이동 거리</span>
                <span className="font-bold">{course.totalDistance}</span>
              </div>
            </Card>
          </aside>
        </div>
      </main>
      <SiteFooter />
    </div>
  )
}
