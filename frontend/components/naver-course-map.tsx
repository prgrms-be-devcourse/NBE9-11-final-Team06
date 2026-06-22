"use client"

import { useEffect, useRef, useState } from "react"

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
    naver?: any
  }
}

const NAVER_MAP_SCRIPT_ID = "naver-map-script"
const NAVER_MAP_CLIENT_ID = process.env.NEXT_PUBLIC_NAVER_MAP_CLIENT_ID

function isValidPoint(point: Point) {
  return (
    Number.isFinite(point.latitude) &&
    Number.isFinite(point.longitude) &&
    point.latitude !== 0 &&
    point.longitude !== 0
  )
}

function isNaverMapReady() {
  return Boolean(
    typeof window !== "undefined" &&
      window.naver &&
      window.naver.maps &&
      window.naver.maps.LatLng &&
      window.naver.maps.Map,
  )
}

function waitForNaverMapReady(): Promise<void> {
  return new Promise((resolve, reject) => {
    if (isNaverMapReady()) {
      resolve()
      return
    }

    let count = 0
    const maxCount = 50

    const timer = window.setInterval(() => {
      count += 1

      if (isNaverMapReady()) {
        window.clearInterval(timer)
        resolve()
        return
      }

      if (count >= maxCount) {
        window.clearInterval(timer)
        reject(new Error("네이버 지도 스크립트 로딩에 실패했습니다."))
      }
    }, 100)
  })
}

function loadNaverMapScript(): Promise<void> {
  return new Promise((resolve, reject) => {
    if (typeof window === "undefined") {
      reject(new Error("브라우저 환경이 아닙니다."))
      return
    }

    if (isNaverMapReady()) {
      resolve()
      return
    }

    const existingScript = document.getElementById(NAVER_MAP_SCRIPT_ID)

    if (existingScript) {
      waitForNaverMapReady().then(resolve).catch(reject)
      return
    }

    if (!NAVER_MAP_CLIENT_ID) {
      reject(
        new Error(
          "NEXT_PUBLIC_NAVER_MAP_CLIENT_ID 환경변수가 설정되어 있지 않습니다.",
        ),
      )
      return
    }

    const script = document.createElement("script")
    script.id = NAVER_MAP_SCRIPT_ID
    script.type = "text/javascript"
    script.async = true
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${NAVER_MAP_CLIENT_ID}`

    script.onload = () => {
      waitForNaverMapReady().then(resolve).catch(reject)
    }

    script.onerror = () => {
      reject(new Error("네이버 지도 스크립트를 불러오지 못했습니다."))
    }

    document.head.appendChild(script)
  })
}

function escapeHtml(value: string) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;")
}

export function NaverCourseMap({ points }: Props) {
  const mapRef = useRef<any>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  useEffect(() => {
    let ignore = false

    async function renderMap() {
      setErrorMessage(null)

      const validPoints = points
        .filter(isValidPoint)
        .sort((a, b) => a.order - b.order)

      if (validPoints.length === 0) {
        setErrorMessage("지도에 표시할 위치 정보가 없습니다.")
        return
      }

      if (!containerRef.current) {
        return
      }

      try {
        await loadNaverMapScript()

        if (ignore || !containerRef.current || !isNaverMapReady()) {
          return
        }

        const naver = window.naver

        const center = new naver.maps.LatLng(
          validPoints[0].latitude,
          validPoints[0].longitude,
        )

        const map = new naver.maps.Map(containerRef.current, {
          center,
          zoom: 13,
        })

        mapRef.current = map

        const bounds = new naver.maps.LatLngBounds()
        const path: any[] = []

        validPoints.forEach((point) => {
          const position = new naver.maps.LatLng(
            point.latitude,
            point.longitude,
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
                <b>${escapeHtml(point.title)}</b>
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

        if (path.length > 1) {
          new naver.maps.Polyline({
            map,
            path,
            strokeWeight: 4,
            strokeColor: "#2563eb",
            strokeOpacity: 0.9,
            strokeLineCap: "round",
          })

          map.fitBounds(bounds)
        }
      } catch (error) {
        console.error(error)

        if (!ignore) {
          setErrorMessage("지도를 불러오지 못했습니다.")
        }
      }
    }

    renderMap()

    return () => {
      ignore = true
    }
  }, [points])

  if (errorMessage) {
    return (
      <div className="flex h-[500px] w-full items-center justify-center rounded-xl border bg-muted/40 p-4 text-center text-sm text-muted-foreground">
        {errorMessage}
      </div>
    )
  }

  return <div ref={containerRef} className="h-[500px] w-full rounded-xl" />
}