package com.yooiistudios.news.detail;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.PaletteItem;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Transition;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowInsets;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.yooiistudios.news.NLNewsApplication;
import com.yooiistudios.news.R;
import com.yooiistudios.news.main.NLMainActivity;
import com.yooiistudios.news.model.NLNews;
import com.yooiistudios.news.model.NLNewsFeed;
import com.yooiistudios.news.model.detail.NLDetailNewsAdapter;
import com.yooiistudios.news.model.detail.TransitionAdapter;
import com.yooiistudios.news.ui.itemanimator.NLDetailNewsItemAnimator;
import com.yooiistudios.news.ui.widget.ObservableScrollView;
import com.yooiistudios.news.ui.widget.recyclerview.DividerItemDecoration;
import com.yooiistudios.news.util.ImageMemoryCache;
import com.yooiistudios.news.util.dp.DipToPixel;
import com.yooiistudios.news.util.log.NLLog;
import com.yooiistudios.news.util.screen.NLScreenUtils;
import com.yooiistudios.news.util.web.NLWebUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class NLDetailActivity extends Activity
        implements NLDetailNewsAdapter.OnItemClickListener,
        ObservableScrollView.Callbacks, ImageLoader.ImageListener {
    @InjectView(R.id.detail_actionbar_overlay_view)         View mActionBarOverlayView;
    @InjectView(R.id.detail_top_overlay_view)               View mTopOverlayView;
    @InjectView(R.id.detail_scrollView)                     ObservableScrollView mScrollView;
    // Top
    @InjectView(R.id.detail_top_content_layout)             RelativeLayout mTopContentLayout;
    @InjectView(R.id.detail_top_news_image_ripple_view)     View mTopNewsImageRippleView;
    @InjectView(R.id.detail_top_news_image_view)            ImageView mTopImageView;
    @InjectView(R.id.detail_top_news_text_layout)           LinearLayout mTopNewsTextLayout;
    @InjectView(R.id.detail_top_news_text_ripple_layout)    LinearLayout mTopNewsTextRippleLayout;
    @InjectView(R.id.detail_top_news_title_text_view)       TextView mTopTitleTextView;
    @InjectView(R.id.detail_top_news_description_text_view) TextView mTopDescriptionTextView;
    // Bottom
    @InjectView(R.id.detail_bottom_news_recycler_view)      RecyclerView mBottomNewsListRecyclerView;

    private static final int BOTTOM_NEWS_ANIM_DELAY_UNIT_MILLI = 60;
    private static final String TAG = NLDetailActivity.class.getName();

    private Palette mPalette;

    private ImageLoader mImageLoader;

    private NLNewsFeed mNewsFeed;
    private NLNews mTopNews;
    private Bitmap mTopImageBitmap;
    private NLDetailNewsAdapter mAdapter;
//    private ArrayList<NLNews> mBottomNewsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detail);
        ButterKnife.inject(this);

        mImageLoader = new ImageLoader(((NLNewsApplication)getApplication()).getRequestQueue(),
                ImageMemoryCache.getInstance(getApplicationContext()));

        // retrieve feed from intent
        mNewsFeed = getIntent().getExtras().getParcelable(NLNewsFeed.KEY_NEWS_FEED);
        int topNewsIndex = getIntent().getExtras().getInt(NLNews.KEY_NEWS);
//        mBottomNewsList = new ArrayList<NLNews>(mNewsFeed.getNewsList());
        if (topNewsIndex < mNewsFeed.getNewsList().size()) {
            mTopNews = mNewsFeed.getNewsList().remove(topNewsIndex);
        }
//        Bitmap bitmap = getIntent().getExtras().getParcelable("bitmap");
//        NLLog.i(TAG, "bitmap : " + (bitmap != null ? "exists" : "null"));
        String imageViewName = getIntent().getExtras().getString(NLMainActivity
                .INTENT_KEY_VIEW_NAME_IMAGE, null);
//        String titleViewName = getIntent().getExtras().getString(NLMainActivity
//                .INTENT_KEY_VIEW_NAME_TITLE, null);

        // set view name to animate
        mTopImageView.setViewName(imageViewName);
//        mTopTitleTextView.setViewName(titleViewName);

        applySystemWindowsBottomInset(R.id.detail_scrollView);
        initActionBar();
        initCustomScrollView();
        initTopNews();
        initBottomNewsList();


        getWindow().getEnterTransition().addListener(new TransitionAdapter() {
            @Override
            public void onTransitionEnd(Transition transition) {
                ObjectAnimator color = ObjectAnimator.ofArgb(
                        mTopImageView.getColorFilter(), "color", 0);
                color.addUpdateListener(new ColorFilterListener(mTopImageView));
                color.setDuration(1000).start();

                getWindow().getEnterTransition().removeListener(this);
            }
        });
    }

    private void initActionBar() {
        initActionBarGradientView();

        if (getActionBar() != null && mNewsFeed != null) {
            getActionBar().setTitle(mNewsFeed.getTitle());
        }
    }

    private void initActionBarGradientView() {
        int actionBarSize = NLScreenUtils.calculateActionBarSize(this);
        int statusBarSize = 0;

        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarSize = getResources().getDimensionPixelSize(resourceId);
        }

        if (actionBarSize != 0) {
            mTopOverlayView.getLayoutParams().height = (actionBarSize + statusBarSize) * 2;
            mActionBarOverlayView.getLayoutParams().height = actionBarSize + statusBarSize;
        }
    }

    private void initCustomScrollView() {
        mScrollView.addCallbacks(this);
    }

    private void initTopNews() {
        mTopTitleTextView.setAlpha(0);
        mTopDescriptionTextView.setAlpha(0);

//        mTopNews = mNewsFeed.getNewsListContainsImageUrl().get(0);
        if (mTopNews != null) {
            loadTopNews();
//            if (mTopImageBitmap != null) {
//                colorize(mTopImageBitmap);
//            } else {
//                // TODO 이미지가 없을 경우 색상 처리
//            }
        } else {
            //TODO when NLNewsFeed is invalid.
        }
    }

    private void initBottomNewsList() {
        //init ui
        mBottomNewsListRecyclerView.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL_LIST));

        mBottomNewsListRecyclerView.setHasFixedSize(true);
        mBottomNewsListRecyclerView.setItemAnimator(
                new NLDetailNewsItemAnimator(mBottomNewsListRecyclerView));
        LinearLayoutManager layoutManager = new LinearLayoutManager
                (getApplicationContext());
        mBottomNewsListRecyclerView.setLayoutManager(layoutManager);

        mAdapter = new NLDetailNewsAdapter(this, this);

        mBottomNewsListRecyclerView.setAdapter(mAdapter);

        // make bottom news array list. EXCLUDE top news.
