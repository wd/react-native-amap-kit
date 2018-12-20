//
//  RCTPolyline.h
//  RCTAMap
//
//  Created by breeze deng on 2017/7/26.
//  Copyright © 2017年 react-native-component. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <MAMapKit/MAMapKit.h>

@interface RCTPolyline : MAPolyline

/*!
 @brief 每个Polyline唯一的key值
 */
@property (nonatomic, copy) NSString *key;

@property (strong) UIColor *strokeColor;

@property CGFloat lineWidth;

@end
