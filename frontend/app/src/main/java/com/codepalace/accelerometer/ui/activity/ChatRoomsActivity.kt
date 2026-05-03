package com.codepalace.accelerometer.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codepalace.accelerometer.R
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.data.local.AppDatabase
import com.codepalace.accelerometer.data.model.dto.ChatRoomUi
import com.codepalace.accelerometer.ui.MessageKind
import com.codepalace.accelerometer.ui.showAppMessage
import com.codepalace.accelerometer.ui.viewmodel.ChatRoomsViewModel
import com.codepalace.accelerometer.util.ChatRoomAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

@RequiresApi(Build.VERSION_CODES.O)
class ChatRoomsActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var searchInput: EditText
    private lateinit var rvChatRooms: RecyclerView
    private lateinit var tvEmpty: android.widget.TextView

    private val viewModel: ChatRoomsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val dao = AppDatabase.getDatabase(this@ChatRoomsActivity).chatRoomDao()
                return ChatRoomsViewModel(dao, ApiClient.getSessionStorage()) as T
            }
        }
    }
    private lateinit var adapter: ChatRoomAdapter
    private var isSearchMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats)

        toolbar = findViewById(R.id.toolbar)
        searchInput = findViewById(R.id.searchInput)
        rvChatRooms = findViewById(R.id.rvChatRooms)
        tvEmpty = findViewById(R.id.tvEmpty)

        setupToolbar()
        setupRecyclerView()
        setupSearch()

        if (!ApiClient.getSessionStorage().isLoggedIn()) {
            showAppMessage("Log in to access chats", MessageKind.INFO)
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        observeViewModel()
        viewModel.loadInitialData()
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        adapter = ChatRoomAdapter(
            onClick = { room ->
                if (room.isSubscribed) {
                    val intent = ChatActivity.createIntent(this, room.id, room.name)
                    startActivity(intent)
                }
            },
            onSubscribeClick = { room -> toggleSubscription(room.id, isSubscribe = true) },
            onUnsubscribeClick = { room -> toggleSubscription(room.id, isSubscribe = false) }
        )
        rvChatRooms.adapter = adapter
        rvChatRooms.layoutManager = LinearLayoutManager(this)
    }
    private fun toggleSubscription(roomId: Long, isSubscribe: Boolean) {
        lifecycleScope.launch {
            viewModel.toggleSubscription(roomId)   // already does local refresh inside

            if (isSubscribe) {
                showAppMessage("Chat room added to your list", MessageKind.SUCCESS)
                exitSearchMode()
            } else {
                showAppMessage("You were unsubscribed from this chat room", MessageKind.SUCCESS)
            }
        }
    }

    private fun exitSearchMode() {
        isSearchMode = false
        searchInput.text.clear()
        searchInput.clearFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchInput.windowToken, 0)
        viewModel.onSearchQueryChanged("") // go back to subscribed list
    }

    private fun setupSearch() {
        searchInput.doAfterTextChanged { text ->
            val query = text?.toString() ?: ""
            isSearchMode = query.isNotEmpty()
            viewModel.onSearchQueryChanged(query)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.chatRooms.collectLatest { rooms ->
                adapter.submitList(rooms)
                tvEmpty.visibility = if (rooms.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }
}