package com.deathstar.vader.event.domain;

public enum EventRoute {
    AUDIT("AUDIT", "audit.events."),
    LOOM("LOOM", "loom."),
    SYSTEM("SYSTEM", "system.events");

    private final String streamName;
    private final String subjectPrefix;

    EventRoute(String streamName, String subjectPrefix) {
        this.streamName = streamName;
        this.subjectPrefix = subjectPrefix;
    }

    public String stream() {
        return streamName;
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
