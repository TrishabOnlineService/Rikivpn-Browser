package com.rikivpn.browser.core;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks whether BongoVpn currently reports a connected tunnel, so features outside
 * {@link com.rikivpn.browser.VpnActivity} (namely the Kill Switch check in the browser's
 * request pipeline) can read live status without needing their own BongoVpn instance.
 * Updated from {@code VpnActivity}'s VpnListener callbacks only.
 */
public class VpnState {
    private static final AtomicBoolean connected = new AtomicBoolean(false);

    public static void setConnected(boolean value) {
        connected.set(value);
    }

    public static boolean isConnected() {
        return connected.get();
    }
}
