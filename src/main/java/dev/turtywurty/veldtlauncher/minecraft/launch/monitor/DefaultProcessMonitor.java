package dev.turtywurty.veldtlauncher.minecraft.launch.monitor;

import dev.turtywurty.veldtlauncher.event.SimpleEventStream;
import dev.turtywurty.veldtlauncher.minecraft.launch.monitor.event.ProcessExitEvent;
import dev.turtywurty.veldtlauncher.minecraft.launch.monitor.event.ProcessMonitoringFailedEvent;
import dev.turtywurty.veldtlauncher.minecraft.launch.monitor.event.ProcessMonitoringStartedEvent;
import dev.turtywurty.veldtlauncher.minecraft.launch.monitor.event.ProcessOutputEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class DefaultProcessMonitor implements ProcessMonitor {
    @Override
    public ProcessMonitorHandle attach(Process process) {
        var handle = new DefaultProcessMonitorHandle(process, new SimpleEventStream());
        handle.emit(new ProcessMonitoringStartedEvent(process));
        handle.addMonitoringThread(Thread.startVirtualThread(() -> monitorStdout(process, handle)));
        handle.addMonitoringThread(Thread.startVirtualThread(() -> monitorStderr(process, handle)));
        handle.addMonitoringThread(Thread.startVirtualThread(() -> monitorForExitCode(process, handle)));
        return handle;
    }

    private void monitorStdout(Process process, DefaultProcessMonitorHandle handle) {
        try (InputStream stdout = process.getInputStream();
             var reader = new BufferedReader(new InputStreamReader(stdout, StandardCharsets.UTF_8))) {
            String line;
            while (handle.isMonitoring() && (line = reader.readLine()) != null) {
                handle.emit(new ProcessOutputEvent(process, new ProcessOutputLine(ProcessOutputLine.StreamType.STDOUT, line)));
            }
        } catch (IOException exception) {
            if (handle.isMonitoring())
                handle.emit(new ProcessMonitoringFailedEvent(process, exception));
        } finally {
            handle.removeMonitoringThread(Thread.currentThread());
        }
    }

    private void monitorStderr(Process process, DefaultProcessMonitorHandle handle) {
        try (InputStream stderr = process.getErrorStream();
             var reader = new BufferedReader(new InputStreamReader(stderr, StandardCharsets.UTF_8))) {
            String line;
            while (handle.isMonitoring() && (line = reader.readLine()) != null) {
                handle.emit(new ProcessOutputEvent(process, new ProcessOutputLine(ProcessOutputLine.StreamType.STDERR, line)));
            }
        } catch (IOException exception) {
            if (handle.isMonitoring())
                handle.emit(new ProcessMonitoringFailedEvent(process, exception));
        } finally {
            handle.removeMonitoringThread(Thread.currentThread());
        }
    }

    private void monitorForExitCode(Process process, DefaultProcessMonitorHandle handle) {
        try {
            int exitCode = process.waitFor();
            if (handle.isMonitoring())
                handle.emit(new ProcessExitEvent(process, exitCode));
        } catch (InterruptedException exception) {
            if (handle.isMonitoring())
                handle.emit(new ProcessMonitoringFailedEvent(process, exception));
            Thread.currentThread().interrupt();
        } finally {
            handle.removeMonitoringThread(Thread.currentThread());
        }
    }
}
