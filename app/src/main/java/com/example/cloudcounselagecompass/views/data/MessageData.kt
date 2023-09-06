package com.example.cloudcounselagecompass.views.data

data class MessageData(
    var message: String,
    var isReceived: Boolean  //to differentiate whether the message is form bot or user
)
