import React, {
    Component,
} from 'react'
import {
    requireNativeComponent,
    NativeModules,
    findNodeHandle,
    Platform,
    StyleProp,
    ViewStyle
} from 'react-native';

const AMapManager = Platform.OS == 'ios' ? NativeModules.AMap : NativeModules.AMapModule;

export interface CoordinateShort {
    lat: number;
    lng: number;
}

export interface CoordinateLong {
    latitude: number;
    longitude: number;
}

export type Coordinate = CoordinateLong | CoordinateShort

export interface Region {
    center: Coordinate;
    radius: number;
    animate: boolean;
}

export interface CustomViewProps {
    imgName: string;
    addrName: string;
    index: number;
    isSelected: boolean;
}

export interface Marker {
    coordinate: Coordinate;
    imageName?: string;
    customView?: string;
    customViewProps?: CustomViewProps;
    isToTop?: boolean;
    flat?: boolean;
    uid?: string;
}

export interface Circle {
    coordinate: Coordinate;
    radius: number;
    lineWidth: number;
    strokeColor: string;
    fillColor: string;
}

export interface Line {
    lineWidth: number;
    strokeColor: string;
    coordinates: Coordinate[];
}

export interface Polygon {
    lineWidth: number;
    strokeColor: string;
    fillColor: string;
    coordinates: Coordinate[];
}


function isFunction(fn: Function) {
    return typeof fn === 'function';
}

function safeCallback(callback: Function) {
    return isFunction(callback) ? callback : function() {};
}

export interface MoveByUserEvent {
    data: {
        centerCoordinate: CoordinateLong
    }
}

export interface TapEvent {
    nativeEvent: {
        coordinate: CoordinateLong
    }
}

export interface ZoomChangeEvent {
    nativeEvent: {
        zoomLevel: number;
    }
}

export interface AnnotationDragChangeEvent {
    nativeEvent: {
        key: string;
        coordinate: CoordinateLong;
    }
}

export interface AnnotationClickEvent {
    nativeEvent: {
        customViewProps: CustomViewProps
    }
}

export interface InfoWindowClickEvent {
    nativeEvent: {
        key: string
    }
}

export interface CommonProps {
    //          standart    satellite
    //  ios         0           1
    //  android     1           2
    style: StyleProp<ViewStyle>
    mapType?: 0|1|2; //地图样式
    showsUserLocation?: boolean; //是否显示中心点
    bearing?: number; //地图旋转角度
    tilt?: number; //地图倾斜角度
    // 设置地图视图类型 0 是 MapView 1 是 TextureMapView，默认是 0
    mapViewType?: 0|1;
    centerCoordinate?: Coordinate; //设置地图中心点
    customMapStyleFileName?: string; //设置地图样式
    zoomLevel?: number; //地图缩放比例
    onDidMoveByUser?: (event: MoveByUserEvent) => void; //挪动地图事件
    onSingleTapped?: (evnet: TapEvent) => void; //点击地u事件
    onLongTapped?: (evnet: TapEvent) => void; //长按地图事件
    onMapZoomChange?: (event: ZoomChangeEvent) => void; //地图缩放比例改变事件
    onAnnotationDragChange?: (event: AnnotationDragChangeEvent) => void; //Marker 被拖动事件
    onAnnotationClick?: (event: AnnotationClickEvent) => void; //Marker 点击事件
    circles?: Circle[]; //地图上面画圆形
    markers?: Marker[]; //地图上画点标记
    polygons?: Polygon[]; //地图上画多边形
    polylines?: Line[]; //地图上画折线
    region?: Region; //设置地图可视区域
}

export interface AndroidProps {
    scaleControls?: boolean; //是否显示缩放按钮
    tiltGestures?: boolean; //是否允许用户倾斜地图
    rotateGestures?: boolean; //是否允许用户旋转地图
    infoWindowClass?: boolean; //自定义 InfoWindow 样式的 class 名
    onAttachedToWindow?: () => void; //地图 ready 事件
    onInfoWindowClick?: (event: InfoWindowClickEvent) => void; // InfoWindow 点击的时候触发的事件
}

