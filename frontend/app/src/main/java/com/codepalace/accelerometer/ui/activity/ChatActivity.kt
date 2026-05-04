package com.codepalace.accelerometer.ui.activity

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import com.codepalace.accelerometer.data.local.AppDatabase
import com.codepalace.accelerometer.ui.MessageKind
import com.codepalace.accelerometer.ui.showAppMessage
import com.codepalace.accelerometer.ui.viewmodel.ChatViewModel
import com.codepalace.accelerometer.util.MessageAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class ChatActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessageInput: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var bottomInputBar: View          // ← added
    private lateinit var adapter: MessageAdapter

    private var chatRoomId: Long = 0L
    private var chatRoomName: String = ""

    private val viewModel: ChatViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getDatabase(this@ChatActivity)
                return ChatViewModel(chatRoomId, db.chatDao()) as T   // ← fixed: was chatDao()
            }
        }
    }

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
        bottomInputBar = findViewById(R.id.bottomInputBar)   // ← added

        setupToolbar()
        setupRecyclerView()
        setupSendButton()
        observeViewModel()

        updateMessagingAvailability()   // ← initial check
    }

    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun updateMessagingAvailability() {
        val online = isOnline()
        bottomInputBar.visibility = if (online) View.VISIBLE else View.GONE

        if (!online) {
            showAppMessage("You are offline.\nMessaging is unavailable (cached messages are visible)", MessageKind.INFO)
        }
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
                etMessageInput.text.clear()
                etMessageInput.requestFocus()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.messages.collectLatest { items ->
                adapter.submitList(items) {
                    if (items.isNotEmpty()) {
                        rvMessages.post {
                            rvMessages.smoothScrollToPosition(items.size - 1)
                        }
                    }
                }

                val tvEmpty = findViewById<TextView>(R.id.tvEmptyState)
                tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    // Refresh availability when returning to the screen (network may have changed)
    override fun onResume() {
        super.onResume()
        updateMessagingAvailability()
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
    }
}