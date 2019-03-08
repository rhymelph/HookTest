package com.rhyme.project.hooktest;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 作者： rhyme.
 * 日期： 2019/3/8.
 * 描述： .
 **/
public class HookHelper {
    public static final String TAG = HookHelper.class.getSimpleName();

    /**
     * Hook 点击事件
     * @param view 需要Hook的视图
     * @throws Exception 异常
     */
    public static void hookOnClickListener(View view) throws Exception {
        //反射获取ListenerInfo对象
        Method getListenrInfo = View.class.getDeclaredMethod("getListenerInfo");
        getListenrInfo.setAccessible(true);
        Object listenerInfo = getListenrInfo.invoke(view);
        //得到原始的OnClickListener事件方法
        Class<?> listenerInfoClz = Class.forName("android.view.View$ListenerInfo");
        Field mOnClickListener = listenerInfoClz.getDeclaredField("mOnClickListener");
        mOnClickListener.setAccessible(true);
        View.OnClickListener originOnClickListener = (View.OnClickListener) mOnClickListener.get(listenerInfo);
        //使用Hook代理类 替换原始的OnClickListener
        View.OnClickListener hookedOnClickListener = new HookedClickListenerProxy(originOnClickListener);
        mOnClickListener.set(listenerInfo, hookedOnClickListener);
    }


    public static class HookedClickListenerProxy implements View.OnClickListener {
        private View.OnClickListener onClickListener;

        HookedClickListenerProxy(View.OnClickListener onClickListener) {
            this.onClickListener = onClickListener;
        }

        @Override
        public void onClick(View v) {
            Toast.makeText(v.getContext(), "Hook click ", Toast.LENGTH_SHORT).show();
            if (onClickListener != null) {
                onClickListener.onClick(v);
            }
        }
    }


    /**
     * Hook 通知栏
     * @param context 上下文
     * @throws Exception 异常
     */
    public static void hookNotificationManager(final Context context) throws Exception {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Method getService = NotificationManager.class.getDeclaredMethod("getService");
        getService.setAccessible(true);
        //获取系统的sService
        final Object service = getService.invoke(notificationManager);
        Class iNotiMngClz = Class.forName("android.app.INotificationManager");
        // 得到动态代理的对象
        Object proxyNotiMng = Proxy.newProxyInstance(context.getClass().getClassLoader(),
                new Class[]{iNotiMngClz}, new HookedNotificationHandler(context,service));
        Field mServiceField = NotificationManager.class.getDeclaredField("sService");
        mServiceField.setAccessible(true);
        mServiceField.set(notificationManager, proxyNotiMng);
    }

    public static class HookedNotificationHandler implements InvocationHandler{
        private Context context;
        private Object service;
        HookedNotificationHandler(Context context, Object service){
            this.context=context;
            this.service=service;
        }
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Log.d(TAG, "invoke: method：" + method);
            String methodName = method.getName();
            Log.d(TAG, "invoke: methodName：" + methodName);
            if (args != null && args.length > 0) {
                for (Object arg : args) {
                    if (arg instanceof Notification){
                        Log.d(TAG, "invoke: args:"+((Notification) arg).tickerText);
                    }
                    Log.d(TAG, "invoke: arg：" + arg);
                }
            }
            Toast.makeText(context, "发送了通知", Toast.LENGTH_SHORT).show();
            //交回给系统处理，不拦截通知
            return method.invoke(service, args);
        }
    }



    public static void hookClipboardService(final Context context) throws Exception{
        ClipboardManager clipboardManager= (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        Field mServiceFiled = Context.class.getDeclaredField("mService");
        mServiceFiled.setAccessible(true);
        //获取系统的mService
        final Object mService = mServiceFiled.get(clipboardManager);
        //初始化动态代理对象
        Class aClass=Class.forName("android.content.IClipboard");
        Object proxyInstance=Proxy.newProxyInstance(context.getClass().getClassLoader(), new Class[]{aClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Log.d(TAG, "invoke: method:"+method);
                String name=method.getName();
                Log.d(TAG, "invoke: methodName:"+name);
                if (args!=null&&args.length>0){
                    for (Object arg : args){
                        Log.d(TAG, "invoke: arg="+arg);
                    }
                }
                if ("setPrimaryClip".equals(name)){
                    Object arg=args[0];
                    if (arg instanceof ClipData){
                        ClipData clipData= (ClipData) arg;
                        int itemCount=clipData.getItemCount();
                        for (int i=0;i<itemCount;i++){
                            ClipData.Item item=clipData.getItemAt(i);
                            Log.d(TAG, "invoke: item："+item);
                        }
                    }
                    Toast.makeText(context,"检查到设置粘贴内容",Toast.LENGTH_SHORT).show();

                }else if ("getPrimaryClip".equals(name)){
                    Toast.makeText(context,"检查到获取粘贴内容",Toast.LENGTH_SHORT).show();
                }
                return method.invoke(mService,args);
            }
        });

        mServiceFiled.set(clipboardManager,proxyInstance);

    }
}