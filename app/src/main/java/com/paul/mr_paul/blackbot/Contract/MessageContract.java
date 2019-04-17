package com.android.mr_paul.blackbot.Contract;

import android.provider.BaseColumns;

public class MessageContract {

    private MessageContract(){}

    public static final class MessageEntry implements BaseColumns {
        public static final String TABLE_NAME = "message_data";
        public static final String COLUMN_SENDER = "sender";
        public static final String COLUMN_MESSAGE = "message";
        public static final String COLUMN_TIMESTAMP = "timestamp";
    }

}
