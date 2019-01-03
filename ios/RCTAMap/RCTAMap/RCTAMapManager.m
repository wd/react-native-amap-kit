

//#define SCREENHEIGHT [UIScreen mainScreen].bounds.size.height
//#define SCREENWIDTH [UIScreen mainScreen].bounds.size.width

#import "RCTAMapManager.h"
#import "RCTAMap.h"
#import <React/RCTUIManager.h>
#import <React/RCTBridge.h>

#import <AMapFoundationKit/AMapFoundationKit.h>
#import <AMapSearchKit/AMapSearchKit.h>
#import "RCTAnnotation.h"
#import "RCTCircle.h"
#import "RCTPolyline.h"
#import "RCTPolygon.h"
#import "UIImage+Rotate.h"
#import "NSMutableDictionary+AMap.h"
#import "RCTCustomAnnotation.h"
#import "UIColor+utility.h"


@interface RCTAMapManager ()<MAMapViewDelegate, AMapSearchDelegate>

@property (nonatomic, strong) AMapSearchAPI *search;

@property (nonatomic, strong) NSMutableSet *randomSet;

@end

@implementation RCTAMapManager

- (NSMutableSet *)randomSet {
    if (_randomSet == nil) {
        _randomSet = [NSMutableSet set];
    }
    
    return _randomSet;
}

- (NSString *)generateKeyWithPrefix:(NSString *)prefix {
    
    while(YES) {
        uint32_t x = arc4random();
        NSString *key = [NSString stringWithFormat: @"%@%d", prefix, x];
        
        if ([self.randomSet containsObject: key]) {
            continue;
        }
        
        [self.randomSet addObject: key];
        return key;
    }
    
    return @"";
}

+ (BOOL)requiresMainQueueSetup {
    return YES;
}




- (NSDictionary<NSString *, id> *)constantsToExport {
    return @{@"OnPOISearchDoneEvent": @"amap.onPOISearchDone",
             @"OnReGeocodeSearchDoneEvent": @"amap.onReGeocodeSearchDone"
             };
}


RCT_EXPORT_MODULE(RCTAMap)

- (UIView *)view {
    RCTAMap *mapViewContainer = [RCTAMap new];
    mapViewContainer.mapView.delegate = self;

    self.search = [[AMapSearchAPI alloc] init];
    self.search.delegate = self;

    // 一些默认参数，以后可以增加配置项
    mapViewContainer.mapView.showsScale = false; // 不显示比例尺
    mapViewContainer.mapView.showsCompass = false; // 不显示指南针

    return mapViewContainer;
}

RCT_EXPORT_VIEW_PROPERTY(onDidMoveByUser, RCTBubblingEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onSingleTapped, RCTBubblingEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onLongTapped, RCTBubblingEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onMapZoomChange, RCTBubblingEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onAnnotationDragChange, RCTBubblingEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onAnnotationClick, RCTBubblingEventBlock);

RCT_CUSTOM_VIEW_PROPERTY(mapType, NSNumber, RCTAMap) {
    NSNumber *mapType = [RCTConvert NSNumber:json];
    RCTAMap *mapViewContainer = (RCTAMap *)view;
    mapViewContainer.mapView.mapType = [mapType integerValue];
    
}

RCT_CUSTOM_VIEW_PROPERTY(showsUserLocation, NSNumber, RCTAMap) {
    NSNumber *showsUserLocation = [RCTConvert NSNumber: json];
    RCTAMap *mapViewContainer = (RCTAMap *)view;
    mapViewContainer.mapView.showsUserLocation = [showsUserLocation boolValue];
}

RCT_CUSTOM_VIEW_PROPERTY(bearing, NSNumber, RCTAMap) {
    NSNumber *bearing = [RCTConvert NSNumber:json];
    RCTAMap *mapViewContainer = (RCTAMap *)view;

    mapViewContainer.mapView.rotationDegree = [bearing doubleValue];
}

RCT_CUSTOM_VIEW_PROPERTY(tilt, NSNumber, RCTAMap) {
    NSNumber *tilt = [RCTConvert NSNumber:json];
    RCTAMap *mapViewContainer = (RCTAMap *)view;

    mapViewContainer.mapView.cameraDegree = [tilt doubleValue];
}


RCT_CUSTOM_VIEW_PROPERTY(circles, NSArray, RCTAMap) {
    NSArray *circles = [RCTConvert NSArray:json];
    RCTAMap *mapView = (RCTAMap *)view;

    NSArray *keys = [mapView.circleDict allKeys];

    __weak RCTAMapManager *weakSelf = self;
    [self addMapKeys:keys annotations:circles addCallback:^(NSDictionary *config) {
        [weakSelf addCirclesWithView: mapView circleConfig: config];
    } removeCallback:^(NSString *key) {
        [weakSelf removeCircleWithView: mapView key: key];
    }];
}

RCT_CUSTOM_VIEW_PROPERTY(markers, NSArray, RCTAMap) {
    NSArray *markers = [RCTConvert NSArray:json];
    RCTAMap *mapView = (RCTAMap *)view;

    NSArray *keys = [mapView.annotDict allKeys];

    __weak RCTAMapManager *weakSelf = self;
    [self addMapKeys:keys annotations:markers addCallback:^(NSDictionary *config) {
        [weakSelf addAnnotationWithView: mapView annotationConfig: config];
    } removeCallback:^(NSString *key) {
        [weakSelf removeAnnotationWithView: mapView key: key];
    }];
}

- (void)addMapKeys:(NSArray *)keys
       annotations:(NSArray *)annotations
       addCallback:(void (^ __nullable)(NSDictionary *config))addCallback
    removeCallback:(void (^ __nullable)(NSString *key))removeCallback {

    BOOL hasUid = false;
    if (annotations.count > 0) {
        NSDictionary *annotationConfig = [annotations firstObject];
        if ([annotationConfig objectForKey: @"uid"]) {
            hasUid = true;
        }
    }

    if (hasUid) {   // 有uid
        // 原有的key Set
        NSSet *origKeysSet = [NSSet setWithArray: keys];

        // 新添加进来的Key Set
        NSMutableSet *newKeySet = [NSMutableSet set];
        for (NSDictionary *annotationConfig in annotations) {
            [newKeySet addObject: [annotationConfig objectForKey: @"uid"]];
        }

        // 两个set之间的交集
        NSMutableSet *interSet = [NSMutableSet setWithSet: origKeysSet];
        [interSet intersectSet: newKeySet];

        // old maker need delete
        NSMutableSet *deleteSet = [NSMutableSet setWithSet: origKeysSet];
        [deleteSet minusSet: interSet];
        for (NSString *key in deleteSet) {
            removeCallback(key);
        }


        // new maker need add
        NSMutableSet *addSet = [NSMutableSet setWithSet: newKeySet];
        [addSet minusSet: interSet];
        for (NSDictionary *config in annotations) {
            NSString *key = [config objectForKey: @"uid"];
            if ([addSet containsObject: key]) {
                addCallback(config);
            }
        }
    } else {    // 没有uid
        for (NSString *key in keys) {
            removeCallback(key);
        }

        for (NSDictionary *config in annotations) {
            addCallback(config);
        }
    }
}

