"use client"

import { Suspense, useEffect, useState } from "react"
import Link from "next/link"
import Image from "next/image"
import { useSearchParams, useRouter } from "next/navigation"
import {
  CalendarDays,
  MapPin,
  Search,
  SlidersHorizontal,
  Tag,
  Clock,
  Ticket,
} from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"

// 백엔드 API 기본 주소 및 자치구 데이터 목록
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"
const SEOUL_DISTRICTS = [
  "강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구",
  "노원구", "도봉구", "동대문구", "동작구", "마포구", "서대문구", "서초구", "성동구",
  "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구", "종로구", "중구", "중랑구"
]

// 백엔드 카테고리 ID 매핑 정보
const EVENT_CATEGORIES = [
  { id: 14, name: "콘서트" },
  { id: 16, name: "클래식" },
  { id: 17, name: "연극" },
  { id: 18, name: "무용" },
  { id: 19, name: "국악" },
  { id: 20, name: "대중가요" },
  { id: 21, name: "전시/미술" },
  { id: 22, name: "축제" },
]

// 백엔드 EventListResponse 스펙 정의
interface EventListResponse {
  id: number
  title: string
  startDate: string
  endDate: string
  eventTime: string
  area: string
  imageUrl: string
  categoryName: string
}

// 백엔드 공통 ApiResponse 스펙 정의
interface ApiResponse<T> {
  success: boolean
  code?: string
  message?: string
  data?: T
}

// 백엔드 Page 데이터 스펙 정의
interface PageResponse<T> {
  content: T[]
  numberOfElements: number
  totalPages: number
  totalElements: number
  number: number
}

export default function EventsPage() {
  return (
    <div className="flex min-h-screen flex-col">
      <SiteHeader />
      <Suspense fallback={<EventsPageFallback />}>
        <EventsContent />
      </Suspense>
      <SiteFooter />
    </div>
  )
}

function EventsPageFallback() {
  return (
    <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6">
      <Card className="p-6 text-sm text-muted-foreground">
        행사 목록을 불러오는 중입니다...
      </Card>
    </main>
  )
}

