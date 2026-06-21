"use client"

import { useEffect, useRef } from "react"

type LatLng = {
  lat: number
  lng: number
  name?: string
  address?: string
}

type Props = {
  points: LatLng[]
}

export function NaverSimpleMap({ points }: Props) {
  const containerRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    if (!window.naver?.maps || !containerRef.current) return
    if (points.length === 0) return

    const map = new window.naver.maps.Map(containerRef.current, {
      center: new window.naver.maps.LatLng(
        points[0].lat,
        points[0].lng
      ),
      zoom: 14,
    })

    const bounds = new window.naver.maps.LatLngBounds()

    const path: any[] = []

    points.forEach((point, index) => {
      const position = new window.naver.maps.LatLng(
        point.lat,
        point.lng
      )

      bounds.extend(position)
      path.push(position)

      const marker = new window.naver.maps.Marker({
        map,
        position,
        title: point.name || `장소 ${index + 1}`,
      })

      const infoWindow = new window.naver.maps.InfoWindow({
        content: `
          <div style="padding:10px;min-width:180px">
            <div style="font-weight:bold">
              ${index + 1}. ${point.name ?? "장소"}
            </div>
            <div style="font-size:12px;color:#666;margin-top:4px">
              ${point.address ?? ""}
            </div>
          </div>
        `,
      })

      window.naver.maps.Event.addListener(marker, "click", () => {
        if (infoWindow.getMap()) {
          infoWindow.close()
        } else {
          infoWindow.open(map, marker)
        }
      })
    })

    // 코스 선 연결
    new window.naver.maps.Polyline({
      map,
      path,
      strokeColor: "#2563eb",
      strokeOpacity: 0.8,
      strokeWeight: 4,
      strokeStyle: "solid",
    })

    // 전체 마커 보이게
    if (points.length > 1) {
      map.fitBounds(bounds)
    }
  }, [points])

  return (
    <div
      ref={containerRef}
      className="h-72 w-full rounded-2xl border"
    />
  )
}