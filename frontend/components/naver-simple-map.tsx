"use client"

import { useEffect, useRef } from "react"

type LatLng = {
  lat: number
  lng: number
}

type Props = {
  points: LatLng[]
}

export function NaverSimpleMap({ points }: Props) {
  const containerRef = useRef<HTMLDivElement | null>(null)
  const mapRef = useRef<any>(null)
  const markersRef = useRef<any[]>([])

  useEffect(() => {
    if (!window.naver?.maps || !containerRef.current) return
    if (points.length === 0) return

    const center = new window.naver.maps.LatLng(
      points[0].lat,
      points[0].lng
    )

    const map = new window.naver.maps.Map(containerRef.current, {
      center,
      zoom: 14,
    })

    mapRef.current = map

    // 마커 생성
    markersRef.current = points.map((p) => {
      return new window.naver.maps.Marker({
        map,
        position: new window.naver.maps.LatLng(p.lat, p.lng),
      })
    })
  }, [points])

  return (
    <div
      ref={containerRef}
      className="h-72 w-full rounded-2xl border"
    />
  )
}