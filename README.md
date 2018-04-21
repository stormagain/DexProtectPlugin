# DexProtectPlugin
能自动实现Apk加固的Gradle插件

# 背景
很多开发者都是使用第三方加固服务，这些加固基本上都是面向Apk文件的加固，可能还涉及到重新签名，感觉挺麻烦。
也许我们需要一种加固服务，让自己可以控制核心的加解密算法，也不需要将原始apk暴露给第三方平台，
而且伴随着Gradle构建Apk的过程就自动产生了加固后的Apk，听起来是不是有趣呢？

# 现有Feature
1：一代加固方案（后续会努力实现二代三代加固方案，从目前来看，知识储备有限，距离VMP加固方案还比较远，有研究的朋友可以联系我）
2：Gradle自动生成加固Apk

# 预计下个版本支持的Feature
1: multiDex
2: Dex文件不落地加载

# 灵感来源(感谢开源精神)
1:Tinker(https://github.com/Tencent/tinker)
2:ApkToolPlus(https://github.com/linchaolong/ApkToolPlus)
