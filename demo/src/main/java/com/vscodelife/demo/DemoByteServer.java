package com.vscodelife.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.server.TestByteServer;

public class DemoByteServer {
    private static final Logger logger = LoggerFactory.getLogger(DemoByteServer.class);

    public static void main(String[] args) {
        TestByteServer server = new TestByteServer(30001, 100);
        server.bind();

        logger.info("start byte socket start.");
    }
}