RCT_CUSTOM_VIEW_PROPERTY(polygons, NSArray, RCTAMap) {
    NSArray *polygons = [RCTConvert NSArray:json];
    RCTAMap *mapView = (RCTAMap *)view;

    NSArray *keys = [mapView.polygonDict allKeys];
    __weak RCTAMapManager *weakSelf = self;
    [self addMapKeys:keys annotations:polygons addCallback:^(NSDictionary *config) {
        [weakSelf addPolygonWithView: mapView polygonConfig: config];
    } removeCallback:^(NSString *key) {
        [weakSelf removePolygonWithView: mapView key: key];
    }];
}

RCT_CUSTOM_VIEW_PROPERTY(polylines, NSArray, RCTAMap) {
    NSArray *polylines = [RCTConvert NSArray:json];
    RCTAMap *mapView = (RCTAMap *)view;

    NSArray *keys = [mapView.polylineDict allKeys];

    __weak RCTAMapManager *weakSelf = self;
    [self addMapKeys:keys annotations:polylines addCallback:^(NSDictionary *config) {
        [weakSelf addPolylineWithView: mapView polylineConfig: config];
    } removeCallback:^(NSString *key) {
        [weakSelf removePolylineWithView: mapView key: key];
    }];
}

// 指定缩放级别
RCT_CUSTOM_VIEW_PROPERTY(zoomLevel, NSNumber, RCTAMap) {
    NSNumber *zoomLevel = [RCTConvert NSNumber:json];
    [view.mapView setZoomLevel:[zoomLevel floatValue] animated: YES];
}

// set custome map style
RCT_CUSTOM_VIEW_PROPERTY(customMapStyleFileName, NSString, RCTAMap) {
    NSString *fileName = [RCTConvert NSString:json];
    RCTAMap *mapViewContainer = (RCTAMap *)view;

    NSString *path = [NSString stringWithFormat:@"%@/%@", [NSBundle mainBundle].bundlePath, fileName];
    NSData *data = [NSData dataWithContentsOfFile:path];
    [mapViewContainer.mapView setCustomMapStyleWithWebData:data];
    [mapViewContainer.mapView setCustomMapStyleEnabled:YES];
}

//根据经纬度指定地图的中心点
RCT_CUSTOM_VIEW_PROPERTY(centerCoordinate, NSDictionary, RCTAMap) {
    NSDictionary *centerCoordinate = [RCTConvert NSDictionary:json];
    NSNumber *latitude = [centerCoordinate objectForKey:@"latitude"];
    NSNumber *longitude = [centerCoordinate objectForKey:@"longitude"];

    if ([centerCoordinate objectForKey:@"latitude"] != nil
        && [centerCoordinate objectForKey:@"longitude"] != nil) {
        latitude = [centerCoordinate objectForKey:@"latitude"];
        longitude = [centerCoordinate objectForKey:@"longitude"];
    } else {
        latitude = [centerCoordinate objectForKey:@"lat"];
        longitude = [centerCoordinate objectForKey:@"lng"];
    }
    [view.mapView setCenterCoordinate:CLLocationCoordinate2DMake([latitude floatValue], [longitude floatValue]) animated:NO];
}

// set region
RCT_CUSTOM_VIEW_PROPERTY(region, NSDictionary, RCTAMap) {
    NSDictionary *region = [RCTConvert NSDictionary:json];
    RCTAMap *mapView = (RCTAMap *)view;

    if(region == nil) {
        return;
    }

    if([region objectForKey:@"radius"]) {
        [self setRegionWithView:mapView :region];
    } else {
        [self setRegionByLatLngsWithView:mapView :region];
    }
}

//定位当前用户位置，并自动显示在地图中心
RCT_EXPORT_METHOD(setCenterCoordinate:(nonnull NSNumber *)reactTag :(nonnull NSDictionary *)coordinate)
{
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];
        RCTAMap *mapViewContainer = (RCTAMap *)view;
        CGFloat latitude = [[coordinate objectForKey:@"latitude"] floatValue];
        CGFloat longitude = [[coordinate objectForKey:@"longitude"] floatValue];
        [mapViewContainer.mapView setCenterCoordinate:CLLocationCoordinate2DMake(latitude, longitude) animated:YES];
    }];
}

RCT_EXPORT_METHOD(setRegion:(nonnull NSNumber *)reactTag :(nonnull NSDictionary *)region)
{
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];
        RCTAMap *mapView = (RCTAMap *)view;
        [self setRegionWithView:mapView :region];
    }];
}

- (void)setRegionWithView:(RCTAMap *)mapView :(NSDictionary *)region {
    NSDictionary *coordinateCenter = [region objectForKey: @"center"];
    double latitude = 0;
    double longitude = 0;
    if ([coordinateCenter objectForKey:@"latitude"] != nil
        && [coordinateCenter objectForKey:@"longitude"] != nil) {
        latitude = [[coordinateCenter objectForKey:@"latitude"] doubleValue];
        longitude = [[coordinateCenter objectForKey:@"longitude"] doubleValue];
    } else {
        latitude = [[coordinateCenter objectForKey:@"lat"] doubleValue];
        longitude = [[coordinateCenter objectForKey:@"lng"] doubleValue];
    }

    double radius = [[region objectForKey: @"radius"] doubleValue];
    double padding = 50;
    if([region objectForKey: @"padding"] != nil) {
        padding = [[region objectForKey: @"padding"] doubleValue];
    }
    BOOL animate = [[region objectForKey: @"animate"] boolValue];

    MACoordinateRegion corRegion = MACoordinateRegionMakeWithDistance(CLLocationCoordinate2DMake(latitude, longitude), 2 * radius, 2 * radius);
    MAMapRect rect = MAMapRectForCoordinateRegion(corRegion);
    [mapView.mapView setVisibleMapRect:rect edgePadding:UIEdgeInsetsMake(padding, padding, padding, padding) animated:animate];
}

RCT_EXPORT_METHOD(setRegionByLatLngs:(nonnull NSNumber *)reactTag :(nonnull NSDictionary *)region) {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];
        RCTAMap *mapView = (RCTAMap *)view;
        [self setRegionByLatLngsWithView:mapView :region];
    }];
}

