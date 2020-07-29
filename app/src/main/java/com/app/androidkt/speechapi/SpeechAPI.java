package com.app.androidkt.speechapi;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.ArraySet;
import android.util.Log;
import android.widget.Toast;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeechGrpc;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.protobuf.ByteString;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.okhttp.OkHttpChannelProvider;
import io.grpc.stub.StreamObserver;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SpeechAPI {

    public static final List<String> SCOPE = Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");

    private static final String PREFS = "SpeechService";
    private static final String PREF_ACCESS_TOKEN_VALUE = "access_token_value";
    private static final String PREF_ACCESS_TOKEN_EXPIRATION_TIME = "access_token_expiration_time";

    /**
     * We reuse an access token if its expiration time is longer than this.
     */
    private static final int ACCESS_TOKEN_EXPIRATION_TOLERANCE = 30 * 60 * 1000; // thirty minutes

    /**
     * We refresh the current access token before it expires.
     */
    private static final int ACCESS_TOKEN_FETCH_MARGIN = 60 * 1000; // one minute

    private static final String HOSTNAME = "speech.googleapis.com";
    private static final int PORT = 443;
    private static Handler mHandler;

    private int token = 1;

    //private final SpeechBinder mBinder = new SpeechBinder();
    private final ArrayList<Listener> mListeners = new ArrayList<>();

    private static final String BASE_URL = "https://acfcb9e8c91d.ngrok.io";

    final String TAG = MainActivity.class.getSimpleName();

    private MediaPlayer mPlayer = new MediaPlayer();

//    runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {

            token = 0;
        }
    };

    Thread threadS;

    private final StreamObserver<StreamingRecognizeResponse> mResponseObserver = new StreamObserver<StreamingRecognizeResponse>() {

        String text = null;

        @Override
        public void onNext(StreamingRecognizeResponse response) {
            String text = null;
            boolean isFinal = false;

            if (response.getResultsCount() > 0) {
                final StreamingRecognitionResult result = response.getResults(0);
                isFinal = result.getIsFinal();
                if (result.getAlternativesCount() > 0) {
                    final SpeechRecognitionAlternative alternative = result.getAlternatives(0);
                    text = alternative.getTranscript();
                }
            }

            if (text != null) {
                if (token == 1) {
                    //                Toast.makeText(mContext, "text", Toast.LENGTH_SHORT).show();
//                System.out.println(text);
                    for (Listener listener : mListeners) {
                        listener.onSpeechRecognized(text, isFinal);
                        this.text = text;
                        System.out.println("this is the text: " + this.text);
                    }
//                    timerHandler = new Handler();
                    System.out.println("it reaches here fast");
                } else {
                    if (text.equals("mambo")) {
                        token = 1;
                        System.out.println("=========================================----------------------------------------------- you said mambo this time");
                        // TODO: make a sound signifying that it is listening.
                    }
                }

            }

        }

        @Override
        public void onError(Throwable t) {
            Log.e(TAG, "Error calling the API.", t);
        }

        @Override
        public void onCompleted() {
            if (text != null && token == 1) {
//                MainActivity.mVoiceRecorder.stop();
//                if (!mPlayer.isPlaying()) {
//                    switch (text) {
//                        case "1":
//                        case "moja":
//                        case "mojwa":
////                            queryResponse("moja");
//                            Log.d(TAG, "--------------------------------------------------------------" + text);
//                            break;
//                        case "2":
//                        case "mbili":
//                        case "mbiri":
////                            queryResponse("mbili");
//                            Log.d(TAG, "--------------------------------------------------------------" + text);
//                            break;
//                        case "3":
//                        case "tatu":
//                        case "tadu":
//                        case "dadu":
////                            queryResponse("tatu");
//                            Log.d(TAG, "--------------------------------------------------------------" + text);
//                            break;
//                        default:
////                            queryResponse(text);
//                            Log.d(TAG, "--------------------------------------------------------------" + text);
//                            break;
//                    }
                Log.d(TAG, "--------------------------------------------------------------" + text);

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        token = 0;
                    } }, 4000);


