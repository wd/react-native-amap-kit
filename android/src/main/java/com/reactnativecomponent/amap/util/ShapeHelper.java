package com.reactnativecomponent.amap.util;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.reactnativecomponent.amap.RCTAMapView;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
public class ShapeHelper<T> {
    private static final String UID = "uid";
    public static final int CIRCLE = 1;
    public static final int POLYLINE = 2;
    public static final int POLYGON = 3;
    public static final int MARKER = 4;

    private ReadableArray dataFromJS = null;
    private HashMap<String,ShapeHolder<T>> drawedShapes = new HashMap<>();
    private HashSet<String> drawedUIDS = new HashSet<>();
    private RCTAMapView mapView = null;
    private int type = 0;

    public ShapeHelper(RCTAMapView mapView, int shapeType){
        this.mapView = mapView;
        this.type = shapeType;
    }

    public int getsize(){
        if (dataFromJS == null)
            return 0;
        return dataFromJS.size();
    }

    public void addShape(String key ,T t, ReadableMap config){
        if (drawedShapes == null)
            return;
        String uid = null;
        if (config.hasKey(UID)){
            uid = config.getString(UID);
            drawedUIDS.add(uid);
        }
        ShapeHolder<T> holder = new ShapeHolder(t,config,uid);
        drawedShapes.put(key, holder);
    }

    public void removeShape(String key){
        switch (type){
            case CIRCLE:
                mapView.removeCircle(key);
                break;
            case POLYLINE:
                mapView.removePolyline(key);
                break;
            case POLYGON:
                mapView.removePolygon(key);
                break;
            case MARKER:
                mapView.removeMarker(key);
                break;
            default:break;
        }
    }

    public HashSet<String> getUidsFromJS(ReadableArray dataFromJS){
        HashSet<String> UIDsfromJS = new HashSet<>();
        for (int i = 0; i < dataFromJS.size(); i++) {
            ReadableMap map = dataFromJS.getMap(i);
            if (map.hasKey(UID)){
                UIDsfromJS.add(map.getString(UID));
            }
        }
        return UIDsfromJS;
    }

    public void removeShapes(ReadableArray dataFromJS){
        HashSet<String> UIDsfromJS = getUidsFromJS(dataFromJS);

        Iterator<String> iterator = drawedShapes.keySet().iterator();
        while (iterator.hasNext()){
            String key = iterator.next();
            ShapeHolder<T> next = drawedShapes.get(key);
            if (next.uid == null){
                removeShape(key);
                iterator.remove();
            }else {
                if (!UIDsfromJS.contains(next.uid)){
                    removeShape(key);
                    iterator.remove();
                    drawedUIDS.remove(next.uid);
                }
            }
        }
    }

    public void removeAllShapes(){
        Iterator<String> iterator = drawedShapes.keySet().iterator();
        while (iterator.hasNext()){
            String key = iterator.next();
            removeShape(key);
        }
        drawedShapes.clear();
        drawedUIDS.clear();
    }

    private void addShapeView(ReadableMap config){
        switch (type){
            case CIRCLE:
                mapView.addCircle(config);
                break;
            case POLYLINE:
                mapView.addPolyline(config);
                break;
            case POLYGON:
                mapView.addPolygon(config);
                break;
            case MARKER:
                mapView.addMarkers(config);
                break;
            default:break;
        }
    }

    public void addShapes(ReadableArray dataFromJS){
        for (int i = 0; i < dataFromJS.size(); i++) {
            if (dataFromJS.getMap(i).hasKey(UID)){
                String uid = dataFromJS.getMap(i).getString(UID);
                if (!drawedUIDS.contains(uid)){
                    addShapeView(dataFromJS.getMap(i));
                }
            }else {
                addShapeView(dataFromJS.getMap(i));
            }
        }
    }

    public void drawShapes(ReadableArray dataFromJS){
        this.dataFromJS = dataFromJS;
        removeShapes(dataFromJS);
        addShapes(dataFromJS);
    }

    public ReadableMap getShapeConfig(String key){
        return drawedShapes.get(key).drawedMarkerConfig;
    }

    public T getShape(String key){
        return drawedShapes.get(key).shape;
    }

    public ReadableArray getDataFromJS() {
        return dataFromJS;
    }

    public void setDataFromJS(ReadableArray dataFromJS) {
        this.dataFromJS = dataFromJS;
    }

    public HashMap<String, ShapeHolder<T>> getDrawedShapes() {
        return drawedShapes;
    }

    public static class ShapeHolder<T>{
        public T shape;
        public ReadableMap drawedMarkerConfig;
        public String uid;

        ShapeHolder(T t, ReadableMap config, String uid){
            this.shape = t;
            this.drawedMarkerConfig = config;
            this.uid = uid;
        }
    }

}
