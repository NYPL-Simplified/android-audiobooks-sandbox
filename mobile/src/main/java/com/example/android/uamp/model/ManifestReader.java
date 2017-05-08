package com.example.android.uamp.model;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;


/**
 * TODO
 * Created by daryachernikhova on 5/2/17.
 */

public class ManifestReader {
	private static final String TAG = ManifestReader.class.getSimpleName();
	public static Boolean LOGGING = true;

	private static boolean isExternalStorageReadOnly() {
		String extStorageState = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
			return true;
		}
		return false;
	}


	private static boolean isExternalStorageAvailable() {
		String extStorageState = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
			return true;
		}
		return false;
	}


	public static void logd(String message) {
		if (LOGGING) {
			Log.d(TAG, message);
		} else {
			System.out.println(TAG + message);
		}
	}


	public String readManifest(File manifestFile) {
		/*
		if (isExternalStorageAvailable() && !isExternalStorageReadOnly()) {
			myFileHandle = new File(getExternalFilesDir(filepath), filename);
		} else {

		}

			FileOutputStream fos = new FileOutputStream(myFileHandle); // if using external storage
			FileOutputStream fOut = context.openFileOutput(filename, context.MODE_PRIVATE); // if internal
			fos.write(inputText.getText().toString().getBytes());
			fos.close();

			FileInputStream fis = new FileInputStream(myExternalFile); // if external storage used
			FileInputStream fin = openFileInput(filename);  // if internal
		*/

		if (manifestFile == null || !manifestFile.canRead()) {
			// TODO: is this what should happen, or better to throw an exception?
			return null;
		}
		logd(String.format("Found and can read manifest file: %s", manifestFile));

		BufferedReader br = null;
		String manifestBody = null;
		try {
			br = new BufferedReader(new FileReader(manifestFile));
			manifestBody = readManifest(br);
		} catch (FileNotFoundException e) {
			// TODO
			e.printStackTrace();
		} catch (Exception e) {
			// TODO clean up
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}

		return manifestBody;
	}


	public String readManifest(InputStream manifestFile) {
		if (manifestFile == null) {
			// TODO: is this what should happen, or better to throw an exception?
			return null;
		}

		BufferedReader br = null;
		String manifestBody = null;
		try {
			br = new BufferedReader(new InputStreamReader(manifestFile));
			manifestBody = readManifest(br);
		} catch (Exception e) {
			// TODO clean up
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}

		return manifestBody;
	}


	public String readManifest(BufferedReader reader) {
		StringBuilder manifestBody = new StringBuilder();
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				manifestBody.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return manifestBody.toString();
	}


	public ManifestModel parseManifest(String manifestBody) {
		ManifestModel manifestModel = null;

		if (manifestBody == null) {
			// TODO clean up
			return manifestModel;
		}

		try {
			Gson gson = new GsonBuilder().create();
			manifestModel = gson.fromJson(manifestBody, ManifestModel.class);

		} catch (JsonSyntaxException e) {
			// TODO clean up
			e.printStackTrace();
		}
		/*
                        Log.d("VOLLEY", entireJokeResponse.value.joke);
                        Log.d("VOLLEY", entireJokeResponse.type);

                    } else if (object instanceof JSONArray) {
                        JSONArray json = (JSONArray) object;
                        Log.d("VOLLEY array", json.toString());
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
		*/
		return manifestModel;
	}

}
