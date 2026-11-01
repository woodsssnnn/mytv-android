package top.yogiczy.mytv.ui.screens.leanback.main.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yogiczy.mytv.data.entities.EpgProgramme
import top.yogiczy.mytv.data.entities.Iptv
import top.yogiczy.mytv.data.entities.IptvGroupList
import top.yogiczy.mytv.data.entities.IptvGroupList.Companion.iptvIdx
import top.yogiczy.mytv.data.entities.IptvGroupList.Companion.iptvList
import top.yogiczy.mytv.data.utils.CatchupHelper
import top.yogiczy.mytv.data.utils.Constants
import top.yogiczy.mytv.ui.screens.leanback.toast.LeanbackToastState
import top.yogiczy.mytv.ui.screens.leanback.video.LeanbackVideoPlayerState
import top.yogiczy.mytv.ui.screens.leanback.video.rememberLeanbackVideoPlayerState
import top.yogiczy.mytv.ui.utils.SP
import top.yogiczy.mytv.utils.Loggable
import kotlin.math.max

@Stable
class LeanbackMainContentState(
    coroutineScope: CoroutineScope,
    private val videoPlayerState: LeanbackVideoPlayerState,
    private val iptvGroupList: IptvGroupList,
) : Loggable() {
    private var _currentIptv by mutableStateOf(Iptv())
    val currentIptv get() = _currentIptv

    private var _currentIptvUrlIdx by mutableIntStateOf(0)
    val currentIptvUrlIdx get() = _currentIptvUrlIdx

    private var _isPanelVisible by mutableStateOf(false)
    var isPanelVisible
        get() = _isPanelVisible
        set(value) {
            _isPanelVisible = value
        }

    private var _isSettingsVisible by mutableStateOf(false)
    var isSettingsVisible
        get() = _isSettingsVisible
        set(value) {
            _isSettingsVisible = value
        }

    private var _isTempPanelVisible by mutableStateOf(false)
    var isTempPanelVisible
        get() = _isTempPanelVisible
        set(value) {
            _isTempPanelVisible = value
        }

    /** 换台黑幕：换台/换线路到新画面就绪前用黑色遮盖，避免老人误看残留帧 */
    private var _isSwitchingBlackout by mutableStateOf(false)
    val isSwitchingBlackout get() = _isSwitchingBlackout

    /** 回看播放中（此时不参与线路环形轮换，失败即回直播） */
    private var _isCatchupPlayback by mutableStateOf(false)
    val isCatchupPlayback get() = _isCatchupPlayback

    /** 长辈首页：长辈模式下开机显示四大卡片（直播在背后继续播） */
    private var _isHomeVisible by mutableStateOf(SP.appElderMode)
    var isHomeVisible
        get() = _isHomeVisible
        set(value) {
            _isHomeVisible = value
        }

    private var _isQuickPanelVisible by mutableStateOf(false)
    var isQuickPanelVisible
        get() = _isQuickPanelVisible
        set(value) {
            _isQuickPanelVisible = value
        }

    init {
        changeCurrentIptv(iptvGroupList.iptvList.getOrElse(SP.iptvLastIptvIdx) {
            iptvGroupList.firstOrNull()?.iptvList?.firstOrNull() ?: Iptv()
        })

        videoPlayerState.onReady {
            _isSwitchingBlackout = false
            coroutineScope.launch {
                val name = _currentIptv.name
                val urlIdx = _currentIptvUrlIdx
                delay(Constants.UI_TEMP_PANEL_SCREEN_SHOW_DURATION)
                if (name == _currentIptv.name && urlIdx == _currentIptvUrlIdx) {
                    _isTempPanelVisible = false
                }
            }

            // 记忆可播放的域名
            SP.iptvPlayableHostList += getUrlHost(_currentIptv.urlList[_currentIptvUrlIdx])
        }

        videoPlayerState.onError {
            // 回看播放失败：退出回看返回直播（回看 URL 不参与线路轮换）
            if (_isCatchupPlayback) {
                _isCatchupPlayback = false
                coroutineScope.launch {
                    delay(1500)
                    changeCurrentIptv(_currentIptv, 0)
                }
                return@onError
            }

            // 适老改造：线路列表环形重试——全部线路失败后回到第1条继续，
            // 永不停机（配合「可播放域名记忆」，通常第二轮就恢复）
            if (_currentIptv.urlList.isNotEmpty()) {
                changeCurrentIptv(
                    _currentIptv,
                    (_currentIptvUrlIdx + 1) % _currentIptv.urlList.size,
                )
            }

            // 从记忆中删除不可播放的域名
            SP.iptvPlayableHostList -= getUrlHost(_currentIptv.urlList[_currentIptvUrlIdx])
        }

        videoPlayerState.onCutoff {
            if (_isCatchupPlayback) return@onCutoff // 回看断流由 onError 兜底
            // 断流优先重连当前线路；连续失败会自然滑向环形换线路
            changeCurrentIptv(_currentIptv, _currentIptvUrlIdx)
        }
    }

    /**
     * 回看指定频道的指定节目（需该频道源带 catchup 属性）。
     * 若频道非当前播放频道会先切台；失败自动回直播。
     */
    fun playCatchup(iptv: Iptv, programme: EpgProgramme) {
        if (iptv != _currentIptv) changeCurrentIptv(iptv)
        val url = CatchupHelper.build(_currentIptv, programme.startAt / 1000, programme.endAt / 1000)
        if (url == null) {
            LeanbackToastState.I.showToast("当前频道不支持回看")
            return
        }
        _isCatchupPlayback = true
        _isSwitchingBlackout = true
        _isPanelVisible = false
        videoPlayerState.prepare(url)
        LeanbackToastState.I.showToast("正在回看：${programme.title}")
    }

    /** 预约节目：到点浮层提醒 */
    fun remindProgramme(iptv: Iptv, programme: EpgProgramme) {
        val item = "${iptv.name}|${programme.startAt}|${programme.title}"
        SP.epgRemindList = SP.epgRemindList + item
        LeanbackToastState.I.showToast("好的，节目开始时会提醒您")
    }

    private fun getPrevIptv(): Iptv {
        val currentIndex = iptvGroupList.iptvIdx(_currentIptv)
        return iptvGroupList.iptvList.getOrElse(currentIndex - 1) {
            iptvGroupList.lastOrNull()?.iptvList?.lastOrNull() ?: Iptv()
        }
    }

    private fun getNextIptv(): Iptv {
        val currentIndex = iptvGroupList.iptvIdx(_currentIptv)
        return iptvGroupList.iptvList.getOrElse(currentIndex + 1) {
            iptvGroupList.firstOrNull()?.iptvList?.firstOrNull() ?: Iptv()
        }
    }

    fun changeCurrentIptv(iptv: Iptv, urlIdx: Int? = null) {
        // 只有换台才关面板；同频道换线路（含自动重试）保持面板，
        // 否则流不稳时重试会反复把用户打开的节目单/面板关掉
        if (iptv != _currentIptv) _isPanelVisible = false
        _isCatchupPlayback = false

        if (iptv == _currentIptv && urlIdx == null) return

        if (iptv == _currentIptv && urlIdx != _currentIptvUrlIdx) {
            SP.iptvPlayableHostList -= getUrlHost(_currentIptv.urlList[_currentIptvUrlIdx])
        }

        _isTempPanelVisible = true
        _isSwitchingBlackout = true

        _currentIptv = iptv
        SP.iptvLastIptvIdx = iptvGroupList.iptvIdx(_currentIptv)

        _currentIptvUrlIdx = if (urlIdx == null) {
            // 优先从记忆中选择可播放的域名
            max(0, _currentIptv.urlList.indexOfFirst {
                SP.iptvPlayableHostList.contains(getUrlHost(it))
            })
        } else {
            (urlIdx + _currentIptv.urlList.size) % _currentIptv.urlList.size
        }

        val url = iptv.urlList[_currentIptvUrlIdx]
        log.d("播放${iptv.name}（${_currentIptvUrlIdx + 1}/${_currentIptv.urlList.size}）: $url")

        videoPlayerState.prepare(url)
    }

    fun changeCurrentIptvToPrev() {
        changeCurrentIptv(getPrevIptv())
    }

    fun changeCurrentIptvToNext() {
        changeCurrentIptv(getNextIptv())
    }
}

@Composable
fun rememberLeanbackMainContentState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    videoPlayerState: LeanbackVideoPlayerState = rememberLeanbackVideoPlayerState(),
    iptvGroupList: IptvGroupList = IptvGroupList(),
) = remember {
    LeanbackMainContentState(
        coroutineScope = coroutineScope,
        videoPlayerState = videoPlayerState,
        iptvGroupList = iptvGroupList,
    )
}

private fun getUrlHost(url: String): String {
    return url.split("://").getOrElse(1) { "" }.split("/").firstOrNull() ?: url
}