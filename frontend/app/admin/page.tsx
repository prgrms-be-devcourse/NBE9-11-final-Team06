import { SiteHeader } from "@/components/site-header"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { CrowdBadge } from "@/components/crowd-badge"
import { PLACES, EVENTS, SEOUL_AREAS, CROWD_META } from "@/lib/data"
import { Plus, MapPin, CalendarDays, Users, LayoutGrid } from "lucide-react"

const stats = [
  { label: "등록 장소", value: PLACES.length, icon: MapPin },
  { label: "진행 행사", value: EVENTS.length, icon: CalendarDays },
  { label: "관리 지역", value: SEOUL_AREAS.length, icon: LayoutGrid },
  { label: "오늘 추천 생성", value: 128, icon: Users },
]

export default function AdminPage() {
  return (
    <div className="flex min-h-screen flex-col bg-background">
      <SiteHeader />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-10">
        <div className="flex flex-col gap-2">
          <Badge variant="secondary" className="w-fit">
            관리자 콘솔
          </Badge>
          <h1 className="font-heading text-2xl font-bold tracking-tight">콘텐츠 관리</h1>
          <p className="text-sm text-muted-foreground">장소, 행사, 지역 혼잡도 데이터를 관리합니다.</p>
        </div>

        {/* Stats */}
        <div className="mt-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
          {stats.map((s) => (
            <Card key={s.label} className="border-border/60">
              <CardContent className="flex items-center gap-4 py-5">
                <div className="flex size-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
                  <s.icon className="size-5" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{s.value}</p>
                  <p className="text-xs text-muted-foreground">{s.label}</p>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>

        <Tabs defaultValue="places" className="mt-8">
          <TabsList>
            <TabsTrigger value="places">장소</TabsTrigger>
            <TabsTrigger value="events">행사</TabsTrigger>
            <TabsTrigger value="areas">지역 혼잡도</TabsTrigger>
          </TabsList>

          {/* Places */}
          <TabsContent value="places" className="mt-6">
            <Card className="border-border/60">
              <CardHeader className="flex-row items-center justify-between space-y-0">
                <CardTitle className="text-base">장소 목록</CardTitle>
                <Button size="sm" className="gap-1.5">
                  <Plus className="size-4" /> 장소 추가
                </Button>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>이름</TableHead>
                      <TableHead>카테고리</TableHead>
                      <TableHead>지역</TableHead>
                      <TableHead>실내</TableHead>
                      <TableHead>혼잡도</TableHead>
                      <TableHead className="text-right">관리</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {PLACES.map((p) => (
                      <TableRow key={p.id}>
                        <TableCell className="font-medium">{p.name}</TableCell>
                        <TableCell>
                          <Badge variant="secondary">{p.category}</Badge>
                        </TableCell>
                        <TableCell className="text-muted-foreground">{p.area}</TableCell>
                        <TableCell className="text-muted-foreground">{p.indoor ? "실내" : "실외"}</TableCell>
                        <TableCell>
                          <CrowdBadge level={p.crowd} />
                        </TableCell>
                        <TableCell className="text-right">
                          <Button variant="ghost" size="sm">
                            수정
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </TabsContent>

          {/* Events */}
          <TabsContent value="events" className="mt-6">
            <Card className="border-border/60">
              <CardHeader className="flex-row items-center justify-between space-y-0">
                <CardTitle className="text-base">행사 목록</CardTitle>
                <Button size="sm" className="gap-1.5">
                  <Plus className="size-4" /> 행사 추가
                </Button>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>행사명</TableHead>
                      <TableHead>카테고리</TableHead>
                      <TableHead>장소</TableHead>
                      <TableHead>기간</TableHead>
                      <TableHead>요금</TableHead>
                      <TableHead className="text-right">관리</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {EVENTS.map((e) => (
                      <TableRow key={e.id}>
                        <TableCell className="font-medium">{e.name}</TableCell>
                        <TableCell>
                          <Badge variant="secondary">{e.category}</Badge>
                        </TableCell>
                        <TableCell className="text-muted-foreground">{e.place}</TableCell>
                        <TableCell className="text-muted-foreground">{e.period}</TableCell>
                        <TableCell className="text-muted-foreground">{e.fee}</TableCell>
                        <TableCell className="text-right">
                          <Button variant="ghost" size="sm">
                            수정
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </TabsContent>

          {/* Areas */}
          <TabsContent value="areas" className="mt-6">
            <Card className="border-border/60">
              <CardHeader>
                <CardTitle className="text-base">지역별 실시간 혼잡도</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                  {SEOUL_AREAS.map((a) => (
                    <div
                      key={a.name}
                      className="flex items-center justify-between rounded-xl border border-border/60 px-4 py-3"
                    >
                      <div>
                        <p className="font-medium">{a.name}</p>
                        <p className="text-xs text-muted-foreground">{CROWD_META[a.crowd].range}</p>
                      </div>
                      <CrowdBadge level={a.crowd} />
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </main>
    </div>
  )
}
