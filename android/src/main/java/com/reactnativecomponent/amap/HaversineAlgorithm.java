package com.reactnativecomponent.amap;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

/**
 * Created by wd on 10/08/2017.
 * code from: https://stackoverflow.com/questions/365826/calculate-distance-between-2-gps-coordinates
 * and https://stackoverflow.com/questions/1185408/converting-from-longitude-latitude-to-cartesian-coordinates
 */

public class HaversineAlgorithm {

    private static final double _eQuatorialEarthRadius = 6378.1370D;
    private static final double _d2r = (Math.PI / 180D);

    public static int HaversineInM(double lat1, double long1, double lat2, double long2) {
        return (int) (1000D * HaversineInKM(lat1, long1, lat2, long2));
    }

    public static double HaversineInKM(double lat1, double long1, double lat2, double long2) {
        double dlong = (long2 - long1) * _d2r;
        double dlat = (lat2 - lat1) * _d2r;
        double a = Math.pow(Math.sin(dlat / 2D), 2D) + Math.cos(lat1 * _d2r) * Math.cos(lat2 * _d2r)
                * Math.pow(Math.sin(dlong / 2D), 2D);
        double c = 2D * Math.atan2(Math.sqrt(a), Math.sqrt(1D - a));
        double d = _eQuatorialEarthRadius * c;

        return d;
    }

    public static WritableMap gpsToCartesian(double lat, double lng) {
        double x = _eQuatorialEarthRadius * Math.cos(lat * _d2r) * Math.cos(lng * _d2r);
        double y = _eQuatorialEarthRadius * Math.cos(lat * _d2r) * Math.sin(lng * _d2r);
        double z = _eQuatorialEarthRadius * Math.sin(lat * _d2r);

        WritableMap ret = Arguments.createMap();
        ret.putDouble("x", x);
        ret.putDouble("y", y);
        ret.putDouble("z", z);
        return ret;
    }

    public static WritableMap cartesianToGPS(double x, double y, double z) {
        double lat = Math.asin(z/_eQuatorialEarthRadius)/_d2r;
        double lng= Math.atan2(y, x)/_d2r;

        WritableMap ret = Arguments.createMap();
        ret.putDouble("latitude", lat);
        ret.putDouble("longitude", lng);
        return ret;
    }

    public static ReadableMap WGS84toGoogleBing(double lat, double lng) {
        double x = lng * 20037508.34 / 180;
        double y = Math.log(Math.tan((90 + lat) * Math.PI / 360)) / (Math.PI / 180);
        y = y * 20037508.34 / 180;

        WritableMap ret = Arguments.createMap();
        ret.putDouble("x", x);
        ret.putDouble("y", y);
        return ret;
    }

    public static ReadableMap GoogleBingtoWGS84Mercator (double x, double y) {
        double lng = (x / 20037508.34) * 180;
        double lat = (y / 20037508.34) * 180;

        lat = 180/Math.PI * (2 * Math.atan(Math.exp(lat * Math.PI / 180)) - Math.PI / 2);
        WritableMap ret = Arguments.createMap();
        ret.putDouble("latitude", lat);
        ret.putDouble("longitude", lng);
        return ret;
    }

    public static ReadableMap addDistanceToGPS(double lat, double lng, double xSpan, double ySpan) {
        ReadableMap xy = WGS84toGoogleBing(lat, lng);
        ReadableMap gps = GoogleBingtoWGS84Mercator(xy.getDouble("x") + xSpan, xy.getDouble("y") + ySpan);
        return gps;
    }

}