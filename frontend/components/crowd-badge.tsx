import { CROWD_META, type CrowdLevel } from "@/lib/data"
import { cn } from "@/lib/utils"
import { Users } from "lucide-react"

function formatPopulationRange(populationMin?: number | null, populationMax?: number | null) {
  if (populationMin == null || populationMax == null) {
    return null
  }

  return `${populationMin.toLocaleString()}~${populationMax.toLocaleString()}명`
}

export function CrowdBadge({
  level,
  showRange = false,
  populationMin,
  populationMax,
  className,
}: {
  level: CrowdLevel
  showRange?: boolean
  populationMin?: number | null
  populationMax?: number | null
  className?: string
}) {
  const meta = CROWD_META[level] ?? CROWD_META["보통"]
  const populationRange = formatPopulationRange(populationMin, populationMax)

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 whitespace-nowrap rounded-full px-2.5 py-1 text-xs font-semibold",
        meta.bg,
        meta.color,
        className,
      )}
    >
      <Users className="size-3" />
      {level}
      {showRange && <span className="font-normal opacity-80">· {populationRange ?? meta.range}</span>}
    </span>
  )
}
