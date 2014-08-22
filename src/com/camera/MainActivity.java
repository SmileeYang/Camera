package com.camera;

import android.app.Activity;
import android.os.Bundle;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends Activity {

	private Button mButtonTakePhoto, mButtonChoose;
	private ImageView mImageView;
	private File mPhotoFile;
	private String mPhotoPath;
	private Uri mPhotoOnSDCardUri, photoUriInMedia;
	public final static int SELECT_PIC_TO_CUT = 111;
	public final static int CAMERA_RESULT_CUT = 222;
	public final static int CAMERA_RESULT_CUT_OVER = 333;
	private final static String TAG = "MainActivity";

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mButtonTakePhoto = (Button) findViewById(R.id.button);
		mButtonChoose = (Button) findViewById(R.id.button1);
		mButtonTakePhoto.setOnClickListener(new ButtonOnClickListener());
		mButtonChoose.setOnClickListener(new ButtonOnClickListener());
		mImageView = (ImageView) findViewById(R.id.imageView);
	}

	private class ButtonOnClickListener implements View.OnClickListener {
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.button:
				try {
					Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
					mPhotoPath = "mnt/sdcard/DCIM/Camera/" + getPhotoFileName();
					Log.d(TAG, "mPhotoPath=" + mPhotoPath);
					mPhotoFile = new File(mPhotoPath);
					if (!mPhotoFile.exists()) {
						mPhotoFile.createNewFile();
					}
					mPhotoOnSDCardUri = Uri.fromFile(mPhotoFile);
					intent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoOnSDCardUri);
					startActivityForResult(intent, CAMERA_RESULT_CUT);
				} catch (Exception e) {
				}
				break;
			case R.id.button1:
				Intent intent = new Intent(Intent.ACTION_PICK);
				intent.setType("image/*");
				Intent destIntent = Intent.createChooser(intent, "Choose picture");
				startActivityForResult(destIntent, SELECT_PIC_TO_CUT);
				break;
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.d(TAG, ">>>>>resultCode="+resultCode+">>>>>requestCode="+requestCode);
		if (requestCode == CAMERA_RESULT_CUT && resultCode == RESULT_OK) {
			Log.d(TAG, "CAMERA_RESULT_CUT>>>>>resultCode="+resultCode+">>>>>requestCode="+requestCode);
			Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, mPhotoOnSDCardUri);
			sendBroadcast(intent);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Uri systemImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
			ContentResolver contentResolver = getContentResolver();
			Cursor cursor = contentResolver.query(
					systemImageUri,
					null,
					MediaStore.Images.Media.DISPLAY_NAME 
					+ "='" + mPhotoFile.getName() + "'"
					, null
					, null);
			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToLast();
				long id = cursor.getLong(0);
				photoUriInMedia = ContentUris.withAppendedId(systemImageUri, id);
				Log.d(TAG, "CAMERA_RESULT_CUT: photoUriInMedia="+photoUriInMedia);
			}
			cursor.close();
			cutPhoto(data);
		} else if (requestCode == CAMERA_RESULT_CUT_OVER && resultCode == RESULT_OK) {
			Log.d(TAG, "CAMERA_RESULT_CUT_OVER>>>>>resultCode="+resultCode+">>>>>requestCode="+requestCode);
			// if choose cancel when cutting picture, will return data = null
			if (data != null) {
				Bitmap bitmap = (Bitmap) data.getExtras().get("data");
				mImageView.setImageBitmap(bitmap);
				
				// can set picture saving location
//				mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/CuttingPic/";
//				File dir = new File(mFilePath);
//				if(!dir.exists()) {
//					dir.mkdirs();
//				}
				mPhotoPath = "mnt/sdcard/DCIM/Camera/" + getPhotoFileName(); // by now: setting directly path to save picture
				Log.d(TAG, "mPhotoPath="+mPhotoPath);
				File file = new File(mPhotoPath);
				try {
					FileOutputStream fOut = new FileOutputStream(file);
					bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
					fOut.flush();
					fOut.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else if (requestCode == SELECT_PIC_TO_CUT && resultCode == RESULT_OK) {
			photoUriInMedia = data.getData();
			String[] filePathColumn = { MediaStore.Images.Media.DATA };
			
			Cursor cursor = getContentResolver().query(photoUriInMedia, filePathColumn, null, null, null);
			cursor.moveToFirst();
			
			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			mPhotoPath = cursor.getString(columnIndex);
			cursor.close();
			
			mPhotoFile = new File(mPhotoPath);
			photoUriInMedia = Uri.fromFile(mPhotoFile);
			Log.d(TAG, "+++++++++photoUriInMedia="+photoUriInMedia);
			if (photoUriInMedia != null) {
				cutPhoto(data);
			} else {
				Log.d(TAG, "no data");
			}			
		}
	}

	private String getPhotoFileName() {
		Date date = new Date(System.currentTimeMillis());
		SimpleDateFormat dateFormat = new SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss");
		return dateFormat.format(date) + ".jpg";
	}
	
	private void cutPhoto(Intent data) {
		Log.d(TAG, "cutPhoto");
		Intent in = new Intent("com.android.camera.action.CROP");
		in.setDataAndType(photoUriInMedia, "image/*");
		
		in.putExtra("crop", "true");
		
		in.putExtra("outputX", 320);
		in.putExtra("outputY", 320);
		
		in.putExtra("aspectX", 1);
		in.putExtra("aspectY", 1);
		
		in.putExtra("return-data", true);
		startActivityForResult(in, CAMERA_RESULT_CUT_OVER);
	}
}
