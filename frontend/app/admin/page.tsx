"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
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
import { SEOUL_AREAS, CROWD_META, type CrowdLevel } from "@/lib/data"
import { Plus, MapPin, CalendarDays, Users, LayoutGrid } from "lucide-react"

type ApiResponse<T> = {
  data: T
  message?: string
}

type MemberInfo = {
  id: number
  email: string
  nickname: string
  role: "USER" | "ADMIN"
  status: string
}

type PageResponse<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
  empty: boolean
}

type AdminPlaceResponse = {
  id: number
  categoryId: number
  name: string
  address: string
  roadAddress: string | null
  latitude: number
  longitude: number
  phone: string | null
  placeUrl: string | null
  description: string | null
  source: string
  externalId: string | null
  isActive: boolean
}

type AdminEventResponse = {
  id: number
  title: string
  startDate: string
  endDate: string
  eventTime: string | null
  area: string | null
  imageUrl: string | null
  categoryName: string
}

type AdminMemberResponse = {
  id?: number
  memberId?: number
  email: string
  nickname: string
  profileImageUrl: string | null
  role: string
  status: string
  createdAt: string
  updatedAt: string
}

type CrowdApiResponse = {
  areaName: string
  areaCode: string
  congestionLevel: "RELAXED" | "NORMAL" | "CROWDED" | "VERY_CROWDED"
  congestionText: string
  message: string
  populationMin: number | null
  populationMax: number | null
  measuredAt: string | null
}

type AdminSeoulAreaWithCrowd = (typeof SEOUL_AREAS)[number] & {
  apiAreaName: string | null
  populationMin: number | null
  populationMax: number | null
  measuredAt: string | null
  message: string | null
  isRealtime: boolean
}

type PlaceForm = {
  categoryId: string
  name: string
  address: string
  roadAddress: string
  latitude: string
  longitude: string
  phone: string
  placeUrl: string
  description: string
  externalId: string
}

const PLACE_PAGE_SIZE = 20
const EVENT_PAGE_SIZE = 20
const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

const CROWD_API_AREA_NAMES: Record<string, string> = {
  성수: "성수카페거리",
  연남동: "연남동",
  익선동: "익선동",
  삼청동: "북촌한옥마을",
  여의도: "여의도한강공원",
  잠실: "잠실 관광특구",
  홍대: "홍대 관광특구",
  이태원: "이태원 관광특구",
}

const DEFAULT_ADMIN_SEOUL_AREAS: AdminSeoulAreaWithCrowd[] = SEOUL_AREAS.map((area) => ({
  ...area,
  apiAreaName: CROWD_API_AREA_NAMES[area.name] ?? null,
  populationMin: null,
  populationMax: null,
  measuredAt: null,
  message: null,
  isRealtime: false,
}))

const emptyPlaceForm: PlaceForm = {
  categoryId: "",
  name: "",
  address: "",
  roadAddress: "",
  latitude: "",
  longitude: "",
  phone: "",
  placeUrl: "",
  description: "",
  externalId: "",
}

const normalizeCrowdLevel = (congestionLevel: CrowdApiResponse["congestionLevel"]): CrowdLevel => {
  switch (congestionLevel) {
    case "RELAXED":
      return "여유"
    case "NORMAL":
      return "보통"
    case "CROWDED":
      return "혼잡"
    case "VERY_CROWDED":
      return "매우혼잡"
    default:
      return "보통"
  }
}

const formatPopulationRange = (
  populationMin: number | null,
  populationMax: number | null,
  fallback: string,
) => {
  if (populationMin === null || populationMax === null) {
    return fallback
  }

  return `${populationMin.toLocaleString()}~${populationMax.toLocaleString()}명`
}

const formatMeasuredAt = (measuredAt: string | null) => {
  if (!measuredAt) {
    return null
  }

  const date = new Date(measuredAt)

  if (Number.isNaN(date.getTime())) {
    return null
  }

  return date.toLocaleString("ko-KR", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  })
}

function getAccessToken() {
  const storages = [localStorage, sessionStorage]
  const tokenKeys = ["accessToken", "access_token", "token", "jwt"]

  for (const storage of storages) {
    for (const tokenKey of tokenKeys) {
      const value = storage.getItem(tokenKey)?.trim().replace(/^Bearer\s+/i, "")

      if (value && value !== "undefined" && value !== "null") {
        return value
      }
    }
  }

  return null
}

