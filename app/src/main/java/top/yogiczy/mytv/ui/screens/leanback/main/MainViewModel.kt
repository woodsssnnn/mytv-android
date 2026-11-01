package top.yogiczy.mytv.ui.screens.leanback.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import top.yogiczy.mytv.data.entities.EpgList
import top.yogiczy.mytv.data.entities.IptvGroupList
import top.yogiczy.mytv.data.entities.IptvGroupList.Companion.iptvList
import top.yogiczy.mytv.data.repositories.epg.EpgRepository
import top.yogiczy.mytv.data.repositories.iptv.IptvRepository
import top.yogiczy.mytv.data.utils.Constants
import top.yogiczy.mytv.ui.utils.SP

class LeanbackMainViewModel : ViewModel() {
    private val iptvRepository = IptvRepository()
    private val epgRepository = EpgRepository()

    private val _uiState = MutableStateFlow<LeanbackMainUiState>(LeanbackMainUiState.Loading())
    val uiState: StateFlow<LeanbackMainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            refreshIptv()
            refreshEpg()
        }
    }

    private suspend fun refreshIptv() {
        // 多级源回退链：用户自设源 → BuildConfig 注入远程链 → 内置 asset 兜底
        val sourceUrls = buildList {
            if (SP.iptvSourceUrl != Constants.IPTV_SOURCE_URL) add(SP.iptvSourceUrl)
            addAll(Constants.IPTV_REMOTE_SOURCE_URLS)
            add(Constants.IPTV_SOURCE_URL)
        }.filter { it.isNotBlank() }

        for (sourceUrl in sourceUrls) {
            val result = kotlin.runCatching {
                var got: IptvGroupList? = null
                flow {
                    emit(
                        iptvRepository.getIptvGroupList(
                            sourceUrl = sourceUrl,
                            cacheTime = SP.iptvSourceCacheTime,
                            simplify = SP.iptvSourceSimplify,
                        )
                    )
                }
                    .retryWhen { _, attempt ->
                        if (attempt >= Constants.HTTP_RETRY_COUNT) return@retryWhen false
                        _uiState.value = LeanbackMainUiState.Loading(
                            "获取直播源(${attempt + 1}/${Constants.HTTP_RETRY_COUNT})..."
                        )
                        delay(Constants.HTTP_RETRY_INTERVAL)
                        true
                    }
                    .collect { got = it }
                got ?: error("直播源返回为空")
            }

            result.fold(
                onSuccess = { groupList ->
                    _uiState.value = LeanbackMainUiState.Ready(iptvGroupList = groupList)
                    SP.iptvSourceUrlHistoryList += sourceUrl
                    return
                },
                onFailure = {
                    SP.iptvSourceUrlHistoryList -= sourceUrl
                }
            )
        }

        _uiState.value = LeanbackMainUiState.Error(
            "直播源不可用。请按菜单键(或长按OK)打开设置配置直播源，" +
                    "或在电脑浏览器打开 http://<电视IP>:10481 推送直播源"
        )
    }

    private suspend fun refreshEpg() {
        if (_uiState.value is LeanbackMainUiState.Ready) {
            val iptvGroupList = (_uiState.value as LeanbackMainUiState.Ready).iptvGroupList

            // EPG 多地址回退 + 快速放弃（避免开机空转）
            val epgUrls = buildList {
                if (SP.epgXmlUrl != Constants.EPG_XML_URL) add(SP.epgXmlUrl)
                add(Constants.EPG_XML_URL)
                addAll(Constants.EPG_XML_FALLBACK_URLS)
            }.filter { it.isNotBlank() }.distinct()

            for (epgUrl in epgUrls) {
                val result = kotlin.runCatching {
                    var got: EpgList? = null
                    flow {
                        emit(
                            epgRepository.getEpgList(
                                xmlUrl = epgUrl,
                                filteredChannels = iptvGroupList.iptvList.map { it.channelName },
                                refreshTimeThreshold = SP.epgRefreshTimeThreshold,
                            )
                        )
                    }
                        .retry(Constants.EPG_RETRY_COUNT) { delay(Constants.HTTP_RETRY_INTERVAL); true }
                        .collect { got = it }
                    got ?: error("EPG返回为空")
                }

                result.fold(
                    onSuccess = { epgList ->
                        _uiState.value =
                            (_uiState.value as LeanbackMainUiState.Ready).copy(epgList = epgList)
                        SP.epgXmlUrlHistoryList += epgUrl
                        return
                    },
                    onFailure = { SP.epgXmlUrlHistoryList -= epgUrl }
                )
            }
            // EPG 全部失败不阻塞观看：保持空节目单
        }
    }
}

sealed interface LeanbackMainUiState {
    data class Loading(val message: String? = null) : LeanbackMainUiState
    data class Error(val message: String? = null) : LeanbackMainUiState
    data class Ready(
        val iptvGroupList: IptvGroupList = IptvGroupList(),
        val epgList: EpgList = EpgList(),
    ) : LeanbackMainUiState
}