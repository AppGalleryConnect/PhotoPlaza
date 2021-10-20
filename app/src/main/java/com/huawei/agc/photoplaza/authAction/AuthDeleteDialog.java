package com.huawei.agc.photoplaza.authAction;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import com.huawei.agc.photoplaza.R;
/**
 * Password text box for Delete of a user
 *
 * @author x00454024
 * @since 2021-10-13
 * <p>
 * Copyright (c) HuaWei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */

public class AuthDeleteDialog extends Dialog implements View.OnClickListener {

    private EditText mEtPwdReal;

    public interface PasswordCallback {
        void callback(String password);
    }

    public PasswordCallback mPasswordCallback;

    public AuthDeleteDialog(Context context) {
        super(context);
        initDialog();
    }

    void initDialog() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.password_edit_dialog);
        setCancelable(false);
        setCanceledOnTouchOutside(false);

        findViewById(R.id.btn_cancel).setOnClickListener(this);
        findViewById(R.id.btn_confirm).setOnClickListener(this);
        mEtPwdReal = findViewById(R.id.et_passwd);

    }

    private void requestFocus() {
        mEtPwdReal.requestFocus();
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    /**
     * 重制
     */
    public void clearPasswordText() {
        mEtPwdReal.setText("");
        requestFocus();
    }


    /**
     * 设置回调
     *
     */
    public void setPasswordCallback(PasswordCallback passwordCallback) {
        this.mPasswordCallback = passwordCallback;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_cancel:
                dismiss();
                break;
            case R.id.btn_confirm:
                String mPassword = mEtPwdReal.getText().toString();
                mPasswordCallback.callback(mPassword);
                break;
        }
    }
}
