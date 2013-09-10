package ch.ethz.showmeyourworld;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;


public class MainActivity extends Activity implements PictureCallback
{
	private ImageView imageView;
	private Camera camera;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
        //allow network action on main thread
        ThreadPolicy tp = ThreadPolicy.LAX;
        StrictMode.setThreadPolicy(tp);
		
		
		Log.println(Log.INFO, "cam", "before"); 

		camera = Camera.open();

		Parameters params = camera.getParameters();
		List<Camera.Size> sizes = params.getSupportedPictureSizes();
		for (Camera.Size s : sizes)
		{
			Log.println(Log.INFO, "cam", "w" + s.width + " h" + s.height);
		}
		params.setPictureSize(1280, 960);
		params.setRotation(90);
		camera.setParameters(params);
		
		
		SurfaceView dummy = new SurfaceView(getApplicationContext());
        try 
        {
			camera.setPreviewDisplay(dummy.getHolder());
			camera.startPreview(); 
		} 
        catch (IOException e) 
        {

			e.printStackTrace();
		}    
        
		
		Log.println(Log.INFO, "cam", "after");
		
        Button button = (Button) findViewById(R.id.button1);
        button.setOnClickListener(new OnClickListener() 
        {
            public void onClick(View v) 
            {
                System.gc();
                camera.setPreviewCallback(null);
                camera.setOneShotPreviewCallback(null);
            	
                camera.takePicture(null, null, MainActivity.this);
            }
        });
        
        imageView = (ImageView) findViewById(R.id.imageView1);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		return true;
	}

	@Override
	public void onPictureTaken(byte[] data, Camera camera) 
	{
		Log.println(Log.INFO, "cam", "onPictureTaken " + System.currentTimeMillis());
		camera.startPreview();
		Log.println(Log.INFO, "cam", "data.length=" + data.length);
		
		Bitmap bigBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

		if (bigBitmap != null)
		{
			int width = bigBitmap.getWidth();
			int height = bigBitmap.getHeight();
			int bytes = width * height * 3;
			Log.println(Log.INFO, "cam", "onPictureTaken w" + width + " h" + height + " " + bytes + " bytes.");
			
			
			Matrix matrix = new Matrix();
			matrix.postRotate(90);
			Bitmap bitmap = Bitmap.createBitmap(bigBitmap , 0, 0, bigBitmap.getWidth(), bigBitmap.getHeight(), matrix, true);
			bigBitmap.recycle();
			bigBitmap = null;
			
			imageView.setImageBitmap(bitmap);
		}
		else
		{
			Log.println(Log.INFO, "cam", "could not create bitmap.");
		}
		
		uploadImage(data, "" + System.currentTimeMillis());
	}
	
	static final String lineEnd = "\r\n";
	static final  String twoHyphens = "--";
	static final String boundary =  "*****";
    static void uploadImage(byte[] rawImageData, String filename) 
    {
    	try
    	{
	    	URL url = new URL("http://livingscience.inn.ac/showmeyourworldupload");
	    	HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	    	connection.setDoInput(true);
	    	connection.setDoOutput(true);
	    	connection.setUseCaches(false);
	    	connection.setRequestMethod("POST");
	    	connection.setRequestProperty("Connection", "Keep-Alive");
	    	connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
	
	    	DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
	    	outputStream.writeBytes(twoHyphens + boundary + lineEnd);
	    	outputStream.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + filename +"\"" + lineEnd);
	    	outputStream.writeBytes(lineEnd);
	    	outputStream.write(rawImageData, 0, rawImageData.length);
	    	outputStream.writeBytes(lineEnd);
	    	outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
	    	outputStream.flush();
	    	outputStream.close();
	
	    	// Responses from the server (code and message)
	    	Log.println(Log.INFO, "cam", "image upload response: " + connection.getResponseCode() + " " + connection.getResponseMessage());
    	}
    	catch (Exception ex)
    	{
    		ex.printStackTrace();
    	}
    }
}
