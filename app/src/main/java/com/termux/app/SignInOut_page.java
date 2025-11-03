package com.termux.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.termux.R;
import com.termux.SignInFragment;
import com.termux.SignUpFragment;

public class SignInOut_page extends AppCompatActivity {

    private static final String REQUEST_KEY_AUTH_NAV = "auth_nav";
    private static final String NAV_KEY = "nav";
    private static final String NAV_TO_SIGN_IN = "toSignIn";
    private static final String NAV_TO_SIGN_UP = "toSignUp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signinout_page);

        getSupportFragmentManager().setFragmentResultListener(REQUEST_KEY_AUTH_NAV, this, (requestKey, bundle) -> {
            String nav = bundle.getString(NAV_KEY);
            if (NAV_TO_SIGN_IN.equals(nav)) {
                navigateToSignIn();
            } else if (NAV_TO_SIGN_UP.equals(nav)) {
                navigateToSignUp();
            }
        });

        if (savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.SignFragment_container, new SignInFragment(), "SignIn");
            ft.commit();
        }
    }

    private void navigateToSignIn() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        SignInFragment fragment = new SignInFragment();
        fragmentTransaction.replace(R.id.SignFragment_container, fragment, "SignIn");
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void navigateToSignUp() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        SignUpFragment fragment = new SignUpFragment();
        fragmentTransaction.replace(R.id.SignFragment_container, fragment, "SignUp");
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}
