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

type PlaceSearchResult = {
  name: string
  category?: string
  address?: string
  roadAddress?: string
  phone?: string
  placeUrl?: string
  mapy?: number
  mapx?: number
  source: string
}

type PlaceSearchApiResponse = {
  success: boolean
  message: string
  data: PlaceSearchResult[]
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

const NAVER_LOCAL_COORDINATE_SCALE = 10_000_000


export function NaverLocationPicker({ initialKeyword = "", onSelect }: NaverLocationPickerProps) {
  const mapContainerRef = useRef<HTMLDivElement | null>(null)
  const mapRef = useRef<NaverMap | null>(null)
  const markerRef = useRef<NaverMarker | null>(null)
  const infoWindowRef = useRef<NaverInfoWindow | null>(null)

  const [keyword, setKeyword] = useState(initialKeyword)
  const [selectedLocationName, setSelectedLocationName] = useState<string | null>(initialKeyword || null)
  const [isMapReady, setIsMapReady] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [results, setResults] = useState<PlaceSearchResult[]>([])
  const [isSearching, setIsSearching] = useState(false)

  const naverMapClientId = process.env.NEXT_PUBLIC_NAVER_MAP_CLIENT_ID
  const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080"

  useEffect(() => {
    setKeyword(initialKeyword)

    if (initialKeyword) {
      setSelectedLocationName(initialKeyword)
    }
  }, [initialKeyword])

  useEffect(() => {
    let isMounted = true

    if (!naverMapClientId) {
      setErrorMessage("네이버 지도 Client ID가 설정되어 있지 않습니다.")
      return () => {
        isMounted = false
      }
    }

    const handleLoad = () => {
      if (isMounted) {
        initializeMap()
      }
    }

    const handleError = () => {
      if (isMounted) {
        setErrorMessage("네이버 지도 SDK를 불러오지 못했습니다.")
      }
    }

    if (window.naver?.maps) {
      handleLoad()
      return () => {
        isMounted = false
      }
    }

    const existingScript = document.querySelector<HTMLScriptElement>("script[data-naver-map-script]")

    if (existingScript) {
      existingScript.addEventListener("load", handleLoad)
      existingScript.addEventListener("error", handleError)

      return () => {
        isMounted = false
        existingScript.removeEventListener("load", handleLoad)
        existingScript.removeEventListener("error", handleError)
      }
    }

    const script = document.createElement("script")
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${naverMapClientId}`
    script.async = true
    script.dataset.naverMapScript = "true"
    script.addEventListener("load", handleLoad)
    script.addEventListener("error", handleError)
    document.head.appendChild(script)

    return () => {
      isMounted = false
      script.removeEventListener("load", handleLoad)
      script.removeEventListener("error", handleError)
    }
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

    const trimmedKeyword = initialKeyword.trim()

    if (trimmedKeyword) {
      void searchLocations(trimmedKeyword, map)
    }
  }

  async function searchLocations(searchKeyword = keyword, targetMap?: NaverMap) {
    if (isSearching) return

    const trimmedKeyword = searchKeyword.trim()

    if (!trimmedKeyword) {
      setResults([])
      setSelectedLocationName(null)
      setErrorMessage("검색어를 입력해 주세요.")
      return
    }

    setIsSearching(true)
    setErrorMessage(null)

    try {
      const normalizedApiBaseUrl = apiBaseUrl.replace(/\/$/, "")
      const response = await fetch(
        `${normalizedApiBaseUrl}/api/places/search?query=${encodeURIComponent(trimmedKeyword)}`,
      )

      if (!response.ok) {
        throw new Error("장소 검색 요청에 실패했습니다.")
      }

      const body = (await response.json()) as PlaceSearchApiResponse
      const searchedResults = body.data ?? []
      const mappableResults = searchedResults.filter(
        (location) => typeof location.mapx === "number" && typeof location.mapy === "number",
      )

      if (mappableResults.length === 0) {
        setResults([])
        setSelectedLocationName(null)
        setErrorMessage("검색 결과가 없거나 지도에 표시할 수 있는 좌표가 없습니다.")
        return
      }

      setResults(mappableResults)
      setSelectedLocationName(null)
    } catch (error) {
      console.error(error)
      setResults([])
      setSelectedLocationName(null)
      setErrorMessage("장소 검색 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.")
    } finally {
      setIsSearching(false)
    }
  }

  function selectLocation(location: PlaceSearchResult, targetMap?: NaverMap) {
    const map = targetMap ?? mapRef.current

    if (!window.naver?.maps || !map) return
    if (typeof location.mapx !== "number" || typeof location.mapy !== "number") {
      setErrorMessage("선택한 장소의 좌표 정보가 없습니다.")
      return
    }

    markerRef.current?.setMap(null)
    infoWindowRef.current?.close()

    const latitude = location.mapy / NAVER_LOCAL_COORDINATE_SCALE
    const longitude = location.mapx / NAVER_LOCAL_COORDINATE_SCALE
    const position = new window.naver.maps.LatLng(latitude, longitude)

    const marker = new window.naver.maps.Marker({
      map,
      position,
    })
    const infoWindow = new window.naver.maps.InfoWindow({
      content: `<div style="padding:6px 10px;font-size:13px;white-space:nowrap;">${location.name}</div>`,
    })

    markerRef.current = marker
    infoWindowRef.current = infoWindow

    map.setCenter(position)
    infoWindow.open(map, marker)
    setSelectedLocationName(location.name)
    setKeyword(location.name)

    window.naver.maps.Event.addListener(marker, "click", () => {
      infoWindow.open(map, marker)
    })

    onSelect({
      name: location.name,
      address: location.roadAddress || location.address,
      latitude: position.lat(),
      longitude: position.lng(),
      source: "naver",
    })
  }

  return (
    <div className="rounded-2xl border bg-background p-4">
      <p className="font-semibold">네이버 지도로 위치 선택</p>
      <p className="mt-1 text-sm text-muted-foreground">
        장소명을 검색한 뒤 결과를 선택하면 지도 중심이 이동하고 마커가 표시됩니다.
      </p>

      <div className="mt-3 flex flex-col gap-2 sm:flex-row">
        <input
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter") {
              event.preventDefault()
              void searchLocations()
            }
          }}
          placeholder="예: 성수 카페, 서울숲 맛집, 잠실 롯데월드몰"
          className="h-11 flex-1 rounded-xl border border-input bg-background px-3 text-sm outline-none ring-offset-background placeholder:text-muted-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
        />
        <button
          type="button"
          onClick={() => void searchLocations()}
          disabled={!isMapReady || isSearching}
          className="h-11 rounded-xl bg-primary px-4 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {isSearching ? "검색 중..." : "검색"}
        </button>
      </div>

      {errorMessage && <p className="mt-2 text-sm text-destructive">{errorMessage}</p>}

      <div ref={mapContainerRef} className="mt-4 h-72 overflow-hidden rounded-2xl border bg-secondary/30" />

      {results.length > 0 && (
        <div className="mt-4 space-y-2">
          <p className="text-sm font-semibold text-muted-foreground">선택 가능한 장소</p>
          <div className="max-h-48 space-y-2 overflow-y-auto pr-1">
            {results.slice(0, 5).map((location) => {
              const isSelected = selectedLocationName === location.name

              return (
                <button
                  key={`${location.name}-${location.mapy}-${location.mapx}-${location.address ?? location.roadAddress ?? ""}`}
                  type="button"
                  onClick={() => selectLocation(location)}
                  className={`w-full rounded-xl border p-3 text-left transition-colors ${
                    isSelected
                      ? "border-primary bg-primary/5 ring-1 ring-primary"
                      : "bg-background hover:border-primary/60 hover:bg-primary/5"
                  }`}
                >
                  <p className="font-semibold">{location.name}</p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {location.roadAddress || location.address || "주소 정보 없음"}
                  </p>
                  {location.category && (
                    <p className="mt-1 text-xs text-muted-foreground">{location.category}</p>
                  )}
                </button>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}
