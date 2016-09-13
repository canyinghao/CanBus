package com.canyinghao.canbus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author canyinghao
 */
public class CanBus {


    private Map<Integer, List<Object>> subMap = new HashMap<>();

    private CanBusHandler handler = new CanBusHandler();

    public static String methodName = "onCanBus";

    private static volatile CanBus defaultInstance;

    public static CanBus getDefault() {
        if (defaultInstance == null) {
            synchronized (CanBus.class) {
                if (defaultInstance == null) {

                    defaultInstance = new CanBus();
                }
            }
        }
        return defaultInstance;
    }

    public void register(@NonNull Object subscriber) {
        register(subscriber, false, 0);

    }

    public void register(@NonNull Object subscriber, int priority) {
        register(subscriber, false, priority);
    }

    private synchronized void register(@NonNull Object subscriber, boolean sticky,
                                       int priority) {
        List<Object> list = null;
        if (subMap.containsKey(priority)) {
            list = subMap.get(priority);
        } else {
            list = new ArrayList<>();

        }

        list.add(subscriber);
        subMap.put(priority, list);

    }

    public synchronized void unregister(@NonNull Object subscriber) {

        unregister(subscriber, 0);
    }

    public synchronized void unregister(@NonNull Object subscriber, int priority) {

        if (subMap.containsKey(priority)) {
            List<Object> list = subMap.get(priority);
            if (!list.isEmpty() && list.contains(subscriber)) {

                list.remove(subscriber);

                subMap.put(priority, list);

            }

        }

    }

    public void post(@NonNull Object event) {

        post(event, 0);

    }

    public void post(@NonNull Object event, int priority) {

        if (Looper.myLooper() == Looper.getMainLooper()) {
            postOnMain(event, priority);
        } else {
            Message msg = new Message();
            msg.what = priority;
            msg.obj = event;
            handler.sendMessage(msg);
        }

    }

    private void postOnMain(@NonNull Object event, int priority) {
        if (subMap.containsKey(priority)) {
            List<Object> list = subMap.get(priority);
            List<Object> temp = new ArrayList<>();
            temp.addAll(list);

            for (int i = 0; i < temp.size(); i++) {
                Object object = temp.get(i);
                boolean isRemove = false;

                if (object == null) {
                    isRemove = true;
                } else {

                    if (object instanceof Activity) {
                        Activity act = (Activity) object;
                        if (act.isFinishing()) {
                            isRemove = true;
                        }
                    }

                    if (object instanceof Fragment) {
                        Fragment act = (Fragment) object;
                        if (act.isDetached()) {
                            isRemove = true;
                        }
                    }
                }

                if (isRemove) {
                    list.remove(object);
                }

            }

            for (Object object : list) {

                try {
                    invokeMethod(object, methodName, new Object[]{event});
                } catch (Exception e) {
                    e.printStackTrace();

                }
            }

        }
    }

    private Object invokeMethod(@NonNull Object owner, @NonNull String methodName, @NonNull Object[] args)
            throws Exception {

        Class ownerClass = owner.getClass();

        Class[] argsClass = new Class[args.length];

        for (int i = 0, j = args.length; i < j; i++) {
            argsClass[i] = args[i].getClass();
        }


        Method method = ownerClass.getMethod(methodName, argsClass);

        return method.invoke(owner, args);
    }

    @SuppressLint("HandlerLeak")
    class CanBusHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            if (msg.obj != null) {

                postOnMain(msg.obj, msg.what);
            }


            super.handleMessage(msg);
        }
    }


}
