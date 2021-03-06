# react-native-amap-kit
高德地图的 RN 扩展，支持各种图形，搜索，自定义 view 等。 最初的代码来自 [react-native-smart-amap](https://github.com/react-native-component/react-native-smart-amap) ，现在已经改动了很多了，那个项目似乎也没人维护了，所以起了一个新的名字发布出来。

支持的功能简单描述一下：
* 使用 typescript，定义了 types。
* 使用 3D 地图， 高德 2D 地图已经不怎么更新了，并且自定义地图样式只支持是 3D 地图。
* 支持通过设置 `props` 方式画图，或者通过 `this._amap.setCirle()` 这样的方式画图。
* 支持设置地图的自定义样式
* Android 支持设置自定义的浮动窗口，这个只实现了 android，实现方式也不灵活，后面可能会参考 [react-native-amap3d](https://github.com/qiuxiang/react-native-amap3d) 做一个重构。
* 支持地图查询和搜索

具体可以看看 `index.tsx` 里面 `CommonProps` 和 `AndroidProps` 的定义。

# 安装
```
$ yarn add react-native-amap-kit
$ react-native link react-native-amap-kit
```

## iOS
### CocoaPods
如果使用 CocoaPods。那么 link 会自动在 `ios/Podfile` 里面添加下面的设置

```
    pod 'react-native-amap-kit', path: '../node_modules/react-native-amap-kit'
```

这里有一个例子 https://github.com/wd/react-native-amap-kit-example/blob/master/ios/Podfile 。

之后执行 `pod install`。

### 没有使用 CocoaPods
如果没有使用，那 link 会自动给项目文件 `ios/XXXXX.xcodeproj/project.pbxproj` 增加配置，如果没有，那可能需要手动添加，找到对应目录拖到项目里面，具体就不多说了，推荐使用 pod 方式管理。

## Android
link 会修改类似下面的三个文件，一般不需要做其他事情了。如果有问题，那查看 react-native 官方的帮助或者参考其他模块吧。

` ` `
        modified:   android/app/build.gradle
        modified:   android/app/src/main/java/com/XXXXX/MainApplication.java
        modified:   android/settings.gradle
` ` `

# 配置高德地图的 key

先申请一个 key，参考[这里](https://lbs.amap.com/api/android-sdk/guide/create-project/get-key)。
如果只是测试一下，没有 key 也可以看到地图，以及使用一些功能，不过可能会出现不正常什么的。如果准备后续继续使用的话，最好还是申请一个。

## iOS
打开你项目 `AppDelegate.m` 文件，增加 `import ...` 和 `[AMapServices ...` 那两行，其中 `your key` 是前面申请的 key。

```
#import <AMapFoundationKit/AMapFoundationKit.h>

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
  [AMapServices sharedServices].apiKey = @"your key";
  
  NSURL *jsCodeLocation;
  ....
}
```

## Android

打开 `AndroidManifest.xml` 文件，在 `application` 段增加一个配置
```
<application>
   <meta-data
      android:name="com.amap.api.v2.apikey"
      android:value="your key" />
</application>
```

没有配置 key 或者配置错的话，在 logcat 日志里面可以看到类似下面的错误
```
2018-12-20 14:36:47.518 5562-5718/com.rnamapkitexample I/authErrLog: |json:{"status":"0","info":"INVALID_USER_KEY","infocode":"10001"}              |
2018-12-20 14:36:47.518 5562-5718/com.rnamapkitexample I/authErrLog: 请在高德开放平台官网中搜索"INVALID_USER_KEY"相关内容进行解决
2018-12-20 14:36:47.538 5562-5562/com.rnamapkitexample W/amapsdk: Key验证失败：[INVALID_USER_KEY]
```

# 使用

可以参考 [react-native-amap-kit-example](https://github.com/wd/react-native-amap-kit-example) 这个项目。

iOS 里面最新的 AMAP sdk 和 code-push 会有符号冲突，`aes_decrypt_key128` 和 `aes_encrypt_key128`，我的做法是通过 patch-package 给 code-push 打一个补丁，把这两个函数改了一下名字。

具体做法可以参考这里 https://wdicc.com/amap-work-with-code-push/ 。

# 升级

```
$ yarn add react-native-amap-kit
$ cd ios && rm Podfile.lock && pod install
```

# Authors
* @wd https://github.com/wd
* @haoxinlei1994 https://github.com/haoxinlei1994
* @chenmo230 https://github.com/chenmo230

# TODO
* 示例项目增加更多例子。
* 支持通过 childProps 方式添加图形。

# 其他项目
* https://github.com/qiuxiang/react-native-amap3d
* https://github.com/react-native-component/react-native-smart-amap
