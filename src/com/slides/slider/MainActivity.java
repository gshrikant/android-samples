package com.slides.slider;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.slides.slider.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class MainActivity extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /*
     * App-specific data
     * */
    public int number;
    public String host_addr;
    public String host_port;
  
    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);
        // timerText.setText("0");
        
        final EditText timerText = (EditText) findViewById(R.id.fullscreen_content); 
        
        final EditText address_field = (EditText) findViewById(R.id.ip_field);
        final EditText port_field = (EditText) findViewById(R.id.port_field);
        final View send_button = findViewById(R.id.dummy_button);
        
        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });
        
        
        contentView.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keycode, KeyEvent event) {
				if (keycode == KeyEvent.KEYCODE_VOLUME_DOWN) {
					number--;
					String _strnum = Integer.toString(number);
					timerText.setText(_strnum);
					SocketSend async_socket = new SocketSend('d', host_addr, host_port);
					async_socket.execute();					
				}
				else {
					number++;
					String _strnum = Integer.toString(number);
					timerText.setText(_strnum);
					SocketSend async_socket = new SocketSend('u', host_addr, host_port);
					async_socket.execute();
				}
				return false;
			}
		});

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        send_button.setOnTouchListener(mDelayHideTouchListener);
        send_button.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				host_addr = address_field.getText().toString();
				host_port = port_field.getText().toString();
			}
		});
    }

/*    @Override
    protected void onStart()) {
    	super.onStart();
    	
    }
    
*/    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
    
	/**
	 * Shamelessly copied from a great SO answer
	 * {@link http://stackoverflow.com/questions/2874743/android-volume-buttons-used-in-my-application}
	 * 
	 */	
	@Override
	public boolean dispatchKeyEvent(KeyEvent event)
	{
		int action = event.getAction();
		int keycode = event.getKeyCode();
		switch (keycode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			// If pressed
			if (action == KeyEvent.ACTION_DOWN) {
				return true;
			}
				
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (action == KeyEvent.ACTION_DOWN) {
				return true;
			}
		default:
			return super.dispatchKeyEvent(event);
		}
	}
    
	
	/*
	 * Create asynchronous task to create and maintain socket connection
	 */
	public class SocketSend extends AsyncTask<Void, Void, Void> 
	{
		char char_to_send;
		String addr;
		int port;
		// Constructor
		SocketSend(char c, String address, String hport)
		{
			char_to_send = c;
			addr = address;
			port = Integer.parseInt(hport);
		}
		
		@Override
		protected Void doInBackground(Void... params)
		{
			Socket sock = null;
			InetAddress server_addr;
			try {
				server_addr = InetAddress.getByName(addr);
				sock = new Socket(server_addr, port);
			} 
			catch (UnknownHostException e) {}
			catch (IOException e) {}
			try {
				PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
																		sock.getOutputStream())), true);
				out.println(char_to_send);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
}