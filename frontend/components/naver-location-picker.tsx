
import { useEffect, useRef, useState } from "react"

export type NaverSelectedLocation = {
  name: string
  address?: string
  latitude?: number
  longitude?: number
  source: "naver" | "district"
}

export type NaverDistrictLocation = {
  name: string
  address: string
  latitude: number
  longitude: number
}

type NaverLocationPickerProps = {
  initialKeyword?: string
  selectedDistrictLocation?: NaverDistrictLocation | null
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

type NaverMapClickEvent = {
  coord: NaverLatLng
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


let naverMapsSdkPromise: Promise<void> | null = null

function loadNaverMapsSdk(clientId: string): Promise<void> {
  if (window.naver?.maps) {
    return Promise.resolve()
  }

  if (naverMapsSdkPromise) {
    return naverMapsSdkPromise
  }

  const existingScript = document.querySelector<HTMLScriptElement>(
    'script[data-naver-map-script="true"]',
  )

  if (existingScript) {
    existingScript.remove()
  }

  naverMapsSdkPromise = new Promise<void>((resolve, reject) => {
    const script = document.createElement("script")
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${clientId}&submodules=geocoder`
    script.async = true
    script.dataset.naverMapScript = "true"

    script.addEventListener("load", () => {
      if (window.naver?.maps) {
        resolve()
        return
      }

      reject(new Error("네이버 지도 SDK를 불러오지 못했습니다."))
    })

    script.addEventListener("error", () => {
      reject(new Error("네이버 지도 SDK를 불러오지 못했습니다."))
    })

    document.head.appendChild(script)
  }).catch((error) => {
    naverMapsSdkPromise = null
    throw error
  })

  return naverMapsSdkPromise
}

export function NaverLocationPicker({
  initialKeyword = "",
  selectedDistrictLocation = null,
  onSelect,
}: NaverLocationPickerProps) {
  const mapContainerRef = useRef<HTMLDivElement | null>(null)
  const mapRef = useRef<NaverMap | null>(null)
  const markerRef = useRef<NaverMarker | null>(null)
  const infoWindowRef = useRef<NaverInfoWindow | null>(null)
  const selectionVersionRef = useRef(0)

  const [keyword, setKeyword] = useState(initialKeyword)
  const [selectedLocationName, setSelectedLocationName] = useState<string | null>(initialKeyword || null)
  const [selectedLocationAddress, setSelectedLocationAddress] = useState<string | null>(null)
  const [isMapReady, setIsMapReady] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [results, setResults] = useState<PlaceSearchResult[]>([])
  const [isSearching, setIsSearching] = useState(false)

  const naverMapClientId = process.env.NEXT_PUBLIC_NAVER_MAP_CLIENT_ID

  useEffect(() => {
    setKeyword(initialKeyword)

    if (initialKeyword) {
      setSelectedLocationName(initialKeyword)
    }
  }, [initialKeyword])

  useEffect(() => {
    if (!selectedDistrictLocation || !isMapReady || !mapRef.current || !window.naver?.maps) {
      return
    }

    selectDistrictLocation(selectedDistrictLocation)
  }, [
    isMapReady,
    selectedDistrictLocation?.address,
    selectedDistrictLocation?.latitude,
    selectedDistrictLocation?.longitude,
    selectedDistrictLocation?.name,
  ])
  function selectDistrictLocation(location: NaverDistrictLocation) {
    const map = mapRef.current

    if (!window.naver?.maps || !map) {
      return
    }

    selectionVersionRef.current += 1
    markerRef.current?.setMap(null)
    infoWindowRef.current?.close()

    const position = new window.naver.maps.LatLng(location.latitude, location.longitude)
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
    setKeyword(location.name)
    setSelectedLocationName(location.name)
    setSelectedLocationAddress(location.address)
    setResults([])
    setErrorMessage(null)

    window.naver.maps.Event.addListener(marker, "click", () => {
      infoWindow.open(map, marker)
    })

    onSelect({
      name: location.name,
      address: location.address,
      latitude: location.latitude,
      longitude: location.longitude,
      source: "district",
    })
  }

  useEffect(() => {
    let isMounted = true

    if (!naverMapClientId) {
      setErrorMessage("네이버 지도 Client ID가 설정되어 있지 않습니다.")
      return () => {
        isMounted = false
      }
    }

    void loadNaverMapsSdk(naverMapClientId)
      .then(() => {
        if (isMounted) {
          initializeMap()
        }
      })
      .catch((error: unknown) => {
        console.error(error)

        if (isMounted) {
          setErrorMessage("네이버 지도 SDK를 불러오지 못했습니다.")
        }
      })

    return () => {
      isMounted = false
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

    window.naver.maps.Event.addListener(map, "click", function (event: NaverMapClickEvent) {
      if (!event?.coord) {
        console.warn("네이버 지도 클릭 좌표를 가져오지 못했습니다.", event)
        return
      }

      selectMapPosition(event.coord, map)
    })

    const trimmedKeyword = initialKeyword.trim()

    if (trimmedKeyword) {
      void searchLocations(trimmedKeyword, map)
    }
  }

  async function resolveNeighborhoodName(
    latitude: number,
    longitude: number,
    fallbackName: string,
    onResolved: (name: string) => void,
  ) {
    try {
      const response = await fetch(
        `/api/places/reverse-geocode?latitude=${encodeURIComponent(latitude)}&longitude=${encodeURIComponent(longitude)}`,
      )

      if (!response.ok) {
        throw new Error(`역지오코딩 요청 실패: ${response.status}`)
      }

      const payload = await response.json()
      const resolvedName =
        payload?.data?.areaName ??
        payload?.result?.areaName ??
        payload?.areaName

      if (typeof resolvedName !== "string" || !resolvedName.trim()) {
        throw new Error("역지오코딩 응답에 지역명이 없습니다.")
      }

      onResolved(resolvedName.trim())
    } catch (error) {
      console.warn("좌표 기반 지역명 조회에 실패했습니다.", error)
      onResolved(fallbackName)
    }
  }

  function selectMapPosition(position: NaverLatLng, targetMap?: NaverMap) {
    const map = targetMap ?? mapRef.current
    const selectionVersion = ++selectionVersionRef.current

    if (!window.naver?.maps || !map) return

    markerRef.current?.setMap(null)
    infoWindowRef.current?.close()

    const latitude = position.lat()
    const longitude = position.lng()
    const locationName = "선택한 위치"
    const marker = new window.naver.maps.Marker({
      map,
      position,
    })
    const infoWindow = new window.naver.maps.InfoWindow({
      content: `<div style="padding:6px 10px;font-size:13px;white-space:nowrap;">${locationName}</div>`,
    })

    markerRef.current = marker
    infoWindowRef.current = infoWindow

    map.setCenter(position)
    infoWindow.open(map, marker)
    setSelectedLocationName(locationName)
    setSelectedLocationAddress(null)
    setKeyword("")
    setResults([])
    setErrorMessage(null)

    window.naver.maps.Event.addListener(marker, "click", () => {
      infoWindow.open(map, marker)
    })

    void resolveNeighborhoodName(latitude, longitude, locationName, (resolvedName) => {
      if (selectionVersion !== selectionVersionRef.current) {
        return
      }

      if (resolvedName === locationName) {
        setErrorMessage("선택한 위치의 동 이름을 확인하지 못했습니다. 다른 위치를 선택해 주세요.")
        return
      }

      setSelectedLocationName(resolvedName)
      setSelectedLocationAddress(resolvedName)
      onSelect({
        name: resolvedName,
        address: resolvedName,
        latitude,
        longitude,
        source: "naver",
      })
    })
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
      const response = await fetch(
          `/api/places/search?query=${encodeURIComponent(trimmedKeyword)}`,
          {
            credentials: "include",
          },
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
    if (typeof location.mapx !== "number" || typeof location.mapy !== "number") {
      setErrorMessage("선택한 장소의 좌표 정보가 없습니다.")
      return
    }

    const latitude = location.mapy / NAVER_LOCAL_COORDINATE_SCALE
    const longitude = location.mapx / NAVER_LOCAL_COORDINATE_SCALE
    const map = targetMap ?? mapRef.current

    selectionVersionRef.current += 1
    const address = location.roadAddress || location.address || null
    setSelectedLocationName(location.name)
    setSelectedLocationAddress(address)
    setKeyword(location.name)
    setErrorMessage(null)

    onSelect({
      name: location.name,
      address: address ?? undefined,
      latitude,
      longitude,
      source: "naver",
    })

    if (!window.naver?.maps || !map) {
      return
    }

    markerRef.current?.setMap(null)
    infoWindowRef.current?.close()

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

    window.naver.maps.Event.addListener(marker, "click", () => {
      infoWindow.open(map, marker)
    })
  }

  return (
    <div className="rounded-2xl border bg-background p-4">
      <p className="font-semibold">네이버 지도로 위치 선택</p>
      <p className="mt-1 text-sm text-muted-foreground">
        장소명을 검색해 결과를 선택하거나, 지도에서 원하는 위치를 직접 클릭해 선택할 수 있습니다.
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
          disabled={isSearching}
          className="h-11 rounded-xl bg-primary px-4 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {isSearching ? "검색 중..." : "검색"}
        </button>
      </div>

      {errorMessage && <p className="mt-2 text-sm text-destructive">{errorMessage}</p>}

      <p className="mt-3 text-xs text-muted-foreground">
        지도 위를 한 번 클릭하면 해당 위치에 마커가 표시되고 위치가 선택됩니다.
      </p>
      <div ref={mapContainerRef} className="mt-4 h-72 overflow-hidden rounded-2xl border bg-secondary/30" />
      {selectedLocationName && (
        <div className="mt-4 rounded-xl border bg-background p-3">
          <p className="text-sm font-semibold">선택된 위치</p>
          <p className="mt-1 font-medium">{selectedLocationName}</p>
          {selectedLocationAddress ? (
            <p className="mt-1 text-sm text-muted-foreground">{selectedLocationAddress}</p>
          ) : (
            <p className="mt-1 text-sm text-muted-foreground">주소 정보를 확인하는 중입니다.</p>
          )}
        </div>
      )}

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