package com.gill.travelmate;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.gill.travelmate.utils.FontHelper;
import com.gill.travelmate.utils.GeneralValues;
import com.gill.travelmate.utils.TinyDB;
import com.gill.travelmate.utils.Utils;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SigningActivity extends AppCompatActivity implements View.OnClickListener{

    LinearLayout ll_fb_signin;
    TextView tv_signup,tv_signin,title;
    Context mContext;
    TinyDB tinyDB;
    Animation animationFadeIn;
    CallbackManager callbackManager;
    LoginButton login_button;
    String fid="",fname="",femail="";
    Dialog dialog;
    Intent i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FacebookSdk.sdkInitialize(getApplicationContext());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signing);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();

        FontHelper.applyFont(this, findViewById(R.id.container_signing), "bauhaus.ttf");

        mContext=SigningActivity.this;
        tinyDB=new TinyDB(mContext);
        dialog=Utils.get_progressDialog(mContext);

        callbackManager = CallbackManager.Factory.create();

        initialize_views();
        set_listener();

        login_button.setReadPermissions(Arrays.asList("email", "public_profile", "user_friends"));
        login_button.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {

                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        if (response.getError() == null) {
                            femail = object.optString("email");
                            fid = object.optString("id");
                            fname = object.optString("name");

                            api_facebook_login();
                        } else {
                            Utils.showToast(getApplicationContext(),getString(R.string.can_not_login));
                        }
                    }
                });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,gender, birthday");
                request.setParameters(parameters);
                request.executeAsync();
            }

            @Override
            public void onCancel() {
                Utils.showToast(getApplicationContext(),getString(R.string.cancelled));
            }

            @Override
            public void onError(FacebookException error) {
                Utils.showToast(getApplicationContext(),getString(R.string.there_is_an_error));
                Utils.show_log("Error : "+error.getMessage().toString());
            }
        });
    }

    public void initialize_views(){
        login_button = (LoginButton) findViewById(R.id.login_button);

        ll_fb_signin=(LinearLayout)findViewById(R.id.ll_fb_signin);
        tv_signin=(TextView)findViewById(R.id.tv_signin);
        tv_signup=(TextView)findViewById(R.id.tv_signup);
        title=(TextView)findViewById(R.id.title);

        animationFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        title.setDrawingCacheEnabled(true);
    }

    public void set_listener(){
        ll_fb_signin.setOnClickListener(this);
        tv_signin.setOnClickListener(this);
        tv_signup.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        title.startAnimation(animationFadeIn);
        LoginManager.getInstance().logOut();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ll_fb_signin:
                if(Utils.isNetworkConnected(mContext)){
                    login_button.performClick();
                }else{
                    Utils.showToast(mContext,getString(R.string.no_internet_connection));
                }
                break;
            case R.id.tv_signin:
                startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                overridePendingTransition(R.anim.to_leftin, R.anim.to_leftout);
                break;
            case R.id.tv_signup:
                startActivity(new Intent(getApplicationContext(), SignUpActivity.class));
                overridePendingTransition(R.anim.to_leftin, R.anim.to_leftout);
                break;
            default:
                break;
        }
    }

    public void api_facebook_login(){
        try{
            dialog.show();
        }catch (Exception e){

        }
        HashMap<String,String> map=new HashMap<>();
        map.put("fID",""+fid);
        map.put("username",""+fname);
        map.put("email",""+femail);
        Call<ResponseBody> call = Utils.requestApi_Default().requestJson_withValues(GeneralValues.FACEBOOK_LOGIN, map);

        //Utils.show_log("url = "+call.request().url());

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    try{
                        dialog.dismiss();
                    }catch (Exception e){

                    }
                    String jsonResponse = response.body().string();
                    Log.e("res = ",jsonResponse);

                    JSONObject result = new JSONObject(jsonResponse);
                    String res = result.optString(getString(R.string.j_response));
                    String message = result.optString(getString(R.string.j_message));
                    if (res != null && res.equals("1")) {
                        JSONObject data=new JSONObject(result.getString("data"));
                        i=new Intent(mContext,SelectDestinationActivity.class);
                        i.putExtra("uid",""+data.getString("id"));
                        i.putExtra("uname",""+data.getString("username"));
                        startActivity(i);
                        overridePendingTransition(R.anim.to_leftin, R.anim.to_leftout);
                    }else{
                        LoginManager.getInstance().logOut();
                        Utils.showToast(getApplicationContext(), "" + message);
                    }
                } catch (Exception e) {
                    LoginManager.getInstance().logOut();
                    Utils.showToast(getApplicationContext(),getString(R.string.server_error));
                    Log.e("exception", "" + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Utils.showToast(getApplicationContext(), getString(R.string.server_not_responding));
                try{
                    dialog.dismiss();
                    LoginManager.getInstance().logOut();
                }catch (Exception e){

                }
            }
        });
    }
}
