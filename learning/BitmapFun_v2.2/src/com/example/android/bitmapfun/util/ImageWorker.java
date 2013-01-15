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

import java.lang.ref.WeakReference;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.example.android.bitmapfun.BuildConfig;

/**
 * 该类封装了一个负责去加载一个bitmap到一个ImageView的任意时长的work，
 * 他处理了一些事情，例如使用内存缓存和磁盘缓存，运行在后台线程里并且会为imageView设置一张占位图（默认预览图）
 */
public abstract class ImageWorker {
	private static final String TAG = "ImageWorker";
	private static final int FADE_IN_TIME = 200; // 图片显示渐进效果（200毫秒）

	private ImageCache mImageCache; // 缓存类，包括内存缓存和磁盘缓存
	private Bitmap mLoadingBitmap; // 占位图
	private boolean mFadeInBitmap = true; // 显示图片时是否有渐进效果
	private boolean mExitTasksEarly = false; // 提前结束tasks

	protected Context mContext;
	protected ImageWorkerAdapter mImageWorkerAdapter; // 使用了ImageWorker类与子类的简单adapter

	protected ImageWorker(Context context) {
		mContext = context;
	}

	/**
	 * 加载一张指定了data参数的image到一个ImageView里（需复写方法
	 * {@link ImageWorker#processBitmap(Object)}去自定义处理逻辑）。 如果{@link ImageCache}
	 * 被设置为使用{@link ImageWorker#setImageCache(ImageCache)} 则内存缓存与磁盘缓存将被使用。
	 * 如果image在内存缓存中被找到了则该image立刻被set， 否则{@link AsyncTask} 将被创建并用来异步加载bitmap。
	 * 
	 * @param data
	 *            需要下载的图片的url
	 * @param imageView
	 *            用来绑定下载完的图片
	 */
	public void loadImage(Object data, ImageView imageView) {
		Bitmap bitmap = null;

		if (mImageCache != null) {
			bitmap = mImageCache.getBitmapFromMemCache(String.valueOf(data));
		}

		if (bitmap != null) {
			// 在内存缓存中找到bitmap
			Log.d(TAG, "");
			imageView.setImageBitmap(bitmap);
			// 尝试取消潜在的work
		} else if (cancelPotentialWork(data, imageView)) {
			// 如果取消成功则开启新的task
			final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
			final AsyncDrawable asyncDrawable = new AsyncDrawable( mContext.getResources(), mLoadingBitmap, task);
			imageView.setImageDrawable(asyncDrawable);
			Log.d(TAG, "取消work成功，开启新的task， data = " + data);
			task.execute(data);
		}
	}

	/**
	 * 从一个被设置的adapter里面加载一张image到imageview里面（需复写方法
	 * {@link ImageWorker#processBitmap(Object)}去自定义处理逻辑）。 如果{@link ImageCache}
	 * 被设置为使用{@link ImageWorker#setImageCache(ImageCache)} 则内存缓存与磁盘缓存将被使用。
	 * 如果image在内存缓存中被找到了则该image立刻被set， 否则{@link AsyncTask} 将被创建并用来异步加载bitmap。
	 * 在使用此方法之前 {@link ImageWorker#setAdapter(ImageWorkerAdapter)}必须要先被调用
	 * 
	 * @param data
	 *            需要下载的图片的url
	 * @param 用来绑定下载完的图片
	 */
	public void loadImage(int num, ImageView imageView) {
		if (mImageWorkerAdapter != null) {
			// mImageWorkerAdapter.getItem(num)返回的是图片的url
			loadImage(mImageWorkerAdapter.getItem(num), imageView);
		} else {
			throw new NullPointerException("数据没有被设置, 必须先调用setAdapter()。");
		}
	}

	/**
	 * 设置当后台线程启动时要显示的占位图
	 * 
	 * @param bitmap
	 */
	public void setLoadingImage(Bitmap bitmap) {
		mLoadingBitmap = bitmap;
	}

	/**
	 * 设置当后台线程启动时要显示的占位图
	 * 
	 * @param resId
	 */
	public void setLoadingImage(int resId) {
		mLoadingBitmap = BitmapFactory.decodeResource(mContext.getResources(), resId);
	}

