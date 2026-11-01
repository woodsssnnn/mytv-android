package top.yogiczy.mytv.data.utils

import top.yogiczy.mytv.data.entities.Iptv
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 回看地址构造（m3u catchup 社区约定的六种模式）。
 * 语义基准：Kodi pvr.iptvsimple；实现参考 guangyu CatchupHelper 思路，自实现。
 *
 * 占位符：{utc}/{start}=开始秒  {utcend}/{end}=结束秒  {lutc}/{now}=当前秒
 * {duration}=时长秒  {Y}{m}{d}{H}{M}{S}=开始时间分量  {utc:格式}=自定义格式
 */
object CatchupHelper {

    fun isSupported(iptv: Iptv): Boolean =
        iptv.catchupSource.isNotBlank() && iptv.catchupMode.isNotBlank()

    /** 构造回看地址；返回 null = 不支持 */
    fun build(iptv: Iptv, startUtcSec: Long, endUtcSec: Long): String? {
        if (!isSupported(iptv)) return null
        val liveUrl = iptv.urlList.firstOrNull() ?: return null
        val now = System.currentTimeMillis() / 1000
        val duration = (endUtcSec - startUtcSec).coerceAtLeast(0)

        return when (iptv.catchupMode.lowercase()) {
            "append" -> liveUrl + (if (liveUrl.contains("?")) "&" else "?") +
                fill(iptv.catchupSource, startUtcSec, endUtcSec, now)

            "shift" -> "$liveUrl?utc=$startUtcSec&lutc=$now"

            "flussonic" -> flussonic(liveUrl, startUtcSec, duration, hls = false)
            "flussonic-hls", "fs" -> flussonic(liveUrl, startUtcSec, duration, hls = true)

            "xc" -> xtream(liveUrl, startUtcSec, duration)

            "pltv" -> pltv(liveUrl, startUtcSec, endUtcSec)

            else -> fill(iptv.catchupSource, startUtcSec, endUtcSec, now)
        }
    }

    /** default：catchup-source 即完整模板，替换占位符 */
    private fun fill(
        template: String,
        start: Long,
        end: Long,
        now: Long,
    ): String {
        val d = Date(start * 1000)
        var s = template
            .replace("{utc}", start.toString()).replace("{start}", start.toString())
            .replace("{utcend}", end.toString()).replace("{end}", end.toString())
            .replace("{lutc}", now.toString()).replace("{now}", now.toString())
            .replace("{duration}", ((end - start).coerceAtLeast(0)).toString())
        s = Regex("\\{utc:([^}]+)\\}").replace(s) { m ->
            SimpleDateFormat(m.groupValues[1], Locale.getDefault()).format(d)
        }
        val cal = Calendar.getInstance().apply { time = d }
        val parts = mapOf(
            "{Y}" to String.format(Locale.getDefault(), "%04d", cal.get(Calendar.YEAR)),
            "{m}" to String.format(Locale.getDefault(), "%02d", cal.get(Calendar.MONTH) + 1),
            "{d}" to String.format(Locale.getDefault(), "%02d", cal.get(Calendar.DAY_OF_MONTH)),
            "{H}" to String.format(Locale.getDefault(), "%02d", cal.get(Calendar.HOUR_OF_DAY)),
            "{M}" to String.format(Locale.getDefault(), "%02d", cal.get(Calendar.MINUTE)),
            "{S}" to String.format(Locale.getDefault(), "%02d", cal.get(Calendar.SECOND)),
        )
        parts.forEach { (t, v) -> s = s.replace(t, v) }
        Regex("\\{offset:(\\d+)\\}").replace(s) { m -> (now - m.groupValues[1].toLong()).toString() }
        return s
    }

    /** flussonic: http://h/stream.m3u8 -> http://h/stream-archive-{start}-{duration}.m3u8 */
    private fun flussonic(liveUrl: String, start: Long, duration: Long, hls: Boolean): String {
        val path = liveUrl.substringBeforeLast("/")
        val stream = liveUrl.substringAfterLast("/").substringBefore(".")
        val mode = if (hls) "archive" else "timeshift_abs"
        val ext = if (hls) "m3u8" else "ts"
        return "$path/$stream-$mode-$start-$duration.$ext"
    }

    /**
     * Xtream: http://host:port/live/USER/PASS/ID.m3u8
     * -> http://host:port/timeshift/USER/PASS/{分钟}/{yyyy-MM-dd:HH-mm}/{ID}.ts
     * 注意：xc 的 start 用本地墙钟时间（时区陷阱），duration 单位是分钟
     */
    private fun xtream(liveUrl: String, startUtcSec: Long, durationSec: Long): String {
        val m = Regex("^(https?://[^/]+)/live/([^/]+)/([^/]+)/(\\d+)(?:\\.m3u8|\\.ts)?$")
            .find(liveUrl) ?: return liveUrl
        val (host, user, pass, id) = m.destructured
        val df = SimpleDateFormat("yyyy-MM-dd:HH-mm", Locale.getDefault())
        val durMin = (durationSec / 60).coerceAtLeast(1)
        return "$host/timeshift/$user/$pass/$durMin/${df.format(Date(startUtcSec * 1000))}/$id.ts"
    }

    /** 中国电信 PLTV: /PLTV/... -> /TVOD/... + playseek=yyyyMMddHHmmss-yyyyMMddHHmmss(GMT+8) */
    private fun pltv(liveUrl: String, startUtcSec: Long, endUtcSec: Long): String {
        if (!liveUrl.contains("/PLTV/", ignoreCase = true)) return liveUrl
        val df = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        df.timeZone = java.util.TimeZone.getTimeZone("GMT+8")
        val playseek = "${df.format(Date(startUtcSec * 1000))}-${df.format(Date(endUtcSec * 1000))}"
        val sep = if (liveUrl.contains("?")) "&" else "?"
        return liveUrl.replace("/PLTV/", "/TVOD/", ignoreCase = true) + "${sep}playseek=$playseek"
    }
}
