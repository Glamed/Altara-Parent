package games.sparking.altara.logging;

import games.sparking.altara.AltaraProxy;

public class VelocityLogger implements CommonLogger {

    @Override
    public void info(String msg) {
        AltaraProxy.getProxyLogger().info(msg);
    }

    @Override
    public void warn(String msg) {
        AltaraProxy.getProxyLogger().warn(msg);
    }

    @Override
    public void error(String msg) {
        AltaraProxy.getProxyLogger().error(msg);
    }
}