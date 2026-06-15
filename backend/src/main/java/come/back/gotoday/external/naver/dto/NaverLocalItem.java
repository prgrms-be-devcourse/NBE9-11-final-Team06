package come.back.gotoday.external.naver.dto;

public record NaverLocalItem(
        String title,
        String link,
        String category,
        String description,
        String telephone,
        String address,
        String roadAddress,
        String mapx,
        String mapy
) {
}
