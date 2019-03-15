package grabpacket.bryan.com.grabpackettool;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

public class MainActivity extends AppCompatActivity {

    GrabPacketService mService = new GrabPacketService();
    private static final int GET_PULLET = 1000;
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case GET_PULLET:
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void open(View v){
        new CheckPage().start();
    }
    class CheckPage extends Thread{
        @Override
        public void run() {
            super.run();
            while (true){
                AccessibilityNodeInfo rootNode = mService.getRootInActiveWindow();
                getPacket(rootNode);
                Message msg = new Message();
                msg.what = GET_PULLET;
                handler.sendMessage(msg);
            }
        }
    }

    private void getPacket(AccessibilityNodeInfo rootNode){
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
                getPacket(rootNode.getChild(i));
            }
        }
    }
}
