package com.codepalace.accelerometer.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.codepalace.accelerometer.R

/**
 * RecyclerView adapter for search results.
 * Uses ListAdapter + DiffUtil for efficient updates on every keystroke.
 *
 * Usage in MainActivity:
 *
 *   private lateinit var searchAdapter: SearchResultAdapter
 *
 *   searchAdapter = SearchResultAdapter { result ->
 *       // handle item click — navigate or highlight the star
 *       viewModel.closeSearch()
 *   }
 *   rvSearchResults.layoutManager = LinearLayoutManager(this)
 *   rvSearchResults.adapter = searchAdapter
 */
class SearchResultAdapter(
    private val onItemClick: (SearchResult) -> Unit
) : ListAdapter<SearchResult, SearchResultAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView      = itemView.findViewById(R.id.tvResultName)
        private val tvSubtitle: TextView  = itemView.findViewById(R.id.tvResultSubtitle)

        fun bind(result: SearchResult) {
            tvName.text = result.displayName
            tvSubtitle.text = buildSubtitle(result)
            itemView.setOnClickListener { onItemClick(result) }
        }

        private fun buildSubtitle(r: SearchResult): String {
            val type = r.objectType.lowercase()
                .replaceFirstChar { it.uppercase() }
            return "$type · mag ${String.format("%.1f", r.magnitude)}"
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SearchResult>() {
            override fun areItemsTheSame(a: SearchResult, b: SearchResult) = a.id == b.id
            override fun areContentsTheSame(a: SearchResult, b: SearchResult) = a == b
        }
    }
}