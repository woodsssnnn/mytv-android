package top.yogiczy.mytv.data.entities

import kotlinx.serialization.Serializable

/**
 * 频道节目
 */
@Serializable
data class EpgProgramme(
    /**
     * 开始时间（时间戳）
     */
    val startAt: Long = 0,

    /**
     * 结束时间（时间戳）
     */
    val endAt: Long = 0,

    /**
     * 节目名称
     */
    val title: String = "",
) {
    companion object {
        /**
         * 是否正在直播
         */
        fun EpgProgramme.isLive() = System.currentTimeMillis() in startAt..<endAt

        /**
         * 是否已播完（可回看）
         */
        fun EpgProgramme.isPast() = System.currentTimeMillis() >= endAt

        /**
         * 是否未播出（可预约）
         */
        fun EpgProgramme.isFuture() = System.currentTimeMillis() < startAt

        /**
         * 节目进度
         */
        fun EpgProgramme.progress() =
            (System.currentTimeMillis() - startAt).toFloat() / (endAt - startAt)
    }
}