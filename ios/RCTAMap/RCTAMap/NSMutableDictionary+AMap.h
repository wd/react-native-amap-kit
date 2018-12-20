//
//  NSMutableDictionary+AMap.h
//  RCTAMap
//
//  Created by breeze deng on 2017/12/8.
//  Copyright © 2017年 react-native-component. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface NSMutableDictionary (AMap)

- (void)setObjectSafeInAmap:(id)anObject forKey:(id<NSCopying>)aKey;

@end
