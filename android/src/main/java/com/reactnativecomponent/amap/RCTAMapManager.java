package com.reactnativecomponent.amap;


import android.util.Log;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.Polyline;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.reactnativecomponent.amap.util.ShapeHelper;
import java.util.Map;


public class RCTAMapManager extends ViewGroupManager<RCTAMapView> {
    //    public static final LatLng SHANGHAI = new LatLng(31.238068, 121.501654);// 上海市经纬度

    @Override
    public String getName() {
        return "RCTAMapView";
    }


    @Override
    protected RCTAMapView createViewInstance(ThemedReactContext reactContext) {
        RCTAMapView mapView = new RCTAMapView(reactContext);
        Log.d("AMAP", "createViewInstance: new inst");
        return mapView;
    }

    // 是否显示用户当前所在位置的蓝点
    @ReactProp(name="showsUserLocation")
    public void setShowsUserLocation(RCTAMapView mapView, final boolean showsUserLocation) {
        if (mapView.isReady()){
            mapView.setUserLocationMarker(showsUserLocation);
        }else {
            mapView.setShowUserLocation(showsUserLocation);
        }
    }

    // 设置地图类型
    @ReactProp(name="mapType")
    public void setMapType(RCTAMapView mapView, final int mapType) {
        mapView.setMapType(mapType);
    }

    // 设置地图视图类型 0 是 MapView 1 是 TextureMapView，默认是 0
    @ReactProp(name="mapViewType")
    public void setMapViewType(RCTAMapView mapView, final int mapViewType) {
        mapView.setMapViewType(mapViewType);
    }

    // 设置缩放等级
    @ReactProp(name="zoomLevel")
    public void setZoomLevel(RCTAMapView mapView, final float zoomLevel) {
        mapView.setZoomLevel(zoomLevel);
    }

    // 设置中心点
    @ReactProp(name="centerCoordinate")
    public void setCenterCoordinate(RCTAMapView mapView, final ReadableMap centerCoordinate) {
        Double lat = centerCoordinate.getDouble("latitude");
        Double lng = centerCoordinate.getDouble("longitude");
        if ((lat >= -90 && lat <= 90) && (lng >= -180 && lng <= 180))
            mapView.setCenterLocation(lat, lng);
    }

    // frame，暂时没用，ios 那边有用，兼容一下
    @ReactProp(name="frame")
    public void setFrame(RCTAMapView mapView, ReadableMap frame) {
    }

    // customMapStylePath
    @ReactProp(name="customMapStyleFileName")
    public void setCustomMapStyleFileName(RCTAMapView mapView, String customMapStyleFileName) {
        mapView.setCustomMapStylePath(customMapStyleFileName);
    }

    // 设置需要特殊 infoWindow 处理的 class，会通过反射来使用
    @ReactProp(name="infoWindowClass")
    public void setInfoWindowClass(RCTAMapView mapView, final String className) {
        mapView.setInfoWindowClass(className);
    }

    // 是否允许旋转地图
    @ReactProp(name="rotateGestures")
    public void setRotateGestures(RCTAMapView mapView, final boolean rotateGestures) {
        mapView.setRotateGestures(rotateGestures);
    }

    // 是否允许倾斜地图
    @ReactProp(name="tiltGestures")
    public void setTiltGestures(RCTAMapView mapView, final boolean tiltGestures) {
        mapView.setTiltGestures(tiltGestures);
    }

    // 是否显示比例尺
    @ReactProp(name="scaleControls")
    public void setScaleControls(RCTAMapView mapView, final boolean scaleControls) {
        mapView.setScaleControls(scaleControls);
    }

    @ReactProp(name="circles")
    public void setCircles(RCTAMapView mapView, ReadableArray circles){
        if (circles==null)
            return;
        ShapeHelper<Circle> circleHelper = mapView.getCircleHelper();
        if (mapView.isReady()){
            circleHelper.drawShapes(circles);
        }else {
            circleHelper.setDataFromJS(circles);
        }
    }

    @ReactProp(name="polygons")
    public void setPolygons(RCTAMapView mapView, ReadableArray polygons){
        if (polygons==null)
            return;
        ShapeHelper<Polygon> polygonHelper = mapView.getPolygonHelper();
        if (mapView.isReady()){
            polygonHelper.drawShapes(polygons);
        }else {
           polygonHelper.setDataFromJS(polygons);
        }
    }

    @ReactProp(name="polylines")
    public void setPolylines(RCTAMapView mapView, ReadableArray polylines){
        if (polylines==null)
            return;
        ShapeHelper<Polyline> polylineHelper = mapView.getPolylineHelper();
        if (mapView.isReady()){
            polylineHelper.drawShapes(polylines);
        }else {
           polylineHelper.setDataFromJS(polylines);
        }
    }


    @ReactProp(name="markers")
    public void setMarkers(RCTAMapView mapView, ReadableArray markers){
        if (markers==null)
            return;
        ShapeHelper<Marker> shapeHelper = mapView.getMarkerHelper();
        if (mapView.isReady()){
            shapeHelper.drawShapes(markers);
        }else {
            shapeHelper.setDataFromJS(markers);
        }
    }

    @ReactProp(name="region")
    public void setRegion(RCTAMapView mapView,ReadableMap region){
        if (region==null)
            return;
        if (mapView.isReady()){
            if (region.hasKey("radius")){
                mapView.setRegion(region);
            }else {
                mapView.setRegionByLatLngs(region);
            }
        }else {
            mapView.setMapRegion(region);
        }
    }

    @ReactProp(name="bearing")
    public void setBearing(RCTAMapView mapView, float bearing){
        if (mapView.isReady()){
            mapView.getAMAP().animateCamera(CameraUpdateFactory.changeBearing(bearing));
        }else {
            mapView.setBearing(bearing);
        }
    }

    @ReactProp(name="tilt")
    public void setTilt(RCTAMapView mapView, float tilt){
        if (mapView.isReady()){
            mapView.getAMAP().animateCamera(CameraUpdateFactory.changeTilt(tilt));
        }else {
            mapView.setTilt(tilt);
        }
    }

    @Override
    protected void addEventEmitters(
            final ThemedReactContext reactContext,
            final RCTAMapView view) {
    }


    @Override
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.<String, Object>builder()
                .put("onDidMoveByUser", MapBuilder.of("registrationName", "onDidMoveByUser"))//registrationName 后的名字,RN中方法也要是这个名字否则不执行
                .put("onMapZoomChange", MapBuilder.of("registrationName", "onMapZoomChange"))
                .put("onSingleTapped", MapBuilder.of("registrationName", "onSingleTapped"))
                .put("onLongTapped", MapBuilder.of("registrationName", "onLongTapped"))
                .put("onAnnotationDragChange", MapBuilder.of("registrationName", "onAnnotationDragChange"))
                .put("onAttachedToWindow", MapBuilder.of("registrationName", "onAttachedToWindow"))
                .put("onAnnotationClick", MapBuilder.of("registrationName", "onAnnotationClick"))
                .put("onInfoWindowClick", MapBuilder.of("registrationName", "onInfoWindowClick"))
                .build();
    }

}
