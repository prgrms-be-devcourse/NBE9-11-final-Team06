import Link from "next/link"
import { Compass } from "lucide-react"

export function SiteFooter() {
  return (
    <footer className="mt-20 border-t border-border/60 bg-secondary/40">
      <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-10 sm:px-6 md:flex-row md:items-center md:justify-between">
        <div className="flex items-center gap-2">
          <span className="flex size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <Compass className="size-4" />
          </span>
          <span className="font-bold">하루서울</span>
        </div>
        <p className="max-w-md text-sm leading-relaxed text-muted-foreground">
          날짜, 위치, 동행, 취향만 고르면 실시간 혼잡도까지 반영한 서울 하루 여행
          코스를 추천해 드려요.
        </p>
        <nav className="flex flex-wrap gap-x-5 gap-y-2 text-sm text-muted-foreground">
          <Link href="/plan" className="hover:text-foreground">
            코스 추천
          </Link>
          <Link href="/explore" className="hover:text-foreground">
            둘러보기
          </Link>
          <Link href="/mypage" className="hover:text-foreground">
            마이페이지
          </Link>
          <Link href="/admin" className="hover:text-foreground">
            관리자
          </Link>
        </nav>
      </div>
      <div className="border-t border-border/60 py-4">
        <p className="text-center text-xs text-muted-foreground">
          © 2026 하루서울 · 서울 실시간 도시데이터 기반 여행 코스 추천 서비스
        </p>
      </div>
    </footer>
  )
}