- (void)setRegionByLatLngsWithView:(RCTAMap *)mapView :(NSDictionary *)region {
    NSArray *coordinates = [region objectForKey: @"coordinates"];
    BOOL animate = [[region objectForKey: @"animate"] boolValue];

    double padding = 50;
    if([region objectForKey: @"padding"] != nil) {
        padding = [[region objectForKey: @"padding"] doubleValue];
    }

    double maxLng = 0; // 最大经度
    double minLng = 0; // 最小经度
    double maxLat = 0; // 最大纬度
    double minLat = 0; // 最小纬度

    for (NSInteger i = 0; i < coordinates.count; i++) {
        NSDictionary *coor = coordinates[i];
        double tmpLat;
        double tmpLng;

        if ([coor objectForKey:@"latitude"] != nil
            && [coor objectForKey:@"longitude"] != nil) {
            tmpLat = [[coor objectForKey:@"latitude"] doubleValue];
            tmpLng = [[coor objectForKey:@"longitude"] doubleValue];
        } else {
            tmpLat = [[coor objectForKey:@"lat"] doubleValue];
            tmpLng = [[coor objectForKey:@"lng"] doubleValue];
        }

        if(i == 0) {
            maxLat = tmpLat;
            minLat = tmpLat;
            maxLng = tmpLng;
            minLng = tmpLng;
        } else {
            if (tmpLng > maxLng) {
                maxLng = tmpLng;
            }

            if(tmpLng < minLng) {
                minLng = tmpLng;
            }

            if(tmpLat > maxLat) {
                maxLat = tmpLat;
            }

            if(tmpLat < minLat) {
                minLat = tmpLat;
            }
        }
    }

    MAMapPoint northEast = MAMapPointForCoordinate(CLLocationCoordinate2DMake(maxLat, maxLng));
    MAMapPoint sourceWest = MAMapPointForCoordinate(CLLocationCoordinate2DMake(minLat, minLng));

    ///根据annotation点和对角线点计算出对应的rect（相对于中心点）
    MAMapRect rect = MAMapRectMake(sourceWest.x, northEast.y, ABS(northEast.x - sourceWest.x), ABS(northEast.y - sourceWest.y));
    [mapView.mapView setVisibleMapRect:rect edgePadding:UIEdgeInsetsMake(padding, padding, padding, padding) animated:animate];
}

RCT_EXPORT_METHOD(movieToUserLocation:(nonnull NSNumber *)reactTag) {

    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];
        RCTAMap *mapViewContainer = (RCTAMap *)view;
        MAUserLocation *userLoc = mapViewContainer.mapView.userLocation;


        [mapViewContainer.mapView setCenterCoordinate:userLoc.location.coordinate animated:YES];
    }];
}

RCT_EXPORT_METHOD(setBearing:(nonnull NSNumber *)reactTag :(nonnull NSNumber *)rotate) {

    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];
        RCTAMap *mapViewContainer = (RCTAMap *)view;

        [mapViewContainer.mapView setRotationDegree: [rotate floatValue] animated: YES duration: 0.25];
    }];
}

RCT_EXPORT_METHOD(setTilt:(nonnull NSNumber *)reactTag :(nonnull NSNumber *)cameraDegree) {

    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];
        RCTAMap *mapViewContainer = (RCTAMap *)view;

        [mapViewContainer.mapView setCameraDegree: [cameraDegree floatValue] animated: YES duration: 0.25];
    }];
}


RCT_EXPORT_METHOD(zoomLevel:(nonnull NSNumber *)reactTag :(RCTResponseSenderBlock)callback) {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];
        RCTAMap *mapViewContainer = (RCTAMap *)view;
        NSNumber *zoomLevel = [NSNumber numberWithDouble: mapViewContainer.mapView.zoomLevel];
        callback(@[zoomLevel]);
    }];
}


RCT_EXPORT_METHOD(minZoomLevel:(nonnull NSNumber *)reactTag :(RCTResponseSenderBlock)callback) {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];
        RCTAMap *mapViewContainer = (RCTAMap *)view;
        NSNumber *minZoomLevel = [NSNumber numberWithDouble: mapViewContainer.mapView.minZoomLevel];
        callback(@[minZoomLevel]);
    }];
}

RCT_EXPORT_METHOD(maxZoomLevel:(nonnull NSNumber *)reactTag :(RCTResponseSenderBlock)callback) {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];
        RCTAMap *mapViewContainer = (RCTAMap *)view;
        NSNumber *maxZoomLevel = [NSNumber numberWithDouble: mapViewContainer.mapView.maxZoomLevel];
        callback(@[maxZoomLevel]);
    }];
}

- (NSString *)addAnnotationWithView:(RCTAMap *)mapViewContainer annotationConfig:(nonnull NSDictionary *)annotationConfig {
    NSDictionary *coordinate = [annotationConfig objectForKey:@"coordinate"];

    double latitude = 0;
    double longitude = 0;
    if ([coordinate objectForKey:@"latitude"] != nil
        && [coordinate objectForKey:@"longitude"] != nil) {
        latitude = [[coordinate objectForKey:@"latitude"] doubleValue];
        longitude = [[coordinate objectForKey:@"longitude"] doubleValue];
    } else {
        latitude = [[coordinate objectForKey:@"lat"] doubleValue];
        longitude = [[coordinate objectForKey:@"lng"] doubleValue];
    }

    NSString *key = nil;
    if ([annotationConfig objectForKey: @"uid"]) {
        key = [annotationConfig objectForKey: @"uid"];
    } else {
        key = [self generateKeyWithPrefix: @"Annot"];
    }
    if ([annotationConfig objectForKey: @"customView"]) {
        RCTCustomAnnotation *customAnno = [[RCTCustomAnnotation alloc] initWithKey: key
                                                                           coordinate: CLLocationCoordinate2DMake(latitude, longitude)];
        customAnno.customViewName = [annotationConfig objectForKey: @"customView"];
        customAnno.customProps = annotationConfig;
        [mapViewContainer.annotDict setObject: customAnno forKey: key];
        [mapViewContainer.mapView addAnnotation: customAnno];
    } else {
        NSString *title = [annotationConfig objectForKey:@"title"];
        NSString *imageName = [annotationConfig objectForKey: @"imageName"];
        NSString *subtitle = [annotationConfig objectForKey:@"subtitle"];
        BOOL hasAngle = [annotationConfig objectForKey: @"angle"] != nil;
        double angle = [[annotationConfig objectForKey: @"angle"] doubleValue];
        BOOL draggable = [[annotationConfig objectForKey: @"draggable"] boolValue];

        RCTAnnotation *annotation = [[RCTAnnotation alloc] initWithKey: key
                                                            coordinate: CLLocationCoordinate2DMake(latitude, longitude)
                                                                 title: title
                                                              subtitle: subtitle
                                                             imageName: imageName];
        annotation.draggable = draggable;
        annotation.angle = angle;
        annotation.hasAngle = hasAngle;
        
        annotation.customProps = annotationConfig;
        [mapViewContainer.annotDict setObject:annotation forKey: key];
        [mapViewContainer.mapView addAnnotation: annotation];
    }

    return key;
}

