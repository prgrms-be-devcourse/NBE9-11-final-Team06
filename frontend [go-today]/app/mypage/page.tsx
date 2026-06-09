import Link from "next/link"
import Image from "next/image"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Separator } from "@/components/ui/separator"
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs"
import { SAVED_COURSES, CATEGORIES, COMPANIONS } from "@/lib/data"
import { MapPin, Clock, Route, Heart, Settings, Bookmark } from "lucide-react"

const myInterests = ["전시", "카페", "산책"]
const myCompanion = "커플"

export default function MyPage() {
  return (
    <div className="flex min-h-screen flex-col bg-background">
      <SiteHeader />
      <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-10">
        {/* Profile header */}
        <div className="flex flex-col gap-5 rounded-3xl border border-border/60 bg-card p-6 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-4">
            <Avatar className="size-16">
              <AvatarFallback className="bg-primary text-lg text-primary-foreground">여행</AvatarFallback>
            </Avatar>
            <div>
              <h1 className="font-heading text-xl font-bold">여행자님</h1>
              <p className="text-sm text-muted-foreground">traveler@example.com</p>
              <div className="mt-2 flex flex-wrap gap-1.5">
                <Badge variant="secondary" className="gap-1">
                  <Heart className="size-3" /> {SAVED_COURSES.length}개 코스 저장
                </Badge>
              </div>
            </div>
          </div>
          <Button variant="outline" className="gap-2 bg-transparent">
            <Settings className="size-4" /> 프로필 설정
          </Button>
        </div>

        <Tabs defaultValue="saved" className="mt-8">
          <TabsList>
            <TabsTrigger value="saved" className="gap-1.5">
              <Bookmark className="size-4" /> 저장한 코스
            </TabsTrigger>
            <TabsTrigger value="prefs" className="gap-1.5">
              <Settings className="size-4" /> 선호 정보
            </TabsTrigger>
          </TabsList>

          <TabsContent value="saved" className="mt-6">
            <div className="grid gap-5 sm:grid-cols-2">
              {SAVED_COURSES.map((course) => (
                <Link key={course.id} href={`/course/${course.id}`}>
                  <Card className="group h-full overflow-hidden border-border/60 pt-0 transition-shadow hover:shadow-md">
                    <div className="relative aspect-[16/9] overflow-hidden">
                      <Image
                        src={course.cover || "/placeholder.svg"}
                        alt={course.title}
                        fill
                        className="object-cover transition-transform duration-300 group-hover:scale-105"
                      />
                      <Badge className="absolute left-3 top-3 bg-background/90 text-foreground hover:bg-background/90">
                        {course.area}
                      </Badge>
                    </div>
                    <CardContent className="px-5 pb-5">
                      <h3 className="font-heading font-semibold">{course.title}</h3>
                      <p className="mt-1 line-clamp-2 text-sm text-muted-foreground">{course.description}</p>
                      <div className="mt-3 flex flex-wrap gap-3 text-xs text-muted-foreground">
                        <span className="flex items-center gap-1">
                          <Clock className="size-3.5" /> {course.totalDuration}
                        </span>
                        <span className="flex items-center gap-1">
                          <Route className="size-3.5" /> {course.totalDistance}
                        </span>
                        <span className="flex items-center gap-1">
                          <MapPin className="size-3.5" /> {course.stops.length}개 장소
                        </span>
                      </div>
                    </CardContent>
                  </Card>
                </Link>
              ))}
            </div>
          </TabsContent>

          <TabsContent value="prefs" className="mt-6">
            <Card className="border-border/60">
              <CardHeader>
                <CardTitle className="text-base">나의 선호 정보</CardTitle>
              </CardHeader>
              <CardContent className="flex flex-col gap-6">
                <div>
                  <p className="mb-3 text-sm font-medium">관심 카테고리</p>
                  <div className="flex flex-wrap gap-2">
                    {CATEGORIES.map((c) => {
                      const active = myInterests.includes(c.value)
                      return (
                        <span
                          key={c.value}
                          className={`rounded-full border px-3 py-1.5 text-sm ${
                            active
                              ? "border-primary bg-primary text-primary-foreground"
                              : "border-border bg-background text-muted-foreground"
                          }`}
                        >
                          {c.label}
                        </span>
                      )
                    })}
                  </div>
                </div>
                <Separator />
                <div>
                  <p className="mb-3 text-sm font-medium">주 동행 유형</p>
                  <div className="flex flex-wrap gap-2">
                    {COMPANIONS.map((c) => {
                      const active = myCompanion === c.value
                      return (
                        <span
                          key={c.value}
                          className={`rounded-full border px-3 py-1.5 text-sm ${
                            active
                              ? "border-accent bg-accent text-accent-foreground"
                              : "border-border bg-background text-muted-foreground"
                          }`}
                        >
                          {c.value}
                        </span>
                      )
                    })}
                  </div>
                </div>
                <Button className="w-fit">선호 정보 수정</Button>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </main>
      <SiteFooter />
    </div>
  )
}
