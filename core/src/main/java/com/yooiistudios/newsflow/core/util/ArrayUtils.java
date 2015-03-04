package com.yooiistudios.newsflow.core.util;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Dongheyon Jeong on in News-Android-L from Yooii Studios Co., LTD. on 14. 11. 12.
 *
 * ArrayUtils
 *  배열에 관한 편의기능을 제공하는 클래스
 */
public class ArrayUtils {
    public static <T> ArrayList<T> toArrayList(T[] array) {
        ArrayList<T> returnList = new ArrayList<>(array.length);
        Collections.addAll(returnList, array);

        return returnList;
    }
}