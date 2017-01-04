package org.jboss.aerogear.proxy.endpoint.model;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

public class GCMNotification {

    private Map<String, String> data;

    private String collapseKey;

    private Boolean delayWhileIdle;

    private Integer timeToLive;

    private List<String> registrationIds;

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    public String getCollapseKey() {
        return collapseKey;
    }

    public void setCollapseKey(String collapseKey) {
        this.collapseKey = collapseKey;
    }

    public Boolean getDelayWhileIdle() {
        return delayWhileIdle;
    }

    public void setDelayWhileIdle(Boolean delayWhileIdle) {
        this.delayWhileIdle = delayWhileIdle;
    }

    public Integer getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(Integer timeToLive) {
        this.timeToLive = timeToLive;
    }

    public List<String> getDeviceTokens() {
        return registrationIds;
    }

    public void setRegistrationIds(List<String> registrationIds) {
        this.registrationIds = registrationIds;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((collapseKey == null) ? 0 : collapseKey.hashCode());
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        result = prime * result + ((delayWhileIdle == null) ? 0 : delayWhileIdle.hashCode());
        result = prime * result + ((registrationIds == null) ? 0 : registrationIds.hashCode());
        result = prime * result + ((timeToLive == null) ? 0 : timeToLive.hashCode());
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
        GCMNotification other = (GCMNotification) obj;
        if (collapseKey == null) {
            if (other.collapseKey != null)
                return false;
        } else if (!collapseKey.equals(other.collapseKey))
            return false;
        if (data == null) {
            if (other.data != null)
                return false;
        } else if (!data.equals(other.data))
            return false;
        if (delayWhileIdle == null) {
            if (other.delayWhileIdle != null)
                return false;
        } else if (!delayWhileIdle.equals(other.delayWhileIdle))
            return false;
        if (registrationIds == null) {
            if (other.registrationIds != null)
                return false;
        } else if (!registrationIds.equals(other.registrationIds))
            return false;
        if (timeToLive == null) {
            if (other.timeToLive != null)
                return false;
        } else if (!timeToLive.equals(other.timeToLive))
            return false;
        return true;
    }

}
