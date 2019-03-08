package com.rhyme.project.hooktest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn=findViewById(R.id.btn);
        btn.setOnClickListener(this);
        //Hook 点击事件
        try {
            HookHelper.hookOnClickListener(btn);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Hook 通知栏
        try {
            HookHelper.hookNotificationManager(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Notification.Builder createNotification(String title, String content,int notification_icon, Class<?> intent_class) {
        Notification.Builder builder = new Notification.Builder(this);
        Intent notificationIntent = new Intent(this, intent_class);
        Bitmap icon = BitmapFactory.decodeResource(this.getResources(),
                notification_icon);
        // 设置PendingIntent
        builder.setContentIntent(PendingIntent.getActivity(this, 0, notificationIntent, 0))
                .setLargeIcon(icon)  // 设置下拉列表中的图标(大图标)
                .setContentTitle(title) // 设置下拉列表里的标题
                .setSmallIcon(notification_icon) // 设置状态栏内的小图标
                .setContentText(content) // 设置上下文内容
                .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间

        return builder;
    }

    @Override
    public void onClick(View view) {
        getTv().setText("My name is ben");

        NotificationManager manager= (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(1,createNotification("标题","内容",R.mipmap.ic_launcher,MainActivity.class).build());
    }
    private TextView getTv(){
        return findViewById(R.id.textView);
    }
}
