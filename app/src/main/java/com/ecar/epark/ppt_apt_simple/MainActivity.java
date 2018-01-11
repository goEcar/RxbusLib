package com.ecar.epark.ppt_apt_simple;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


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
                Tes a = new Tes();
                a.a = "b";
                RxBus.getDefault().post(a,"asb");
            }
        },2000);
        activity_main.setOnClickListener(this);
    }

    @RxBusReact(clazz = Tes.class,tag = "asb")
    public void testRxBus(Tes content){
        textView.setText(content.a);
    }

    public static class Tes{
        String a;
    }

    @Override
    public void onClick(View v) {
        Tes a = new Tes();
        a.a = "a";
        RxBus.getDefault().post(a,"asb");
    }
}
