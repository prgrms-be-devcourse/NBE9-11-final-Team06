package come.back.gotoday.weather.util;

/**
 * WGS84 위도·경도를 기상청 단기예보 DFS 격자 좌표(nx, ny)로 변환하는 유틸리티입니다.
 */
public final class KmaGridConverter {

    private static final double EARTH_RADIUS_KM = 6371.00877;
    private static final double GRID_SPACING_KM = 5.0;
    private static final double STANDARD_LATITUDE_1 = 30.0;
    private static final double STANDARD_LATITUDE_2 = 60.0;
    private static final double ORIGIN_LONGITUDE = 126.0;
    private static final double ORIGIN_LATITUDE = 38.0;
    private static final double ORIGIN_X = 43.0;
    private static final double ORIGIN_Y = 136.0;

    private static final double DEGREE_TO_RADIAN = Math.PI / 180.0;

    private KmaGridConverter() {
    }

    /**
     * 위도·경도를 기상청 단기예보 격자 좌표로 변환합니다.
     *
     * @param latitude WGS84 위도
     * @param longitude WGS84 경도
     * @return 기상청 격자 좌표
     */
    public static GridCoordinate toGrid(double latitude, double longitude) {
        double re = EARTH_RADIUS_KM / GRID_SPACING_KM;
        double slat1 = STANDARD_LATITUDE_1 * DEGREE_TO_RADIAN;
        double slat2 = STANDARD_LATITUDE_2 * DEGREE_TO_RADIAN;
        double olon = ORIGIN_LONGITUDE * DEGREE_TO_RADIAN;
        double olat = ORIGIN_LATITUDE * DEGREE_TO_RADIAN;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5)
                / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);

        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;

        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + latitude * DEGREE_TO_RADIAN * 0.5);
        ra = re * sf / Math.pow(ra, sn);

        double theta = longitude * DEGREE_TO_RADIAN - olon;
        if (theta > Math.PI) {
            theta -= Math.PI * 2.0;
        }
        if (theta < -Math.PI) {
            theta += Math.PI * 2.0;
        }
        theta *= sn;

        int nx = (int) Math.floor(ra * Math.sin(theta) + ORIGIN_X + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + ORIGIN_Y + 0.5);

        return new GridCoordinate(nx, ny);
    }

    public record GridCoordinate(int nx, int ny) {
    }
}
