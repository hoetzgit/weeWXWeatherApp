package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import java.io.File;

class Weather
{
    private Common common;
    private View rootView;
    private WebView wv;
    private boolean dark_theme;
	private SwipeRefreshLayout swipeLayout;

    Weather(Common common)
    {
        this.common = common;
	    dark_theme = common.GetBoolPref("dark_theme", false);
    }

    private void checkFields(TextView tv, String txt)
    {
        if(!tv.getText().toString().equals(txt))
            tv.setText(txt);

        if(!dark_theme)
        {
	        tv.setTextColor(0xff000000);
	        tv.setBackgroundColor(0xffffffff);
	        Common.LogMessage("no dark theme");
        } else {
	        tv.setTextColor(0xffffffff);
	        tv.setBackgroundColor(0xff000000);
	        Common.LogMessage("dark theme");
        }
    }

    private View updateFields()
    {
	    int iw = 17;

        Common.LogMessage("updateFields()");
        String bits[] = common.GetStringPref("LastDownload","").split("\\|");
        if(bits.length < 65)
            return rootView;

        checkFields((TextView)rootView.findViewById(R.id.textView), bits[56]);
        checkFields((TextView)rootView.findViewById(R.id.textView2), bits[54] + " " + bits[55]);

	    final WebView current = rootView.findViewById(R.id.current);
	    current.getSettings().setUserAgentString(Common.UA);
	    current.setOnLongClickListener(new View.OnLongClickListener()
	    {
		    @Override
		    public boolean onLongClick(View v)
		    {
			    Vibrator vibrator = (Vibrator)common.context.getSystemService(Context.VIBRATOR_SERVICE);
			    if(vibrator != null)
				    vibrator.vibrate(250);
			    Common.LogMessage("current long press");
			    updateFields();
			    return true;
		    }
	    });

	    current.setWebViewClient(new WebViewClient()
	    {
		    @Override
		    public boolean shouldOverrideUrlLoading(WebView view, String url)
		    {
			    return false;
		    }
	    });
	    current.setWebChromeClient(new WebChromeClient()
	    {
		    @Override
		    public boolean onConsoleMessage(ConsoleMessage cm)
		    {
			    return true;
		    }
	    });

	    String stmp;
	    StringBuilder sb = new StringBuilder();

	    String header;
	    if(!dark_theme)
	    	header = "<html><body>";
	    else
	    	header = "<html><head><style>body{color: #fff; background-color: #000;}</style></head><body>";
	    String footer = "</body></html>";
	    sb.append(header);

	    sb.append("<table style='width:100%;border:0px;'>");

	    stmp = "<tr><td style='font-size:36pt;text-align:right;'>" + bits[0] + bits[60] + "</td>";
	    if(bits.length > 203)
		    stmp += "<td style='font-size:18pt;text-align:right;vertical-align:bottom;'>AT: " + bits[203] + bits[60] +"</td></tr></table>";
	    else
	    	stmp += "<td>&nbsp</td></tr></table>";

	    sb.append(stmp);
	    sb.append("<table style='width:100%;border:0px;'>");

	    stmp = "<tr><td><img style='width:" + iw + "px' src='windsock.png'></td><td>" + bits[25] + bits[61] + "</td>" +
			    "<td style='text-align:right;'>" + bits[37] + bits[63] + "</td><td><img style='width:" + iw + "px' src='barometer.png'></td></tr>";
	    sb.append(stmp);

	    stmp = "<tr><td><img style='width:" + iw + "px' src='compass.png'></td><td>" + bits[30] + "</td>" +
			    "<td style='text-align:right;'>" + bits[6] + bits[64] + "</td><td><img style='width:" + iw + "px' src='humidity.png'></td></tr>";
	    sb.append(stmp);

	    String rain = bits[20] + bits[62] + " since mn";
	    if(bits.length > 160 && !bits[160].equals(""))
		    rain = bits[158] + bits[62] + " since " + bits[160];

	    stmp = "<tr><td><img style='width:" + iw + "px' src='umbrella.png'></td><td>" + rain + "</td>" +
			    "<td style='text-align:right;'>" + bits[12] + bits[60] + "</td><td><img style='width:" + iw + "px' src='droplet.png'></td></tr>";
	    sb.append(stmp);

	    stmp = "<tr><td><img style='width:" + iw + "px' src='sunglasses.png'></td><td>" + bits[45] + "UVI</td>" +
			    "<td style='text-align:right;'>" + bits[43] + "W/m\u00B2</td><td><img style='width:" + iw + "px' src='sunglasses.png'></td></tr>";
	    sb.append(stmp);

	    if(bits.length > 202 && common.GetBoolPref("showIndoor", false))
	    {
		    stmp = "<tr><td><img style='width:" + iw + "px' src='home.png'></td><td>" + bits[161] + bits[60] + "</td>" +
				    "<td style='text-align:right;'>" + bits[166] + bits[64] + "</td><td><img style='width:" + iw + "px' src='home.png'></td></tr>";
		    sb.append(stmp);
	    }

	    stmp = "</table>";
	    sb.append(stmp);

	    sb.append("<table style='width:100%;border:0px;'>");

	    stmp = "<tr>" +
			    "<td><img style='width:" + iw + "px' src='sunrise.png'></td><td>" + bits[57] + "</td>" +
			    "<td><img style='width:" + iw + "px' src='sunset.png'></td><td>" + bits[58] + "</td>" +
			    "<td><img style='width:" + iw + "px' src='moonrise.png'></td><td>" + bits[47] + "</td>" +
	            "<td><img style='width:" + iw + "px' src='moonset.png'></td><td>" + bits[48] + "</td></tr>";
	    sb.append(stmp);

	    stmp = "</table>";
	    sb.append(stmp);

	    sb.append(footer);
	    final String html = sb.toString().trim();

	    current.post(new Runnable()
	    {
		    @Override
		    public void run()
		    {
			    current.loadDataWithBaseURL("file:///android_res/drawable/", html, "text/html", "utf-8", null);
		    }
	    });

        return rootView;
    }

