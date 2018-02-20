package com.example.anupam.musicplayer;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
//import android.icu.math.BigDecimal;
import java.math.BigDecimal;

import android.media.AudioManager;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.IOException;

public class playActivity extends ListActivity {

    private static final int UPDATE_FREQUENCY = 500;
    private static final int STEP_VALUE = 4000;

    private MediaCursorAdapter mediaAdapter = null;
    private TextView selectedFile = null;
    private SeekBar seekbar = null;
    private MediaPlayer player = null;
    private ImageButton playButton = null;
    private ImageButton prevButton = null;
    private ImageButton nextButton = null;

    private boolean isStarted = true;
    private String currentFile = " ";
    private boolean isMoveingSeekBar = false;

    private final Handler handler = new Handler();

    private final Runnable updatePositionRunnable = new Runnable() {
        @Override
        public void run() {
            updatePosition();
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        selectedFile = (TextView) findViewById(R.id.selectedfile);
        seekbar = (SeekBar) findViewById(R.id.seekbar);
        playButton = (ImageButton) findViewById(R.id.play);
        prevButton = (ImageButton) findViewById(R.id.prev);
        nextButton = (ImageButton) findViewById(R.id.next);

        player = new MediaPlayer();

        player.setOnCompletionListener(onCompletion);
        player.setOnErrorListener(onError);
        seekbar.setOnSeekBarChangeListener(seekBarChanged);

        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);

        if(null != cursor){
            cursor.moveToFirst();

            mediaAdapter = new MediaCursorAdapter(this, R.layout.listitem, cursor);

            setListAdapter(mediaAdapter);

            playButton.setOnClickListener(onButtonClick);
            nextButton.setOnClickListener(onButtonClick);
            prevButton.setOnClickListener(onButtonClick);
        }

    }

    AudioManager am;/*= (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    AudioManager.OnAudioFocusChangeListener afChangeListener;*/


    AudioManager.OnAudioFocusChangeListener afChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {

                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {

                        am.abandonAudioFocus(afChangeListener);
                        player.stop();
                    } else if (focusChange ==
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {

                    } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {

                        player.start();
                    }
                }
            };

    @Override
    protected void onListItemClick(ListView list, View view, int position, long id) {
        super.onListItemClick(list, view, position, id);

        //currentFile = view.getTag().toString();
        currentFile = (String) view.getTag();

        startPlay(currentFile);
    }

    @Override

    protected void onDestroy(){
        super.onDestroy();

        handler.removeCallbacks(updatePositionRunnable);
        player.stop();
        player.reset();
        player.release();

        player = null;
    }

    private void startPlay(String file){
        Log.i("Selected : ", file);

        selectedFile.setText(file);
        seekbar.setProgress(0);

        player.stop();
        player.reset();

        try{
            player.setDataSource(file);
            player.prepare();
            player.start();
        }
        catch(IllegalArgumentException e){
            e.printStackTrace();
        }
        catch(IllegalStateException e){
            e.printStackTrace();
        }
        catch(IOException e) {
            e.printStackTrace();
        }

        seekbar.setMax(player.getDuration());
        playButton.setImageResource(android.R.drawable.ic_media_pause);

        updatePosition();

        isStarted = true;
    }

    private void stopPlay(){
        player.stop();
        player.reset();
        playButton.setImageResource(R.drawable.ic_pause_black_24dp);
        handler.removeCallbacks(updatePositionRunnable);
        seekbar.setProgress(0);

        isStarted = false;
    }

    private void updatePosition(){
        handler.removeCallbacks(updatePositionRunnable);

        seekbar.setProgress(player.getCurrentPosition());

        handler.postDelayed(updatePositionRunnable, UPDATE_FREQUENCY);
    }

    private class MediaCursorAdapter extends SimpleCursorAdapter{

        public MediaCursorAdapter(Context context, int layout, Cursor c){
            super(context, layout, c,
            new String[]{MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.TITLE, MediaStore.Audio.AudioColumns.DURATION},
            new int[]{R.id.displayname, R.id.title, R.id.duration});
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor){
            TextView title = (TextView) view.findViewById(R.id.title);
            TextView name = (TextView) view.findViewById(R.id.displayname);
            TextView duration = (TextView) view.findViewById(R.id.duration);

            name.setText(cursor.getString(
                    cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            ));

            title.setText(cursor.getString(
                    cursor.getColumnIndex(MediaStore.MediaColumns.TITLE)
            ));

            long durationInMs = Long.parseLong(cursor.getString(
               cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)
            ));

            double durationInMin = ((double) durationInMs/1000.0)/60.0;

            durationInMin = new BigDecimal(Double.toString(durationInMin)).setScale(2, BigDecimal.ROUND_UP).doubleValue();

            duration.setText("" + durationInMin);

            view.setTag(cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA)));
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent){
            LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.listitem, parent, false);

            bindView(v, context, cursor);

            return v;
        }
    }

    private View.OnClickListener onButtonClick = new View.OnClickListener(){
         @Override
        public void onClick(View v) {
             switch (v.getId()) {
                 case R.id.play: {
                     if (player.isPlaying()) {
                         handler.removeCallbacks(updatePositionRunnable);
                         player.pause();
                         playButton.setImageResource(android.R.drawable.ic_media_play);
                     } else {
                         if (isStarted) {
                             player.start();
                             playButton.setImageResource(android.R.drawable.ic_media_pause);

                             updatePosition();
                         } else {
                             startPlay(currentFile);
                         }
                     }
                     break;
                 }
                 case R.id.next: {
                     int seekto = player.getCurrentPosition() + STEP_VALUE;
                     if (seekto > player.getDuration())
                         seekto = player.getDuration();

                     player.pause();
                     player.seekTo(seekto);
                     player.start();

                     break;
                 }
                 case R.id.prev: {
                     int seekto = player.getCurrentPosition() - STEP_VALUE;

                     if (seekto < 0)
                         seekto = 0;

                     player.pause();
                     player.seekTo(seekto);
                     player.start();

                     break;
                 }

             }
         }
    };

    private MediaPlayer.OnCompletionListener onCompletion = new MediaPlayer.OnCompletionListener(){
        @Override
        public void onCompletion(MediaPlayer mp){
            stopPlay();
        }
    };

    private MediaPlayer.OnErrorListener onError = new MediaPlayer.OnErrorListener(){
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra){
            return false;
        }

    };

    private SeekBar.OnSeekBarChangeListener seekBarChanged = new SeekBar.OnSeekBarChangeListener(){
        @Override
        public void onStopTrackingTouch(SeekBar seekBar){
            isMoveingSeekBar = false;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar){
            isMoveingSeekBar = true;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
            if(isMoveingSeekBar) {
                player.seekTo(progress);

                Log.i("OnSeekBarChangeListener", "OnProgressChanged");
            }
        }

    };
}