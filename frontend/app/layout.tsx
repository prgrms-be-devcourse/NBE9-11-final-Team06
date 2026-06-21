import { Analytics } from '@vercel/analytics/next'
import type { Metadata } from 'next'
import { Plus_Jakarta_Sans, Noto_Sans_KR } from 'next/font/google'
import { Toaster } from '@/components/ui/sonner'
import './globals.css'


import Script from "next/script"


const jakarta = Plus_Jakarta_Sans({
  variable: '--font-jakarta',
  subsets: ['latin'],
  display: 'swap',
})
const notoKr = Noto_Sans_KR({
  variable: '--font-noto-kr',
  subsets: ['latin'],
  display: 'swap',
})

export const metadata: Metadata = {
  title: '하루서울 · 서울 하루 여행 코스 추천',
  description:
    '날짜, 위치, 동행, 취향만 고르면 실시간 혼잡도까지 반영한 서울 하루 여행 코스를 추천해 드려요.',
  generator: 'v0.app',
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html
      lang="ko"
      className={`${jakarta.variable} ${notoKr.variable} bg-background`}
    >
      <body className="font-sans antialiased">
        {children}

        <Toaster position="top-center" richColors />

        <Script
          strategy="afterInteractive"
          src={`https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${process.env.NEXT_PUBLIC_NAVER_MAP_CLIENT_ID}`}
        />

        {process.env.NODE_ENV === 'production' && <Analytics />}
      </body>
    </html>
  )
}
