package com.appacitive.sdk.push;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class RawNotification extends WindowsPhoneNotification implements Serializable {
        public RawNotification(String rawData) {
            super(WindowsPhoneNotificationType.Raw);
            this.rawData = rawData;
        }

        public String rawData;

        @Override
        public Map<String, Object> getMap()
        {
            return new HashMap<String, Object>(){{
                put("notificationtype", "raw");
                put("data", rawData);
            }};
        }
    }