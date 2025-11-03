package com.termux;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SignUpFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String param1;
    private String param2;

    public static SignUpFragment newInstance(String param1, String param2) {
        SignUpFragment fragment = new SignUpFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            param1 = arguments.getString(ARG_PARAM1);
            param2 = arguments.getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.signup_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageButton loginBtn = view.findViewById(R.id.LoginBtn);
        ImageButton loginBtn2 = view.findViewById(R.id.LoginBtn2);

        View.OnClickListener toSignIn = v -> {
            Bundle result = new Bundle();
            result.putString("nav", "toSignIn");
            getParentFragmentManager().setFragmentResult("auth_nav", result);
        };

        if (loginBtn != null) loginBtn.setOnClickListener(toSignIn);
        if (loginBtn2 != null) loginBtn2.setOnClickListener(toSignIn);
    }
}
