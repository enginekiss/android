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
import android.graphics.Bitmap.CompressFormat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.example.android.bitmapfun.BuildConfig;

import java.io.File;

/**
 * 此类保存了我们的bitmap caches（包括内存缓存和磁盘缓存）
 */
public class ImageCache {
	private static final String TAG = "ImageCache";

	// 默认内存缓存大小
	private static final int DEFAULT_MEM_CACHE_SIZE = 1024 * 1024 * 5; // 5MB

	// 默认磁盘缓存大小
	private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB

	// 当将images写到磁盘缓存时的一些压缩设置
	private static final CompressFormat DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG;
	private static final int DEFAULT_COMPRESS_QUALITY = 70;

	// 可以简单控制各种缓存的开关常量
	private static final boolean DEFAULT_MEM_CACHE_ENABLED = true; // 是否使用内存缓存
	private static final boolean DEFAULT_DISK_CACHE_ENABLED = true;// 是否使用磁盘缓存
	private static final boolean DEFAULT_CLEAR_DISK_CACHE_ON_START = false; // 是否在start的时候清除磁盘缓存

	private DiskLruCache mDiskCache;
	private LruCache<String, Bitmap> mMemoryCache;

	/**
	 * 使用指定的参数创建一个新的ImageCache对象
	 * 
	 * @param context
	 * @param cacheParams
	 *            用来初始化缓存的缓存参数
	 */
	public ImageCache(Context context, ImageCacheParams cacheParams) {
		init(context, cacheParams);
	}

	/**
	 * 使用默认参数创建一个新的ImageCache对象
	 * 
	 * @param context
	 * @param uniqueName
	 *            给缓存目录指定的唯一的名字
	 */
	public ImageCache(Context context, String uniqueName) {
		init(context, new ImageCacheParams(uniqueName));
	}

	/**
	 * 在{@link RetainFragment}里寻找并返回一个存在的ImageCache，
	 * 如果不存在则创建一个默认的ImageCache，并将其保存到{@link RetainFragment}里
	 * 
	 * @param activity
	 *            The calling {@link FragmentActivity}
	 * @param uniqueName
	 *            附加在缓存目录里的唯一名字
	 * @return 如果存在ImageCache对象则返回，否则创建一个新的
	 */
	public static ImageCache findOrCreateCache(final FragmentActivity activity, final String uniqueName) {
		return findOrCreateCache(activity, new ImageCacheParams(uniqueName));
	}

	/**
	 * 在{@link RetainFragment}里寻找并返回一个存在的ImageCache，
	 * 如果不存在则用给定的参数创建一个ImageCache，并将其保存到{@link RetainFragment}里
	 * 
	 * @param activity
	 *            The calling {@link FragmentActivity}
	 * @param cacheParams
	 *            创建ImageCache需用到的缓存参数
	 * @return 如果存在ImageCache对象则返回，否则创建一个新的
	 */
	public static ImageCache findOrCreateCache(final FragmentActivity activity, ImageCacheParams cacheParams) {

		// 寻找或者创建一个新的非UI的RetainFragment实例
		final RetainFragment mRetainFragment = RetainFragment.
				findOrCreateRetainFragment(activity.getSupportFragmentManager());

		// 检测是否有ImageCache存储在RetainFragment
		ImageCache imageCache = (ImageCache) mRetainFragment.getObject();

		// 如果ImageCache不存在则创建一个新的，并将其保存至RetainFragment
		if (imageCache == null) {
			Log.d(TAG, "imageCache不存在,创建一个新的，将其保存至RetainFragment里");
			imageCache = new ImageCache(activity, cacheParams);
			mRetainFragment.setObject(imageCache);
		}

		return imageCache;
	}