RCT_EXPORT_METHOD(addAnnotation:(nonnull NSNumber *)reactTag :(nonnull NSDictionary *)annotationConfig :(RCTResponseSenderBlock)callback) {

    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];

        RCTAMap *mapView = (RCTAMap *)view;
        NSString *key = [self addAnnotationWithView: mapView annotationConfig: annotationConfig];

        callback(@[key]);
    }];
}

RCT_EXPORT_METHOD(addAnnotations: (nonnull NSNumber *)reactTag
                  :(nonnull NSArray *)annotationConfigs
                  :(RCTResponseSenderBlock)callback){
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];

        RCTAMap *mapView = (RCTAMap *)view;
        NSMutableArray *keyArr = [[NSMutableArray alloc] init];
        for (NSDictionary *annotationConfig in annotationConfigs) {
            NSString *key = [self addAnnotationWithView: mapView annotationConfig: annotationConfig];
            [keyArr addObject: key];
        }

        callback(@[keyArr]);
    }];
}

- (void)removeAnnotationWithView: (RCTAMap *)mapViewContainer key:(nonnull NSString *)key {
    if (key != nil) {
        RCTAnnotation *tmpAnnotation = [mapViewContainer.annotDict objectForKey: key];
        if (tmpAnnotation != nil) {
            [mapViewContainer.mapView removeAnnotation: tmpAnnotation];
        }

        [mapViewContainer.annotDict removeObjectForKey: key];
    }
}

RCT_EXPORT_METHOD(removeAnnotation:(nonnull NSNumber *)reactTag :(nonnull NSString *)key) {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];

        RCTAMap *mapView = (RCTAMap *)view;
        [self removeAnnotationWithView: mapView key: key];
    }];
}

RCT_EXPORT_METHOD(removeAnnotations:(nonnull NSNumber *)reactTag :(nonnull NSArray *)keys) {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];

        RCTAMap *mapView = (RCTAMap *)view;

        for (NSString *key in keys) {
            [self removeAnnotationWithView: mapView key: key];
        }
    }];
}

RCT_EXPORT_METHOD(removeAllAnnotations:(nonnull NSNumber *)reactTag :(RCTResponseSenderBlock)callback) {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];

        RCTAMap *mapView = (RCTAMap *)view;
        NSArray *keys = [mapView.annotDict allKeys];
        for (NSString *key in keys) {
            [self removeAnnotationWithView: mapView key: key];
        }

        callback(@[@true]);
    }];
}


- (NSString *)addCirclesWithView:(RCTAMap *)mapViewContainer circleConfig:(nonnull NSDictionary *)circleConfig {
    CGFloat radius = [[circleConfig objectForKey:@"radius"] floatValue];
    NSDictionary *coordinate = [circleConfig objectForKey:@"coordinate"];
    double latitude = 0;
    double longitude = 0;
    if ([coordinate objectForKey:@"latitude"] != nil) {
        latitude = [[coordinate objectForKey:@"latitude"] doubleValue];
        longitude = [[coordinate objectForKey:@"longitude"] doubleValue];
    } else {
        latitude = [[coordinate objectForKey:@"lat"] doubleValue];
        longitude = [[coordinate objectForKey:@"lng"] doubleValue];
    }

    UIColor *fillColor = [UIColor colorWithHexStr: [circleConfig objectForKey: @"fillColor"]];
    UIColor *strokeColor = [UIColor colorWithHexStr: [circleConfig objectForKey: @"strokeColor"]];
    NSInteger lineWidth = [[circleConfig objectForKey: @"lineWidth"] integerValue];

    NSString *key = nil;
    if ([circleConfig objectForKey: @"uid"]) {
        key = [circleConfig objectForKey: @"uid"];
    } else {
        key = [self generateKeyWithPrefix: @"Circ"];
    }
    RCTCircle *circle = [RCTCircle circleWithCenterCoordinate: CLLocationCoordinate2DMake(latitude, longitude) radius: radius];
    circle.key = key;
    circle.fillColor = fillColor;
    circle.strokeColor = strokeColor;
    circle.lineWidth = lineWidth;


    [mapViewContainer.circleDict setObject:circle forKey: key];

    [mapViewContainer.mapView addOverlay: circle];

    return key;
}

RCT_EXPORT_METHOD(addCircle:(nonnull NSNumber *)reactTag :(nonnull NSDictionary *)circleConfig :(RCTResponseSenderBlock)callback) {

    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];

        RCTAMap *mapView = (RCTAMap *)view;

        NSString *key = [self addCirclesWithView: mapView circleConfig: circleConfig];
        callback(@[key]);
    }];
}

RCT_EXPORT_METHOD(addCircles:(nonnull NSNumber *)reactTag :(nonnull NSArray *)circleConfigs :(RCTResponseSenderBlock)callback) {

    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];

        RCTAMap *mapView = (RCTAMap *)view;
        NSMutableArray *keyArr = [[NSMutableArray alloc] init];

        for (NSDictionary *circleConfig in circleConfigs) {
            NSString *key = [self addCirclesWithView: mapView circleConfig: circleConfig];
            [keyArr addObject:key];
        }


        callback(@[keyArr]);
    }];
}

- (void)removeCircleWithView:(RCTAMap *)mapViewContainer key:(NSString *)key {
    if (key != nil) {
        RCTCircle *tmpCircle = [mapViewContainer.circleDict objectForKey: key];
        if (tmpCircle != nil) {
            [mapViewContainer.mapView removeOverlay: tmpCircle];
        }

        [mapViewContainer.circleDict removeObjectForKey: key];
    }
}

RCT_EXPORT_METHOD(removeCircle:(nonnull NSNumber *)reactTag :(nonnull NSString *)key) {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];

        RCTAMap *mapView = (RCTAMap *)view;
        [self removeCircleWithView: mapView key: key];
    }];
}

RCT_EXPORT_METHOD(removeCircles:(nonnull NSNumber *)reactTag :(nonnull NSArray *)keys) {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];

        RCTAMap *mapView = (RCTAMap *)view;
        for (NSString *key in keys) {
            [self removeCircleWithView: mapView key: key];
        }
    }];
}

RCT_EXPORT_METHOD(removeAllCircles:(nonnull NSNumber *)reactTag :(RCTResponseSenderBlock)callback) {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];

        RCTAMap *mapView = (RCTAMap *)view;
        NSArray *keys = [mapView.circleDict allKeys];
        for (NSString *key in keys) {
            [self removeCircleWithView: mapView key: key];
        }

        callback(@[@true]);
    }];
}

