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

package com.example.android.bitmapfun.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.bitmapfun.BuildConfig;
import com.example.android.bitmapfun.R;
import com.example.android.bitmapfun.provider.Images;
import com.example.android.bitmapfun.util.DiskLruCache;
import com.example.android.bitmapfun.util.ImageCache;
import com.example.android.bitmapfun.util.ImageCache.ImageCacheParams;
import com.example.android.bitmapfun.util.ImageFetcher;
import com.example.android.bitmapfun.util.ImageResizer;
import com.example.android.bitmapfun.util.Utils;

/**
 * 显示在ImageGridActivity（可看做MainActivity）上面的主要的fragment 可以相当平滑的过度到GridView里
 * 实现的关键点是ImageWorker类，用ImageCache去异步加载children UI界面显示很流畅，并且能快速地缓存缩略图并且快速恢复
 * 在用户旋转屏幕时也能很顺畅
 */
public class ImageGridFragment extends Fragment implements AdapterView.OnItemClickListener {
	private static final String TAG = "ImageGridFragment";
	private static final String IMAGE_CACHE_DIR = "thumbs";

	private int mImageThumbSize;
	private int mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private ImageResizer mImageWorker;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public ImageGridFragment() {
		
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "ImageGridFragment--onCreate()调用");
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		// dip转换成pixel，例如100转换成150
		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
		// 1转换成2
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);
		Log.d(TAG, "缩略图大小 mImageThumbSize = " + mImageThumbSize);
		Log.d(TAG, "缩略图间隔 mImageThumbSpacing = " + mImageThumbSpacing);

		mAdapter = new ImageAdapter(getActivity());

		// 创建一个图片缓存参数（唯一标识名、内存缓存、磁盘缓存大小、图片压缩格式、是否存缓存、是否启动时清磁盘缓存等）
		ImageCacheParams cacheParams = new ImageCacheParams(IMAGE_CACHE_DIR);
		Log.d(TAG, "缩略图图片缓存目录 IMAGE_CACHE_DIR = " + IMAGE_CACHE_DIR);

		// Allocate a third of the per-app memory limit to the bitmap memory
		// cache. This value
		// should be chosen carefully based on a number of factors. Refer to the
		// corresponding
		// Android Training class for more discussion:
		// http://developer.android.com/training/displaying-bitmaps/
		// In this case, we aren't using memory for much else other than this
		// activity and the
		// ImageDetailActivity so a third lets us keep all our sample image
		// thumbnails in memory
		// at once.

		// 在夏普sh8188u里Utils.getMemoryClass(getActivity())的值为64
		// cacheParams.memCacheSize为64/3 = 22M左右
		cacheParams.memCacheSize = 1024 * 1024 * Utils.getMemoryClass(getActivity()) / 3;
		Log.d(TAG, "memCacheSize = " + cacheParams.memCacheSize + ", 内存等级 = "
				+ Utils.getMemoryClass(getActivity()));

		// ImageWorker负责进行异步加载images到ImageView里
		mImageWorker = new ImageFetcher(getActivity(), mImageThumbSize);
		mImageWorker.setAdapter(Images.imageThumbWorkerUrlsAdapter);
		mImageWorker.setLoadingImage(R.drawable.empty_photo);
		mImageWorker.setImageCache(ImageCache.findOrCreateCache(getActivity(), cacheParams));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "ImageGridFragment--onCreateView()调用");

		// 填充一个GridView布局
		final View v = inflater.inflate(R.layout.image_grid_fragment, container, false);
		final GridView mGridView = (GridView) v.findViewById(R.id.gridView);
		mGridView.setAdapter(mAdapter);
		mGridView.setOnItemClickListener(this);

		// This listener is used to get the final width of the GridView and then
		// calculate the
		// number of columns and the width of each column. The width of each
		// column is variable
		// as the GridView has stretchMode=columnWidth. The column width is used
		// to set the height
		// of each view so we get nice square thumbnails.
		mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						if (mAdapter.getNumColumns() == 0) {
							final int numColumns = (int) Math.floor(mGridView .getWidth()
									/ (mImageThumbSize + mImageThumbSpacing));
							if (numColumns > 0) {
								final int columnWidth = (mGridView.getWidth() / numColumns) - mImageThumbSpacing;
								mAdapter.setNumColumns(numColumns);
								mAdapter.setItemHeight(columnWidth);
								Log.d(TAG, "mGridView.getWidth() = " + mGridView.getWidth());
								Log.d(TAG, "numColumns = " + numColumns + ", columnWidth = " + columnWidth);

								if (BuildConfig.DEBUG) {
									Log.d(TAG, "onCreateView - numColumns set to " + numColumns);
								}
							}
						}
					}
				});

		return v;
	}

	@Override
	public void onResume() {
		Log.d(TAG, "ImageGridFragment--onResume()调用");
		super.onResume();
		mImageWorker.setExitTasksEarly(false);
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onPause() {
		super.onPause();
		mImageWorker.setExitTasksEarly(true);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		final Intent i = new Intent(getActivity(), ImageDetailActivity.class);
		i.putExtra(ImageDetailActivity.EXTRA_IMAGE, (int) id);
		startActivity(i);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.main_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.clear_cache:
			final ImageCache cache = mImageWorker.getImageCache();
			if (cache != null) {
				mImageWorker.getImageCache().clearCaches();
				DiskLruCache.clearCache(getActivity(), ImageFetcher.HTTP_CACHE_DIR);
				Toast.makeText(getActivity(), R.string.clear_cache_complete, Toast.LENGTH_SHORT).show();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * GridView的Adapter
	 * 
	 * The main adapter that backs the GridView. This is fairly standard except
	 * the number of columns in the GridView is used to create a fake top row of
	 * empty views as we use a transparent ActionBar and don't want the real top
	 * row of images to start off covered by it.
	 */
	private class ImageAdapter extends BaseAdapter {

		private final Context mContext;
		private int mItemHeight = 0;
		private int mNumColumns = 0;
		private int mActionBarHeight = -1;
		private GridView.LayoutParams mImageViewLayoutParams;

		public ImageAdapter(Context context) {
			super();
			mContext = context;
			mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		}

		@Override
		public int getCount() {
			// adapter的大小 + 顶部空行的列数
			return mImageWorker.getAdapter().getSize() + mNumColumns; // mNumColumns
																		// = 0
		}

		@Override
		public Object getItem(int position) {
			return position < mNumColumns ? null : mImageWorker.getAdapter().getItem(position - mNumColumns);
		}

		@Override
		public long getItemId(int position) {
			return position < mNumColumns ? 0 : position - mNumColumns;
		}

		/**
		 * 返回有多少个不同样式的view
		 */
		@Override
		public int getViewTypeCount() {
			// 这里有两种view，普通的ImageView和顶部行的空view
			return 2;
		}

		@Override
		public int getItemViewType(int position) {
			return (position < mNumColumns) ? 1 : 0;
		}

		/**
		 * 无论项ID代表的基础数据的是否变化都保持不变。 返回值如果为TRUE，意味着相同的ID始终引用相同的对象。
		 */
		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup container) {
			// 首先检查是不是顶部行
			if (position < mNumColumns) {
				if (convertView == null) {
					convertView = new View(mContext);
				}
				// 计算ActionBar的高度
				if (mActionBarHeight < 0) {
					TypedValue tv = new TypedValue();
					// yexiubiao
					if (false/*
							 * mContext.getTheme().resolveAttribute(android.R.attr
							 * .actionBarSize, tv, true)
							 */) {
						mActionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, mContext
										.getResources().getDisplayMetrics());
					} else {
						// No ActionBar style (pre-Honeycomb or ActionBar not in
						// theme)
						mActionBarHeight = 0;
					}
				}
				// 设置一个有着ActionBar高度的空view
				convertView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mActionBarHeight));
				return convertView;
			}

			// 接下来处理main ImageView的缩略图
			ImageView imageView;
			if (convertView == null) { // 如果convertView没有被回收，则实例化并且对其初始化
				imageView = new ImageView(mContext);
				// 缩放类型,CENTER_CROP:按统一比例缩放图片（保持图片的尺寸比例）便于图片的两维（宽度和高度）等于或者大于相应的视图的维度
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView.setLayoutParams(mImageViewLayoutParams);
			} else {
				imageView = (ImageView) convertView;
			}

			// 检测高度来匹配我们的列宽
			if (imageView.getLayoutParams().height != mItemHeight) {
				imageView.setLayoutParams(mImageViewLayoutParams);
			}

			// 最后异步加载image到ImageView里，在这里也维护了当后台线程启动时设置占位图
			mImageWorker.loadImage(position - mNumColumns, imageView);
			return imageView;
		}

		/**
		 * 设置item的高度，当我们知道列宽的时候，height能够被匹配
		 * 
		 * @param height
		 */
		public void setItemHeight(int height) {
			if (height == mItemHeight) {
				return;
			}
			mItemHeight = height;
			mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, mItemHeight);
			mImageWorker.setImageSize(height);
			notifyDataSetChanged();
		}

		public void setNumColumns(int numColumns) {
			mNumColumns = numColumns;
		}

		public int getNumColumns() {
			return mNumColumns;
		}
	}
}
