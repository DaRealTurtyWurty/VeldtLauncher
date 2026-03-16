package dev.turtywurty.minecraftlauncher.event;

public interface EventStream {
    <T extends Event> void emit(T event);

    <T extends Event> EventListener<T> registerListener(EventListener<T> listener);

    <T extends Event> void unregisterListener(EventListener<T> listener);

    default <T extends Event> void emit(Class<T> eventClass) {
        try {
            emit(eventClass.getConstructor().newInstance());
        } catch (Exception exception) {
            throw new RuntimeException("Failed to emit event of type " + eventClass.getName(), exception);
        }
    }
}