- (NSString *)addPolylineWithView:(RCTAMap *)mapViewContainer polylineConfig:(nonnull NSDictionary *)polylineConfig {
    NSArray *coordinates = [polylineConfig objectForKey:@"coordinates"];
    NSInteger count = coordinates.count;
    CLLocationCoordinate2D coordinateArr[count];

    for (int i = 0; i < count; i++) {
        NSDictionary *coordinate = coordinates[i];
        double latitude = 0;
        double longitude = 0;
        if ([coordinate objectForKey:@"latitude"] != nil
            && [coordinate objectForKey:@"longitude"] != nil) {
            latitude = [[coordinate objectForKey:@"latitude"] doubleValue];
            longitude = [[coordinate objectForKey:@"longitude"] doubleValue];
        } else {
            latitude = [[coordinate objectForKey:@"lat"] doubleValue];
            longitude = [[coordinate objectForKey:@"lng"] doubleValue];
        }
        coordinateArr[i] = CLLocationCoordinate2DMake(latitude, longitude);
    }

    UIColor *strokeColor = [UIColor colorWithHexStr: [polylineConfig objectForKey: @"strokeColor"]];
    NSInteger lineWidth = [[polylineConfig objectForKey: @"lineWidth"] integerValue];

    NSString *key = nil;
    if ([polylineConfig objectForKey: @"uid"]) {
        key = [polylineConfig objectForKey: @"uid"];
    } else {
        key = [self generateKeyWithPrefix: @"Polyl"];
    }
    RCTPolyline *polyline = [RCTPolyline polylineWithCoordinates:coordinateArr count:count];
    polyline.key = key;
    polyline.strokeColor = strokeColor;
    polyline.lineWidth = lineWidth;

    [mapViewContainer.polylineDict setObject:polyline forKey: key];
    [mapViewContainer.mapView addOverlay: polyline];

    return key;
}

RCT_EXPORT_METHOD(addPolyline:(nonnull NSNumber *)reactTag :(nonnull NSDictionary *)polylineConfig :(RCTResponseSenderBlock)callback) {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];

        RCTAMap *mapView = (RCTAMap *)view;

        NSString *key = [self addPolylineWithView: mapView polylineConfig: polylineConfig];
        callback(@[key]);
    }];
}

RCT_EXPORT_METHOD(addPolylines:(nonnull NSNumber *)reactTag :(nonnull NSArray *)polylineConfigs :(RCTResponseSenderBlock)callback) {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];

        RCTAMap *mapView = (RCTAMap *)view;
        NSMutableArray *keyArr = [[NSMutableArray alloc] init];
        for (NSDictionary *polylineConfig in polylineConfigs) {
            NSString *key = [self addPolylineWithView: mapView polylineConfig: polylineConfig];
            [keyArr addObject: key];
        }

        callback(@[keyArr]);
    }];
}

- (void)removePolylineWithView:(RCTAMap *)mapViewContainer key: (NSString *)key {
    if (key != nil) {
        RCTPolyline *tmpPolyline = [mapViewContainer.polylineDict objectForKey: key];
        if (tmpPolyline != nil) {
            [mapViewContainer.mapView removeOverlay: tmpPolyline];
        }

        [mapViewContainer.polylineDict removeObjectForKey: key];
    }
}

RCT_EXPORT_METHOD(removePolyline:(nonnull NSNumber *)reactTag :(nonnull NSString *)key) {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];

        RCTAMap *mapView = (RCTAMap *)view;
        [self removePolylineWithView: mapView key: key];
    }];
}

RCT_EXPORT_METHOD(removePolylines:(nonnull NSNumber *)reactTag :(nonnull NSArray *)keys) {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];

        RCTAMap *mapView = (RCTAMap *)view;
        for (NSString *key in keys) {
            [self removePolylineWithView: mapView key: key];
        }
    }];
}

RCT_EXPORT_METHOD(removeAllPolylines:(nonnull NSNumber *)reactTag :(RCTResponseSenderBlock)callback) {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];

        RCTAMap *mapView = (RCTAMap *)view;
        NSArray *keys = [mapView.polylineDict allKeys];
        for (NSString *key in keys) {
            [self removePolylineWithView: mapView key: key];
        }

        callback(@[@true]);
    }];
}


- (NSString *)addPolygonWithView:(RCTAMap *)mapViewContainer polygonConfig:(NSDictionary *)polygonConfig {
    UIColor *strokeColor = [UIColor colorWithHexStr: [polygonConfig objectForKey: @"strokeColor"]];
    UIColor *fillColor = [UIColor colorWithHexStr: [polygonConfig objectForKey: @"fillColor"]];
    NSInteger lineWidth = [[polygonConfig objectForKey: @"lineWidth"] integerValue];

    NSArray *coordinates = [polygonConfig objectForKey:@"coordinates"];
    NSInteger count = coordinates.count;
    CLLocationCoordinate2D coordinateArr[count];

    for (int i = 0; i < count; i++) {
        NSDictionary *coordinate = coordinates[i];
        CGFloat latitude;
        CGFloat longitude;

        if ([coordinate objectForKey:@"latitude"] != nil
            && [coordinate objectForKey:@"longitude"] != nil) {
            latitude = [[coordinate objectForKey:@"latitude"] doubleValue];
            longitude = [[coordinate objectForKey:@"longitude"] doubleValue];
        } else {
            latitude = [[coordinate objectForKey:@"lat"] doubleValue];
            longitude = [[coordinate objectForKey:@"lng"] doubleValue];
        }
        coordinateArr[i] = CLLocationCoordinate2DMake(latitude, longitude);
    }

    NSString *key = nil;
    if ([polygonConfig objectForKey: @"uid"]) {
        key = [polygonConfig objectForKey: @"uid"];
    } else {
        key = [self generateKeyWithPrefix: @"Polygon"];
    }

    RCTPolygon *polygon = [RCTPolygon polygonWithCoordinates: coordinateArr count:count];
    polygon.strokeColor = strokeColor;
    polygon.fillColor = fillColor;
    polygon.lineWidth = lineWidth;

    [mapViewContainer.polygonDict setObject: polygon forKey:key];
    [mapViewContainer.mapView addOverlay: polygon];

    return key;
}

RCT_EXPORT_METHOD(addPolygon:(nonnull NSNumber *)reactTag :(nonnull NSDictionary *)polygonConfig :(RCTResponseSenderBlock)callback) {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];

        RCTAMap *mapView = (RCTAMap *)view;

        NSString *key = [self addPolygonWithView: mapView polygonConfig: polygonConfig];
        callback(@[key]);
    }];
}

