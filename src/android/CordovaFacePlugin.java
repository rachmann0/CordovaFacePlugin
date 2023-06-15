package pluginid;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContextWrapper;
import android.app.Activity;
import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Build;
import android.content.pm.PackageManager;
import android.content.Context;
import android.telecom.Call;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;

import mcv.facepass.FacePassException;
import mcv.facepass.FacePassHandler;
import mcv.facepass.auth.AuthApi.AuthApi;
import mcv.facepass.auth.AuthApi.AuthApplyResponse;
import mcv.facepass.auth.AuthApi.ErrorCodeConfig;
import mcv.facepass.types.FacePassAddFaceResult;
import mcv.facepass.types.FacePassConfig;
import mcv.facepass.types.FacePassDetectionResult;
import mcv.facepass.types.FacePassFace;
import mcv.facepass.types.FacePassGroupSyncDetail;
import mcv.facepass.types.FacePassImage;
import mcv.facepass.types.FacePassImageRotation;
import mcv.facepass.types.FacePassImageType;
import mcv.facepass.types.FacePassModel;
import mcv.facepass.types.FacePassPose;
import mcv.facepass.types.FacePassRCAttribute;
import mcv.facepass.types.FacePassRecognitionResult;
import mcv.facepass.types.FacePassAgeGenderResult;
import mcv.facepass.types.FacePassRecognitionState;
import mcv.facepass.types.FacePassTrackOptions;



/**
 * This class echoes a string called from JavaScript.
 */
