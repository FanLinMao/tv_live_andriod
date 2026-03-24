package com.codex.tvlive

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.codex.tvlive.data.M3uRepository
import com.codex.tvlive.databinding.ActivityMainBinding
import com.codex.tvlive.model.Channel
import com.codex.tvlive.ui.ChannelAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var channelAdapter: ChannelAdapter
    private lateinit var repository: M3uRepository

    private val handler = Handler(Looper.getMainLooper())
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = M3uRepository(this)
        channelAdapter = ChannelAdapter(::playChannelFromDrawer)

        setupPlayer()
        setupDrawer()
        setupList()
        showLoadingThenStart()
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            binding.playerView.player = exoPlayer
            exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
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

    private fun showLoadingThenStart() {
        binding.loadingOverlay.isVisible = true
        binding.currentChannel.text = getString(R.string.default_playing_title)
        loadDefaultChannels()
        handler.postDelayed({
            binding.loadingOverlay.isVisible = false
        }, 2500L)
    }

    private fun loadDefaultChannels() {
        val channels = repository.loadDefaultChannels()
        channelAdapter.submitList(channels)
        binding.emptyView.isVisible = channels.isEmpty()
        binding.channelCount.text = getString(R.string.channel_count, channels.size)

        if (channels.isNotEmpty()) {
            playChannel(channels.first())
        }
    }

    private fun playChannelFromDrawer(channel: Channel) {
        playChannel(channel)
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun playChannel(channel: Channel) {
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
        binding.currentChannel.text = channel.name
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

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP && event.isOkKey()) {
            if (!binding.drawerLayout.isDrawerOpen(GravityCompat.START) &&
                !binding.loadingOverlay.isVisible
            ) {
                binding.drawerLayout.openDrawer(GravityCompat.START)
                return true
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
}
