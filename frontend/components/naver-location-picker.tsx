"use client"

import { useEffect, useRef, useState } from "react"

export type NaverSelectedLocation = {
  name: string
  address?: string
  latitude?: number
  longitude?: number
  source: "naver"
}

type NaverLocationPickerProps = {
  initialKeyword?: string
  onSelect: (location: NaverSelectedLocation) => void
}

declare global {
  interface Window {
    naver?: {
      maps: {
        LatLng: new (latitude: number, longitude: number) => NaverLatLng
        Map: new (container: HTMLElement, options: NaverMapOptions) => NaverMap
        Marker: new (options: NaverMarkerOptions) => NaverMarker
        InfoWindow: new (options: NaverInfoWindowOptions) => NaverInfoWindow
        Event: {
          addListener: (target: NaverMarker, eventName: string, listener: () => void) => void
        }
      }
    }
  }
}

type NaverLatLng = {
  lat: () => number
  lng: () => number
}

type NaverMapOptions = {
  center: NaverLatLng
  zoom: number
}

type NaverMap = {
  setCenter: (latLng: NaverLatLng) => void
}

type NaverMarkerOptions = {
  map: NaverMap | null
  position: NaverLatLng
}

type NaverMarker = {
  setMap: (map: NaverMap | null) => void
  getPosition: () => NaverLatLng
}

type NaverInfoWindowOptions = {
  content: string
}

type NaverInfoWindow = {
  open: (map: NaverMap, marker: NaverMarker) => void
  close: () => void
}

const DEFAULT_CENTER = {
  latitude: 37.5665,
  longitude: 126.978,
}

const SAMPLE_LOCATIONS = [
  {
    name: "성수역",
    address: "서울 성동구 아차산로 100",
    latitude: 37.5446,
    longitude: 127.0557,
  },
  {
    name: "서울숲",
    address: "서울 성동구 뚝섬로 273",
    latitude: 37.5444,
    longitude: 127.0374,
  },
  {
    name: "잠실 롯데월드몰",
    address: "서울 송파구 올림픽로 300",
    latitude: 37.5131,
    longitude: 127.1035,
  },
  {
    name: "홍대입구역",
    address: "서울 마포구 양화로 160",
    latitude: 37.5572,
    longitude: 126.9254,
  },
  {
    name: "건대입구역",
    address: "서울 광진구 아차산로 243",
    latitude: 37.5404,
    longitude: 127.0692,
  },
]

