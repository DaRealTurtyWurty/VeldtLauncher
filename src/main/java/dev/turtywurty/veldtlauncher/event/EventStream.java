package dev.turtywurty.veldtlauncher.event;

import java.util.function.Consumer;

public interface EventStream {
    <T extends Event> void emit(T event);

    <T extends Event> EventListener<T> registerListener(EventListener<T> listener);

    default <T extends Event> EventListener<T> registerListener(Class<T> clazz, Consumer<T> listener) {
        return registerListener(new EventListener<>() {
            @Override
            public Class<T> getEventType() {
                return clazz;
            }

            @Override
            public void onEvent(T event) {
                listener.accept(event);
            }
        });
    }

    <T extends Event> void unregisterListener(EventListener<T> listener);

    default <T extends Event> void emit(Class<T> eventClass) {
        try {
            emit(eventClass.getConstructor().newInstance());
        } catch (Exception exception) {
            throw new RuntimeException("Failed to emit event of type " + eventClass.getName(), exception);
        }
    }
}
