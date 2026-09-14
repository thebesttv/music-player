# music-player

基于 Kotlin + Jetpack Compose 的 Android music player，支持：

- 通过系统分享（`ACTION_SEND` + `audio/*`）导入音频 URI
- 配置随机调度播放：例如 1 小时窗口内随机时间触发，并从音频随机位置开始播放指定时长
- 每次实际开始播放时，通过飞书 webhook 发送通知（JSON 结构参考 `feishu-ntfy` 文本消息）
- GitHub Actions：
  - 任意分支提交/PR 都会尝试编译（并运行 `:logic:test`）
  - `main` 分支每次提交都会自动构建并创建 release

## 本地构建

```bash
./gradlew :logic:test :app:assembleDebug
```