	/**
	 * 用提供的参数cacheParams初始化cache
	 * 
	 * @param context
	 * @param cacheParams
	 *            初始化cache需要用到的参数
	 */
	private void init(Context context, ImageCacheParams cacheParams) {
		final File diskCacheDir = DiskLruCache.getDiskCacheDir(context, cacheParams.uniqueName);

		// Set up disk cache
		if (cacheParams.diskCacheEnabled) {
			mDiskCache = DiskLruCache.openCache(context, diskCacheDir, cacheParams.diskCacheSize);
			mDiskCache.setCompressParams(cacheParams.compressFormat, cacheParams.compressQuality);
			if (cacheParams.clearDiskCacheOnStart) {
				Log.d(TAG, "cacheParams.clearDiskCacheOnStart = true,清除磁盘缓存！");
				mDiskCache.clearCache();
			}
		}

		// Set up memory cache
		if (cacheParams.memoryCacheEnabled) {
			mMemoryCache = new LruCache<String, Bitmap>(cacheParams.memCacheSize) {
				/**
				 * Measure item size in bytes rather than units which is more
				 * practical for a bitmap cache
				 */
				@Override
				protected int sizeOf(String key, Bitmap bitmap) {
					return Utils.getBitmapSize(bitmap);
				}
			};
		}
	}

	public void addBitmapToCache(String data, Bitmap bitmap) {
		if (data == null || bitmap == null) {
			return;
		}

		// 添加到内存缓存
		if (mMemoryCache != null && mMemoryCache.get(data) == null) {
			Log.d(TAG, "添加到内存缓存,data = " + data);
			mMemoryCache.put(data, bitmap);
		}

		// 添加到磁盘缓存
		if (mDiskCache != null && !mDiskCache.containsKey(data)) {
			Log.d(TAG, "添加到磁盘缓存,data = " + data);
			mDiskCache.put(data, bitmap);
		}
	}

	/**
	 * 从内存中获取缓存
	 * 
	 * @param data
	 *            要获取的item的唯一标识
	 * @return 如果找到则返回对应bitmap, 否则为null
	 */
	public Bitmap getBitmapFromMemCache(String data) {
		if (mMemoryCache != null) {
			final Bitmap memBitmap = mMemoryCache.get(data);
			if (memBitmap != null) {
				if (BuildConfig.DEBUG) {
					Log.d(TAG, "内存缓存Hit(命中)");
				}
				return memBitmap;
			}
		}
		return null;
	}

	/**
	 * 从磁盘缓存获取
	 * 
	 * @param data
	 *            要获取的item的唯一标识
	 * @return 如果找到则返回对应bitmap, 否则为null
	 */
	public Bitmap getBitmapFromDiskCache(String data) {
		if (mDiskCache != null) {
			return mDiskCache.get(data);
		}
		return null;
	}

	/**
	 * 清除所有缓存（内存和磁盘）
	 */
	public void clearCaches() {
		Log.d(TAG, "清除所有缓存（内存和磁盘）");
		mDiskCache.clearCache(); // 清除所有磁盘缓存
		mMemoryCache.evictAll(); // 清除所有内存缓存
	}

	/**
	 * 包含缓存参数的holder类
	 */
	public static class ImageCacheParams {
		public String uniqueName;
		public int memCacheSize = DEFAULT_MEM_CACHE_SIZE; // 默认内存缓存大小
		public int diskCacheSize = DEFAULT_DISK_CACHE_SIZE; // 默认磁盘缓存大小
		public CompressFormat compressFormat = DEFAULT_COMPRESS_FORMAT; // 图片的默认压缩格式
		public int compressQuality = DEFAULT_COMPRESS_QUALITY; // 图片的默认压缩质量
		public boolean memoryCacheEnabled = DEFAULT_MEM_CACHE_ENABLED; // 是否使用内存缓存
		public boolean diskCacheEnabled = DEFAULT_DISK_CACHE_ENABLED;// 是否使用磁盘缓存
		public boolean clearDiskCacheOnStart = DEFAULT_CLEAR_DISK_CACHE_ON_START; // 是否在start是清除磁盘缓存

		public ImageCacheParams(String uniqueName) {
			this.uniqueName = uniqueName;
		}
	}
}
