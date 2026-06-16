"use client"

import { useEffect, useState } from "react"
import { toast } from "sonner"
import { categoryApi } from "@/lib/category-api"
import type { PreferenceCategory } from "@/lib/types"

interface CategoryMultiSelectProps {
  selectedCategoryIds: number[]
  onChange: (categoryIds: number[]) => void
  disabled?: boolean
}

export function CategoryMultiSelect({
  selectedCategoryIds,
  onChange,
  disabled = false,
}: CategoryMultiSelectProps) {
  const [categories, setCategories] = useState<PreferenceCategory[]>([])
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    let ignore = false

    async function fetchCategories() {
      setIsLoading(true)

      try {
        const response = await categoryApi.getCategories()

        if (ignore) {
          return
        }

        if (!response.success || !response.data) {
          toast.error(response.message ?? "카테고리 목록을 불러오지 못했습니다.")
          setCategories([])
          return
        }

        setCategories(response.data)
      } catch {
        if (!ignore) {
          toast.error("카테고리 목록 조회 중 오류가 발생했습니다.")
          setCategories([])
        }
      } finally {
        if (!ignore) {
          setIsLoading(false)
        }
      }
    }

    fetchCategories()

    return () => {
      ignore = true
    }
  }, [])

  function toggleCategory(categoryId: number) {
    if (disabled) {
      return
    }

    if (selectedCategoryIds.includes(categoryId)) {
      onChange(selectedCategoryIds.filter((id) => id !== categoryId))
      return
    }

    onChange([...selectedCategoryIds, categoryId])
  }

  if (isLoading) {
    return (
      <p className="text-sm text-muted-foreground">
        카테고리 목록을 불러오는 중입니다.
      </p>
    )
  }

  if (categories.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        등록된 카테고리가 없습니다.
      </p>
    )
  }

  return (
    <div className="flex flex-wrap gap-2">
      {categories.map((category) => {
        const active = selectedCategoryIds.includes(category.id)

        return (
          <button
            key={category.id}
            type="button"
            disabled={disabled}
            onClick={() => toggleCategory(category.id)}
            className={`rounded-full border px-3 py-1.5 text-sm transition-colors disabled:cursor-not-allowed disabled:opacity-60 ${
              active
                ? "border-primary bg-primary text-primary-foreground"
                : "border-border bg-background text-foreground hover:border-primary/40"
            }`}
          >
            {category.name}
          </button>
        )
      })}
    </div>
  )
}