package com.core.event;

import org.springframework.context.ApplicationEvent;

import java.util.HashMap;

public class BatchCallbackEvent extends ApplicationEvent {

    private final HashMap<String, String> data;

    public BatchCallbackEvent(Object source, HashMap<String, String> data) {
        super(source);
        this.data = data;
    }

    public HashMap<String, String> getData() {
        return data;
    }

    public String getResult() {
        return data.get("result");
    }

    public String getBatch() {
        return data.get("batch");
    }
}
