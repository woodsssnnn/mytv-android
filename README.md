<div align="center">

# 电视直播 · 适老版

**基于开源项目 [mytv-android](https://github.com/yaoxieyoulei/mytv-android) (MIT) 的适老化定制分支**

`Android TV` · `长辈模式` · `数字选台` · `永续播放` · `工具公开 / 数据私有`

</div>

---

## 这是什么

为家里老人定制的电视直播播放器。目标只有一句话：**开机即播、遥控器怎么按都不坏、断流自己恢复**。

在保持上游简洁架构的基础上做了三层改造：

| 层 | 改造内容 |
|---|---|
| 适老化 | 开机自启+频道记忆、数字键选台（列表位置=传统频道号）、长辈模式（设置入口全屏蔽，仅网页面板可开）、字体放大、经典三列选台面板 |
| 播放稳定性 | 线路**环形重试**（全部线路失败后回到第 1 条继续，永不停机）、断流 8 秒快切、多级源回退链（面板自设源→注入远程链→内置兜底→引导页）、EPG 多地址回退+快速放弃 |
| 运维 | 网页面板 `http://电视IP:10481`：开关长辈模式 / 推送源 / 推送 EPG / 查看日志 |

## 合规边界（重要）

本仓库**只含工具，不含数据**：

- ❌ 不内置、不提交任何频道直播流地址、EPG 数据、私源 URL
- ✅ 源码、数据管道脚本（公开池抓取→实测连通性→筛选）、工作流全部公开
- ✅ 私有数据通过 `source-private.properties`（gitignored）在构建期注入，填入你自己的源即得完整版

详见 [ARCHITECTURE.md](ARCHITECTURE.md) 与 [PRIVACY-SETUP.md](PRIVACY-SETUP.md)。

> 仓库 Releases 提供的是**公开干净版** APK（零数据，安装后显示配置引导）。
> 填好私有配置自行构建，即得到完整体验。

## 遥控器（教老人三句话）

- **换台**：上下方向键，或数字键直选（1=CCTV1、13=CCTV-13 新闻、19+=卫视）
- **看所有台**：OK 键打开频道列表，长按 OK 收藏
- **按乱了**：按返回键退出当前界面；长辈模式下菜单键/长按 OK 只会打开频道列表，坏不了

## 接力（fork 者看这里）

```bash
git clone -b oldman https://github.com/woodsssnnn/mytv-android.git
cp source-private.properties.example source-private.properties   # 填你自己的源
# 可选：放离线兜底源到 app/src/main/assets/old-man-tv.m3u（gitignored）
./gradlew assembleRelease
```

每日数据管道（可选）：启用 `daily-source` 工作流，自动抓公开池→实测连通性→生成精选源+瘦身 EPG。

## 许可与致谢

- 上游 [yaoxieyoulei/mytv-android](https://github.com/yaoxieyoulei/mytv-android)（MIT），本项目延续其 MIT 许可，原项目版权声明与 LICENSE 文件完整保留
- 数据管道思路参考社区生态：[Guovin/iptv-api](https://github.com/Guovin/iptv-api)、[iptv-org/iptv](https://github.com/iptv-org/iptv)、[fanmingming/live](https://github.com/fanmingming/live)
- 仅供个人学习与家庭使用；频道数据的合法性由使用者自行判断与承担，与上游作者无关

## 免责声明

本项目为个人学习性质的家庭定制工具，不存储、不分发任何受版权保护的音视频内容；
仓库内不包含任何频道地址数据。使用者通过自行配置的第三方地址访问的内容，
其合法性与可用性由使用者自行负责。
