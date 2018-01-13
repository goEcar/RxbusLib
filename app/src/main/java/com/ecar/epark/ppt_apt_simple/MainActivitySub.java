package com.ecar.epark.ppt_apt_simple;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.ecar.epark.simulationlib.TestBean;

import rxbus.ecaray.com.rxbuslib.rxbus.RxBus;
import rxbus.ecaray.com.rxbuslib.rxbus.RxBusReact;

public class MainActivitySub extends MainActivity {

//    View activity_main;
//    TextView textView;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        //1.方式1
////        MainActivit y$$BindView.bindView(this);
//        //1.
//        textView = (TextView) findViewById(R.id.tx_content);
//        activity_main = findViewById(R.id.activity_main);
//        RxBus.getDefault().register(this);
//        textView.setText("APT");
//        textView.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                TestBean bean = new TestBean();
//                bean.setName("postDelayed1");
//                RxBus.getDefault().post(bean,"asb1");
//            }
//        },2000);
//        activity_main.setOnClickListener(this);
//    }
//
    @RxBusReact(clazz = Tes.class,tag = "asb1")
    public void testRxBus1(TestBean content){
        textView.setText(content.getName());
    }
//

    public static class Tes{
        String a;
    }

    @Override
    public void onClick(View v) {
        TestBean bean = new TestBean();
        bean.setName("onClick1");
        RxBus.getDefault().post(bean,"asb");
    }
}
