"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { Bookmark, BookmarkCheck, Share2 } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { courseBookmarkApi } from "@/lib/course-bookmark-api"

interface CourseActionsProps {
  courseId: number
  title: string
}

export function CourseActions({ courseId, title }: CourseActionsProps) {
  const router = useRouter()

  const [saved, setSaved] = useState(false)
  const [isStatusLoading, setIsStatusLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)

  useEffect(() => {
    let ignore = false

    async function fetchBookmarkStatus() {
      setIsStatusLoading(true)

      try {
        const response = await courseBookmarkApi.getBookmarkStatus(courseId)

        if (ignore) {
          return
        }

        if (response.success && typeof response.data === "boolean") {
          setSaved(response.data)
          return
        }

        if (response.code === "401") {
          setSaved(false)
          return
        }
      } catch {
        if (!ignore) {
          setSaved(false)
        }
      } finally {
        if (!ignore) {
          setIsStatusLoading(false)
        }
      }
    }

    if (courseId) {
      fetchBookmarkStatus()
    }

    return () => {
      ignore = true
    }
  }, [courseId])

  async function toggleSave() {
    if (isSaving) {
      return
    }

    setIsSaving(true)

    try {
      const response = await courseBookmarkApi.toggleBookmark(courseId)

      if (!response.success || !response.data) {
        if (response.code === "401") {
          toast.error("로그인이 필요한 기능입니다.")
          router.push("/login")
          return
        }

        toast.error(response.message ?? "코스 북마크 처리에 실패했습니다.")
        return
      }

      setSaved(response.data.bookmarked)

      if (response.data.bookmarked) {
        toast.success("코스를 북마크했어요.", {
          description: `${title} · 마이페이지에서 확인할 수 있어요.`,
        })
      } else {
        toast("코스 북마크를 해제했어요.")
      }
    } catch {
      toast.error("서버와 통신 중 오류가 발생했습니다.")
    } finally {
      setIsSaving(false)
    }
  }

  async function share() {
    try {
      await navigator.clipboard.writeText(window.location.href)
      toast.success("코스 링크를 복사했어요.")
    } catch {
      toast.error("코스 링크 복사에 실패했습니다.")
    }
  }

  return (
    <div className="flex gap-2">
      <Button
        type="button"
        onClick={toggleSave}
        disabled={isStatusLoading || isSaving}
        className="gap-2"
      >
        {saved ? (
          <>
            <BookmarkCheck className="size-4" />
            북마크됨
          </>
        ) : (
          <>
            <Bookmark className="size-4" />
            코스 북마크
          </>
        )}
      </Button>

      <Button type="button" onClick={share} variant="outline" size="icon">
        <Share2 className="size-4" />
        <span className="sr-only">공유</span>
      </Button>
    </div>
  )
}