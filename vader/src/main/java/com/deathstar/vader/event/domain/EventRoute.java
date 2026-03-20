package com.deathstar.vader.event.domain;

public enum EventRoute {
    AUDIT("AUDIT", "audit.events.", true),
    LOOM("LOOM", "loom.", true),
    SYSTEM("SYSTEM", "system.events", false),
    AUTH("AUTH", "auth.", false);

    private final String streamName;
    private final String subjectPrefix;
    private final boolean durable;

    EventRoute(String streamName, String subjectPrefix, boolean durable) {
        this.streamName = streamName;
        this.subjectPrefix = subjectPrefix;
        this.durable = durable;
    }

    public String stream() {
        return streamName;
    }

    public boolean isDurable() {
        return durable;
    }

    public String subject(String specificSubject) {
        if (this == SYSTEM) {
            return subjectPrefix;
        }
        return subjectPrefix + specificSubject;
    }

    public String wildcardSubject() {
        if (this == SYSTEM) {
            return subjectPrefix;
        }
        return subjectPrefix + ">";
    }
}
