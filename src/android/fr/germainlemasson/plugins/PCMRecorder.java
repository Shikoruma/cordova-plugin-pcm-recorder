package fr.germainlemasson.plugins;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ShortBuffer;
import java.util.UUID;

public class PCMRecorder extends CordovaPlugin {

    private String outputFile;
    private AudioRecord recorder = null;
    private boolean isRecording = false;

    private static final int RECORDER_SAMPLERATE = 32000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;


    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format


    //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        Context context = cordova.getActivity().getApplicationContext();

        if (action.equals("record")) {

            outputFile = context.getDir("tmp", Context.MODE_PRIVATE)
                    .getAbsoluteFile() + "/" + UUID.randomUUID().toString();
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                            RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                            RECORDER_AUDIO_ENCODING, bufferSize * BytesPerElement);
                    ShortBuffer sb= ShortBuffer.allocate(1000000);
                    recorder.startRecording();
                    isRecording = true;

                    short sData[] = new short[bufferSize];

                    FileOutputStream os = null;
                    try {
                        os = new FileOutputStream(outputFile);
                    } catch (FileNotFoundException e) {
                        callbackContext.error(e.getMessage());
                    }
                    int max=0;
                    int i;
                    int readnb;
                    short val;
                    long starttime = System.currentTimeMillis();
                    while (isRecording && sb.position()<5000000) {
                        // gets the voice output from microphone to byte format
                        readnb=recorder.read(sData, 0, bufferSize);
                        for (i=0;i<readnb;i++){
                            val=sData[i];
                            max = max<Math.abs(val) ? Math.abs(val) : max;
                            sb.put(sData[i]);
                        }
                        //System.out.println("Short wirting to file" + sData.toString());
                        /*try {
                            // // writes the data to file from buffer
                            // // stores the voice buffer
                            byte bData[] = short2byte(sData);
                            os.write(bData, 0, bufferSize * BytesPerElement);
                        } catch (IOException e) {
                            callbackContext.error(e.getMessage());
                        }*/
                    }
                    int shortArrsize = sb.position();
                    byte[] bytes = new byte[shortArrsize * 2];
                    float ratio =  32767.0f/max;
                    for (i = 0; i < shortArrsize; i++) {
                        val=(short)(sb.get(i)*ratio);
                        bytes[i * 2] = (byte) (val & 0x00FF);
                        bytes[(i * 2) + 1] = (byte) (val >> 8);
                    }
                    try {
                        os.write(bytes);
                        os.close();
                    } catch (IOException e) {
                        callbackContext.error(e.getMessage());
                    }
                    cordova.getThreadPool().execute(new Runnable() {
                        public void run() {
                            callbackContext.success(outputFile);
                        }
                    });

                }
            });
            return true;
        }

        if (action.equals("stop")) {
            if (null != recorder) {
                isRecording = false;
                recorder.stop();
                recorder.release();
                recorder = null;

                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        callbackContext.success(outputFile);
                    }
                });
                return true;
            }
            return false;
        }

        if (action.equals("playback")) {

            final String filePath = args.getString(0);

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    byte[] byteData = null;
                    File file = null;
                    file = new File(filePath); // for ex. path= "/sdcard/samplesound.pcm" or "/sdcard/samplesound.wav"
                    byteData = new byte[(int) file.length()];
                    FileInputStream in = null;
                    try {
                        in = new FileInputStream( file );
                        in.read( byteData );
                        in.close();

                    } catch (Exception e) {
                        callbackContext.error(e.getMessage());
                    }

                    int bufferSize = AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO, RECORDER_AUDIO_ENCODING);
                    AudioTrack mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

                    if (mAudioTrack!=null) {
                        mAudioTrack.play();
                        mAudioTrack.write(byteData, 0, byteData.length);
                        mAudioTrack.stop();
                        mAudioTrack.release();
                        callbackContext.success();
                    }
                    else
                        callbackContext.error("Audio track is not initialised ");
                    

                }
            });

            return true;
        }
        return false;
    }

}
