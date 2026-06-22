export type Category =
  | "전시"
  | "공연"
  | "축제"
  | "체험"
  | "식당"
  | "카페"
  | "산책"
  | "문화시설"

export type Companion = "SOLO" | "COUPLE" | "FRIEND" | "FAMILY" | "PARENT"

export type RestaurantType =
  | "KOREAN"
  | "WESTERN"
  | "JAPANESE"
  | "CHINESE"

export type CrowdLevel = "여유" | "보통" | "혼잡" | "매우혼잡"

export type Place = {
  id: string
  name: string
  category: Category
  area: string
  address: string
  description: string
  lat: number
  lng: number
  image: string
  rating: number
  priceLabel: string
  duration: number // minutes
  crowd: CrowdLevel
  indoor: boolean
  url: string
}

export type Event = {
  id: string
  name: string
  category: Category
  place: string
  area: string
  address: string
  period: string
  fee: string
  image: string
  url: string
}

export type CourseStop = {
  place: Place
  order: number
  arrive: string
  reasons: string[]
  travelToNext?: { mode: string; minutes: number; distance: string }
}

export type Course = {
  id: string
  title: string
  area: string
  description: string
  companion: Companion
  categories: Category[]
  totalDuration: string
  totalDistance: string
  summaryReasons: string[]
  stops: CourseStop[]
  cover: string
}

export const CATEGORIES: { value: Category; emoji: string; label: string }[] = [
  { value: "전시", emoji: "🖼", label: "전시" },
  { value: "공연", emoji: "🎭", label: "공연" },
  { value: "축제", emoji: "🎪", label: "축제" },
  { value: "체험", emoji: "🧗", label: "체험" },
  { value: "산책", emoji: "🌳", label: "산책" },
  { value: "문화시설", emoji: "🏛", label: "문화시설" },
]

export const COMPANIONS = [
  { value: "SOLO", label: "혼자", desc: "조용하고 여유로운 코스" },
  { value: "FRIEND", label: "친구", desc: "활동형 · 핫플 중심" },
  { value: "FAMILY", label: "가족", desc: "체험형 · 편안한 동선" },
  { value: "COUPLE", label: "커플", desc: "전시 · 카페 · 산책" },
]


export const SEOUL_AREAS = [
  { name: "성수", lat: 37.5446, lng: 127.0559, crowd: "혼잡" as CrowdLevel },
  { name: "연남동", lat: 37.5604, lng: 126.925, crowd: "보통" as CrowdLevel },
  { name: "익선동", lat: 37.5746, lng: 126.9913, crowd: "혼잡" as CrowdLevel },
  { name: "삼청동", lat: 37.5836, lng: 126.9819, crowd: "여유" as CrowdLevel },
  { name: "여의도", lat: 37.5219, lng: 126.9245, crowd: "보통" as CrowdLevel },
  { name: "잠실", lat: 37.5133, lng: 127.1, crowd: "매우혼잡" as CrowdLevel },
  { name: "홍대", lat: 37.5563, lng: 126.9236, crowd: "매우혼잡" as CrowdLevel },
  { name: "이태원", lat: 37.5345, lng: 126.9946, crowd: "보통" as CrowdLevel },
]

export const CROWD_META: Record<
  CrowdLevel,
  { color: string; bg: string; range: string }
> = {
  여유: { color: "text-emerald-700", bg: "bg-emerald-100", range: "~3천명" },
  보통: { color: "text-sky-700", bg: "bg-sky-100", range: "3천~1만명" },
  혼잡: { color: "text-amber-700", bg: "bg-amber-100", range: "1만~3만명" },
  매우혼잡: { color: "text-rose-700", bg: "bg-rose-100", range: "3만명 이상" },
}

