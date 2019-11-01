package com.rnfingerprint;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

@SuppressLint("ValidFragment")
public class FingerprintDialog extends DialogFragment implements FingerprintHandler.Callback {

    private FingerprintManager.CryptoObject mCryptoObject;
    private DialogResultListener dialogCallback;
    private FingerprintHandler mFingerprintHandler;
    private boolean isAuthInProgress;

    private ImageView mFingerprintImage;
    private TextView mFingerprintSensorDescription;
    private TextView mFingerprintError;

    private String authReason;
    private int imageColor = 0;
    private int imageErrorColor = 0;
    private String dialogTitle = "";
    private String cancelText = "";
    private String sensorDescription = "";
    private String sensorErrorDescription = "";
    private String errorText = "";
    private ReactContext mReactContext;

    @SuppressLint("ValidFragment")
    public FingerprintDialog(ReactContext mContext) {
        this.mReactContext = mContext;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mFingerprintHandler = new FingerprintHandler(context, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }
    @Override
    public void onStart() {
        super.onStart();
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics( dm );
        getDialog().getWindow().setLayout( dm.widthPixels,  getDialog().getWindow().getAttributes().height );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fingerprint_dialog, container, false);
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

        this.mFingerprintError = (TextView) v.findViewById(R.id.fingerprint_error);
        TextView pwd_button = v.findViewById(R.id.pwd_button);
        pwd_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendEvent(mReactContext, "PASS_AUTH", null);
                onCancelled();
            }
        });
        final RelativeLayout mCancelButton = (RelativeLayout) v.findViewById(R.id.cancel_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancelled();
            }
        });
        getDialog().setTitle(this.dialogTitle);
        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode != KeyEvent.KEYCODE_BACK || mFingerprintHandler == null) {
                    return false; // pass on to be processed as normal
                }
                onCancelled();
                return true; // pretend we've processed it
            }
        });

        return v;
    }

    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }
    @Override
    public void onResume() {
        super.onResume();

        if (this.isAuthInProgress) {
            return;
        }

        this.isAuthInProgress = true;
        this.mFingerprintHandler.startAuth(mCryptoObject);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.isAuthInProgress) {
            this.mFingerprintHandler.endAuth();
            this.isAuthInProgress = false;
        }
    }


    public void setCryptoObject(FingerprintManager.CryptoObject cryptoObject) {
        this.mCryptoObject = cryptoObject;
    }

    public void setDialogCallback(DialogResultListener newDialogCallback) {
        this.dialogCallback = newDialogCallback;
    }

    public void setReasonForAuthentication(String reason) {
        this.authReason = reason;
    }

    public void setAuthConfig(final ReadableMap config) {
        if (config == null) {
            return;
        }

        if (config.hasKey("title")) {
            this.dialogTitle = config.getString("title");
        }

        if (config.hasKey("cancelText")) {
            this.cancelText = config.getString("cancelText");
        }

        if (config.hasKey("sensorDescription")) {
            this.sensorDescription = config.getString("sensorDescription");
        }

        if (config.hasKey("sensorErrorDescription")) {
            this.sensorErrorDescription = config.getString("sensorErrorDescription");
        }

        if (config.hasKey("imageColor")) {
            this.imageColor = config.getInt("imageColor");
        }

        if (config.hasKey("imageErrorColor")) {
            this.imageErrorColor = config.getInt("imageErrorColor");
        }
    }

    public interface DialogResultListener {
        void onAuthenticated();

        void onError(String errorString, int errorCode);

        void onCancelled();
    }

    @Override
    public void onAuthenticated() {
        this.isAuthInProgress = false;
        this.dialogCallback.onAuthenticated();
        dismiss();
    }

    @Override
    public void onError(String errorString, int errorCode) {
        this.mFingerprintError.setText(errorString);
    }

    @Override
    public void onCancelled() {
        this.isAuthInProgress = false;
        this.mFingerprintHandler.endAuth();
        this.dialogCallback.onCancelled();
        dismiss();
    }
}