//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        token = 0;
//                    }
//                }, 4000);
//                    Log.d(TAG, "=========================================================================== The audio is not playing");

//                } else {
//                    Log.d(TAG, "=========================================================================== The audio is still playing");
//                }

            }
            Log.i(TAG, "API completed.");
        }

    };

    private Context mContext;
    private volatile AccessTokenTask mAccessTokenTask;
    private final Runnable mFetchAccessTokenRunnable = new Runnable() {
        @Override
        public void run() {
            fetchAccessToken();
        }
    };
    private SpeechGrpc.SpeechStub mApi;
    private StreamObserver<StreamingRecognizeRequest> mRequestObserver;

    public SpeechAPI(Context mContext) {
        this.mContext = mContext;
        mHandler = new Handler();
        fetchAccessToken();
    }

    private final int MAKE_CALL_PERMISSION_REQUEST_CODE = 1;

    public void responseMaleSpeaker(String reply_text) {

////        Create a new Thread because JLayer is running on the current Thread and will make the application to lag
//        Thread threadS = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                MainActivity.mVoiceRecorder.stop();
//                System.out.println("the thread is preparing");
//            }
//        });
//
//        //We don't want the application to terminate before this Thread terminates
//        threadS.setDaemon(false);
//
//        //Start the Thread
//        threadS.start();


        final InputStream fis = mContext.getResources().openRawResource(R.raw.martha_credentials1);
//
        CredentialsProvider credentialsProvider = null;

        try {
            credentialsProvider = FixedCredentialsProvider.create(ServiceAccountCredentials.fromStream(fis));
        } catch (IOException e) {
            e.printStackTrace();
        }

        TextToSpeechSettings settings = null;

        try {
            settings = TextToSpeechSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Instantiates a client
        assert settings != null;
        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create(settings)) {
            // Set the text input to be synthesized
            SynthesisInput input = SynthesisInput.newBuilder().setText(reply_text).build();
//            SynthesisInput input = SynthesisInput.newBuilder().setText("Habari mimi ni Ona. Naue` za'ge kukusaidia leo?").build();
//            SynthesisInput input = SynthesisInput.newBuilder().setText("Mimi pia sijambo, ge unataka elimu ya afya ya uzasi na jinshiya?").build();
//            SynthesisInput input = SynthesisInput.newBuilder().setText("Napenda kukuambia nipo ili nikupatie elimu na kuwa rafiki yako.").build();
//                    SynthesisInput input = SynthesisInput.newBuilder().setText("Usinifikirie hivo, naomba unifikirie kama m'sai disi na rafiki yako.").build();
//            SynthesisInput input = SynthesisInput.newBuilder().setText("qua ninavokujua Halidi, unampenda sana Halima!").build();
//            SynthesisInput input = SynthesisInput.newBuilder().setText("Ni mwendo wa mi'chongo tu sasa h'ivi.").build();
//            SynthesisInput input = SynthesisInput.newBuilder().setText("hwewue ni Michael, ninafurahi kuku fundisha.").build();
//            SynthesisInput input = SynthesisInput.newBuilder().setText("ge unapenda sana vicekesho Saidi?.").build();
//            SynthesisInput input = SynthesisInput.newBuilder().setText("kunwa dawa za migomba ili upone korona qua haraka.").build();
//            SynthesisInput input = SynthesisInput.newBuilder().setText("Napenda kukutaarifu quwa hali ya hewa leo siyo n'zuri, qua hiyo usitoche, n'ge.").build();

            // Build the voice request, select the language code ("it-IT") and the ssml voice gender
            // ("neutral")
            VoiceSelectionParams voice =
                    VoiceSelectionParams.newBuilder()
                            .setLanguageCode("it-IT")
                            .setSsmlGender(SsmlVoiceGender.MALE)
                            .build();

            // Select the type of audio file you want returned
            AudioConfig audioConfig =
                    AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.MP3).build();

            // Perform the text-to-speech request on the text input with the selected voice parameters and
            // audio file type
            SynthesizeSpeechResponse response =
                    textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

//             Get the audio contents from the response
            ByteString audioContents = response.getAudioContent();

            // Write the response to the output file.
            File extDir = Environment.getExternalStorageDirectory();
            File file = new File(extDir, "outputaudio.mp3");
            try (OutputStream out = new FileOutputStream(file)) {
                out.write(audioContents.toByteArray());
                System.out.println("Audio content written to file \"outputaudio.mp3\"");
//                Toast.makeText(this, "It reaches here", Toast.LENGTH_SHORT).show();

                MediaPlayer mp = new MediaPlayer();
                mp.setDataSource(file.getPath());//Write your location here
                mp.prepare();
                mp.start();

                mPlayer = mp;

                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
//                        Thread thread = new Thread(new Runnable() {
//                            @Override
//                            public void run() {
//                                if (MainActivity.mVoiceRecorder != null)
//                        if (!threadS.isAlive()) {
//                            System.out.println("the thread is not alive");
//                        } else
//                            System.out.println("the thread is alive");

                        if (checkPermission(Manifest.permission.CALL_PHONE)) {

                            if (reply_text.toLowerCase().contains("simu inapigwa")) {
                                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:0712957994"));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                mContext.startActivity(intent);
                            }

                        } else {
                            String[] permission = {Manifest.permission.CALL_PHONE};
                            ActivityCompat.requestPermissions(
                                    (Activity) mContext,
                                    permission,
                                    MAKE_CALL_PERMISSION_REQUEST_CODE
                            );
                        }

                        MainActivity.mVoiceRecorder.start();
//                            }
//                        });
//
//                        //We don't want the application to terminate before this Thread terminates
//                        thread.setDaemon(false);
//
//                        //Start the Thread
//                        thread.start();
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void responseFemaleSpeaker(String reply_text) {

//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                main.stopVoiceRecorder();
//            }
//        });
//
//        //We don't want the application to terminate before this Thread terminates
//        thread.setDaemon(false);
//
//        //Start the Thread
//        thread.start();

        final InputStream fis = mContext.getResources().openRawResource(R.raw.martha_credentials1);
//
        CredentialsProvider credentialsProvider = null;

        try {
            credentialsProvider = FixedCredentialsProvider.create(ServiceAccountCredentials.fromStream(fis));
        } catch (IOException e) {
            e.printStackTrace();
        }

        TextToSpeechSettings settings = null;

        try {
            settings = TextToSpeechSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Instantiates a client
        assert settings != null;
        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create(settings)) {
            // Set the text input to be synthesized
            SynthesisInput input = SynthesisInput.newBuilder().setText("Habari mimi ni Ona. Naue` za'ge kukusaidia leo?").build();
//            SynthesisInput input = SynthesisInput.newBuilder().setText("Mimi pia sijambo, ge unataka elimu ya afya ya uzasi na jinshiya?").build();
//            SynthesisInput input = SynthesisInput.newBuilder().setText("Napenda kukuambia nipo ili nikupatie elimu na kuwa rafiki yako.").build();
//                    SynthesisInput input = SynthesisInput.newBuilder().setText("Usinifikirie hivo, naomba unifikirie kama m'sai disi na rafiki yako.").build();
//            SynthesisInput input = SynthesisInput.newBuilder().setText("qua ninavokujua Halidi, unampenda sana Halima!").build();
//            SynthesisInput input = SynthesisInput.newBuilder().setText("Ni mwendo wa mi'chongo tu sasa h'ivi.").build();
//            SynthesisInput input = SynthesisInput.newBuilder().setText("hwewue ni Michael, ninafurahi kuku fundisha.").build();
//            SynthesisInput input = SynthesisInput.newBuilder().setText("ge unapenda sana vicekesho Saidi?.").build();
//            SynthesisInput input = SynthesisInput.newBuilder().setText("kunwa dawa za migomba ili upone korona qua haraka.").build();
//            SynthesisInput input = SynthesisInput.newBuilder().setText("Napenda kukutaarifu quwa hali ya hewa leo siyo n'zuri, qua hiyo usitoche, n'ge.").build();

            // Build the voice request, select the language code ("it-IT") and the ssml voice gender
            // ("neutral")
            VoiceSelectionParams voice =
                    VoiceSelectionParams.newBuilder()
                            .setLanguageCode("it-IT")
                            .setSsmlGender(SsmlVoiceGender.FEMALE)
                            .build();

            // Select the type of audio file you want returned
            AudioConfig audioConfig =
                    AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.MP3).build();

            // Perform the text-to-speech request on the text input with the selected voice parameters and
            // audio file type
            SynthesizeSpeechResponse response =
                    textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

//             Get the audio contents from the response
            ByteString audioContents = response.getAudioContent();

            // Write the response to the output file.
            File extDir = Environment.getExternalStorageDirectory();
            File file = new File(extDir, "outputaudio.mp3");
            try (OutputStream out = new FileOutputStream(file)) {
                out.write(audioContents.toByteArray());
                System.out.println("Audio content written to file \"outputaudio.mp3\"");
//                Toast.makeText(this, "It reaches here", Toast.LENGTH_SHORT).show();

                MediaPlayer mp = new MediaPlayer();
                mp.setDataSource(file.getPath());//Write your location here
                mp.prepare();
                mp.start();

                mPlayer = mp;

                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
//                        Thread thread = new Thread(new Runnable() {
//                            @Override
//                            public void run() {
                        if (checkPermission(Manifest.permission.CALL_PHONE)) {

                            if (reply_text.toLowerCase().contains("simu inapigwa")) {
                                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:0712957994"));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                mContext.startActivity(intent);
                            }

                        } else {
                            String[] permission = {Manifest.permission.CALL_PHONE};
                            ActivityCompat.requestPermissions(
                                    (Activity) mContext,
                                    permission,
                                    MAKE_CALL_PERMISSION_REQUEST_CODE
                            );
                        }

                        MainActivity.mVoiceRecorder.start();
//                            }
//                        });
//
//                        //We don't want the application to terminate before this Thread terminates
//                        thread.setDaemon(false);
//
//                        //Start the Thread
//                        thread.start();
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void queryResponse(String message) {

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

//        OkHttpClient okHttpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
//                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        QueryApi queryApi = retrofit.create(QueryApi.class);

        Call<List<QueryResponse>> call = queryApi.queryResponse(new Query(message));

        call.enqueue(new Callback<List<QueryResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<QueryResponse>> call, @NonNull Response<List<QueryResponse>> response) {
                if (response.isSuccessful()) {
                    List<QueryResponse> postList = response.body();
                    assert postList != null;
                    Log.d(TAG, "Returned count " + postList.size());

                    for (QueryResponse responseQ : postList) {
                        if (responseQ.getText() != null) {
                            if (mPlayer.isPlaying()) {
                                System.out.println("audio is still playing.");
                            } else {
                                responseMaleSpeaker(responseQ.getText());
                                System.out.println("-------------------------- " + responseQ.getText());
                            }
                        }
                    }
                } else {
                    responseMaleSpeaker("Bado sijakuelewa vizuri");
//                    System.out.println("It gives null response!");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<QueryResponse>> call, @NonNull Throwable t) {
                //showErrorMessage();
                Log.d(TAG, "error loading from API");
                Log.d(TAG, Objects.requireNonNull(t.getLocalizedMessage()));
            }
        });
    }

    private Boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(
                mContext,
                permission
        ) == PackageManager.PERMISSION_GRANTED;
    }

    public void destroy() {
        mHandler.removeCallbacks(mFetchAccessTokenRunnable);
        mHandler = null;
        // Release the gRPC channel.
        if (mApi != null) {
            final ManagedChannel channel = (ManagedChannel) mApi.getChannel();
            if (channel != null && !channel.isShutdown()) {
                try {
                    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Error shutting down the gRPC channel.", e);
                }
            }
            mApi = null;
        }
    }

    private void fetchAccessToken() {
        if (mAccessTokenTask != null) {
            return;
        }
        mAccessTokenTask = new AccessTokenTask();
        mAccessTokenTask.execute();
    }

    public void addListener(@NonNull Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(@NonNull Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * Starts recognizing speech audio.
     *
     * @param sampleRate The sample rate of the audio.
     */
    public void startRecognizing(int sampleRate) {
        if (mApi == null) {
            Log.w(TAG, "API not ready. Ignoring the request.");
            return;
        }

        // Configure the API
        mRequestObserver = mApi.streamingRecognize(mResponseObserver);

        StreamingRecognitionConfig streamingConfig = StreamingRecognitionConfig.newBuilder()
                .setConfig(RecognitionConfig.newBuilder()
                        .setLanguageCode("sw-TZ")
                        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                        .setSampleRateHertz(sampleRate)
                        .build()
                )
                .setInterimResults(true)
                .setSingleUtterance(true)
                .build();

        StreamingRecognizeRequest streamingRecognizeRequest = StreamingRecognizeRequest.newBuilder().setStreamingConfig(streamingConfig).build();
        mRequestObserver.onNext(streamingRecognizeRequest);
    }

    /**
     * Recognizes the speech audio. This method should be called every time a chunk of byte buffer
     * is ready.
     *
     * @param data The audio data.
     * @param size The number of elements that are actually relevant in the {@code data}.
     */
    public void recognize(byte[] data, int size) {
        if (mRequestObserver == null) {
            return;
        }
        // Call the streaming recognition API
        mRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                .setAudioContent(ByteString.copyFrom(data, 0, size))
                .build());
    }

    /**
     * Finishes recognizing speech audio.
     */
    public void finishRecognizing() {
        if (mRequestObserver == null) {
            return;
        }
        mRequestObserver.onCompleted();
        mRequestObserver = null;
    }

    public interface Listener {
        //Called when a new piece of text was recognized by the Speech API.
        void onSpeechRecognized(String text, boolean isFinal);
    }

    private class AccessTokenTask extends AsyncTask<Void, Void, AccessToken> {

        @Override
        protected AccessToken doInBackground(Void... voids) {

            final SharedPreferences prefs = mContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String tokenValue = prefs.getString(PREF_ACCESS_TOKEN_VALUE, null);
            long expirationTime = prefs.getLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME, -1);

            // Check if the current token is still valid for a while
            if (tokenValue != null && expirationTime > 0) {
                if (expirationTime > System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TOLERANCE) {
                    return new AccessToken(tokenValue, new Date(expirationTime));
                }
            }

            final InputStream stream = mContext.getResources().openRawResource(R.raw.martha_credentials1);
            try {
                final GoogleCredentials credentials = GoogleCredentials.fromStream(stream).createScoped(SCOPE);
                final AccessToken token = credentials.refreshAccessToken();
                prefs.edit()
                        .putString(PREF_ACCESS_TOKEN_VALUE, token.getTokenValue())
                        .putLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME, token.getExpirationTime().getTime())
                        .apply();
                return token;
            } catch (IOException e) {
                Log.e(TAG, "Failed to obtain access token.", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(AccessToken accessToken) {
            mAccessTokenTask = null;
            final ManagedChannel channel = new OkHttpChannelProvider()
                    .builderForAddress(HOSTNAME, PORT)
                    .nameResolverFactory(new DnsNameResolverProvider())
                    .intercept(new GoogleCredentialsInterceptor(new GoogleCredentials(accessToken)
                            .createScoped(SCOPE)))
                    .build();
            mApi = SpeechGrpc.newStub(channel);

            // Schedule access token refresh before it expires
            if (mHandler != null) {
                mHandler.postDelayed(mFetchAccessTokenRunnable,
                        Math.max(accessToken.getExpirationTime().getTime() - System.currentTimeMillis() - ACCESS_TOKEN_FETCH_MARGIN, ACCESS_TOKEN_EXPIRATION_TOLERANCE));
            }
        }
    }
}