async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const accessToken = getAccessToken()
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...(options.headers ?? {}),
    },
  })

  if (!res.ok) {
    let message = `API 요청 실패: ${res.status}`
    const errorText = await res.text()

    if (errorText) {
      try {
        const errorBody = JSON.parse(errorText) as {
          message?: string
          error?: string
        }
        message = errorBody.message ?? errorBody.error ?? errorText
      } catch {
        message = errorText
      }
    }

    throw new Error(message)
  }

  if (res.status === 204) {
    return null as T
  }

  const contentType = res.headers.get("content-type")

  if (!contentType?.includes("application/json")) {
    throw new Error("백엔드 API가 JSON이 아닌 응답을 반환했습니다.")
  }

  return res.json()
}

export default function AdminPage() {
  const router = useRouter()

  const [checkingAuth, setCheckingAuth] = useState(true)
  const [loading, setLoading] = useState(false)

  const [places, setPlaces] = useState<AdminPlaceResponse[]>([])
  const [placePage, setPlacePage] = useState(0)
  const [placeTotalPages, setPlaceTotalPages] = useState(0)
  const [placeTotalElements, setPlaceTotalElements] = useState(0)

  const [events, setEvents] = useState<AdminEventResponse[]>([])
  const [eventPage, setEventPage] = useState(0)
  const [eventTotalPages, setEventTotalPages] = useState(0)
  const [eventTotalElements, setEventTotalElements] = useState(0)

  const [members, setMembers] = useState<AdminMemberResponse[]>([])

  const [areaCrowds, setAreaCrowds] = useState<AdminSeoulAreaWithCrowd[]>(
    DEFAULT_ADMIN_SEOUL_AREAS,
  )
  const [areaCrowdLoading, setAreaCrowdLoading] = useState(false)
  const [areaCrowdError, setAreaCrowdError] = useState<string | null>(null)

  const [keyword, setKeyword] = useState("")
  const [categoryId, setCategoryId] = useState("")
  const [isActive, setIsActive] = useState("")
  const [source, setSource] = useState("")

  const [placeForm, setPlaceForm] = useState<PlaceForm>(emptyPlaceForm)
  const [editingPlaceId, setEditingPlaceId] = useState<number | null>(null)

  const stats = [
    { label: "등록 장소", value: placeTotalElements, icon: MapPin },
    { label: "등록 행사", value: eventTotalElements, icon: CalendarDays },
    { label: "회원 수", value: members.length, icon: Users },
    { label: "관리 지역", value: areaCrowds.length, icon: LayoutGrid },
  ]

  useEffect(() => {
    checkAdminAuth()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function checkAdminAuth() {
    if (!getAccessToken()) {
      router.replace("/login")
      return
    }

    try {
      const response = await apiFetch<ApiResponse<MemberInfo>>("/api/members/me")
      const member = response.data

      if (member.role !== "ADMIN") {
        alert("관리자 권한이 없습니다.")
        router.replace("/")
        return
      }

      setCheckingAuth(false)
      await Promise.all([loadPlaces(0), loadMembers(), loadEvents(0), loadAreaCrowds()])
    } catch (error) {
      console.warn("관리자 권한 확인에 실패했습니다.", error)
      router.replace("/login")
    }
  }

  async function loadPlaces(page = placePage) {
    const params = new URLSearchParams()
    params.set("page", String(page))
    params.set("size", String(PLACE_PAGE_SIZE))

    if (keyword.trim()) {
      params.set("keyword", keyword.trim())
    }

    if (categoryId.trim()) {
      params.set("categoryId", categoryId.trim())
    }

    if (isActive.trim()) {
      params.set("isActive", isActive.trim())
    }

    if (source.trim()) {
      params.set("source", source.trim())
    }

    const response = await apiFetch<PageResponse<AdminPlaceResponse>>(
      `/api/admin/places?${params.toString()}`,
    )

    setPlaces(response.content)
    setPlacePage(response.number)
    setPlaceTotalPages(response.totalPages)
    setPlaceTotalElements(response.totalElements)
  }

  async function handleChangePlacePage(nextPage: number) {
    if (nextPage < 0 || nextPage >= placeTotalPages) {
      return
    }

    await loadPlaces(nextPage)
  }

  async function loadEvents(page = eventPage) {
    const params = new URLSearchParams()
    params.set("page", String(page))
    params.set("size", String(EVENT_PAGE_SIZE))

    const response = await apiFetch<PageResponse<AdminEventResponse>>(
      `/api/admin/events?${params.toString()}`,
    )

    setEvents(response.content)
    setEventPage(response.number)
    setEventTotalPages(response.totalPages)
    setEventTotalElements(response.totalElements)
  }

  async function handleChangeEventPage(nextPage: number) {
    if (nextPage < 0 || nextPage >= eventTotalPages) {
      return
    }

    await loadEvents(nextPage)
  }

  async function loadMembers() {
    const response = await apiFetch<PageResponse<AdminMemberResponse>>(
      "/api/admin/members?page=0&size=20",
    )

    setMembers(response.content)
  }

  async function loadAreaCrowds() {
    setAreaCrowdLoading(true)
    setAreaCrowdError(null)

    try {
      const updatedAreas = await Promise.all(
        DEFAULT_ADMIN_SEOUL_AREAS.map(async (area) => {
          if (!area.apiAreaName) {
            return area
          }

          try {
            const crowdData = await apiFetch<CrowdApiResponse>(
              `/api/crowds?areaName=${encodeURIComponent(area.apiAreaName)}`,
            )

            return {
              ...area,
              crowd: normalizeCrowdLevel(crowdData.congestionLevel),
              populationMin: crowdData.populationMin,
              populationMax: crowdData.populationMax,
              measuredAt: crowdData.measuredAt,
              message: crowdData.message,
              isRealtime: true,
            }
          } catch (error) {
            console.warn(`관리자 혼잡도 조회 실패: ${area.name}`, error)
            return {
              ...area,
              isRealtime: false,
            }
          }
        }),
      )

      const failedCount = updatedAreas.filter((area) => !area.isRealtime).length

      if (failedCount > 0) {
        setAreaCrowdError(
          `일부 지역(${failedCount}개)의 실시간 혼잡도 조회에 실패해 기본값을 표시합니다.`,
        )
      }

      setAreaCrowds(updatedAreas)
    } catch (error) {
      console.error(error)
      setAreaCrowdError("지역별 혼잡도 조회 중 오류가 발생했습니다.")
      setAreaCrowds(DEFAULT_ADMIN_SEOUL_AREAS)
    } finally {
      setAreaCrowdLoading(false)
    }
  }

  function validatePlaceForm() {
    if (!placeForm.categoryId.trim()) {
      alert("카테고리 ID를 입력해주세요.")
      return false
    }

    if (!placeForm.name.trim()) {
      alert("장소명을 입력해주세요.")
      return false
    }

    if (!placeForm.address.trim()) {
      alert("주소를 입력해주세요.")
      return false
    }

    if (!placeForm.latitude.trim()) {
      alert("위도를 입력해주세요.")
      return false
    }

    if (!placeForm.longitude.trim()) {
      alert("경도를 입력해주세요.")
      return false
    }

    if (Number.isNaN(Number(placeForm.categoryId))) {
      alert("카테고리 ID는 숫자로 입력해주세요.")
      return false
    }

    if (Number.isNaN(Number(placeForm.latitude))) {
      alert("위도는 숫자로 입력해주세요.")
      return false
    }

    if (Number.isNaN(Number(placeForm.longitude))) {
      alert("경도는 숫자로 입력해주세요.")
      return false
    }

    return true
  }

  function toPlaceRequestBody() {
    return {
      categoryId: Number(placeForm.categoryId),
      name: placeForm.name.trim(),
      address: placeForm.address.trim(),
      roadAddress: placeForm.roadAddress.trim(),
      latitude: Number(placeForm.latitude),
      longitude: Number(placeForm.longitude),
      phone: placeForm.phone.trim(),
      placeUrl: placeForm.placeUrl.trim(),
      description: placeForm.description.trim(),
      externalId: placeForm.externalId.trim(),
    }
  }

  async function handleSubmitPlace() {
    if (!validatePlaceForm()) {
      return
    }

    try {
      setLoading(true)

      if (editingPlaceId) {
        await apiFetch<AdminPlaceResponse>(`/api/admin/places/${editingPlaceId}`, {
          method: "PUT",
          body: JSON.stringify(toPlaceRequestBody()),
        })

        alert("장소가 수정되었습니다.")
      } else {
        await apiFetch<AdminPlaceResponse>("/api/admin/places", {
          method: "POST",
          body: JSON.stringify(toPlaceRequestBody()),
        })

        alert("장소가 등록되었습니다.")
      }

      setPlaceForm(emptyPlaceForm)
      setEditingPlaceId(null)
      await loadPlaces(0)
    } catch (error) {
      console.error(error)
      alert(error instanceof Error ? error.message : "장소 저장에 실패했습니다.")
    } finally {
      setLoading(false)
    }
  }

  function handleEditPlace(place: AdminPlaceResponse) {
    if (!place.isActive) {
      alert("비활성 처리된 장소는 수정할 수 없습니다.")
      return
    }

    setEditingPlaceId(place.id)
    setPlaceForm({
      categoryId: String(place.categoryId),
      name: place.name,
      address: place.address,
      roadAddress: place.roadAddress ?? "",
      latitude: String(place.latitude),
      longitude: String(place.longitude),
      phone: place.phone ?? "",
      placeUrl: place.placeUrl ?? "",
      description: place.description ?? "",
      externalId: place.externalId ?? "",
    })

    window.scrollTo({ top: 0, behavior: "smooth" })
  }

  function getMemberId(member: AdminMemberResponse) {
    return member.id ?? member.memberId
  }

  async function handleDeletePlace(placeId: number) {
    if (!confirm("정말 이 장소를 삭제하시겠습니까?")) {
      return
    }

    try {
      await apiFetch<void>(`/api/admin/places/${placeId}`, {
        method: "DELETE",
      })

      alert("장소가 삭제되었습니다.")
      await loadPlaces(placePage)
    } catch (error) {
      console.error(error)
      alert(error instanceof Error ? error.message : "장소 삭제에 실패했습니다.")
    }
  }

  async function handleDeleteMember(memberId: number) {
    if (!confirm("정말 이 회원을 탈퇴 처리하시겠습니까?")) {
      return
    }

    try {
      await apiFetch<void>(`/api/admin/members/${memberId}`, {
        method: "DELETE",
      })

      alert("회원이 탈퇴 처리되었습니다.")
      await loadMembers()
    } catch (error) {
      console.error(error)
      alert(error instanceof Error ? error.message : "회원 탈퇴 처리에 실패했습니다.")
    }
  }

  if (checkingAuth) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <p className="text-sm text-muted-foreground">관리자 권한을 확인하는 중입니다...</p>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <SiteHeader />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-10">
        <div className="flex flex-col gap-2">
          <Badge variant="secondary" className="w-fit">
            관리자 콘솔
          </Badge>
          <h1 className="font-heading text-2xl font-bold tracking-tight">콘텐츠 관리</h1>
          <p className="text-sm text-muted-foreground">
            장소, 행사, 회원 정보를 관리합니다.
          </p>
        </div>

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
            <TabsTrigger value="members">회원</TabsTrigger>
            <TabsTrigger value="events">행사</TabsTrigger>
            <TabsTrigger value="areas">지역 혼잡도</TabsTrigger>
          </TabsList>

          <TabsContent value="places" className="mt-6">
            <Card className="border-border/60">
              <CardHeader className="flex-row items-center justify-between space-y-0">
                <CardTitle className="text-base">장소 관리</CardTitle>
                <Button
                  size="sm"
                  className="gap-1.5"
                  onClick={() => {
                    setEditingPlaceId(null)
                    setPlaceForm(emptyPlaceForm)
                    window.scrollTo({ top: 0, behavior: "smooth" })
                  }}
                >
                  <Plus className="size-4" /> 장소 추가
                </Button>
              </CardHeader>

              <CardContent>
                <div className="mb-6 rounded-xl border border-border/60 p-4">
                  <div className="mb-3 flex items-center justify-between">
                    <h3 className="text-sm font-semibold">
                      {editingPlaceId ? "장소 수정" : "장소 등록"}
                    </h3>
                    {editingPlaceId && (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => {
                          setEditingPlaceId(null)
                          setPlaceForm(emptyPlaceForm)
                        }}
                      >
                        취소
                      </Button>
                    )}
                  </div>

                  <div className="grid gap-2 md:grid-cols-2">
                    <input
                      className="rounded-md border border-border bg-background px-3 py-2 text-sm"
                      placeholder="카테고리 ID"
                      value={placeForm.categoryId}
                      onChange={(e) =>
                        setPlaceForm({ ...placeForm, categoryId: e.target.value })
                      }
                    />
                    <input
                      className="rounded-md border border-border bg-background px-3 py-2 text-sm"
                      placeholder="장소명"
                      value={placeForm.name}
                      onChange={(e) =>
                        setPlaceForm({ ...placeForm, name: e.target.value })
                      }
                    />
                    <input
                      className="rounded-md border border-border bg-background px-3 py-2 text-sm"
                      placeholder="주소"
                      value={placeForm.address}
                      onChange={(e) =>
                        setPlaceForm({ ...placeForm, address: e.target.value })
                      }
                    />
                    <input
                      className="rounded-md border border-border bg-background px-3 py-2 text-sm"
                      placeholder="도로명 주소"
                      value={placeForm.roadAddress}
                      onChange={(e) =>
                        setPlaceForm({ ...placeForm, roadAddress: e.target.value })
                      }
                    />
                    <input
                      className="rounded-md border border-border bg-background px-3 py-2 text-sm"
                      placeholder="위도"
                      value={placeForm.latitude}
                      onChange={(e) =>
                        setPlaceForm({ ...placeForm, latitude: e.target.value })
                      }
                    />
                    <input
                      className="rounded-md border border-border bg-background px-3 py-2 text-sm"
                      placeholder="경도"
                      value={placeForm.longitude}
                      onChange={(e) =>
                        setPlaceForm({ ...placeForm, longitude: e.target.value })
                      }
                    />
                    <input
                      className="rounded-md border border-border bg-background px-3 py-2 text-sm"
                      placeholder="전화번호"
                      value={placeForm.phone}
                      onChange={(e) =>
                        setPlaceForm({ ...placeForm, phone: e.target.value })
                      }
                    />
                    <input
                      className="rounded-md border border-border bg-background px-3 py-2 text-sm"
                      placeholder="장소 URL"
                      value={placeForm.placeUrl}
                      onChange={(e) =>
                        setPlaceForm({ ...placeForm, placeUrl: e.target.value })
                      }
                    />
                    <input
                      className="rounded-md border border-border bg-background px-3 py-2 text-sm"
                      placeholder="externalId"
                      value={placeForm.externalId}
                      onChange={(e) =>
                        setPlaceForm({ ...placeForm, externalId: e.target.value })
                      }
                    />
                    <input
                      className="rounded-md border border-border bg-background px-3 py-2 text-sm"
                      placeholder="설명"
                      value={placeForm.description}
                      onChange={(e) =>
                        setPlaceForm({ ...placeForm, description: e.target.value })
                      }
                    />
                  </div>

                  <div className="mt-3 flex justify-end">
                    <Button type="button" onClick={handleSubmitPlace} disabled={loading}>
                      {editingPlaceId ? "수정하기" : "등록하기"}
                    </Button>
                  </div>
                </div>

                <div className="mb-4 grid gap-2 md:grid-cols-5">
                  <input
                    className="rounded-md border border-border bg-background px-3 py-2 text-sm"
                    placeholder="장소명 검색"
                    value={keyword}
                    onChange={(e) => setKeyword(e.target.value)}
                  />
                  <input
                    className="rounded-md border border-border bg-background px-3 py-2 text-sm"
                    placeholder="카테고리 ID"
                    value={categoryId}
                    onChange={(e) => setCategoryId(e.target.value)}
                  />
                  <select
                    className="rounded-md border border-border bg-background px-3 py-2 text-sm"
                    value={isActive}
                    onChange={(e) => setIsActive(e.target.value)}
                  >
                    <option value="">전체 상태</option>
                    <option value="true">활성</option>
                    <option value="false">비활성</option>
                  </select>
                  <input
                    className="rounded-md border border-border bg-background px-3 py-2 text-sm"
                    placeholder="source"
                    value={source}
                    onChange={(e) => setSource(e.target.value)}
                  />
                  <Button type="button" onClick={() => loadPlaces(0)}>
                    검색
                  </Button>
                </div>

                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>이름</TableHead>
                      <TableHead>카테고리 ID</TableHead>
                      <TableHead>주소</TableHead>
                      <TableHead>출처</TableHead>
                      <TableHead>상태</TableHead>
                      <TableHead className="text-right">관리</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {places.length === 0 ? (
                      <TableRow>
                        <TableCell
                          colSpan={6}
                          className="py-8 text-center text-sm text-muted-foreground"
                        >
                          조회된 장소가 없습니다.
                        </TableCell>
                      </TableRow>
                    ) : (
                      places.map((p) => (
                        <TableRow key={p.id}>
                          <TableCell className="font-medium">{p.name}</TableCell>
                          <TableCell>
                            <Badge variant="secondary">{p.categoryId}</Badge>
                          </TableCell>
                          <TableCell className="text-muted-foreground">
                            {p.address}
                          </TableCell>
                          <TableCell className="text-muted-foreground">
                            {p.source}
                          </TableCell>
                          <TableCell>
                            <Badge variant={p.isActive ? "secondary" : "outline"}>
                              {p.isActive ? "활성" : "비활성"}
                            </Badge>
                          </TableCell>
                          <TableCell className="text-right">
                            {p.isActive ? (
                              <>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  onClick={() => handleEditPlace(p)}
                                >
                                  수정
                                </Button>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  onClick={() => handleDeletePlace(p.id)}
                                >
                                  삭제
                                </Button>
                              </>
                            ) : (
                              <>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  disabled
                                  className="cursor-not-allowed opacity-40"
                                >
                                  수정 불가
                                </Button>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  disabled
                                  className="cursor-not-allowed opacity-40"
                                >
                                  삭제됨
                                </Button>
                              </>
                            )}
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>

                <div className="mt-4 flex items-center justify-between text-sm text-muted-foreground">
                  <div>
                    총 {placeTotalElements}개 ·{" "}
                    {placeTotalPages === 0 ? 0 : placePage + 1} / {placeTotalPages} 페이지
                  </div>

                  <div className="flex gap-2">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={placePage === 0}
                      onClick={() => handleChangePlacePage(placePage - 1)}
                    >
                      이전
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={placePage + 1 >= placeTotalPages}
                      onClick={() => handleChangePlacePage(placePage + 1)}
                    >
                      다음
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="members" className="mt-6">
            <Card className="border-border/60">
              <CardHeader className="flex-row items-center justify-between space-y-0">
                <CardTitle className="text-base">회원 목록</CardTitle>
                <Button variant="outline" size="sm" onClick={loadMembers}>
                  새로고침
                </Button>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>ID</TableHead>
                      <TableHead>이메일</TableHead>
                      <TableHead>닉네임</TableHead>
                      <TableHead>권한</TableHead>
                      <TableHead>상태</TableHead>
                      <TableHead className="text-right">관리</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {members.length === 0 ? (
                      <TableRow>
                        <TableCell
                          colSpan={6}
                          className="py-8 text-center text-sm text-muted-foreground"
                        >
                          조회된 회원이 없습니다.
                        </TableCell>
                      </TableRow>
                    ) : (
                      members.map((m) => (
                        <TableRow key={getMemberId(m)}>
                          <TableCell>{getMemberId(m)}</TableCell>
                          <TableCell className="font-medium">{m.email}</TableCell>
                          <TableCell>{m.nickname}</TableCell>
                          <TableCell>
                            <Badge variant="secondary">{m.role}</Badge>
                          </TableCell>
                          <TableCell>
                            <Badge variant={m.status === "ACTIVE" ? "secondary" : "outline"}>
                              {m.status}
                            </Badge>
                          </TableCell>
                          <TableCell className="text-right">
                            <Button
                              variant="ghost"
                              size="sm"
                              disabled={m.status === "DELETED"}
                              onClick={() => {
                                const memberId = getMemberId(m)

                                if (!memberId) {
                                  alert("회원 ID를 찾을 수 없습니다.")
                                  return
                                }

                                handleDeleteMember(memberId)
                              }}
                            >
                              탈퇴 처리
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="events" className="mt-6">
            <Card className="border-border/60">
              <CardHeader className="flex-row items-center justify-between space-y-0">
                <CardTitle className="text-base">행사 목록</CardTitle>
                <Button size="sm" className="gap-1.5" disabled>
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
                      <TableHead>시간</TableHead>
                      <TableHead className="text-right">관리</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {events.length === 0 ? (
                      <TableRow>
                        <TableCell
                          colSpan={6}
                          className="py-8 text-center text-sm text-muted-foreground"
                        >
                          조회된 행사가 없습니다.
                        </TableCell>
                      </TableRow>
                    ) : (
                      events.map((e) => (
                        <TableRow key={e.id}>
                          <TableCell className="font-medium">{e.title}</TableCell>
                          <TableCell>
                            <Badge variant="secondary">{e.categoryName}</Badge>
                          </TableCell>
                          <TableCell className="text-muted-foreground">
                            {e.area ?? "-"}
                          </TableCell>
                          <TableCell className="text-muted-foreground">
                            {e.startDate} ~ {e.endDate}
                          </TableCell>
                          <TableCell className="text-muted-foreground">
                            {e.eventTime ?? "-"}
                          </TableCell>
                          <TableCell className="text-right">
                            <Button variant="ghost" size="sm" disabled>
                              수정
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>

                <div className="mt-4 flex items-center justify-between text-sm text-muted-foreground">
                  <div>
                    총 {eventTotalElements}개 ·{" "}
                    {eventTotalPages === 0 ? 0 : eventPage + 1} / {eventTotalPages} 페이지
                  </div>

                  <div className="flex gap-2">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={eventPage === 0}
                      onClick={() => handleChangeEventPage(eventPage - 1)}
                    >
                      이전
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={eventPage + 1 >= eventTotalPages}
                      onClick={() => handleChangeEventPage(eventPage + 1)}
                    >
                      다음
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="areas" className="mt-6">
            <Card className="border-border/60">
              <CardHeader className="flex-row items-center justify-between space-y-0">
                <div>
                  <CardTitle className="text-base">지역별 실시간 혼잡도</CardTitle>
                  <p className="mt-1 text-xs text-muted-foreground">
                    서울시 실시간 도시데이터 API 기준 혼잡도입니다.
                  </p>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={loadAreaCrowds}
                  disabled={areaCrowdLoading}
                >
                  {areaCrowdLoading ? "갱신 중..." : "새로고침"}
                </Button>
              </CardHeader>
              <CardContent>
                {areaCrowdError && (
                  <div className="mb-4 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-700">
                    {areaCrowdError}
                  </div>
                )}

                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                  {areaCrowds.map((a) => {
                    const measuredAt = formatMeasuredAt(a.measuredAt)

                    return (
                      <div
                        key={a.name}
                        className="flex items-center justify-between gap-3 rounded-xl border border-border/60 px-4 py-3"
                      >
                        <div className="min-w-0">
                          <div className="flex items-center gap-2">
                            <p className="font-medium">{a.name}</p>
                            {!a.isRealtime && (
                              <Badge variant="outline" className="text-[10px]">
                                기본값
                              </Badge>
                            )}
                          </div>
                          <p className="text-xs text-muted-foreground">
                            {formatPopulationRange(
                              a.populationMin,
                              a.populationMax,
                              CROWD_META[a.crowd].range,
                            )}
                          </p>
                          <p className="mt-1 text-[11px] text-muted-foreground">
                            {measuredAt ? `기준 ${measuredAt}` : "실시간 기준 정보 없음"}
                          </p>
                          {a.message && (
                            <p className="mt-1 line-clamp-2 text-[11px] text-muted-foreground">
                              {a.message}
                            </p>
                          )}
                        </div>
                        <CrowdBadge level={a.crowd} />
                      </div>
                    )
                  })}
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </main>
    </div>
  )
}