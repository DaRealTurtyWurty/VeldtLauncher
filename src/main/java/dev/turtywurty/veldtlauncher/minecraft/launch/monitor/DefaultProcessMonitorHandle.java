package dev.turtywurty.veldtlauncher.minecraft.launch.monitor;

import dev.turtywurty.veldtlauncher.event.Event;
import dev.turtywurty.veldtlauncher.event.EventStream;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultProcessMonitorHandle implements ProcessMonitorHandle {
    private final Process process;
    private final EventStream eventStream;
    private final List<Thread> monitoringThreads = new CopyOnWriteArrayList<>();
    private volatile boolean monitoring = true;

    public DefaultProcessMonitorHandle(Process process, EventStream eventStream) {
        this.process = process;
        this.eventStream = eventStream;
    }

    @Override
    public boolean isAlive() {
        return process.isAlive();
    }

    @Override
    public void stopMonitoring() {
        if (!monitoring)
            return;

        monitoring = false;
        for (Thread monitoringThread : monitoringThreads) {
            monitoringThread.interrupt();
        }

        closeQuietly(process.getInputStream());
        closeQuietly(process.getErrorStream());
    }

    @Override
    public EventStream eventStream() {
        return eventStream;
    }

    @Override
    public void emit(Event event) {
        eventStream.emit(event);
    }

    @Override
    public void addMonitoringThread(Thread thread) {
        if (thread == null)
            return;

        monitoringThreads.add(thread);
        if (!monitoring)
            thread.interrupt();
    }

    boolean isMonitoring() {
        return monitoring;
    }

    void removeMonitoringThread(Thread thread) {
        monitoringThreads.remove(thread);
    }

    private void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }
}
