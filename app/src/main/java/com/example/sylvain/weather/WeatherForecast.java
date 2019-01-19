package com.example.sylvain.weather;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class WeatherForecast extends Activity {

    ProgressBar weatherProgressBar;
    ImageView currentWeatherImage;
    TextView currentTemperatureText;
    TextView minTemperatureText;
    TextView maxTemperatureText;
    TextView windSpeedText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_forecast);

        weatherProgressBar = (ProgressBar) findViewById(R.id.weatherProgressBar);
        currentWeatherImage = (ImageView) findViewById(R.id.currentWeather);
        currentTemperatureText = (TextView) findViewById(R.id.currentTemperature);
        minTemperatureText = (TextView) findViewById(R.id.minTemperature);
        maxTemperatureText = (TextView) findViewById(R.id.maxTemperature);
        windSpeedText = (TextView) findViewById(R.id.windSpeed);

        ForecastQuery query = new ForecastQuery();
        query.execute();
    }

    public class ForecastQuery extends AsyncTask<String, Integer, String> {
        private String windSpeed, minTemp, maxTemp, currentTemp, iconName;
        private Bitmap currentWeather;
        private String urlString = "http://api.openweathermap.org/data/2.5/weather?q=ottawa,ca&APPID=d99666875e0e51521f0040a3d97d0f6a&mode=xml&units=metric";
        HttpUtils httpUtils = new HttpUtils();
        private String imageUrl;

        @Override
        protected String doInBackground(String... args) {
            try {
                //connect to Server:
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                InputStream response = urlConnection.getInputStream();

                //Read the XML:
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(false);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(response, "UTF-8");

                while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                    switch (xpp.getEventType()) {
                        case XmlPullParser.START_TAG:
                            String name = xpp.getName();
                            if (name.equals("temperature")) {
                                currentTemp = xpp.getAttributeValue(null, "value");
                                minTemp = xpp.getAttributeValue(null, "min");
                                maxTemp = xpp.getAttributeValue(null, "max");
                                publishProgress(25);
                            } else if (name.equals("speed")) {
                                windSpeed = xpp.getAttributeValue(null, "value");
                                publishProgress(50);
                            } else if (name.equals("weather")) {
                                iconName = xpp.getAttributeValue(null, "icon");
                                publishProgress(75);
                            }

                            Log.i("read XML tag:", name);
                            break;

                        case XmlPullParser.TEXT:
                            break;
                    }
                    xpp.next();//look at next XML tag
                }

                imageUrl = "http://openweathermap.org/img/w/" + iconName + ".png";
                if(fileExistance(iconName + ".png")) {
                    FileInputStream fis = null;
                    try {    fis = openFileInput(iconName + ".png");   }
                    catch (FileNotFoundException e) {    e.printStackTrace();  }
                    currentWeather = BitmapFactory.decodeStream(fis);
                    Log.i("Looking For Filename:", iconName + ".png");
                    Log.i("Image Found: ", "locally");
                } else {
                    currentWeather  = httpUtils.getImage(imageUrl);
                    FileOutputStream outputStream = openFileOutput( iconName + ".png", Context.MODE_PRIVATE);
                    currentWeather.compress(Bitmap.CompressFormat.PNG, 80, outputStream);
                    outputStream.flush();
                    outputStream.close();
                    Log.i("Looking For Filename:", iconName + ".png");
                    Log.i("Image Found: ", "download");
                }
                publishProgress(100);

            } catch (Exception e) {
                Log.i("Exception", e.getMessage());
            }

            return "";
        }

        public boolean fileExistance(String fname){
            File file = getBaseContext().getFileStreamPath(fname);
            return file.exists();
        }

        @Override
        public void onProgressUpdate(Integer ...value) //update your GUI
        {
            weatherProgressBar.setVisibility(View.VISIBLE);

            for (int i = 0; i < value.length; i++)
                weatherProgressBar.setProgress(value[i]);
        }

        @Override
        public void onPostExecute(String result)  // doInBackground has finished
        {
            minTemperatureText.setText("Min: " + minTemp + " °C");
            maxTemperatureText.setText("Max: " + maxTemp + " °C");
            currentTemperatureText.setText("Current: " + currentTemp + " °C");
            windSpeedText.setText("Wind: " + windSpeed + " km/h");
            currentWeatherImage.setImageBitmap(currentWeather);
            weatherProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * @author Terry E-mail: yaoxinghuo at 126 dot com
     * @version create: 2010-10-21 ??01:40:03
     */
    class HttpUtils {
        public Bitmap getImage(URL url) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    return BitmapFactory.decodeStream(connection.getInputStream());
                } else
                    return null;
            } catch (Exception e) {
                return null;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        public Bitmap getImage(String urlString) {
            try {
                URL url = new URL(urlString);
                return getImage(url);
            } catch (MalformedURLException e) {
                return null;
            }
        }
    }
}