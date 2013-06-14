package com.sks.twitter;
/**
 * @author saurabh
 * Sharma 
 * */


import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class TwitterHomeActivity extends Activity {
	
	// Constants
// Register your here app https://dev.twitter.com/apps/new and get your consumer key and secret
	static String TWITTER_CONSUMER_KEY = "marlVCZaLYAG52rVvholRw"; // place your cosumer key here
	static String TWITTER_CONSUMER_SECRET = "5uZZFboHSK9psBPsqJUDSb1GuC36Fy83cYornOPu9A"; // place your consumer secret here
// Shared Preference Constants
	static String PREFERENCE_NAME = "twitter_oauth";
	static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
	static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
	static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLogedIn";
	static final String TWITTER_CALLBACK_URL = "oauth://t4jsample";
// Twitter oauth urls
	static final String URL_TWITTER_AUTH = "auth_url";
	static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
	static final String URL_TWITTER_OAUTH_TOKEN = "oauth_token";
	
	// Progress dialog bar 
	ProgressDialog pDialog;
	// Twitter Variables
	private static Twitter twitter;
	private static RequestToken requestToken;
	// Shared Preferences 
	private static SharedPreferences mSharedPreferences;
	// Internet Connection detector
	private ConnectionDetector cd;
	// Alert Dialog Manager
	AlertDialogManager alert = new AlertDialogManager();
	EditText sts;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_twitter_home);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		
		cd = new ConnectionDetector(getApplicationContext());
		if (!cd.isConnectingToInternet()) {
			// Internet Connection is not present
			alert.showAlertDialog(TwitterHomeActivity.this, "Internet Connection Error",
					"Please connect to working Internet connection", false);
			// stop executing code by return
			return;
		}
		// Check if twitter keys are set
		if(TWITTER_CONSUMER_KEY.trim().length() == 0 || TWITTER_CONSUMER_SECRET.trim().length() == 0){
			// Internet Connection is not present
			alert.showAlertDialog(TwitterHomeActivity.this, "Twitter oAuth tokens", "Please set your twitter oauth tokens first!", false);
			// stop executing code by return
			return;
		}
		// Shared Preferences
		mSharedPreferences = getApplicationContext().getSharedPreferences("MyPref", 0);
		
		//This if conditions is tested once is redirected from twitter page. 
		// I am Parsing the uri to get oAuth Verifier
			if (!isTwitterLoggedInAlready()) {
				Uri uri = getIntent().getData();
				if (uri != null && uri.toString().startsWith(TWITTER_CALLBACK_URL)) {
					// oAuth verifier
					String verifier = uri.getQueryParameter(URL_TWITTER_OAUTH_VERIFIER);
					try {
						// Get the access token
						AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);
						// Shared Preferences
						Editor e = mSharedPreferences.edit();
						
						// After getting access token, access token secret
						// store them in application preferences
						e.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
						e.putString(PREF_KEY_OAUTH_SECRET,accessToken.getTokenSecret());
						
						// Store login status - true
						e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
						e.commit(); // save changes

						Log.e("Twitter OAuth Token", "> " + accessToken.getToken());

						// Hide login button
						findViewById(R.id.button1).setVisibility(View.GONE);

						// Show Update Twitter
						findViewById(R.id.editText1).setVisibility(View.VISIBLE);
						findViewById(R.id.button2).setVisibility(View.VISIBLE);
						
						// Getting user details from twitter
						// For now i am getting his name only
						long userID = accessToken.getUserId();
						User user = twitter.showUser(userID);
						String username = user.getName();
						Log.e("UserID: ", "userID: "+userID+""+username);
						
						// Displaying in xml ui
						Log.v("Welcome:","Thanks:"+Html.fromHtml("<b>Welcome " + username + "</b>"));
					} catch (Exception e) {
						// Check log for login errors
						Log.e("Twitter Login Error", "> " + e.getMessage());
					}
				}
			}
		
		findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				loginToTwitter();
				//finish();
			}
		});
		findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				sts = (EditText)findViewById(R.id.editText1);
				String status = sts.getText().toString();

				// Check for blank text
				if (status.trim().length() > 0) {
					// update status
					new updateTwitterStatus().execute(status);
				} else {
					// EditText is empty
					Toast.makeText(getApplicationContext(),
							"Please enter status message", Toast.LENGTH_SHORT).show();
				}
				
			}
		});
		
	}

	private void loginToTwitter() {
		// Check if already logged in
		if (!isTwitterLoggedInAlready()) {
			ConfigurationBuilder builder = new ConfigurationBuilder();
			builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
			builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
			Configuration configuration = builder.build();
			
			TwitterFactory factory = new TwitterFactory(configuration);
			twitter = factory.getInstance();

			try {
				requestToken = twitter.getOAuthRequestToken(TWITTER_CALLBACK_URL);
				this.startActivity(new Intent(Intent.ACTION_VIEW, 
						Uri.parse(requestToken.getAuthenticationURL())));
			} catch (TwitterException e) {
				e.printStackTrace();
			}
		} else {
			// user already logged into twitter
			Toast.makeText(getApplicationContext(),
					"Already Logged into twitter", Toast.LENGTH_LONG).show();
		}
	}

	// Checking User is already logged or not by using Shared preferences 
	private boolean isTwitterLoggedInAlready() {
		// return twitter login status from Shared Preferences
		return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
	}
	
	//Function to update status
	class updateTwitterStatus extends AsyncTask<String, String, String> {
		
		//Before starting background thread I am showing Progress Dialog 
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(TwitterHomeActivity.this);
			pDialog.setMessage("Updating to twitter...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}

		/**
		 * getting Places JSON
		 * */
		protected String doInBackground(String... args) {
			Log.d("Tweet Text", "> " + args[0]);
			String status = args[0];
			try {
				ConfigurationBuilder builder = new ConfigurationBuilder();
				builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
				builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
				// Access Token 
				String access_token = mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, "");
				// Access Token Secret
				String access_token_secret = mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET, "");
				
				AccessToken accessToken = new AccessToken(access_token, access_token_secret);
				Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);
				
				// Update status
				twitter4j.Status response = twitter.updateStatus(status);
				
				Log.d("Status", "> " + response.getText());
			} catch (TwitterException e) {
				// Error in updating status
				Log.d("Twitter Update Error", e.getMessage());
			}
			return null;
		}

		/**
		 * After completing background task Dismiss the progress dialog and show
		 * the data in UI Always use runOnUiThread(new Runnable()) to update UI
		 * from background thread, otherwise you will get error
		 * **/
		protected void onPostExecute(String file_url) {
			// dismiss the dialog after getting all products
			pDialog.dismiss();
			// updating UI from Background Thread
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getApplicationContext(),
							"Status tweeted successfully", Toast.LENGTH_SHORT)
							.show();
					// Clearing EditText field
					sts.setText("");
				}
			});
		}

	}
}
