package com.example.cloudcounselagecompass.views.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cloudcounselagecompass.R
import com.example.cloudcounselagecompass.databinding.FragmentChatBinding
import com.example.cloudcounselagecompass.views.adapter.ChatAdapter
import com.example.cloudcounselagecompass.views.data.MessageData
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.dialogflow.v2.DetectIntentRequest
import com.google.cloud.dialogflow.v2.DetectIntentResponse
import com.google.cloud.dialogflow.v2.QueryInput
import com.google.cloud.dialogflow.v2.SessionName
import com.google.cloud.dialogflow.v2.SessionsClient
import com.google.cloud.dialogflow.v2.SessionsSettings
import com.google.cloud.dialogflow.v2.TextInput
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.log

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ChatFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ChatFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var binding:FragmentChatBinding?=null

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messageList: ArrayList<MessageData>

    // private var viewHolder: ChatAdapter.ChatViewHolder?=null

    //dialogflow
    private var sessionsClient: SessionsClient? = null
    private var sessionName: SessionName? = null
    private var uuid = UUID.randomUUID().toString()

    //network
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding= FragmentChatBinding.inflate(inflater,container,false)
        return binding!!.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ChatFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ChatFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val window: Window = requireActivity().window
        window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.dark_blue)
        binding!!.tagOnline.visibility = View.VISIBLE
        // Check initial network connectivity state
        if (isNetworkAvailable(requireContext())) {
            binding!!.tagOnline.text = this.resources.getString(R.string.online)
        } else {
            binding!!.tagOnline.text = this.resources.getString(R.string.connecting)
            Toast.makeText(requireContext(), "No Internet Connection!", Toast.LENGTH_SHORT).show()
        }
// Initialize ConnectivityManager
        connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Initialize the network callback
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                // Network is available, handle it accordingly.
                requireActivity().runOnUiThread {
                    binding!!.tagOnline.text = requireContext().getString(R.string.online)

                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                // Network is lost, handle it accordingly.
                requireActivity().runOnUiThread {
                    binding!!.tagOnline.text = requireContext().getString(R.string.connecting)
                    Toast.makeText(requireContext(), "Network Error!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Register the network callback
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
//end
        messageList = ArrayList()
        binding!!.chatView.layoutManager = LinearLayoutManager(context)
        chatAdapter = ChatAdapter(requireContext(), messageList)
        binding!!.chatView.adapter = chatAdapter

        binding!!.sendLl.setOnClickListener {
            val userMessage = binding!!.messageInput.text.toString()
            if (userMessage.isNotEmpty()) {
                addMessageToList(userMessage, false)
                sendMessageToBot(userMessage)
                if (isNetworkAvailable(requireContext()))
                   binding!!.tagOnline.text = this.resources.getString(R.string.typing)
            } else {
                Toast.makeText(requireContext(), "No message to send!", Toast.LENGTH_SHORT).show()
            }
        }
        botSetUp()

        binding!!.backBtn.setOnClickListener {
            findNavController().navigate(R.id.action_chatFragment_to_homeFragment)
        }
    }


    private fun botSetUp() {
        //initiate dialogflow
        try {
            val stream = this.resources.openRawResource(R.raw.credentials)
            val credentials: GoogleCredentials = GoogleCredentials.fromStream(stream)
                .createScoped("https://www.googleapis.com/auth/cloud-platform")
            val projectId: String = (credentials as ServiceAccountCredentials).projectId
            val settingsBuilder: SessionsSettings.Builder = SessionsSettings.newBuilder()
            val sessionsSettings: SessionsSettings = settingsBuilder.setCredentialsProvider(
                FixedCredentialsProvider.create(credentials)
            ).build()
            sessionsClient = SessionsClient.create(sessionsSettings)
            sessionName = SessionName.of(projectId, uuid)
            Log.d("CharFragment", "projectId : $projectId")
        } catch (e: Exception) {
            Log.d("ChatFragment", "setUpBot: ${e.message}")
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun sendMessageToBot(userMessage: String) {
        val input = QueryInput.newBuilder()
            .setText(TextInput.newBuilder().setText(userMessage).setLanguageCode("en-US")).build()
        GlobalScope.launch {
            sendMessageInBg(input)
        }
    }

    private suspend fun sendMessageInBg(
        queryInput: QueryInput
    ) {
        withContext(Default) {
            try {
                val detectIntentRequest = DetectIntentRequest.newBuilder()
                    .setSession(sessionName.toString())
                    .setQueryInput(queryInput)
                    .build()
                val result = sessionsClient?.detectIntent(detectIntentRequest)
                if (result != null) {
                    requireActivity().runOnUiThread {
                        updateUI(result)
                    }
                }
            } catch (e: java.lang.Exception) {
                Log.d("ChatFragment", "doInBackground: " + e.message)
                e.printStackTrace()
            }
        }
    }

    private fun updateUI(result: DetectIntentResponse) {
        val botReply: String = result.queryResult.fulfillmentText
        if (botReply.isNotEmpty()) {
            addMessageToList(botReply, true)
            if (isNetworkAvailable(requireContext()))
                binding!!.tagOnline.text = this.resources.getString(R.string.online)
            else
              binding!!.tagOnline.text = this.resources.getString(R.string.connecting)
        } else {
            Toast.makeText(requireContext(), "something went wrong", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun addMessageToList(userMessage: String, isReceived: Boolean) {
        messageList.add(MessageData(userMessage, isReceived))
        binding!!.messageInput.setText("")
        chatAdapter.notifyDataSetChanged()
       binding!!.chatView.layoutManager!!.scrollToPosition(messageList.size - 1)
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the NetworkCallback when the fragment is destroyed
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

}