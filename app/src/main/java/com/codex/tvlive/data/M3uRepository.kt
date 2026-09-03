package com.codex.tvlive.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.codex.tvlive.model.Channel
import java.net.HttpURLConnection
import java.net.URL
import java.util.Properties
import java.util.concurrent.Executors

class M3uRepository(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    fun loadDefaultChannels(onResult: (ChannelLoadResult) -> Unit) {
        executor.execute {
            val result = loadChannelsInternal()
            mainHandler.post {
                onResult(result)
            }
        }
    }

    private fun loadChannelsInternal(): ChannelLoadResult {
        val remoteUrl = loadRemotePlaylistUrl()
        if (remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://")) {
            val remoteChannels = loadRemoteChannels(remoteUrl)
            if (remoteChannels.isNotEmpty()) {
                return ChannelLoadResult(
                    channels = remoteChannels,
                    source = ChannelSource.REMOTE
                )
            }

            val localChannels = loadLocalChannels()
            return if (localChannels.isNotEmpty()) {
                ChannelLoadResult(
                    channels = localChannels,
                    source = ChannelSource.LOCAL_FALLBACK
                )
            } else {
                ChannelLoadResult(
                    channels = emptyList(),
                    source = ChannelSource.EMPTY
                )
            }
        }

        val localChannels = loadLocalChannels()
        return if (localChannels.isNotEmpty()) {
            ChannelLoadResult(
                channels = localChannels,
                source = ChannelSource.LOCAL_ONLY
            )
        } else {
            ChannelLoadResult(
                channels = emptyList(),
                source = ChannelSource.EMPTY
            )
        }
    }

    private fun loadRemotePlaylistUrl(): String {
        return runCatching {
            val properties = Properties()
            context.assets.open(PlaylistSourceConfig.configFileName).use { input ->
                properties.load(input)
            }
            properties.getProperty(PlaylistSourceConfig.remotePlaylistUrlKey)
                ?.trim()
                .orEmpty()
        }.getOrDefault("")
    }

    private fun loadRemoteChannels(remoteUrl: String): List<Channel> {
        return runCatching {
            val connection = URL(remoteUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = PlaylistSourceConfig.connectTimeoutMillis
            connection.readTimeout = PlaylistSourceConfig.readTimeoutMillis
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true

            try {
                connection.connect()

                if (connection.responseCode !in 200..299) {
                    return emptyList()
                }

                connection.inputStream.bufferedReader().use { reader ->
                    parse(reader.readLines())
                }
            } finally {
                connection.disconnect()
            }
        }.getOrDefault(emptyList())
    }

    private fun loadLocalChannels(): List<Channel> {
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
                    .ifBlank { "直播源${channels.size + 1}" }
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

data class ChannelLoadResult(
    val channels: List<Channel>,
    val source: ChannelSource
)

enum class ChannelSource {
    REMOTE,
    LOCAL_FALLBACK,
    LOCAL_ONLY,
    EMPTY
}
