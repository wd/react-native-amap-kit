package com.reactnativecomponent.amap;

import android.graphics.Bitmap;
import android.support.v4.print.PrintHelper;
import android.util.Log;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.PolygonOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.amap.api.maps.AMapUtils.calculateLineDistance;

public class RCTAMapModule extends ReactContextBaseJavaModule implements PoiSearch.OnPoiSearchListener, GeocodeSearch.OnGeocodeSearchListener {
    ReactApplicationContext mContext;

    private PoiSearch poiSearch;
    private int defaultRadius = 3000;
    private final String onPOISearchDoneEventName = "amap.onPOISearchDone";
    private final String onReGeocodeSearchDoneEvent = "OnReGeocodeSearchDoneEvent";

    public RCTAMapModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mContext = reactContext;
        PoiSearch poiSearch = new PoiSearch(mContext, null);
        this.poiSearch = poiSearch;
    }

    @Override
    public String getName() {
        return "AMapModule";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("OnPOISearchDoneEvent", onPOISearchDoneEventName);
        constants.put("OnReGeocodeSearchDoneEvent", onReGeocodeSearchDoneEvent);
        return constants;
    }

    @ReactMethod
    public void setCenterCoordinate(final int reactTag, final ReadableMap coordinate) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));
                if (mapView == null) {
                    Log.d("AMAP", "mapView is null.");
                    return;
                }
                Log.d("AMAP", String.format("setCenterCoordinate: %s", coordinate.toString()));
                mapView.setCenterLocation(coordinate.getDouble("latitude"), coordinate.getDouble("longitude"));
            }
        });
    }

    @ReactMethod
    public void searchPoiByCenterCoordinate(ReadableMap params) {

        String types = "";
        if (params.hasKey("types")) {
            types = params.getString("types");
        }
        String keywords = "";
        if (params.hasKey("keywords")) {
            keywords = params.getString("keywords");
        }

        PoiSearch.Query query = new PoiSearch.Query(keywords, types);

        if (params.hasKey("limit")) {
            int limit = params.getInt("limit");
            query.setPageSize(limit);// 设置每页最多返回多少条 poiitem
        }
        if (params.hasKey("page")) {
            int page = params.getInt("page");
            query.setPageNum(page);//设置查询页码，从 1 开始
        }
        poiSearch.setQuery(query);
        if (params.hasKey("coordinate")) {
            ReadableMap coordinateMap = params.getMap("coordinate");
            double latitude = coordinateMap.getDouble("latitude");
            double longitude = coordinateMap.getDouble("longitude");
            int radius = defaultRadius;
            if (params.hasKey("radius")) {
                radius = params.getInt("radius");
            }
            poiSearch.setBound(new PoiSearch.SearchBound(new LatLonPoint(latitude, longitude), radius)); //设置周边搜索的中心点以及半径(单位: 米, 默认3公里)
        }
        poiSearch.setOnPoiSearchListener(this);
        poiSearch.searchPOIAsyn();
    }

    @Override
    public void onPoiSearched(PoiResult result, int rCode) {
        List<PoiItem> poiItems;
        WritableMap dataMap = Arguments.createMap();
        if (rCode == 1000) {
            if (result != null && result.getQuery() != null) {// 搜索poi的结果
                poiItems = result.getPois();

                WritableArray array = Arguments.createArray();
                for (PoiItem poi : poiItems) {
                    WritableMap data = Arguments.createMap();
                    data.putString("uid", poi.getPoiId());
                    data.putString("name", poi.getTitle());
                    data.putString("type", poi.getTypeDes());
                    data.putDouble("longitude", poi.getLatLonPoint().getLongitude());
                    data.putDouble("latitude", poi.getLatLonPoint().getLatitude());
                    data.putString("address", poi.getSnippet());
                    data.putString("tel", poi.getTel());
                    data.putInt("distance", poi.getDistance());
                    data.putString("cityCode", poi.getCityCode());
                    data.putString("cityName", poi.getCityName());
                    data.putString("provinceCode", poi.getProvinceCode());
                    data.putString("provinceName", poi.getProvinceName());
                    data.putString("adCode", poi.getAdCode());
                    data.putString("adName", poi.getAdName());
                    array.pushMap(data);
                }

                dataMap.putString("keywords", poiSearch.getQuery().getQueryString());
                dataMap.putString("types", poiSearch.getQuery().getCategory());
                dataMap.putArray("list", array);
            }
        } else {
            WritableMap error = Arguments.createMap();
            error.putString("code", String.valueOf(rCode));
            dataMap.putMap("error", error);
        }

        mContext
                .getJSModule(RCTNativeAppEventEmitter.class)
                .emit(onPOISearchDoneEventName, dataMap);
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {

    }

    private Boolean isLineInCircle(LatLng center, LatLng location, Float radius) {
        float dist = calculateLineDistance(center, location);
        if (dist > radius) {
            return false;
        }
        return true;
    }

    @ReactMethod
    public void isLinesInCircle(ReadableMap centerCoordinate, Float radius, ReadableArray lines, Callback callback) {
        LatLng center = new LatLng(centerCoordinate.getDouble("latitude"), centerCoordinate.getDouble("longitude"));
        WritableMap data = Arguments.createMap();
        WritableArray ret = Arguments.createArray();

        for (int i = 0; i < lines.size(); i++) {
            ReadableMap lineCoordinate = lines.getMap(i);
            ReadableMap departure = lineCoordinate.getMap("departure");
            ReadableMap arrive = lineCoordinate.getMap("arrive");

            LatLng depLatLng = new LatLng(departure.getDouble("latitude"), departure.getDouble("longitude"));
            LatLng arrLatLng = new LatLng(arrive.getDouble("latitude"), arrive.getDouble("longitude"));

            if (isLineInCircle(center, depLatLng, radius) && isLineInCircle(center, arrLatLng, radius)) {
                ret.pushBoolean(true);
            } else {
                ret.pushBoolean(false);
            }
        }
        data.putArray("results", ret);
        callback.invoke(data);
    }

    @ReactMethod
    public void searchPoiByUid(String uid, Callback callback) {
        try {
            PoiItem poi = poiSearch.searchPOIId(uid);
            callback.invoke(poi);
        } catch (AMapException e) {
            callback.invoke();
        }
    }

    @ReactMethod
    public void setRegionByLatLngs(final int reactTag, final ReadableMap region) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "setRegionByLatLngs: mapView is null.");
                    return;
                }

                ReadableArray coordinates = region.getArray("coordinates");
                boolean animate = true;
                if (region.hasKey("animate"))
                    animate = region.getBoolean("animate");

                mapView.setRegionConfigByLatLngs(region);
            }
        });
    }

    @ReactMethod
    public void setLatLngZoom(final int reactTag, final ReadableMap config) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "setLatLngZoom: mapView is null.");
                    return;
                }

                ReadableMap coordinate = config.getMap("coordinate");
                boolean animate = true;
                if (config.hasKey("animate"))
                    animate = config.getBoolean("animate");

                Double zoomLevel = mapView.getZoomLevel();
                if (config.hasKey("zoomLevel"))
                    zoomLevel = config.getDouble("zoomLevel");


                LatLng latLng;
                if (coordinate.hasKey("latitude"))
                    latLng = new LatLng(coordinate.getDouble("latitude"), coordinate.getDouble("longitude"));
                else
                    latLng = new LatLng(coordinate.getDouble("lat"), coordinate.getDouble("lng"));

                CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel.floatValue());
                if (animate) {
                    mapView.animateCamera(update);
                } else {
                    mapView.changeCamera(update);
                }
            }
        });
    }


    @ReactMethod
    public void setRegion(final int reactTag, final ReadableMap region) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                                                            if (mapView == null) {
                                                                Log.d("AMAP", "setRegion: mapView is null.");
                                                                return;
                                                            }

                                                            mapView.setRegionConfig(region);
                                                        }
                                                    }
        );
    }

    @ReactMethod
    public void minZoomLevel(final int reactTag, final Callback callback) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "minZoomLevel: mapView is null.");
                    return;
                }

                callback.invoke(mapView.getMinZoomLevel());
            }
        });
    }

    @ReactMethod
    public void maxZoomLevel(final int reactTag, final Callback callback) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "maxZoomLevel: mapView is null.");
                    return;
                }

                callback.invoke(mapView.getMaxZoomLevel());
            }
        });
    }

    @ReactMethod
    public void zoomLevel(final int reactTag, final Callback callback) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "zoomLevel: mapView is null.");
                    return;
                }

                callback.invoke(mapView.getZoomLevel());
            }
        });
    }

    private Marker addAnnotation(RCTAMapView mapView, ReadableMap config) {
        ReadableMap point = config.getMap("coordinate");
        String imgName = config.getString("imageName");

        MarkerOptions markerOptions = new MarkerOptions();
        if (config.hasKey("title")) {
            String title = config.getString("title");
            if (!title.isEmpty())
                markerOptions.title(title);
        }

        if (config.hasKey("snippet")) {
            String snippet = config.getString("snippet");
            if (!snippet.isEmpty())
                markerOptions.snippet(snippet);
        }

        if (config.hasKey("angle")) {
            float angle = (float) config.getDouble("angle");
            markerOptions.rotateAngle(angle);
        }

        boolean draggable = false;
        if (config.hasKey("draggable")) {
            draggable = config.getBoolean("draggable");
        }
        markerOptions.draggable(draggable);

        boolean visible = true;
        if (config.hasKey("visible")) {
            visible = config.getBoolean("visible");
        }
        markerOptions.visible(visible);

        LatLng latLng;
        if (point.hasKey("latitude"))
            latLng = new LatLng(point.getDouble("latitude"), point.getDouble("longitude"));
        else
            latLng = new LatLng(point.getDouble("lat"), point.getDouble("lng"));
        markerOptions.position(latLng);
        Marker marker = mapView.addMarker(markerOptions, imgName, config);
        return marker;
    }

    @ReactMethod
    public void addAnnotation(final int reactTag, final ReadableMap config, final Callback callback) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "addAnnotation: mapView is null.");
                    return;
                }

                Marker marker = addAnnotation(mapView, config);
                callback.invoke(marker.getId());
            }
        });
    }

    @ReactMethod
    public void addAnnotations(final int reactTag, final ReadableArray configs, final Callback callback) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "addAnnotations: mapView is null.");
                    return;
                }

                WritableArray markers = new WritableNativeArray();
                for (int i = 0; i < configs.size(); i++) {
                    Marker marker = addAnnotation(mapView, configs.getMap(i));
                    markers.pushString(marker.getId());
                }
                callback.invoke(markers);
            }
        });
    }

    @ReactMethod
    public void removeAnnotation(final int reactTag, final String key) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "removeAnnotation: mapView is null.");
                    return;
                }

                mapView.removeMarker(key);
            }
        });
    }

    @ReactMethod
    public void removeAnnotations(final int reactTag, final ReadableArray keys) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "removeAnnotation: mapView is null.");
                    return;
                }

                for (int i = 0; i < keys.size(); i++) {
                    mapView.removeMarker(keys.getString(i));
                }
            }
        });
    }

    @ReactMethod
    public void removeAllAnnotations(final int reactTag, final Callback callback) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "addAnnotation: mapView is null.");
                    return;
                }

                mapView.removeAllMarkers();
                callback.invoke(true);
            }
        });
    }

    @ReactMethod
    public void showInfoWindow(final int reactTag, final String key) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "showInfoWindow: mapView is null.");
                    return;
                }
                mapView.showInfoWindow(key);
            }
        });
    }

    @ReactMethod
    public void hideInfoWindow(final int reactTag, final String key) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "hideInfoWindow: mapView is null.");
                    return;
                }
                mapView.hideInfoWindow(key);
            }
        });
    }

    @ReactMethod
    public void showAnnotation(final int reactTag, final String key) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "showAnnotation: mapView is null.");
                    return;
                }
                mapView.showAnnotation(key);
            }
        });
    }


    @ReactMethod
    public void hideAnnotation(final int reactTag, final String key) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "hideAnnotation: mapView is null.");
                    return;
                }
                mapView.hideAnnotation(key);
            }
        });
    }

    @ReactMethod
    public void showAnnotations(final int reactTag, final ReadableArray keys) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "showAnnotations: mapView is null.");
                    return;
                }
                for (int i = 0; i < keys.size(); i++) {
                    mapView.showAnnotation(keys.getString(i));
                }
            }
        });
    }


    @ReactMethod
    public void hideAnnotations(final int reactTag, final ReadableArray keys) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "hideAnnotations: mapView is null.");
                    return;
                }
                for (int i = 0; i < keys.size(); i++) {
                    mapView.hideAnnotation(keys.getString(i));
                }
            }
        });
    }


    private Circle addCircle(RCTAMapView mapView, ReadableMap config) {
        int strokeColor = Utils.parseColor(config.getString("strokeColor"));
        int fillColor = Utils.parseColor(config.getString("fillColor"));
        int lineWidth = config.getInt("lineWidth");
        double radius = config.getDouble("radius");
        ReadableMap coordinate = config.getMap("coordinate");
        LatLng latLng;
        if (coordinate.hasKey("latitude"))
            latLng = new LatLng(coordinate.getDouble("latitude"), coordinate.getDouble("longitude"));
        else
            latLng = new LatLng(coordinate.getDouble("lat"), coordinate.getDouble("lng"));

        CircleOptions circleOption = new CircleOptions();
        circleOption.strokeWidth(lineWidth);
        circleOption.fillColor(fillColor);
        circleOption.strokeColor(strokeColor);
        circleOption.radius(radius);
        circleOption.center(latLng);

        Circle circle = mapView.addCircle(circleOption,config);
        return circle;
    }

    @ReactMethod
    public void addCircle(final int reactTag, final ReadableMap config, final Callback callback) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "addCircle: mapView is null.");
                    return;
                }
                Circle circle = addCircle(mapView, config);
                callback.invoke(circle.getId());
            }
        });
    }

    @ReactMethod
    public void addCircles(final int reactTag, final ReadableArray configs, final Callback callback) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "addCircles: mapView is null.");
                    return;
                }

                WritableArray circles = new WritableNativeArray();
                for (int i = 0; i < configs.size(); i++) {
                    ReadableMap config = configs.getMap(i);
                    Circle circle = addCircle(mapView, config);
                    circles.pushString(circle.getId());
                }
                callback.invoke(circles);
            }
        });
    }

    @ReactMethod
    public void removeCircle(final int reactTag, final String key) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "removeCircle: mapView is null.");
                    return;
                }

                mapView.removeCircle(key);
            }
        });
    }

    @ReactMethod
    public void removeCircles(final int reactTag, final ReadableArray circles) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "removeCircles: mapView is null.");
                    return;
                }

                for (int i = 0; i < circles.size(); i++) {
                    mapView.removeCircle(circles.getString(i));
                }
            }
        });
    }

    @ReactMethod
    public void removeAllCircles(final int reactTag, final Callback callback) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "removeCircles: mapView is null.");
                    return;
                }

                mapView.removeAllCircles();
                callback.invoke(true);
            }
        });
    }

    private Polyline addPolyline(RCTAMapView mapView, final ReadableMap config) {
        int strokeColor = Utils.parseColor(config.getString("strokeColor"));
        int lineWidth = config.getInt("lineWidth");

        ReadableArray coordinates = config.getArray("coordinates");
        List<LatLng> latLngs = new ArrayList<>();
        for (int i = 0; i < coordinates.size(); i++) {
            ReadableMap point = coordinates.getMap(i);
            LatLng latLng;
            if (point.hasKey("latitude"))
                latLng = new LatLng(point.getDouble("latitude"), point.getDouble("longitude"));
            else
                latLng = new LatLng(point.getDouble("lat"), point.getDouble("lng"));
            latLngs.add(latLng);
        }
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.color(strokeColor);
        polylineOptions.width(lineWidth);
        polylineOptions.addAll(latLngs);
        Polyline polyline = mapView.addPolyline(polylineOptions,config);
        return polyline;
    }

    @ReactMethod
    public void addPolylines(final int reactTag, final ReadableArray configs, final Callback callback) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "addPolylines: mapView is null.");
                    return;
                }

                WritableArray polylines = new WritableNativeArray();
                for (int i = 0; i < configs.size(); i++) {
                    Polyline polyline = addPolyline(mapView, configs.getMap(i));
                    polylines.pushString(polyline.getId());
                }
                callback.invoke(polylines);
            }
        });
    }

    @ReactMethod
    public void addPolyline(final int reactTag, final ReadableMap config, final Callback callback) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "addPolyline: mapView is null.");
                    return;
                }

                Polyline polyline = addPolyline(mapView, config);
                callback.invoke(polyline.getId());
            }
        });
    }

    @ReactMethod
    public void removePolyline(final int reactTag, final String key) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "removePolyline: mapView is null.");
                    return;
                }

                mapView.removePolyline(key);
            }
        });
    }

    @ReactMethod
    public void removePolylines(final int reactTag, final ReadableArray keys) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "removePolyline: mapView is null.");
                    return;
                }

                for (int i = 0; i < keys.size(); i++) {
                    mapView.removePolyline(keys.getString(i));
                }
            }
        });
    }

    @ReactMethod
    public void removeAllPolylines(final int reactTag, final Callback callback) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "removePolyline: mapView is null.");
                    return;
                }

                mapView.removeAllPolylines();
                callback.invoke(true);
            }
        });
    }

    private Polygon addPolygon(RCTAMapView mapView, ReadableMap config) {
        int strokeColor = Utils.parseColor(config.getString("strokeColor"));
        int lineWidth = config.getInt("lineWidth");
        int fillColor = Utils.parseColor(config.getString("fillColor"));

        ReadableArray coordinates = config.getArray("coordinates");
        List<LatLng> latLngs = new ArrayList<>();
        for (int i = 0; i < coordinates.size(); i++) {
            ReadableMap point = coordinates.getMap(i);
            LatLng latLng;
            if (point.hasKey("latitude"))
                latLng = new LatLng(point.getDouble("latitude"), point.getDouble("longitude"));
            else
                latLng = new LatLng(point.getDouble("lat"), point.getDouble("lng"));
            latLngs.add(latLng);
        }

        PolygonOptions polygonOptions = new PolygonOptions();
        polygonOptions.strokeWidth(lineWidth);
        polygonOptions.fillColor(fillColor);
        polygonOptions.strokeColor(strokeColor);
        polygonOptions.addAll(latLngs);

        Polygon polygon = mapView.addPolygon(polygonOptions,config);
        return polygon;
    }

    @ReactMethod
    public void addPolygon(final int reactTag, final ReadableMap config, final Callback callback) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "addPolygon: mapView is null.");
                    return;
                }

                Polygon polygon = addPolygon(mapView, config);
                callback.invoke(polygon.getId());
            }
        });
    }

    @ReactMethod
    public void addPolygons(final int reactTag, final ReadableArray configs, final Callback callback) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "addPolygons: mapView is null.");
                    return;
                }

                WritableArray polygons = new WritableNativeArray();
                for (int i = 0; i < configs.size(); i++) {
                    Polygon polygon = addPolygon(mapView, configs.getMap(i));
                    polygons.pushString(polygon.getId());
                }
                callback.invoke(polygons);
            }
        });
    }

    @ReactMethod
    public void removePolygon(final int reactTag, final String key) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "removePolygon: mapView is null.");
                    return;
                }

                mapView.removePolygon(key);
            }
        });
    }

    @ReactMethod
    public void removePolygons(final int reactTag, final ReadableArray keys) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "removePolygons: mapView is null.");
                    return;
                }

                for (int i = 0; i < keys.size(); i++) {
                    mapView.removePolygon(keys.getString(i));
                }
            }
        });
    }

    @ReactMethod
    public void removeAllPolygons(final int reactTag, final Callback callback) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "removePolygons: mapView is null.");
                    return;
                }

                mapView.removeAllPolygons();
                callback.invoke(true);
            }
        });
    }

    @ReactMethod
    public void userLocation(final int reactTag, final Callback callback) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "userLocation: mapView is null.");
                    return;
                }
            }
        });
    }

    @ReactMethod
    public void setMapType(final int reactTag, final int mapType) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "setMapType: mapView is null.");
                    return;
                }

                mapView.setMapType(mapType);
            }
        });
    }

    @ReactMethod
    public void movieToUserLocation(final int reactTag) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));

                if (mapView == null) {
                    Log.d("AMAP", "movieToUserLocation: mapView is null.");
                    return;
                }

                mapView.moveToMyLocation();
            }
        });
    }

    @ReactMethod
    public void reGoecodeSearch(ReadableMap config) {
        if (config == null)
            return;
        GeocodeSearch geocodeSearch = new GeocodeSearch(mContext);
        geocodeSearch.setOnGeocodeSearchListener(this);
        ReadableMap coordinate = config.getMap("coordinate");
        LatLonPoint latLonPoint = new LatLonPoint(coordinate.getDouble("latitude"), coordinate.getDouble("longitude"));
        RegeocodeQuery query = new RegeocodeQuery(latLonPoint, (float) config.getDouble("radius"), GeocodeSearch.GPS);
        geocodeSearch.getFromLocationAsyn(query);
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
        WritableMap data = Arguments.createMap();
        if (i == 1000) {
            RegeocodeAddress address = regeocodeResult.getRegeocodeAddress();
            data.putString("district", address.getDistrict());
            data.putString("township", address.getTownship());
            data.putString("city", address.getCity());
        } else {
            WritableMap error = Arguments.createMap();
            error.putString("code", String.valueOf(i));
            data.putMap("error", error);
        }

        mContext
                .getJSModule(RCTNativeAppEventEmitter.class)
                .emit(onReGeocodeSearchDoneEvent, data);

    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

    }

    @ReactMethod
    public void printCurrentMapShot(final int reactTag) {
        RCTAMapView rctaMapView = (RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag);
        rctaMapView.getAMAP().getMapScreenShot(new AMap.OnMapScreenShotListener() {
            @Override
            public void onMapScreenShot(final Bitmap bitmap) {
                PrintHelper printHelper = new PrintHelper(mContext.getCurrentActivity());
                printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
                printHelper.printBitmap("print_map", bitmap);

            }

            @Override
            public void onMapScreenShot(Bitmap bitmap, int i) {

            }
        });
    }

    @ReactMethod
    public void setBearing(final int reactTag, final float bearing){
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RCTAMapView mapView = (RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag);
                mapView.getAMAP().animateCamera(CameraUpdateFactory.changeBearing(bearing));
            }
        });
    }

    @ReactMethod
    public void setTilt(final int reactTag, final float tilt){
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RCTAMapView mapView = (RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag);
                mapView.getAMAP().animateCamera(CameraUpdateFactory.changeTilt(tilt));
            }
        });
    }

}
