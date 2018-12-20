# react-native-amap-kit
高德地图的 RN 扩展，支持各种图形，搜索，自定义 view 等。 最初的代码来自 [react-native-smart-amap](https://github.com/react-native-component/react-native-smart-amap) ，现在已经改动了很多了，那个项目似乎也没人维护了，所以起了一个新的名字发布出来。

# 安装
```
$ yarn add react-native-amap-kit
$ react-native link react-native-amap-kit

## iOS
需要先配置 CocoaPods。然后在 `ios/Podfile` 里面添加下面两个设置

```
    pod 'react-native-amap-kit', path: '../node_modules/react-native-amap-kit/ios'  ### add this line

```
和
```
  installer.pods_project.targets.each do |target|
    if target.name == 'Pods-RNAmapKitExample'
      target.build_configurations.each do |config|
        # fix amap
        xcconfig_path = config.base_configuration_reference.real_path
        build_settings = Hash[*File.read(xcconfig_path).lines.map{|x| x.split(/\s*=\s*/, 2)}.flatten]
        build_settings['OTHER_LDFLAGS'][' -l"stdc++.6.0.9"'] = ''
        File.open(xcconfig_path, "w") do |file|
          build_settings.each do |key,value|
            file.puts "#{key} = #{value}"
          end
        end
      end
    end
  end
```
这里有一个例子 https://github.com/wd/react-native-amap-kit-example/tree/master/ios 。

## Android
不需要做其他事情了。

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