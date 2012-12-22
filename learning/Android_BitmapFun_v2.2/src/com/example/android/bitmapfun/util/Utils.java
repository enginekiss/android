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

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;

/**
 * 包含了一些静态方法的工具类
 */
public class Utils {
	
	public static final int IO_BUFFER_SIZE = 8 * 1024;
	
	private Utils() {
		
	};

	/**
	 * 如果有必要的话是连接重用无效
	 * 
	 * Froyo之前的版本bug的权宜之计，关于详细信息：
	 * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
	 */
	public static void disableConnectionReuseIfNecessary() {
		// HTTP connection reuse在froyo版本之前是有bug的
		if (hasHttpConnectionBug()) {
			System.setProperty("http.keepAlive", "false");
		}
	}

	/**
	 * 获取一个bitmap所占的字节大小
	 * 
	 * @param bitmap
	 * @return size in bytes
	 */
	@SuppressLint("NewApi")
	public static int getBitmapSize(Bitmap bitmap) {
		// yexiubiao
		/*
		 * if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
		 * return bitmap.getByteCount(); }
		 */
		// Pre HC-MR1
		return bitmap.getRowBytes() * bitmap.getHeight();
	}

	/**
	 * 姜饼（GINGERBREAD），即2.3 检测外部储存是内置的还是可移动的
	 * 
	 * @return 如果外部储存是可移动的（例如sd卡）则返回true，否则返回false
	 */
	@SuppressLint("NewApi")
	public static boolean isExternalStorageRemovable() {
		// yexiubiao
		/*
		 * if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
		 * return Environment.isExternalStorageRemovable(); }
		 */
		return true;
	}

	/**
	 * 获取外部的缓存目录
	 * 
	 * @param context
	 * @return 外部缓存目录
	 */
	@SuppressLint("NewApi")
	public static File getExternalCacheDir(Context context) {
		if (hasExternalCacheDir()) {
			return context.getExternalCacheDir();
		}

		// 在Froyo（2.2）之前我们需要自己构建一个外部缓存目录
		final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
		return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
	}

	/**
	 * 检测指定目录还有多少可用空间（字节数） 缓存目录（/Android/data/）的可用空间貌似是有限制的
	 * 
	 * @param path
	 *            需要检测的路径
	 * @return 空间可用的字节数
	 */
	@SuppressLint("NewApi")
	public static long getUsableSpace(File path) {
		// yexiubiao
		/*
		 * if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
		 * return path.getUsableSpace(); }
		 */
		final StatFs stats = new StatFs(path.getPath());
		return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
	}

	/**
	 * 获取设备的内存等级（memory class），（每个app自己能使用的内存极限）
	 * 
	 * @param context
	 * @return
	 */
	public static int getMemoryClass(Context context) {
		return ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
	}

	/**
	 * 检测系统版本是否有http URLConnection的bug，关于详细信息
	 * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
	 * 
	 * @return
	 */
	public static boolean hasHttpConnectionBug() {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO;
	}

	/**
	 * 检测对应的系统版本是否有内置的，获取外部存储目录的方法 Check if OS version has built-in external
	 * cache dir method.
	 * 
	 * @return
	 */
	public static boolean hasExternalCacheDir() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
	}

	/**
	 * 检测ActionBar是否可用
	 * 
	 * @return
	 */
	public static boolean hasActionBar() {
		// yexiubiao
		// return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
		return false;
	}
}
