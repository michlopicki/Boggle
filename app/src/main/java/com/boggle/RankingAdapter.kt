package com.boggle

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RankingAdapter : ListAdapter<GameResult, RankingAdapter.RankingViewHolder>(DiffCallback) {

    class RankingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val scoreText: TextView = view.findViewById(R.id.tv_ranking_score)
        val wordsText: TextView = view.findViewById(R.id.tv_ranking_words)
        val dateText: TextView = view.findViewById(R.id.tv_ranking_date)
        val miniBoard: GridLayout = view.findViewById(R.id.grid_mini_board)

        // Formater daty (np. "24 Paź 2023, 14:30")
        private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        val durationText: TextView = view.findViewById(R.id.tv_ranking_duration)

        fun bind(game: GameResult) {
            scoreText.text = "${game.score} pkt"
            wordsText.text = "${game.wordCount} słów"
            dateText.text = dateFormat.format(Date(game.dateMillis))

            val minutes = game.durationSeconds / 60
            val seconds = game.durationSeconds % 60
            durationText.text = String.format("Czas: %d:%02d", minutes, seconds)
            setupMiniBoard(game.board)
        }

        private fun setupMiniBoard(boardString: String) {
            val context = miniBoard.context
            val density = context.resources.displayMetrics.density
            val sizePx = (20 * density).toInt()
            val marginPx = (1 * density).toInt()

            if (miniBoard.childCount == 0) {
                for (i in 0 until 16) {
                    val tv = TextView(context).apply {
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = sizePx
                            height = sizePx
                            setMargins(marginPx, marginPx, marginPx, marginPx)
                        }
                        gravity = Gravity.CENTER
                        textSize = 10f
                        setTextColor(Color.BLACK)
                        setBackgroundResource(R.drawable.tile_normal)
                    }
                    miniBoard.addView(tv)
                }
            }

            for (i in 0 until 16) {
                val tv = miniBoard.getChildAt(i) as TextView
                tv.text = boardString[i].toString()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ranking, parent, false)
        return RankingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RankingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<GameResult>() {
        override fun areItemsTheSame(oldItem: GameResult, newItem: GameResult) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: GameResult, newItem: GameResult) = oldItem == newItem
    }
}