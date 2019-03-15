package grabpacket.bryan.com.grabpackettool;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.IOException;
import java.util.List;

public class GrabPacketService extends AccessibilityService {
    private final static String MM_PNAME = "com.tencent.mm";
    boolean hasAction = false;
    boolean locked = false;
    private String scontent = "";
    private KeyguardManager.KeyguardLock kl;
    private Handler handler = new Handler();


    /**
     * 必须重写的方法，响应各种事件。
     *
     * @param event
     */
    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {
        int eventType = event.getEventType();
        Logger.i("get event = " + eventType);
        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:// 通知栏事件
                Logger.i("获取通知栏消息");
                List<CharSequence> texts = event.getText();
                if (!texts.isEmpty()) {
                    for (CharSequence text : texts) {
                        String content = text.toString();
                        Logger.i("content" + content);
                        if (!TextUtils.isEmpty(content) && content.contains(":")) {
                            //推送消息不为空，判断屏幕是否锁屏
                            if (isScreenLocked()) {
                                locked = true;
                                //如果锁屏，则唤醒并解锁
                                wakeAndUnlock();
                                //如果微信在前台运行
                                if (isAppForeground(MM_PNAME)) {
                                    pressBackButton();
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            Logger.i("微信在前台，开始拉起微信页面");
                                            parseNotification(event);
                                        }
                                    }, 1000);
                                } else {
                                    Logger.i("is mm in background");
                                    parseNotification(event);
                                }
                            } else {
                                locked = false;
                                wakeAndUnlock();
                                parseNotification(event);
                            }
                        }
                    }
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                String className = event.getClassName().toString();
                Logger.i("当前页面" + className);
                if (TextUtils.equals(className,"com.tencent.mm.ui.LauncherUI")) {
                    AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                    if (scontent != null && scontent.contains("[微信红包]")){
                        Logger.i("收到推送红包");
                        getPacket(rootNode);
                    }else{
                        Logger.i("收到列表页红包");
                        findPacket(rootNode);
                    }
                }else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI")){
                    AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                    openPacket(rootNode);
                }
                break;
        }
    }


    /**
     * 解析通知栏信息，并模拟点击通知栏对应消息拉起微信
     *
     * @param event
     */
    private void parseNotification(AccessibilityEvent event) {
        hasAction = true;
        if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
            Notification notification = (Notification) event.getParcelableData();
            String content = notification.tickerText.toString();
            Logger.i("原始报文：" + content);
            String[] cc = content.split(":");
            scontent = cc[1].trim();
            if (scontent.contains("[微信红包]")){
                PendingIntent pendingIntent = notification.contentIntent;
                try {
                    pendingIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onInterrupt() {

    }

    /**
     * 判断指定的应用是否在前台运行
     *
     * @param packageName
     * @return
     */
    private boolean isAppForeground(String packageName) {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        String currentPackageName = cn.getPackageName();
        if (!TextUtils.isEmpty(currentPackageName) && currentPackageName.equals(packageName)) {
            return true;
        }
        return false;
    }




    /**
     * 系统是否在锁屏状态
     *
     * @return
     */
    private boolean isScreenLocked() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        return keyguardManager.inKeyguardRestrictedInputMode();
    }

    private void wakeAndUnlock() {
        //获取电源管理器对象
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        //获取PowerManager.WakeLock对象，后面的参数|表示同时传入两个值，最后的是调试用的Tag
        @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");

        //点亮屏幕
        wl.acquire(2000);

        //得到键盘锁管理器对象
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        kl = km.newKeyguardLock("unLock");

        //解锁
        kl.disableKeyguard();

    }

    private void release() {

        if (locked && kl != null) {
            Logger.i("release the lock");
            //得到键盘锁管理器对象
            kl.reenableKeyguard();
            locked = false;
        }
    }
    private void getPacket(AccessibilityNodeInfo rootNode){
        if (rootNode.getChildCount() == 0){
            if (rootNode.getText() != null){
                if ("领取红包".equals(rootNode.getText().toString())){
                    rootNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    AccessibilityNodeInfo parent = rootNode.getParent();
                    while (parent != null){
                        if (parent.isClickable()){
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            break;
                        }
                        parent = parent.getParent();
                    }
                }
            }
        }else{
            for (int i=0; i<rootNode.getChildCount(); i++){
                getPacket(rootNode.getChild(i));
            }
        }
    }

    private void findPacket(AccessibilityNodeInfo rootNode){
        if (rootNode.getChildCount() == 0){
            if (rootNode.getText() != null){
                if ("微信红包".equals(rootNode.getText().toString())){
                    rootNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    AccessibilityNodeInfo parent = rootNode.getParent();
                    while (parent != null){
                        if (parent.isClickable()){
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            break;
                        }
                        parent = parent.getParent();
                    }
                }
            }
        }else{
            for (int i=0; i<rootNode.getChildCount(); i++){
                findPacket(rootNode.getChild(i));
            }
        }

    }

    private void sendText() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("发送");
            if (list != null && list.size() > 0) {
                for (AccessibilityNodeInfo n : list) {
                    if (n.getClassName().equals("android.widget.Button") && n.isEnabled()) {
                        n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }

            } else {
                List<AccessibilityNodeInfo> liste = nodeInfo.findAccessibilityNodeInfosByText("Send");
                if (liste != null && liste.size() > 0) {
                    for (AccessibilityNodeInfo n : liste) {
                        if (n.getClassName().equals("android.widget.Button") && n.isEnabled()) {
                            n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                }
            }

        }

    }

    private void openPacket(AccessibilityNodeInfo rootNode){
        Logger.i("開紅包");
        if (rootNode != null){
            if (rootNode.getChildCount() == 0){
                rootNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                AccessibilityNodeInfo parent = rootNode.getParent();
                while (parent != null){

                    if (parent.getText() != null && parent.isClickable()){
                        if (!parent.getText().toString().contains("查看领取详情")){
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            pressBackButton();
                        }
                    }else{
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        pressBackButton();
                    }
                    parent = parent.getParent();
                }
            }else{
                for (int i=0; i<rootNode.getChildCount(); i++){

                    if (rootNode.getChild(i).getText() != null && rootNode.getChild(i).isClickable()){
                        if (!rootNode.getChild(i).getText().toString().contains("查看领取详情")){
                            rootNode.getChild(i).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            pressBackButton();
                        }
                    }else{
                        rootNode.getChild(i).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        pressBackButton();
                    }
                    openPacket(rootNode.getChild(i));
                }
            }

        }
    }
    /**
     * 模拟back按键
     */
    private void pressBackButton() {
        Logger.i("回到桌面");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Runtime runtime = Runtime.getRuntime();
                try {
                    runtime.exec("input keyevent " + KeyEvent.KEYCODE_BACK);
                    back2Home();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 1000);
    }

    /**
     * 回到系统桌面
     */
    private void back2Home() {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        home.addCategory(Intent.CATEGORY_HOME);
        startActivity(home);
    }


    public AccessibilityNodeInfo getNodeInfo() {
        return getRootInActiveWindow();
    }
}

