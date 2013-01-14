package com.nostra13.example.universalimageloader;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nostra13.example.universalimageloader.Constants.Extra;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public class ImageListActivity extends BaseActivity {

	DisplayImageOptions options;

	String[] imageUrls;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ac_image_list);

		Bundle bundle = getIntent().getExtras();
		imageUrls = bundle.getStringArray(Extra.IMAGES);

		options = new DisplayImageOptions.Builder()
			 // 未加载完成前显示的默认图片
			.showStubImage(R.drawable.stub_image)
			 // URL为空时显示的默认图片
			.showImageForEmptyUri(R.drawable.image_for_empty_url)
			 // 加载图片时，图片被缓存到内存。
			.cacheInMemory()
			 // 加载图片时，图片被缓存到SD卡。
			.cacheOnDisc()
			 // 设置图片显示的样式，此为20像素的圆角图片
			.displayer(new RoundedBitmapDisplayer(20))
			 // 实例化DisplayImageOptions对象
			.build();

		ListView listView = (ListView) findViewById(android.R.id.list);
		listView.setAdapter(new ItemAdapter());
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				startImageGalleryActivity(position);
			}
		});

		listView.setOnScrollListener(new PauseOnScrollListener(false, true));
	}

	private void startImageGalleryActivity(int position) {
		Intent intent = new Intent(this, ImagePagerActivity.class);
		intent.putExtra(Extra.IMAGES, imageUrls);
		intent.putExtra(Extra.IMAGE_POSITION, position);
		startActivity(intent);
	}

	class ItemAdapter extends BaseAdapter {

		private class ViewHolder {
			public TextView text;
			public ImageView image;
		}

		@Override
		public int getCount() {
			return imageUrls.length;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View view = convertView;
			final ViewHolder holder;
			if (convertView == null) {
				view = getLayoutInflater().inflate(R.layout.item_list_image, null);
				holder = new ViewHolder();
				holder.text = (TextView) view.findViewById(R.id.text);
				holder.image = (ImageView) view.findViewById(R.id.image);
				view.setTag(holder);
			} else
				holder = (ViewHolder) view.getTag();

			holder.text.setText("Item " + position);
            // 使用ImageLoader为ImageView设置图片资源
			imageLoader.displayImage(imageUrls[position], holder.image, options);

			return view;
		}
	}
}