//
//  RCTCustomAnnotation.m
//  RCTAMap
//
//  Created by breeze deng on 2018/12/21.
//  Copyright © 2018 react-native-component. All rights reserved.
//

#import "RCTCustomAnnotation.h"

@implementation RCTCustomAnnotation

- (instancetype)initWithKey: (NSString *)key
                 coordinate:(CLLocationCoordinate2D)coordinate {
    if (self = [super init]) {
        _key = key;
        _coordinate = coordinate;
    }
    
    return self;
}


@end
