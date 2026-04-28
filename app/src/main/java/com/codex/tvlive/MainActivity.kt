package com.codex.tvlive

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.DefaultHlsExtractorFactory
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.recyclerview.widget.LinearLayoutManager
import com.codex.tvlive.data.ChannelSource
import com.codex.tvlive.data.M3uRepository
import com.codex.tvlive.databinding.ActivityMainBinding
import com.codex.tvlive.model.Channel
import com.codex.tvlive.ui.ChannelAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var channelAdapter: ChannelAdapter
    private lateinit var repository: M3uRepository

    private val handler = Handler(Looper.getMainLooper())
    private val hideOverlayRunnable = Runnable {
        binding.channelOverlay.isVisible = false
    }

    private var player: ExoPlayer? = null
    private var lastBackPressedAt = 0L
    private var selectedVideoMimeType: String? = null
    private var selectedVideoCodecs: String? = null
    private var selectedVideoResolution: String? = null
    private var channels: List<Channel> = emptyList()
    private var currentChannelIndex = 0
    private var pendingOverlayChannelName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = M3uRepository(this)
        channelAdapter = ChannelAdapter(::playChannelFromDrawer)

        setupPlayer()
        setupDrawer()
        setupList()
        setupBackHandler()
        showLoadingThenStart()
    }

    private fun setupPlayer() {
        val renderersFactory = DefaultRenderersFactory(this)
            .setEnableDecoderFallback(true)

        val hlsExtractorFlags =
            DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS or
                DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES

        val hlsExtractorFactory = DefaultHlsExtractorFactory(
            hlsExtractorFlags,
            true
        )

        val dataSourceFactory = DefaultDataSource.Factory(this)
        val mediaSourceFactory = HlsMediaSource.Factory(dataSourceFactory)
            .setExtractorFactory(hlsExtractorFactory)

        val softwareFirstSelector = MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
            val codecs = MediaCodecSelector.DEFAULT.getDecoderInfos(
                mimeType,
                requiresSecureDecoder,
                requiresTunnelingDecoder
            )
            codecs.sortedWith(
                compareByDescending<androidx.media3.exoplayer.mediacodec.MediaCodecInfo> { it.softwareOnly }
                    .thenBy { it.name }
            )
        }

        renderersFactory.setMediaCodecSelector(softwareFirstSelector)

        player = ExoPlayer.Builder(this, renderersFactory, mediaSourceFactory)
            .build()
            .also { exoPlayer ->
                binding.playerView.player = exoPlayer
                exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
                exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                exoPlayer.addListener(object : Player.Listener {
                    override fun onTracksChanged(tracks: Tracks) {
                        logSelectedTracks(tracks)
                    }

                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        Log.d(
                            LOG_TAG,
                            "VideoSize width=${videoSize.width}, height=${videoSize.height}, ratio=${videoSize.pixelWidthHeightRatio}"
                        )
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        Log.d(LOG_TAG, "PlaybackState=$playbackState")
                        when (playbackState) {
                            Player.STATE_BUFFERING -> showLoadingOverlay()
                            Player.STATE_READY -> {
                                hideLoadingOverlay()
                                binding.errorOverlay.isVisible = false
                                pendingOverlayChannelName?.let {
                                    showChannelOverlay(it)
                                    pendingOverlayChannelName = null
                                }
                            }
                            Player.STATE_IDLE,
                            Player.STATE_ENDED -> {
                                hideLoadingOverlay()
                                pendingOverlayChannelName = null
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(LOG_TAG, "PlayerError code=${error.errorCodeName}, message=${error.message}", error)
                        handlePlayerError(error)
                    }
                })
            }
    }

    private fun setupDrawer() {
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        binding.drawerLayout.setScrimColor(Color.TRANSPARENT)
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                focusSelectedChannel()
            }
        })
    }

    private fun setupList() {
        binding.channelList.layoutManager = LinearLayoutManager(this)
        binding.channelList.adapter = channelAdapter
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    binding.drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                        binding.drawerLayout.closeDrawer(GravityCompat.START)
                    }

                    shouldExitApp() -> finish()
                    else -> {
                        lastBackPressedAt = System.currentTimeMillis()
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.exit_app_tip),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    private fun showLoadingThenStart() {
        showLoadingOverlay()
        binding.channelOverlay.isVisible = false
        binding.errorOverlay.isVisible = false
        binding.currentChannel.text = getString(R.string.default_playing_title)
        loadDefaultChannels()
    }

    private fun loadDefaultChannels() {
        repository.loadDefaultChannels { result ->
            channels = result.channels
            channelAdapter.submitList(channels)
            binding.emptyView.isVisible = channels.isEmpty()

            when (result.source) {
                ChannelSource.LOCAL_FALLBACK -> {
                    Toast.makeText(
                        this,
                        "远程节目单加载失败，已自动切换到本地节目单",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                ChannelSource.LOCAL_ONLY -> Unit

                ChannelSource.EMPTY -> {
                    hideLoadingOverlay()
                }

                ChannelSource.REMOTE -> Unit
            }

            if (channels.isNotEmpty()) {
                currentChannelIndex = 0
                playChannel(channels.first())
            } else {
                hideLoadingOverlay()
            }
        }
    }

    private fun playChannelFromDrawer(channel: Channel) {
        val index = channels.indexOfFirst { it.url == channel.url && it.name == channel.name }
        if (index >= 0) {
            currentChannelIndex = index
            channelAdapter.setSelectedPosition(index)
        }
        playChannel(channel)
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun playChannel(channel: Channel) {
        showLoadingOverlay()
        binding.errorOverlay.isVisible = false
        binding.errorMessage.text = ""
        binding.channelOverlay.isVisible = false
        handler.removeCallbacks(hideOverlayRunnable)
        selectedVideoMimeType = null
        selectedVideoCodecs = null
        selectedVideoResolution = null
        pendingOverlayChannelName = channel.name
        Log.d(LOG_TAG, "PlayChannel name=${channel.name}, url=${channel.url}")
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(channel.url))
            .setMimeType("application/x-mpegURL")
            .setMediaId(channel.name)
            .build()

        player?.apply {
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    private fun showChannelOverlay(channelName: String) {
        binding.currentChannel.text = getString(R.string.now_playing_format, channelName)
        binding.channelOverlay.isVisible = true
        handler.removeCallbacks(hideOverlayRunnable)
        handler.postDelayed(hideOverlayRunnable, 5000L)
    }

    private fun playNextChannel(step: Int) {
        if (channels.isEmpty()) return
        currentChannelIndex = (currentChannelIndex + step + channels.size) % channels.size
        channelAdapter.setSelectedPosition(currentChannelIndex)
        channels.getOrNull(currentChannelIndex)?.let(::playChannel)
    }

    private fun focusSelectedChannel() {
        val selectedPosition = channelAdapter.getSelectedPosition()
        if (selectedPosition == -1) {
            binding.channelList.requestFocus()
            return
        }

        binding.channelList.scrollToPosition(selectedPosition)
        binding.channelList.post {
            binding.channelList.findViewHolderForAdapterPosition(selectedPosition)
                ?.itemView
                ?.requestFocus()
                ?: binding.channelList.requestFocus()
        }
    }

    private fun shouldExitApp(): Boolean {
        return System.currentTimeMillis() - lastBackPressedAt < 2000L
    }

    private fun logSelectedTracks(tracks: Tracks) {
        for (group in tracks.groups) {
            val trackType = when (group.type) {
                C.TRACK_TYPE_VIDEO -> "video"
                C.TRACK_TYPE_AUDIO -> "audio"
                C.TRACK_TYPE_TEXT -> "text"
                else -> "other(${group.type})"
            }
            for (index in 0 until group.length) {
                if (!group.isTrackSelected(index)) continue
                val format = group.getTrackFormat(index)
                if (group.type == C.TRACK_TYPE_VIDEO) {
                    selectedVideoMimeType = format.sampleMimeType ?: format.containerMimeType
                    selectedVideoCodecs = format.codecs
                    selectedVideoResolution = if (format.width > 0 && format.height > 0) {
                        "${format.width}x${format.height}"
                    } else {
                        "unknown"
                    }
                }
                Log.d(LOG_TAG, buildTrackLog(trackType, format))
            }
        }
    }

    private fun handlePlayerError(error: PlaybackException) {
        hideLoadingOverlay()
        pendingOverlayChannelName = null
        binding.channelOverlay.isVisible = false
        handler.removeCallbacks(hideOverlayRunnable)
        if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
            binding.errorOverlay.isVisible = true
            binding.errorMessage.text = getString(R.string.playback_error_live_window)
            player?.apply {
                seekToDefaultPosition()
                prepare()
                playWhenReady = true
            }
            return
        }

        val mimeType = selectedVideoMimeType.orEmpty()
        val codecs = selectedVideoCodecs.orEmpty()
        val resolution = selectedVideoResolution ?: "unknown"
        val isLikelyAvc = mimeType.contains("video/avc", ignoreCase = true) ||
            codecs.contains("avc", ignoreCase = true)

        binding.errorOverlay.isVisible = true
        binding.errorMessage.text = if (isLikelyAvc) {
            getString(
                R.string.playback_error_emulator_compat,
                if (codecs.isBlank()) "H.264/AVC" else codecs,
                resolution
            )
        } else {
            getString(R.string.playback_error_generic)
        }
    }

    private fun showLoadingOverlay() {
        binding.loadingOverlay.isVisible = true
    }

    private fun hideLoadingOverlay() {
        binding.loadingOverlay.isVisible = false
    }

    private fun buildTrackLog(trackType: String, format: Format): String {
        val resolution = if (format.width > 0 && format.height > 0) {
            "${format.width}x${format.height}"
        } else {
            "unknown"
        }
        val bitrate = if (format.bitrate > 0) format.bitrate.toString() else "unknown"
        val codecs = format.codecs ?: "unknown"
        val mimeType = format.sampleMimeType ?: format.containerMimeType ?: "unknown"
        val label = format.label ?: "unknown"
        return "SelectedTrack type=$trackType, label=$label, mime=$mimeType, codecs=$codecs, resolution=$resolution, bitrate=$bitrate"
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP && !binding.loadingOverlay.isVisible) {
            if (event.isOkKey()) {
                if (!binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.openDrawer(GravityCompat.START)
                    return true
                }
            }

            if (!binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        playNextChannel(-1)
                        return true
                    }

                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        playNextChannel(1)
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onStart() {
        super.onStart()
        binding.playerView.onResume()
    }

    override fun onStop() {
        binding.playerView.onPause()
        super.onStop()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        binding.playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun KeyEvent.isOkKey(): Boolean {
        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
            keyCode == KeyEvent.KEYCODE_ENTER ||
            keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
            keyCode == KeyEvent.KEYCODE_BUTTON_A
    }

    companion object {
        private const val LOG_TAG = "HPCTV-Player"
    }
}
