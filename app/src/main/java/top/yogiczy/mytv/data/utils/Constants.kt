package top.yogiczy.mytv.data.utils

/**
 * 常量
 */
object Constants {
    /**
     * 应用 标题
     */
    const val APP_TITLE = "我的电视"

    /**
     * 应用 代码仓库
     */
    const val APP_REPO = "https://github.com/yaoxieyoulei/mytv-android"

    /**
     * IPTV源地址：APK 内置精选源（asset:// 协议），零网络依赖，开机即用
     * 文件不入库（合规边界），由使用者自行放入 app/src/main/assets/
     * 缺失时该兜底级自动跳过
     */
    const val IPTV_SOURCE_URL = "asset://old-man-tv.m3u"

    /**
     * 每日再生远程精选源（构建期由 source-private.properties 注入，公开构建为空链）
     * 应用加载顺序：用户面板自设源 → 本链逐个尝试 → 内置 asset 兜底
     */
    val IPTV_REMOTE_SOURCE_URLS: List<String> =
        top.yogiczy.mytv.BuildConfig.REMOTE_SOURCE_URLS.toList()

    /**
     * 节目单默认地址（构建期注入链的首位；空=不启用远程EPG）
     */
    val EPG_XML_URL: String
        get() = top.yogiczy.mytv.BuildConfig.EPG_FALLBACK_URLS.firstOrNull() ?: ""

    /**
     * 节目单备选地址
     */
    val EPG_XML_FALLBACK_URLS: List<String>
        get() = top.yogiczy.mytv.BuildConfig.EPG_FALLBACK_URLS.drop(1)

    /**
     * IPTV源缓存时间（毫秒）
     */
    const val IPTV_SOURCE_CACHE_TIME = 1000 * 60 * 60 * 24L // 24小时

    /**
     * 节目单刷新时间阈值（小时）
     */
    const val EPG_REFRESH_TIME_THRESHOLD = 2 // 不到2点不刷新

    /**
     * 节目单重试次数（适老化：快速放弃，避免开机空转半分钟）
     */
    const val EPG_RETRY_COUNT = 3L

    /**
     * Git最新版本信息
     */
    const val GIT_RELEASE_LATEST_URL =
        "https://api.github.com/repos/yaoxieyoulei/mytv-android/releases/latest"

    /**
     * GitHub加速代理地址
     */
    const val GITHUB_PROXY = "https://mirror.ghproxy.com/"

    /**
     * HTTP请求重试次数
     */
    const val HTTP_RETRY_COUNT = 10L

    /**
     * HTTP请求重试间隔时间（毫秒）
     */
    const val HTTP_RETRY_INTERVAL = 3000L

    /**
     * 播放器 userAgent
     */
    const val VIDEO_PLAYER_USER_AGENT = "ExoPlayer"

    /**
     * 日志历史最大保留条数
     */
    const val LOG_HISTORY_MAX_SIZE = 50

    /**
     * 播放器加载超时（适老化：快速失败换线路，环形重试不停机）
     */
    const val VIDEO_PLAYER_LOAD_TIMEOUT = 1000L * 8 // 8秒

    /**
     * 界面 超时未操作自动关闭界面
     */
    const val UI_SCREEN_AUTO_CLOSE_DELAY = 1000L * 15 // 15秒

    /**
     * 界面 时间显示前后范围
     */
    const val UI_TIME_SHOW_RANGE = 1000L * 30 // 前后30秒

    /**
     * 界面 临时面板界面显示时间
     */
    const val UI_TEMP_PANEL_SCREEN_SHOW_DURATION = 1500L // 1.5秒
}