RCT_EXPORT_METHOD(addPolygons:(nonnull NSNumber *)reactTag :(nonnull NSArray *)polygonConfigs :(RCTResponseSenderBlock)callback) {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];

        RCTAMap *mapView = (RCTAMap *)view;

        NSMutableArray *keyArr = [NSMutableArray array];
        for (NSDictionary *polygonConfig in polygonConfigs) {
            NSString *key = [self addPolygonWithView: mapView polygonConfig: polygonConfig];
            [keyArr addObject: key];
        }

        callback(@[keyArr]);
    }];
}

- (void)removePolygonWithView:(RCTAMap *)mapViewContainer key: (NSString *)key {
    if (key != nil) {
        RCTPolygon *tmpPolygon = [mapViewContainer.polygonDict objectForKey: key];
        if (tmpPolygon != nil) {
            [mapViewContainer.mapView removeOverlay: tmpPolygon];
        }

        [mapViewContainer.polygonDict removeObjectForKey: key];
    }
}

RCT_EXPORT_METHOD(removePolygon:(nonnull NSNumber *)reactTag :(nonnull NSString *)key) {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];

        RCTAMap *mapView = (RCTAMap *)view;
        [self removePolygonWithView: mapView key: key];
    }];
}

RCT_EXPORT_METHOD(removePolygons:(nonnull NSNumber *)reactTag :(nonnull NSArray *)keys) {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];

        RCTAMap *mapView = (RCTAMap *)view;
        for (NSString *key in keys) {
            [self removePolygonWithView: mapView key: key];
        }
    }];
}

RCT_EXPORT_METHOD(removeAllPolygons:(nonnull NSNumber *)reactTag :(RCTResponseSenderBlock)callback) {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];

        RCTAMap *mapView = (RCTAMap *)view;
        NSArray *keys = [mapView.polygonDict allKeys];
        for (NSString *key in keys) {
            [self removePolygonWithView: mapView key: key];
        }

        callback(@[@true]);
    }];
}


RCT_EXPORT_METHOD(userLocation:(nonnull NSNumber *)reactTag :(RCTResponseSenderBlock)callback) {
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];

        RCTAMap *mapViewContainer = (RCTAMap *)view;
        CLLocationCoordinate2D coordinate = mapViewContainer.mapView.userLocation.coordinate;
        NSMutableDictionary *coordinateDict = [NSMutableDictionary dictionary];
        [coordinateDict setObject: [NSNumber numberWithFloat:coordinate.latitude] forKey:@"latitude"];
        [coordinateDict setObject: [NSNumber numberWithFloat:coordinate.longitude] forKey:@"longitude"];

        callback(@[coordinateDict]);
    }];
}

RCT_EXPORT_METHOD(isPointInCenter:(nonnull NSDictionary *)centerDict
                  :(nonnull NSDictionary *)pointDict
                  :(nonnull NSNumber *)radius
                  :(nonnull NSNumber *)index
                  :(RCTResponseSenderBlock)callback) {

    CLLocationCoordinate2D center = CLLocationCoordinate2DMake([centerDict[@"latitude"] floatValue], [centerDict[@"longitude"] floatValue]);
    CLLocationCoordinate2D location = CLLocationCoordinate2DMake([pointDict[@"latitude"] floatValue], [pointDict[@"longitude"] floatValue]);
    BOOL isContains = MACircleContainsCoordinate(location, center, [radius integerValue]);

    return callback(@[@{@"result": [NSNumber numberWithBool: isContains],
                        @"index": index}]);
}

RCT_EXPORT_METHOD(isLinesInCircle:(nonnull NSDictionary *)centerDict
                  :(nonnull NSNumber *)radius
                  :(nonnull NSArray *)lines
                  :(RCTResponseSenderBlock)callback) {

    CLLocationCoordinate2D center = CLLocationCoordinate2DMake([centerDict[@"latitude"] floatValue], [centerDict[@"longitude"] floatValue]);
    NSMutableArray *resArr = [NSMutableArray array];

    for (NSDictionary *lineDict in lines) {
        NSDictionary *depDict = lineDict[@"departure"];
        CLLocationCoordinate2D depLocation = CLLocationCoordinate2DMake([depDict[@"latitude"] floatValue], [depDict[@"longitude"] floatValue]);
        BOOL isContainDep = MACircleContainsCoordinate(center, depLocation, [radius integerValue]);

        NSDictionary *arriveDict = lineDict[@"arrive"];
        CLLocationCoordinate2D arriveLocation = CLLocationCoordinate2DMake([arriveDict[@"latitude"] floatValue], [arriveDict[@"longitude"] floatValue]);
        BOOL isContianArrive = MACircleContainsCoordinate(center, arriveLocation, [radius integerValue]);
        [resArr addObject: [NSNumber numberWithBool: (isContainDep && isContianArrive)]];
    }



    return callback(@[@{@"results": resArr}]);
}


/* 根据中心点坐标来搜周边的POI. */
RCT_EXPORT_METHOD(searchPoiByCenterCoordinate:(NSDictionary *)params)
{
    AMapPOIKeywordsSearchRequest *request = [[AMapPOIKeywordsSearchRequest alloc] init];

    if(params != nil) {
        NSArray *keys = [params allKeys];

        if([keys containsObject:@"types"]) {
            NSString *types = [params objectForKey:@"types"];
            request.types = types;
        }
        if([keys containsObject:@"sortrule"]) {
            int sortrule = [[params objectForKey:@"sortrule"] intValue];
            request.sortrule = sortrule;
        }
        if([keys containsObject:@"limit"]) {
            int offset = [[params objectForKey:@"limit"] intValue];
            request.offset = offset;
        }
        if([keys containsObject:@"page"]) {
            int page = [[params objectForKey:@"page"] intValue];
            request.page = page;
        }
        if([keys containsObject:@"requireExtension"]) {
            BOOL requireExtension = [[params objectForKey:@"requireExtension"] boolValue];
            request.requireExtension = requireExtension;
        }
        if([keys containsObject:@"requireSubPOIs"]) {
            BOOL requireSubPOIs = [[params objectForKey:@"requireSubPOIs"] boolValue];
            request.requireSubPOIs = requireSubPOIs;
        }


        if([keys containsObject:@"keywords"]) {
            NSString *keywords = [params objectForKey:@"keywords"];
            request.keywords = keywords;
        }

    }

    [self.search AMapPOIKeywordsSearch:request];
}

RCT_EXPORT_METHOD(searchPoiByUid:(NSString *)uid) {
    AMapPOIIDSearchRequest *request = [[AMapPOIIDSearchRequest alloc] init];
    request.uid = uid;

    [self.search AMapPOIIDSearch:request];
}

