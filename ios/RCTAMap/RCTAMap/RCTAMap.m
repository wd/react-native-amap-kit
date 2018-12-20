
#import "RCTAMap.h"

@implementation RCTAMap

- (void)setupMapView {
    self.mapView = [[MAMapView alloc] init];
    [self addSubview: self.mapView];
}

- (instancetype) init {
    if (self = [super init]) {
        [self setupMapView];
    }
    
    return self;
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame: frame]) {
        [self setupMapView];
    }
    
    return self;
}

- (instancetype)initWithCoder:(NSCoder *)aDecoder {
    if (self == [super initWithCoder: aDecoder]) {
        [self setupMapView];
    }
    
    return self;
}

- (NSMutableDictionary *)circleDict {
    if (_circleDict == nil) {
        _circleDict = [NSMutableDictionary dictionary];
    }

    return _circleDict;
}

- (NSMutableDictionary *)annotDict {
    if (_annotDict == nil) {
        _annotDict = [NSMutableDictionary dictionary];
    }

    return _annotDict;
}

- (NSMutableDictionary *)polylineDict {
    if (_polylineDict == nil) {
        _polylineDict = [NSMutableDictionary dictionary];
    }

    return _polylineDict;
}

- (NSMutableDictionary *)polygonDict {
    if(_polygonDict == nil) {
        _polygonDict = [NSMutableDictionary dictionary];
    }

    return _polygonDict;
}

- (void)layoutSubviews {
    [super layoutSubviews];
    
    self.mapView.frame = self.bounds;
}

@end