export const PLACES: Place[] = [
  {
    id: "p1",
    name: "성수연방",
    category: "카페",
    area: "성수",
    address: "서울 성동구 성수이로 14길 14",
    description: "옛 인쇄소를 개조한 복합문화공간. 루프탑 카페와 편집숍이 모여있어요.",
    lat: 37.5419,
    lng: 127.0561,
    image: "/cafe-rooftop-seongsu.png",
    rating: 4.6,
    priceLabel: "₩₩",
    duration: 60,
    crowd: "보통",
    indoor: true,
    url: "#",
  },
  {
    id: "p2",
    name: "디뮤지엄 성수",
    category: "전시",
    area: "성수",
    address: "서울 성동구 왕십리로 83-21",
    description: "감각적인 기획 전시로 유명한 미술관. 실내라 날씨와 무관하게 좋아요.",
    lat: 37.5447,
    lng: 127.0445,
    image: "/modern-art-museum-interior.png",
    rating: 4.7,
    priceLabel: "₩15,000",
    duration: 90,
    crowd: "보통",
    indoor: true,
    url: "#",
  },
  {
    id: "p3",
    name: "서울숲",
    category: "산책",
    area: "성수",
    address: "서울 성동구 뚝섬로 273",
    description: "도심 속 넓은 공원. 사슴방사장과 호수, 산책로가 잘 갖춰져 있어요.",
    lat: 37.5443,
    lng: 127.0379,
    image: "/seoul-forest-park-autumn.png",
    rating: 4.8,
    priceLabel: "무료",
    duration: 80,
    crowd: "여유",
    indoor: false,
    url: "#",
  },
  {
    id: "p4",
    name: "대림창고",
    category: "식당",
    area: "성수",
    address: "서울 성동구 성수이로 78",
    description: "정미소를 개조한 대형 다이닝 카페. 브런치와 파스타가 인기예요.",
    lat: 37.5419,
    lng: 127.0566,
    image: "/industrial-cafe-restaurant-brunch.png",
    rating: 4.4,
    priceLabel: "₩₩₩",
    duration: 70,
    crowd: "혼잡",
    indoor: true,
    url: "#",
  },
  {
    id: "p5",
    name: "어니언 성수",
    category: "카페",
    area: "성수",
    address: "서울 성동구 아차산로9길 8",
    description: "노출콘크리트 감성의 베이커리 카페. 팡도르가 시그니처예요.",
    lat: 37.5443,
    lng: 127.0561,
    image: "/concrete-bakery-cafe-bread.png",
    rating: 4.5,
    priceLabel: "₩₩",
    duration: 50,
    crowd: "혼잡",
    indoor: true,
    url: "#",
  },
  {
    id: "p6",
    name: "언더스탠드에비뉴",
    category: "체험",
    area: "성수",
    address: "서울 성동구 왕십리로 63",
    description: "컨테이너로 구성된 체험형 복합공간. 가족 단위 방문객에게 좋아요.",
    lat: 37.5446,
    lng: 127.0408,
    image: "/container-market-experience-space.png",
    rating: 4.3,
    priceLabel: "무료",
    duration: 60,
    crowd: "여유",
    indoor: false,
    url: "#",
  },
]

export const EVENTS: Event[] = [
  {
    id: "e1",
    name: "성수 디자인 페어 2026",
    category: "축제",
    place: "에스팩토리",
    area: "성수",
    address: "서울 성동구 연무장15길 11",
    period: "2026.06.05 ~ 06.14",
    fee: "₩12,000",
    image: "/design-fair-exhibition-booth.png",
    url: "#",
  },
  {
    id: "e2",
    name: "빛의 정원 미디어아트",
    category: "전시",
    place: "디뮤지엄 성수",
    area: "성수",
    address: "서울 성동구 왕십리로 83-21",
    period: "2026.05.01 ~ 08.31",
    fee: "₩15,000",
    image: "/immersive-media-art-light-exhibition.png",
    url: "#",
  },
  {
    id: "e3",
    name: "한강 여름 야시장",
    category: "축제",
    place: "뚝섬한강공원",
    area: "성수",
    period: "2026.06.06 ~ 08.30",
    address: "서울 광진구 강변북로 139",
    fee: "무료",
    image: "/han-river-night-market-food.png",
    url: "#",
  },
]

export const SAMPLE_COURSE: Course = {
  id: "c1",
  title: "성수 감성 하루 코스",
  area: "성수",
  description:
    "전시와 카페, 산책이 균형 있게 어우러진 커플 추천 코스예요. 실내 전시로 시작해 노을 무렵 서울숲 산책으로 마무리합니다.",
  companion: "COUPLE",
  categories: ["전시", "카페", "산책", "식당"],
  totalDuration: "약 6시간",
  totalDistance: "2.4km",
  cover: "/seoul-forest-park-autumn.png",
  summaryReasons: [
    "선택하신 날짜에 운영 중인 전시가 포함되어 있어요.",
    "커플과 함께 방문하기 좋은 전시·카페·산책 위주로 구성했어요.",
    "혼잡도가 낮은 시간대와 장소를 우선 배치했어요.",
    "장소 간 평균 이동 거리가 600m로 도보 이동이 편해요.",
  ],
  stops: [
    {
      order: 1,
      arrive: "11:00",
      place: PLACES[1],
      reasons: ["선택한 날짜에 운영 중인 전시입니다.", "비 오는 날에도 좋은 실내 장소예요."],
      travelToNext: { mode: "도보", minutes: 8, distance: "600m" },
    },
    {
      order: 2,
      arrive: "12:40",
      place: PLACES[3],
      reasons: ["전시 관람 후 식사하기 좋은 위치예요.", "커플에게 인기 있는 브런치 맛집이에요."],
      travelToNext: { mode: "도보", minutes: 3, distance: "200m" },
    },
    {
      order: 3,
      arrive: "14:10",
      place: PLACES[0],
      reasons: ["식당과 가까운 디저트 카페예요.", "루프탑에서 휴식하기 좋아요."],
      travelToNext: { mode: "도보", minutes: 12, distance: "900m" },
    },
    {
      order: 4,
      arrive: "15:40",
      place: PLACES[2],
      reasons: ["혼잡도가 낮아 쾌적하게 산책할 수 있어요.", "노을 시간대 산책으로 마무리하기 좋아요."],
    },
  ],
}

export const SAVED_COURSES: Course[] = [
  SAMPLE_COURSE,
  {
    ...SAMPLE_COURSE,
    id: "c2",
    title: "연남동 브런치 산책 코스",
    area: "연남동",
    companion: "FRIEND",
    description: "경의선숲길을 따라 걷는 여유로운 친구 코스예요.",
    totalDuration: "약 5시간",
    totalDistance: "1.8km",
    cover: "/yeonnam-park-trail-cafe.png",
  },
]
