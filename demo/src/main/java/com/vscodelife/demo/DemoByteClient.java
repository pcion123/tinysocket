package com.vscodelife.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.client.TestByteClient;

public class DemoByteClient {
    private static final Logger logger = LoggerFactory.getLogger(DemoByteClient.class);

    @SuppressWarnings("static-access")
    public static void main(String[] args) {
        TestByteClient client = new TestByteClient();
        client.connect("127.0.0.1", 30001);
        logger.info("start byte client start.");
        while (client.isConnecting() || client.isConnected()) {
            // Do something
            try {
                Thread.currentThread().sleep(1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        client.shutdown();
        logger.info("start byte client shutdown.");
    }
}
