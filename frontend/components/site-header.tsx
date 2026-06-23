"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { Compass, Menu } from "lucide-react"
import { Button, buttonVariants } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { cn } from "@/lib/utils"
import { useAuth } from "@/hooks/use-auth"

const NAV = [
  { href: "/", label: "홈" },
  { href: "/pricing", label: "구독하기" }, //1
  { href: "/plan", label: "코스 추천받기" },
  { href: "/events", label: "행사 둘러보기" },
  { href: "/mypage", label: "마이페이지" },
]

export function SiteHeader() {
  const pathname = usePathname()

  const { isLoggedIn, isAuthLoading, isLogoutLoading, logout } = useAuth()

  return (
    <header className="sticky top-0 z-50 border-b border-border/60 bg-background/80 backdrop-blur-md">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between gap-4 px-4 sm:px-6">
        <Link href="/" className="flex items-center gap-2">
          <span className="flex size-9 items-center justify-center rounded-xl bg-primary text-primary-foreground">
            <Compass className="size-5" />
          </span>
          <span className="text-lg font-bold tracking-tight">오늘 어디가?</span>
        </Link>

        <nav className="hidden items-center gap-1 md:flex">
          {NAV.map((item) => {
            const active =
              item.href === "/"
                ? pathname === "/"
                : pathname.startsWith(item.href)

            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "rounded-full px-4 py-2 text-sm font-medium transition-colors",
                  active
                    ? "bg-secondary text-foreground"
                    : "text-muted-foreground hover:text-foreground",
                )}
              >
                {item.label}
              </Link>
            )
          })}
        </nav>

        <div className="flex items-center gap-2">
          <Button
            render={<Link href="/pricing" />}
            variant="outline"
            size="sm"
            className="hidden sm:inline-flex border-primary text-primary hover:bg-primary/5"
          >
            멤버십 구독
          </Button>
          {!isAuthLoading &&
            (isLoggedIn ? (
              <Button
                type="button"
                variant="ghost"
                size="sm"
                className="hidden sm:inline-flex"
                onClick={logout}
                disabled={isLogoutLoading}
              >
                {isLogoutLoading ? "로그아웃 중..." : "로그아웃"}
              </Button>
            ) : (
              <Button
                render={<Link href="/login" />}
                variant="ghost"
                size="sm"
                className="hidden sm:inline-flex"
              >
                로그인
              </Button>
            ))}

          <Button render={<Link href="/plan" />} size="sm" className="hidden sm:inline-flex">
            코스 추천받기
          </Button>

          <DropdownMenu>
            <DropdownMenuTrigger
              className={cn(
                buttonVariants({ variant: "outline", size: "icon" }),
                "md:hidden",
              )}
            >
              <Menu className="size-5" />
              <span className="sr-only">메뉴 열기</span>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end" className="w-44">
              {NAV.map((item) => (
                <DropdownMenuItem key={item.href} render={<Link href={item.href} />}>
                  {item.label}
                </DropdownMenuItem>
              ))}

              {!isAuthLoading &&
                (isLoggedIn ? (
                  <DropdownMenuItem disabled={isLogoutLoading} onClick={logout}>
                    {isLogoutLoading ? "로그아웃 중..." : "로그아웃"}
                  </DropdownMenuItem>
                ) : (
                  <DropdownMenuItem render={<Link href="/login" />}>
                    로그인
                  </DropdownMenuItem>
                ))}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
    </header>
  )
}