public class CordovaFacePlugin extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("initializeSDK")) {
            this.initializeSDK(callbackContext);
            return true;
        }
        if (action.equals("createGroup")) {
            String groupName = args.getString(0);
            createGroup(groupName, callbackContext);
            return true;
        }
        if (action.equals("addFace")) {
            String bitmapBase64 = args.getString(0);
            addFace(bitmapBase64, callbackContext);
            return true;
        }
        if (action.equals("bindGroupFaceToken")) {
            String groupName = args.getString(0);
            String faceTokenStr = args.getString(1);
            bindGroupFaceToken(groupName, faceTokenStr, callbackContext);
            return true;
        }
        return false;
    }

    private void initializeSDK(CallbackContext callbackContext) {
        if (!hasPermission()) {
            requestPermission();
        } else {
            try {
                initFacePassSDK();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        initFaceHandler(callbackContext);
        //callbackContext.error("Expected one non-empty string argument.");
    }

    private static final String TAG = "MyActivity";
    private static final String DEBUG_TAG = "FacePassDemo";
    private boolean ageGenderEnabledGlobal;
    private enum FacePassCameraType{
        FACEPASS_SINGLECAM,
        FACEPASS_DUALCAM
    };
    private static FacePassCameraType CamType = FacePassCameraType.FACEPASS_SINGLECAM;
    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_WRITE_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
/*
    private static final String PERMISSION_READ_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE;
*/
    private static final String PERMISSION_READ_STORAGE = Manifest.permission.READ_MEDIA_IMAGES;
    private static final String PERMISSION_INTERNET = Manifest.permission.INTERNET;
    private static final String PERMISSION_ACCESS_NETWORK_STATE = Manifest.permission.ACCESS_NETWORK_STATE;
    private String[] Permission = new String[]{PERMISSION_CAMERA, PERMISSION_WRITE_STORAGE, PERMISSION_READ_STORAGE, PERMISSION_INTERNET, PERMISSION_ACCESS_NETWORK_STATE};

    /* SDK 实例对象 */
    FacePassHandler mFacePassHandler;

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return true;
/*
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED &&
*/
/*
                    checkSelfPermission(PERMISSION_READ_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(PERMISSION_WRITE_STORAGE) == PackageManager.PERMISSION_GRANTED &&
*//*

                    checkSelfPermission(PERMISSION_INTERNET) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(PERMISSION_ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED;
*/
        } else {
            return true;
        }
    }
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cordova.getActivity().requestPermissions(Permission, PERMISSIONS_REQUEST);
        }
    }

    public static final String CERT_PATH = "Cert/CBG_Android_Face_Reco---30-Trial-one-stage.cert";
    private void singleCertification(Context mContext) throws IOException {
/*
        String cert = FileUtil.readExternal(CERT_PATH).trim();
*/
        String cert = "\"{\"\"serial\"\":\"\"z0005a8759f61d5f4b2862852034c139ddada\"\",\"\"key\"\":\"\"2a6a6e824b1bfb87553faecb38faf4122936055c915fb3ac814c8879994d1542f304999e02ec2ff25d278b110695b76980b3002d57d2f4f20d779f2ffc95e1bac4ff713f244ad0d7da10a0491ee0fbfce6c9ee0f4a8fd42f0fb17ef56070773c73272014a60096f06154620fa427ea3b0dbace0ec3d7a9b59e4cb9775da41275d6fe6b904539f59910ad012bc89dc86d3fd43af436040a036375767226261a30e9d05e87c89f821b9875da230409f7d66748bcfc9f8281cf802305a8664739f3354a3d13565b16ce\"\"}\"\n".trim();
        if(TextUtils.isEmpty(cert)){
            Log.d("mcvsafe", "cert is null");
            return;
        }
        final AuthApplyResponse[] resp = {new AuthApplyResponse()};
        FacePassHandler.authDevice(mContext.getApplicationContext(), cert, "", new AuthApi.AuthDeviceCallBack() {
            @Override
            public void GetAuthDeviceResult(AuthApplyResponse result) {
                resp[0] = result;
                if (resp[0].errorCode == ErrorCodeConfig.AUTH_SUCCESS) {
                    try {
                        cordova.getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("mcvsafe", "Apply update: OK");
                            }

                        });
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                } else {
                    try {
                        cordova.getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("mcvsafe", "Apply update: error. error code is: " + resp[0].errorCode + " error message: " + resp[0].errorMessage);
                            }
                        });
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                }
            }
        });
    }
    private static final String authIP = "https://api-cn.faceplusplus.com";
    public static final String apiKey = "";
    public static final String apiSecret = "";
    private enum FacePassAuthType{
        FASSPASS_AUTH_MCVFACE,
        FACEPASS_AUTH_MCVSAFE
    };
    private static FacePassAuthType authType = FacePassAuthType.FACEPASS_AUTH_MCVSAFE;
    private void initFacePassSDK() throws IOException {
        Log.d(DEBUG_TAG, "initFacePassSDK");

        Context mContext = cordova.getContext().getApplicationContext();
        FacePassHandler.initSDK(mContext);
        if (authType == FacePassAuthType.FASSPASS_AUTH_MCVFACE) {
            // face++授权
            FacePassHandler.authPrepare(cordova.getContext().getApplicationContext());
            FacePassHandler.getAuth(authIP, apiKey, apiSecret, true);
        } else if (authType == FacePassAuthType.FACEPASS_AUTH_MCVSAFE) {
            Log.d(DEBUG_TAG, "authType = FACEPASS_AUTH_MCVSAFE");
            // 金雅拓授权接口
            boolean auth_status = FacePassHandler.authCheck();
            Log.d(DEBUG_TAG, "FacePassHandler.authCheck(): " + FacePassHandler.authCheck());
            if ( !auth_status ) {
                singleCertification(mContext);
                auth_status = FacePassHandler.authCheck();
            }

            if ( !auth_status ) {
                Log.d(DEBUG_TAG, "Authentication result : failed.");
                Log.d("mcvsafe", "Authentication result : failed.");
                // 授权不成功，根据业务需求处理
                // ...
                return ;
            }
        } else {
            Log.d(DEBUG_TAG, "have no auth.");
            Log.d("FacePassDemo", "have no auth.");
            return ;
        }

        Log.d(DEBUG_TAG, "FacePassHandler.getVersion() = " + FacePassHandler.getVersion());
    }

    private void initFaceHandler(CallbackContext callbackContext) {
        Log.d(DEBUG_TAG, "initFaceHandler");
        new Thread() {
            @Override
            public void run() {
                while (true && !cordova.getActivity().isFinishing()) {
//                while (!isFinishing()) {
                    Log.d(DEBUG_TAG, "FacePassHandler.isAvailable() = " + String.valueOf(FacePassHandler.isAvailable()));
                    while (FacePassHandler.isAvailable()) {
                        Log.d(DEBUG_TAG, "start to build FacePassHandler");
                        FacePassConfig config;
                        try {

                            config = new FacePassConfig();
                            config.poseBlurModel = FacePassModel.initModel(cordova.getActivity().getApplicationContext().getAssets(), "attr.pose_blur.arm.190630.bin");

                            config.livenessModel = FacePassModel.initModel(cordova.getActivity().getApplicationContext().getAssets(), "liveness.CPU.rgb.G.bin");
                            if (CamType == FacePassCameraType.FACEPASS_DUALCAM) {
                                config.rgbIrLivenessModel = FacePassModel.initModel(cordova.getActivity().getApplicationContext().getAssets(), "liveness.CPU.rgbir.I.bin");
                                // 真假人同屏模型
                                config.rgbIrGaLivenessModel = FacePassModel.initModel(cordova.getContext().getApplicationContext().getAssets(), "liveness.CPU.rgbir.ga_case.A.bin");
                                // 若需要使用GPU模型则加载以下模型文件
                                config.livenessGPUCache = FacePassModel.initModel(cordova.getContext().getApplicationContext().getAssets(), "liveness.GPU.rgbir.I.cache");
                                config.rgbIrLivenessGpuModel = FacePassModel.initModel(cordova.getContext().getApplicationContext().getAssets(), "liveness.GPU.rgbir.I.bin");
                                config.rgbIrGaLivenessGpuModel = FacePassModel.initModel(cordova.getContext().getApplicationContext().getAssets(), "liveness.GPU.rgbir.ga_case.A.bin");
                            }

                            config.searchModel = FacePassModel.initModel(cordova.getContext().getApplicationContext().getAssets(), "feat2.arm.K.v1.0_1core.bin");

                            config.detectModel = FacePassModel.initModel(cordova.getContext().getApplicationContext().getAssets(), "detector.arm.G.bin");
                            config.detectRectModel = FacePassModel.initModel(cordova.getContext().getApplicationContext().getAssets(), "detector_rect.arm.G.bin");
                            config.landmarkModel = FacePassModel.initModel(cordova.getContext().getApplicationContext().getAssets(), "pf.lmk.arm.E.bin");

                            config.rcAttributeModel = FacePassModel.initModel(cordova.getContext().getApplicationContext().getAssets(), "attr.RC.arm.G.bin");
                            config.occlusionFilterModel = FacePassModel.initModel(cordova.getContext().getApplicationContext().getAssets(), "attr.occlusion.arm.20201209.bin");
                            //config.smileModel = FacePassModel.initModel(getApplicationContext().getAssets(), "attr.RC.arm.200815.bin");


                            config.rcAttributeAndOcclusionMode = 1;
                            config.searchThreshold = 65f;
                            config.livenessThreshold = 80f;
                            config.livenessGaThreshold = 85f;
                            if (CamType == FacePassCameraType.FACEPASS_DUALCAM) {
                                config.livenessEnabled = false;
                                config.rgbIrLivenessEnabled = true;      // 启用双目活体功能(默认CPU)
                                config.rgbIrLivenessGpuEnabled = true;   // 启用双目活体GPU功能
                                config.rgbIrGaLivenessEnabled = true;    // 启用真假人同屏功能(默认CPU)
                                config.rgbIrGaLivenessGpuEnabled = true; // 启用真假人同屏GPU功能
                            } else {
                                config.livenessEnabled = true;
                                config.rgbIrLivenessEnabled = false;
                            }

                            ageGenderEnabledGlobal = (config.ageGenderModel != null);

                            config.poseThreshold = new FacePassPose(35f, 35f, 35f);
                            config.blurThreshold = 0.8f;
                            config.lowBrightnessThreshold = 30f;
                            config.highBrightnessThreshold = 210f;
                            config.brightnessSTDThreshold = 80f;
                            config.faceMinThreshold = 60;
                            config.retryCount = 10;
                            config.smileEnabled = false;
                            config.maxFaceEnabled = true;
                            config.fileRootPath = cordova.getContext().getExternalFilesDir("Download").getAbsolutePath();
/*
                            config.fileRootPath = "/storage/emulated/0/Android/dataHelloCordova/files/Download";
*/


                            mFacePassHandler = new FacePassHandler(config);


                            FacePassConfig addFaceConfig = mFacePassHandler.getAddFaceConfig();
                            addFaceConfig.poseThreshold.pitch = 35f;
                            addFaceConfig.poseThreshold.roll = 35f;
                            addFaceConfig.poseThreshold.yaw = 35f;
                            addFaceConfig.blurThreshold = 0.7f;
                            addFaceConfig.lowBrightnessThreshold = 70f;
                            addFaceConfig.highBrightnessThreshold = 220f;
                            addFaceConfig.brightnessSTDThresholdLow = 14.14f;
                            addFaceConfig.brightnessSTDThreshold = 63.25f;
                            addFaceConfig.faceMinThreshold = 100;
                            addFaceConfig.rcAttributeAndOcclusionMode = 2;
                            mFacePassHandler.setAddFaceConfig(addFaceConfig);

                            checkGroup();



                        } catch (FacePassException e) {
                            e.printStackTrace();
//                            Log.d(DEBUG_TAG, "FacePassHandler is null");
                            // Log.d(DEBUG_TAG, "FacePassHandler is null: " + e.getMessage());
                            callbackContext.error("FacePassHandler is null: " + e.getMessage());
                            return;
                        }
                        // Log.d(DEBUG_TAG, "SDK successfully initialized");
                        callbackContext.success("SDK successfully initialized");
                        return;
                    }
                    try {

                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private boolean isLocalGroupExist = false;
    private static final String group_name = "facepass";
    private void checkGroup() {
        Log.d(DEBUG_TAG, "checkGroup");
        if (mFacePassHandler == null) {
            return;
        }
        try {
            String[] localGroups = mFacePassHandler.getLocalGroups();
            isLocalGroupExist = false;
            if (localGroups == null || localGroups.length == 0) {
                Log.d(DEBUG_TAG, group_name);
                return;
            }
            for (String group : localGroups) {
                if (group_name.equals(group)) {
                    isLocalGroupExist = true;
                }
            }
            if (!isLocalGroupExist) {
                Log.d(DEBUG_TAG, group_name);
            }
        } catch (FacePassException e) {
            e.printStackTrace();
        }
    }

    public void createGroup(String groupName, CallbackContext callbackContext) {
        if (mFacePassHandler == null) {
            Log.d(DEBUG_TAG, "FacePassHandle is null ! ");
            return;
        }
        if (TextUtils.isEmpty(groupName)) {
            Log.d(DEBUG_TAG,"please input group name ！");
            return;
        }
        boolean isSuccess = false;
        try {
            isSuccess = mFacePassHandler.createLocalGroup(groupName);
        } catch (FacePassException e) {
            e.printStackTrace();
        }
        Log.d(DEBUG_TAG,"create group " + isSuccess);
        callbackContext.success("create group " + groupName + " " + isSuccess);
        if (isSuccess && group_name.equals(groupName)) {
            isLocalGroupExist = true;
        }

    }

    private static byte[] faceToken;
    public void addFace(String bitmapBase64, CallbackContext callbackContext) {
        byte[] decodedString = Base64.decode(bitmapBase64, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

        if (mFacePassHandler == null) {
            Log.d(DEBUG_TAG,"FacePassHandle is null !");
            callbackContext.error("FacePassHandle is null !");
            return;
        }

        try {
            FacePassAddFaceResult result = mFacePassHandler.addFace(bitmap);
            if (result != null) {
                if (result.result == 0) {
                    android.util.Log.d("qujiaqi", "result:" + result
                            + ",bl:" + result.blur
                            + ",pp:" + result.pose.pitch
                            + ",pr:" + result.pose.roll
                            + ",py" + result.pose.yaw);
                    callbackContext.success(new String(result.faceToken));
                    faceToken = result.faceToken;
                    Log.d(DEBUG_TAG, "add face successfully! ");
                    Log.d(DEBUG_TAG, new String(result.faceToken));

                } else if (result.result == 1) {
                    callbackContext.success("no face ！");
                } else {
                    callbackContext.success("quality problem！");
                }
            }
        } catch (FacePassException e) {
            e.printStackTrace();
            callbackContext.error(e.getMessage());
        }
    }

    public void bindGroupFaceToken(String groupName, String faceTokenStr, CallbackContext callbackContext) {
        byte[] faceToken = faceTokenStr.getBytes();

        if (mFacePassHandler == null) {
            callbackContext.error("FacePassHandle is null ! ");
            return;
        }

        if (faceToken == null || faceToken.length == 0 || TextUtils.isEmpty(groupName)) {
            callbackContext.error("params error！");
            return;
        }
        try {
            boolean b = mFacePassHandler.bindGroup(groupName, faceToken);
            String result = b ? "success " : "failed";
            callbackContext.success("bind  " + result);
        } catch (Exception e) {
            e.printStackTrace();
            callbackContext.error(e.getMessage());
        }


    }

}
