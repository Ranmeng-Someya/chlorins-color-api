package com.chlorins.colorapi.client;

/** Flip-flop behind the debug key. */
public final class DebugState {

    private static volatile boolean enabled;

    private DebugState() {
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void toggle() {
        enabled = !enabled;
    }
}
