package com.termux.app;

import android.content.Intent;

public class SamsaraIntents {
    public static final String EXTRA_SAMSARA_MODE = "samsara_mode";
    public static final String EXTRA_SAMSARA_ENV = "samsara_env";
    public static final String ACTION_SAMSARA_OPEN_TERMUX = "com.termux.app.action.SAMSARA_OPEN_TERMUX";
    public static final String ACTION_SAMSARA_OPEN_ALPINE = "com.termux.app.action.SAMSARA_OPEN_ALPINE";

    public static final String ENV_TERMUX = "termux";
    public static final String ENV_ALPINE = "alpine";

    public static void putEnv(Intent intent, String env) {
        // #COMPLETION_DRIVE: Do not write legacy samsara_mode; rely on explicit env and actions
        // #SUGGEST_VERIFY: Launch Alpine/Termux via navbar; ensure older callers that still write samsara_mode are handled by getEnv()
        intent.putExtra(EXTRA_SAMSARA_ENV, env);
    }

    public static String getEnv(Intent intent) {
        if (intent == null || intent.getExtras() == null) return null;
        String env = intent.getExtras().getString(EXTRA_SAMSARA_ENV, null);
        if (env != null && !env.isEmpty()) return env;
        boolean legacy = intent.getExtras().getBoolean(EXTRA_SAMSARA_MODE, false);
        return legacy ? ENV_ALPINE : null;
    }

    public static boolean isOpenTermuxAction(Intent intent) {
        return intent != null && ACTION_SAMSARA_OPEN_TERMUX.equals(intent.getAction());
    }

    public static boolean isOpenAlpineAction(Intent intent) {
        return intent != null && ACTION_SAMSARA_OPEN_ALPINE.equals(intent.getAction());
    }
}
