package come.back.gotoday.admin.dto.response;

public record AdminSyncResponse(
        String target,
        String status,
        Integer processedCount,
        String message
) {
}