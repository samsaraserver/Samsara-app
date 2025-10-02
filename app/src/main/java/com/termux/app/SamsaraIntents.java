package com.termux.app;

import android.content.Intent;

public class SamsaraIntents {
    public static final String EXTRA_SAMSARA_MODE = "samsara_mode";
    public static final String EXTRA_SAMSARA_ENV = "samsara_env";

    public static final String ENV_TERMUX = "termux";
    public static final String ENV_ALPINE = "alpine";

    public static void putEnv(Intent intent, String env) {
        intent.putExtra(EXTRA_SAMSARA_ENV, env);
        // #COMPLETION_DRIVE: Only set legacy flag true for Alpine for backward compatibility
        // #SUGGEST_VERIFY: Verify Termux selection no longer invokes Alpine path in TermuxActivity
        intent.putExtra(EXTRA_SAMSARA_MODE, ENV_ALPINE.equals(env));
    }

    public static String getEnv(Intent intent) {
        if (intent == null || intent.getExtras() == null) return null;
        String env = intent.getExtras().getString(EXTRA_SAMSARA_ENV, null);
        if (env != null && !env.isEmpty()) return env;
        boolean legacy = intent.getExtras().getBoolean(EXTRA_SAMSARA_MODE, false);
        return legacy ? ENV_ALPINE : null;
    }
}
