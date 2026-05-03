package com.codepalace.accelerometer.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codepalace.accelerometer.R
import com.codepalace.accelerometer.ui.MessageKind
import com.codepalace.accelerometer.ui.showAppMessage
import com.codepalace.accelerometer.ui.viewmodel.ChatViewModel
import com.codepalace.accelerometer.util.MessageAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessageInput: EditText
    private lateinit var btnSend: ImageButton

    private lateinit var adapter: MessageAdapter

    // Chat room data from intent
    private var chatRoomId: Long = 0L
    private var chatRoomName: String = ""

    // ✅ Custom factory like in ChatRoomsActivity
    private val viewModel: ChatViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(chatRoomId) as T
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Extract data from intent first
        chatRoomId = intent.getLongExtra("chat_room_id", 0L)
        chatRoomName = intent.getStringExtra("chat_room_name") ?: "Chat"

        toolbar = findViewById(R.id.toolbar)
        rvMessages = findViewById(R.id.rvMessages)
        etMessageInput = findViewById(R.id.etMessageInput)
        btnSend = findViewById(R.id.btnSend)

        setupToolbar()
        setupRecyclerView()
        setupSendButton()
        observeViewModel()
    }

    private fun setupToolbar() {
        toolbar.title = chatRoomName
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter()
        rvMessages.adapter = adapter
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupSendButton() {
        btnSend.setOnClickListener {
            val message = etMessageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                viewModel.sendMessage(message)
                etMessageInput.text.clear()     // clear input only

                etMessageInput.requestFocus()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.messages.collectLatest { items ->
                adapter.submitList(items) {
                    // This callback runs AFTER the list is fully applied to the adapter
                    if (items.isNotEmpty()) {
                        // Smooth scroll + small delay ensures it always goes to the very bottom
                        rvMessages.post {
                            rvMessages.smoothScrollToPosition(items.size - 1)
                        }
                    }

                    val tvEmpty = findViewById<TextView>(R.id.tvEmptyState)
                    tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    companion object {
        fun createIntent(context: android.content.Context, chatRoomId: Long, chatRoomName: String): Intent {
            return Intent(context, ChatActivity::class.java).apply {
                putExtra("chat_room_id", chatRoomId)
                putExtra("chat_room_name", chatRoomName)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // ViewModel already cleans up WebSocket via onCleared()
    }
}