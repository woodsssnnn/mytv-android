# 架构（工具公开 / 数据私有）

本仓库是 [mytv-android](https://github.com/yaoxieyoulei/mytv-android) (MIT) 的适老化定制分支。
核心设计：**播放器与工具公开，直播源/节目单等数据私有**——公开仓库零数据，任何人可接力。

## 合规边界

| 入库 | 不入库（gitignore） |
|---|---|
| 播放器源码、构建/探测/筛选脚本、工作流、文档 | 频道流地址(m3u)、EPG 产物、私源 URL、签名密钥 |

## 源加载链（App 启动）

```
用户面板自设源 → BuildConfig 注入远程链(逐个) → 内置 asset 兜底 → Error 态引导配置
```

- `source-private.properties`（gitignore）：构建期注入 `remote_source_urls` / `epg_fallback_urls`（逗号分隔多镜像）
- `app/src/main/assets/old-man-tv.m3u`（gitignore）：离线兜底源，缺失则该级自动跳过
- 公开构建 = 空链无 asset = 启动显示配置引导；填好私有配置构建 = 完整体验

## 每日数据管道（可选启用）

`.github/workflows/daily-source.yml`：抓公开池 → 20 并发实测连通性 → 生成精选源+瘦身EPG。
产物默认上传 artifact（仅登录可见）；配置 Secrets `DEST_REPO`/`DEST_TOKEN` 可推送到你自己的私有仓库。

## 接力步骤（fork 者看这里）

1. fork 本仓库
2. `cp source-private.properties.example source-private.properties`，填你自己的源地址
3. （可选）把每日精选源文件放入 `app/src/main/assets/old-man-tv.m3u` 作离线兜底
4. `./gradlew assembleRelease`（签名配置见 key.properties.example……用你自己的密钥）
5. （可选）启用 daily-source 工作流自动维护数据
