package net.mosajon.humiture;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import static net.mosajon.humiture.Account.strHttp;
import static net.mosajon.humiture.Account.strName;
import static net.mosajon.humiture.Account.strPassword;
import static net.mosajon.humiture.Account.strTelephone;
import static net.mosajon.humiture.MainActivity.getHtml;

public class LoginActivity extends AppCompatActivity {

    private NetWorkStateReceiver netWorkStateReceiver;
    private EditText loginhttpeEditText;
    private EditText loginnameEditText;
    private EditText loginpasswordEditText;
    private EditText calltelephoneEditText;
    private CheckBox accountCheckbox;
    private String path;
    private MediaPlayer mediaPlayer;
    private String filePath;
    private FileOutputStream fileOutputStream;
    private FileInputStream fileInputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads().detectDiskWrites().detectNetwork()
                .penaltyLog().build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects().detectLeakedClosableObjects()
                .penaltyLog().penaltyDeath().build());

        loginhttpeEditText = (EditText) findViewById(R.id.login_http);
        loginnameEditText = (EditText) findViewById(R.id.login_name);
        loginpasswordEditText = (EditText) findViewById(R.id.login_password);
        calltelephoneEditText = (EditText) findViewById(R.id.call_telephone);
        accountCheckbox = (CheckBox) findViewById(R.id.account_checkbox);

        loginhttpeEditText.setText(strHttp);
        loginnameEditText.setText(strName);
        loginpasswordEditText.setText(strPassword);
        calltelephoneEditText.setText(strTelephone);

        filePath = Environment.getExternalStorageDirectory().toString() + "/humiture/account.json";
        try {
            fileInputStream = new FileInputStream(filePath);
            JsonReader jsonReader = new JsonReader(new InputStreamReader(fileInputStream, "UTF-8"));
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                if (jsonReader.nextName().equals("loginHttp")) {
                    loginhttpeEditText.setText(jsonReader.nextString());
                }
                if (jsonReader.nextName().equals("loginName")) {
                    loginnameEditText.setText(jsonReader.nextString());
                }
                if (jsonReader.nextName().equals("loginPassword")) {
                    loginpasswordEditText.setText(jsonReader.nextString());
                }
                if (jsonReader.nextName().equals("callTelephone")) {
                    calltelephoneEditText.setText(jsonReader.nextString());
                }
                accountCheckbox.setChecked(true);
            }
            jsonReader.endObject();
            jsonReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        BtnLogin_Click(null);
    }

    public void BtnLogin_Click(View view) {

        if (TextUtils.isEmpty(loginhttpeEditText.getText().toString().trim())) {
            Toast.makeText(LoginActivity.this, "服务器地址不能为空！", Toast.LENGTH_SHORT).show();
        }

        if (TextUtils.isEmpty(loginnameEditText.getText().toString().trim())) {
            Toast.makeText(LoginActivity.this, "用户名不能为空！", Toast.LENGTH_SHORT).show();
        }

        if (TextUtils.isEmpty(loginpasswordEditText.getText().toString().trim())) {
            Toast.makeText(LoginActivity.this, "密码不能为空！", Toast.LENGTH_SHORT).show();
        }

        path = loginhttpeEditText.getText().toString().trim() + ":9001/Device/AppLogin?userID=" + loginnameEditText.getText().toString().trim()
                + "&userPassword=" + loginpasswordEditText.getText().toString().trim();

        String htmlContent = null;
        try {
            htmlContent = getHtml(path);

            strHttp = loginhttpeEditText.getText().toString().trim();
            strName = loginnameEditText.getText().toString().trim();
            strPassword = loginpasswordEditText.getText().toString().trim();
            strTelephone = calltelephoneEditText.getText().toString().trim();

            File destDir = new File(Environment.getExternalStorageDirectory().toString() + "/humiture");
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            filePath = Environment.getExternalStorageDirectory().toString() + "/humiture/account.json";
            try {
                if (accountCheckbox.isChecked()) {
                    fileOutputStream = new FileOutputStream(filePath);
                    JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(fileOutputStream, "UTF-8"));
                    jsonWriter.beginObject();
                    jsonWriter.name("loginHttp").value(loginhttpeEditText.getText().toString().trim());
                    jsonWriter.name("loginName").value(loginnameEditText.getText().toString().trim());
                    jsonWriter.name("loginPassword").value(loginpasswordEditText.getText().toString().trim());
                    jsonWriter.name("callTelephone").value(calltelephoneEditText.getText().toString().trim());
                    jsonWriter.endObject();
                    jsonWriter.close();
                } else {
                    File destFile = new File(filePath);
                    destFile.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            parseJSONWithJSONObject(htmlContent);
        } catch (Exception e) {
            Toast.makeText(LoginActivity.this, "登录失败！", Toast.LENGTH_LONG).show();
            mediaPlayer = MediaPlayer.create(LoginActivity.this, R.raw.choochoo);
            mediaPlayer.start();
        }
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

    protected void onPause() {
        unregisterReceiver(netWorkStateReceiver);
        super.onPause();
    }

    private void parseJSONWithJSONObject(String JsonData) {
        try {
            JSONObject jsonObject = new JSONObject(JsonData);
            String strLogin = jsonObject.getString("login");
            String strRight = jsonObject.getString("right");
            if (strLogin.equals("1")) {
                Intent intentLogin = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intentLogin);
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}