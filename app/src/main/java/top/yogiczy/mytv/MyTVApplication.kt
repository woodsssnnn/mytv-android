package top.yogiczy.mytv

import android.app.Application
import android.content.Context
import top.yogiczy.mytv.ui.utils.SP

class MyTVApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        UnsafeTrustManager.enableUnsafeTrustManager()
        AppGlobal.cacheDir = applicationContext.cacheDir
        AppGlobal.context = applicationContext
        SP.init(applicationContext)
    }
}
