"use client"

import { useState, useMemo } from "react"
import { useRouter } from "next/navigation"
import {
    CalendarDays,
    MapPin,
    Users,
    Check,
    ArrowRight,
    ArrowLeft,
    Sparkles,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Calendar } from "@/components/ui/calendar"
import { cn } from "@/lib/utils"
import {
    CATEGORIES,
    COMPANIONS,
    RestaurantType,
    type Companion,
} from "@/lib/data"
import { SiteHeader } from "@/components/site-header"
import { NaverLocationPicker } from "@/components/naver-location-picker"

const STEPS = ["날짜", "위치", "동행", "음식", "취향"]
const MAX_CATEGORIES = 5

type SelectedLocation = {
    name: string
    address?: string
    latitude?: number
    longitude?: number
    source: "preset" | "naver"
}

type CategoryOption = (typeof CATEGORIES)[number]

export function PlanWizard() {
    const router = useRouter()
    const [step, setStep] = useState(0)
    const [date, setDate] = useState<Date | undefined>(new Date())
    const [area, setArea] = useState<string | null>(null)
    const [selectedLocation, setSelectedLocation] = useState<SelectedLocation | null>(null)
    const [locationKeyword, setLocationKeyword] = useState("")
    const [companion, setCompanion] = useState<Companion | null>(null)
    const [categories, setCategories] = useState<CategoryOption[]>([])
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [submitError, setSubmitError] = useState<string | null>(null)
    const [restaurantType, setRestaurantType] = useState<RestaurantType | null>(null)
    const disabledDays = useMemo(() => {
      const today = new Date()
      today.setHours(0, 0, 0, 0)
      const maxDate = new Date(today)
      maxDate.setMonth(maxDate.getMonth() + 1)
      return [{ before: today }, { after: maxDate }]
  }, [])

    const canNext =
        (step === 0 && !!date) ||
        (step === 1 && !!selectedLocation) ||
        (step === 2 && !!companion) ||
        (step === 3 && !!restaurantType) ||
        (step === 4 && categories.length > 0)

    function getCategoryIdentity(category: CategoryOption) {
        return String(category.value ?? category.label ?? "")
    }

    function toggleCategory(category: CategoryOption) {
        setCategories((prev) => {
            const categoryIdentity = getCategoryIdentity(category)
            const alreadySelected = prev.some(
                (selectedCategory) => getCategoryIdentity(selectedCategory) === categoryIdentity
            )

            if (alreadySelected) {
                setSubmitError(null)
                return prev.filter(
                    (selectedCategory) => getCategoryIdentity(selectedCategory) !== categoryIdentity
                )
            }

            if (prev.length >= MAX_CATEGORIES) {
                setSubmitError(`카테고리는 최대 ${MAX_CATEGORIES}개까지 선택할 수 있습니다.`)
                return prev
            }

            setSubmitError(null)
            return [...prev, category]
        })
    }

    function formatDate(value: Date) {
        const year = value.getFullYear()
        const month = String(value.getMonth() + 1).padStart(2, "0")
        const day = String(value.getDate()).padStart(2, "0")
        return `${year}-${month}-${day}`
    }

    function toCategoryCode(category: unknown): string | null {
        const candidates: string[] = []

        if (typeof category === "string") {
            candidates.push(category)
        } else if (typeof category === "object" && category !== null) {
            const record = category as Record<string, unknown>

            candidates.push(
                String(record.value ?? ""),
                String(record.name ?? ""),
                String(record.label ?? ""),
                String(record.description ?? "")
            )
        } else {
            candidates.push(String(category ?? ""))
        }

        const normalizedCandidates = candidates
            .map((candidate) => candidate.trim())
            .filter(Boolean)

        if (normalizedCandidates.length === 0) {
            return null
        }

        const hasTourKeyword = normalizedCandidates.some((candidate) => {
            const upperValue = candidate.toUpperCase()

            return (
                upperValue === "TOUR" ||
                upperValue === "TOUR_PLACE" ||
                upperValue === "TOURISM" ||
                upperValue === "TRAVEL" ||
                candidate.includes("관광") ||
                candidate.includes("여행")
            )
        })

        if (hasTourKeyword) {
            return "TOUR"
        }

        return normalizedCandidates[0]
    }

    function toRecommendationCategories(
        selectedCategories: CategoryOption[]
    ): string[] {
        return Array.from(
            new Set(
                selectedCategories
                    .map(toCategoryCode)
                    .filter((category): category is string => Boolean(category))
            )
        )
    }

    function getBaseAreaValue(
        selectedLocation: SelectedLocation,
        area: string | null
    ): string {
        if (area?.trim()) {
            return area.trim()
        }

        const address = selectedLocation.address ?? ""
        const districtMatch = address.match(/서울(?:특별시)?\s*([가-힣]+구)/)

        if (districtMatch?.[1]) {
            return districtMatch[1]
        }

        return selectedLocation.name
    }

    function normalizeAccessToken(value: string | null | undefined) {
        if (!value) return null

        const token = value.trim().replace(/^Bearer\s+/i, "")

        if (!token || token === "undefined" || token === "null") {
            return null
        }

        return token
    }

    function getAccessToken() {
        if (typeof window === "undefined") return null

        const storages = [localStorage, sessionStorage]
        const tokenKeys = ["accessToken", "access_token", "token", "jwt"]

        for (const storage of storages) {
            for (const tokenKey of tokenKeys) {
                const token = normalizeAccessToken(storage.getItem(tokenKey))
                if (token) return token
            }
        }

        for (const storage of storages) {
            for (let i = 0; i < storage.length; i++) {
                const key = storage.key(i)
                if (!key) continue

                const value = storage.getItem(key)
                if (!value) continue

                const rawToken = normalizeAccessToken(value)
                if (rawToken?.startsWith("eyJ")) {
                    return rawToken
                }

                try {
                    const parsed = JSON.parse(value)
                    const token = findAccessToken(parsed)
                    if (token) return token
                } catch {
                    // JSON이 아닌 값은 건너뜁니다.
                }
            }
        }

        return null
    }

    function findAccessToken(value: any): string | null {
        if (!value) return null

        if (typeof value === "string") {
            const token = normalizeAccessToken(value)
            return token?.startsWith("eyJ") ? token : null
        }

        if (typeof value !== "object") return null

        const directToken =
            normalizeAccessToken(value.accessToken) ??
            normalizeAccessToken(value.access_token) ??
            normalizeAccessToken(value.token) ??
            normalizeAccessToken(value.jwt)

        if (directToken) {
            return directToken
        }

        for (const nestedValue of Object.values(value)) {
            const token = findAccessToken(nestedValue)
            if (token) return token
        }

        return null
    }

    function extractRecommendedCourse(result: any) {
        return (
            result?.data ??
            result?.result ??
            result?.body ??
            result?.content ??
            result?.response ??
            result
        )
    }


    async function submit() {
        if (!date || !selectedLocation) return
        

        setIsSubmitting(true)
        setSubmitError(null)

        const params = new URLSearchParams()
        const selectedDate = formatDate(date)
        const baseArea = getBaseAreaValue(selectedLocation, area)
        const recommendationCategories = toRecommendationCategories(categories)


        const coursePreviewRequest = {
            courseType: "RECOMMENDATION",
            startDate: selectedDate,
            endDate: selectedDate,
            baseArea: baseArea,
            categories: recommendationCategories,
            companionType: companion,
            restaurantType: restaurantType,
            startLatitude: selectedLocation.latitude,
            startLongitude: selectedLocation.longitude,
        }

        localStorage.setItem(
            "coursePreviewRequest",
            JSON.stringify(coursePreviewRequest)
        )

        localStorage.setItem(
            "recommendationCategories",
            JSON.stringify(recommendationCategories)
        )

        console.log("recommendationCategories:", recommendationCategories)
        console.log("coursePreviewRequest 저장값:", coursePreviewRequest)

        params.set("date", selectedDate)
        params.set("area", baseArea)
        params.set("locationName", selectedLocation.name)
        params.set("locationSource", selectedLocation.source)

        if (selectedLocation.address) {
            params.set("locationAddress", selectedLocation.address)
        }

        if (selectedLocation.latitude !== undefined) {
            params.set("lat", String(selectedLocation.latitude))
        }

        if (selectedLocation.longitude !== undefined) {
            params.set("lng", String(selectedLocation.longitude))
        }

        if (companion) {
            params.set("companion", companion)
        }

        if (recommendationCategories.length) {
            params.set("cats", recommendationCategories.join(","))
        }

        try {
            const accessToken = getAccessToken()
            const headers: HeadersInit = {
                "Content-Type": "application/json",
            }

            if (accessToken) {
                headers.Authorization = `Bearer ${accessToken}`
            }

            const response = await fetch(`/api/recommendations/candidates`, {
                method: "POST",
                redirect: "manual",
                credentials: "include",
                headers,
                body: JSON.stringify({
                    title: `${baseArea} 추천 후보`,
                    startDate: selectedDate,
                    endDate: selectedDate,
                    area: baseArea,
                    categories: recommendationCategories,
                    companionType: companion,
                    address: selectedLocation.address ?? selectedLocation.name,
                    latitude: selectedLocation.latitude,
                    longitude: selectedLocation.longitude,
                }),
            })

            const result = await response.json().catch(() => null)

            if (
                response.status === 0 ||
                response.status === 302 ||
                response.type === "opaqueredirect"
            ) {
                throw new Error("로그인 인증이 만료되었거나 토큰이 전달되지 않았습니다. 다시 로그인해주세요.")
            }

            if (!response.ok) {
                throw new Error(result?.message ?? result?.error ?? "코스 추천 생성에 실패했습니다.")
            }

            const recommendationResult = extractRecommendedCourse(result)
            const candidates = Array.isArray(recommendationResult?.candidates)
                ? recommendationResult.candidates
                : []

            if (candidates.length === 0) {
                throw new Error("추천 후보를 찾지 못했습니다.")
            }

            const eventIds = candidates
                .filter((candidate: any) => candidate?.type === "EVENT")
                .map((candidate: any) => Number(candidate.eventId))
                .filter(Number.isFinite)

            const tourIds = candidates
                .filter((candidate: any) => candidate?.type === "TOUR")
                .map((candidate: any) => Number(candidate.tourId))
                .filter(Number.isFinite)

            localStorage.setItem("recommendedEventIds", JSON.stringify(eventIds))
            localStorage.setItem("recommendedTourIds", JSON.stringify(tourIds))
            sessionStorage.setItem(
                "recommendationCandidates",
                JSON.stringify(recommendationResult)
            )

            console.log("추천 후보 조회 응답:", result)
            console.log("추출된 추천 후보:", recommendationResult)
            console.log("추천 eventIds:", eventIds)
            console.log("추천 tourIds:", tourIds)

            params.set("candidateMode", "true")
            router.push(`/recommend?${params.toString()}`)
        } catch (error) {
            setSubmitError(error instanceof Error ? error.message : "추천 후보 조회에 실패했습니다.")
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <div className="flex min-h-screen flex-col">
            <SiteHeader />
            <main className="mx-auto w-full max-w-2xl flex-1 px-4 py-10 sm:px-6">
                <div className="mb-8">
                    <div className="flex items-center justify-between">
                        {STEPS.map((s, i) => (
                            <div key={s} className="flex flex-1 items-center">
                                <div className="flex flex-col items-center gap-1.5">
                                    <span
                                        className={cn(
                                            "flex size-9 items-center justify-center rounded-full text-sm font-bold transition-colors",
                                            i < step
                                                ? "bg-primary text-primary-foreground"
                                                : i === step
                                                    ? "bg-primary text-primary-foreground ring-4 ring-primary/20"
                                                    : "bg-secondary text-muted-foreground",
                                        )}
                                    >
                                        {i < step ? <Check className="size-4" /> : i + 1}
                                    </span>

                                    <span
                                        className={cn(
                                            "text-xs font-medium",
                                            i <= step ? "text-foreground" : "text-muted-foreground",
                                        )}
                                    >
                                        {s}
                                    </span>
                                </div>
                                {i < STEPS.length - 1 && (
                                    <div
                                        className={cn(
                                            "mx-1 mb-5 h-0.5 flex-1 rounded-full",
                                            i < step ? "bg-primary" : "bg-border",
                                        )}
                                    />
                                )}
                            </div>
                        ))}
                    </div>
                </div>

                <Card className="p-6 sm:p-8">
                    {step === 0 && (
                        <div className="flex flex-col gap-4">
                            <StepHeader
                                icon={CalendarDays}
                                title="언제 떠나시나요?"
                                desc="방문할 날짜를 선택하면 그날 운영 중인 행사를 찾아드려요."
                            />

                            <div className="flex justify-center">
                                <Calendar
                                    mode="single"
                                    selected={date}
                                    onSelect={setDate}
                                    disabled={disabledDays}
                                    className="rounded-2xl border"
                                />
                            </div>
                        </div>
                    )}

                    {step === 1 && (
                        <div className="flex flex-col gap-4">
                            <StepHeader
                                icon={MapPin}
                                title="어디에서 출발하시나요?"
                                desc="출발할 장소를 검색하거나 지도에서 직접 선택해 주세요."
                            />

                            <NaverLocationPicker
                                initialKeyword={locationKeyword}
                                onSelect={(location) => {
                                    setArea(null)
                                    setLocationKeyword(location.name)
                                    setSelectedLocation(location)
                                }}
                            />

                            {selectedLocation && (
                                <div className="rounded-2xl border bg-secondary/30 p-4">
                                    <p className="text-sm font-semibold text-muted-foreground">
                                        선택된 위치
                                    </p>

                                    <div className="mt-2 flex items-start gap-2">
                                        <MapPin className="mt-0.5 size-4 text-primary" />

                                        <div>
                                            <p className="font-semibold">{selectedLocation.name}</p>

                                            <p className="text-sm text-muted-foreground">
                                                {selectedLocation.source === "preset"
                                                    ? "기본 지역 선택"
                                                    : "네이버 지도 선택"}
                                            </p>

                                            {selectedLocation.address && (
                                                <p className="mt-1 text-sm text-muted-foreground">
                                                    {selectedLocation.address}
                                                </p>
                                            )}

                                            {selectedLocation.latitude !== undefined &&
                                                selectedLocation.longitude !== undefined && (
                                                    <p className="mt-1 text-xs text-muted-foreground">
                                                        위도 {selectedLocation.latitude}, 경도 {selectedLocation.longitude}
                                                    </p>
                                                )}
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>
                    )}

                    {step === 2 && (
                        <div className="flex flex-col gap-4">
                            <StepHeader
                                icon={Users}
                                title="누구와 함께 가나요?"
                                desc="동행 유형에 맞는 분위기의 코스를 추천해 드려요."
                            />

                            <div className="grid gap-3 sm:grid-cols-2">
                                {COMPANIONS.map((c) => (
                                    <button
                                        key={c.value}
                                        type="button"
                                        onClick={() => setCompanion(c.value as Companion)}
                                        className={cn(
                                            "flex flex-col gap-1 rounded-2xl border p-4 text-left transition-all",
                                            companion === c.value
                                                ? "border-primary bg-primary/5 ring-1 ring-primary"
                                                : "border-border hover:border-primary/50",
                                        )}
                                    >
                                        <span className="font-semibold">{c.label}</span>
                                        <span className="text-sm text-muted-foreground">{c.desc}</span>
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}

                    {step === 3 && (
                        <div className="flex flex-col gap-4">
                            <StepHeader
                                icon={Sparkles}
                                title="어떤 음식을 좋아하세요?"
                                desc="식당 추천을 위해 음식 종류를 선택해주세요. (선택안함 선택시, 카페 식당 추천 안함)"
                            />

                            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                                {[
                                    { value: "KOREAN", label: "한식", emoji: "🍚" },
                                    { value: "WESTERN", label: "양식", emoji: "🍝" },
                                    { value: "JAPANESE", label: "일식", emoji: "🍣" },
                                    { value: "CHINESE", label: "중식", emoji: "🥟" },
                                    { value: "NONE", label: "선택안함", emoji: "❌" },
                                ].map((type) => (
                                    <button
                                        key={type.value}
                                        type="button"
                                        onClick={() => setRestaurantType(type.value as RestaurantType)}
                                        className={cn(
                                            "flex flex-col items-center gap-1.5 rounded-2xl border p-4",
                                            restaurantType === type.value
                                                ? "border-primary bg-primary/5 ring-1 ring-primary"
                                                : "border-border hover:border-primary/50"
                                        )}
                                    >
                                        <span className="text-2xl">{type.emoji}</span>
                                        <span className="text-sm font-semibold">{type.label}</span>
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}

                    {step === 4 && (
                        <div className="flex flex-col gap-4">
                            <StepHeader
                                icon={Sparkles}
                                title="어떤 하루를 보내고 싶으세요?"
                                desc={`원하는 분위기를 골라주세요. (최대 ${MAX_CATEGORIES}개)`}
                            />

                            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                                {CATEGORIES.map((category) => {
                                    const active = categories.some(
                                        (selectedCategory) =>
                                            getCategoryIdentity(selectedCategory) === getCategoryIdentity(category)
                                    )

                                    return (
                                        <button
                                            key={getCategoryIdentity(category)}
                                            type="button"
                                            onClick={() => toggleCategory(category)}
                                            className={cn(
                                                "flex flex-col items-center gap-1.5 rounded-2xl border p-4",
                                                active
                                                    ? "border-primary bg-primary/5 ring-1 ring-primary"
                                                    : "border-border hover:border-primary/50"
                                            )}
                                        >
                                            <span className="text-2xl">{category.emoji}</span>

                                            {category.description && (
                                                <span className="text-center text-xs text-muted-foreground">
                                                    {category.description}
                                                </span>
                                            )}

                                            <span className="text-sm font-semibold">{category.label}</span>
                                        </button>
                                    )
                                })}
                            </div>
                        </div>
                    )}

                    {submitError && (
                        <div className="mt-6 rounded-2xl border border-destructive/30 bg-destructive/5 p-4 text-sm font-medium text-destructive">
                            {submitError}
                        </div>
                    )}

                    <div className="mt-8 flex items-center justify-between">
                        <Button
                            variant="ghost"
                            onClick={() => setStep((s) => Math.max(0, s - 1))}
                            disabled={step === 0}
                            className="gap-1"
                        >
                            <ArrowLeft className="size-4" />
                            이전
                        </Button>

                        {step < STEPS.length - 1 ? (
                            <Button
                                onClick={() => setStep((s) => s + 1)}
                                disabled={!canNext}
                                className="gap-1"
                            >
                                다음
                                <ArrowRight className="size-4" />
                            </Button>
                        ) : (
                            <Button
                                onClick={submit}
                                disabled={isSubmitting || !restaurantType}
                                className="gap-1"
                            >
                                <Sparkles className="size-4" />
                                {isSubmitting ? "추천 후보 찾는 중..." : "코스 추천받기"}
                            </Button>
                        )}
                    </div>
                </Card>
            </main>
        </div>
    )
}

function StepHeader({
    icon: Icon,
    title,
    desc,
}: {
    icon: React.ElementType
    title: string
    desc: string
}) {
    return (
        <div className="flex flex-col gap-2">
            <span className="flex size-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
                <Icon className="size-5" />
            </span>

            <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
            <p className="text-muted-foreground">{desc}</p>
        </div>
    )
}