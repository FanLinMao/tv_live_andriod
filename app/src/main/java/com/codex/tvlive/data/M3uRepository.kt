package com.codex.tvlive.data

import android.content.Context
import com.codex.tvlive.model.Channel

class M3uRepository(private val context: Context) {

    fun loadDefaultChannels(): List<Channel> {
        return runCatching {
            context.assets.open("channels.m3u").bufferedReader().use { reader ->
                parse(reader.readLines())
            }
        }.getOrDefault(emptyList())
    }

    private fun parse(lines: List<String>): List<Channel> {
        val channels = mutableListOf<Channel>()
        var pendingName = ""
        var pendingGroup = ""

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#EXTM3U")) {
                continue
            }

            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                pendingGroup = Regex("""group-title="([^"]*)"""")
                    .find(line)
                    ?.groupValues
                    ?.getOrNull(1)
                    .orEmpty()

                pendingName = line.substringAfterLast(",", "").trim().ifBlank { "未命名频道" }
                continue
            }

            if (line.startsWith("http://") || line.startsWith("https://")) {
                val fallbackName = line.substringAfterLast("/").substringBefore("?")
                    .ifBlank { "直播源 ${channels.size + 1}" }
                channels += Channel(
                    name = pendingName.ifBlank { fallbackName },
                    url = line,
                    group = pendingGroup
                )
                pendingName = ""
                pendingGroup = ""
            }
        }

        return channels
    }
}
