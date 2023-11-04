[![Build](https://github.com/way-zer/ContentsTweaker/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/way-zer/ContentsTweaker/actions/workflows/build.yml)
[![](https://jitpack.io/v/way-zer/ContentsTweaker.svg)](https://jitpack.io/#way-zer/ContentsTweaker)

# ContentsTweaker (for Mindustry)

一个`内容包`加载器的像素工厂MOD  
A Mindustry MOD to dynamically load `Contents Patch`

## 功能 Features

* 接受服务器指令，为加载下张地图时，更换指定的`内容补丁`
* Receive Info from Server, load special  `Contents Patch` when join server or change map.
* 为其他MOD提供接口，提供动态加载`内容补丁`的能力
* Provide API for other mods, provide feature to dynamically load `Contents Patch`

## 内容补丁 Definition for `Contents Patch`

一个(h)json文件，可以修改游戏内所有物品的属性  
A (h)json file. According to it modify all contents property.

客户端将会自动加载`config/contents-patch/default.(h)json`文件(如果存在)，  
并且根据地图信息或服务器指令，加载补丁(如果不存在，会自动从服务器下载)  
Client will auto load patch in `config/contents-patch/default.(h)json` (if exists)  
And will load patch according to map info or server command.(May auto download patch for server)
### 示例 Exmaple

```json5
{
  //ContentType
  block: {
    //Content name
    "copper-wall-large": {
      //Property to modify
      //Value is format of origin json
      "health": 1200
    },
    "phase-wall": {
      "chanceDeflect": 0,
      "absorbLasers": true
    },
    "force-projector": {
      "shieldHealth": 2500
    },
    "lancer": {
      "shootType.damage": 30,
      "requirements": [
        "copper/10",
        "lead/100"
      ]
    },
  },
}
```

你也可以单行声明`key:value`形式(Since v2)
Or you can define it in single line like

```json5
"block.copper-wall-large.health" : 1200
```

### 网络协议 Protocal

* map tag: `ContentsPatch`  
  地图所需patch列表 List of patch names  
  例(For example): `flood;patchA;xxxx`
* map tag: `CT@{name}`  
  Patch内容. The Content of patch
* C->S ContentsLoader|version
  发送版本信息，可用来判断是否安装Mod (示例版本号: `2.0.1` or `beta-99`)
  Send version, also for checking installation. (version example: `2.0.1` or `beta-99`)
* ~~S->C ContentsLoader|loadPatch  
  命令客户端加载一个补丁(传递参数: 仅name)  
  command client to load a patch (param only name)~~
* C-> ContentsLoader|requestPatch  
  客户端找不到时，向服务器请求patch(传递参数: 仅name)  
  send when client not found this patch locally (param only name)
* S->C ContentsLoader|newPatch  
  命令客户端加载一个新补丁，通常作为`requestPatch`的回复，或动态Patch如`UIExt`相关(传递参数: name & content)
  Command client to load a patch, normally as respond of `requestPatch` (params: name & content)
    * 约定,若名字`$`开头视为可变patch, 否则应该为不可变patch
      Conventionally, if name start with `$`, see it as mutable patch, don't cache it in client

通常来说，补丁名应该为其内容的hash，方便客户端进行缓存.
Normally patch's name should be a hash of content, which can be cached currently.

## 安装 Setup

安装Release中的MOD即可(多人游戏兼容)  
Install mod in Release(multiplayer compatible)
