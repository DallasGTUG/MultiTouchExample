package com.example.multitouch;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

public class Main extends Activity {

	ImageView imageView; 
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        imageView = (ImageView)findViewById(R.id.imageview);
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.cat);
        imageView.setImage(bmp);
    }
}