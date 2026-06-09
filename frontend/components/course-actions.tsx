"use client"

import { useState } from "react"
import { Bookmark, BookmarkCheck, Share2 } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"

export function CourseActions({ title }: { title: string }) {
  const [saved, setSaved] = useState(false)

  function toggleSave() {
    if (saved) {
      setSaved(false)
      toast("저장을 취소했어요.")
    } else {
      setSaved(true)
      toast.success("내 코스에 저장했어요.", {
        description: `${title} · 마이페이지에서 확인할 수 있어요.`,
      })
    }
  }

  function share() {
    toast.success("코스 링크를 복사했어요.")
  }

  return (
    <div className="flex gap-2">
      <Button onClick={toggleSave} className="gap-2">
        {saved ? (
          <>
            <BookmarkCheck className="size-4" />
            저장됨
          </>
        ) : (
          <>
            <Bookmark className="size-4" />
            코스 저장
          </>
        )}
      </Button>
      <Button onClick={share} variant="outline" size="icon">
        <Share2 className="size-4" />
        <span className="sr-only">공유</span>
      </Button>
    </div>
  )
}
