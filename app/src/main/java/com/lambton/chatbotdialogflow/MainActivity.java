package com.lambton.chatbotdialogflow;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonElement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import ai.api.AIListener;
import ai.api.AIServiceContext;
import ai.api.AIServiceContextBuilder;
import ai.api.BuildConfig;
import ai.api.android.AIConfiguration;
import ai.api.android.AIDataService;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

public class MainActivity extends AppCompatActivity  {

    private static final String TAG = MainActivity.class.getSimpleName();
    private String uuid = UUID.randomUUID().toString();

    Button submitButton;
    TextView resultTextView;
    EditText queryEditText;
    TextToSpeech textToSpeech;
    TextView test;
    private ArrayList<String> entityArrayList;

    private AIRequest aiRequest;
    private AIDataService aiDataService;
    private AIServiceContext customAIServiceContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        submitButton = findViewById(R.id.button);
        resultTextView = findViewById(R.id.textView);
        queryEditText = findViewById(R.id.editText);

       initChatbot();
       processJson();
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(v);
            }
        });

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.UK);
                }
            }
        });

    }

    private void initChatbot() {
        final AIConfiguration config = new AIConfiguration("e194dd6256564e10a03f2e07db49619d",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        aiDataService = new AIDataService(this, config);
        customAIServiceContext = AIServiceContextBuilder.buildFromSessionId(uuid);// helps to create new session whenever app restarts
        aiRequest = new AIRequest();
    }

    private void sendMessage(View view) {
        String msg = queryEditText.getText().toString();
        if (msg.trim().isEmpty()) {
            Toast.makeText(MainActivity.this, "Please enter your query!", Toast.LENGTH_LONG).show();
        } else {
            queryEditText.setText("");
            aiRequest.setQuery(msg);
           RequestTask requestTask = new RequestTask(MainActivity.this, aiDataService, customAIServiceContext);
           requestTask.execute(aiRequest);

        }
    }

    public void callback(AIResponse aiResponse) {

        Result result = aiResponse.getResult();

        if (aiResponse != null) {
            // process aiResponse here
            String botReply = aiResponse.getResult().getFulfillment().getSpeech();
            Log.d(TAG, "Bot Reply: " + botReply);
           resultTextView.setText(botReply);
            String toSpeak = resultTextView.getText().toString();
            textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);

           /* String parameterString = "";
            if (result.getParameters() != null && !result.getParameters().isEmpty()) {
                for (final Map.Entry<String, JsonElement> entry : result.getParameters().entrySet()) {
                    parameterString += "(" + entry.getKey() + ", " + entry.getValue() + ") ";
                }
            }*/

        } else {
            Log.d(TAG, "Bot Reply: Null");
            resultTextView.setText("There was some communication issue. Please Try again!");
        }
        
    }

    public void onPause(){
        if(textToSpeech !=null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onPause();
    }

    public  String loadJSONFromAsset()
    {
        String json;
        try {
            InputStream inputStream = getAssets().open("drinks.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return json;
    }

    private void processJson()
    {
        String js = loadJSONFromAsset();
        if(js !=null)
        {
            try {
                JSONArray mJSONArray=new JSONArray(js);
                entityArrayList = new ArrayList<>();
                for(int i=0;i<mJSONArray.length();i++) {
                    JSONObject mJSONObj=mJSONArray.getJSONObject(i);
                    if(mJSONObj.has("value")) {
                        String id = mJSONObj.getString("value");
                        entityArrayList.add(id);
                    }
                }
                } catch (JSONException e) {
                e.printStackTrace();
            }
            }
        }
    }

