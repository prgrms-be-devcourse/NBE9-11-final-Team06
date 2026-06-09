import { CROWD_META, type CrowdLevel } from "@/lib/data"
import { cn } from "@/lib/utils"
import { Users } from "lucide-react"

export function CrowdBadge({
  level,
  showRange = false,
  className,
}: {
  level: CrowdLevel
  showRange?: boolean
  className?: string
}) {
  const meta = CROWD_META[level]
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-semibold",
        meta.bg,
        meta.color,
        className,
      )}
    >
      <Users className="size-3" />
      {level}
      {showRange && <span className="font-normal opacity-80">· {meta.range}</span>}
    </span>
  )
}
