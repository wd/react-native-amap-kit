//
//  RCTAnnotation.m
//  RCTAMap
//
//  Created by breeze deng on 2017/7/26.
//  Copyright © 2017年 react-native-component. All rights reserved.
//

#import "RCTAnnotation.h"

@implementation RCTAnnotation

- (instancetype)initWithKey: (NSString *)key
                 coordinate:(CLLocationCoordinate2D)coordinate
                      title:(NSString *)title
                   subtitle:(NSString *)subtitle
                  imageName:(NSString *)imageName {
    if (self = [super init]) {
        _key = key;
        _coordinate = coordinate;
        _title = title;
        _subtitle = subtitle;
        _imageName = imageName;
    }
    
    return self;
}

- (void)setCoordinate:(CLLocationCoordinate2D)newCoordinate {
    _coordinate = newCoordinate;
}

@end