function EventsContent() {
  const searchParams = useSearchParams()
  const router = useRouter()

  // URL에서 초기 검색 상태 추출
  const currentArea = searchParams.get("area") ?? ""
  const currentCategoryId = searchParams.get("categoryId") ?? ""
  const currentKeyword = searchParams.get("keyword") ?? ""
  const currentStatus = searchParams.get("status") ?? ""
  const currentPage = parseInt(searchParams.get("page") ?? "0", 10)

  // 컴포넌트 내부 검색창 입력값 상태관리
  const [keywordInput, setKeywordInput] = useState(currentKeyword)

  // API 데이터 상태관리
  const [events, setEvents] = useState<EventListResponse[]>([])
  const [pageInfo, setPageInfo] = useState({
    page: 0,
    totalPages: 1,
    totalElements: 0,
    numberOfElements: 0,
  })
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 필터 조건 변경 시 URL 변경 및 초기화 처리 함수
  const updateFilters = (newFilters: Record<string, string | number>) => {
    const params = new URLSearchParams(searchParams.toString())
    
    // 신규 필터 적용 및 0페이지로 리셋
    Object.entries(newFilters).forEach(([key, value]) => {
      if (value === "") {
        params.delete(key)
      } else {
        params.set(key, String(value))
      }
    })
    params.set("page", "0") 

    router.push(`/events?${params.toString()}`)
  }

  // 페이지 이동 함수
  const handlePageChange = (newPage: number) => {
    const params = new URLSearchParams(searchParams.toString())
    params.set("page", String(newPage))
    router.push(`/events?${params.toString()}`)
  }

  // 엔터키 입력 시 키워드 검색 적용
  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    updateFilters({ keyword: keywordInput })
  }

  // URL 파라미터가 변경될 때마다 백엔드 API 호출 실행
  useEffect(() => {
    async function fetchEvents() {
      setIsLoading(true)
      setError(null)

      try {
        const queryParams = new URLSearchParams()
        if (currentArea) queryParams.set("area", currentArea)
        if (currentCategoryId) queryParams.set("categoryId", currentCategoryId)
        if (currentKeyword) queryParams.set("keyword", currentKeyword)
        if (currentStatus) queryParams.set("status", currentStatus)
        queryParams.set("page", String(currentPage))
        queryParams.set("size", "10")

        const response = await fetch(`${API_BASE_URL}/api/events?${queryParams.toString()}`, {
          method: "GET",
          credentials: "include",
        })

        if (!response.ok) {
          setError("행사 목록을 가져오지 못했습니다.")
          return
        }

        const result: ApiResponse<PageResponse<EventListResponse>> = await response.json()
        
        if (result.success && result.data) {
          setEvents(result.data.content)
          setPageInfo({
            page: result.data.number,
            totalPages: result.data.totalPages,
            totalElements: result.data.totalElements,
            numberOfElements: result.data.numberOfElements,
          })
        } else {
          setError(result.message ?? "데이터를 불러오는 중 문제가 발생했습니다.")
        }
      } catch {
        setError("백엔드 서버 연결에 실패했습니다. API 서버 상태를 확인해주세요.")
      } finally {
        setIsLoading(false)
      }
    }

    fetchEvents()
  }, [currentArea, currentCategoryId, currentKeyword, currentStatus, currentPage])

  // 동기화 유지: URL 파라미터가 비워지면 입력창 내용도 비움
  useEffect(() => {
    setKeywordInput(currentKeyword)
  } ,[currentKeyword])

  return (
    <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6">
      {/* 상단 네비게이션 경로 */}
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <Link href="/" className="hover:text-foreground">홈</Link>
        <span>/</span>
        <span className="font-medium text-foreground">행사 둘러보기</span>
      </div>

      <h1 className="mt-4 text-3xl font-extrabold tracking-tight sm:text-4xl">
        서울 문화 행사 둘러보기
      </h1>
      <p className="mt-1 text-muted-foreground">
        지구별, 카테고리별로 진행 중인 다채로운 서울의 행사를 만나보세요.
      </p>

      {/* 필터 및 검색 컨트롤 영역 */}
      <Card className="mt-6 p-4">
        <form onSubmit={handleSearchSubmit} className="flex flex-col gap-3 md:flex-row md:items-center">
          {/* 키워드 검색창 */}
          <div className="relative flex-1">
            <Search className="absolute top-2.5 left-3 size-4 text-muted-foreground" />
            <Input
              placeholder="행사 제목을 입력해 보세요 (예: 금난새)"
              className="pl-9 h-10"
              value={keywordInput}
              onChange={(e) => setKeywordInput(e.target.value)}
            />
          </div>

          {/* 자치구 선택 필터 */}
          <Select value={currentArea} onValueChange={(value) => updateFilters({ area: value })}>
            <SelectTrigger className="w-full md:w-44 h-10">
                <SelectValue>{currentArea || "모든 자치구"}</SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="">모든 자치구</SelectItem>
              {SEOUL_DISTRICTS.map((district) => (
                <SelectItem key={district} value={district}>{district}</SelectItem>
              ))}
            </SelectContent>
          </Select>

          {/* 카테고리 선택 필터 */}
          <Select value={currentCategoryId} onValueChange={(value) => updateFilters({ categoryId: value })}>
            <SelectTrigger className="w-full md:w-44 h-10">
                <SelectValue>
                    {EVENT_CATEGORIES.find(c => String(c.id) === currentCategoryId)?.name || "모든 카테고리"}
                </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="">모든 카테고리</SelectItem>
              {EVENT_CATEGORIES.map((cat) => (
                <SelectItem key={cat.id} value={String(cat.id)}>{cat.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>

          {/* 행사 상태 필터 (진행중 / 마감) */}
          <Select value={currentStatus} onValueChange={(value) => updateFilters({ status: value })}>
            <SelectTrigger className="w-full md:w-36 h-10">
                <SelectValue>
                  {currentStatus === "UPCOMING" ? "진행예정" : 
                  currentStatus === "ING" ? "진행중" : 
                  currentStatus === "END" ? "종료/마감" : "모든 상태"}
                </SelectValue>
            </SelectTrigger>
            <SelectContent>
                <SelectItem value="">모든 상태</SelectItem>
                <SelectItem value="UPCOMING">진행예정</SelectItem>
                <SelectItem value="ING">진행중</SelectItem>
                <SelectItem value="END">종료/마감</SelectItem>
            </SelectContent>
          </Select>

          <Button type="submit" className="h-10 px-5">검색</Button>
        </form>
      </Card>

      {/* 총 결과 건수 표시 */}
      <div className="mt-6 flex items-center justify-between text-sm text-muted-foreground">
        <p className="flex items-center gap-1.5 font-medium">
          <SlidersHorizontal className="size-4" />
          총 <span className="font-bold text-foreground">{pageInfo.totalElements}</span>건의 행사
        </p>
      </div>

      {/* 로딩 상태 렌더링 */}
      {isLoading && (
        <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-3 opacity-60">
          {[...Array(6)].map((_, i) => (
            <Card key={i} className="h-80 animate-pulse bg-secondary/30" />
          ))}
        </div>
      )}

      {/* 에러 상태 렌더링 */}
      {error && !isLoading && (
        <Card className="mt-8 border-destructive/30 bg-destructive/5 p-6 text-center">
          <p className="font-semibold text-destructive">{error}</p>
          <Button variant="outline" size="sm" className="mt-3" onClick={() => router.refresh()}>
            다시 시도
          </Button>
        </Card>
      )}

      {/* 결과 목록 출력 */}
      {!isLoading && !error && (
        <>
          {events.length > 0 ? (
            <div className="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
              {events.map((event) => (
                <Card key={event.id} className="overflow-hidden flex flex-col group h-full">
                  {/* 행사 이미지 썸네일 영역 */}
                  <Link 
                    href={`/events/${event.id}`} 
                    className="relative aspect-[16/10] w-full bg-secondary flex items-center justify-center overflow-hidden cursor-pointer"
                  >
                    {event.imageUrl ? (
                      <Image
                        src={event.imageUrl}
                        alt={event.title}
                        fill
                        className="object-cover transition-transform duration-300 group-hover:scale-105"
                        sizes="(max-width: 768px) 100vw, 33vw"
                      />
                    ) : (
                      <div className="flex flex-col items-center gap-2 text-muted-foreground">
                        <Ticket className="size-7 stroke-[1.5]" />
                        <span className="text-xs">등록된 이미지가 없습니다</span>
                      </div>
                    )}
                    <Badge className="absolute top-3 left-3 bg-background/90 text-foreground backdrop-blur-sm hover:bg-background z-20">
                      {event.categoryName}
                    </Badge>
                  </Link>

                  {/* 행사 정보 콘텐츠 설명 영역 */}
                  <div className="p-5 flex flex-col flex-1 justify-between gap-4">
                    <div>
                      <div className="flex items-center gap-2 text-xs text-muted-foreground">
                        <span className="flex items-center gap-1">
                          <MapPin className="size-3.5 text-primary" />
                          {event.area}
                        </span>
                      </div>
                      <h3 className="mt-2 text-lg font-bold leading-snug line-clamp-2 group-hover:text-primary transition-colors">
                        <Link href={`/events/${event.id}`}>{event.title}</Link>
                      </h3>
                    </div>

                    <div className="space-y-2 pt-2 border-t border-border/60 text-sm text-muted-foreground">
                      <p className="flex items-center gap-2">
                        <CalendarDays className="size-4 shrink-0 text-muted-foreground/80" />
                        <span className="truncate">{event.startDate} ~ {event.endDate}</span>
                      </p>
                      {event.eventTime && (
                        <p className="flex items-center gap-2">
                          <Clock className="size-4 shrink-0 text-muted-foreground/80" />
                          <span className="truncate">{event.eventTime}</span>
                        </p>
                      )}
                    </div>

                    <Button asChild variant="secondary" size="sm" className="w-full mt-1">
                      <Link href={`/events/${event.id}`}>상세보기</Link>
                    </Button>
                  </div>
                </Card>
              ))}
            </div>
          ) : (
            <Card className="mt-8 p-12 text-center text-muted-foreground">
              <Tag className="mx-auto size-8 stroke-[1.5] text-muted-foreground/70" />
              <p className="mt-3 font-medium">검색 조건과 일치하는 행사가 없습니다.</p>
              <p className="text-xs mt-1">다른 자치구나 카테고리를 선택해 보세요.</p>
            </Card>
          )}

          {/* 하단 페이지네이션 컴포넌트 */}
          {pageInfo.totalPages > 1 && (
            <div className="mt-10 flex items-center justify-center gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={pageInfo.page === 0}
                onClick={() => handlePageChange(pageInfo.page - 1)}
              >
                이전
              </Button>
              <span className="text-sm font-medium px-3">
                <span className="text-foreground">{pageInfo.page + 1}</span> / {pageInfo.totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={pageInfo.page >= pageInfo.totalPages - 1}
                onClick={() => handlePageChange(pageInfo.page + 1)}
              >
                다음
              </Button>
            </div>
          )}
        </>
      )}
    </main>
  )
}