package come.back.gotoday.crowd.util;

public final class GeoDistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoDistanceCalculator() {
    }

    public static double calculateKilometers(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2
    ) {
        double latitudeDistance = Math.toRadians(latitude2 - latitude1);
        double longitudeDistance = Math.toRadians(longitude2 - longitude1);

        double value = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2)
                + Math.cos(Math.toRadians(latitude1))
                * Math.cos(Math.toRadians(latitude2))
                * Math.sin(longitudeDistance / 2)
                * Math.sin(longitudeDistance / 2);

        return EARTH_RADIUS_KM
                * 2
                * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
    }
}