export function NaverLocationPicker({ initialKeyword = "", onSelect }: NaverLocationPickerProps) {
  const mapContainerRef = useRef<HTMLDivElement | null>(null)
  const mapRef = useRef<NaverMap | null>(null)
  const markerRef = useRef<NaverMarker | null>(null)
  const infoWindowRef = useRef<NaverInfoWindow | null>(null)

  const [keyword, setKeyword] = useState(initialKeyword)
  const [isMapReady, setIsMapReady] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [results, setResults] = useState(SAMPLE_LOCATIONS)

  const naverMapClientId = process.env.NEXT_PUBLIC_NAVER_MAP_CLIENT_ID

  useEffect(() => {
    if (!naverMapClientId) {
      setErrorMessage("네이버 지도 Client ID가 설정되어 있지 않습니다.")
      return
    }

    if (window.naver?.maps) {
      initializeMap()
      return
    }

    const existingScript = document.querySelector<HTMLScriptElement>("script[data-naver-map-script]")

    if (existingScript) {
      existingScript.addEventListener("load", initializeMap, {
        once: true,
      })
      return
    }

    const script = document.createElement("script")
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${naverMapClientId}`
    script.async = true
    script.dataset.naverMapScript = "true"
    script.onload = initializeMap
    script.onerror = () => setErrorMessage("네이버 지도 SDK를 불러오지 못했습니다.")
    document.head.appendChild(script)
  }, [naverMapClientId])

  function initializeMap() {
    if (!mapContainerRef.current || !window.naver?.maps) return

    const center = new window.naver.maps.LatLng(DEFAULT_CENTER.latitude, DEFAULT_CENTER.longitude)
    const map = new window.naver.maps.Map(mapContainerRef.current, {
      center,
      zoom: 12,
    })

    mapRef.current = map
    setIsMapReady(true)
    setErrorMessage(null)
  }

  function searchLocations() {
    const trimmedKeyword = keyword.trim()

    if (!trimmedKeyword) {
      setResults(SAMPLE_LOCATIONS)
      return
    }

    const filteredResults = SAMPLE_LOCATIONS.filter((location) =>
      `${location.name} ${location.address}`.includes(trimmedKeyword),
    )

    if (filteredResults.length === 0) {
      setResults([])
      setErrorMessage("임시 검색 결과가 없습니다. 기본 지역을 선택하거나 다른 키워드로 검색해 주세요.")
      return
    }

    setResults(filteredResults)
    setErrorMessage(null)
    selectLocation(filteredResults[0])
  }

  function selectLocation(location: (typeof SAMPLE_LOCATIONS)[number]) {
    if (!window.naver?.maps || !mapRef.current) return

    markerRef.current?.setMap(null)
    infoWindowRef.current?.close()

    const position = new window.naver.maps.LatLng(location.latitude, location.longitude)
    const marker = new window.naver.maps.Marker({
      map: mapRef.current,
      position,
    })
    const infoWindow = new window.naver.maps.InfoWindow({
      content: `<div style="padding:6px 10px;font-size:13px;white-space:nowrap;">${location.name}</div>`,
    })

    markerRef.current = marker
    infoWindowRef.current = infoWindow

    mapRef.current.setCenter(position)
    infoWindow.open(mapRef.current, marker)

    window.naver.maps.Event.addListener(marker, "click", () => {
      infoWindow.open(mapRef.current as NaverMap, marker)
    })

    onSelect({
      name: location.name,
      address: location.address,
      latitude: location.latitude,
      longitude: location.longitude,
      source: "naver",
    })
  }

  return (
    <div className="rounded-2xl border bg-background p-4">
      <p className="font-semibold">네이버 지도로 위치 선택</p>
      <p className="mt-1 text-sm text-muted-foreground">
        현재는 지도 표시와 샘플 장소 검색을 지원합니다. 장소 검색 API 연동은 이후 작업에서 확장합니다.
      </p>

      <div className="mt-3 flex flex-col gap-2 sm:flex-row">
        <input
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter") {
              event.preventDefault()
              searchLocations()
            }
          }}
          placeholder="예: 성수역, 서울숲, 잠실 롯데월드몰"
          className="h-11 flex-1 rounded-xl border border-input bg-background px-3 text-sm outline-none ring-offset-background placeholder:text-muted-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
        />
        <button
          type="button"
          onClick={searchLocations}
          disabled={!isMapReady}
          className="h-11 rounded-xl bg-primary px-4 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
        >
          검색
        </button>
      </div>

      {errorMessage && <p className="mt-2 text-sm text-destructive">{errorMessage}</p>}

      <div ref={mapContainerRef} className="mt-4 h-72 overflow-hidden rounded-2xl border bg-secondary/30" />

      {results.length > 0 && (
        <div className="mt-4 space-y-2">
          <p className="text-sm font-semibold text-muted-foreground">선택 가능한 장소</p>
          <div className="max-h-48 space-y-2 overflow-y-auto pr-1">
            {results.slice(0, 5).map((location) => (
              <button
                key={`${location.name}-${location.latitude}-${location.longitude}`}
                type="button"
                onClick={() => selectLocation(location)}
                className="w-full rounded-xl border bg-background p-3 text-left transition-colors hover:border-primary/60 hover:bg-primary/5"
              >
                <p className="font-semibold">{location.name}</p>
                <p className="mt-1 text-sm text-muted-foreground">{location.address}</p>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