	/**
	 * 设置在该ImageWorker上要使用的{@link ImageCache}
	 * 
	 * @param cacheCallback
	 */
	public void setImageCache(ImageCache cacheCallback) {
		mImageCache = cacheCallback;
	}

	public ImageCache getImageCache() {
		return mImageCache;
	}

	/**
	 * 如果设置为true，则当后台线程加载完毕后image将显示渐进效果
	 * 
	 * @param fadeIn
	 */
	public void setImageFadeIn(boolean fadeIn) {
		mFadeInBitmap = fadeIn;
	}

	/**
	 * 设置是否提前结束tasks
	 * 
	 * @param exitTasksEarly
	 */
	public void setExitTasksEarly(boolean exitTasksEarly) {
		mExitTasksEarly = exitTasksEarly;
	}

	/**
	 * 子类应该重写此方法去定义任意processing或work，用来创建一个最终的bitmap。 此方法将被运行在后台并且可以长时间运行。
	 * 例如，在此可以重新定义一些大图的尺寸，或者从网上加载一张image
	 * 
	 * @param data
	 *            该参数为需要处理的指定image, 例如可以是
	 *            {@link ImageWorker#loadImage(Object, ImageView)}返回的值
	 * @return 返回处理好的bitmap
	 */
	protected abstract Bitmap processBitmap(Object data);

	/**
	 * 取消与imageview相关的task
	 * 
	 * @param imageView
	 */
	public static void cancelWork(ImageView imageView) {
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
		if (bitmapWorkerTask != null) {
			bitmapWorkerTask.cancel(true);
			if (BuildConfig.DEBUG) {
				final Object bitmapData = bitmapWorkerTask.data;
				Log.d(TAG, "cancelWork - 与" + bitmapData + "对应的work被取消");
			}
		}
	}

	/**
	 * 如果当前工作已经被取消或者如果当前没有对应此imageview的工作在进行，则返回true
	 * 如果这个work正在处理相同的url，该work还没有被停止，则返回false
	 */
	public static boolean cancelPotentialWork(Object data, ImageView imageView) {
		
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
		if (bitmapWorkerTask != null) {
			// 如果bitmapWorkerTask不为null，则表示之前有对应此imageview的任务在进行，
			// 下面将判断此imageciew的任务是否对应相同的data（即url）
			final Object bitmapData = bitmapWorkerTask.data;
			if (bitmapData == null || !bitmapData.equals(data)) {
				// 如果不是相同的data（即url），则取消之前的url任务
				bitmapWorkerTask.cancel(true); // cancel为AsyncTask的方法，尝试取消正在执行的task
				if (BuildConfig.DEBUG) {
					Log.d(TAG, "cancelPotentialWork - 当前不是之前的work，取消之前的work: " + data);
				}
			} else {
				// 否则为相同的url任务，返回false后，则当前将继续进行，不在处理新的url任务
				Log.d(TAG, "cancelPotentialWork - 和之前的是同一个work，不取消。work = "
						+ data);
				return false;
			}
		}
		// 如果bitmapWorkerTask为null,则表示之前没有任务在进行，--》将启动新的任务
		return true;
	}

