package com.alibaba.jstorm.kafka;

import java.io.Serializable;

public class KafkaBroker implements Serializable {
    private String host;
    private int port;

    public KafkaBroker(String host) {
        this(host, 9092);
    }

    public KafkaBroker(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        if (obj instanceof KafkaBroker) {
            final KafkaBroker other = (KafkaBroker) obj;
            return this.host.equals(other.host) && this.port == other.port;
        } else {
            return false;
        }
    }
}
