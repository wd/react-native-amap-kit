
#import "RCTAMapManager.h"
#import <MAMapKit/MAMapKit.h>


@interface RCTAMap : UIView

@property (nonatomic, copy) RCTBubblingEventBlock onDidMoveByUser;
@property (nonatomic, copy) RCTBubblingEventBlock onSingleTapped;
@property (nonatomic, copy) RCTBubblingEventBlock onLongTapped;
@property (nonatomic, copy) RCTBubblingEventBlock onMapZoomChange;
@property (nonatomic, copy) RCTBubblingEventBlock onAnnotationDragChange;
@property (nonatomic, copy) RCTBubblingEventBlock onAnnotationClick;


@property (nonatomic, strong) NSMutableDictionary *annotDict;
@property (nonatomic, strong) NSMutableDictionary *circleDict;
@property (nonatomic, strong) NSMutableDictionary *polylineDict;
@property (nonatomic, strong) NSMutableDictionary *polygonDict;

@property (nonatomic, strong) MAMapView *mapView;

@end
