package com.codex.tvlive.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.codex.tvlive.databinding.ItemChannelBinding
import com.codex.tvlive.model.Channel

class ChannelAdapter(
    private val onChannelClick: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    private val items = mutableListOf<Channel>()
    private var selectedPosition = RecyclerView.NO_POSITION

    fun submitList(channels: List<Channel>) {
        items.clear()
        items.addAll(channels)
        selectedPosition = if (channels.isNotEmpty()) 0 else RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    fun getSelectedPosition(): Int = selectedPosition

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ItemChannelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class ChannelViewHolder(
        private val binding: ItemChannelBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: Channel, position: Int) {
            binding.channelName.text = channel.name
            applyState(binding.root, position == selectedPosition)
            binding.root.setOnClickListener {
                val previous = selectedPosition
                selectedPosition = bindingAdapterPosition
                if (previous != RecyclerView.NO_POSITION) {
                    notifyItemChanged(previous)
                }
                notifyItemChanged(selectedPosition)
                onChannelClick(channel)
            }
            binding.root.setOnFocusChangeListener { _, hasFocus ->
                applyState(binding.root, hasFocus || position == selectedPosition)
                binding.root.scaleX = if (hasFocus) 1.03f else 1f
                binding.root.scaleY = if (hasFocus) 1.03f else 1f
            }
        }

        private fun applyState(view: View, active: Boolean) {
            view.isSelected = active
        }
    }
}
