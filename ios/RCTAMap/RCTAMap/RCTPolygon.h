//
//  RCTPolygon.h
//  RCTAMap
//
//  Created by breeze deng on 2017/11/16.
//  Copyright © 2017年 react-native-component. All rights reserved.
//

#import <MAMapKit/MAMapKit.h>

@interface RCTPolygon : MAPolygon

/*!
 @brief 每个Polyline唯一的key值
 */
@property (nonatomic, copy) NSString *key;

@property (strong) UIColor *strokeColor;

@property CGFloat lineWidth;

@property (strong) UIColor *fillColor;

@end
