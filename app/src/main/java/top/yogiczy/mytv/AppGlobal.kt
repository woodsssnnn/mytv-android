package top.yogiczy.mytv

import android.content.Context
import java.io.File

/**
 * 应用全局变量
 */
object AppGlobal {
    /**
     * 缓存目录
     */
    lateinit var cacheDir: File

    /**
     * 应用上下文
     */
    lateinit var context: Context
}