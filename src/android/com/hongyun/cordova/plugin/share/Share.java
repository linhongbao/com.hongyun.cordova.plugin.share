/*******************************************************************************
 * Copyright (c) 1999, 2016 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 */
package com.hongyun.cordova.plugin.share;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import org.apache.cordova.PluginResult;


import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.util.Log;


import com.google.zxing.common.BitMatrix;


import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;

import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;


/*
*
* 这个函数,一次想将标题，以及9个照片,全都发了的微信朋友圈
* */

public class Share extends CordovaPlugin {

    //直接广播给微信朋友圈
    private void doWSString(String title,JSONArray fileArr){
        File[] files;
        int len = fileArr.length();
        files = new File[len];
        String fileName;
        for(int i  = 0;i< len; i++){
            try {
                fileName = fileArr.get(i).toString();
                files[i] = new File(fileName);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        doWS(title,files);
    }

    /***
     * 这种通过intent发送给微信的方式,看网上资料不需要,注册微信
     * @param title
     * @param files
     */
    private void doWS(String title,File[] files){
        Intent intent = new Intent();
        ComponentName comp = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareToTimeLineUI");
        intent.setComponent(comp);
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("image/*");
        intent.putExtra("Kdescription", title);
        ArrayList<Uri> imageUris = new ArrayList<Uri>();

//        for (File f : files) {
//            imageUris.add(Uri.fromFile(f));
//        }

        Uri data;
        // 判断版本大于等于7.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // "net.csdn.blog.ruancoder.fileprovider"即是在清单文件中配置的authorities

            // 给目标应用一个临时授权
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            for (File f : files) {
                data = FileProvider.getUriForFile(m_context, m_context.getApplicationContext().getPackageName() + ".fileprovider", f);
                imageUris.add(data);

                /***
                 * 这种方式,虽然在andorid来说,没有把file暴露给其他应用,但是微信却是不支持/
                 */
            }
        } else {
            for (File f : files) {
                imageUris.add(Uri.fromFile(f));
            }
        }


        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
        this.cordova.startActivityForResult(this, intent, 0);
    }

    private IWXAPI api;
    //这个平台在微信开发者平台查看
    public static final String APP_ID = "wx154caf29298feace";
    private int mTargetScene = SendMessageToWX.Req.WXSceneSession;


    private CallbackContext currCallbackContext;
    public Context m_context;

    public boolean execute2(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        CallbackContext currCallbackContext = callbackContext;

        try {
            JSONObject jo = args.getJSONObject(0);
//            doSendIntent(jo.getString("subject"), jo.getString("text"), jo.getString("imagePath"), jo.getString("mimeType"));

            doWSString(jo.getString("title"),jo.getJSONArray("files"));
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
            return true;
        } catch (JSONException e) {
            Log.e("PhoneGapLog", "Share Plugin: Error: " + PluginResult.Status.JSON_EXCEPTION);
            e.printStackTrace();
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
            return false;
        }
    }

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        CallbackContext currCallbackContext = callbackContext;
        JSONObject jo = args.getJSONObject(0);
        /***
         * 俩种分享方式
         */
        if(action.equals("show")){
            /***
             * 这个是通过Intent的方式来分享的
             */
            doWSString(jo.getString("title"),jo.getJSONArray("files"));
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
        }else if(action.equals("share")){
            /***
             * 通过微信sdk的方式
             */
            share2(jo);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
        }
        return true;
    }


    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        m_context = this.cordova.getActivity().getApplicationContext();

        //int iconResID = m_context.getResources().getIdentifier(iconname,"drawable", m_context.getPackageName());
        api = WXAPIFactory.createWXAPI(this.cordova.getActivity(),APP_ID);
        boolean reuslt =  api.registerApp(APP_ID);
        super.initialize(cordova, webView);
    }

    public void share2( JSONObject jo){
        int how =1;
        String title = null;
        String description = null;
        String url = null;
        String thumbBmp = null;
        try {
            how = jo.getInt("how");
            title = jo.getString("title");
            description = jo.getString("description");
            url = jo.getString("url");
            thumbBmp = jo.getString("thumbBmp");
            if(how ==1){
                mTargetScene = SendMessageToWX.Req.WXSceneSession;
            }else{
                mTargetScene = SendMessageToWX.Req.WXSceneTimeline;
            }
            sendLink(title,description,url,thumbBmp);

        } catch (JSONException e) {
            e.printStackTrace();
            currCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
        }
    }
    //发送到微信，
    public void sendLink (String title,String description,String url,String thumbBmpName) {
        boolean t1;
        try {
            WXWebpageObject webpage = new WXWebpageObject();
            webpage.webpageUrl = url;
            WXMediaMessage msg = new WXMediaMessage(webpage);
            msg.title = title;
            msg.description = description;

//            Resources tmp = m_context.getResources();
//            int iconResID = m_context.getResources().getIdentifier("send_music_thumb", "drawable", m_context.getPackageName());
//            Bitmap bmp = BitmapFactory.decodeResource(m_context.getResources(), iconResID);
//            Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, 150, 150, true);
//            bmp.recycle();

            //直接就将图像给缩放了
            Bitmap bmp= BitmapFactory.decodeFile(thumbBmpName);
            Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, 150, 150, true);
            bmp.recycle();

            msg.thumbData = Util.bmpToByteArray(thumbBmp, true);

            SendMessageToWX.Req req = new SendMessageToWX.Req();
            req.transaction = buildTransaction("webpage");
            req.message = msg;
            req.scene = mTargetScene;
            t1 = api.sendReq(req);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }
}
