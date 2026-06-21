"use client"

import { useEffect, useRef } from "react"

type Point = {
  title: string
  latitude: number
  longitude: number
  order: number
}

type Props = {
  points: Point[]
}

declare global {
  interface Window {
    naver: any
  }
}

export function NaverCourseMap({ points }: Props) {
  console.log("NaverCourseMap render", points)
  const mapRef = useRef<any>(null)
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {

    console.log("effect start")
    console.log("window.naver", window.naver)
    console.log("points", points)
    if (!window.naver || !containerRef.current || points.length === 0) {
      console.log("return")
      return
    }
    const naver = window.naver

    const center = new naver.maps.LatLng(
      points[0].latitude,
      points[0].longitude
    )

    const map = new naver.maps.Map(containerRef.current, {
      center,
      zoom: 13,
    })

    mapRef.current = map

    const bounds = new naver.maps.LatLngBounds()

    const path: any[] = []

    points.forEach((point) => {
      const position = new naver.maps.LatLng(
        point.latitude,
        point.longitude
      )

      bounds.extend(position)

      path.push(position)

      const marker = new naver.maps.Marker({
        position,
        map,
        icon: {
          content: `
            <div
              style="
                width:32px;
                height:32px;
                border-radius:50%;
                background:#2563eb;
                color:white;
                display:flex;
                align-items:center;
                justify-content:center;
                font-weight:bold;
                border:2px solid white;
              "
            >
              ${point.order}
            </div>
          `,
          anchor: new naver.maps.Point(16, 16),
        },
      })

      const infoWindow = new naver.maps.InfoWindow({
        content: `
          <div style="padding:10px;min-width:220px">
            <b>${point.title}</b>
            <br/>
            위도: ${point.latitude}
            <br/>
            경도: ${point.longitude}
          </div>
        `,
      })

      naver.maps.Event.addListener(marker, "click", () => {
        infoWindow.open(map, marker)
      })
    })

    new naver.maps.Polyline({
      map,
      path,
      strokeWeight: 4,
      strokeColor: "#2563eb",
      strokeOpacity: 0.9,
      strokeLineCap: "round",
    })

    map.fitBounds(bounds)
  }, [points])

  return (
    <div
      ref={containerRef}
      className="h-[500px] w-full rounded-xl"
    />
  )
}