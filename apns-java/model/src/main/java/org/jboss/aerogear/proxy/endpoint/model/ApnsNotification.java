package org.jboss.aerogear.proxy.endpoint.model;

import com.google.gson.Gson;

public class ApnsNotification {

    private final int type;
    private final int identifier;
    private final int expiry;
    private final String deviceToken;
    private final String payload;
    private final byte priority;

    public ApnsNotification(final int type, final String deviceToken, final String payload) {
        this(type, 0, 0, deviceToken, payload);
    }

    public ApnsNotification(final int type, final int identifier, final int expiry, final String deviceToken, final String payload) {
        this(type, identifier, expiry, deviceToken, payload, (byte) 10);
    }

    public ApnsNotification(final int type, final int identifier, final int expiry, final String deviceToken, final String payload,
        final byte priority) {
        this.priority = priority;
        this.type = type;
        this.identifier = identifier;
        this.expiry = expiry;
        this.deviceToken = deviceToken;
        this.payload = payload;
    }

    public String getPayload() {
        return new String(payload);
    }

    public String getDeviceToken() {
        return new String(deviceToken);
    }

    public int getType() {
        return type;
    }

    public int getExpiry() {
        return expiry;
    }

    public int getIdentifier() {
        return identifier;
    }

    public byte getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((deviceToken == null) ? 0 : deviceToken.hashCode());
        result = prime * result + expiry;
        result = prime * result + identifier;
        result = prime * result + ((payload == null) ? 0 : payload.hashCode());
        result = prime * result + priority;
        result = prime * result + type;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ApnsNotification other = (ApnsNotification) obj;
        if (deviceToken == null) {
            if (other.deviceToken != null)
                return false;
        } else if (!deviceToken.equals(other.deviceToken))
            return false;
        if (expiry != other.expiry)
            return false;
        if (identifier != other.identifier)
            return false;
        if (payload == null) {
            if (other.payload != null)
                return false;
        } else if (!payload.equals(other.payload))
            return false;
        if (priority != other.priority)
            return false;
        if (type != other.type)
            return false;
        return true;
    }


}
