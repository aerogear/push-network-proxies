package org.jboss.aerogear.proxy.endpoint.model;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author <a href="mailto:miklosovic@gmail.com">Stefan Miklosovic</a>
 */
public class NotificationRegisterResponseHelper {

    public static List<String> getDeviceTokens(final NotificationRegisterResponse notificationRegisterResponse) {

        final List<String> deviceTokens = new ArrayList<String>();

        for (final ApnsNotification apnsNotification : notificationRegisterResponse.getApnsNotifications()) {
            deviceTokens.add(apnsNotification.getDeviceToken());
        }

        for (final GCMNotification gcmNotification : notificationRegisterResponse.getGcmNotifications()) {
            deviceTokens.addAll(gcmNotification.getDeviceTokens());
        }

        return deviceTokens;
    }

    public static NotificationRegisterResponse merge(NotificationRegisterResponse... responses) {

        NotificationRegisterResponse mergedNotificationRegisterResponse = new NotificationRegisterResponse();

        for (final NotificationRegisterResponse response : responses) {
            mergedNotificationRegisterResponse.getApnsNotifications().addAll(response.getApnsNotifications());
            mergedNotificationRegisterResponse.getGcmNotifications().addAll(response.getGcmNotifications());
        }

        return mergedNotificationRegisterResponse;
    }
}