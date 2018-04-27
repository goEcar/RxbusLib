package com.ecar.epark.ppt_apt_simple;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;


import com.ecar.epark.ppt_apt_simple.bean.TestBean;

import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import rxbus.ecaray.com.rxbuslib.rxbus.RxBus;
import rxbus.ecaray.com.rxbuslib.rxbus.RxBusReact;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    View activity_main;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //1.方式1
//        MainActivit y$$BindView.bindView(this);
        //1.
        textView = (TextView) findViewById(R.id.tx_content);
        activity_main = findViewById(R.id.activity_main);
        RxBus.getDefault().register(this);
        textView.setText("APT");
        textView.postDelayed(new Runnable() {
            @Override
            public void run() {
                TestBean bean = new TestBean();
                bean.setName("postDelayed");
                RxBus.getDefault().post(bean,"asb");
            }
        },2000);
        activity_main.setOnClickListener(this);
    }

    @RxBusReact(clazz = Tes.class,tag = "asb")
    public void testRxBus(TestBean content){
        textView.setText(content.getName());
    }

    public static class Tes{
        String a;
    }

    @Override
    public void onClick(View v) {
        TestBean bean = new TestBean();
        bean.setName("onClick");
        RxBus.getDefault().post(bean,"asb");


    }
}
