import { useState } from "react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { PaymentHistoryResponse } from "@/lib/billing-api"
import { PaymentCancelDialog } from "./payment-cancel-dialog"

export function PaymentHistoryList({ histories, refetch,refetchSubscription }: { histories: PaymentHistoryResponse[], refetch: () => void ,refetchSubscription: () => Promise<void>}) {
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const now = new Date()
  const handleCancelSuccess = async () => {
    refetch();                  // 기존 결제 내역 갱신
    await refetchSubscription(); // 백엔드 DB에서 CANCELED가 된 구독 정보를 프론트에 즉시 반영
  }
  return (
    <div className="space-y-4">
      {histories.map((h) => {
        const date = new Date(h.createdAt)
        const isCurrentMonth = date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth()
        const canCancel = h.status === 'SUCCESS' && isCurrentMonth

        return (
          <div key={h.paymentHistoryId} className="flex items-center justify-between p-4 border border-border/50 rounded-xl hover:bg-muted/30 transition-colors">
            <div>
              <p className="font-semibold text-lg">₩{h.amount.toLocaleString()}</p>
              <p className="text-xs text-muted-foreground">결제일: {h.createdAt.split('T')[0]}</p>
            </div>
            <div className="flex items-center gap-3">
              <Badge variant={h.status === 'SUCCESS' ? 'default' : 'secondary'}>
                {h.status === 'SUCCESS' ? '결제 완료' : h.status}
              </Badge>
              {canCancel && (
                <Button 
                  size="sm" 
                  variant="outline" 
                  className="border-red-200 text-red-600 hover:bg-red-50 hover:text-red-700"
                  onClick={() => setSelectedId(h.paymentHistoryId)}
                >
                  결제 취소
                </Button>
              )}
            </div>
          </div>
        )
      })}
      
      {selectedId && (
        <PaymentCancelDialog 
          isOpen={!!selectedId} 
          onClose={() => setSelectedId(null)} 
          paymentHistoryId={selectedId} 
          onSuccess={handleCancelSuccess}
        />
      )}
    </div>
  )
}