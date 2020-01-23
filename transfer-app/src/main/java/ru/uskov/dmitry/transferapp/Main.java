package ru.uskov.dmitry.transferapp;

import ru.uskov.dmitry.transferapp.config.TransferAppConfig;


public class Main {
    public static void main(String[] args) {
        TransferAppConfig cfg = new TransferAppConfig()
                .serverPort(8085)
                .serverExecutorPoolSize(2)
                .httpContextPath("/transfer-app");
        TransferApp transferApp = new TransferApp(cfg);
        transferApp.start();
    }
}
