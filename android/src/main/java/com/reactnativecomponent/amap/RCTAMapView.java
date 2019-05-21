package com.reactnativecomponent.amap;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.TextureMapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.PolygonOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.reactnativecomponent.amap.util.ShapeHelper;
import com.reactnativecomponent.amap.util.SensorEventHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import static com.amap.api.maps.AMapOptions.LOGO_POSITION_BOTTOM_RIGHT;
import static com.amap.api.maps.AMapOptions.ZOOM_POSITION_RIGHT_CENTER;


public class RCTAMapView extends FrameLayout implements AMapLocationListener,
        AMap.OnCameraChangeListener,
        AMap.OnMarkerDragListener, AMap.OnMarkerClickListener,
        AMap.OnMapLongClickListener,
        AMap.OnMapClickListener,
        AMap.InfoWindowAdapter, AMap.OnInfoWindowClickListener {
    private static final String UID = "uid";
    private TextureMapView TEXTUREMAPVIEW;
    private MapView MAPVIEW;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;
    private SensorEventHelper mSensorHelper; // 给定位点增加方向
    private AMap AMAP;
    private UiSettings mapUiSettings;

    private ThemedReactContext CONTEXT;
    private ViewGroup.LayoutParams PARAM;

    // 一些默认设置
    private boolean zoomControls = false;
    private boolean zoomGestures = true;
    private boolean scaleControls = false;
    private boolean compassEnable = false;
    private boolean tiltGestures = true;
    private boolean rotateGestures = true;

    private ImageView CenterView;

    private ShapeHelper<Marker> markerHelper = new ShapeHelper<Marker>(this,ShapeHelper.MARKER);
    private ShapeHelper<Circle> circleHelper = new ShapeHelper<Circle>(this,ShapeHelper.CIRCLE);
    private ShapeHelper<Polyline> polylineHelper = new ShapeHelper<Polyline>(this,ShapeHelper.POLYLINE);
    private ShapeHelper<Polygon> polygonHelper = new ShapeHelper<Polygon>(this, ShapeHelper.POLYGON);
    private ReadableMap region;
    private boolean isReady;

    private long startTime;
    private LatLng myLatLng;
    public boolean autoCenterCameraWhenLocationChanged = true;
    private CameraPosition lastCameraPosition;

    // 一些可以通过 props 配置的项目
    private int mapType;
    private float zoomLevel;
    private LatLng centerLocation;
    private String customMapStylePath;
    private int mapViewType = 1;
    private RCTInfoWindow infoWindowObj;
    private float bearing = -1;
    private float tilt = -1;
    private boolean showUserLocation = true;
    private boolean hasLoadMapView = true;

    public RCTAMapView(ThemedReactContext context) {
        super(context);
        this.CONTEXT = context;
        CenterView = new ImageView(context);
        PARAM = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        init();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    /**
     * Activity onResume后调用view的onAttachedToWindow
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.d("AMAP", "onAttachedToWindow");

        if (!hasLoadMapView){
            init();
        }
        isReady = true;
        draw();
        makeBearing();
        makeTilt();
        setUserLocationMarker(showUserLocation);
        ReactContext reactContext = (ReactContext) getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                "onAttachedToWindow",
                null);
    }

    public void setUserLocationMarker(boolean showUserLocation) {
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.interval(2000);
        myLocationStyle.showMyLocation(showUserLocation);
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.gps_point));
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW_NO_CENTER);
        myLocationStyle.strokeColor(Color.TRANSPARENT);
        myLocationStyle.radiusFillColor(Color.TRANSPARENT);
        AMAP.setMyLocationStyle(myLocationStyle);
        AMAP.setMyLocationEnabled(true);
    }

    private void makeTilt() {
        if (tilt == -1)
            return;
        AMAP.animateCamera(CameraUpdateFactory.changeTilt(tilt));
    }

    private void makeBearing() {
        if (bearing == -1)
            return;
        AMAP.animateCamera(CameraUpdateFactory.changeBearing(bearing));
    }

    private void draw() {
        if (circleHelper.getsize()!=0){
            circleHelper.addShapes(circleHelper.getDataFromJS());
        }
        if (polygonHelper.getsize()!=0){
            polygonHelper.addShapes(polygonHelper.getDataFromJS());
        }
        if (polylineHelper.getsize()!=0){
            polylineHelper.addShapes(polylineHelper.getDataFromJS());
        }
        if (markerHelper.getsize()!=0){
            markerHelper.addShapes(markerHelper.getDataFromJS());
        }
        if (region!=null){
            if (region.hasKey("radius")){
                setRegion(region);
            }else {
                setRegionByLatLngs(region);
            }
        }
    }

    /**
     * 初始化控件,定位位置
     */
    private void init() {
        mSensorHelper = new SensorEventHelper(CONTEXT);
        if (mSensorHelper != null) {
            mSensorHelper.registerSensorListener();
        }
        if (this.mapViewType == 1) {
            TEXTUREMAPVIEW = new TextureMapView(CONTEXT);
            TEXTUREMAPVIEW.setLayoutParams(PARAM);
            TEXTUREMAPVIEW.onCreate(getSavedState());
            AMAP = TEXTUREMAPVIEW.getMap();
            setMapOptions();
            this.addView(TEXTUREMAPVIEW);
        } else {
            MAPVIEW = new MapView(CONTEXT);
            MAPVIEW.setLayoutParams(PARAM);
            MAPVIEW.onCreate(getSavedState());
            AMAP = MAPVIEW.getMap();
            setMapOptions();
            this.addView(MAPVIEW);
        }
    }

    private Bundle getSavedState(){
        if (CONTEXT != null && CONTEXT.getCurrentActivity() != null){
            return CONTEXT.getCurrentActivity().getIntent().getExtras();
        }
        return new Bundle();
    }


    /**
     * 设置一些默认的amap的属性
     */
    private void setMapOptions() {
        AMAP.setMapType(AMap.MAP_TYPE_NORMAL);// 矢量地图模式
        mapUiSettings = AMAP.getUiSettings();//实例化UiSettings类
        mapUiSettings.setZoomControlsEnabled(zoomControls);//是否显示缩放按钮
        mapUiSettings.setZoomPosition(ZOOM_POSITION_RIGHT_CENTER);//缩放按钮位置  右边界中部：ZOOM_POSITION_RIGHT_CENTER 右下：ZOOM_POSITION_RIGHT_BUTTOM。
        mapUiSettings.setLogoPosition(LOGO_POSITION_BOTTOM_RIGHT);//Logo的位置 左下：LOGO_POSITION_BOTTOM_LEFT 底部居中：LOGO_POSITION_BOTTOM_CENTER 右下：LOGO_POSITION_BOTTOM_RIGHT
        mapUiSettings.setCompassEnabled(compassEnable);//是否显示 指南针
        mapUiSettings.setZoomGesturesEnabled(zoomGestures);//是否支持手势缩放
        mapUiSettings.setScaleControlsEnabled(scaleControls);//是否显示比例尺
        mapUiSettings.setTiltGesturesEnabled(tiltGestures);//是否允许倾斜
        mapUiSettings.setRotateGesturesEnabled(rotateGestures); // 是否允许旋转

        mapUiSettings.setMyLocationButtonEnabled(false);// 设置默认定位按钮是否显示
        AMAP.setMyLocationEnabled(false);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false

        AMAP.setOnCameraChangeListener(this);// 对 amap 添加移动地图事件监听器
        AMAP.setOnMarkerDragListener(this); // 对 marker 移动事件监听
        AMAP.setOnMapLongClickListener(this); // 对 map 长按事件监听
        AMAP.setOnMapClickListener(this); // 对 map 点击事件监听

        AMAP.setOnMarkerClickListener(this);
        AMAP.setOnInfoWindowClickListener(this);

        // 一些初始化的时候设定的数据，需要在此完成，因为设定的时候还没有初始化地图
        setCustomMapStylePath();
        setZoomLevel();
        setMapType();
        setCenterLocation();

    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        Log.d("AMAP", "on marker darg start");
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        Log.d("AMAP", "on marker drag");
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        Log.d("AMAP", "onAnnotationDragChange");

        WritableMap dataMap = Arguments.createMap();
        dataMap.putString("key", marker.getId());

        WritableMap position = Arguments.createMap();
        position.putDouble("latitude", marker.getPosition().latitude);
        position.putDouble("longitude", marker.getPosition().longitude);
        dataMap.putMap("coordinate", position);

        CONTEXT.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                "onAnnotationDragChange",
                dataMap);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        WritableMap dataMap = Arguments.createMap();
        dataMap.merge(markerHelper.getShapeConfig(marker.getId()));
        CONTEXT.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                "onAnnotationClick",
                dataMap);
        return true;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Log.d("AMAP", "infoWindow click : " + marker.getId());
        WritableMap dataMap = Arguments.createMap();
        dataMap.putString("key", marker.getId());
        CONTEXT.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                "onInfoWindowClick",
                dataMap);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Log.d("AMAP", "onLongTapped");

        WritableMap dataMap = Arguments.createMap();
        WritableMap coordinate = Arguments.createMap();

        coordinate.putDouble("latitude", latLng.latitude);
        coordinate.putDouble("longitude", latLng.longitude);

        dataMap.putMap("coordinate", coordinate);

        CONTEXT.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                "onLongTapped",
                dataMap);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.d("AMAP", "onSingleTapped");

        WritableMap dataMap = Arguments.createMap();
        WritableMap coordinate = Arguments.createMap();

        coordinate.putDouble("latitude", latLng.latitude);
        coordinate.putDouble("longitude", latLng.longitude);

        dataMap.putMap("coordinate", coordinate);

        CONTEXT.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                "onSingleTapped",
                dataMap);
    }

    /**
     * 调用函数moveCamera来改变可视区域
     */
    public void changeCamera(CameraUpdate update) {
        AMAP.moveCamera(update);
    }

    public void animateCamera(CameraUpdate update) {
        AMAP.stopAnimation();
        AMAP.animateCamera(update);
    }

    /**
     * 获得图片资源ID
     *
     * @return
     */
    private int getImageId(String fileName) {
        int drawableId = CONTEXT.getResources().getIdentifier(fileName, "drawable", CONTEXT.getPackageName());
        if (drawableId == 0) {
            drawableId = CONTEXT.getResources().getIdentifier("splash", "drawable", CONTEXT.getPackageName());
        }

        return drawableId;
    }

    /**
     * 根据动画调用函数animateCamera来改变可视区域
     */
    private void animateCamera(CameraUpdate update, AMap.CancelableCallback callback) {

        AMAP.animateCamera(update, 1000, callback);

    }

    @Override
    protected Parcelable onSaveInstanceState() {
        if (MAPVIEW != null) {
            MAPVIEW.onSaveInstanceState(getSavedState());
        } else {
            TEXTUREMAPVIEW.onSaveInstanceState(getSavedState());
        }
        return super.onSaveInstanceState();
    }

    @Override
    protected void onDetachedFromWindow() {
        Log.d("AMAP", "onDetachedFromWindow");
        if(MAPVIEW != null) {
            this.removeView(MAPVIEW);
            MAPVIEW.onDestroy();
        } else {
            this.removeView(TEXTUREMAPVIEW);
            TEXTUREMAPVIEW.onDestroy();
        }
        if (mlocationClient != null){
            mlocationClient.onDestroy();
            mlocationClient = null;
        }
        hasLoadMapView = false;
        super.onDetachedFromWindow();
    }

    /**
     * 对应onResume、对应onPause
     *
     * @param hasWindowFocus
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {

        super.onWindowFocusChanged(hasWindowFocus);

        if (hasWindowFocus) {
//            对应onResume
            if(MAPVIEW != null)
                MAPVIEW.onResume();
            else
                TEXTUREMAPVIEW.onResume();
        } else {
            //对应onPause
            if(MAPVIEW != null)
                MAPVIEW.onPause();
            else
                TEXTUREMAPVIEW.onPause();

        }

    }

    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (amapLocation != null && amapLocation.getErrorCode() == 0) {
            // 有返回值就认为定位成功，不管是否有改变，都绘制定位点
            myLatLng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());

            if(autoCenterCameraWhenLocationChanged) {
                //移动镜头定位到点location
                CameraUpdate update = CameraUpdateFactory.newLatLng(myLatLng);
                animateCamera(update);
            }
        }
    }

    /**
     * 获得当前控件中心点坐标
     */
    public LatLng getCenterLocation() {
        LatLng latlng = AMAP.getCameraPosition().target;
        return latlng;
    }

    /**
     * 设置并移动到中心点坐标
     */
    public void setCenterLocation(double latitude, double longitude) {
        LatLng latlng = new LatLng(latitude, longitude);
        this.centerLocation = latlng;
        setCenterLocation();
    }

    private void setCenterLocation() {
        if(AMAP != null && this.centerLocation != null)
            animateCamera(CameraUpdateFactory.newLatLng(this.centerLocation));
    }
    /**
     * 定位到设备定位位置
     */
    public void startLocation(Boolean isOnceLocation) {
        startTime = System.currentTimeMillis();
        Log.i("AMAP", "start get my location, startTime:" + startTime);
        if (mlocationClient == null) {
            Log.i("AMAP", "mlocationClient = null");
            mlocationClient = new AMapLocationClient(CONTEXT);
            //设置定位监听
            mlocationClient.setLocationListener(this);

            mLocationOption = new AMapLocationClientOption();
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            mLocationOption.setOnceLocation(isOnceLocation);
//            mLocationOption.setOnceLocationLatest(true);
            mLocationOption.setLocationCacheEnable(true);//定位缓存策略
            //mLocationOption.setInterval(2000); // ms

            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);

            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
        }
        mlocationClient.startLocation();
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    //@Override
    public long getInfoWindowUpdateTime() {
        return 5 * 60 * 1000; // 5min
        //return 1000;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return this.infoWindowObj.getInfoWindow(marker, markerHelper.getShapeConfig(marker.getId()));
    }

    public void showInfoWindow(String key) {
        if(markerHelper.getDrawedShapes().containsKey(key)) {
            Marker marker = markerHelper.getShape(key);
            marker.showInfoWindow();
        }
    }

    public void hideInfoWindow(String key) {
        if(markerHelper.getDrawedShapes().containsKey(key)) {
            Marker marker = markerHelper.getShape(key);
            marker.hideInfoWindow();
        }
    }

    public void showAnnotation(String key) {
        if(markerHelper.getDrawedShapes().containsKey(key)) {
            Marker marker = markerHelper.getShape(key);
            marker.setVisible(true);
        }
    }

    public void hideAnnotation(String key) {
        if(markerHelper.getDrawedShapes().containsKey(key)) {
            Marker marker = markerHelper.getShape(key);
            marker.setVisible(false);
        }
    }


    private Marker addMarker(LatLng latLng, BitmapDescriptor bitmapDescriptor) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.icon(bitmapDescriptor);
        return addMarker(markerOptions);
    }

    public Marker addMarker(MarkerOptions markerOptions, String imgName, ReadableMap config) {
        float offsetX = 0.5f;
        float offsetY = 0.5f;
        if(config.hasKey("customView")){
            try {
                Class customView = Class.forName(config.getString("customView"));
                Constructor constructor = customView.getConstructor(Context.class,ReadableMap.class);
                BaseCustomView view = (BaseCustomView) constructor.newInstance(CONTEXT,config.hasKey("customViewProps")?config.getMap("customViewProps"):null);
                markerOptions.icon(BitmapDescriptorFactory.fromView(view));
                HashMap<String,Float> map = view.getOffset();
                if(map != null){
                    if (map.containsKey("offsetX"))
                        offsetX = map.get("offsetX");
                    if (map.containsKey("offsetY"))
                        offsetY = map.get("offsetY");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else {
            int resId = getImageId(imgName.toLowerCase());
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(resId);
            markerOptions.icon(bitmapDescriptor);
        }

        if (config.hasKey("offset")) {
            offsetX = (float) config.getMap("offset").getDouble("x");
            offsetY = (float) config.getMap("offset").getDouble("y");
        }
        markerOptions.anchor(offsetX,offsetY);

        return addMarker(markerOptions, config);
    }

    public Marker addMarker(MarkerOptions markerOptions) {
        ReadableMap config =  Arguments.createMap();
        return addMarker(markerOptions, config);
    }

    public Marker addMarker(MarkerOptions markerOptions, ReadableMap config) {
        Marker marker = AMAP.addMarker(markerOptions);
        boolean flat = true;
        if (config.hasKey("flat")){
            flat = config.getBoolean("flat");
        }
        marker.setFlat(flat);

        if (config.hasKey("isToTop")){
            if (config.getBoolean("isToTop"))
                marker.setToTop();
        }
        markerHelper.addShape(marker.getId(),marker,config);
        return marker;
    }

    public void removeMarker(String key) {
        Marker marker = markerHelper.getShape(key);
        marker.destroy();
        if(this.infoWindowObj != null)
            this.infoWindowObj.removeInfoWindow(key);
    }

    public void removeAllMarkers(){
        markerHelper.removeAllShapes();
    }

    public Circle addCircle(CircleOptions circleOptions,ReadableMap config) {
        Circle circle = AMAP.addCircle(circleOptions);
        circleHelper.addShape(circle.getId(),circle,config);
        return circle;
    }
    public void addCircle(final ReadableMap config) {
        int strokeColor = Utils.parseColor(config.getString("strokeColor"));
        int fillColor = Utils.parseColor(config.getString("fillColor"));
        int lineWidth = config.getInt("lineWidth");
        double radius = config.getDouble("radius");
        ReadableMap coordinate = config.getMap("coordinate");
        LatLng latLng;
        if(coordinate.hasKey("latitude"))
            latLng = new LatLng(coordinate.getDouble("latitude"), coordinate.getDouble("longitude"));
        else
            latLng = new LatLng(coordinate.getDouble("lat"), coordinate.getDouble("lng"));

        CircleOptions circleOption = new CircleOptions();
        circleOption.strokeWidth(lineWidth);
        circleOption.fillColor(fillColor);
        circleOption.strokeColor(strokeColor);
        circleOption.radius(radius);
        circleOption.center(latLng);
        addCircle(circleOption,config);
    }
    public void addPolygon(final ReadableMap config) {
        int strokeColor = Utils.parseColor(config.getString("strokeColor"));
        int lineWidth = config.getInt("lineWidth");
        int fillColor = Utils.parseColor(config.getString("fillColor"));

        ReadableArray coordinates = config.getArray("coordinates");
        List<LatLng> latLngs = new ArrayList<>();
        for(int i=0; i< coordinates.size(); i++) {
            ReadableMap point = coordinates.getMap(i);
            LatLng latLng;
            if(point.hasKey("latitude"))
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
        addPolygon(polygonOptions,config);
    }
    public void addPolyline(final ReadableMap config) {
        int strokeColor = Utils.parseColor(config.getString("strokeColor"));
        int lineWidth = config.getInt("lineWidth");

        ReadableArray coordinates = config.getArray("coordinates");
        List<LatLng> latLngs = new ArrayList<>();
        for(int i=0; i< coordinates.size(); i++) {
            ReadableMap point = coordinates.getMap(i);
            LatLng latLng;
            if(point.hasKey("latitude"))
                latLng = new LatLng(point.getDouble("latitude"), point.getDouble("longitude"));
            else
                latLng = new LatLng(point.getDouble("lat"), point.getDouble("lng"));
            latLngs.add(latLng);
        }
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.color(strokeColor);
        polylineOptions.width(lineWidth);
        polylineOptions.addAll(latLngs);
        addPolyline(polylineOptions,config);
    }
    public void addMarkers(final ReadableMap config) {
        ReadableMap point = config.getMap("coordinate");
        String imgName = "";
        if (config.hasKey("imageName")){
            imgName = config.getString("imageName");
        }

        MarkerOptions markerOptions = new MarkerOptions();
        if (config.hasKey("title")) {
            String title = config.getString("title");
            if(!title.isEmpty())
                markerOptions.title(title);
        }

        if (config.hasKey("snippet")) {
            String snippet = config.getString("snippet");
            if(!snippet.isEmpty())
                markerOptions.snippet(snippet);
        }

        if(config.hasKey("angle")) {
            float angle = (float) config.getDouble("angle");
            markerOptions.rotateAngle(angle);
        }

        boolean draggable = false;
        if(config.hasKey("draggable")) {
            draggable = config.getBoolean("draggable");
        }
        markerOptions.draggable(draggable);

        boolean visible = true;
        if(config.hasKey("visible")) {
            visible = config.getBoolean("visible");
        }
        markerOptions.visible(visible);

        LatLng latLng;
        if(point.hasKey("latitude"))
            latLng = new LatLng(point.getDouble("latitude"), point.getDouble("longitude"));
        else
            latLng = new LatLng(point.getDouble("lat"), point.getDouble("lng"));
        markerOptions.position(latLng);
        addMarker(markerOptions, imgName, config);
    }

    public void removeCircle(String key) {
        if(circleHelper.getDrawedShapes().containsKey(key)) {
            Circle circle = circleHelper.getShape(key);
            circle.remove();
        }
    }

    public void removeAllCircles(){
        circleHelper.removeAllShapes();
    }

    public Polygon addPolygon(PolygonOptions polygonOptions, ReadableMap config) {
        Polygon polygon = AMAP.addPolygon(polygonOptions);
        polygonHelper.addShape(polygon.getId(),polygon,config);
        return polygon;
    }

    public void removePolygon(String key) {
        if(polygonHelper.getDrawedShapes().containsKey(key)) {
            Polygon polygon = polygonHelper.getShape(key);
            polygon.remove();
        }
    }

    public void removeAllPolygons(){
        polygonHelper.removeAllShapes();
    }

    public Polyline addPolyline(PolylineOptions polylineOptions, ReadableMap config) {
        Polyline polyline = AMAP.addPolyline(polylineOptions);
        polylineHelper.addShape(polyline.getId(),polyline,config);
        return polyline;
    }

    public void removePolyline(String key) {
        if(polylineHelper.getDrawedShapes().containsKey(key)) {
            Polyline polyline = polylineHelper.getShape(key);
            polyline.remove();
        }
    }

    public void removeAllPolylines(){
        polylineHelper.removeAllShapes();
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {


    }

    /**
     * 控制中心点动画 获取中心点坐标 查询周边
     *
     * @param cameraPosition
     */
    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        Log.i("AMAP", String.format("onCameraChangeFinish: %s", cameraPosition));
        LatLng latlng = cameraPosition.target;//获取屏幕中心点
        ReactContext reactContext = (ReactContext) getContext();

        if (lastCameraPosition != null) {
            LatLng lastLatLng = lastCameraPosition.target;
            if (lastLatLng.latitude != latlng.latitude || lastLatLng.longitude != latlng.longitude) {

                WritableMap eventMap = Arguments.createMap();
                WritableMap dataMap = Arguments.createMap();
                WritableMap centerCoordinateMap = Arguments.createMap();
                centerCoordinateMap.putDouble("latitude", latlng.latitude);
                centerCoordinateMap.putDouble("longitude", latlng.longitude);
                dataMap.putMap("centerCoordinate", centerCoordinateMap);
                eventMap.putMap("data", dataMap);
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        getId(),
                        "onDidMoveByUser",
                        eventMap);
            }

            if (lastCameraPosition.zoom != cameraPosition.zoom) {
                WritableMap dataMap = Arguments.createMap();
                dataMap.putDouble("zoomLevel", cameraPosition.zoom);
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        getId(),
                        "onMapZoomChange",
                        dataMap);
            }
        }
        lastCameraPosition = cameraPosition;
    }

    public void setMapViewType(int mapViewType){
        this.mapViewType = mapViewType;
        // 这个属性在运行时修改无效
    }

    public void setMapType(int mapType) {
        this.mapType = mapType;
        setMapType();
    }

    private void setMapType() {
        // 取值是 1-5 例如 AMAP.MAP_TYPE_BUS
        if(AMAP != null && this.mapType != 0)
            AMAP.setMapType(mapType);
    }

    public void moveToMyLocation() {
        boolean isOnce = true;
        startLocation(isOnce);
    }

    private String getAssetsPath(String fileName) {
        String filePath = null;
        try {
            filePath = fileName;
        }catch (Exception e){};
        return filePath;
    }

    public void setCustomMapStylePath(String customMapStyleFileName) {
        String filePath = null;
        InputStream inputStream = null;
        FileOutputStream out = null;

        try {
            inputStream = CONTEXT.getAssets().open(customMapStyleFileName);
            byte[] b = new byte[inputStream.available()];
            inputStream.read(b);
            filePath = CONTEXT.getFilesDir().getAbsolutePath() + "/" + customMapStyleFileName;
            File f = new File(filePath);
            if (!f.exists()) {
                // TODO: 要更新必须要重新起一个地图的名字，或许可以有更好的方式
                Log.d("AMAP", "Style not exist.");
                f.createNewFile();
                out = new FileOutputStream(f);
                out.write(b);
            } else {
                Log.d("AMAP", "Style file is there");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();

                if (out != null)
                    out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.customMapStylePath = filePath;

        setCustomMapStylePath();
    }

    private void setCustomMapStylePath() {
        if(AMAP != null && this.customMapStylePath != null) {
            AMAP.setCustomMapStylePath(this.customMapStylePath);
            AMAP.setMapCustomEnable(true);
        }
    }

    public void setInfoWindowClass(String className) {

        try {
            this.infoWindowObj = new RCTInfoWindow(CONTEXT, className);
            if(this.infoWindowObj != null)
                AMAP.setInfoWindowAdapter(this); //自定义的 infoWindow
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setScaleControls(boolean scaleControls) {
        if(scaleControls)
            this.scaleControls = scaleControls;
    }

    public void setTiltGestures(boolean tiltGestures) {
        if(!tiltGestures)
            this.tiltGestures = tiltGestures;
    }

    public void setRotateGestures(boolean rotateGestures) {
        if(!rotateGestures)
            this.rotateGestures = rotateGestures;
    }
    public boolean isReady(){
        return isReady;
    }

    public void setRegion(final ReadableMap region) {
        setRegionConfig(region);
    }

    public void setRegionConfig(ReadableMap region){
        ReadableMap center = region.getMap("center");
        double centerLat;
        double centerLng;
        if(center.hasKey("latitude")){
            centerLat = center.getDouble("latitude");
            centerLng = center.getDouble("longitude");
        }else {
            centerLat = center.getDouble("lat");
            centerLng = center.getDouble("lng");
        }


        double radius = region.getDouble("radius");

        boolean animate = true;
        if(region.hasKey("animate"))
            animate = region.getBoolean("animate");


        double distance = Math.sqrt(2*radius*radius);
        ReadableMap southwestGPS = HaversineAlgorithm.addDistanceToGPS(centerLat, centerLng, -distance, -distance);
        LatLng southwestLatLng = new LatLng(southwestGPS.getDouble("latitude"), southwestGPS.getDouble("longitude"));

        ReadableMap northeastGPS = HaversineAlgorithm.addDistanceToGPS(centerLat, centerLng, distance, distance);
        LatLng northeastLatLng = new LatLng(northeastGPS.getDouble("latitude"), northeastGPS.getDouble("longitude"));

        int padding2Px = 100;
        if (region.hasKey("padding")){
            padding2Px = Utils.dip2px(CONTEXT,(float) region.getDouble("padding"));
        }

        CameraUpdate update = CameraUpdateFactory.newLatLngBounds(new LatLngBounds(southwestLatLng, northeastLatLng), padding2Px); //100 是经验值。。

        Log.d("AMAP", String.format("setRegion input: %s, output: sw %s, ne %s", region, southwestGPS.toString(), northeastGPS.toString()));

        if(animate) {
            animateCamera(update);
        } else {
            changeCamera(update);
        }
    }

    public void setRegionByLatLngs(final ReadableMap region) {
        setRegionConfigByLatLngs(region);
    }
    public void setRegionConfigByLatLngs(ReadableMap region){
        ReadableArray coordinates = region.getArray("coordinates");
        boolean animate = true;
        if (region.hasKey("animate"))
            animate = region.getBoolean("animate");

        LatLngBounds.Builder builder = LatLngBounds.builder();
        for (int i = 0; i < coordinates.size(); i++) {
            ReadableMap coordinate = coordinates.getMap(i);
            LatLng latLng;
            if(coordinate.hasKey("latitude"))
                latLng = new LatLng(coordinate.getDouble("latitude"), coordinate.getDouble("longitude"));
            else
                latLng = new LatLng(coordinate.getDouble("lat"), coordinate.getDouble("lng"));

            builder.include(latLng);
        }

        int padding2Px = 100;
        if (region.hasKey("padding")){
            padding2Px = Utils.dip2px(CONTEXT,(float) region.getDouble("padding"));
        }

        CameraUpdate update = CameraUpdateFactory.newLatLngBounds(builder.build(), padding2Px);
        if (animate) {
            animateCamera(update);
        } else {
            changeCamera(update);
        }
    }

    public boolean isShowUserLocation() {
        return showUserLocation;
    }
    public void setShowUserLocation(boolean showUserLocation) {
        this.showUserLocation = showUserLocation;
    }


    public float getBearing() {
        return bearing;
    }

    public void setBearing(float bearing) {
        this.bearing = bearing;
    }

    public float getTilt() {
        return tilt;
    }

    public void setTilt(float tilt) {
        this.tilt = tilt;
    }

    public void setZoomLevel(float zoomLevel) {
        this.zoomLevel = zoomLevel;
        setZoomLevel();
    }

    private void setZoomLevel() {
        if(AMAP != null && zoomLevel != 0.0f) {
            CameraUpdate update = CameraUpdateFactory.zoomTo(zoomLevel);
            animateCamera(update);
        }
    }

    public float getMinZoomLevel() {return this.AMAP.getMinZoomLevel();}

    public float getMaxZoomLevel() {return this.AMAP.getMaxZoomLevel();}

    public double getZoomLevel() {return AMAP.getCameraPosition().zoom;}


    public ShapeHelper getMarkerHelper(){
        return markerHelper;
    }

    public ShapeHelper<Circle> getCircleHelper() {
        return circleHelper;
    }

    public ShapeHelper<Polyline> getPolylineHelper() {
        return polylineHelper;
    }

    public ShapeHelper<Polygon> getPolygonHelper() {
        return polygonHelper;
    }

    //region
    public void setMapRegion(ReadableMap region){
        this.region = region;
    }

    public AMap getAMAP(){
        return AMAP;
    }

}

