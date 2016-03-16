package org.zywx.wbpalmstar.plugin.uexmobsms;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;

import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;

public class EUEXMobSMS extends EUExBase {
    private static final String TAG = "EUEXMobSMS";
    public static final String CALLBACK_ON_SEND_CODE = "uexMobSMS.cbSendClick";
    public static final String CALLBACK_ON_COMMIT_CODE = "uexMobSMS.cbCommitClick";
    public static final String MSG_COMMIT_CODE_SUCCESS = "验证成功";
    public static final String MSG_COMMIT_CODE_FAIL = "验证失败";
    public static final String MSG_GET_CODE_SUCCESS = "获取验证码成功";
    public static final String MSG_GET_CODE_FAIL = "获取验证码失败";

    public EUEXMobSMS(Context context, EBrowserView view) {
        super(context, view);

    }

    public void init(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        String json = params[0];
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(json);
            if (TextUtils.isEmpty(jsonObject.optString("uexMobSMS_APPKey")) ||
                    TextUtils.isEmpty(jsonObject.optString("uexMobSMS_APPSecret"))) {
                Toast.makeText(mContext, "uexMobSMS_APPKey or uexMobSMS_APPSecret is null !", Toast.LENGTH_SHORT).show();
                return;
            }
            String appKey = jsonObject.getString("uexMobSMS_APPKey");
            String appSecret =  jsonObject.getString("uexMobSMS_APPSecret");
            SMSSDK.initSDK(mContext, appKey, appSecret, false);
            SMSSDK.registerEventHandler(eventHandler);
        } catch (JSONException e) {
            Log.i(TAG, e.getMessage());
            Toast.makeText(mContext, "JSON解析错误", Toast.LENGTH_SHORT).show();
        }
    }
    EventHandler eventHandler = new EventHandler(){

        @Override
        public void afterEvent(int event, int result, Object data) {
            Message msg = new Message();
            msg.arg1 = event;
            msg.arg2 = result;
            msg.obj = data;
            smsHandler.sendMessage(msg);
        }
    };
    Handler smsHandler = new Handler() {

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int event = msg.arg1;
            int result = msg.arg2;
            Object data = msg.obj;
            if (result == SMSSDK.RESULT_COMPLETE) {
                JSONObject jsonObject = new JSONObject();
                if (event == SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE) {
                    try {
                        jsonObject.put("status", EUExCallback.F_C_SUCCESS);
                        jsonObject.put("msg", MSG_COMMIT_CODE_SUCCESS);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    callBackPluginJs(CALLBACK_ON_COMMIT_CODE, jsonObject.toString());
                } else if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE){
                    try {
                        jsonObject.put("status", EUExCallback.F_C_SUCCESS);
                        jsonObject.put("msg", MSG_GET_CODE_SUCCESS);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    callBackPluginJs(CALLBACK_ON_SEND_CODE, jsonObject.toString());
                }

            } else {
                errorCallback(0, 0, "操作出错，请重试");
            }
        }
    };

    public void errorCallback(String fun, String msg) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("status", EUExCallback.F_C_FAILED);
            jsonObject.put("msg", msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        callBackPluginJs(fun, jsonObject.toString());
    }

    public void sendCode(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        String json = params[0];
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(json);
            if (TextUtils.isEmpty(jsonObject.optString("phoneNum")) ||
                    TextUtils.isEmpty(jsonObject.optString("countryCode"))) {
                errorCallback(CALLBACK_ON_SEND_CODE, "phoneNum or countryCode is null !");
                return;
            }
            String phoneNum = jsonObject.getString("phoneNum");
            String countryCode =  jsonObject.getString("countryCode");
            SMSSDK.getVerificationCode(countryCode, phoneNum);
        } catch (JSONException e) {
            Log.i(TAG, e.getMessage());
            Toast.makeText(mContext, "JSON解析错误", Toast.LENGTH_SHORT).show();
        }
    }

    public void commitCode(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        String json = params[0];
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(json);
            if (TextUtils.isEmpty(jsonObject.optString("phoneNum"))
                    || TextUtils.isEmpty(jsonObject.optString("countryCode"))
                    || TextUtils.isEmpty(jsonObject.optString("validCode")) ) {
                errorCallback(CALLBACK_ON_COMMIT_CODE, "phoneNum, countryCode or validCode is null !");
                return;
            }
            String phoneNum = jsonObject.getString("phoneNum");
            String countryCode =  jsonObject.getString("countryCode");
            String validCode =  jsonObject.getString("validCode");
            SMSSDK.submitVerificationCode(countryCode, phoneNum, validCode);
        } catch (JSONException e) {
            Log.i(TAG, e.getMessage());
            Toast.makeText(mContext, "JSON解析错误", Toast.LENGTH_SHORT).show();
        }
    }

    private void callBackPluginJs(String methodName, String jsonData){
        String js = SCRIPT_HEADER + "if(" + methodName + "){"
                + methodName + "('" + jsonData + "');}";
        onCallback(js);
    }

    @Override
    protected boolean clean() {
        return true;
    }
}