export interface SetLatLngZoomParams {
    coordinate:  Coordinate;
    animate: boolean;
    zoomLevel: number;
}

export type ReturnAKeyCallback = (key: string) => void;
export type ReturnKeysCallback = (keys: string[]) => void;
export type ReturnResultCallback = (result: boolean) => void;

export default class AMap extends Component<CommonProps&AndroidProps> {
    render() {
        return (
            <NativeAMap
                {...this.props}
            />
        )
    }

    printCurrentMapShot(){
        AMapManager.printCurrentMapShot(findNodeHandle(this));
    }
    setCenterCoordinate(coordinate: Coordinate) {
        AMapManager.setCenterCoordinate(findNodeHandle(this), coordinate)
    }
    setRegion(region: Region) {
        AMapManager.setRegion(findNodeHandle(this), region)
    }

    setRegionByLatLngs(region: Region[]) {
        AMapManager.setRegionByLatLngs(findNodeHandle(this), region)
    }

    setLatLngZoom(config: SetLatLngZoomParams) {
        AMapManager.setLatLngZoom(findNodeHandle(this), config)
    }

    setMapType(mapType: 0|1|2) {
        AMapManager.setMapType(findNodeHandle(this), mapType)
    }

    movieToUserLocation() {
        AMapManager.movieToUserLocation(findNodeHandle(this))
    }

    setZoomLevel(zoomLevel: number) {
        AMapManager.setZoomLevel(findNodeHandle(this), zoomLevel)
    }

    zoomLevel(callback: (zoomLevel: number) => void) {
        AMapManager.zoomLevel(findNodeHandle(this), safeCallback(callback))
    }

    minZoomLevel(callback: (zoomLevel: number) => void) {
        AMapManager.minZoomLevel(findNodeHandle(this), safeCallback(callback))
    }

    maxZoomLevel(callback: (zoomLevel: number) => void) {
        AMapManager.maxZoomLevel(findNodeHandle(this), safeCallback(callback))
    }

    addAnnotation(annotationConfig: Marker, callback: ReturnAKeyCallback) {
        AMapManager.addAnnotation(findNodeHandle(this), annotationConfig, safeCallback(callback))
    }

    addAnnotations(annotationConfigs: Marker[], callback: ReturnKeysCallback) {
        AMapManager.addAnnotations(findNodeHandle(this), annotationConfigs, safeCallback(callback))
    }

    removeAnnotation(key: string) {
        AMapManager.removeAnnotation(findNodeHandle(this), key)
    }

    removeAnnotations(keys: string) {
        AMapManager.removeAnnotations(findNodeHandle(this), keys)
    }

    removeAllAnnotations(callback: ReturnResultCallback) {
        AMapManager.removeAllAnnotations(findNodeHandle(this),callback)
    }


    showInfoWindow(key: string) {
        AMapManager.showInfoWindow(findNodeHandle(this), key)
    }

    hideInfoWindow(key: string) {
        AMapManager.hideInfoWindow(findNodeHandle(this), key)
    }

    showAnnotation(key: string) {
        AMapManager.showAnnotation(findNodeHandle(this), key)
    }

    hideAnnotation(key: string) {
        AMapManager.hideAnnotation(findNodeHandle(this), key)
    }

    showAnnotations(keys: string[]) {
        AMapManager.showAnnotations(findNodeHandle(this), keys)
    }

    hideAnnotations(keys: string[]) {
        AMapManager.hideAnnotations(findNodeHandle(this), keys)
    }

    addCircle(circleConfig: Circle, callback: ReturnAKeyCallback) {
        AMapManager.addCircle(findNodeHandle(this), circleConfig, safeCallback(callback))
    }

