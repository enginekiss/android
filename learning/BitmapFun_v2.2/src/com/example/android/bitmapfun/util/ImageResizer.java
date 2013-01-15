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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.example.android.bitmapfun.BuildConfig;

/**
 * 此类为 {@link ImageWorker} 的一个简单子类，用指定的宽高来重定义资源图片的尺寸 
 * 例如,在加载一张很大的图片到内存时，此方法将是很有用的。
 */
public class ImageResizer extends ImageWorker {
	private static final String TAG = "ImageWorker";
	protected int mImageWidth;
	protected int mImageHeight;

	/**
	 * 初始化被提供的单张目标图片的尺寸（使用宽和高参数）
	 * 
	 * @param context
	 * @param imageWidth
	 * @param imageHeight
	 */
	public ImageResizer(Context context, int imageWidth, int imageHeight) {
		super(context);
		setImageSize(imageWidth, imageHeight);
	}

	/**
	 * 初始化被提供的单张目标图片的尺寸（使用宽和高参数）
	 * 
	 * @param context
	 * @param imageSize
	 */
	public ImageResizer(Context context, int imageSize) {
		super(context);
		setImageSize(imageSize);
	}

	/**
	 * 设置目标图片的宽和高
	 * 
	 * @param width
	 * @param height
	 */
	public void setImageSize(int width, int height) {
		mImageWidth = width;
		mImageHeight = height;
	}

	/**
	 * 设置目标图片的尺寸(宽和高相同).
	 * 
	 * @param size
	 */
	public void setImageSize(int size) {
		setImageSize(size, size);
	}

	/**
	 * 主要的处理方法。此方法将在后台task中被处理。 在这里只是对资源文件做了采样处理并将其返回
	 * 
	 * @param resId
	 * @return
	 */
	private Bitmap processBitmap(int resId) {
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "调用processBitmap() - " + resId);
		}
		return decodeSampledBitmapFromResource(mContext.getResources(), resId, mImageWidth, mImageHeight);
	}

	@Override
	protected Bitmap processBitmap(Object data) {
		return processBitmap(Integer.parseInt(String.valueOf(data)));
	}

	/**
	 * 从资源文件里用指定的请求宽高解码出一张位图（指定样式）
	 * 
	 * @param res
	 *            包含了image data的资源对象
	 * @param resId
	 *            image data的资源id
	 * @param reqWidth
	 *            请求的结果位图的宽
	 * @param reqHeight
	 *            请求的结果位图的高
	 * @return 返回的bitmap跟原始图片有着相同宽高比，他的尺寸等于或大于请求的宽高
	 */
	public static Bitmap decodeSampledBitmapFromResource(Resources res,
			int resId, int reqWidth, int reqHeight) {

		// 第一次设置为inJustDecodeBounds=true只是去检测图片的尺寸
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);

		// 计算采样尺寸（即动态地计算出图片需缩放的大小）
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// 用指定的inSampleSize解码位图
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res, resId, options);
	}

	/**
	 * 从文件（File）里用指定的请求宽高解码出一张位图（指定样式）
	 * 
	 * @param filename
	 *            需要解码的文件的完整路径名
	 * @param reqWidth
	 *            请求的结果位图的宽
	 * @param reqHeight
	 *            请求的结果位图的高
	 * @return 返回的bitmap跟原始图片有着相同宽高比，他的尺寸等于或大于请求的宽高
	 */
	public static synchronized Bitmap decodeSampledBitmapFromFile(
			String filename, int reqWidth, int reqHeight) {

		// 第一次设置为inJustDecodeBounds=true只是去检测图片的尺寸
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filename, options);

		// 计算采样尺寸（即动态地计算出图片需缩放的大小）
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// 用指定的inSampleSize解码位图
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(filename, options);
	}

	/**
	 * 注意：使用2的倍数会对解码更高效的（应该是二进制的原因），
	 * 
	 * Calculate an inSampleSize for use in a {@link BitmapFactory.Options}
	 * object when decoding bitmaps using the decode* methods from
	 * {@link BitmapFactory}. This implementation calculates the closest
	 * inSampleSize that will result in the final decoded bitmap having a width
	 * and height equal to or larger than the requested width and height. This
	 * implementation does not ensure a power of 2 is returned for inSampleSize
	 * which can be faster when decoding but results in a larger bitmap which
	 * isn't as useful for caching purposes.
	 * 
	 * @param options
	 *            一个已经获得值的options对象（options为out*参数，
	 *            设置为inJustDecodeBounds==true后调用decode*，options的值会被填充）
	 * 
	 * @param reqWidth
	 *            请求的结果位图的宽
	 * @param reqHeight
	 *            请求的结果位图的高
	 * @return 返回值为一个动态的缩放值
	 */
	public static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		// image的原始宽高
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = Math.round((float) height / (float) reqHeight);
			} else {
				inSampleSize = Math.round((float) width / (float) reqWidth);
			}

			// This offers some additional logic in case the image has a strange
			// aspect ratio. For example, a panorama may have a much larger
			// width than height. In these cases the total pixels might still
			// end up being too large to fit comfortably in memory, so we should
			// be more aggressive with sample down the image (=larger
			// inSampleSize).

			final float totalPixels = width * height;

			// Anything more than 2x the requested pixels we'll sample down
			// further.
			final float totalReqPixelsCap = reqWidth * reqHeight * 2;

			while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
				inSampleSize++;
			}
		}
		return inSampleSize;
	}
}
