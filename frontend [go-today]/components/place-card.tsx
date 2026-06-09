import Image from "next/image"
import { MapPin, Star, Clock } from "lucide-react"
import { Card } from "@/components/ui/card"
import { CrowdBadge } from "@/components/crowd-badge"
import { type Place } from "@/lib/data"

export function PlaceCard({ place }: { place: Place }) {
  return (
    <Card className="group overflow-hidden p-0 transition-shadow hover:shadow-lg">
      <div className="relative aspect-[4/3] overflow-hidden">
        <Image
          src={place.image || "/placeholder.svg"}
          alt={place.name}
          fill
          className="object-cover transition-transform duration-300 group-hover:scale-105"
          sizes="(max-width: 768px) 100vw, 33vw"
        />
        <span className="absolute left-3 top-3 rounded-full bg-background/90 px-2.5 py-1 text-xs font-semibold text-foreground backdrop-blur">
          {place.category}
        </span>
        <span className="absolute right-3 top-3">
          <CrowdBadge level={place.crowd} />
        </span>
      </div>
      <div className="flex flex-col gap-2 p-4">
        <div className="flex items-start justify-between gap-2">
          <h3 className="font-bold leading-tight">{place.name}</h3>
          <span className="flex shrink-0 items-center gap-0.5 text-sm font-semibold text-amber-600">
            <Star className="size-3.5 fill-current" />
            {place.rating}
          </span>
        </div>
        <p className="line-clamp-2 text-sm leading-relaxed text-muted-foreground">
          {place.description}
        </p>
        <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
          <span className="flex items-center gap-1">
            <MapPin className="size-3" />
            {place.area}
          </span>
          <span className="flex items-center gap-1">
            <Clock className="size-3" />
            {place.duration}분
          </span>
          <span className="font-medium text-foreground">{place.priceLabel}</span>
        </div>
      </div>
    </Card>
  )
}
