package com.yooiistudios.news.iab;

import android.content.Context;
import android.content.SharedPreferences;

import com.yooiistudios.news.iab.util.Inventory;
import com.yooiistudios.news.util.StoreDebugCheckUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by StevenKim in Morning Kit from Yooii Studios Co., LTD. on 2014. 1. 9.
 *
 * SKIabProducts
 */
public class IabProducts {
    public static final String SKU_FULL_VERSION_ORIGINAL = "full_version_original";
    public static final String SKU_FULL_VERSION = "test_full_version"; // "full_version"
    public static final String SKU_NO_ADS = "test_no_ad"; // "no_ad"
    public static final String SKU_MORE_PANELS = "test_more_news"; // "more_news"
    public static final String SKU_TOPIC_SELECT = "test_temp1"; // "topic_select"
    public static final String SKU_CUSTOM_RSS_URL = "test_custom_rss_feed"; // custom_rss_feed

    private static final String SHARED_PREFERENCES_IAB = "SHARED_PREFERENCES_IAB";
    private static final String SHARED_PREFERENCES_IAB_DEBUG = "SHARED_PREFERENCES_IAB_DEBUG";

    public static List<String> makeProductKeyList() {
        List<String> iabKeyList = new ArrayList<String>();
        iabKeyList.add(SKU_FULL_VERSION);
        iabKeyList.add(SKU_MORE_PANELS);
        iabKeyList.add(SKU_NO_ADS);
        iabKeyList.add(SKU_TOPIC_SELECT);
        return iabKeyList;
    }

    // 구매완료시 적용
    public static void saveIabProduct(String sku, Context context) {
        SharedPreferences prefs;
        if (StoreDebugCheckUtils.isUsingStore(context)) {
            prefs = context.getSharedPreferences(SHARED_PREFERENCES_IAB, Context.MODE_PRIVATE);
        } else {
            prefs = context.getSharedPreferences(SHARED_PREFERENCES_IAB_DEBUG, Context.MODE_PRIVATE);
        }
        prefs.edit().putBoolean(sku, true).apply();
    }

    // 인앱 정보를 읽어오며 자동으로 적용
    public static void saveIabProducts(Inventory inventory, Context context) {
        List<String> ownedSkus = inventory.getAllOwnedSkus();

        SharedPreferences.Editor edit = context.getSharedPreferences(SHARED_PREFERENCES_IAB, Context.MODE_PRIVATE).edit();
        edit.clear(); // 모두 삭제 후 다시 추가
        for (String sku : ownedSkus) {
            edit.putBoolean(sku, true);
        }
        edit.apply();
    }

    public static boolean isIabProductBought(String sku, Context context) {
        return loadOwnedIabProducts(context).contains(sku);
    }

    public static boolean containsSku(Context context, String sku) {
        List<String> ownedSkus = IabProducts.loadOwnedIabProducts(context);
        return ownedSkus.contains(sku);
    }

    // 구매된 아이템들을 로드
    public static List<String> loadOwnedIabProducts(Context context) {
        List<String> ownedSkus = new ArrayList<String>();

        SharedPreferences prefs;
        if (StoreDebugCheckUtils.isUsingStore(context)) {
            prefs = context.getSharedPreferences(SHARED_PREFERENCES_IAB, Context.MODE_PRIVATE);
        } else {
            prefs = context.getSharedPreferences(SHARED_PREFERENCES_IAB_DEBUG, Context.MODE_PRIVATE);
        }
        if (prefs.getBoolean(SKU_FULL_VERSION, false)) {
            ownedSkus.add(SKU_FULL_VERSION);
            ownedSkus.add(SKU_NO_ADS);
            ownedSkus.add(SKU_MORE_PANELS);
            ownedSkus.add(SKU_TOPIC_SELECT);
            ownedSkus.add(SKU_CUSTOM_RSS_URL);
        } else {
            if (prefs.getBoolean(SKU_NO_ADS, false)) {
                ownedSkus.add(SKU_NO_ADS);
            }
            if (prefs.getBoolean(SKU_MORE_PANELS, false)) {
                ownedSkus.add(SKU_MORE_PANELS);
            }
            if (prefs.getBoolean(SKU_TOPIC_SELECT, false)) {
                ownedSkus.add(SKU_TOPIC_SELECT);
            }
            if (prefs.getBoolean(SKU_CUSTOM_RSS_URL, false)) {
                ownedSkus.add(SKU_CUSTOM_RSS_URL);
            }
            // 추가: 언락화면에서 리뷰로 사용한 아이템도 체크
//            SharedPreferences unlockPrefs = context.getSharedPreferences(MNUnlockActivity.SHARED_PREFS, Context.MODE_PRIVATE);
//            String reviewUsedProductSku = unlockPrefs.getString(MNUnlockActivity.REVIEW_USED_PRODUCT_SKU, null);
//            if (reviewUsedProductSku != null && ownedSkus.indexOf(reviewUsedProductSku) == -1) {
//                ownedSkus.add(reviewUsedProductSku);
//            }
        }
        return ownedSkus;
    }

    /**
     * For Debug Mode
     */
    public static void resetIabProductsDebug(Context context) {
        SharedPreferences.Editor edit = context.getSharedPreferences(SHARED_PREFERENCES_IAB_DEBUG,
                Context.MODE_PRIVATE).edit();
        edit.clear().apply();
    }

    /**
     * For Naver Store Mode
     */
    // 인앱 정보를 읽어오며 자동으로 적용
//    public static void saveIabProducts(List<NaverIabInventoryItem> productList, Context context) {
//        SharedPreferences.Editor edit = context.getSharedPreferences(SHARED_PREFERENCES_IAB, Context.MODE_PRIVATE).edit();
//        edit.clear(); // 모두 삭제 후 다시 추가
//        for (NaverIabInventoryItem naverIabInventoryItem : productList) {
//            if (naverIabInventoryItem.isAvailable()) {
//                edit.putBoolean(NaverIabProductUtils.googleSkuMap.get(naverIabInventoryItem.getKey()), true);
//            }
//        }
//        edit.apply();
//    }
}