    @SuppressLint("SetJavaScriptEnabled")
    View myWeather(LayoutInflater inflater, ViewGroup container)
    {
    	if(inflater == null || container == null)
    		return null;

        rootView = inflater.inflate(R.layout.fragment_weather, container, false);
        rootView.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                Vibrator vibrator = (Vibrator)common.context.getSystemService(Context.VIBRATOR_SERVICE);
                if(vibrator != null)
                    vibrator.vibrate(250);
                Common.LogMessage("rootview long press");
	            swipeLayout.setRefreshing(true);
	            forceRefresh();
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
			    forceRefresh();
		    }
	    });
	    swipeLayout.setRefreshing(true);

        wv = rootView.findViewById(R.id.radar);

	    wv.getSettings().setJavaScriptEnabled(true);
	    wv.getSettings().setUserAgentString(Common.UA);
        wv.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                Vibrator vibrator = (Vibrator)common.context.getSystemService(Context.VIBRATOR_SERVICE);
                if(vibrator != null)
                    vibrator.vibrate(250);
	            swipeLayout.setRefreshing(true);
                Common.LogMessage("wv long press");
	            forceRefresh();
                return true;
            }
        });

	    wv.setWebViewClient(new WebViewClient()
	    {
		    @Override
		    public boolean shouldOverrideUrlLoading(WebView view, String url)
		    {
			    return false;
		    }
	    });

	    wv.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener()
	    {
		    @Override
		    public void onScrollChanged()
		    {
			    if (wv.getScrollY() == 0)
			    {
				    swipeLayout.setEnabled(true);
			    } else {
				    swipeLayout.setEnabled(false);
			    }
		    }
	    });

	    wv.setWebChromeClient(new WebChromeClient()
	    {
		    @Override
		    public boolean onConsoleMessage(ConsoleMessage cm)
		    {
			    return true;
		    }
	    });

        loadWebView();

	    File f2 = new File(common.context.getFilesDir(), "/radar.gif");
	    long[] period = common.getPeriod();

        if(!common.GetStringPref("RADAR_URL", "").equals("") && f2.lastModified() + period[0] < System.currentTimeMillis())
            reloadWebView(false);

	    int curtime = Math.round(System.currentTimeMillis() / 1000);
	    if(!common.GetBoolPref("radarforecast", true) && common.GetIntPref("rssCheck", 0) + 7190 < curtime)
        	reloadForecast(false);

        return updateFields();
    }

    private void forceRefresh()
    {
	    swipeLayout.setRefreshing(true);
	    common.getWeather();
	    reloadWebView(true);
	    reloadForecast(true);
    }

    private void loadWebView()
    {
	    swipeLayout.setRefreshing(true);

	    Thread t = new Thread(new Runnable()
	    {
		    @Override
		    public void run()
		    {
			    final StringBuffer sb = new StringBuffer();
			    String tmp;

			    swipeLayout.setRefreshing(true);

			    if (common.GetBoolPref("radarforecast", true))
			    {
				    switch (common.GetStringPref("radtype", "image"))
				    {
					    case "image":
					    {
						    String radar = common.context.getFilesDir() + "/radar.gif";

						    File myFile = new File(radar);
						    Common.LogMessage("myFile == " + myFile.getAbsolutePath());
						    Common.LogMessage("myFile.exists() == " + myFile.exists());

						    if (radar.equals("") || !myFile.exists() || common.GetStringPref("RADAR_URL", "").equals(""))
						    {
							    sb.append("<html><body>Radar URL not set or is still downloading. You can go to settings to change.</body></html>");
							    wv.loadDataWithBaseURL(null, sb.toString(), "text/html", "utf-8", null);
							    return;
						    } else {
							    sb.append("<!DOCTYPE html>\n" +
									    "<html>\n" +
									    "  <head>\n" +
									    "    <meta charset='utf-8'>\n" +
									    "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
							    if (dark_theme)
								    sb.append("<style>body{color: #fff; background-color: #000;}</style>");

							    tmp = "  </head>\n" +
									    "  <body>\n" +
									    "\t<img style='margin:0px;padding:0px;border:0px;text-align:center;max-width:100%;width:auto;height:auto;'\n" +
									    "\tsrc='file://" + radar + "'>\n" +
									    "  </body>\n" +
									    "</html>";
							    sb.append(tmp);
							    wv.loadDataWithBaseURL(null, sb.toString(), "text/html", "utf-8", null);
							    break;
						    }
					    }
					    case "webpage":
						    wv.post(new Runnable()
						    {
							    @Override
							    public void run()
							    {
								    wv.loadUrl(common.GetStringPref("RADAR_URL", ""));
							    }
						    });
						    return;
					    default:
						    sb.append("<html>");
						    if (dark_theme)
							    sb.append("<head><style>body{color: #fff; background-color: #000;}</style></head>");
						    sb.append("<body>Radar URL not set or is still downloading. You can go to settings to change.</body></html>");
						    break;
				    }

				    wv.post(new Runnable()
				    {
					    @Override
					    public void run()
					    {
						    wv.loadDataWithBaseURL("file:///android_res/drawable/", sb.toString(), "text/html", "utf-8", null);
					    }
				    });
				    swipeLayout.setRefreshing(false);
			    } else {
				    String fctype = common.GetStringPref("fctype", "Yahoo");
				    String data = common.GetStringPref("forecastData", "");

				    if (data.equals(""))
				    {
					    sb.append("<html>");
					    if (dark_theme)
						    sb.append("<head><style>body{color: #fff; background-color: #000;}</style></head>");
					    tmp = "<body>Forecast URL not set or is still downloading. You can go to settings to change.</body></html>";
					    sb.append(tmp);
					    return;
				    }

				    switch (fctype.toLowerCase())
				    {
					    case "yahoo":
					    {
						    String[] content = common.processYahoo(data);
						    if (content == null || content.length <= 0)
							    return;
						    String logo = "<img src='purple.png' height='29px'/><br/>";
						    sb.append("<html>");
						    if (dark_theme)
							    sb.append("<head><style>body{color: #fff; background-color: #000;}</style></head>");
						    tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						    sb.append(tmp);
						    break;
					    }
					    case "weatherzone":
					    {
						    String[] content = common.processWZ(data);
						    if (content == null || content.length <= 0)
							    return;

						    String logo = "<img src='wz.png' height='29px'/><br/>";
						    sb.append("<html>");
						    if (dark_theme)
							    sb.append("<head><style>body{color: #fff; background-color: #000;}</style></head>");
						    tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						    sb.append(tmp);
						    break;
					    }
					    case "yr.no":
					    {
						    String[] content = common.processYR(data);
						    if (content == null || content.length <= 0)
							    return;

						    String logo = "<img src='yrno.png' height='29px'/><br/>";
						    sb.append("<html>");
						    if (dark_theme)
							    sb.append("<head><style>body{color: #fff; background-color: #000;}</style></head>");
						    tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						    sb.append(tmp);
						    break;
					    }
					    case "bom.gov.au":
					    {
						    String[] content = common.processBOM(data);
						    if (content == null || content.length <= 0)
							    return;

						    String logo = "<img src='bom.png' height='29px'/><br/>";
						    sb.append("<html>");
						    if (dark_theme)
							    sb.append("<head><style>body{color: #fff; background-color: #000;}</style></head>");
						    tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						    sb.append(tmp);
						    break;
					    }
					    case "wmo.int":
					    {
						    String[] content = common.processWMO(data);
						    if (content == null || content.length <= 0)
							    return;

						    String logo = "<img src='wmo.png' height='29px'/><br/>";
						    sb.append("<html>");
						    if (dark_theme)
							    sb.append("<head><style>body{color: #fff; background-color: #000;}</style></head>");
						    tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						    sb.append(tmp);
						    break;
					    }
					    case "weather.gov":
					    {
						    String[] content = common.processWGOV(data);
						    if (content == null || content.length <= 0)
							    return;

						    String logo = "<img src='wgov.png' height='29px'/><br/>";
						    sb.append("<html>");
						    if (dark_theme)
							    sb.append("<head><style>body{color: #fff; background-color: #000;}</style></head>");
						    tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						    sb.append(tmp);
						    break;
					    }
					    case "weather.gc.ca":
					    {
						    String[] content = common.processWCA(data);
						    if (content == null || content.length <= 0)
							    return;

						    String logo = "<img src='wca.png' height='29px'/><br/>";
						    sb.append("<html>");
						    if (dark_theme)
							    sb.append("<head><style>body{color: #fff; background-color: #000;}</style></head>");
						    tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						    sb.append(tmp);
						    break;
					    }
					    case "weather.gc.ca-fr":
					    {
						    String[] content = common.processWCAF(data);
						    if (content == null || content.length <= 0)
							    return;

						    String logo = "<img src='wca.png' height='29px'/><br/>";
						    sb.append("<html>");
						    if (dark_theme)
							    sb.append("<head><style>body{color: #fff; background-color: #000;}</style></head>");
						    tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						    sb.append(tmp);
						    break;
					    }
					    case "metoffice.gov.uk":
					    {
						    String[] content = common.processMET(data);
						    if (content == null || content.length <= 0)
							    return;

						    String logo = "<img src='met.png' height='29px'/><br/>";
						    sb.append("<html>");
						    if (dark_theme)
							    sb.append("<head><style>body{color: #fff; background-color: #000;}</style></head>");
						    tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						    sb.append(tmp);
						    break;
					    }
					    case "bom2":
					    {
						    String[] content = common.processBOM2(data);
						    if (content == null || content.length <= 0)
							    return;

						    String logo = "<img src='bom.png' height='29px'/><br/>";
						    sb.append("<html>");
						    if (dark_theme)
							    sb.append("<head><style>body{color: #fff; background-color: #000;}</style></head>");
						    tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						    sb.append(tmp);
						    break;
					    }
					    case "aemet.es":
					    {
						    String[] content = common.processAEMET(data);
						    if (content == null || content.length <= 0)
							    return;

						    String logo = "<img src='aemet.png' height='29px'/><br/>";
						    sb.append("<html>");
						    if (dark_theme)
							    sb.append("<head><style>body{color: #fff; background-color: #000;}</style></head>");
						    tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						    sb.append(tmp);
						    break;
					    }
					    case "dwd.de":
					    {
						    String[] content = common.processDWD(data);
						    if (content == null || content.length <= 0)
							    return;

						    String logo = "<img src='dwd.png' height='29px'/><br/>";
						    sb.append("<html>");
						    if (dark_theme)
							    sb.append("<head><style>body{color: #fff; background-color: #000;}</style></head>");
						    tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						    sb.append(tmp);
						    break;
					    }
					    case "metservice.com":
					    {
						    String[] content = common.processMetService(data);
						    if (content == null || content.length <= 0)
							    return;

						    String logo = "<img src='metservice.png' height='29px'/><br/>";
						    sb.append("<html>");
						    if (dark_theme)
							    sb.append("<head><style>body{color: #fff; background-color: #000;}</style></head>");
						    tmp = "<body style='text-align:center'>" + logo + content[0] + "</body></html>";
						    sb.append(tmp);
						    break;
					    }
				    }
			    }

			    wv.post(new Runnable()
			    {
				    @Override
				    public void run()
				    {
					    wv.loadDataWithBaseURL("file:///android_res/drawable/", sb.toString(), "text/html", "utf-8", null);
				    }
			    });
			    swipeLayout.setRefreshing(false);
		    }
	    });

	    t.start();
    }

	private void reloadForecast(boolean force)
    {
	    if(common.GetBoolPref("radarforecast", true))
		    return;

	    final String forecast_url = common.GetStringPref("FORECAST_URL", "");

	    if(forecast_url.equals(""))
	    {
		    final String html = "<html><body>Forecast URL not set. Edit inigo-settings.txt to change.</body></html>";
		    wv.post(new Runnable()
		    {
			    @Override
			    public void run()
			    {
				    wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
			    }
		    });

		    return;
	    }

	    if(!common.checkWifiOnAndConnected() && !force)
	    {
	    	Common.LogMessage("Not on wifi and not a forced refresh");
		    if(swipeLayout.isRefreshing())
			    swipeLayout.setRefreshing(false);
		    return;
	    }

	    if(!swipeLayout.isRefreshing())
		    swipeLayout.setRefreshing(true);

	    Common.LogMessage("forecast checking: " + forecast_url);

	    Thread t = new Thread(new Runnable()
	    {
		    @Override
		    public void run()
		    {
			    try
			    {
				    int curtime = Math.round(System.currentTimeMillis() / 1000);

				    if(common.GetStringPref("forecastData", "").equals("") || common.GetIntPref("rssCheck", 0) + 7190 < curtime)
				    {
					    Common.LogMessage("no forecast data or cache is more than 2 hour old");

					    String tmp = common.downloadForecast();
						if(tmp != null)
						{
							Common.LogMessage("updating rss cache");
							common.SetIntPref("rssCheck", curtime);
							common.SetStringPref("forecastData", tmp);

							handlerDone.sendEmptyMessage(0);
						}
				    }
			    } catch (Exception e) {
				    e.printStackTrace();
			    }

			    swipeLayout.setRefreshing(false);
		    }
	    });

	    t.start();
    }

    private void reloadWebView(boolean force)
    {
	    if(!common.GetBoolPref("radarforecast", true))
	    	return;

        Common.LogMessage("reload radar...");
        final String radar = common.GetStringPref("RADAR_URL", "");

        if(radar.equals(""))
        {
	        loadWebView();
            return;
        }

	    if(!common.checkWifiOnAndConnected() && !force)
	    {
		    Common.LogMessage("Not on wifi and not a forced refresh");
		    swipeLayout.setRefreshing(false);
		    return;
	    }

	    if(!swipeLayout.isRefreshing())
		    swipeLayout.setRefreshing(true);

        Thread t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Common.LogMessage("starting to download image from: " + radar);
                    File f = common.downloadRADAR(radar);
                    Common.LogMessage("done downloading " + f.getAbsolutePath() + ", prompt handler to draw to movie");
                    File f2 = new File(common.context.getFilesDir(), "/radar.gif");
                    if(f.renameTo(f2))
                    	handlerDone.sendEmptyMessage(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }

	            swipeLayout.setRefreshing(false);
            }
        });

        t.start();
    }

    @SuppressLint("HandlerLeak")
    private Handler handlerDone = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
			common.SendRefresh();
        }
    };

    void doResume()
    {
	    IntentFilter filter = new IntentFilter();
	    filter.addAction(Common.UPDATE_INTENT);
	    filter.addAction(Common.EXIT_INTENT);
	    filter.addAction(Common.REFRESH_INTENT);
	    common.context.registerReceiver(serviceReceiver, filter);
	    Common.LogMessage("weather.java -- adding a new filter");
    }

    void doPause()
    {
	    try
	    {
		    common.context.unregisterReceiver(serviceReceiver);
	    } catch (Exception e) {
			//TODO: ignore this exception...
	    }
	    Common.LogMessage("weather.java -- unregisterReceiver");
    }

    private final BroadcastReceiver serviceReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            try
            {
                String action = intent.getAction();
                if(action != null && action.equals(Common.UPDATE_INTENT))
                {
	                Common.LogMessage("Weather() We have a update_intent, so we should probably update the screen.");
	                dark_theme = common.GetBoolPref("dark_theme", false);
	                updateFields();
	                reloadWebView(false);
	                reloadForecast(false);
                } else if(action != null && action.equals(Common.REFRESH_INTENT)) {
	                Common.LogMessage("Weather() We have a refresh_intent, so we should probably update the screen.");
	                dark_theme = common.GetBoolPref("dark_theme", false);
	                updateFields();
	                loadWebView();
                } else if(action != null && action.equals(Common.EXIT_INTENT)) {
                    doPause();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
}