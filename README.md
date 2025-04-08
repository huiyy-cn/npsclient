# npsclient

A NPC client for Android.

一个用于 Android 的 NPC 客户端。

---

## 📦 How to Use / 使用方法

1. Download the NPC binary for your target architecture from [NPS](https://github.com/djylb/nps).

2. Rename it to `libnpc.so` and put it into:

   将对应架构的 NPC 可执行文件重命名为 `libnpc.so`，并放入：

   ```
   app/src/main/jniLibs/<abi>/libnpc.so
   ```

   Example / 例如：

   ```
   app/src/main/jniLibs/armeabi-v7a/libnpc.so
   app/src/main/jniLibs/arm64-v8a/libnpc.so
   app/src/main/jniLibs/x86_64/libnpc.so
   ```

3. Open with Android Studio and build.

   使用 Android Studio 打开项目并编译运行。

---

## 📚 References / 参考项目

- [AceDroidX/frp-Android](https://github.com/AceDroidX/frp-Android)
- [djylb/nps](https://github.com/djylb/nps)