	/**
	 * @param imageView
	 *            任何imageView
	 * @return 返回当前活动的并且与imageView相关联的work task 如果没有这样的task则返回null
	 */
	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncDrawable) {
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}

	/**
	 * 将进行异步处理image的当前AsyncTask
	 */
	private class BitmapWorkerTask extends AsyncTask<Object, Void, Bitmap> {
		private Object data;
		// WeakReference是为了避免ImageView被回收时由于引用造成无法回收
		private final WeakReference<ImageView> imageViewReference;

		public BitmapWorkerTask(ImageView imageView) {
			imageViewReference = new WeakReference<ImageView>(imageView);
		}

		/**
		 * 后台处理
		 */
		@Override
		protected Bitmap doInBackground(Object... params) {
			data = params[0];
			final String dataString = String.valueOf(data);
			Bitmap bitmap = null;

			// 如果该image cache是可用的，并且该task没有被另外一个线程取消，
			// 并且最初绑定到这个task的ImageView任然绑定在当前task上，
			// 并且我们的"exit early"标志是没有被设定的话，
			// 则尝试从cache中抓取该bitmap
			if (mImageCache != null && !isCancelled() && getAttachedImageView() != null && !mExitTasksEarly) {
				bitmap = mImageCache.getBitmapFromDiskCache(dataString);
			}

			// 如果在cache里没有找到该bitmap，并且该task没有被另外一个线程取消，
			// 并且最初绑定到这个task的ImageView任然绑定在当前task上，
			// 并且我们的"exit early"标志是没有被设定的话，
			// 则调用main处理方法（被子类实现的方法）
			if (bitmap == null && !isCancelled() && getAttachedImageView() != null && !mExitTasksEarly) {
				bitmap = processBitmap(params[0]);
			}

			// 如果该bitmap被处理了并且image cache是可用的，那么则添加该处理过的bitmap到缓存里作为将来使用，
			// 要注意的是这里没检测是否task在这里被取消，如果刚好被取消的话，并且线程还在运行，
			// 则也一样将这处理过的bitmap添加到cache里便于将来可能会用到
			if (bitmap != null && mImageCache != null) {
				// 同时缓存位图到内存与磁盘
				mImageCache.addBitmapToCache(dataString, bitmap);
			}

			return bitmap;
		}

		/**
		 * 一旦image被处理完毕了，则将其与imageview关联起来
		 */
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			// 如果cancel在该task里被调用或者"exit early"标志被设置，则将bitmap设置为null
			// if cancel was called on this task or the "exit early" flag is set
			// then we're done
			if (isCancelled() || mExitTasksEarly) {
				bitmap = null;
			}

			// 获取被附加（Attached）的imageview
			final ImageView imageView = getAttachedImageView();
			if (bitmap != null && imageView != null) {
				setImageBitmap(imageView, bitmap);
			}
		}

		/**
		 * 如果该ImageView的task任然指向本task，则返回与之相关联的ImageView 否则返回null
		 */
		private ImageView getAttachedImageView() {
			final ImageView imageView = imageViewReference.get();
			final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

			// 如果imageView相关联的task就是本task
			if (this == bitmapWorkerTask) {
				return imageView;
			}

			return null;
		}
	}

	/**
	 * 一个自定义的Drawable，当work在处理时他将添付在imageView上面。 他包含了一个当前work
	 * task的一个引用，所以如果有一个新的绑定到来的话他将能被停止掉。 这样能保证仅仅只有最后一个开始的worker
	 * process能够绑定他的结果，并且是独立的完成顺序。
	 */
	private static class AsyncDrawable extends BitmapDrawable {
		private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
			super(res, bitmap);
			bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>( bitmapWorkerTask);
		}

		public BitmapWorkerTask getBitmapWorkerTask() {
			return bitmapWorkerTaskReference.get();
		}
	}

	/**
	 * 当处理结束后并且最后该bitmap应该被设置在Imageview上，则调用此方法
	 * 
	 * @param imageView
	 * @param bitmap
	 */
	private void setImageBitmap(ImageView imageView, Bitmap bitmap) {
		if (mFadeInBitmap) { // 渐进标志
			// 将drawable从透明的状态过度到正常显示状态
			final TransitionDrawable td = new TransitionDrawable(
					new Drawable[] {
							new ColorDrawable(android.R.color.transparent),
							new BitmapDrawable(mContext.getResources(), bitmap) });
			// 设置背景为预加载图片（R.drawable.empty_photo）
			imageView.setBackgroundDrawable(new BitmapDrawable(mContext .getResources(), mLoadingBitmap));

			imageView.setImageDrawable(td);
			td.startTransition(FADE_IN_TIME); // 渐进时间200毫秒
		} else {
			// 否则不用渐进效果，直接设置图片
			imageView.setImageBitmap(bitmap);
		}
	}

	/**
	 * 设置拥有后台数据的simple adapter
	 * 
	 * @param adapter
	 */
	public void setAdapter(ImageWorkerAdapter adapter) {
		mImageWorkerAdapter = adapter;
	}

	/**
	 * 获取当前的适配器
	 * 
	 * @return
	 */
	public ImageWorkerAdapter getAdapter() {
		return mImageWorkerAdapter;
	}

	/**
	 * 使用了ImageWorker类与子类的非常简单的adapter
	 */
	public static abstract class ImageWorkerAdapter {
		public abstract Object getItem(int num);

		public abstract int getSize();
	}
}
