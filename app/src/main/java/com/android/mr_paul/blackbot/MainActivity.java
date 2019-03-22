package com.android.mr_paul.blackbot;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.media.Image;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.android.mr_paul.blackbot.Adapters.ChatDataAdapter;
import com.android.mr_paul.blackbot.DataTypes.MessageData;
import com.android.mr_paul.blackbot.UtilityPackage.Constants;

import org.alicebot.ab.AIMLProcessor;
import org.alicebot.ab.AIMLProcessorExtension;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListView listView;
    private EditText messageInputView;
    private ImageView messageSendButton;
    ArrayList<MessageData> messageDataList;

    public Bot bot;
    public static Chat chat;

    private ChatDataAdapter chatDataAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.list_view);
        messageInputView = findViewById(R.id.message_input_view);
        messageSendButton = findViewById(R.id.message_send_button);

        messageDataList = new ArrayList<>();
        chatDataAdapter = new ChatDataAdapter(this, R.layout.activity_main, messageDataList);

        AssetManager assetManager = getResources().getAssets();
        File cacheDirectory = new File(getCacheDir().toString() + "/mr_paul/bots/darkbot");
        cacheDirectory.mkdirs();

        if(cacheDirectory.exists()){
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
            Toast.makeText(this, "Initialization Completed!", Toast.LENGTH_SHORT).show();
        }

        MagicStrings.root_path = getCacheDir().toString() + "/mr_paul";

        // initialize the bot
        AIMLProcessor.extension = new PCAIMLProcessorExtension();
        bot = new Bot("darkbot", MagicStrings.root_path, "chat");
        chat = new Chat(bot);


        listView.setAdapter(chatDataAdapter);
        messageSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendChatMessage();
            }
        });

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

        messageDataList.add(new MessageData(Constants.USER,message,timeStamp));

        if(message.toUpperCase().startsWith("CALL")){
            // calling a phone number as requested by user
            String[] temp = message.split(" ", 2);
            messageDataList.add(new MessageData(Constants.BOT, "Placing a call on " + temp[1] ,timeStamp));
            chatDataAdapter.notifyDataSetChanged();
            makeCall(temp[1]);
        } else if(message.toUpperCase().startsWith("OPEN")){
            // call intent to app, requested by user
            String[] temp = message.split(" ", 2);
            messageDataList.add(new MessageData(Constants.BOT, "Sure! Trying to open " + temp[1] + "...",timeStamp));
            chatDataAdapter.notifyDataSetChanged();
            launchApp(getAppName(temp[1]));
        } else{
            // chat with bot - save the reply from the bot
            messageDataList.add(new MessageData(Constants.BOT, mainFunction(message),timeStamp));
            chatDataAdapter.notifyDataSetChanged();
        }

        messageInputView.setText("");

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

        String number = getNumber(name,MainActivity.this);
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + number));
        try {
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

        if(packageName.equals("package.not.found")) {
            Toast.makeText(getApplicationContext(),"I'm afraid, there's no such app!", Toast.LENGTH_SHORT).show();
        }
        else if (mIntent != null) {
            try {
                startActivity(mIntent);
            } catch (Exception err) {
                Log.i("darkbot","App launch failed!");
                Toast.makeText(this, "I'm afraid, there's no such app!", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
