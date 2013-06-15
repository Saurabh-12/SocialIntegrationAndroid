package com.sks.facebook;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Util;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.method.BaseKeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class FacebookActivity extends Activity {
	//Global Variable;
	private static int RESULT_LOAD_IMAGE = 1;
	String picturePath ;
	Boolean selectedimage = false;
	EditText puttext;
	
	//Facebook Upload Variable started 
	private Button Upload_to_Facebook; 
	private static final String APP_ID = "344638742254679";
	private static final String TOKEN = "access_token";
	private static final String EXPIRES = "expires_in";
	private static final String KEY = "facebook-credentials";
	private Facebook facebook;

	//Facebook Upload Variable Finished

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_facebook);
		
		puttext = (EditText)findViewById(R.id.editText1);
		findViewById(R.id.imageView1).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				selectedimage = true;
				Intent i = new Intent(Intent.ACTION_PICK,
						android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				startActivityForResult(i, RESULT_LOAD_IMAGE);
				
			}
		});
		facebook = new Facebook(APP_ID);
		restoreCredentials(facebook);
		findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(selectedimage){
					if(!puttext.getText().toString().equals("")){
						
						facebook.authorize(FacebookActivity.this, 
								new String[]{ "user_photos,publish_checkins,publish_actions,publish_stream"},
								new DialogListener() {
						@Override
						public void onComplete(Bundle values) {
							postImage();
							Toast.makeText(getApplicationContext(), "Image Posted on Facebook.", Toast.LENGTH_SHORT).show();
						}
						@Override
						public void onFacebookError(FacebookError error) {
						}

						@Override
						public void onError(DialogError e) {
						}

						@Override
						public void onCancel() {
						}

					});	
					}else{
						Toast.makeText(getBaseContext(), "Please select text", Toast.LENGTH_SHORT).show();	
					}	
					}else{
					Toast.makeText(getBaseContext(), "Please select an image", Toast.LENGTH_SHORT).show();	
					}
			}
		});
		
	}
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	
		if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
			Uri selectedImage = data.getData();
			String[] filePathColumn = { MediaStore.Images.Media.DATA };

			Cursor cursor = getContentResolver().query(selectedImage,
					filePathColumn, null, null, null);
			cursor.moveToFirst();

			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
		     picturePath = cursor.getString(columnIndex);
			cursor.close();
			
			ImageView imageView = (ImageView) findViewById(R.id.imageView1);
			imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
		
		}
    	
    	}
	public void postImage(){
		byte[] data = null;               
		//String outPath =  file.toString();
		String outPath = picturePath;
		Log.e("postWall", "postWall "+outPath);
		Bitmap bi = BitmapFactory.decodeFile(outPath);
		//Bitmap bi = BitmapFactory.decodeFile("/sdcard/img.jpg");
		//Bitmap bi = BitmapFactory.decodeResource(getResources(), R.drawable.icon);             
		ByteArrayOutputStream baos = new ByteArrayOutputStream();              
		bi.compress(Bitmap.CompressFormat.JPEG, 100, baos);              
		data = baos.toByteArray();
		
		Bundle params = new Bundle();              
		params.putString(Facebook.TOKEN, facebook.getAccessToken());
		params.putString("method", "photos.upload");              
		params.putByteArray("picture", data);
//		params.putString("message", puttext.getText().toString());
//		params.putString("description", "topic share");
		params.putString("caption",  puttext.getText().toString());
		AsyncFacebookRunner mAsyncRunner = new AsyncFacebookRunner(facebook);              
		mAsyncRunner.request(null, params, "POST", new SampleUploadListener(), null);   

	}
	public class SampleUploadListener extends BaseKeyListener implements RequestListener {

		public void onComplete(final String response, final Object state) {
			try {
				// process the response here: (executed in background thread)
				Log.d("Facebook-Example", "Response: " + response.toString());
				JSONObject json = Util.parseJson(response);
				final String src = json.getString("src");
				Log.d("Facebook-Example", "URL: " + src.toString().trim());
				// then post the processed result back to the UI thread
				// if we do not do this, an runtime exception will be generated
				// e.g. "CalledFromWrongThreadException: Only the original
				// thread that created a view hierarchy can touch its views."

			} catch (JSONException e) {
				Log.w("Facebook-Example", "JSON Error in response");
			} catch (FacebookError e) {
				Log.w("Facebook-Example", "Facebook Error: " + e.getMessage());
			}
		}

		public void onFacebookError(FacebookError e, Object state) {
			// TODO Auto-generated method stub

		}

		public Bitmap getInputType(Bitmap img) {
			// TODO Auto-generated method stub
			return img;
		}

		public int getInputType() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void onIOException(IOException e, Object state) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onFileNotFoundException(FileNotFoundException e,
				Object state) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onMalformedURLException(MalformedURLException e,
				Object state) {
			// TODO Auto-generated method stub

		}

	}
	public boolean saveCredentials(Facebook facebook) {
		Editor editor = getApplicationContext().getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
		editor.putString(TOKEN, facebook.getAccessToken());
		editor.putLong(EXPIRES, facebook.getAccessExpires());
		return editor.commit();
	}
	public boolean restoreCredentials(Facebook facebook) {
		SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(KEY, Context.MODE_PRIVATE);
		facebook.setAccessToken(sharedPreferences.getString(TOKEN, null));
		facebook.setAccessExpires(sharedPreferences.getLong(EXPIRES, 0));
		return facebook.isSessionValid();
	}
}
