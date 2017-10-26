package net.mosajon.humiture;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.JsonReader;
import android.view.KeyEvent;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import static net.mosajon.humiture.Account.alarmCount;
import static net.mosajon.humiture.Account.closeCount;
import static net.mosajon.humiture.Account.strHttp;
import static net.mosajon.humiture.Account.strName;
import static net.mosajon.humiture.Account.strPassword;
import static net.mosajon.humiture.Account.strTelephone;
import static net.mosajon.humiture.Account.telCount;
import static net.mosajon.humiture.Account.timerCount;

public class MainActivity extends AppCompatActivity {

    private NetWorkStateReceiver netWorkStateReceiver;
    private PowerManager.WakeLock wakeLock;
    private Timer timer = new Timer();
    private MediaPlayer mediaPlayer;
    private TextView textViewTemp;
    private String path;
    private String filePath;
    private FileInputStream fileInputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //版本4.0后需加这个，不然就报错android.os.NetworkOnMainThreadException
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads().detectDiskWrites().detectNetwork()
                .penaltyLog().build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects().detectLeakedClosableObjects()
                .penaltyLog().penaltyDeath().build());

        textViewTemp = (TextView) findViewById(R.id.textViewTemp);

        filePath = Environment.getExternalStorageDirectory().toString() + "/humiture/account.json";
        File destFile = new File(filePath);
        if (destFile.exists()) {
            try {
                fileInputStream = new FileInputStream(filePath);
                JsonReader jsonReader = new JsonReader(new InputStreamReader(fileInputStream, "UTF-8"));
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    if (jsonReader.nextName().equals("loginHttp")) {
                        strHttp = jsonReader.nextString();
                    }
                    if (jsonReader.nextName().equals("loginName")) {
                        strName = jsonReader.nextString();
                    }
                    if (jsonReader.nextName().equals("loginPassword")) {
                        strPassword = jsonReader.nextString();
                    }
                    if (jsonReader.nextName().equals("callTelephone")) {
                        strTelephone = jsonReader.nextString();
                    }
                }
                jsonReader.endObject();
                jsonReader.close();

                path = strHttp + ":9001/Device/getDeviceData?userID=" + strName + "&userPassword=" + strPassword;

                acquireWakeLock();

                if (timerCount < 1) {
                    timer.schedule(task, 0, 3000);
                    timerCount++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (strHttp != null && strHttp != null && strHttp != null) {
                try {
                    path = strHttp + ":9001/Device/getDeviceData?userID=" + strName + "&userPassword=" + strPassword;
                    acquireWakeLock();
                    if (timerCount < 1) {
                        timer.schedule(task, 0, 3000);
                        timerCount++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                Intent intentLogin = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intentLogin);
                finish();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK)
            return true;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        if (netWorkStateReceiver == null) {
            netWorkStateReceiver = new NetWorkStateReceiver();
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(netWorkStateReceiver, filter);
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(netWorkStateReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        synchronized (this) {
            alarmCount = 0;
            telCount = 0;
            closeCount = 0;
            timerCount--;
            releaseWakeLock();
            timer.cancel();
        }
        super.onDestroy();
    }

    //获取锁，保持屏幕亮度
    private void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getCanonicalName());
            wakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }

    }

    private void call(String phone) {
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        startActivity(intent);
    }

    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            Message message = new Message();
            message.what = 1;
            handler.sendMessage(message);
        }
    };

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    String htmlContent = null;
                    try {
                        htmlContent = getHtml(path);
                        parseJSONWithJSONObject(htmlContent);
                    } catch (Exception e) {
                        textViewTemp.setText("连接服务器失败！");
                        mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.choochoo);
                        mediaPlayer.start();
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    //方法一：使用JSONObject
    //DevKey:设备唯一 ID
    //DevName：设备名
    //DevType：设备类型（0为模拟量，1为开关量）
    //DevAddr：设备地址
    //DevTempName：模拟量一名称
    //DevTempValue：模拟量一的值
    //DevHumiName：模拟量二名称
    //DevHumiValue：模拟量二的值
    //DevStatus：设备状态（false表示离线，true表示在线）
    //DevLng：设备所处纬度
    //DevLat：设备所处经度
    //TempStatus：模拟量一报警状态（0表示不报警，1表示超上限，2表示超下限）
    //HumiStatus：模拟量二报警状态（0表示不报警，1表示超上限，2表示超下限）
    //devDataType1：模拟量一相关参数设置标志（0表示不具备设置权限，1表示有设置权限）
    //devDataType2：模拟量二相关参数设置标志（0表示不具备设置权限，1表示有设置权限）
    //devPos：设备节点号
    private void parseJSONWithJSONObject(String JsonData) {
        try {
            JSONArray jsonArray = new JSONArray(JsonData);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String devKey = jsonObject.getString("DevKey");
                String devName = jsonObject.getString("DevName");
                String devType = jsonObject.getString("DevType");
                String devAddr = jsonObject.getString("DevAddr");
                String devTempName = jsonObject.getString("DevTempName");
                String devTempValue = jsonObject.getString("DevTempValue");
                String devHumiName = jsonObject.getString("DevHumiName");
                String devHumiValue = jsonObject.getString("DevHumiValue");
                String devStatus = jsonObject.getString("DevStatus");
                String devLng = jsonObject.getString("DevLng");
                String devLat = jsonObject.getString("DevLat");
                String tempStatus = jsonObject.getString("TempStatus");
                String humiStatus = jsonObject.getString("HumiStatus");
                String DevDataType1 = jsonObject.getString("devDataType1");
                String DevDataType2 = jsonObject.getString("devDataType2");
                String DevPos = jsonObject.getString("devPos");

                if (devStatus.equals("True")) {
                    devStatus = "在线";
                }

                if (devStatus.equals("False")) {
                    devStatus = "离线";
                }

                if (!tempStatus.equals("0")) {
                    if (alarmCount < 10) {
                        mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.alarm1);
                        mediaPlayer.start();
                        alarmCount++;
                    } else {
                        if (telCount < 1) {
                            call(strTelephone);
                            telCount++;
                        }
                    }

                    if (closeCount < 20 * 5) {
                        closeCount++;
                    } else {
                        alarmCount = 0;
                        telCount = 0;
                        closeCount = 0;
                    }
                } else {
                    alarmCount = 0;
                    telCount = 0;
                    closeCount = 0;
                }

                textViewTemp.setText("设备名：" + devName + "\n\n" + devTempName + "：" + devTempValue + "\n\n" + devHumiName + "：" + devHumiValue + "\n\n" + "设备类型：" + devType
                        + "\n\n设备状态：" + devStatus + "\n\n设备节点：" + DevPos); //+ "\n\nalarmCount" + alarmCount + "\ntelCount" + telCount + "\ncloseCount" + closeCount);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getHtml(String path) throws Exception {
        // 通过网络地址创建URL对象
        URL url = new URL(path);
        // 根据URL
        // 打开连接，URL.openConnection函数会根据URL的类型，返回不同的URLConnection子类的对象，这里URL是一个http，因此实际返回的是HttpURLConnection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        // 设定URL的请求类别，有POST、GET 两类
        conn.setRequestMethod("GET");
        //设置从主机读取数据超时（单位：毫秒）
        conn.setConnectTimeout(5000);
        //设置连接主机超时（单位：毫秒）
        conn.setReadTimeout(5000);
        // 通过打开的连接读取的输入流,获取html数据
        InputStream inStream = conn.getInputStream();
        // 得到html的二进制数据
        byte[] data = readInputStream(inStream);
        // 是用指定的字符集解码指定的字节数组构造一个新的字符串
        String html = new String(data, "UTF-8");
        return html;
    }

    //读取输入流，得到html的二进制数据
    public static byte[] readInputStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        inStream.close();
        return outStream.toByteArray();
    }
}