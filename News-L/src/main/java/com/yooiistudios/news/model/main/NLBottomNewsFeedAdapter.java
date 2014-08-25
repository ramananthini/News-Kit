package com.yooiistudios.news.model.main;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.PaletteItem;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.yooiistudios.news.R;
import com.yooiistudios.news.main.NLMainActivity;
import com.yooiistudios.news.model.news.NLNews;
import com.yooiistudios.news.model.news.NLNewsFeed;
import com.yooiistudios.news.util.ImageMemoryCache;
import com.yooiistudios.news.util.log.NLLog;

import java.util.ArrayList;

/**
 * Created by Dongheyon Jeong on in News-Android-L from Yooii Studios Co., LTD. on 2014. 8. 19.
 *
 * NLBottomNewsFeedAdapter
 *  메인 화면 하단 뉴스피드 리스트의 RecyclerView에 쓰일 어뎁터
 */
public class NLBottomNewsFeedAdapter extends
        RecyclerView.Adapter<NLBottomNewsFeedAdapter
                .NLBottomNewsFeedViewHolder> {
    private static final String TAG = NLBottomNewsFeedAdapter.class.getName();
    private static final String VIEW_NAME_POSTFIX = "_bottom_";

    private Context mContext;
    private ArrayList<NLNewsFeed> mNewsFeedList;
    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        public void onBottomItemClick(
                NLBottomNewsFeedAdapter.NLBottomNewsFeedViewHolder
                        viewHolder, NLNewsFeed newsFeed);
    }

    public NLBottomNewsFeedAdapter(Context context, OnItemClickListener
                                   listener) {
        mContext = context;
        mNewsFeedList = new ArrayList<NLNewsFeed>();
        mOnItemClickListener = listener;
    }

    @Override
    public NLBottomNewsFeedViewHolder onCreateViewHolder(ViewGroup parent,
                                                         int i) {
        Context context = parent.getContext();
        View v = LayoutInflater.from(context).inflate(
                R.layout.main_bottom_item, parent, false);
//        v.setElevation(DipToPixel.dpToPixel(context,
//                context.getResources().getDimension(
//                        R.dimen.main_bottom_card_view_elevation)
//        ));
//        ((ViewGroup)v).setTransitionGroup(false);

        NLBottomNewsFeedViewHolder viewHolder =
                new NLBottomNewsFeedViewHolder(v);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final NLBottomNewsFeedViewHolder viewHolder,
            final int position) {
        TextView titleView = viewHolder.newsTitleTextView;
        ImageView imageView = viewHolder.imageView;
        TextView newsFeedTitleView = viewHolder.newsFeedTitleTextView;
        ArrayList<NLNews> newsList = mNewsFeedList.get(position).getNewsList();
        NLNews displayingNews = newsList.get(0);

        titleView.setText(displayingNews.getTitle());
        titleView.setViewName(NLMainActivity.VIEW_NAME_TITLE_PREFIX +
                VIEW_NAME_POSTFIX + position);

        imageView.setBackgroundColor(Color.argb(200, 16, 16, 16));
        imageView.setImageDrawable(new ColorDrawable(Color.argb(200, 16, 16, 16)));
        imageView.setViewName(NLMainActivity.VIEW_NAME_IMAGE_PREFIX +
                VIEW_NAME_POSTFIX + position);

        newsFeedTitleView.setText(mNewsFeedList.get(position).getTitle());


        viewHolder.progressBar.setVisibility(View.VISIBLE);

        String imageUrl;
        if (newsList.size() > 0 &&
                (imageUrl = displayingNews.getImageUrl()) != null) {

            ImageLoader imageLoader = new ImageLoader(Volley.newRequestQueue
                    (mContext), ImageMemoryCache.getInstance(mContext));

            imageLoader.get(imageUrl, new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                    NLLog.i(TAG, "onResponse\nposition : " + position);

                    Bitmap bitmap = response.getBitmap();

                    if (bitmap != null) {
                        viewHolder.imageView.setImageBitmap(bitmap);
                        Palette palette = Palette.generate(bitmap);
                        PaletteItem paletteItem = palette.getDarkVibrantColor();
                        if (paletteItem != null) {
                            int darkVibrantColor = paletteItem.getRgb();
                            int red = Color.red(darkVibrantColor);
                            int green = Color.green(darkVibrantColor);
                            int blue = Color.blue(darkVibrantColor);
                            int alpha = mContext.getResources().getInteger(
                                    R.integer.vibrant_color_tint_alpha);
                            viewHolder.imageView.setColorFilter(Color.argb(
                                    alpha, red, green, blue));
                        }
                    }
                    viewHolder.progressBar.setVisibility(View.GONE);
//                    viewHolder.imageView.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
                }

                @Override
                public void onErrorResponse(VolleyError error) {

                }
            });
        }

        viewHolder.itemView.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    NLNewsFeed newsFeed = mNewsFeedList.get(position);

                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onBottomItemClick(viewHolder, newsFeed);
                    }
                }
            }
        );
    }

    @Override
    public int getItemCount() {
        return mNewsFeedList.size();
    }

    public void addNewsFeed(NLNewsFeed newsFeed) {
        mNewsFeedList.add(newsFeed);
        notifyItemInserted(mNewsFeedList.size() - 1);
    }

    public void setImageUrlAt(String imageUrl, int position) {
    }

//    @Override
//    public void onClick(View view) {
//        int position = ((Integer)view.getTag(KEY_INDEX));
//        NLBottomNewsFeedViewHolder viewHolder = (NLBottomNewsFeedViewHolder)
//                view.getTag(KEY_VIEW_HOLDER);
//        NLNewsFeed newsFeed = mNewsFeedList.get(position);
//
//        if (mOnItemClickListener != null) {
//            mOnItemClickListener.onItemClick(viewHolder, newsFeed);
//        }
//    }

    public static class NLBottomNewsFeedViewHolder extends RecyclerView
            .ViewHolder {

        public TextView newsTitleTextView;
        public ImageView imageView;
        public ProgressBar progressBar;
        public TextView newsFeedTitleTextView;

        public NLBottomNewsFeedViewHolder(View itemView) {
            super(itemView);
            newsTitleTextView = (TextView) itemView.findViewById(R.id.main_bottom_item_title);
            imageView = (ImageView) itemView.findViewById(R.id.main_bottom_item_image_view);
            progressBar = (ProgressBar) itemView.findViewById(R.id.main_bottom_item_progress);
            newsFeedTitleTextView = (TextView) itemView.findViewById(R.id.main_bottom_news_feed_title);
        }

    }
}
