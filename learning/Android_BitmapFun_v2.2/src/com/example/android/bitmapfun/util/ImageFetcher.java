/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bitmapfun.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.example.android.bitmapfun.BuildConfig;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * {@link ImageResizer}的简单子类，用于从指定url抓取图片并重定义图片的尺寸
 */
public class ImageFetcher extends ImageResizer {
	private static final String TAG = "ImageFetcher";
	private static final int HTTP_CACHE_SIZE = 10 * 1024 * 1024; // 10MB
	public static final String HTTP_CACHE_DIR = "http";

	/**
	 * 初始化目标图片的要处理成的宽高
	 * 
	 * @param context
	 * @param imageWidth
	 * @param imageHeight
	 */
	public ImageFetcher(Context context, int imageWidth, int imageHeight) {
		super(context, imageWidth, imageHeight);
		init(context);
	}

	/**
	 * 初始化目标图片的要处理成的宽高
	 * 
	 * @param context
	 * @param imageSize
	 */
	public ImageFetcher(Context context, int imageSize) {
		super(context, imageSize);
		init(context);
	}

	private void init(Context context) {
		checkConnection(context);
	}

	/**
	 * 简单的网络连接检测
	 * 
	 * @param context
	 */
	private void checkConnection(Context context) {
		final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		if (networkInfo == null || !networkInfo.isConnectedOrConnecting()) {
			Toast.makeText(context, "没有可用的网络连接", Toast.LENGTH_LONG).show();
			Log.e(TAG, "checkConnection - 没有可用的网络连接");
		}
	}

	/**
	 * 主要的处理方法，将被ImageWorker类在后台线程里调用
	 * 
	 * @param data
	 *            要加载的bitmap的数据，在这里为一个规则的http url地址
	 * @return 返回加载后并重定义尺寸的bitmap
	 */
	private Bitmap processBitmap(String data) {
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "processBitmap - " + data);
		}

		// 从网上下载bitmap，并将其写到file里
		final File f = downloadBitmap(mContext, data);

		if (f != null) {
			// 返回一个缩略版本的bitmap
			return decodeSampledBitmapFromFile(f.toString(), mImageWidth, mImageHeight);
		}

		return null;
	}

	/**
	 * 将data转换成String后交给processBitmap处理
	 */
	@Override
	protected Bitmap processBitmap(Object data) {
		return processBitmap(String.valueOf(data));
	}

	/**
	 * Download a bitmap from a URL, write it to a disk and return the File
	 * pointer. This implementation uses a simple disk cache.
	 * 
	 * @param context
	 *            The context to use
	 * @param urlString
	 *            The URL to fetch
	 * @return A File pointing to the fetched bitmap
	 */
	public static File downloadBitmap(Context context, String urlString) {
		final File cacheDir = DiskLruCache.getDiskCacheDir(context, HTTP_CACHE_DIR);
		final DiskLruCache cache = DiskLruCache.openCache(context, cacheDir, HTTP_CACHE_SIZE);
		final File cacheFile = new File(cache.createFilePath(urlString));

		if (cache.containsKey(urlString)) {
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "downloadBitmap() - 在磁盘缓存中找到图片缓存 - " + urlString);
			}
			return cacheFile;
		}

		if (BuildConfig.DEBUG) {
			Log.d(TAG, "downloadBitmap() - 开始从网络下载图片 - " + urlString);
		}

		// 如果系统版本是froyo之前的则调用System.setProperty("http.keepAlive", "false")
		Utils.disableConnectionReuseIfNecessary();

		HttpURLConnection urlConnection = null;
		BufferedOutputStream out = null;
		InputStream in = null;

		try {
			final URL url = new URL(urlString);
			urlConnection = (HttpURLConnection) url.openConnection();
			in = new BufferedInputStream(urlConnection.getInputStream(), Utils.IO_BUFFER_SIZE); // IO缓冲大小设定为8K
			out = new BufferedOutputStream(new FileOutputStream(cacheFile), Utils.IO_BUFFER_SIZE);

			int b;
			while ((b = in.read()) != -1) {
				out.write(b);
			}

			return cacheFile;

			// Exception定义为final，在多线程环境下防止出现e的值不正确
		} catch (final IOException e) {
			Log.e(TAG, "下载btimap出现错误 - " + e);
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect(); // 关闭与http服务器的连接
			}
			if (out != null) {
				try {
					out.close();
				} catch (final IOException e) {
					Log.e(TAG, "downloadBitmap()发生错误 - " + e);
				}
			}
		}

		return null;
	}
}
