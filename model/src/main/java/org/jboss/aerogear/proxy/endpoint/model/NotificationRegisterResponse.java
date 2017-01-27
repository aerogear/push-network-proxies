package org.jboss.aerogear.proxy.endpoint.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

/**
 *
 * @author <a href="mailto:miklosovic@gmail.com">Stefan Miklosovic</a>
 */
public class NotificationRegisterResponse {

    private List<ApnsNotification> apnsNotifications = new ArrayList();

    private List<FCMNotification> fcmNotifications = new ArrayList();

    public List<ApnsNotification> getApnsNotifications() {
        return apnsNotifications;
    }

    public void setApnsNotifications(List<ApnsNotification> apnsNotifications) {
        this.apnsNotifications = apnsNotifications;
    }

    public List<FCMNotification> getFcmNotifications() {
        return fcmNotifications;
    }

    public void setFcmNotifications(List<FCMNotification> fcmNotifications) {
        this.fcmNotifications = fcmNotifications;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
