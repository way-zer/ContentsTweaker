[![Build](https://github.com/way-zer/MindustryContents/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/way-zer/MindustryContents/actions/workflows/build.yml)
[![](https://jitpack.io/v/way-zer/MindustryContents.svg)](https://jitpack.io/#way-zer/MindustryContents)

# MindustryContentsLoader

一个`内容包`加载器的像素工厂MOD  
A Mindustry MOD to dynamically load `Contents Patch`

## 功能 Features

* 接受服务器指令，为加载下张地图时，更换指定的`内容补丁`
* Receive Info from Server, load special  `Contents Patch` when join server or change map.
* 为其他MOD提供接口，提供动态加载`内容补丁`的能力
* Provide API for other mods, provide feature to dynamically load `Contents Patch`

## 内容包定义 Definition for `Contents Pack`

~~一组ContentList代码，没有属性，仅包含load函数，为原版Contents赋值~~  
~~A group of ContentList code, NO member, ONLY functionn `load` to assign new instances to original contents.~~
(Removed since v2)

## 内容补丁 Definition for `Contents Patch`

一个(h)json文件，可以修改游戏内所有物品的属性  
A (h)json file. According to it modify all contents property.

客户端将会自动加载`config/contents-patch/default.(h)json`文件(如果存在)，  
并且根据地图信息或服务器指令，加载补丁(如果不存在，会自动从服务器下载)  
Client will auto load patch in `config/contents-patch/default.(h)json` (if exists)  
And will load patch according to map info or server command.(May auto download patch for server)

### 与内容包对比 vs `Contents Pack`

内容补丁为新功能，相比有诸多优势。支持服务器发送，支持离线游玩，抗sync，支持游戏中加载(无需重新加载地图)。
以及更好的Mod兼容性，使用Patch甚至可以修改其他Mod的属性。  
`Contents Patch` is a newer feature, many advantages: Support offline, Anti `sync`, Load when play(no need to reload world).
And better mod compatibility, you can even patch other mod content.

局限性：不能修改物品的类型; 不能使用java代码  
Limit: Can't change content type; Can't use java code.

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
* S->C ContentsLoader|loadPatch  
  命令客户端加载一个补丁(传递参数: 仅name)  
  command client to load a patch (param only name)
* C-> ContentsLoader|requestPatch  
  客户端找不到时，向服务器请求patch(传递参数: 仅name)  
  send when client not found this patch locally (param only name)
* S->C ContentsLoader|newPatch  
  命令客户端加载一个新补丁，通常作为`requestPatch`的回复(传递参数: name & content)
  command client to load a patch, normally as respond of `requestPatch` (params: name & content)
    * 特别的,若名字`$`开头视为可变patch,不会在客户端缓存
      specially, if name start with `$`, see it as mutable patch, don't cache it in client

通常来说，补丁名应该为其内容的hash，方便客户端进行缓存.
Normally patch's name should be a hash of content, which can be cached currently.

## 安装 Setup

安装Release中的MOD即可(多人游戏兼容)  
Install mod in Release(multiplayer compatible)
