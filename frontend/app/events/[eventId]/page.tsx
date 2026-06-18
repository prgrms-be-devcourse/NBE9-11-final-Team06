"use client"

import { Suspense, useEffect, useRef, useState } from "react"
import Link from "next/link"
import Image from "next/image"
import { useParams, useRouter } from "next/navigation"
import {
  CalendarDays,
  Clock,
  ExternalLink,
  MapPin,
  Sparkles,
  Ticket,
  ArrowLeft,
  Info,
  Globe,
  UserCheck,
} from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

// 백엔드 EventDetailResponse 스펙 명세 반영
interface EventDetailResponse {
  id: number
  placeId: number | null
  placeName: string | null
  categoryId: number
  categoryName: string
  title: string
  startDate: string // LocalDate는 string으로 매핑됩니다.
  endDate: string
  eventTime: string
  fee: string
  target: string
  homepageUrl: string
  imageUrl: string
  description: string
  source: string
  area: string
  latitude: number | null
  longitude: number | null
}

interface ApiResponse<T> {
  success: boolean
  data: T
  code: string | null
  message: string | null
}

export default function EventDetailPage() {
  return (
    <div className="flex min-h-screen flex-col">
      <SiteHeader />
      {/* 팀원분들과의 컨벤션을 유지하기 위한 Suspense 래퍼 적용 */}
      <Suspense fallback={<EventDetailFallback />}>
        <EventDetailContent />
      </Suspense>
      <SiteFooter />
    </div>
  )
}

function EventDetailFallback() {
  return (
    <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6">
      <Card className="p-6 text-sm text-muted-foreground animate-pulse">
        상세 정보를 불러오는 중입니다...
      </Card>
    </main>
  )
}

