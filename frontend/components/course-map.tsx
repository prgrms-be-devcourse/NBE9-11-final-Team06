"use client"

import { useState } from "react"
import { MapPin } from "lucide-react"
import { type CourseStop } from "@/lib/data"
import { cn } from "@/lib/utils"

export function CourseMap({ stops }: { stops: CourseStop[] }) {
  const [active, setActive] = useState(0)

  const lats = stops.map((s) => s.place.lat)
  const lngs = stops.map((s) => s.place.lng)
  const minLat = Math.min(...lats)
  const maxLat = Math.max(...lats)
  const minLng = Math.min(...lngs)
  const maxLng = Math.max(...lngs)
  const pad = 0.12

  const points = stops.map((s) => {
    const x =
      ((s.place.lng - minLng) / (maxLng - minLng || 1)) * (1 - 2 * pad) + pad
    const y =
      ((maxLat - s.place.lat) / (maxLat - minLat || 1)) * (1 - 2 * pad) + pad
    return { x: x * 100, y: y * 100 }
  })

  const path = points.map((p) => `${p.x},${p.y}`).join(" ")

  return (
    <div className="relative aspect-square w-full overflow-hidden rounded-2xl border border-border bg-[oklch(0.97_0.02_220)]">
      {/* stylized grid */}
      <svg
        className="absolute inset-0 size-full"
        viewBox="0 0 100 100"
        preserveAspectRatio="none"
        aria-hidden
      >
        <defs>
          <pattern id="grid" width="10" height="10" patternUnits="userSpaceOnUse">
            <path
              d="M 10 0 L 0 0 0 10"
              fill="none"
              stroke="oklch(0.9 0.02 220)"
              strokeWidth="0.4"
            />
          </pattern>
        </defs>
        <rect width="100" height="100" fill="url(#grid)" />
        {/* faux river */}
        <path
          d="M -5 78 Q 30 70 55 82 T 105 80 L 105 105 L -5 105 Z"
          fill="oklch(0.88 0.05 220)"
          opacity="0.7"
        />
        {/* route line */}
        <polyline
          points={path}
          fill="none"
          stroke="oklch(0.62 0.14 230)"
          strokeWidth="1"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeDasharray="2.5 2"
        />
      </svg>

      {/* markers */}
      {points.map((p, i) => {
        const stop = stops[i]
        const isActive = active === i
        return (
          <button
            key={stop.place.id}
            type="button"
            onClick={() => setActive(i)}
            className="absolute -translate-x-1/2 -translate-y-1/2"
            style={{ left: `${p.x}%`, top: `${p.y}%` }}
            aria-label={`${stop.order}번 ${stop.place.name}`}
          >
            <span
              className={cn(
                "flex size-8 items-center justify-center rounded-full border-2 border-background text-sm font-bold shadow-md transition-all",
                isActive
                  ? "scale-125 bg-accent text-accent-foreground"
                  : "bg-primary text-primary-foreground",
              )}
            >
              {stop.order}
            </span>
          </button>
        )
      })}

      {/* active info card */}
      <div className="absolute inset-x-3 bottom-3 flex items-center gap-3 rounded-xl bg-background/95 p-3 shadow-lg backdrop-blur">
        <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
          <MapPin className="size-4" />
        </span>
        <div className="min-w-0">
          <p className="truncate text-sm font-bold">
            {stops[active].order}. {stops[active].place.name}
          </p>
          <p className="truncate text-xs text-muted-foreground">
            {stops[active].place.address}
          </p>
        </div>
        <span className="ml-auto shrink-0 text-xs font-semibold text-muted-foreground">
          {stops[active].arrive} 도착
        </span>
      </div>
    </div>
  )
}
