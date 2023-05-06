package com.example.myapplication.common;

public interface UUIDs {

    byte[] AUTH_CHAR_KEY = new byte[]{
            (byte) 0x6b, (byte) 0xaf, (byte) 0xd8, (byte) 0x88, (byte) 0xe6, (byte) 0x03, (byte) 0xf3, (byte) 0x03,
            (byte) 0x61, (byte) 0xda, (byte) 0x7f, (byte) 0xb1, (byte) 0x94, (byte) 0x7e, (byte) 0x5d, (byte) 0x07
    };

    String SERVICE1 = "0000fee0-0000-1000-8000-00805f9b34fb";
    String SERVICE2 = "0000fee1-0000-1000-8000-00805f9b34fb";
    String SERVICE_HEART_RATE = "0000180d-0000-1000-8000-00805f9b34fb";

    String CHAR_AUTH = "00000009-0000-3512-2118-0009af100700";
    String CHAR_HEART_RATE_CONTROL = "00002a39-0000-1000-8000-00805f9b34fb";
    String CHAR_HEART_RATE_MEASURE = "00002a37-0000-1000-8000-00805f9b34fb";
    String CHAR_SENSOR = "00000001-0000-3512-2118-0009af100700";
    String CHAR_BATTERY = "00000006-0000-3512-2118-0009af100700";
    String CHAR_STEPS = "00000007-0000-3512-2118-0009af100700";

    String NOTIFICATION_DESC = "00002902-0000-1000-8000-00805f9b34fb";
}
