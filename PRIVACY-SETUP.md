# 私有配置指南

## 1. 配置源链（必需）

```bash
cp source-private.properties.example source-private.properties
```

编辑 `source-private.properties`：

```properties
remote_source_urls=<你的m3u订阅地址1>,<镜像2>,<镜像3>
epg_fallback_urls=<你的EPG地址1>,<备选2>
```

- 逗号分隔多个镜像，按可达性排序；留空则只用内置兜底源
- 地址属于你自己的私有数据，不会进入 git

## 2. 内置离线兜底源（可选）

把你的精选 m3u 放到 `app/src/main/assets/old-man-tv.m3u`（gitignore 已覆盖，不会误提交）。
没有就跳过——应用在该级自动跳过。

## 3. 构建

```bash
./gradlew assembleRelease
# 产物: app/build/outputs/apk/release/app-release.apk
```

签名：项目用 `key.properties` 指向你自己的 keystore（同样不入库）。

## 4. 每日自动源（可选）

仓库自带 `daily-source` 工作流：抓公开池 → 实测连通性 → 生成精选源+瘦身EPG。
- 默认：产物上传为 workflow artifact（登录 GitHub 可见，仅自己）
- 进阶：仓库 Secrets 配 `DEST_REPO`(你的私有仓库) + `DEST_TOKEN`(对应 PAT)，产物自动推送到私有仓库
- 把私有仓库的 raw 地址填回第 1 步即完成闭环