//        mBottomNewsList = new ArrayList<NLNews>(mNewsFeed.getNewsList());

        for (int i = 0; i < mNewsFeed.getNewsList().size(); i++) {
            final NLNews news = mNewsFeed.getNewsList().get(i);
            mBottomNewsListRecyclerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mAdapter.addNews(news);
                }
            }, BOTTOM_NEWS_ANIM_DELAY_UNIT_MILLI * i + 1);
        }

        adjustBottomRecyclerViewHeight();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finishAfterTransition();
        return super.onOptionsItemSelected(item);
    }

    private void adjustBottomRecyclerViewHeight() {
        LinearLayout.LayoutParams layoutParams =
                (LinearLayout.LayoutParams) mBottomNewsListRecyclerView.getLayoutParams();
        layoutParams.height = DipToPixel.dpToPixel(this, 100) * (mNewsFeed.getNewsList().size() - 1);
    }

    private void loadTopNews() {
//        final ImageMemoryCache cache = ImageMemoryCache.INSTANCE;
        final ImageMemoryCache cache = ImageMemoryCache.getInstance
                (getApplicationContext());

        // set title
        mTopTitleTextView.setText(mTopNews.getTitle());

        // set description
        if (mTopNews.getDescription() == null) {
            mTopDescriptionTextView.setVisibility(View.GONE);
        } else {
            mTopDescriptionTextView.setText(mTopNews.getDescription());

            // 타이틀 아래 패딩 조절
            mTopTitleTextView.setPadding(mTopTitleTextView.getPaddingLeft(),
                    mTopTitleTextView.getPaddingTop(), mTopTitleTextView.getPaddingRight(), 0);
        }

        // set image
        String imgUrl = mTopNews.getImageUrl();
        if (imgUrl != null) {
            ImageLoader.ImageContainer imageContainer =
                    mImageLoader.get(imgUrl, this);
            if ((mTopImageBitmap = imageContainer.getBitmap()) != null) {
                mTopImageView.setImageBitmap(mTopImageBitmap);
                colorize(mTopImageBitmap);
            }
        } else {
            // mTopImageBitmap
            //TODO 이미지 주소가 없을 경우 기본 이미지 보여주기
        }

        mTopNewsImageRippleView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                NLLog.now("mTopNewsImageRippleView onClink");
                NLWebUtils.openLink(NLDetailActivity.this, mTopNews.getLink());
            }
        });

        mTopNewsTextRippleLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                NLLog.now("mTopNewsTextRippleLayout onClink");
                NLWebUtils.openLink(NLDetailActivity.this, mTopNews.getLink());
            }
        });

        animateTopItems();
    }

    private void animateTopItems() {
        mTopTitleTextView.animate()
                .setStartDelay(450)
                .setDuration(650)
                .alpha(1f)
                .setInterpolator(new DecelerateInterpolator());
        mTopDescriptionTextView.animate()
                .setStartDelay(450)
                .setDuration(650)
                .alpha(1f)
                .setInterpolator(new DecelerateInterpolator());
    }

    private void colorize(Bitmap photo) {
        mPalette = Palette.generate(photo);
        applyPalette();
    }

    private void applyPalette() {
        // TODO 공식 문서가 release 된 후 palette.get~ 메서드가 null 을 반환할 가능성이 있는지 체크
        PaletteItem lightVibrantColor = mPalette.getLightVibrantColor();
        PaletteItem darkVibrantColor = mPalette.getDarkVibrantColor();

        mTopTitleTextView.setTextColor(Color.WHITE);

        if (lightVibrantColor != null) {
            mTopDescriptionTextView.setTextColor(lightVibrantColor.getRgb());
        }

        if (darkVibrantColor != null) {
//            getWindow().setBackgroundDrawable(new ColorDrawable(vibrantColor.getRgb()));
            int color = darkVibrantColor.getRgb();

            mTopContentLayout.setBackground(new ColorDrawable(color));
            mTopNewsTextLayout.setBackground(new ColorDrawable(color));

            int red = Color.red(color);
            int green = Color.green(color);
            int blue = Color.blue(color);
            int alpha = getResources().getInteger(R.integer.vibrant_color_tint_alpha);
            mTopImageView.setColorFilter(Color.argb(alpha, red, green, blue));
        }

        PaletteItem paletteItem = mPalette.getVibrantColor();
        if (paletteItem != null) {
            int vibrantColor = paletteItem.getRgb();
            int red = Color.red(vibrantColor);
            int green = Color.green(vibrantColor);
            int blue = Color.blue(vibrantColor);
            int alpha = getResources().getInteger(R.integer.vibrant_color_tint_alpha);
            mTopImageView.setColorFilter(Color.argb(
                    alpha, red, green, blue));
        }
    }

    @Override
    public void onItemClick(NLDetailNewsAdapter.ViewHolder viewHolder, NLNews news) {
        NLLog.now("detail bottom onItemClick");
//        NLWebUtils.openLink(this, news.getLink());
    }

    private void applySystemWindowsBottomInset(int container) {
        View containerView = findViewById(container);
        containerView.setFitsSystemWindows(true);
        containerView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                if (metrics.widthPixels < metrics.heightPixels) {
                    view.setPadding(0, 0, 0, windowInsets.getSystemWindowInsetBottom());
                } else {
                    view.setPadding(0, 0, windowInsets.getSystemWindowInsetRight(), 0);
                }
                return windowInsets.consumeSystemWindowInsets();
            }
        });
    }

    /**
     * Custom Scrolling
     */

    @Override
    public void onScrollChanged(int deltaX, int deltaY) {
        // Reposition the header bar -- it's normally anchored to the top of the content,
        // but locks to the top of the screen on scroll
        int scrollY = mScrollView.getScrollY();

        // Move background photo (parallax effect)
        if (scrollY >= 0) {
            mTopImageView.setTranslationY(scrollY * 0.4f);

            mActionBarOverlayView.setAlpha(scrollY * 0.0005f);
            if (mActionBarOverlayView.getAlpha() >= 0.6f) {
                mActionBarOverlayView.setAlpha(0.6f);
            }
        } else {
            mTopImageView.setTranslationY(0);
            if (mActionBarOverlayView.getAlpha() != 0) {
                mActionBarOverlayView.setAlpha(0);
            }
        }
    }

    @Override
    public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
//                    mTopImageBitmap = cache.getBitmapFromUrl(imgUrl);
//        if (mTopImageBitmap == null) {
//            mTopImageBitmap = response.getBitmap();
//
//            mTopImageView.setImageBitmap(mTopImageBitmap);
//            colorize(mTopImageBitmap);
//        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {

    }

    private static class ColorFilterListener implements ValueAnimator.AnimatorUpdateListener {
        private final ImageView mHeroImageView;

        public ColorFilterListener(ImageView hero) {
            mHeroImageView = hero;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            mHeroImageView.getDrawable().setColorFilter(mHeroImageView.getColorFilter());
        }
    }
}
