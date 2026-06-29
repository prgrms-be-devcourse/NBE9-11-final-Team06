import { useEffect, useRef } from "react"

type Point = {
  id: string | number
  title: string
  latitude: number
  longitude: number
  order: number
  type: "event" | "tour" | "restaurant" | "cafe"
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


      let bgColor = "white"
      let borderColor = "#000"
      let textColor = "#111"
      let shadow = "0 2px 6px rgba(0,0,0,0.2)"


      // 기본 타입 색
      if (p.type === "restaurant") borderColor = "#dc2626" // 빨강
      if (p.type === "cafe") borderColor = "#16a34a"       // 초록

      // 🔥 선택된 경우 override
      if (isSelected) {
        if (p.type === "restaurant") {
          bgColor = "#dc2626" // 빨강 배경
          borderColor = "white"
          textColor = "white"
          shadow = "0 0 0 4px rgba(220, 38, 38, 0.35)"
        }

        if (p.type === "cafe") {
          bgColor = "#16a34a" // 초록 배경
          borderColor = "white"
          textColor = "white"
          shadow = "0 0 0 4px rgba(22, 163, 74, 0.35)"
        }
      }

      const markerStyle = [
        `width:${size}px`,
        `height:${size}px`,
        "display:flex",
        "align-items:center",
        "justify-content:center",
        `font-size:${fontSize}px`,
        `background:${bgColor}`,
        `color:${textColor}`,
        "border-radius:50%",
        "border:2px solid",
        `border-color:${borderColor}`,
        `box-shadow:${shadow}`,
        "transition:all 0.2s ease",
      ].join(";")

      const marker = new naver.maps.Marker({
      position,
      map,
      title: p.title,
      icon: {
        content: `
        <div style="${markerStyle}">
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

  return <div ref={mapRef} className="h-100 w-full rounded-lg" />
}