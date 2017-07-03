package org.jboss.aerogear.proxy.endpoint.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author <a href="mailto:miklosovic@gmail.com">Stefan Miklosovic</a>
 */
public class NotificationRegisterResponseHelper {

    public static List<String> getDeviceTokens(final NotificationRegisterResponse notificationRegisterResponse) {

        final List<String> deviceTokens = new ArrayList();


        for (final ApnsNotification apnsNotification : notificationRegisterResponse.getApnsNotifications()) {
            deviceTokens.add(apnsNotification.getDeviceToken());
        }

        for (final FCMNotification fcmNotification : notificationRegisterResponse.getFcmNotifications()) {
            deviceTokens.addAll(fcmNotification.getDeviceTokens());
        }

        return deviceTokens;
    }

    public static NotificationRegisterResponse merge(NotificationRegisterResponse... responses) {

        NotificationRegisterResponse mergedNotificationRegisterResponse = new NotificationRegisterResponse();

        for (final NotificationRegisterResponse response : responses) {
            mergedNotificationRegisterResponse.getApnsNotifications().addAll(response.getApnsNotifications());
            mergedNotificationRegisterResponse.getFcmNotifications().addAll(response.getFcmNotifications());
        }

        return mergedNotificationRegisterResponse;
    }
}