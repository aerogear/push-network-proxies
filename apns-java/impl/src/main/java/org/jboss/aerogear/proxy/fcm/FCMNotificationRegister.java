package org.jboss.aerogear.proxy.fcm;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.google.gson.Gson;
import org.jboss.aerogear.proxy.endpoint.model.FCMNotification;

public class FCMNotificationRegister {

    private static final Logger logger = Logger.getLogger(FCMNotificationRegister.class.getName());

    private static final List<FCMNotification> notifications = new ArrayList<FCMNotification>();

    public static synchronized void addNotification(FCMNotification notification) {
        notifications.add(notification);
    }

    public static synchronized List<FCMNotification> getNotifications() {
        return notifications;
    }

    public static synchronized void clear() {
        logger.info("CLEARING FCM NOIFICATION REGISTER");
        notifications.clear();
    }

    @Override
    public String toString() {
        return new Gson().toJson(notifications);
    }
}