RCT_EXPORT_METHOD(reGoecodeSearch:(NSDictionary *)params) {
    AMapReGeocodeSearchRequest *request = [[AMapReGeocodeSearchRequest alloc] init];
    AMapGeoPoint *location = [[AMapGeoPoint alloc] init];


    if([params objectForKey: @"poitype"]) {
        NSString *poitype = [params objectForKey:@"poitype"];
        request.poitype = poitype;
    }

    if([params objectForKey: @"coordinate"]) {
        NSDictionary *coordinate = [params objectForKey: @"coordinate"];
        if ([coordinate objectForKey:@"latitude"] != nil
            && [coordinate objectForKey:@"longitude"] != nil) {
            location.latitude = [[coordinate objectForKey:@"latitude"] floatValue];
            location.longitude = [[coordinate objectForKey:@"longitude"] floatValue];
        } else {
            location.latitude = [[coordinate objectForKey:@"lat"] floatValue];
            location.longitude = [[coordinate objectForKey:@"lng"] floatValue];
        }
    }

    if([params objectForKey: @"radius"]) {
        NSInteger radius = [[params objectForKey:@"radius"] integerValue];
        request.radius = radius;
    }

    if([params objectForKey: @"requireExtension"]) {
        BOOL requireExtension = [[params objectForKey:@"requireExtension"] boolValue];
        request.requireExtension = requireExtension;
    }

    request.location = location;
    [self.search AMapReGoecodeSearch:request];
}



#pragma mark - Map Delegate

/**
 *  地图移动结束后调用此接口
 *
 *  @param mapView       地图view
 *  @param wasUserAction 标识是否是用户动作
 */
- (void)mapView:(MAMapView *)mapView mapDidMoveByUser:(BOOL)wasUserAction {
    RCTAMap *mapViewContainer = (RCTAMap *)mapView.superview;
    if(wasUserAction && mapViewContainer.onDidMoveByUser) {
        mapViewContainer.onDidMoveByUser(@{
                                  @"data": @{
                                          @"centerCoordinate": @{
                                                  @"latitude": @(mapView.centerCoordinate.latitude),
                                                  @"longitude": @(mapView.centerCoordinate.longitude),
                                                  }
                                          },
                                  });
    }
}

/**
 *  地图缩放结束后调用此接口
 *
 *  @param mapView       地图view
 *  @param wasUserAction 标识是否是用户动作
 */
- (void)mapView:(MAMapView *)mapView mapDidZoomByUser:(BOOL)wasUserAction {
    RCTAMap *mapViewContainer = (RCTAMap *)mapView.superview;
    if (!mapViewContainer.onMapZoomChange) {
        return;
    }


    double zoomLevel = mapView.zoomLevel;
    mapViewContainer.onMapZoomChange(@{
                              @"zoomLevel": [NSNumber numberWithDouble:zoomLevel]
                             });

}

/**
 *  单击地图底图调用此接口
 *
 *  @param mapView    地图View
 *  @param coordinate 点击位置经纬度
 */
- (void)mapView:(MAMapView *)mapView didSingleTappedAtCoordinate:(CLLocationCoordinate2D)coordinate {
    RCTAMap *mapViewContainer = (RCTAMap *)mapView.superview;
    if (!mapViewContainer.onSingleTapped) {
        return;
    }


    mapViewContainer.onSingleTapped(@{
       @"coordinate": @{
           @"latitude": @(coordinate.latitude),
           @"longitude": @(coordinate.longitude),
           }
    });
}

- (void)mapView:(MAMapView *)mapView didLongPressedAtCoordinate:(CLLocationCoordinate2D)coordinate {
    RCTAMap *mapViewContainer = (RCTAMap *)mapView.superview;
    if (!mapViewContainer.onLongTapped) {
        return;
    }


    mapViewContainer.onLongTapped(@{
                             @"coordinate": @{
                                     @"latitude": @(coordinate.latitude),
                                     @"longitude": @(coordinate.longitude),
                                     }
                             });
}


/*!
 @brief 根据anntation生成对应的View
 @param mapView 地图View
 @param annotation 指定的标注
 @return 生成的标注View
 */
- (MAAnnotationView*)mapView:(MAMapView *)mapView viewForAnnotation:(id <MAAnnotation>)annotation {

    if ([annotation isKindOfClass:[RCTAnnotation class]]) {
        static NSString *reusableId = @"pointReuseIndetifier";
        RCTAnnotation *rctAnnotation = (RCTAnnotation *)annotation;
        MAAnnotationView *annotationView = (MAAnnotationView *)[mapView dequeueReusableAnnotationViewWithIdentifier:reusableId];

        if (annotationView == nil) {
            annotationView = [[MAAnnotationView alloc] initWithAnnotation:annotation reuseIdentifier:reusableId];
        }

        annotationView.canShowCallout   = NO;
//        annotationView.draggable        = rctAnnotation.draggable;
//        annotationView.enabled          = rctAnnotation.draggable;
        if(rctAnnotation.hasAngle) {
            annotationView.image        = [[UIImage imageNamed: rctAnnotation.imageName] imageRotatedByDegrees: 360 -rctAnnotation.angle];
        } else {
            annotationView.image        = [UIImage imageNamed: rctAnnotation.imageName];
        }


        return annotationView;
    } else if(([annotation isKindOfClass:[RCTCustomAnnotation class]])) {
        static NSString *reusedId = @"customAnnoViewReuseIndetifier";
        RCTCustomAnnotation *customAnno = (RCTCustomAnnotation *)annotation;

        MAAnnotationView *customAnnoView = (MAAnnotationView *)[mapView dequeueReusableAnnotationViewWithIdentifier: reusedId];

        if (customAnnoView == nil) {
            NSString *customViewName = customAnno.customViewName;
            Class customViewClass = NSClassFromString(customViewName);
            if (customViewClass == nil) {
                return nil;
            }
            customAnnoView = [[[customViewClass class] alloc] initWithAnnotation: customAnno reuseIdentifier: reusedId];
        }

        customAnnoView.annotation = customAnno;

        return customAnnoView;
    }

    return nil;
}

- (void)mapView:(MAMapView *)mapView didSelectAnnotationView:(MAAnnotationView *)view {
    
    RCTAMap *mapViewContainer = (RCTAMap *)mapView.superview;
    if ([view.annotation isKindOfClass: [RCTCustomAnnotation class]]) {
        RCTCustomAnnotation *customAnno = view.annotation;
        Boolean disableSelectable = [[customAnno.customProps objectForKey: @"disableSelectable"] boolValue];
        if (disableSelectable) {
            [mapView deselectAnnotation: view.annotation animated: YES];
        } else {
            [view setSelected: YES animated: YES];
        }
        
        if (!mapViewContainer.onAnnotationClick) {
            return;
        }

        
        mapViewContainer.onAnnotationClick(@{@"customViewProps": customAnno.customProps});    }
}

