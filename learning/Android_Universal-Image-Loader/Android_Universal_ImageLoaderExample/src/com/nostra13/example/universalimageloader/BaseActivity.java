package com.nostra13.example.universalimageloader;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.nostra13.universalimageloader.core.ImageLoader;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public abstract class BaseActivity extends Activity {
    /** 得到ImageLoader的单例 */
	protected ImageLoader imageLoader = ImageLoader.getInstance();
    /** Activity状态是否保存的标识 */
	private boolean instanceStateSaved;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.item_clear_memory_cache:
				// 清理内存缓存
				imageLoader.clearMemoryCache();
				return true;
			case R.id.item_clear_disc_cache:
				// 清理SD卡缓存
				imageLoader.clearDiscCache();
				return true;
			default:
				return false;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		instanceStateSaved = true;
	}

	@Override
	protected void onDestroy() {
		if (!instanceStateSaved) {
			// 停止掉所有运行展示图片的任务，丢弃其他执行的任务。
			imageLoader.stop();
		}
		super.onDestroy();
	}
}
