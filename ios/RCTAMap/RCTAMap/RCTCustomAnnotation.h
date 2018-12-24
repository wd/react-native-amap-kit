//
//  RCTCustomAnnotation.h
//  RCTAMap
//
//  Created by breeze deng on 2018/12/21.
//  Copyright © 2018 react-native-component. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <MAMapKit/MAMapKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface RCTCustomAnnotation : NSObject<MAAnnotation>

- (instancetype)initWithKey: (NSString *)key
                 coordinate:(CLLocationCoordinate2D)coordinate;

/*!
 @brief 每个Annotation唯一的key值
 */
@property (nonatomic, readonly, copy) NSString *key;

///标注view中心坐标
@property (nonatomic, readonly) CLLocationCoordinate2D coordinate;

@property (nonatomic, strong) NSDictionary *customProps;

@property (nonatomic, strong) NSString *customViewName;

@end

NS_ASSUME_NONNULL_END
