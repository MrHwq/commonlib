# 滑动返回
### 1. layout布局

```xml
<SwipeBackLayout
    android:id="@+id/swipe_refresh_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/shadow">
    <!--原本布局插入到这里，最外围添加swipebacklayout-->
</SwipeBackLayout>
```

> backgroud添加滑动时背景颜色，activity背景是透明的，不添加则显示下面activity颜色

### 2.Activty透明主题配置

> 配置透明主题

```xml
<activity
    android:name="XXXActivity"
    android:theme="@style/TranslucentTheme">
</activity>
```

> styles.xml添加该透明主题

```xml
<style name="TranslucentTheme" parent="Theme.AppCompat.Light.NoActionBar">
    <item name="android:windowBackground">@android:color/transparent</item>
    <item name="android:windowIsTranslucent">true</item>
</style>
```

### 3.配置滑动方向

> 默认左滑退出，调用setDragEdge可修改