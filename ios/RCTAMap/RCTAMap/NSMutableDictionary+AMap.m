//
//  NSMutableDictionary+AMap.m
//  RCTAMap
//
//  Created by breeze deng on 2017/12/8.
//  Copyright © 2017年 react-native-component. All rights reserved.
//

#import "NSMutableDictionary+AMap.h"

@implementation NSMutableDictionary (AMap)

- (void)setObjectSafeInAmap:(id)anObject forKey:(id<NSCopying>)aKey {
    if (nil != anObject) {
        [self setObject:anObject forKey:aKey];
    }
}

@end
