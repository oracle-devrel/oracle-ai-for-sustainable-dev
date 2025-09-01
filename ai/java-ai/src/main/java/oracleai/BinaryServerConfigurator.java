package oracleai;

import jakarta.websocket.server.ServerEndpointConfig;

public class BinaryServerConfigurator extends ServerEndpointConfig.Configurator {
    @Override
    public boolean checkOrigin(String originHeaderValue) {
        System.out.println("✅ WebSocket checkOrigin originHeaderValue: " + originHeaderValue);
        return true; // Allow all origins for WebSocket
    }
}
