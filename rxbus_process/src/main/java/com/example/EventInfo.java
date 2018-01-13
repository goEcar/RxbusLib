package com.example;

import java.util.LinkedList;

/**
 * Created by Administrator on 2018/1/12 0012.
 */

public class EventInfo {
    String parentClazzName;
    String clzzName;
    String packageName;
    LinkedList<MethodData> methodDatas;
    String parentPackageName;

    public void addData(MethodData methodData){
        methodDatas.add(methodData);
    }
}
