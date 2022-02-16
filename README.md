[![Build](https://github.com/way-zer/MindustryContents/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/way-zer/MindustryContents/actions/workflows/build.yml)
[![](https://jitpack.io/v/way-zer/MindustryContents.svg)](https://jitpack.io/#way-zer/MindustryContents)
# MindustryContentsLoader
一个`内容包`加载器的像素工厂MOD  
A Mindustry MOD to dynamicly load `Contents Pack`

## 功能 Features
* 接受服务器指令，为加载下张地图时，更换指定的`内容包`
* Receive Info from Server, load special `Contents Pack` when join server or change map.
* 为其他MOD提供接口，提供动态加载`内容包`的能力
* Provide API for other mods, provide feature to dynamicly load `Contents Pack`

## 内容包定义 Defination for `Contents Pack`
一组ContentList代码，没有属性，仅包含load函数，为原版Contents赋值  
A group of ContentList code, NO member, ONLY functionn `load` to assign new instances to original contents.

例子参见[洪水模式内容包](./contents/flood)  
For example, see [flood ContentPack](./contents/flood)

### maven引用 import from maven
供插件或其他项目引用  
For plugin or other projects to import.
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'cf.wayzer.MindustryContents:CONTENT_TYPE:VERSION'
}
```
* VERSION: 编译版本号 build version
* CONTENT_TYPE: 内容包类型(如flood) name of `Contents Pack`(ex `flood`)

## 安装 Setup
安装Release中的MOD即可(多人游戏兼容)  
Install mod in Release(multiplayer compatible)
