"use client"

import { useEffect, useRef } from "react"

type Point = {
  id: number
  title: string
  latitude: number
  longitude: number
  type: "event" | "restaurant" | "cafe"
}

declare global {
  interface Window {
    naver: any
  }
}

export function SimpleNaverMap({
  points,
  selectedRestaurantId,
  selectedCafeId,
  onSelect,
}: {
  points: Point[]
  selectedRestaurantId?: number | null
  selectedCafeId?: number | null
  onSelect?: (p: Point) => void
}) 

{
  const mapRef = useRef<HTMLDivElement>(null)
  const mapInstanceRef = useRef<any>(null)
  const infoWindowRef = useRef<any>(null)

  useEffect(() => {
    if (!mapRef.current || !window.naver || points.length === 0) return

    const { naver } = window

    // 👉 기존 map 있으면 제거
    mapRef.current.innerHTML = ""

    const center = new naver.maps.LatLng(
      points[0].latitude,
      points[0].longitude
    )

    const map = new naver.maps.Map(mapRef.current, {
      center,
      zoom: 13,
    })

    mapInstanceRef.current = map

    // 👉 하나만 재사용하는 InfoWindow (핵심 최적화)
    const infoWindow = new naver.maps.InfoWindow({
      content: "",
    })

    infoWindowRef.current = infoWindow

    const bounds = new naver.maps.LatLngBounds()

    points.forEach((p, idx) => {
      const position = new naver.maps.LatLng(p.latitude, p.longitude)

      bounds.extend(position)

      const isSelected =
      (p.type === "restaurant" && p.id === selectedRestaurantId) ||
      (p.type === "cafe" && p.id === selectedCafeId)
    

      let icon = "📍"

      if (p.type === "restaurant") icon = "🍽️"
      if (p.type === "cafe") icon = "☕"

      const size = isSelected ? 40 : 34
      const fontSize = isSelected ? 24 : 20
      let color = "#2563eb"
      
      // 기본 타입 색
      if (p.type === "restaurant") color = "#dc2626"
      if (p.type === "cafe") color = "#16a34a"
      
      // 🔥 선택된 경우 override
      if (isSelected) {
        if (p.type === "restaurant") color = "#f59e0b" // 선택: 주황
        if (p.type === "cafe") color = "#f59e0b"
      }

      const marker = new naver.maps.Marker({
      position,
      map,
      title: p.title,
      icon: {
        content: `
          <div style="
            width:${size}px;
            height:${size}px;
            display:flex;
            align-items:center;
            justify-content:center;
            font-size:${fontSize}px;
            background:white;
            border-radius:50%;
            border:2px solid ${color};
            box-shadow:0 2px 6px rgba(0,0,0,0.2);
          ">
            ${icon}
          </div>
        `,
        anchor: new naver.maps.Point(size / 2, size / 2),
      },
    })
      naver.maps.Event.addListener(marker, "click", () => {
        infoWindow.setContent(
          `<div style="padding:6px;font-size:12px;">
            ${idx + 1}. ${p.title}
          </div>`
        )
        infoWindow.open(map, marker)

        onSelect?.(p)
      })
    })

    map.fitBounds(bounds)
  }, [points, selectedRestaurantId, selectedCafeId])

  return <div ref={mapRef} className="w-full h-[400px] rounded-lg" />
}