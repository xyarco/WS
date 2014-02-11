package com.example.ws;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;


import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;


public class MainActivity extends Activity {
	EditText editLocation;
    TextView viewAlert;
    TextView viewCity;
    TextView viewRegion;
    ImageView viewImage;
    TextView viewDescription;
    TextView viewTemp;
    TextView viewForecast;
    TableLayout table;
    TextView postCurrent;
    TextView postForecast;
    RadioButton radioUnitF;
    JSONObject jsonRoot;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		  super.onActivityResult(requestCode, resultCode, data);
		  Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
		}
	
	public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearContent() {
        viewAlert.setText("");
        //viewHead.setText("");
        viewCity.setText("");
        viewRegion.setText("");
        //viewImage.getBackground().setAlpha(0);
        viewDescription.setText("");
        viewTemp.setText("");
        viewForecast.setText("");
        table.removeAllViews();
        postCurrent.setText("");
        postForecast.setText("");
    }
    
    public boolean validateLocation(String strLocation) {
        String strZipPattern = "^\\d{5}$";
        String strCityPattern = "^[a-zA-Z]+(\\s*,\\s*[a-zA-z]+){1,2}$";
        return strLocation.matches(strZipPattern) || strLocation.matches(strCityPattern);
    }

    private void initComponents () {
        editLocation = (EditText) findViewById(R.id.edit_location);
        viewAlert = (TextView) findViewById(R.id.view_alert);
        //viewHead = (TextView) findViewById(R.id.view_head);
        viewCity = (TextView) findViewById(R.id.city);
        viewRegion = (TextView) findViewById(R.id.region);
        viewImage = (ImageView) findViewById(R.id.w_image);
        viewDescription = (TextView) findViewById(R.id.description);
        viewTemp = (TextView) findViewById(R.id.temp);
        //viewForecast = (TextView) findViewById(R.id.forecast);
        viewForecast = (TextView) findViewById(R.id.view_forecast);
        table = (TableLayout) findViewById(R.id.tablelayout); 
        postCurrent = (TextView) findViewById(R.id.post_current);
        postForecast = (TextView) findViewById(R.id.post_forecast);
        radioUnitF = (RadioButton) findViewById(R.id.radio_unit_f);
       
    }
    
    public void sendInfo(View view) {
        initComponents();
        clearContent();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        editLocation.clearFocus();
        new MainProcess().handleEvent();
    }
    
    private class MainProcess {
        String strZipCode;
        String strCity;
        String strRegion;
        String strTempUnit;
        public boolean validateLocation(String strLocation) {
            String strZipPattern = "^\\d{5}$";
            String strCityPattern = "^[a-zA-Z\\s]+(\\s*,\\s*[a-zA-Z\\s]+){1,2}$";
            return strLocation.matches(strZipPattern) || strLocation.matches(strCityPattern);
        }

        public void handleEvent () {
            String strLocation = editLocation.getText().toString().trim();
            if (strLocation.equals("")) {
                String strAlert = "<h2>Invalid input: must be non-empty</h2><br /><h2>Example: 90001 or Los Angeles, CA</h2>";
                viewAlert.setText(Html.fromHtml(strAlert));
                return;
            }

            if (!validateLocation(strLocation)) {
                String strAlert = null;
                if (strLocation.matches("^\\d+.*$")) {
                    strAlert = "<h2>Invalid zipcode: must be five digits</h2><br /><h2>Example: 90001</h2>";
                } else {
                    strAlert = "<h2>Invalid location: must include state or country seperated by comma</h2><br /><h2>Example: Los Angeles, CA</h2>";
                }
                viewAlert.setText(Html.fromHtml(strAlert));
                return;
            }

            strTempUnit = "f";

            if (!radioUnitF.isChecked()) {
                strTempUnit = "c";
            }

            String strPrefix = "http://cs-server.usc.edu:33262/examples/servlet/WeatherSearch?";
            URL urlJSON = null;

            if (strLocation.matches("^\\d+$")) {
                strZipCode = strLocation;
                try {
                    urlJSON = new URL(strPrefix + "type=zipcode&no_city=" + strZipCode + "&unit=" + strTempUnit);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

            } else {
                String[] strTokens = strLocation.split(",");
                strCity = strTokens[0].trim();
                strRegion = strTokens[1].trim();
                strCity = strCity.replaceAll("\\s+", "_");
                strRegion = strRegion.replaceAll("\\s+", "_");
                try {
                    urlJSON = new URL(strPrefix + "type=name&name_city=" + strCity + "&state=" + strRegion + "&unit=" + strTempUnit);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }

            String strJSON = null;
            try {
                strJSON = new DownloadJSONTask().execute(urlJSON).get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (strJSON == null || strJSON.equals("")) {
                viewAlert.setText(Html.fromHtml("<h2>No location found</h2"));
                return;
            }

            JSONObject jsonRoot = null;
            try {
                jsonRoot = new JSONObject(strJSON);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                //generateHeadContent(jsonRoot);
                generateContent(jsonRoot);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
        private void generateContent(JSONObject jsonRoot) throws JSONException{
        	
            try {
				generateHeadContent(jsonRoot);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            generateForecastContent(jsonRoot);
            generatePost(jsonRoot);
        }

        private void generatePost(final JSONObject jsonRoot) {
			// TODO Auto-generated method stub
			postCurrent.setText("Post Current Weather");
			postForecast.setText("Post Weather Forecast");
			postCurrent.setFocusable(true);
			postForecast.setFocusable(true);
			postCurrent.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					new AlertDialog.Builder(MainActivity.this)  
					.setTitle("Post to Facebook")  
					.setPositiveButton("Post Current Weather", new DialogInterface.OnClickListener() {      
					    @Override  
					    public void onClick(DialogInterface dialog, int which) {  
					    	Session.openActiveSession(MainActivity.this, true, new Session.StatusCallback() {

								@Override
								public void call(Session session,
										SessionState state, Exception exception) {
									// TODO Auto-generated method stub
									if (session.isOpened()) {
										Request.newMeRequest(session, new Request.GraphUserCallback() {

											@Override
											public void onCompleted(
													GraphUser user,
													Response response) {
												// TODO Auto-generated method stub
												try {
													publishfeed("current", jsonRoot);
												} catch (JSONException e) {
													// TODO Auto-generated catch block
													e.printStackTrace();
												}
											}
										}).executeAsync();
									}
								}
					    		
					    	});
					    }  
					})  
					.setNegativeButton("Cancel",null )  
					.show();  
				}
			});
			
            postForecast.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					new AlertDialog.Builder(MainActivity.this)  
					.setTitle("Post to Facebook")  
					.setPositiveButton("Post Weather Forecast", new DialogInterface.OnClickListener() {      
					    @Override  
					    public void onClick(DialogInterface dialog, int which) {  
					    	Session.openActiveSession(MainActivity.this, true, new Session.StatusCallback() {

								@Override
								public void call(Session session,
										SessionState state, Exception exception) {
									// TODO Auto-generated method stub
									if (session.isOpened()) {
										Request.newMeRequest(session, new Request.GraphUserCallback() {

											@Override
											public void onCompleted(
													GraphUser user,
													Response response) {
												// TODO Auto-generated method stub
												try {
													publishfeed("forecast", jsonRoot);
												} catch (JSONException e) {
													// TODO Auto-generated catch block
													e.printStackTrace();
												}
											}
										}).executeAsync();
									}
								}
					    		
					    	});
					    }  
					})  
					.setNegativeButton("Cancel",null )  
					.show();  
				}
			});
			
		}
        private void publishfeed(String type,JSONObject jsonRoot) throws JSONException {
			// TODO Auto-generated method stub
			JSONObject jLocation = jsonRoot.getJSONObject("weather").getJSONObject("location");
			JSONObject jCondition = jsonRoot.getJSONObject("weather").getJSONObject("condition");
			String city = jLocation.getString("city");
			String region = jLocation.getString("region");
			String country = jLocation.getString("country");
			String text = jCondition.getString("text");
			String temp = jCondition.getString("temp");
			String unit = jsonRoot.getJSONObject("weather").getJSONObject("units").getString("temperature");
			String feed = jsonRoot.getJSONObject("weather").getString("feed");
			String detail = jsonRoot.getJSONObject("weather").getString("link");
			String imgUrl = "";
			String name ="";
			String caption = "";
			String description = "";
			
			if(!region.equals("")){
				name = city + "," + region + "," + country;
			} else{
				name = city + "," + country;
			}
			
			if(type.equals("current")){
				caption = "The current condition for " + city + "is " + text;
				description = "Temperature is " + temp + (char) 0x00B0 + unit;
				imgUrl = jsonRoot.getJSONObject("weather").getString("img");
			} else {
				caption = "Weather forecast for " + city;
				imgUrl = "http://www-scf.usc.edu/~csci571/2013Fall/hw8/weather.jpg";
				JSONArray jsonForecasts = jsonRoot.getJSONObject("weather").getJSONArray("forecast");
				for(int i = 0; i < jsonForecasts.length(); i++){
					JSONObject obj = jsonForecasts.getJSONObject(i);
					description += obj.getString("day") + ": " + obj.getString("text") + ", " 
					               + obj.getString("high") + "/" + obj.getString("low") + (char) 0x00B0 + unit + " ";
				}
			}
			
			JSONObject property = new JSONObject(); 
			property.put("text", "here"); 
			property.put("href", detail); 
			JSONObject properties = new JSONObject(); 
			properties.put("Look at details", property); 
			Bundle params = new Bundle();
			params.putString("name", name);
		    params.putString("caption", caption);
		    params.putString("description", description);
		    params.putString("link", feed);
		    params.putString("picture", imgUrl);
		    params.putString("properties", properties.toString());
		    WebDialog feedDialog = (
			        new WebDialog.FeedDialogBuilder(MainActivity.this,
			            Session.getActiveSession(),
			            params))
			        .setOnCompleteListener(new OnCompleteListener() {

			            @Override
			            public void onComplete(Bundle values,
			                FacebookException error) {
			                if (error == null) {
			                    // When the story is posted, echo the success
			                    // and the post Id.
			                    final String postId = values.getString("post_id");
			                    if (postId != null) {
			                        Toast.makeText(MainActivity.this,
			                            "Posted story, id: "+postId,
			                            Toast.LENGTH_SHORT).show();
			                    } else {
			                        // User clicked the Cancel button
			                        Toast.makeText(MainActivity.this.getApplicationContext(), 
			                            "Publish cancelled", 
			                            Toast.LENGTH_SHORT).show();
			                    }
			                } else if (error instanceof FacebookOperationCanceledException) {
			                    // User clicked the "x" button
			                    Toast.makeText(MainActivity.this.getApplicationContext(), 
			                        "Publish cancelled", 
			                        Toast.LENGTH_SHORT).show();
			                } else {
			                    // Generic, ex: network error
			                    Toast.makeText(MainActivity.this.getApplicationContext(), 
			                        "Error posting story", 
			                        Toast.LENGTH_SHORT).show();
			                }
			            }

			        })
			        .build();
			    feedDialog.show();

		}

		private void generateHeadContent(JSONObject jsonRoot) throws JSONException, InterruptedException, ExecutionException {
            JSONObject jsonWeather = jsonRoot.getJSONObject("weather");
            JSONObject jsonLocation = jsonWeather.getJSONObject("location");
            //viewAlert.setText(jsonWeather.getJSONObject("location").getString("country"));
            String strCityName = jsonLocation.getString("city");
            String strRegionName = jsonLocation.getString("region");
            String strCountryName = jsonLocation.getString("country");
            String strImgUrl = jsonWeather.getString("img");
            String strConditionText = jsonWeather.getJSONObject("condition").getString("text");
            String strConditionTemp = jsonWeather.getJSONObject("condition").getString("temp");
            String strUnitsTemp = jsonWeather.getJSONObject("units").getString("temperature");
            
            viewCity.setText(strCityName);
            
            if (!strRegionName.equals("")) {
                String Region = strRegionName + "," + strCountryName;
                viewRegion.setText(Region);
            }else{
            	viewRegion.setText(strCountryName);
            }
            Bitmap bitmap = new GenerateImage().execute(strImgUrl).get();
            viewImage.setImageBitmap(bitmap);
            viewDescription.setText(strConditionText);
            String temp = strConditionTemp + (char) 0x00B0 + strUnitsTemp;
            viewTemp.setText(temp);
        }

        private void generateForecastContent(JSONObject jsonRoot) throws  JSONException {
        	viewForecast.setText("Forecast");
        	//TableLayout table = (TableLayout) findViewById(R.id.tablelayout);  
            table.setStretchAllColumns(true);
            
            TableRow tablehead = new TableRow(MainActivity.this);
            tablehead.setBackgroundColor(Color.rgb(112, 128, 144));
            tablehead.setGravity(Gravity.CENTER_HORIZONTAL);
        	
        	TextView viewDay = new TextView(MainActivity.this);
        	viewDay.setGravity(Gravity.CENTER);
        	viewDay.setText("Day");
        	viewDay.setBackgroundColor(Color.rgb(192, 192, 192));
        	
        	TextView viewWeather = new TextView(MainActivity.this);
        	viewWeather.setGravity(Gravity.CENTER);
        	viewWeather.setText("Weather");
        	viewWeather.setBackgroundColor(Color.rgb(192, 192, 192));
        	
        	TextView viewHigh = new TextView(MainActivity.this);
        	viewHigh.setGravity(Gravity.CENTER);
        	viewHigh.setText("High");
        	viewHigh.setBackgroundColor(Color.rgb(192, 192, 192));
        	
        	TextView viewLow = new TextView(MainActivity.this);
        	viewLow.setGravity(Gravity.CENTER);
        	viewLow.setText("Low");
        	viewLow.setBackgroundColor(Color.rgb(192, 192, 192));
        	
        	tablehead.addView(viewDay);
        	tablehead.addView(viewWeather);
        	tablehead.addView(viewHigh);
        	tablehead.addView(viewLow);
        	
        	table.addView(tablehead);
        	
            JSONArray jsonForecasts = jsonRoot.getJSONObject("weather").getJSONArray("forecast");
            String strUnitTemp = jsonRoot.getJSONObject("weather").getJSONObject("units").getString("temperature");
            
            int bgcolor;
            for (int i = 0; i < jsonForecasts.length(); ++i) {
                JSONObject obj = jsonForecasts.getJSONObject(i);
                if(i%2 == 0){
                	bgcolor = Color.rgb(255,255,255);
                }else{
                	bgcolor = Color.rgb(176,224,230);
                }
                
                TableRow tablerow = new TableRow(MainActivity.this);
                tablerow.setGravity(Gravity.CENTER);
                tablerow.setBackgroundColor(Color.rgb(112, 128, 144));
                
                TextView text_day = new TextView(MainActivity.this);
                text_day.setPadding(3, 3, 3, 3);
                text_day.setGravity(Gravity.CENTER);
                text_day.setText(obj.getString("day"));
                text_day.setBackgroundColor(bgcolor);
                
                TextView text_weather = new TextView(MainActivity.this);
                text_weather.setPadding(3, 3, 3, 3);
                text_weather.setGravity(Gravity.CENTER);
                text_weather.setText(obj.getString("text"));
                text_weather.setBackgroundColor(bgcolor);
                
                TextView text_high = new TextView(MainActivity.this);
                text_high.setPadding(3, 3, 3, 3);
                text_high.setGravity(Gravity.CENTER);
                text_high.setText(obj.getString("high") + (char) 0x00B0 + strUnitTemp);
                text_high.setBackgroundColor(bgcolor);
                text_high.setTextColor(Color.rgb(255,165,0));
                
                TextView text_low = new TextView(MainActivity.this);
                text_low.setPadding(3, 3, 3, 3);
                text_low.setGravity(Gravity.CENTER);
                text_low.setText(obj.getString("low") + (char) 0x00B0 + strUnitTemp);
                text_low.setBackgroundColor(bgcolor);
                text_low.setTextColor(Color.rgb(65,105,225));
                
                tablerow.addView(text_day);
                tablerow.addView(text_weather);
                tablerow.addView(text_high);
                tablerow.addView(text_low);
                
                table.addView(tablerow);
            }
        }
    }

}