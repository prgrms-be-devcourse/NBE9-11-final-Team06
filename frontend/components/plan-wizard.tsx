"use client"

import { useState } from "react"
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
  SEOUL_AREAS,
  type Category,
  type Companion,
} from "@/lib/data"
import { SiteHeader } from "@/components/site-header"
import { NaverLocationPicker } from "@/components/naver-location-picker"

const STEPS = ["날짜", "위치", "동행", "취향"]

type SelectedLocation = {
  name: string
  address?: string
  latitude?: number
  longitude?: number
  source: "preset" | "naver"
}

export function PlanWizard() {
  const router = useRouter()
  const [step, setStep] = useState(0)
  const [date, setDate] = useState<Date | undefined>(new Date())
  const [area, setArea] = useState<string | null>(null)
  const [selectedLocation, setSelectedLocation] = useState<SelectedLocation | null>(null)
  const [locationKeyword, setLocationKeyword] = useState("")
  const [companion, setCompanion] = useState<Companion | null>(null)
  const [categories, setCategories] = useState<Category[]>([])

  const canNext =
    (step === 0 && !!date) ||
    (step === 1 && !!selectedLocation) ||
    (step === 2 && !!companion) ||
    step === 3

  function toggleCategory(c: Category) {
    setCategories((prev) =>
      prev.includes(c) ? prev.filter((x) => x !== c) : [...prev, c],
    )
  }

  function submit() {
    const params = new URLSearchParams()
    if (date) {
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, "0")
      const day = String(date.getDate()).padStart(2, "0")
      params.set("date", `${year}-${month}-${day}`)
    }
    if (selectedLocation) {
      params.set("area", area ?? selectedLocation.name)
      params.set("locationName", selectedLocation.name)
      params.set("locationSource", selectedLocation.source)
      if (selectedLocation.address) params.set("locationAddress", selectedLocation.address)
      if (selectedLocation.latitude !== undefined) params.set("lat", String(selectedLocation.latitude))
      if (selectedLocation.longitude !== undefined) params.set("lng", String(selectedLocation.longitude))
    }
    if (companion) params.set("companion", companion)
    if (categories.length) params.set("cats", categories.join(","))
    router.push(`/recommend?${params.toString()}`)
  }

  return (
    <div className="flex min-h-screen flex-col">
      <SiteHeader />
      <main className="mx-auto w-full max-w-2xl flex-1 px-4 py-10 sm:px-6">
        {/* progress */}
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
                  disabled={{ before: new Date(new Date().setHours(0, 0, 0, 0)) }}
                  className="rounded-2xl border"
                />
              </div>
            </div>
          )}

          {step === 1 && (
            <div className="flex flex-col gap-4">
              <StepHeader
                icon={MapPin}
                title="어디로 가볼까요?"
                desc="장소명이나 주소를 검색하거나, 기본 지역을 선택해 주세요."
              />
              <NaverLocationPicker
                initialKeyword={locationKeyword}
                onSelect={(location) => {
                  setArea(null)
                  setLocationKeyword(location.name)
                  setSelectedLocation(location)
                }}
              />

              <div className="space-y-2">
                <p className="text-sm font-semibold text-muted-foreground">또는 기본 지역 선택</p>
                <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
                  {SEOUL_AREAS.map((a) => (
                    <button
                      key={a.name}
                      type="button"
                      onClick={() => {
                        setArea(a.name)
                        setLocationKeyword("")
                        setSelectedLocation({
                          name: a.name,
                          source: "preset",
                        })
                      }}
                      className={cn(
                        "flex items-center gap-2 rounded-2xl border p-4 text-left transition-all",
                        area === a.name
                          ? "border-primary bg-primary/5 ring-1 ring-primary"
                          : "border-border hover:border-primary/50",
                      )}
                    >
                      <MapPin
                        className={cn(
                          "size-4",
                          area === a.name ? "text-primary" : "text-muted-foreground",
                        )}
                      />
                      <span className="font-semibold">{a.name}</span>
                    </button>
                  ))}
                </div>
              </div>

              {selectedLocation && (
                <div className="rounded-2xl border bg-secondary/30 p-4">
                  <p className="text-sm font-semibold text-muted-foreground">선택된 위치</p>
                  <div className="mt-2 flex items-start gap-2">
                    <MapPin className="mt-0.5 size-4 text-primary" />
                    <div>
                      <p className="font-semibold">{selectedLocation.name}</p>
                      <p className="text-sm text-muted-foreground">
                        {selectedLocation.source === "preset" ? "기본 지역 선택" : "네이버 지도 선택"}
                      </p>
                      {selectedLocation.address && (
                        <p className="mt-1 text-sm text-muted-foreground">{selectedLocation.address}</p>
                      )}
                      {selectedLocation.latitude !== undefined && selectedLocation.longitude !== undefined && (
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
                    onClick={() => setCompanion(c.value)}
                    className={cn(
                      "flex flex-col gap-1 rounded-2xl border p-4 text-left transition-all",
                      companion === c.value
                        ? "border-primary bg-primary/5 ring-1 ring-primary"
                        : "border-border hover:border-primary/50",
                    )}
                  >
                    <span className="font-semibold">{c.value}</span>
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
                title="어떤 걸 좋아하세요?"
                desc="원하는 카테고리를 모두 골라주세요. (선택 안 하면 전체 추천)"
              />
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                {CATEGORIES.map((c) => {
                  const active = categories.includes(c.value)
                  return (
                    <button
                      key={c.value}
                      type="button"
                      onClick={() => toggleCategory(c.value)}
                      className={cn(
                        "flex flex-col items-center gap-1.5 rounded-2xl border p-4 transition-all",
                        active
                          ? "border-primary bg-primary/5 ring-1 ring-primary"
                          : "border-border hover:border-primary/50",
                      )}
                    >
                      <span className="text-2xl" aria-hidden>
                        {c.emoji}
                      </span>
                      <span className="text-sm font-semibold">{c.label}</span>
                    </button>
                  )
                })}
              </div>
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
              <Button onClick={submit} className="gap-1">
                <Sparkles className="size-4" />
                코스 추천받기
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
