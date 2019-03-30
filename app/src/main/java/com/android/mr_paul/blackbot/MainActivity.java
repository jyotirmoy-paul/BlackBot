package com.android.mr_paul.blackbot;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.mr_paul.blackbot.Adapters.MessageAdapter;
import com.android.mr_paul.blackbot.Contract.MessageContract;
import com.android.mr_paul.blackbot.DBHelper.MessageDBHelper;
import com.android.mr_paul.blackbot.DataTypes.MessageData;
import com.android.mr_paul.blackbot.UtilityPackage.Constants;
import com.bumptech.glide.Glide;

import org.alicebot.ab.AIMLProcessor;
import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;
import org.alicebot.ab.Graphmaster;
import org.alicebot.ab.MagicBooleans;
import org.alicebot.ab.MagicStrings;
import org.alicebot.ab.PCAIMLProcessorExtension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private EditText messageInputView;
    private ImageView botWritingView;
    private int timePerCharacter;
    private ImageView botSpeechToggle;

    public Bot bot;
    public static Chat chat;
    private boolean speechAllowed; // the flag for toggling speech engine

    private TextToSpeech textToSpeech;

    private SQLiteDatabase database;
    private MessageAdapter messageAdapter;

    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MessageDBHelper messageDBHelper = new MessageDBHelper(this);
        database = messageDBHelper.getWritableDatabase();

        timePerCharacter = 30 + (new Random().nextInt(30)); // 30 - 60

        messageInputView = findViewById(R.id.message_input_view);
        ImageView messageSendButton = findViewById(R.id.message_send_button);
        botWritingView = findViewById(R.id.bot_writing_view);
        final ImageView deleteChatMessages = findViewById(R.id.delete_chats);
        botSpeechToggle = findViewById(R.id.bot_speech_toggle);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        messageAdapter = new MessageAdapter(this, getAllMessages());
        recyclerView.setAdapter(messageAdapter);

        AssetManager assetManager = getResources().getAssets();
        File cacheDirectory = new File(getCacheDir().toString() + "/mr_paul/bots/darkbot");
        boolean dirMakingSuccessful = cacheDirectory.mkdirs();

        // saving the bot's core data in the cache
        if(dirMakingSuccessful && cacheDirectory.exists()){
            try{
                for(String dir : assetManager.list("darkbot")){
                    File subDirectory = new File(cacheDirectory.getPath() + "/" + dir);
                    subDirectory.mkdirs();
                    for(String file : assetManager.list("darkbot/" + dir)){
                        File f = new File(cacheDirectory.getPath() + "/" + dir + "/" + file);
                        if(!f.exists()){
                            InputStream in;
                            OutputStream out;

                            in = assetManager.open("darkbot/" + dir + "/" + file);
                            out = new FileOutputStream(cacheDirectory.getPath() + "/" + dir + "/" + file);

                            copyFile(in, out);
                            in.close();
                            out.flush();
                            out.close();
                        }
                    }
                }

            } catch(IOException e){
                e.printStackTrace();
                Log.i("darkbot", "IOException occurred when writing from cache!");
            } catch(NullPointerException e){
                Log.i("darkbot", "Nullpoint Exception!");
            }
        }

        // asking for permission for placing call
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE}, 0x12345);
            }
        }


        final ProgressDialog pd = new ProgressDialog(MainActivity.this);
        pd.setTitle("Please Wait");
        pd.setMessage("Starting Paul's Black Bot...");
        pd.setCanceledOnTouchOutside(false);
        pd.setCancelable(false);

        // handler for communication with the background thread
        final Handler handler = new Handler(){
            @Override
            public void dispatchMessage(Message msg) {
                super.dispatchMessage(msg);
                pd.cancel();
            }
        };

        // initializing the bot in background thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                MagicStrings.root_path = getCacheDir().toString() + "/mr_paul";
                AIMLProcessor.extension = new PCAIMLProcessorExtension();
                bot = new Bot("darkbot", MagicStrings.root_path, "chat");
                chat = new Chat(bot);
                handler.sendMessage(new Message()); // dispatch a message to the UI thread
            }
        });

        // finally show the progress dialog box and start the thread
        pd.show();
        thread.start();

        // listen for button click
        messageSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendChatMessage();
            }
        });

        // initialization of speech engine
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){
                    int result = textToSpeech.setLanguage(Locale.getDefault());
                    if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                        Toast.makeText(MainActivity.this,
                                "Default Language not recognized!", Toast.LENGTH_SHORT).show();
                        Log.i("darkbot", "Speech Engine not initialized");
                    } else{
                        preferences = getSharedPreferences(Constants.SHARED_PREFERENCES, MODE_PRIVATE);
                        /*Initially keep speech turned off*/
                        Boolean wasSpeechAllowed = preferences.getBoolean(Constants.WAS_SPEECH_ALLOWED, false);
                        speechAllowed = wasSpeechAllowed;

                        if(wasSpeechAllowed){
                            // show the mute button
                            botSpeechToggle.setImageResource(R.drawable.ic_mute_button);

                        } else{
                            // show the volume up button
                            botSpeechToggle.setImageResource(R.drawable.ic_volume_up_button);
                        }

                    }
                }
            }
        });

        deleteChatMessages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAllChatData();
            }
        });

        botSpeechToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(speechAllowed){
                    speechAllowed = false;
                    // show the volume up button - currently the bot is mute
                    botSpeechToggle.setImageResource(R.drawable.ic_volume_up_button);
                } else{
                    speechAllowed = true;
                    // show the mute button - currently the bot is speaking
                    botSpeechToggle.setImageResource(R.drawable.ic_mute_button);
                }

                // finally write the settings to the shared preference
                preferences.edit().putBoolean(Constants.WAS_SPEECH_ALLOWED, speechAllowed).apply();

            }
        });

        // delete a particular message by just swiping right
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                removeItem((long) viewHolder.itemView.getTag());
            }
        }).attachToRecyclerView(recyclerView);

    }

    // method to delete a single chat message
    private void removeItem(long id){
        database.delete(MessageContract.MessageEntry.TABLE_NAME,
                MessageContract.MessageEntry._ID + "=" + id,null);
        messageAdapter.swapCursor(getAllMessages());
    }

    // method to delete all the chat data
    private void deleteAllChatData(){
        // ask for user confirmation
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Are you sure, You want to delete all the chats?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                database.execSQL("DELETE FROM " + MessageContract.MessageEntry.TABLE_NAME);
                messageAdapter.swapCursor(getAllMessages());

                Toast.makeText(MainActivity.this,
                        "All chats deleted!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Nopes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    private Cursor getAllMessages(){
        return database.query(
                MessageContract.MessageEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                MessageContract.MessageEntry._ID + " DESC"
        );
    }


    // message sending method
    public void sendChatMessage(){
        String message = messageInputView.getText().toString().trim();
        if(message.isEmpty()){
            messageInputView.setError("Can't send empty message!");
            messageInputView.requestFocus();
            return;
        }

        DateFormat dateFormat = new SimpleDateFormat("hh:mm dd/MM/yyyy");
        String timeStamp = dateFormat.format(new Date());

        addMessage(new MessageData(Constants.USER, message, timeStamp));

        if(message.toUpperCase().startsWith("CALL")){
            // calling a phone number as requested by user

            String[] replyForCalling = {
                    "Calling",
                    "Placing a call on",
                    "Definitely, calling",
                    "There we go, calling",
                    "Making a call to",
                    "Ji sir, calling"
            };

            String[] temp = message.split(" ", 2);
            displayBotReply(new MessageData(Constants.BOT, replyForCalling[new Random().nextInt(replyForCalling.length)] + " " + temp[1], timeStamp));
            makeCall(temp[1]);
        } else if(message.toUpperCase().startsWith("OPEN") || message.toUpperCase().startsWith("LAUNCH")){
            // call intent to app, requested by user

            String[] replyForOpeningApp = {
                    "There we go, opening",
                    "Launching",
                    "Opening",
                    "Trying to open",
                    "Trying to launch",
                    "There we go, launching"
            };

            String[] temp = message.split(" ", 2);
            displayBotReply(new MessageData(Constants.BOT, replyForOpeningApp[new Random().nextInt(replyForOpeningApp.length)] + " " + temp[1],timeStamp));
            launchApp(getAppName(temp[1]));
        } else if(message.toUpperCase().startsWith("DELETE") || message.toUpperCase().startsWith("CLEAR")){
            displayBotReply(new MessageData(Constants.BOT,"Okay! I will clear up everything for you!", timeStamp));
        } else if(message.toUpperCase().contains("JOKE")){

            String[] replyForJokes = {
                    "Jokes coming right up...",
                    "Processing a hot'n'fresh joke, right for you!",
                    "There you go...",
                    "This might make you laugh...",
                    "My jokes are still in alpha, Hopefully soon they'll get beta, till then...",
                    "Jokes are my another speciality, there you go...",
                    "Jokes, you ask? This might make you laugh...",
                    "Trying to make you laugh...",
                    "You might find this funny...",
                    "Enjoy your joke..."
            };

            displayBotReply(new MessageData(Constants.BOT, replyForJokes[new Random().nextInt(replyForJokes.length)] + "\n" + mainFunction(message), timeStamp));

        } else{
            // chat with bot - save the reply from the bot
            String botReply = mainFunction(message);
            if(botReply.trim().isEmpty()){
                botReply = mainFunction("UDC");
            }
            displayBotReply(new MessageData(Constants.BOT, botReply,timeStamp));
        }

        messageInputView.setText("");

    }

    // displayBotReply() method
    private void displayBotReply(final MessageData messageData){

        botWritingView.setVisibility(View.VISIBLE);
        Glide.with(MainActivity.this).asGif().load(R.drawable.bot_animation).into(botWritingView);

        final String message = messageData.getMessage();
        int lengthOfMessage = message.length();

        int timeToWriteInMillis = lengthOfMessage*timePerCharacter; // each character taking 10ms to write
        if(timeToWriteInMillis > 3000){timeToWriteInMillis = 3000;} // not letting go beyond 3 secs

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                botWritingView.setVisibility(View.GONE);

                addMessage(messageData);

                if(messageData.getMessage().equals("Okay! I will clear up everything for you!")){
                    // the user requested to delete all chat data
                    deleteAllChatData();
                }

                // speak out the bot reply
                if(speechAllowed){
                    textToSpeech.setSpeechRate(0.9f);
                    textToSpeech.setPitch(1f);

                    textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null);
                }

            }
        }, timeToWriteInMillis); // the delay is according to the length of message

    }

    private void addMessage(MessageData messageData){
        String sender = messageData.getSender();
        String message = messageData.getMessage();
        String timestamp = messageData.getTimeStamp();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MessageContract.MessageEntry.COLUMN_SENDER, sender);
        contentValues.put(MessageContract.MessageEntry.COLUMN_MESSAGE, message);
        contentValues.put(MessageContract.MessageEntry.COLUMN_TIMESTAMP, timestamp);

        database.insert(MessageContract.MessageEntry.TABLE_NAME, null, contentValues);
        messageAdapter.swapCursor(getAllMessages());
    }

    // UTILITY METHODS

    // copying the file
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    // responding of bot to user's requests
    public static String mainFunction (String args) {

        MagicBooleans.trace_mode = false;
        Graphmaster.enableShortCuts = true;

        return chat.multisentenceRespond(args);
    }

    // functionality of the bot

    // method for searching a name in user's contact list
    public String getNumber(String name, Context context){

        String number = "";
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = new String[] {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};

        Cursor people = context.getContentResolver().query(uri, projection, null, null, null);
        if(people == null){
            return number;
        }

        int indexName = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        int indexNumber = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

        people.moveToFirst();
        do {
            String Name   = people.getString(indexName);
            String Number = people.getString(indexNumber);
            if(Name.equalsIgnoreCase(name)){return Number.replace("-", "");}
        } while (people.moveToNext());

        people.close();

        return number;
    }

    // method for placing a call
    private void makeCall(String name){

        String number;

        if(name.matches("[0-9]+") && name.length() > 2){
            // string only contains number
            number = name;
        } else{
            number = getNumber(name,MainActivity.this);
        }

        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + number));
            startActivity(callIntent);
        }catch (SecurityException e){
            Toast.makeText(this,"Calling Permission - DENIED!",Toast.LENGTH_SHORT).show();
        }
    }

    // method for searching through all the apps in the user's phone
    public String getAppName(String name) {
        name = name.toLowerCase();

        PackageManager pm = getPackageManager();
        List<ApplicationInfo> l = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo ai : l) {
            String n = pm.getApplicationLabel(ai).toString().toLowerCase();
            if (n.contains(name) || name.contains(n)){
                return ai.packageName;
            }
        }

        return "package.not.found";
    }

    // method for launching an app
    protected void launchApp(String packageName) {
        Intent mIntent = getPackageManager().getLaunchIntentForPackage(packageName);

        if (packageName.equals("package.not.found")) {
            Toast.makeText(getApplicationContext(), "I'm afraid, there's no such app!", Toast.LENGTH_SHORT).show();
        } else if (mIntent != null) {
            try {
                startActivity(mIntent);
            } catch (Exception err) {
                Log.i("darkbot", "App launch failed!");
                Toast.makeText(this, "I'm afraid, there's no such app!", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