    addCircles(circleConfigs: Circle[], callback: ReturnKeysCallback) {
        AMapManager.addCircles(findNodeHandle(this), circleConfigs, safeCallback(callback))
    }

    removeCircle(key: string) {
        AMapManager.removeCircle(findNodeHandle(this), key)
    }

    removeCircles(keys: string) {
        AMapManager.removeCircles(findNodeHandle(this), keys)
    }

    removeAllCircles(callback: ReturnResultCallback) {
        AMapManager.removeAllCircles(findNodeHandle(this),callback)
    }

    addPolyline(polylineConfig: Line, callback: ReturnAKeyCallback) {
        AMapManager.addPolyline(findNodeHandle(this), polylineConfig, safeCallback(callback))
    }

    addPolylines(polylineConfigs: Line[], callback: ReturnKeysCallback) {
        AMapManager.addPolylines(findNodeHandle(this), polylineConfigs, safeCallback(callback))
    }

    removePolyline(key: string) {
        AMapManager.removePolyline(findNodeHandle(this), key)
    }

    removePolylines(keys: string[]) {
        AMapManager.removePolylines(findNodeHandle(this), keys)
    }

    removeAllPolylines(callback: ReturnResultCallback) {
        AMapManager.removeAllPolylines(findNodeHandle(this),callback)
    }

    addPolygon(polygonConfig: Polygon, callback: ReturnAKeyCallback) {
        AMapManager.addPolygon(findNodeHandle(this), polygonConfig, safeCallback(callback))
    }

    addPolygons(polygonConfigs: Polygon[], callback: ReturnKeysCallback) {
        AMapManager.addPolygons(findNodeHandle(this), polygonConfigs, safeCallback(callback))
    }

    removePolygon(key: string) {
        AMapManager.removePolygon(findNodeHandle(this), key)
    }

    removePolygons(keys: string[]) {
        AMapManager.removePolygons(findNodeHandle(this), keys)
    }

    removeAllPolygons(callback: ReturnResultCallback) {
        AMapManager.removeAllPolygons(findNodeHandle(this),callback)
    }


    setBearing(degree: number){
        AMapManager.setBearing(findNodeHandle(this),degree);
    }

    setTilt(degree: number){
        AMapManager.setTilt(findNodeHandle(this),degree);
    }
}

export interface LinesInCircleCallbackParam {
    results: boolean[]
}

export let AMapUtility= {
    isLinesInCircle(center: CoordinateLong, radius: number, lines: Line[], callback: (param: LinesInCircleCallbackParam) => void) {
        AMapManager.isLinesInCircle(center, radius, lines, safeCallback(callback));
    }
}

export interface PoiItem {
    uid: string;
    name: string;
    type: string;
    typecode: string;
    latitude: number;
    longitude: number;
    address: string;
    tel: string;
    distance: number;
}



export interface SearchParam {
    types?: string;
    keywords?: string;
    limit?: number;
    page?: number;
    coordinate?: CoordinateLong;
    radius?: number;
}

export interface ReGoecodeSearchParam {
    coordinate?: Coordinate;
    radius?: number;
}

export let AMapSearch = {
    searchPoiByCenterCoordinate(params: SearchParam) {
        AMapManager.searchPoiByCenterCoordinate(params) //传null为默认参数配置
    },
    searchPoiByUid(uid: string, callback: (poiItem: PoiItem) => void) {
        AMapManager.searchPoiByUid(uid, safeCallback(callback));
    },

    reGoecodeSearch(params: ReGoecodeSearchParam) {
        AMapManager.reGoecodeSearch(params)
    },
    OnPOISearchDoneEvent: AMapManager != null ? AMapManager.OnPOISearchDoneEvent : null,
    OnReGeocodeSearchDoneEvent: AMapManager != null ? AMapManager.OnReGeocodeSearchDoneEvent : null
}

const NativeAMap = Platform.OS == 'ios' ? requireNativeComponent('RCTAMap') : requireNativeComponent('RCTAMapView');
