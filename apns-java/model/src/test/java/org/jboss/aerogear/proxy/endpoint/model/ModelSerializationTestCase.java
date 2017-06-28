package org.jboss.aerogear.proxy.endpoint.model;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.gson.Gson;

@RunWith(JUnit4.class)
public class ModelSerializationTestCase {

    @Test
    public void apnsNotificationSerializationTest() {
        ApnsNotification notification = new ApnsNotification(1, "token", "payload");

        String marshalled = notification.toString();

        ApnsNotification demarshalled = new Gson().fromJson(marshalled, ApnsNotification.class);

        Assert.assertEquals(notification, demarshalled);
    }

    @Test
    public void fcmNotificationSerializationTest() {
        FCMNotification notification = new FCMNotification();

        notification.setData(new HashMap<String, String>() {
            {
                put("someKey1", "someValue1");
                put("someKey2", "someValue2");
                put("someKey3", "someValue3");
            }
        });

        notification.setRegistrationIds(new ArrayList<String>() {
            {
                add("someToken1");
                add("someToken2");
                add("someToken3");
            }
        });

        notification.setCollapseKey("collapseKey");
        notification.setDelayWhileIdle(false);
        notification.setTimeToLive(500);

        String marshalled = notification.toString();

        FCMNotification demarshalled = new Gson().fromJson(marshalled, FCMNotification.class);

        Assert.assertEquals(notification, demarshalled);
    }
}
