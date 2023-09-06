package com.example.cloudcounselagecompass.views.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.RecyclerView
import com.example.cloudcounselagecompass.R
import com.example.cloudcounselagecompass.views.data.MessageData

class ChatAdapter(private var context: Context, private var messageList: List<MessageData>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userMessage: TextView = itemView.findViewById(R.id.user_chat_tv)
        val botMessage: TextView = itemView.findViewById(R.id.bot_chat_tv)
        val userMessageLL:LinearLayout=itemView.findViewById(R.id.user_chat_ll)
        val botMessageLL:LinearLayout=itemView.findViewById(R.id.bot_chat_ll)
        val dpIcon:LinearLayout=itemView.findViewById(R.id.dp_icon_ll)
        val copyBtn:ImageView=itemView.findViewById(R.id.copy_icon)

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view =
            LayoutInflater.from(context).inflate(R.layout.adapter_chat_in_one, parent, false)
        return ChatViewHolder(view)
    }


    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messageList[position].message
        val isReceived = messageList[position].isReceived
        if (isReceived) {
            holder.botMessageLL.visibility = View.VISIBLE
            holder.dpIcon.visibility=View.VISIBLE
            holder.userMessageLL.visibility = View.GONE
            holder.botMessage.text = message
        } else {
            holder.userMessageLL.visibility = View.VISIBLE
            holder.botMessageLL.visibility = View.GONE
            holder.dpIcon.visibility=View.GONE
            holder.userMessage.text = message
        }
        holder.copyBtn.setOnClickListener {
            copyBotMessage(holder.botMessage.text.toString())
        }
    }

    @SuppressLint("ServiceCast")
    private fun copyBotMessage(message: String) {
        // Get a reference to the clipboard manager
        val clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // Create a ClipData object to hold the text
        val clipData = ClipData.newPlainText("text", message)

        // Set the data on the clipboard
        clipboardManager.setPrimaryClip(clipData)

        // Notify the user that the text has been copied
        Toast.makeText(context,"Copied",Toast.LENGTH_SHORT).show()
    }

    override fun getItemCount(): Int {
        return messageList.size
    }


}