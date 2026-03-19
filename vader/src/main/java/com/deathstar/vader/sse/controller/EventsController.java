package com.deathstar.vader.sse.controller;

import com.deathstar.vader.api.EventsApi;
import com.deathstar.vader.sse.service.SseBroadcasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class EventsController implements EventsApi {

    private final SseBroadcasterService sseBroadcasterService;

    @Override
    public ResponseEntity<SseEmitter> eventsGet() {
        SseEmitter emitter = sseBroadcasterService.registerClient();
        return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(emitter);
    }
}