function EventDetailContent() {
  const params = useParams()
  const router = useRouter()
  const eventId = params.eventId as string

  // 네이버 지도 렌더링용 참조 엘리먼트
  const mapContainerRef = useRef<HTMLDivElement | null>(null)
  
  // 상태 관리
  const [event, setEvent] = useState<EventDetailResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isMapLoaded, setIsMapLoaded] = useState(false)

  const naverMapClientId = process.env.NEXT_PUBLIC_NAVER_MAP_CLIENT_ID

  // 1. 단건 상세 데이터 API Fetching (비로그인/로그인 통합 대응)
  useEffect(() => {
    if (!eventId) return

    async function fetchEventDetail() {
      setIsLoading(true)
      setError(null)

      try {
        const headers: HeadersInit = {}
        if (typeof window !== "undefined") {
          const accessToken = localStorage.getItem("accessToken") || sessionStorage.getItem("accessToken")
          if (accessToken) {
            headers["Authorization"] = accessToken.startsWith("Bearer ") ? accessToken : `Bearer ${accessToken}`
          }
        }

        const response = await fetch(`${API_BASE_URL}/api/events/${eventId}`, {
          method: "GET",
          credentials: "include",
          headers,
        })

        if (!response.ok) {
          setError("행사 상세 정보를 가져오는 데 실패했습니다.")
          return
        }

        const result: ApiResponse<EventDetailResponse> = await response.json()
        
        if (result.success && result.data) {
          setEvent(result.data)
        } else {
          setError(result.message ?? "데이터를 표시하는 도중 문제가 발생했습니다.")
        }
      } catch {
        setError("백엔드 서버와 통신할 수 없습니다. 서버 상태를 확인해주세요.")
      } finally {
        setIsLoading(false)
      }
    }

    fetchEventDetail()
  }, [eventId])

  // 2. 네이버 지도 SDK 동적 로드 및 지도 인스턴스 생성 (보내주신 Picker 로직 규격 반영)
  useEffect(() => {
    if (isLoading || error || !event || event.latitude === null || event.longitude === null) return
    let isMounted = true

    const initializeDetailMap = () => {
      if (!mapContainerRef.current || !window.naver?.maps) return

      // 백엔드 표준 위경도 좌표로 LatLng 객체 생성
      const position = new window.naver.maps.LatLng(event.latitude!, event.longitude!)
      
      const mapOptions = {
        center: position,
        zoom: 15,
        zoomControl: true,
        zoomControlOptions: {
          position: window.naver.maps.Position.TOP_RIGHT
        }
      }

      const map = new window.naver.maps.Map(mapContainerRef.current, mapOptions)
      
      const marker = new window.naver.maps.Marker({
        position,
        map,
      })
        // 1. XSS 방어를 위한 이스케이프 헬퍼 함수 정의 
      const escapeHtml = (str: string) => {
        return str.replace(/[&<>/"']/g, (m) => {
          const map: Record<string, string> = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;',
            '/': '&#x2F;'
          };
          return map[m] || m;
        });
      };
    
      // 2. 전달받은 escapeHtml 함수를 사용해 안전하게 HTML 구성
      const infoWindow = new window.naver.maps.InfoWindow({
        content: `
          <div style="padding:8px 12px; font-size:13px; font-weight:700; border-radius:8px; line-height:1.4;">
            <p style="margin:0; color:#0f172a;">${escapeHtml(event.placeName || event.title)}</p>
            <p style="margin:2px 0 0 0; font-size:11px; font-weight:400; color:#64748b;">${escapeHtml(event.area)}</p>
          </div>
        `, // 💡 기존 가독성이 좋은 백틱(`) 구조를 유지하면서 감싸주어도 똑같이 안전합니다!
        borderWidth: 1,
        disableAnchor: false,
      })

      infoWindow.open(map, marker)
      
      window.naver.maps.Event.addListener(marker, "click", () => {
        infoWindow.open(map, marker)
      })

      if (isMounted) setIsMapLoaded(true)
    }

    if (window.naver?.maps) {
      initializeDetailMap()
      return () => { isMounted = false }
    }

    const existingScript = document.querySelector<HTMLScriptElement>("script[data-naver-map-script]")
    
    const handleScriptLoad = () => { if (isMounted) initializeDetailMap() }

    if (existingScript) {
      existingScript.addEventListener("load", handleScriptLoad)
      return () => {
        isMounted = false
        existingScript.removeEventListener("load", handleScriptLoad)
      }
    }

    if (naverMapClientId) {
      const script = document.createElement("script")
      script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${naverMapClientId}`
      script.async = true
      script.dataset.naverMapScript = "true"
      script.addEventListener("load", handleScriptLoad)
      document.head.appendChild(script)
    }

    return () => { isMounted = false }
  }, [isLoading, error, event])

  // 로딩 상태 UI
  if (isLoading) return <EventDetailFallback />

  // 에러 상태 UI
  if (error || !event) {
    return (
      <main className="mx-auto w-full max-w-4xl flex-1 px-4 py-12">
        <Card className="border-destructive/30 bg-destructive/5 p-8 text-center">
          <p className="font-semibold text-destructive">{error || "행사 정보를 찾을 수 없습니다."}</p>
          <Button variant="outline" size="sm" className="mt-4 gap-2" onClick={() => router.push("/events")}>
            <ArrowLeft className="size-4" /> 목록으로 돌아가기
          </Button>
        </Card>
      </main>
    )
  }

  return (
    <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6">
      {/* 뒤로가기 버튼 및 네비게이션 */}
      <div className="flex items-center justify-between">
        <Button variant="ghost" size="sm" className="gap-1.5 text-muted-foreground hover:text-foreground" asChild>
          <Link href="/events">
            <ArrowLeft className="size-4" />
            목록으로 돌아가기
          </Link>
        </Button>
        <div className="flex items-center gap-2 text-xs text-muted-foreground">
          <span>행사 둘러보기</span>
          <span>/</span>
          <span className="font-medium text-foreground truncate max-w-[180px]">{event.title}</span>
        </div>
      </div>

      {/* 대형 상세 메인 소개 카드 섹션 */}
      <section className="mt-6 grid gap-6 lg:grid-cols-3">
        {/* 좌측: 포스터 이미지 슬롯 */}
        <div className="lg:col-span-1">
          <Card className="overflow-hidden relative aspect-[6/7] bg-secondary flex items-center justify-center shadow-sm group">
            {event.imageUrl ? (
              <Image
                src={event.imageUrl}
                alt={event.title}
                fill
                priority
                className="object-cover"
                sizes="(max-width: 1024px) 100vw, 33vw"
              />
            ) : (
              <div className="flex flex-col items-center gap-2 text-muted-foreground">
                <Ticket className="size-12 stroke-[1.2]" />
                <span className="text-sm">등록된 이미지가 없습니다</span>
              </div>
            )}
            <Badge className="absolute top-4 left-4 bg-background/95 text-foreground backdrop-blur-sm text-sm px-3 py-1">
              {event.categoryName}
            </Badge>
          </Card>
        </div>

        {/* 우측: 핵심 스펙 지표 정보 설명 리스트 */}
        <div className="lg:col-span-2 flex flex-col justify-between h-full gap-4">
          <div className="space-y-4">
            <div className="flex flex-wrap items-center gap-2">
              <Badge variant="secondary" className="gap-1 px-2.5 py-1 text-xs">
                <MapPin className="size-3 text-primary" />
                {event.area}
              </Badge>
              {event.source && (
                <Badge variant="outline" className="text-xs text-muted-foreground">
                  출처: {event.source}
                </Badge>
              )}
            </div>

            <h1 className="text-2xl font-extrabold tracking-tight sm:text-3xl lg:text-4xl leading-tight text-balance">
              {event.title}
            </h1>

            {/* 메인 상세 속성 가이드 보드 표 */}
            <div className="mt-4 rounded-xl border border-border bg-card/50 p-4 space-y-3.5 text-sm">
              <div className="grid grid-cols-4 items-start gap-2">
                <span className="text-muted-foreground flex items-center gap-1.5 font-medium">
                  <CalendarDays className="size-4 text-muted-foreground/80" /> 기간
                </span>
                <span className="col-span-3 font-semibold text-foreground">
                  {event.startDate} ~ {event.endDate}
                </span>
              </div>

              {event.eventTime && (
                <div className="grid grid-cols-4 items-start gap-2 pt-3 border-t border-border/40">
                  <span className="text-muted-foreground flex items-center gap-1.5 font-medium">
                    <Clock className="size-4 text-muted-foreground/80" /> 시간
                  </span>
                  <span className="col-span-3 text-foreground whitespace-pre-line leading-relaxed">
                    {event.eventTime}
                  </span>
                </div>
              )}

              <div className="grid grid-cols-4 items-start gap-2 pt-3 border-t border-border/40">
                <span className="text-muted-foreground flex items-center gap-1.5 font-medium">
                  <Ticket className="size-4 text-muted-foreground/80" /> 비용
                </span>
                <span className="col-span-3 font-bold text-primary">
                  {event.fee || "무료"}
                </span>
              </div>

              {event.target && (
                <div className="grid grid-cols-4 items-start gap-2 pt-3 border-t border-border/40">
                  <span className="text-muted-foreground flex items-center gap-1.5 font-medium">
                    <UserCheck className="size-4 text-muted-foreground/80" /> 대상
                  </span>
                  <span className="col-span-3 text-foreground">
                    {event.target}
                  </span>
                </div>
              )}

              {event.placeName && (
                <div className="grid grid-cols-4 items-start gap-2 pt-3 border-t border-border/40">
                  <span className="text-muted-foreground flex items-center gap-1.5 font-medium">
                    <MapPin className="size-4 text-muted-foreground/80" /> 장소
                  </span>
                  <span className="col-span-3 font-medium text-foreground">
                    {event.placeName}
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* 홈페이지 링크 아웃링크 액션 버튼 */}
          {event.homepageUrl && (
            <Button asChild size="lg" className="w-full sm:w-fit gap-2 mt-2">
              <a href={event.homepageUrl} target="_blank" rel="noopener noreferrer">
                공식 홈페이지 바로가기
                <ExternalLink className="size-4" />
              </a>
            </Button>
          )}
        </div>
      </section>

      {/* 하단 투 트랙 설명 및 지도 상세 배치 정보 레이아웃 */}
      <section className="mt-10 grid gap-6 md:grid-cols-3">
        {/* 행사 상세 소개 소개글 정보 (2칸 차지) */}
        <div className="md:col-span-2 space-y-4">
          <h2 className="text-xl font-bold tracking-tight flex items-center gap-2">
            <Info className="size-5 text-primary" />
            행사 상세 소개
          </h2>
          <Card className="p-6 h-full min-h-[280px]">
            {event.description ? (
              <p className="text-sm leading-relaxed text-muted-foreground whitespace-pre-line">
                {event.description}
              </p>
            ) : (
              <div className="flex flex-col items-center justify-center text-muted-foreground h-full min-h-[200px] gap-2">
                <Sparkles className="size-5 text-accent animate-pulse" />
                <p className="text-sm">해당 행사의 상세 설명 텍스트 요약이 존재하지 않습니다.</p>
                <p className="text-xs text-muted-foreground/70">상단의 공식 홈페이지 링크를 확인해 보세요.</p>
              </div>
            )}
          </Card>
        </div>

        {/* 네이버 지도 위치 안내 컴포넌트 섹션 (1칸 차지) */}
        <div className="md:col-span-1 space-y-4">
          <h2 className="text-xl font-bold tracking-tight flex items-center gap-2">
            <Globe className="size-5 text-primary" />
            개최 장소 위치
          </h2>
          
          <div className="relative aspect-square w-full overflow-hidden rounded-2xl border border-border bg-[oklch(0.97_0.02_220)]">
            {/* 고유의 스타일리시한 배경 가상 그리드 레이어 내장 (CourseMap 디자인 결 통일) */}
            <svg className="absolute inset-0 size-full" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden>
              <defs>
                <pattern id="detail-grid" width="10" height="10" patternUnits="userSpaceOnUse">
                  <path d="M 10 0 L 0 0 0 10" fill="none" stroke="oklch(0.9 0.02 220)" strokeWidth="0.4" />
                </pattern>
              </defs>
              <rect width="100" height="100" fill="url(#detail-grid)" />
            </svg>

            {/* 실제 네이버 지도가 주입될 컨테이너 */}
            {event.latitude !== null && event.longitude !== null ? (
              <div ref={mapContainerRef} className="absolute inset-0 size-full z-10" />
            ) : (
              <div className="absolute inset-0 flex flex-col items-center justify-center p-6 text-center text-muted-foreground bg-secondary/40">
                <MapPin className="size-8 stroke-[1.5] text-muted-foreground/60 mb-2" />
                <p className="text-sm font-semibold">제공된 위치 정보가 없습니다.</p>
                <p className="text-xs text-muted-foreground/70 mt-1">정확한 장소 정보는 주최측 명세를 참고해 주세요.</p>
              </div>
            )}

            {/* 지도 로딩 인디케이터 백그라운드 홀더 */}
            {!isMapLoaded && event.latitude !== null && event.longitude !== null && (
              <div className="absolute inset-0 flex items-center justify-center bg-secondary/50 text-xs text-muted-foreground">
                네이버 지도를 불러오는 중입니다...
              </div>
            )}
          </div>
        </div>
      </section>
    </main>
  )
}