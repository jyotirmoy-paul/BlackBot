package com.android.mr_paul.blackbot.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.mr_paul.blackbot.DataTypes.MessageData;
import com.android.mr_paul.blackbot.R;
import com.android.mr_paul.blackbot.UtilityPackage.Constants;

import java.util.ArrayList;

public class ChatDataAdapter extends ArrayAdapter<MessageData> {

    // default constructor
    public ChatDataAdapter(Context context, int resource, ArrayList<MessageData> list) {
        super(context, resource, list);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater != null){
            convertView = inflater.inflate(R.layout.message_view_layout, parent, false);
        }

        RelativeLayout botLayout = convertView.findViewById(R.id.bot_layout);
        RelativeLayout userLayout = convertView.findViewById(R.id.user_layout);

        TextView botMessageView = convertView.findViewById(R.id.bot_message_view);
        TextView userMessageView = convertView.findViewById(R.id.user_message_view);

        MessageData messageData = getItem(position);
        String sender = messageData.getSender();
        String message = messageData.getMessage();

        if(Constants.BOT.equals(sender)){
            userLayout.setVisibility(View.GONE);
            botLayout.setVisibility(View.VISIBLE);
            botMessageView.setText(message);
        } else{
            botLayout.setVisibility(View.GONE);
            userLayout.setVisibility(View.VISIBLE);
            userMessageView.setText(message);
        }

        return convertView;

    }
}
