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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;

/**
 * 一个简单的非UI Fragment,用来存储一个简单的Object并且用来保持屏幕切换的时候的快速处理,
 * 在这里他将用来保持ImageCache对象。
 */
public class RetainFragment extends Fragment {
	private static final String TAG = "RetainFragment";
	private Object mObject;

	/**
	 * 每个Fragment documentation的空的构造器
	 */
	public RetainFragment() {
	}

	/**
	 * 查找一个存在的该Fragment的实例，如果不存在则创建它并使用FragmentManager把他添加上去
	 * 
	 * @param fm
	 *            要使用的FragmentManager
	 * @return 如果存在则返回Fragment的实例，否则返回一个新创建的Fragment
	 */
	public static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
		Log.d(TAG, "RetainFragment--findOrCreateRetainFragment()调用");
		// 检测是否有保存的worker fragment.
		RetainFragment mRetainFragment = (RetainFragment) fm.findFragmentByTag(TAG);

		// 如果没有保存（或者是第一次运行），则创建新的并将其添加到FragmentManager里
		if (mRetainFragment == null) {
			Log.d(TAG, "RetainFragment--创建新的mRetainFragment并将其添加到FragmentManager里");
			mRetainFragment = new RetainFragment();
			fm.beginTransaction().add(mRetainFragment, TAG).commit();
		}
		return mRetainFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "RetainFragment--onCreate()调用");
		super.onCreate(savedInstanceState);

		// 横竖屏切换时?
		// 通过使用Fragment去调用 setRetainInstance(true) 传递到新的Activity中。
		// 在activity被recreate之后, 这个保留的 Fragment 会重新附着上。
		// 这样就可以访问Cache对象，从中获取到图片信息并快速的重新添加到ImageView对象中。
		setRetainInstance(true);
		Log.d(TAG, "RetainFragment--setRetainInstance(true)");
	}

	/**
	 * 保存一个single object到这个Fragment里.
	 * 在ImageCache类里被调用：mRetainFragment.setObject(imageCache)
	 * 
	 * @param object
	 *            需要保存的对象
	 */
	public void setObject(Object object) {
		Log.d(TAG, "RetainFragment--setObject()调用");
		mObject = object;
	}

	/**
	 * 获取保存的对象
	 * 
	 * @return 存储的对象
	 */
	public Object getObject() {
		Log.d(TAG, "RetainFragment--getObject()调用");
		return mObject;
	}

}
