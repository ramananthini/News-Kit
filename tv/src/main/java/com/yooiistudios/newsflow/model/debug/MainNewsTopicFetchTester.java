package com.yooiistudios.newsflow.model.debug;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.yooiistudios.newsflow.core.news.NewsFeed;
import com.yooiistudios.newsflow.core.news.NewsTopic;
import com.yooiistudios.newsflow.core.news.curation.NewsContentProvider;
import com.yooiistudios.newsflow.core.news.database.NewsDb;
import com.yooiistudios.newsflow.core.util.NLLog;
import com.yooiistudios.newsflow.ui.activity.MainActivity;

import java.util.ArrayList;

/**
 * Created by Dongheyon Jeong in News Flow from Yooii Studios Co., LTD. on 15. 3. 16.
 *
 * MainNewsTopicFetchTest
 *  메인엑티비티 테스트용
 */
public class MainNewsTopicFetchTester {
    private MainActivity mActivity;

    private int mTopNewsCount;
    private SparseArray<Integer> mBottomNewsCount = new SparseArray<>();
    private boolean mTopFetched;
    private boolean mBottomAllFetched;

    public MainNewsTopicFetchTester(MainActivity activity) {
        mActivity = activity;
    }

    // DEBUG

    public void testFetch() {
        NewsDb.getInstance(mActivity).clearArchive();
        NewsFeed topNewsFeed = new NewsFeed(getDefaultTopic("ko", null, "kr", 1));
        ArrayList<NewsFeed> bottomNewsFeeds = new ArrayList<>();
        bottomNewsFeeds.add(new NewsFeed(getDefaultTopic("ko", null, "kr", 2)));
        bottomNewsFeeds.add(new NewsFeed(getDefaultTopic("ko", null, "kr", 3)));
        bottomNewsFeeds.add(new NewsFeed(getDefaultTopic("ko", null, "kr", 4)));
        bottomNewsFeeds.add(new NewsFeed(getDefaultTopic("ko", null, "kr", 5)));
        bottomNewsFeeds.add(new NewsFeed(getDefaultTopic("ko", null, "kr", 6)));
        bottomNewsFeeds.add(new NewsFeed(getDefaultTopic("ko", null, "kr", 7)));
        bottomNewsFeeds.add(new NewsFeed(getDefaultTopic("ko", null, "kr", 8)));
//        bottomNewsFeeds.add(new NewsFeed(getDefaultTopic("ja", null, "jp", 3)));
//        bottomNewsFeeds.add(new NewsFeed(getDefaultTopic("ja", null, "jp", 4)));
//        bottomNewsFeeds.add(new NewsFeed(getDefaultTopic("sv", null, "se", 1)));

        mActivity.fetch(topNewsFeed, bottomNewsFeeds);
    }

    private NewsTopic getDefaultTopic(@NonNull String targetLanguageCode,
                                      @Nullable String targetRegionCode,
                                      @NonNull String targetCountryCode, int targetProviderId) {
        ArrayList<NewsTopic> topics =
                NewsContentProvider.getInstance(mActivity).getNewsProvider(
                        targetLanguageCode, targetRegionCode, targetCountryCode, targetProviderId)
                        .getNewsTopicList();

        NewsTopic targetTopic = null;
        for (NewsTopic topic : topics) {
            if (topic.isDefault()) {
                targetTopic = topic;
                break;
            }
        }

        return targetTopic;
    }

    public void checkAllTopNewsContentFetched() {
        mTopNewsCount -= 1;
        if (mTopNewsCount == 0) {
            NLLog.now("Top NewsContent fetch done.");
            mTopFetched = true;
            saveDebug();
        }
    }

    public void checkAllBottomNewsContentFetched(int newsFeedPosition) {
        int prevNewsCount = mBottomNewsCount.get(newsFeedPosition);
        mBottomNewsCount.put(newsFeedPosition, --prevNewsCount);
        boolean allFetched = true;
        for (int i = 0; i < mBottomNewsCount.size(); i++) {
            Integer cnt = mBottomNewsCount.get(mBottomNewsCount.keyAt(i));
            if (cnt != 0) {
                allFetched = false;
            }
        }
        if (allFetched) {
            NLLog.now("Bottom NewsContent fetch done.");
            mBottomAllFetched = true;
            saveDebug();
        }
    }

    private void saveDebug() {
        if (mTopFetched && mBottomAllFetched) {
            NewsDb.copyDbToExternalStorage(mActivity);
            NLLog.now("db copied");
        }
    }

    public void onFetchAllNewsFeeds(NewsFeed topNewsFeed, ArrayList<NewsFeed> bottomNewsFeeds) {
        mTopNewsCount = topNewsFeed.getNewsList().size();
        mBottomNewsCount = new SparseArray<>();
        for (int i = 0 ; i < bottomNewsFeeds.size(); i++) {
            mBottomNewsCount.put(i, bottomNewsFeeds.get(i).getNewsList().size());
        }
    }
}
