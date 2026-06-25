import { useState } from "react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { 
  Dialog, 
  DialogContent, 
  DialogHeader, 
  DialogTitle, 
  DialogDescription, 
  DialogFooter 
} from "@/components/ui/dialog"
import { billingApi } from "@/lib/billing-api"

interface Props {
  isOpen: boolean
  onClose: () => void
  paymentHistoryId: number
  onSuccess: () => void
}

export function PaymentCancelDialog({ isOpen, onClose, paymentHistoryId, onSuccess }: Props) {
  const [reasonType, setReasonType] = useState("단순 변심")
  const [detailReason, setDetailReason] = useState("")
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleCancel = async () => {
    const finalReason = reasonType === "기타" ? detailReason : reasonType
    if (!finalReason.trim()) {
      toast.error("취소 사유를 입력해주세요.")
      return
    }

    setIsSubmitting(true)
    try {
      const res = await billingApi.cancelPayment(paymentHistoryId, finalReason)
      if (res.success) {
        toast.success("결제가 성공적으로 취소되었습니다.")
        onSuccess()
        onClose()
      } else {
        toast.error(res.message || "취소 요청에 실패했습니다.")
      }
    } catch {
      toast.error("서버 통신 중 오류가 발생했습니다.")
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>결제 취소 요청</DialogTitle>
          <DialogDescription>취소 사유를 선택해주세요. 당월 결제 건은 즉시 전액 환불됩니다.</DialogDescription>
        </DialogHeader>
        
        <div className="flex flex-col gap-3 py-4">
          {["단순 변심", "서비스 미사용", "기타"].map((r) => (
            <label key={r} className="flex items-center gap-2 cursor-pointer border p-3 rounded-lg hover:bg-muted/50">
              <input 
                type="radio" name="reason" value={r} checked={reasonType === r}
                onChange={(e) => setReasonType(e.target.value)} 
              />
              {r}
            </label>
          ))}
          {reasonType === "기타" && (
            <textarea 
              className="w-full min-h-[80px] p-2 rounded-md border border-input bg-background text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              placeholder="기타 사유를 자세히 입력해주세요." 
              value={detailReason} 
              onChange={(e) => setDetailReason(e.target.value)} 
            />
          )}
        </div>

        <DialogFooter>
          <Button variant="ghost" onClick={onClose}>취소</Button>
          <Button variant="destructive" onClick={handleCancel} disabled={isSubmitting}>
            {isSubmitting ? "처리중..." : "결제 취소하기"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}