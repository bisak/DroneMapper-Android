package com.dronemapper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.TypefaceProvider;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import static com.dronemapper.util.Helper.showToast;

public class LoginActivity extends Activity implements View.OnClickListener {
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private EditText emailEditText;
    private EditText passwordEditText;
    private BootstrapButton registerBtn;
    private BootstrapButton loginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUI();
        initFirebase();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.register_button: {
                openWebApp();
                break;
            }
            case R.id.login_button: {
                loginUser();
                break;
            }
            default:
                break;
        }
    }

    private void loginUser(){
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            showToast(getApplicationContext(), "Login Successful");
                        } else {
                            showToast(getApplicationContext(), "Login Error.");
                        }
                    }
                });
    }

    private void openWebApp(){
        Uri uri = Uri.parse("http://dronemapper.sliven.org/");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    private void initUI() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        TypefaceProvider.registerDefaultIconSets();

        setContentView(R.layout.activity_login);

        emailEditText = (EditText) findViewById(R.id.input_email);
        passwordEditText = (EditText) findViewById(R.id.input_password);
        registerBtn = (BootstrapButton) findViewById(R.id.register_button);
        loginBtn = (BootstrapButton) findViewById(R.id.login_button);
        registerBtn.setOnClickListener(this);
        loginBtn.setOnClickListener(this);
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Intent intent = new Intent(LoginActivity.this, EntryActivity.class);
                    startActivity(intent);
                }
            }
        };
    }
}
