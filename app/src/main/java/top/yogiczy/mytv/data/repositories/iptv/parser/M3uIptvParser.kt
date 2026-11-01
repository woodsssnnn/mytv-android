package top.yogiczy.mytv.data.repositories.iptv.parser

import top.yogiczy.mytv.data.entities.Iptv
import top.yogiczy.mytv.data.entities.IptvGroup
import top.yogiczy.mytv.data.entities.IptvGroupList
import top.yogiczy.mytv.data.entities.IptvList

class M3uIptvParser : IptvParser {

    override fun isSupport(url: String, data: String): Boolean {
        return data.startsWith("#EXTM3U")
    }

    override suspend fun parse(data: String): IptvGroupList {
        val lines = data.split("\r\n", "\n")
        val iptvList = mutableListOf<IptvResponseItem>()

        lines.forEachIndexed { index, line ->
            if (!line.startsWith("#EXTINF")) return@forEachIndexed

            val name = line.split(",").last()
            val channelName = Regex("tvg-name=\"(.+?)\"").find(line)?.groupValues?.get(1) ?: name
            val groupName = Regex("group-title=\"(.+?)\"").find(line)?.groupValues?.get(1) ?: "其他"

            // 回看属性（社区 de-facto 约定：catchup/catchup-type + catchup-source + catchup-days）
            val catchupMode =
                Regex("catchup(?:-type)?=\"(.+?)\"").find(line)?.groupValues?.get(1) ?: ""
            val catchupSource =
                Regex("catchup-source=\"(.+?)\"").find(line)?.groupValues?.get(1) ?: ""
            val catchupDays =
                Regex("catchup-days=\"(.+?)\"").find(line)?.groupValues?.get(1)?.toIntOrNull() ?: 0

            iptvList.add(
                IptvResponseItem(
                    name = name.trim(),
                    channelName = channelName.trim(),
                    groupName = groupName.trim(),
                    url = lines[index + 1].trim(),
                    catchupMode = catchupMode.trim(),
                    catchupSource = catchupSource.trim(),
                    catchupDays = catchupDays,
                )
            )
        }

        return IptvGroupList(iptvList.groupBy { it.groupName }.map { groupEntry ->
            IptvGroup(
                name = groupEntry.key,
                iptvList = IptvList(groupEntry.value.groupBy { it.name }.map { nameEntry ->
                    val first = nameEntry.value.first()
                    Iptv(
                        name = nameEntry.key,
                        channelName = first.channelName,
                        urlList = nameEntry.value.map { it.url },
                        catchupMode = nameEntry.value.firstOrNull { it.catchupSource.isNotBlank() }?.catchupMode
                            ?: first.catchupMode,
                        catchupSource = nameEntry.value.firstOrNull { it.catchupSource.isNotBlank() }?.catchupSource
                            ?: first.catchupSource,
                        catchupDays = nameEntry.value.firstOrNull { it.catchupDays > 0 }?.catchupDays
                            ?: first.catchupDays,
                    )
                })
            )
        })
    }

    private data class IptvResponseItem(
        val name: String,
        val channelName: String,
        val groupName: String,
        val url: String,
        val catchupMode: String = "",
        val catchupSource: String = "",
        val catchupDays: Int = 0,
    )
}