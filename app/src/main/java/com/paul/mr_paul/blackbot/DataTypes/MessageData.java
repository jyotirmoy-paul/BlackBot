package com.paul.mr_paul.blackbot.DataTypes;

public class MessageData {

    private String sender;
    private String message;
    private String timeStamp;

    public MessageData(String sender, String message, String timeStamp){
        this.sender = sender;
        this.message = message;
        this.timeStamp = timeStamp;
    }

    public String getSender(){
        return sender;
    }

    public String getMessage(){
        return message;
    }

    public String getTimeStamp(){
        return timeStamp;
    }

}
