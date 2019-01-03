//
//  RCTAnnotation.h
//  RCTAMap
//
//  Created by breeze deng on 2017/7/26.
//  Copyright © 2017年 react-native-component. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <MAMapKit/MAMapKit.h>

@interface RCTAnnotation : NSObject <MAAnnotation>


- (instancetype)initWithKey: (NSString *)key
                 coordinate:(CLLocationCoordinate2D)coordinate
                      title:(NSString *)title
                   subtitle:(NSString *)subtitle
                  imageName:(NSString *)imageName;


/*!
 @brief 每个Annotation唯一的key值
 */
@property (nonatomic, readonly, copy) NSString *key;

/*!
 @brief 标注view中心坐标
 */
@property (nonatomic, readonly) CLLocationCoordinate2D coordinate;

/*!
 @brief Annotation 图片名称
 */
@property (nonatomic, strong) NSString *imageName;

/*!
 @brief 获取annotation标题
 @return 返回annotation的标题信息
 */
@property (nonatomic, copy) NSString *title;

/*!
 @brief 获取annotation副标题
 @return 返回annotation的副标题信息
 */
@property (nonatomic, copy) NSString *subtitle;

/*!
 @brief 是否可以拖动
 */
@property (nonatomic) BOOL draggable;

/*!
 @brief 图片的角度
 */

@property (nonatomic) double angle;

@property (nonatomic) BOOL hasAngle;

/*!
 @brief 用户自定义信息
 */
@property (nonatomic, strong) NSDictionary *customProps;


/**
 @brief 设置标注的坐标，在拖拽时会被调用.
 @param newCoordinate 新的坐标值
 */
- (void)setCoordinate:(CLLocationCoordinate2D)newCoordinate;

@end
