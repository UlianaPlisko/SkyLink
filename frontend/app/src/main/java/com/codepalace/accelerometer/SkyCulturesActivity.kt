package com.codepalace.accelerometer

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codepalace.accelerometer.ui.viewmodel.SkyCulturesViewModel
import com.codepalace.accelerometer.util.SkyCulturesAdapter
import kotlinx.coroutines.launch

class SkyCulturesActivity : AppCompatActivity() {

    private val viewModel: SkyCulturesViewModel by viewModels()

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: SkyCulturesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sky_cultures)

        // Find views
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        recycler = findViewById(R.id.recyclerCultures)

        // Back button
        btnBack.setOnClickListener {
            Log.d("SkyActivity", "Back button clicked - finishing activity")
            finish()
        }

        // Adapter
        adapter = SkyCulturesAdapter(
            items = emptyList(),
            onClick = { viewModel.toggleExpand(it.id) },
            onSetClick = { viewModel.setCurrentCulture(it) }
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // Collect flows (combined for better sync)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.cultures.collect { culturesList ->
                        Log.d("SkyActivity", "📦 cultures updated: ${culturesList.size} items")
                        val expanded = viewModel.expandedId.value
                        adapter.update(culturesList, expanded)
                    }
                }

                launch {
                    viewModel.expandedId.collect { expanded ->
                        Log.d("SkyActivity", "🔄 expandedId changed to: $expanded")
                        adapter.update(viewModel.cultures.value, expanded)
                    }
                }
            }
        }

        // Load data
        viewModel.loadCultures()
    }
}