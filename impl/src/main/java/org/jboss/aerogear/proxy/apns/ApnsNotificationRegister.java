package org.jboss.aerogear.proxy.apns;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.jboss.aerogear.proxy.endpoint.model.ApnsNotification;

import com.google.gson.Gson;

public class ApnsNotificationRegister {

    private static final Logger logger = Logger.getLogger(ApnsNotificationRegister.class.getName());

    private static final List<ApnsNotification> notifications = new ArrayList<ApnsNotification>();

    public static synchronized void addNotification(ApnsNotification notification) {
        notifications.add(notification);
    }

    public static synchronized List<ApnsNotification> getNotifications() {
        return notifications;
    }

    public static synchronized void clear() {
        logger.info("CLEARING APNS REGISTER");
        notifications.clear();
    }

    @Override
    public String toString() {
        return new Gson().toJson(notifications);
    }
}
