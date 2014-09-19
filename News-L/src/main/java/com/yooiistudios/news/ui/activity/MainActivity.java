package com.yooiistudios.news.ui.activity;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.antonioleiva.recyclerviewextensions.GridLayoutManager;
import com.viewpagerindicator.CirclePageIndicator;
import com.yooiistudios.news.NewsApplication;
import com.yooiistudios.news.R;
import com.yooiistudios.news.model.news.News;
import com.yooiistudios.news.model.news.NewsFeed;
import com.yooiistudios.news.model.news.NewsFeedArchiveUtils;
import com.yooiistudios.news.model.news.NewsFeedUrl;
import com.yooiistudios.news.model.news.TintType;
import com.yooiistudios.news.model.news.task.BottomNewsFeedFetchTask;
import com.yooiistudios.news.model.news.task.BottomNewsImageUrlFetchTask;
import com.yooiistudios.news.model.news.task.TopFeedNewsImageUrlFetchTask;
import com.yooiistudios.news.model.news.task.TopNewsFeedFetchTask;
import com.yooiistudios.news.ui.adapter.MainBottomAdapter;
import com.yooiistudios.news.ui.adapter.MainTopPagerAdapter;
import com.yooiistudios.news.ui.itemanimator.SlideInFromBottomItemAnimator;
import com.yooiistudios.news.ui.widget.MainRefreshLayout;
import com.yooiistudios.news.ui.widget.viewpager.SlowSpeedScroller;
import com.yooiistudios.news.util.FeedbackUtils;
import com.yooiistudios.news.util.ImageMemoryCache;
import com.yooiistudios.news.util.NLLog;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends Activity
        implements TopFeedNewsImageUrlFetchTask.OnTopFeedImageUrlFetchListener,
        TopNewsFeedFetchTask.OnFetchListener,
        BottomNewsFeedFetchTask.OnFetchListener,
        MainBottomAdapter.OnItemClickListener,
        BottomNewsImageUrlFetchTask.OnBottomImageUrlFetchListener,
        RecyclerView.ItemAnimator.ItemAnimatorFinishedListener {

    @InjectView(R.id.main_top_view_pager)           ViewPager mTopNewsFeedViewPager;
    @InjectView(R.id.main_top_view_pager_wrapper)   FrameLayout mTopNewsFeedViewPagerWrapper;
    @InjectView(R.id.main_top_unavailable_wrapper)  FrameLayout mTopNewsFeedUnavailableWrapper;
    @InjectView(R.id.main_top_page_indicator)       CirclePageIndicator mTopViewPagerIndicator;
    @InjectView(R.id.main_top_news_feed_title_text_view) TextView mTopNewsFeedTitleTextView;
    @InjectView(R.id.bottomNewsFeedRecyclerView)    RecyclerView mBottomNewsFeedRecyclerView;
    @InjectView(R.id.main_loading_container)        ViewGroup mLoadingContainer;
    @InjectView(R.id.main_loading_log)              TextView mLoadingLog;
    @InjectView(R.id.main_scrolling_content)        View mScrollingContent;
    @InjectView(R.id.main_swipe_refresh_layout)     MainRefreshLayout mSwipeRefreshLayout;

    private static final String TAG = MainActivity.class.getName();
    public static final String VIEW_NAME_IMAGE_PREFIX = "topImage_";
    public static final String VIEW_NAME_TITLE_PREFIX = "topTitle_";
    public static final String INTENT_KEY_VIEW_NAME_IMAGE = "INTENT_KEY_VIEW_NAME_IMAGE";
    public static final String INTENT_KEY_VIEW_NAME_TITLE = "INTENT_KEY_VIEW_NAME_TITLE";
    public static final String INTENT_KEY_TINT_TYPE = "INTENT_KEY_TINT_TYPE";
    private static final int BOTTOM_NEWS_FEED_ANIM_DELAY_UNIT_MILLI = 60;
    private static final int BOTTOM_NEWS_FEED_COLUMN_COUNT = 2;

    // 뉴스 새로고침시 사용할 인텐트 변수
    public static final String INTENT_KEY_NEWS_FEED_LOCATION = "INTENT_KEY_NEWS_FEED_LOCATION";
    public static final String INTENT_VALUE_TOP_NEWS_FEED = "INTENT_VALUE_TOP_NEWS_FEED";
    public static final String INTENT_VALUE_BOTTOM_NEWS_FEED = "INTENT_VALUE_BOTTOM_NEWS_FEED";
    public static final String INTENT_KEY_BOTTOM_NEWS_FEED_INDEX =
                                                            "INTENT_KEY_BOTTOM_NEWS_FEED_INDEX";
    private static final int RC_NEWS_FEED_DETAIL = 10001;

    private ImageLoader mImageLoader;

    private NewsFeed mTopNewsFeed;
    private ArrayList<NewsFeed> mBottomNewsFeedList;

    private TopFeedNewsImageUrlFetchTask mTopImageUrlFetchTask;
    private TopNewsFeedFetchTask mTopNewsFeedFetchTask;
    private SparseArray<BottomNewsFeedFetchTask> mBottomNewsFeedIndexToNewsFetchTaskMap;
    private HashMap<News, BottomNewsImageUrlFetchTask> mBottomNewsFeedNewsToImageTaskMap;
    private HashMap<News, TopFeedNewsImageUrlFetchTask> mTopNewsFeedNewsToImageTaskMap;
    private MainBottomAdapter mBottomNewsFeedAdapter;
    private MainTopPagerAdapter mTopNewsFeedPagerAdapter;

    private SlideInFromBottomItemAnimator mItemAnimator;

    // flags for initializing
    private boolean mTopNewsFeedReady = false;
    private boolean mTopNewsFeedFirstImageReady = false;
    private boolean mBottomNewsFeedReady = false;

    //
    private boolean mIsRefreshingTopNewsFeed = false;
    private boolean mIsRefreshingBottomNewsFeeds = false;

    /**
     * Auto Refresh Handler
     */
    // auto refresh handler
    private static final int AUTO_REFRESH_HANDLER_DELAY = 5 * 1000; // 10 secs
    private boolean mIsHandlerRunning = false;
    private NewsAutoRefreshHandler mNewsAutoRefreshHandler = new NewsAutoRefreshHandler();
    private class NewsAutoRefreshHandler extends Handler {
        @Override
        public void handleMessage( Message msg ){
            // 갱신
//            NLLog.now("newsAutoRefresh");
            autoRefreshTopNewsFeed();
            autoRefreshBottomNewsFeeds();

            // tick 의 동작 시간을 계산해서 정확히 틱 초마다 UI 갱신을 요청할 수 있게 구현
            mNewsAutoRefreshHandler.sendEmptyMessageDelayed(0,
                    AUTO_REFRESH_HANDLER_DELAY);
        }
    }

    private void autoRefreshTopNewsFeed() {
        if (mTopNewsFeedViewPager.getCurrentItem() + 1 < mTopNewsFeedViewPager.getAdapter().getCount()) {
            mTopNewsFeedViewPager.setCurrentItem(mTopNewsFeedViewPager.getCurrentItem() + 1, true);
        } else {
            mTopNewsFeedViewPager.setCurrentItem(0, true);
        }
    }

    private void autoRefreshBottomNewsFeeds() {

    }

    private void startNewsAutoRefresh() {
        if (mIsHandlerRunning) {
            return;
        }
        mIsHandlerRunning = true;
        mNewsAutoRefreshHandler.sendEmptyMessageDelayed(0, AUTO_REFRESH_HANDLER_DELAY);
    }

    private void stopNewsAutoRefresh() {
        if (!mIsHandlerRunning) {
            return;
        }
        mIsHandlerRunning = false;
        mNewsAutoRefreshHandler.removeMessages(0);
    }

    /**
     * Auto Refresh Handler End
     */

    @Override
    protected void onResume() {
        super.onResume();
        if (mTopNewsFeedReady && mBottomNewsFeedReady) {
            startNewsAutoRefresh();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopNewsAutoRefresh();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

//        mImageLoader = new ImageLoader(Volley.newRequestQueue(this), ImageMemoryCache.INSTANCE);
        mImageLoader = new ImageLoader(((NewsApplication)getApplication()).getRequestQueue(),
                ImageMemoryCache.getInstance(getApplicationContext()));

        boolean needsRefresh = NewsFeedArchiveUtils.newsNeedsToBeRefreshed(getApplicationContext());

        // TODO off-line configuration
        // TODO ConcurrentModification 문제 우회를 위해 애니메이션이 끝나기 전 스크롤을 막던지 처리 해야함.
        initRefreshLayout();
        initTopNewsFeed(needsRefresh);
        initBottomNewsFeed(needsRefresh);
        showMainContentIfReady();

        applySystemWindowsBottomInset(mScrollingContent);
    }
    //

    private void applySystemWindowsBottomInset(View containerView) {
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

    private void initRefreshLayout() {
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.refresh_color_scheme_1, R.color.refresh_color_scheme_2,
                R.color.refresh_color_scheme_3, R.color.refresh_color_scheme_4);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                NLLog.i(TAG, "onRefresh called from SwipeRefreshLayout");
                if (!mIsRefreshingTopNewsFeed && !mIsRefreshingBottomNewsFeeds) {
                    mSwipeRefreshLayout.setEnabled(false);
                    mIsRefreshingTopNewsFeed = true;
                    mIsRefreshingBottomNewsFeeds = true;
                    refreshTopNewsFeed();
                    refreshBottomNewsFeeds();

                    // 기존 뉴스 삭제 후 뉴스피드 새로 로딩
//                    NewsFeedArchiveUtils.clearArchive(getApplicationContext());
//                    initTopNewsFeed(false);
//                    initBottomNewsFeed(false);
                }
            }
        });

        // 초기 로딩시 swipe refresh가 되지 않도록 설정
        mSwipeRefreshLayout.setEnabled(false);
    }

    private void initTopNewsFeed(boolean refresh) {
        // Dialog
//        mDialog = ProgressDialog.show(this, getString(R.string.splash_loading_title),
//                getString(R.string.splash_loading_description));

        Context context = getApplicationContext();

        // ViewPager
        try {
            Field mScroller;
            mScroller = ViewPager.class.getDeclaredField("mScroller");
            mScroller.setAccessible(true);
            SlowSpeedScroller scroller = new SlowSpeedScroller(this,
                    new AccelerateDecelerateInterpolator(this, null), true);
            mScroller.set(mTopNewsFeedViewPager, scroller);
        } catch (NoSuchFieldException ignored) {
        } catch (IllegalArgumentException ignored) {
        } catch (IllegalAccessException ignored) {
        }

        // Fetch
        mTopNewsFeed = NewsFeedArchiveUtils.loadTopNewsFeed(context);
        if (refresh) {
            mTopNewsFeedReady = false;
            fetchTopNewsFeed(this);
        } else {
            if (mTopNewsFeed.isValid()) {
                notifyNewTopNewsFeedSet();
            } else {
                mTopNewsFeedReady = false;
                fetchTopNewsFeed(this);
            }
        }

    }
    private void fetchTopNewsFeed(TopNewsFeedFetchTask.OnFetchListener listener) {
        mTopNewsFeedFetchTask = new TopNewsFeedFetchTask(this, mTopNewsFeed.getNewsFeedUrl(), listener);
        mTopNewsFeedFetchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void notifyNewTopNewsFeedSet() {
        // show view pager wrapper
        mTopNewsFeedViewPagerWrapper.setVisibility(View.VISIBLE);
        mTopNewsFeedUnavailableWrapper.setVisibility(View.GONE);
        mTopViewPagerIndicator.setVisibility(View.VISIBLE);

        mTopNewsFeedReady = true;
        ArrayList<News> items = mTopNewsFeed.getNewsList();
        mTopNewsFeedPagerAdapter = new MainTopPagerAdapter(getFragmentManager(), mTopNewsFeed);

        mTopNewsFeedViewPager.setAdapter(mTopNewsFeedPagerAdapter);
        mTopViewPagerIndicator.setViewPager(mTopNewsFeedViewPager);
//        mTopViewPagerIndicator.setCurrentItem(0);

        if (items.size() > 0) {
            News news = items.get(0);

            if (news.getImageUrl() == null && !news.isImageUrlChecked()) {
                mTopNewsFeedFirstImageReady = false;
                mTopImageUrlFetchTask = new TopFeedNewsImageUrlFetchTask(news, 0, this);
                mTopImageUrlFetchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                mTopNewsFeedFirstImageReady = true;
                fetchTopNewsFeedImages();
            }
        } else {
            mTopNewsFeedFirstImageReady = true;
            fetchTopNewsFeedImages();
        }

        mTopNewsFeedTitleTextView.setText(mTopNewsFeed.getTitle());
    }
    private void showTopNewsFeedUnavailable() {
        // show top unavailable wrapper
        mTopNewsFeedViewPagerWrapper.setVisibility(View.GONE);
        mTopNewsFeedUnavailableWrapper.setVisibility(View.VISIBLE);
        mTopViewPagerIndicator.setVisibility(View.INVISIBLE);

        mTopNewsFeed = null;
        mTopNewsFeedReady = true;

        mTopNewsFeedFirstImageReady = true;
    }

    private void initBottomNewsFeed(boolean refresh) {
        mBottomNewsFeedReady = false;

        Context context = getApplicationContext();
        //init ui
        mBottomNewsFeedRecyclerView.setHasFixedSize(true);
//        ((ViewGroup)mBottomNewsFeedRecyclerView).setTransitionGroup(false);
        mItemAnimator = new SlideInFromBottomItemAnimator(
                mBottomNewsFeedRecyclerView);
        mBottomNewsFeedRecyclerView.setItemAnimator(mItemAnimator);
        GridLayoutManager layoutManager = new GridLayoutManager(context);
        layoutManager.setColumns(BOTTOM_NEWS_FEED_COLUMN_COUNT);
        mBottomNewsFeedRecyclerView.setLayoutManager(layoutManager);

        mBottomNewsFeedList = NewsFeedArchiveUtils.loadBottomNews(context);
//        mBottomNewsFeedAdapter.resetDisplayingNewsFeedIndices();

        if (refresh) {
            fetchBottomNewsFeedList(this);
        } else {
            boolean isValid = true;
            for (NewsFeed newsFeed : mBottomNewsFeedList) {
                if (!newsFeed.isValid()) {
                    isValid = false;
                    break;
                }
            }
            if (isValid) {
                mBottomNewsFeedReady = true;
                showMainContentIfReady();
            } else {
                fetchBottomNewsFeedList(this);
            }
        }

        // 메인 하단의 뉴스피드 RecyclerView의 높이를 set
        ViewGroup.LayoutParams recyclerViewLp = mBottomNewsFeedRecyclerView.getLayoutParams();
        recyclerViewLp.height = MainBottomAdapter.measureMaximumHeight(getApplicationContext(),
                mBottomNewsFeedList.size(), BOTTOM_NEWS_FEED_COLUMN_COUNT);

    }

    private void fetchBottomNewsFeedList(BottomNewsFeedFetchTask.OnFetchListener listener) {
        final int bottomNewsCount = mBottomNewsFeedList.size();

        mBottomNewsFeedIndexToNewsFetchTaskMap = new SparseArray<BottomNewsFeedFetchTask>();
        for (int i = 0; i < bottomNewsCount; i++) {
            NewsFeedUrl url = mBottomNewsFeedList.get(i).getNewsFeedUrl();
            BottomNewsFeedFetchTask task = new BottomNewsFeedFetchTask(
                    getApplicationContext(), url, i, listener
            );
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            mBottomNewsFeedIndexToNewsFetchTaskMap.put(i, task);
        }
    }

    private void fetchBottomNewsFeedListImage() {
        mBottomNewsFeedNewsToImageTaskMap = new
                HashMap<News, BottomNewsImageUrlFetchTask>();

        for (int i = 0; i < mBottomNewsFeedList.size(); i++) {
            NewsFeed feed = mBottomNewsFeedList.get(i);

            // IndexOutOfBoundException 방지
            int newsIndex = i < mBottomNewsFeedAdapter.getDisplayingNewsFeedIndices().size() ?
                    mBottomNewsFeedAdapter.getDisplayingNewsFeedIndices().get(i) : 0;

            ArrayList<News> newsList = feed.getNewsList();
            if (newsList.size() > 0) {
                // IndexOutOfBoundException 방지
                News news = newsIndex < newsList.size() ? newsList.get(newsIndex) : newsList.get(0);

                BottomNewsImageUrlFetchTask task = new BottomNewsImageUrlFetchTask(news, i, this);
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                mBottomNewsFeedNewsToImageTaskMap.put(news, task);
            }
        }
    }

    private void fetchTopNewsFeedImages() {
        if (mTopNewsFeed == null) {
            return;
        }
        mTopNewsFeedNewsToImageTaskMap = new HashMap<News, TopFeedNewsImageUrlFetchTask>();

        ArrayList<News> newsList = mTopNewsFeed.getNewsList();

        for (int i = 0; i < newsList.size(); i++) {
            News news = newsList.get(i);

            if (news.getImageUrl() == null && !news.isImageUrlChecked()) {
                TopFeedNewsImageUrlFetchTask task = new
                        TopFeedNewsImageUrlFetchTask(news, i, this);
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                mTopNewsFeedNewsToImageTaskMap.put(news, task);
            }
        }
    }
    private void cancelTopNewsFeedImageFetchTasks() {
        mTopNewsFeedReady = false;
        for (Map.Entry<News, TopFeedNewsImageUrlFetchTask> entry :
                mTopNewsFeedNewsToImageTaskMap.entrySet()) {
            TopFeedNewsImageUrlFetchTask task = entry.getValue();
            if (task != null) {
                task.cancel(true);
            }
        }
        mTopNewsFeedNewsToImageTaskMap.clear();
    }

    private void cancelBottomNewsFetchTasks() {
        mBottomNewsFeedReady = false;
        int taskCount = mBottomNewsFeedIndexToNewsFetchTaskMap.size();
        for (int i = 0; i < taskCount; i++) {
            BottomNewsFeedFetchTask task = mBottomNewsFeedIndexToNewsFetchTaskMap
                    .get(i, null);
            if (task != null) {
                task.cancel(true);
            }
        }
        mBottomNewsFeedIndexToNewsFetchTaskMap.clear();

        for (Map.Entry<News, BottomNewsImageUrlFetchTask> entry :
                mBottomNewsFeedNewsToImageTaskMap.entrySet()) {
            BottomNewsImageUrlFetchTask task = entry.getValue();
            if (task != null) {
                task.cancel(true);
            }
        }
        mBottomNewsFeedNewsToImageTaskMap.clear();
    }
    private void animateBottomNewsFeedListOnInit() {
        mBottomNewsFeedReady = true;
        mBottomNewsFeedAdapter = new MainBottomAdapter
                (getApplicationContext(), this);
        mBottomNewsFeedRecyclerView.setAdapter(mBottomNewsFeedAdapter);

        for (int i = 0; i < mBottomNewsFeedList.size(); i++) {
            final NewsFeed newsFeed = mBottomNewsFeedList.get(i);
            final int idx = i;
            mBottomNewsFeedRecyclerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBottomNewsFeedAdapter.addNewsFeed(newsFeed);

                    if (idx == (mBottomNewsFeedList.size() - 1)) {
                        mItemAnimator.isRunning(MainActivity.this);
                    }
                }
            }, BOTTOM_NEWS_FEED_ANIM_DELAY_UNIT_MILLI * i + 1);

        }
    }

    private void showMainContentIfReady() {
        showMainContentIfReady(false);
    }
    private void showMainContentIfReady(boolean noTopNewsImage) {
        NLLog.i("showMainContentIfReady", "mTopNewsFeedReady : " + mTopNewsFeedReady);
        NLLog.i("showMainContentIfReady", "mBottomNewsFeedReady : " + mBottomNewsFeedReady);
        NLLog.i("showMainContentIfReady", "mTopNewsFeedFirstImageReady : " + mTopNewsFeedFirstImageReady);
        NLLog.i("showMainContentIfReady", "noTopNewsImage : " + noTopNewsImage);

        String loadingStatus = "Top news ready : " + mTopNewsFeedReady
                + "\nTop news first image ready : "
                + (noTopNewsImage ? "NO IMAGE!!!" : mTopNewsFeedFirstImageReady)
                + "\nBottom news feed ready : " + mBottomNewsFeedReady;

        mLoadingLog.setText(loadingStatus);

        if (mLoadingContainer.getVisibility() == View.GONE) {
            return;
        }

        if (mTopNewsFeedReady && mBottomNewsFeedReady) {
            if (noTopNewsImage || mTopNewsFeedFirstImageReady) {
                mSwipeRefreshLayout.setRefreshing(false);

                // 
                mSwipeRefreshLayout.setEnabled(true);

                NewsFeedArchiveUtils.save(getApplicationContext(), mTopNewsFeed,
                        mBottomNewsFeedList);
                animateBottomNewsFeedListOnInit();

                // loaded
                mLoadingContainer.setVisibility(View.GONE);

                // 메인 화면 로딩 후에 오토 리프레시 핸들러를 시작
                startNewsAutoRefresh();
            }
        }
    }

    private void refreshTopNewsFeed() {
        NewsFeedUrl topNewsFeedUrl = mTopNewsFeed.getNewsFeedUrl();
        mTopNewsFeed = new NewsFeed();
        mTopNewsFeed.setNewsFeedUrl(topNewsFeedUrl);

        for (int i = 0; i < TopNewsFeedFetchTask.FETCH_COUNT; i++) {
            mTopNewsFeed.getNewsList().add(null);
        }

        mTopViewPagerIndicator.setCurrentItem(0);
        mTopNewsFeedPagerAdapter = new MainTopPagerAdapter(getFragmentManager(), mTopNewsFeed);
        mTopNewsFeedViewPager.setAdapter(mTopNewsFeedPagerAdapter);
        mTopViewPagerIndicator.setViewPager(mTopNewsFeedViewPager);
        mTopNewsFeedTitleTextView.setText("");

        fetchTopNewsFeed(mOnTopNewsFeedRefreshedListener);
    }

    private void refreshBottomNewsFeeds() {
        ArrayList<NewsFeed> newBottomNewsFeedList = new ArrayList<NewsFeed>();
        for (NewsFeed newsFeed : mBottomNewsFeedList) {
            NewsFeed newNewsFeed = new NewsFeed();
            newNewsFeed.setNewsFeedUrl(newsFeed.getNewsFeedUrl());

            newBottomNewsFeedList.add(newNewsFeed);
        }
        mBottomNewsFeedList = newBottomNewsFeedList;

        // 프로그레스바를 나타내기 위해 NewsFeedUrl만 가지고 있는 뉴스피드를 넣음
        mBottomNewsFeedAdapter.setNewsFeedList(mBottomNewsFeedList);

        mBottomNewsFeedAdapter.resetDisplayingNewsFeedIndices();

        fetchBottomNewsFeedList(mOnBottomNewsFeedListRefreshedListener);
    }

    private TopNewsFeedFetchTask.OnFetchListener mOnTopNewsFeedRefreshedListener
            = new TopNewsFeedFetchTask.OnFetchListener() {

        @Override
        public void onTopNewsFeedFetchSuccess(NewsFeed newsFeed) {
            mIsRefreshingTopNewsFeed = false;
            mTopNewsFeed = newsFeed;
            notifyNewTopNewsFeedSet();

            configAfterRefreshDone();
        }

        @Override
        public void onTopNewsFeedFetchFail() {
            mIsRefreshingTopNewsFeed = false;
            showTopNewsFeedUnavailable();

            configAfterRefreshDone();
        }
    };
    private BottomNewsFeedFetchTask.OnFetchListener mOnBottomNewsFeedListRefreshedListener
             = new BottomNewsFeedFetchTask.OnFetchListener() {

        @Override
        public void onBottomNewsFeedFetchSuccess(int position, NewsFeed newsFeed) {
//            mBottomNewsFeedAdapter.replaceNewsFeedAt(position, newsFeed);
            mBottomNewsFeedList.set(position, newsFeed);
            mBottomNewsFeedIndexToNewsFetchTaskMap.remove(position);

            checkAllBottomNewsFeedFetched();
        }

        @Override
        public void onBottomNewsFeedFetchFail(int position) {
            mBottomNewsFeedIndexToNewsFetchTaskMap.remove(position);

            checkAllBottomNewsFeedFetched();
            // TODO initialize 리스너 참조.
        }

        private void checkAllBottomNewsFeedFetched() {
            int remainingTaskCount = mBottomNewsFeedIndexToNewsFetchTaskMap.size();

            if (remainingTaskCount == 0) {
                mIsRefreshingBottomNewsFeeds = false;
                configAfterRefreshDone();

                mBottomNewsFeedAdapter.setNewsFeedList(mBottomNewsFeedList);
                fetchBottomNewsFeedListImage();
            }
        }
    };
    private BottomNewsFeedFetchTask.OnFetchListener mOnBottomNewsFeedFetchListener
            = new BottomNewsFeedFetchTask.OnFetchListener() {

        @Override
        public void onBottomNewsFeedFetchSuccess(int position, NewsFeed newsFeed) {
            mBottomNewsFeedAdapter.replaceNewsFeedAt(position, newsFeed);
        }

        @Override
        public void onBottomNewsFeedFetchFail(int position) {

        }
    };

    private void configAfterRefreshDone() {
        if (!mIsRefreshingTopNewsFeed && !mIsRefreshingBottomNewsFeeds) {
            // dismiss loading progress bar
            mSwipeRefreshLayout.setRefreshing(false);
            mSwipeRefreshLayout.setEnabled(true);
            NewsFeedArchiveUtils.save(getApplicationContext(), mTopNewsFeed, mBottomNewsFeedList);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingActivity.class));
            return true;
        } else if (id == R.id.action_store) {
            startActivity(new Intent(MainActivity.this, StoreActivity.class));
            return true;
        } else if (id == R.id.action_send_feedback) {
            FeedbackUtils.sendFeedback(this);
            return true;
        } else if (id == R.id.action_remove_archive) {
            NewsFeedArchiveUtils.clearArchive(getApplicationContext());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTopFeedImageUrlFetchSuccess(News news, String url,
                                              final int position) {
        NLLog.i(TAG, "fetch image url success.");
        NLLog.i(TAG, "news link : " + news.getLink());
        NLLog.i(TAG, "image url : " + url);

        news.setImageUrlChecked(true);
        if (url == null) {
            fetchTopNewsFeedImages();
            showMainContentIfReady(true);
        }
        else {
            news.setImageUrl(url);

            mImageLoader.get(url, new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {

                    mTopNewsFeedPagerAdapter.notifyImageLoaded(position);

                    if (position == 0) {
                        mTopNewsFeedFirstImageReady = true;
                        fetchTopNewsFeedImages();
                        showMainContentIfReady();
                    }
                }

                @Override
                public void onErrorResponse(VolleyError error) {
                    if (position == 0) {
                        mTopNewsFeedFirstImageReady = true;
                        fetchTopNewsFeedImages();
                        showMainContentIfReady();
                    }
                }
            });
        }

        NewsFeedArchiveUtils.saveTopNewsFeed(getApplicationContext(), mTopNewsFeed);
    }

    @Override
    public void onTopFeedImageUrlFetchFail(News news, int position) {
        // TODO 여기로 들어올 경우 처리 하자!
        NLLog.i(TAG, "fetch image url failed.");
    }

    /**
     * TopNewsFeedFetch Listener
     */
    @Override
    public void onTopNewsFeedFetchSuccess(NewsFeed newsFeed) {
        NLLog.i(TAG, "onTopNewsFeedFetchSuccess");
//        if (mDialog != null) {
//            mDialog.dismiss();
//        }
        mTopNewsFeed = newsFeed;
        notifyNewTopNewsFeedSet();
        showMainContentIfReady();
    }

    @Override
    public void onTopNewsFeedFetchFail() {
        NLLog.i(TAG, "onTopNewsFeedFetchFail");
        showTopNewsFeedUnavailable();
        showMainContentIfReady();
    }

    @Override
    public void onBottomNewsFeedFetchSuccess(int position, NewsFeed newsFeed) {
        NLLog.i(TAG, "onBottomNewsFeedFetchSuccess");
        mBottomNewsFeedIndexToNewsFetchTaskMap.remove(position);
        mBottomNewsFeedList.set(position, newsFeed);

        int remainingTaskCount = mBottomNewsFeedIndexToNewsFetchTaskMap.size();
        if (remainingTaskCount == 0) {
            NLLog.i(TAG, "All task done. Loaded news feed list size : " +
                    mBottomNewsFeedList.size());
            mBottomNewsFeedReady = true;

            showMainContentIfReady();
        } else {
            NLLog.i(TAG, remainingTaskCount + " remaining tasks.");
        }
    }

    @Override
    public void onBottomNewsFeedFetchFail(int position) {
        NLLog.i(TAG, "onBottomNewsFeedFetchFail");
        // TODO Top news처럼 뉴스 없음 처리하고 notify 해줘야 함
        mBottomNewsFeedReady = true;

        showMainContentIfReady();
    }

    @Override
    public void onBottomItemClick(MainBottomAdapter.BottomNewsFeedViewHolder viewHolder,
                                  NewsFeed newsFeed, int position) {
        NLLog.i(TAG, "onBottomItemClick");
        NLLog.i(TAG, "newsFeed : " + newsFeed.getTitle());

        ImageView imageView = viewHolder.imageView;
        TextView titleView = viewHolder.newsTitleTextView;


        ActivityOptions activityOptions =
                ActivityOptions.makeSceneTransitionAnimation(
                        MainActivity.this,
                        new Pair<View, String>(imageView, imageView.getViewName()),
                        new Pair<View, String>(titleView, titleView.getViewName())
                );
//        ActivityOptions activityOptions2 = ActivityOptions.
//                makeSceneTransitionAnimation(NLMainActivity.this,
//                        imageView, imageView.getViewName());

        Intent intent = new Intent(MainActivity.this,
                NewsFeedDetailActivity.class);
        intent.putExtra(NewsFeed.KEY_NEWS_FEED, newsFeed);
        intent.putExtra(News.KEY_NEWS,
                mBottomNewsFeedAdapter.getDisplayingNewsFeedIndices().get(position));
        intent.putExtra(INTENT_KEY_VIEW_NAME_IMAGE, imageView.getViewName());
        intent.putExtra(INTENT_KEY_VIEW_NAME_TITLE, titleView.getViewName());

        // 뉴스 새로 선택시
        intent.putExtra(INTENT_KEY_NEWS_FEED_LOCATION, INTENT_VALUE_BOTTOM_NEWS_FEED);
        intent.putExtra(INTENT_KEY_BOTTOM_NEWS_FEED_INDEX, position);

        // 미리 이미지뷰에 set해 놓은 태그(TintType)를 인텐트로 보내 적용할 틴트의 종류를 알려줌
        Object tintTag = viewHolder.imageView.getTag();
        TintType tintType = tintTag != null ? (TintType)tintTag : null;
        intent.putExtra(INTENT_KEY_TINT_TYPE, tintType);

        startActivityForResult(intent, RC_NEWS_FEED_DETAIL, activityOptions.toBundle());
    }

    @Override
    public void onBottomImageUrlFetchSuccess(News news, String url,
                                             int position) {
        NLLog.i(TAG, "onBottomImageUrlFetchSuccess");

        news.setImageUrlChecked(true);
        mBottomNewsFeedNewsToImageTaskMap.remove(news);

        if (url != null) {
            news.setImageUrl(url);

            // archive
            NewsFeedArchiveUtils.saveBottomNewsFeedAt(getApplicationContext(),
                    mBottomNewsFeedList.get(position), position);


            NLLog.i(TAG, "title : " + news.getTitle() + "'s image url fetch " +
                    "success.\nimage url : " + url);
        }
        if (mBottomNewsFeedAdapter != null && !mItemAnimator.isRunning()) {
            mBottomNewsFeedAdapter.notifyItemChanged(position);
        }
    }

    @Override
    public void onBottomImageUrlFetchFail(News news, int position) {
        NLLog.i(TAG, "onBottomImageUrlFetchFail");
        news.setImageUrlChecked(true);
        if (mBottomNewsFeedAdapter != null && !mItemAnimator.isRunning()) {
            mBottomNewsFeedAdapter.notifyItemChanged(position);
        }
    }

    @Override
    public void onAnimationsFinished() {
        fetchBottomNewsFeedListImage();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            switch (requestCode) {
                case RC_NEWS_FEED_DETAIL:
                    boolean hasNewsFeedReplaced = extras.getBoolean(
                            NewsFeedDetailActivity.INTENT_KEY_NEWSFEED_REPLACED, false);
                    String newsFeedType = extras.getString(INTENT_KEY_NEWS_FEED_LOCATION, null);

                    if (!hasNewsFeedReplaced || newsFeedType == null) {
                        break;
                    }

                    // 교체된게 top news feed인지 bottom news feed인지 구분
                    if (newsFeedType.equals(INTENT_VALUE_BOTTOM_NEWS_FEED)) {
                        // bottom news feed중 하나가 교체됨

                        // bottom news feed의 index를 가져옴
                        int idx = extras.getInt(INTENT_KEY_BOTTOM_NEWS_FEED_INDEX, -1);
                        if (idx >= 0) {
                            //read from cache
                            NewsFeed newsFeed = NewsFeedArchiveUtils.loadBottomNewsFeedAt(
                                    getApplicationContext(), idx);

                            if (newsFeed.isValid()) {
                                mBottomNewsFeedAdapter.replaceNewsFeedAt(idx, newsFeed);

                                News news = newsFeed.getNewsList().get(0);
                                if (news.getImageUrl() == null) {
                                    new BottomNewsImageUrlFetchTask(news, idx, this)
                                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                }
                            } else {
                                BottomNewsFeedFetchTask task = new BottomNewsFeedFetchTask(
                                        getApplicationContext(), newsFeed.getNewsFeedUrl(), idx,
                                        mOnBottomNewsFeedFetchListener, false);
                                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            }
                        }
                    } else if (newsFeedType.equals(INTENT_VALUE_TOP_NEWS_FEED)) {
                        // top news feed가 교체됨
                    }

                    break;
            }
        }
    }
}
