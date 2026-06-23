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

    private static final double RE = EARTH_RADIUS_KM / GRID_SPACING_KM;
    private static final double SLAT1 = STANDARD_LATITUDE_1 * DEGREE_TO_RADIAN;
    private static final double SLAT2 = STANDARD_LATITUDE_2 * DEGREE_TO_RADIAN;
    private static final double OLON = ORIGIN_LONGITUDE * DEGREE_TO_RADIAN;
    private static final double OLAT = ORIGIN_LATITUDE * DEGREE_TO_RADIAN;
    private static final double SN = Math.log(Math.cos(SLAT1) / Math.cos(SLAT2))
            / Math.log(Math.tan(Math.PI * 0.25 + SLAT2 * 0.5)
            / Math.tan(Math.PI * 0.25 + SLAT1 * 0.5));
    private static final double SF = Math.pow(Math.tan(Math.PI * 0.25 + SLAT1 * 0.5), SN)
            * Math.cos(SLAT1) / SN;
    private static final double RO = RE * SF
            / Math.pow(Math.tan(Math.PI * 0.25 + OLAT * 0.5), SN);

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
        double ra = Math.tan(Math.PI * 0.25 + latitude * DEGREE_TO_RADIAN * 0.5);
        ra = RE * SF / Math.pow(ra, SN);

        double theta = longitude * DEGREE_TO_RADIAN - OLON;
        if (theta > Math.PI) {
            theta -= Math.PI * 2.0;
        }
        if (theta < -Math.PI) {
            theta += Math.PI * 2.0;
        }
        theta *= SN;

        int nx = (int) Math.floor(ra * Math.sin(theta) + ORIGIN_X + 0.5);
        int ny = (int) Math.floor(RO - ra * Math.cos(theta) + ORIGIN_Y + 0.5);

        return new GridCoordinate(nx, ny);
    }

    public record GridCoordinate(int nx, int ny) {
    }
}
