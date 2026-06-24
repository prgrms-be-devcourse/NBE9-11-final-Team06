"use client"

import { useEffect, useRef } from "react"

type Point = {
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

export function SimpleNaverMap({ points }: { points: Point[] }) {
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


      let color = "#2563eb" // event

      if (p.type === "restaurant") {
        color = "#dc2626"
      }

      if (p.type === "cafe") {
        color = "#16a34a"
      }

      const marker = new naver.maps.Marker({
        position,
        map,
        title: p.title,
        icon: {
          content: `
            <div
              style="
                width:24px;
                height:24px;
                border-radius:50%;
                background:${color};
                border:2px solid white;
              "
            ></div>
          `,
          anchor: new naver.maps.Point(12, 12),
        },
      })

      naver.maps.Event.addListener(marker, "click", () => {
        infoWindow.setContent(
          `<div style="padding:6px;font-size:12px;">
            ${idx + 1}. ${p.title}
          </div>`
        )
        infoWindow.open(map, marker)
      })
    })

    map.fitBounds(bounds)
  }, [points])

  return <div ref={mapRef} className="w-full h-[400px] rounded-lg" />
}