# 数字百团 Android 端

这是数字百团项目的原生 Android 学生端工程，采用 Android Studio 常见生产架构：

- Kotlin
- Jetpack Compose
- MVVM
- Clean Architecture 分层思路
- Hilt 依赖注入
- Retrofit + OkHttp 网络层
- DataStore 保存登录 token

> 说明：当前代码已经不依赖 `BuildConfig`。后端地址由 Gradle 写入 Android 资源
> `R.string.baituan_base_url`，再由 `AppConfig` 读取，所以你不需要在源码里寻找
> `BuildConfig.java` 或手动创建它。

## 当前已实现

- 学生登录：`/student/login`
- 学生注册：`/student/register`
- 发送邮箱验证码：`/student/validateEmail`
- token 自动携带：`X-Token` 与 `Authorization: Bearer <token>`
- 首页热门社团：`/group/top`
- 首页热门活动：`/activity/top`
- 社团列表与搜索：`/group/all`
- 社团详情：`/group/studentDetail`
- 申请加入社团：`/individualGroup/applyJoinGroup`
- 活动列表与搜索：`/activity/all`
- 活动详情：`/activity/studentDetail`
- 报名活动：`/individualActivity/joinActivity`
- 我的资料：`/student/profile`
- 我的社团：`/individualGroup/allGroups`
- 我的活动：`/individualActivity/allActivities`
- 我的入团申请：`/application/myApps`
- 退出登录：`/student/logout`
- 首页、社团、活动、我的页面下拉刷新
- 社团、活动、申请列表分页加载
- 社团/活动封面图展示

## 运行方式

### 1. 安装环境

需要安装：

- Android Studio
- Android SDK Platform 35
- Android SDK Build-Tools
- Android Emulator
- Android SDK Platform-Tools

JDK 建议使用 Android Studio 自带 JDK，或者本机已有的 JDK 17。

### 2. 打开工程

用 Android Studio 打开这个目录：

```text
D:\JavaPrograms\baituan\baituan_android
```

打开后等待 Gradle Sync 完成。如果 Android Studio 提示安装缺失 SDK，直接按提示安装。

### 3. 启动后端

先启动 Spring Boot 后端，确保后端运行在：

```text
http://localhost:8081
```

### 4. 使用模拟器运行

在 Android Studio 顶部设备选择器中创建或选择一个模拟器，然后点击 Run。

调试环境默认后端地址：

```text
http://10.0.2.2:8081/
```

这是 Android 模拟器访问电脑本机 `localhost` 的标准地址。

### 5. 使用真机运行

真机不能访问 `10.0.2.2`。如果使用手机连接同一个 Wi-Fi，需要把 debug 后端地址改成电脑局域网 IP，例如：

```properties
BAITUAN_DEBUG_BASE_URL=http://192.168.1.10:8081/
```

推荐把上面的配置写进本地 `gradle.properties` 或 Android Studio 的 Gradle 参数中，不要提交个人局域网 IP。

### 6. 生产环境打包

生产环境地址通过 Gradle 属性配置：

```properties
BAITUAN_RELEASE_BASE_URL=https://你的生产后端域名/
```

发布前需要保证生产地址启用 HTTPS，并关闭本地明文调试域名。

本工程已经支持 release 签名配置。签名文件和密码配置在本地生成，不提交到 Git：

```text
baituan_android/signing/baituan-upload.jks
baituan_android/keystore.properties
```

在 Android Studio 中选择：

```text
Build > Generate Signed App Bundle / APK > Android App Bundle
```

然后选择 release 变体即可生成用于应用市场的 AAB。

## 发布资源

已配置：

- 应用名称：数字百团
- 自适应应用图标：`@mipmap/ic_launcher`
- 圆形应用图标：`@mipmap/ic_launcher_round`
- Android 12+ 启动页图标和背景
- Release 签名配置模板
- 应用市场文案：`release/play-store-listing.md`
- 隐私政策模板：`release/privacy-policy-template.md`

## 后续建议

- 增加忘记密码页。
- 增加系统消息和审核通知。
- 增加空状态插画。
- 增加应用图标、启动页和正式签名配置。
- 将后端接口整理为 OpenAPI 文档，减少移动端字段猜测。
- 发布前替换 release `BASE_URL`，关闭明文 HTTP，启用 HTTPS。
