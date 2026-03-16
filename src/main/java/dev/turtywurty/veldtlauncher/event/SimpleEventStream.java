package dev.turtywurty.veldtlauncher.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimpleEventStream implements EventStream {
    private final List<EventListener<? extends Event>> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void emit(Event event) {
        for (EventListener<? extends Event> listener : listeners) {
            if (listener.getEventType().isInstance(event)) {
                ((EventListener<Event>) listener).onEvent(event);
            }
        }
    }

    @Override
    public <T extends Event> EventListener<T> registerListener(EventListener<T> listener) {
        listeners.add(listener);
        return listener;
    }

    @Override
    public <T extends Event> void unregisterListener(EventListener<T> listener) {
        listeners.remove(listener);
    }
}
