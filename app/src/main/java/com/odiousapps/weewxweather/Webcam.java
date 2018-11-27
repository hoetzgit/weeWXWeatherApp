package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

class Webcam
{
    private Common common;
    private ImageView iv;
    private static Bitmap bm;
    private SwipeRefreshLayout swipeLayout;
    private boolean dark_theme = false;

    Webcam(Common common)
    {
        this.common = common;
	    dark_theme = common.GetBoolPref("dark_theme", false);
    }

    View myWebcam(LayoutInflater inflater, ViewGroup container)
    {
        View rootView = inflater.inflate(R.layout.fragment_webcam, container, false);
        iv = rootView.findViewById(R.id.webcam);

	    if(dark_theme)
		    iv.setBackgroundColor(0xff000000);

	    iv.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                Vibrator vibrator = (Vibrator)common.context.getSystemService(Context.VIBRATOR_SERVICE);
                if(vibrator != null)
                    vibrator.vibrate(250);
                Common.LogMessage("long press");
                reloadWebView(true);
                return true;
            }
        });

	    swipeLayout = rootView.findViewById(R.id.swipeToRefresh);
	    swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
	    {
		    @Override
		    public void onRefresh()
		    {
			    swipeLayout.setRefreshing(true);
			    Common.LogMessage("onRefresh();");
			    reloadWebView(true);
		    }
	    });

        reloadWebView(false);
        return rootView;
    }

    private void reloadWebView(final boolean force)
    {
        Common.LogMessage("reload webcam...");
        final String webURL = common.GetStringPref("WEBCAM_URL", "");

        if(webURL.equals(""))
        {
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
                iv.setImageDrawable(common.context.getApplicationContext().getDrawable(R.drawable.nowebcam));
            } else {
                iv.setImageDrawable(common.context.getResources().getDrawable(R.drawable.nowebcam));
            }

            return;
        }

	    File file = new File(common.context.getFilesDir(), "webcam.jpg");
	    if(file.exists())
	    {
		    Common.LogMessage("file: "+file.toString());
		    try
		    {
			    BitmapFactory.Options options = new BitmapFactory.Options();
			    bm = BitmapFactory.decodeFile(file.toString(), options);
			    iv.setImageBitmap(bm);
		    } catch (Exception e) {
			    e.printStackTrace();
		    }
	    }

	    Thread t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
	            int period = 0;
	            int curtime = Math.round(System.currentTimeMillis() / 1000);

	            File file = new File(common.context.getFilesDir(), "webcam.jpg");

	            Common.LogMessage("curtime = " + curtime + ", file.lastModified() == " + Math.round(file.lastModified() / 1000));

	            if(!force)
	            {
		            int pos = common.GetIntPref("updateInterval", 1);
		            if (pos <= 0)
			            return;

		            switch (pos)
		            {
			            case 1:
				            period = 5 * 60 - 10;
				            break;
			            case 2:
				            period = 10 * 60 - 10;
				            break;
			            case 3:
				            period = 15 * 60 - 10;
				            break;
			            case 4:
				            period = 30 * 60 - 10;
				            break;
			            case 5:
				            period = 60 * 60 - 10;
				            break;
			            default:
				            return;
		            }
	            }

	            if(force || !file.exists() || Math.round(file.lastModified() / 1000) + period < curtime)
	            {
		            if(downloadWebcam(webURL, common.context.getFilesDir()))
			            Common.LogMessage("done downloading, prompt handler to draw to iv");
		            else
			            Common.LogMessage("Skipped downloading");
	            }

	            handlerDone.sendEmptyMessage(0);
            }
        });

        t.start();
    }

    static boolean downloadWebcam(String webURL, File getFiles)
    {
    	try
	    {
		    Common.LogMessage("starting to download bitmap from: " + webURL);
		    URL url = new URL(webURL);
		    if (webURL.toLowerCase().endsWith(".mjpeg") || webURL.toLowerCase().endsWith(".mjpg"))
		    {
			    MjpegRunner mr = new MjpegRunner(url);
			    mr.run();

			    try
			    {
				    while (mr.bm == null)
					    Thread.sleep(1000);

				    Common.LogMessage("trying to set bm");
				    bm = mr.bm;
			    } catch (Exception e) {
				    e.printStackTrace();
				    return false;
			    }
		    } else {
			    InputStream is = url.openStream();
			    bm = BitmapFactory.decodeStream(is);
		    }

		    int width = bm.getWidth();
		    int height = bm.getHeight();

		    Matrix matrix = new Matrix();
		    matrix.postRotate(90);

		    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bm, width, height, true);
		    bm = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

		    FileOutputStream out = null;
		    File file = new File(getFiles, "webcam.jpg");

		    try
		    {
			    out = new FileOutputStream(file);
			    bm.compress(Bitmap.CompressFormat.JPEG, 85, out); // bmp is your Bitmap instance
		    } catch (Exception e) {
			    e.printStackTrace();
			    return false;
		    } finally {
			    try
			    {
				    if (out != null)
					    out.close();
			    } catch (IOException e) {
				    e.printStackTrace();
			    }
		    }

		    return true;
	    } catch (Exception e) {
    		e.printStackTrace();
    		return false;
	    }
    }

    @SuppressLint("HandlerLeak")
    private Handler handlerDone = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            iv.setImageBitmap(bm);
            iv.invalidate();
	        swipeLayout.setRefreshing(false);
        }
    };

	void doResume()
	{
		IntentFilter filter = new IntentFilter();
		filter.addAction(Common.UPDATE_INTENT);
		filter.addAction(Common.EXIT_INTENT);
		common.context.registerReceiver(serviceReceiver, filter);
		Common.LogMessage("webcam.java -- registerReceiver");
	}

    void doPause()
    {
	    try
	    {
		    common.context.unregisterReceiver(serviceReceiver);
	    } catch (Exception e) {
		    e.printStackTrace();
	    }
	    Common.LogMessage("webcam.java -- unregisterReceiver");
    }

    private final BroadcastReceiver serviceReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            try
            {
                Common.LogMessage("Weather() We have a hit, so we should probably update the screen.");
                String action = intent.getAction();
                if(action != null && action.equals(Common.UPDATE_INTENT))
                    reloadWebView(true);
                else if(action != null && action.equals(Common.EXIT_INTENT))
                    doPause();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
}