- (void)mapView:(MAMapView *)mapView didDeselectAnnotationView:(MAAnnotationView *)view {
    if ([view.annotation isKindOfClass: [RCTCustomAnnotation class]]) {
        [view setSelected: NO animated: YES];
    }
}

- (void)mapView:(MAMapView *)mapView didAddAnnotationViews:(NSArray *)views {
    if (views.count == 0) {
        return ;
    }
    MAAnnotationView *annotationView = [views lastObject];
    RCTCustomAnnotation *customAnno = annotationView.annotation;
    if([customAnno isKindOfClass:[RCTCustomAnnotation class]]
       && [[customAnno.customProps objectForKey: @"isToTop"] boolValue]) {
        [mapView selectAnnotation: customAnno animated: YES];
    }
}

/*!
 @brief 拖动annotation view时view的状态变化，ios3.2以后支持
 @param mapView 地图View
 @param view annotation view
 @param newState 新状态
 @param oldState 旧状态
 */
- (void)mapView:(MAMapView *)mapView annotationView:(MAAnnotationView *)view didChangeDragState:(MAAnnotationViewDragState)newState fromOldState:(MAAnnotationViewDragState)oldState {
    RCTAMap *mapViewContainer = (RCTAMap *)mapView.superview;
    if (!mapViewContainer.onAnnotationDragChange) {
        return;
    }

    RCTAnnotation *annotation = (RCTAnnotation *)view.annotation;
    CLLocationCoordinate2D coordinate = annotation.coordinate;

    mapViewContainer.onAnnotationDragChange(@{
                             @"key": annotation.key,
                             @"coordinate": @{
                                     @"latitude": @(coordinate.latitude),
                                     @"longitude": @(coordinate.longitude),
                                     }
                             });
}

/*!
 @brief 根据overlay生成对应的Renderer
 @param mapView 地图View
 @param overlay 指定的overlay
 @return 生成的覆盖物Renderer
 */
- (MAOverlayRenderer *)mapView:(MAMapView *)mapView rendererForOverlay:(id <MAOverlay>)overlay {

    if ([overlay isKindOfClass:[RCTCircle class]]) {
        RCTCircle *circle = (RCTCircle *)overlay;

        MACircleRenderer *circleRender = [[MACircleRenderer alloc] initWithCircle: circle];

        if (circle.fillColor) {
            circleRender.fillColor = circle.fillColor;
        }

        if (circle.strokeColor) {
            circleRender.strokeColor = circle.strokeColor;
        }

        if (circle.lineWidth) {
            circleRender.lineWidth = circle.lineWidth;
        }

        return circleRender;
    } else if([overlay isKindOfClass:[RCTPolyline class]]) {
        RCTPolyline *polyline = (RCTPolyline *)overlay;
        MAPolylineRenderer *polylineRender = [[MAPolylineRenderer alloc] initWithPolyline:polyline];

        if (polyline.strokeColor) {
            polylineRender.strokeColor = polyline.strokeColor;
        }

        if (polyline.lineWidth) {
            polylineRender.lineWidth = polyline.lineWidth;
        }

        return polylineRender;
    } else if([overlay isKindOfClass:[RCTPolygon class]]) {
        RCTPolygon *polygon = (RCTPolygon *)overlay;
        MAPolygonRenderer *polygonRenderer = [[MAPolygonRenderer alloc] initWithPolygon:polygon];

        if (polygon.strokeColor) {
            polygonRenderer.strokeColor = polygon.strokeColor;
        }

        if (polygon.lineWidth) {
            polygonRenderer.lineWidth = polygon.lineWidth;
        }

        if (polygon.fillColor) {
            polygonRenderer.fillColor = polygon.fillColor;
        }

        return polygonRenderer;
    }

    return nil;
}

///*!
// @brief 当mapView新添加overlay renderer时调用此接口
// @param mapView 地图View
// @param renderers 新添加的overlay renderers
// */
//- (void)mapView:(RCTAMap *)mapView didAddOverlayRenderers:(NSArray *)renderers {
//
//}

#pragma mark - AMapSearchDelegate
/* 搜索失败回调. */
- (void)AMapSearchRequest:(id)request didFailWithError:(NSError *)error
{
    NSDictionary *result;
    result = @{
               @"error": @{
                            @"code": @(error.code),
                            @"localizedDescription": error.localizedDescription
                          }
               };

    NSString *eventName = @"amap.onPOISearchDone";
    if([request isKindOfClass:[AMapReGeocodeSearchRequest class]]) {
        eventName = @"amap.onReGeocodeSearchDone";
    }

    [self.bridge.eventDispatcher sendAppEventWithName:eventName
                                                 body:result];

}

/* POI 搜索回调. */
- (void)onPOISearchDone:(AMapPOISearchBaseRequest *)request response:(AMapPOISearchResponse *)response {

    NSDictionary *result;
    NSMutableArray *resultList;
    resultList = [NSMutableArray arrayWithCapacity:response.pois.count];
    if (response.pois.count > 0)
    {
        [response.pois enumerateObjectsUsingBlock:^(AMapPOI *obj, NSUInteger idx, BOOL *stop) {

            [resultList addObject:@{
                                    @"uid": obj.uid,
                                    @"name": obj.name,
                                    @"type": obj.type,
                                    @"typecode": obj.typecode,
                                    @"latitude": @(obj.location.latitude),
                                    @"longitude": @(obj.location.longitude),
                                    @"address": obj.address,
                                    @"tel": obj.tel,
                                    @"distance": @(obj.distance)
                                    }];

        }];
    }

    result = @{
                 @"list": resultList
                 };
    [self.bridge.eventDispatcher sendAppEventWithName:@"amap.onPOISearchDone"
                                                 body:result];
}

- (void)onReGeocodeSearchDone:(AMapReGeocodeSearchRequest *)request response:(AMapReGeocodeSearchResponse *)response {
    NSMutableDictionary *result = [NSMutableDictionary dictionary];
    [result setObjectSafeInAmap: response.regeocode.formattedAddress forKey: @"formattedAddress"];
    [result setObjectSafeInAmap: response.regeocode.addressComponent.province forKey: @"province"];
    [result setObjectSafeInAmap: response.regeocode.addressComponent.city forKey: @"city"];
    [result setObjectSafeInAmap: response.regeocode.addressComponent.district forKey: @"district"];
    [result setObjectSafeInAmap: response.regeocode.addressComponent.township forKey: @"township"];
    [result setObjectSafeInAmap: response.regeocode.addressComponent.neighborhood forKey: @"neighborhood"];
    [self.bridge.eventDispatcher sendAppEventWithName:@"amap.onReGeocodeSearchDone" body:result];
}

@end
