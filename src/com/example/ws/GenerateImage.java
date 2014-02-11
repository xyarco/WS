package com.example.ws;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

public class GenerateImage extends AsyncTask<String, Void, Bitmap> {

	@Override
	protected Bitmap doInBackground(String... params) {
		// TODO Auto-generated method stub
		String imgUrl = params[0];
		Bitmap bitmap = null;
		try {
      	  bitmap = BitmapFactory.decodeStream((InputStream)new URL(imgUrl).getContent());
      	} catch (MalformedURLException e) {
      	  e.printStackTrace();
      	} catch (IOException e) {
      	  e.printStackTrace();
      	}
		return bitmap;
	}

}
