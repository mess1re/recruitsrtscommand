package me.mss1r.recruitsrtscommand.api;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

final class RTSCommandApiLog {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> COMPLAINED = new HashSet<>();

    private RTSCommandApiLog() {
    }

    static void providerFailed(MapObjectProvider provider, RuntimeException failure) {
        complain("Map object provider", provider, failure);
    }

    static void listenerFailed(MapOrderListener listener, RuntimeException failure) {
        complain("Map order listener", listener, failure);
    }

    private static void complain(String what, Object who, RuntimeException failure) {
        String name = who.getClass().getName();
        if (COMPLAINED.add(name)) {
            LOGGER.error("{} {} failed and will be complained about no further", what, name, failure);
        }
    }
}
