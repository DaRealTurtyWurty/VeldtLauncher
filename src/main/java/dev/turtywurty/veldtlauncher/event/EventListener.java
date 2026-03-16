package dev.turtywurty.veldtlauncher.event;

public interface EventListener<T extends Event> {
    Class<T> getEventType();

    void onEvent(T event);
}
