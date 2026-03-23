package dev.turtywurty.veldtlauncher.instance.play;

import dev.turtywurty.veldtlauncher.event.EventStream;
import dev.turtywurty.veldtlauncher.instance.play.event.InstancePlayCompletedEvent;
import dev.turtywurty.veldtlauncher.instance.play.event.InstancePlayFailedEvent;
import dev.turtywurty.veldtlauncher.instance.play.event.InstancePlayLogEvent;
import dev.turtywurty.veldtlauncher.instance.play.event.InstancePlayProgressEvent;
import dev.turtywurty.veldtlauncher.minecraft.launch.monitor.ProcessMonitorHandle;
import dev.turtywurty.veldtlauncher.minecraft.mapping.Mappings;

public final class InstancePlayReporter {
    private static final InstancePlayReporter NO_OP = new InstancePlayReporter(null);

    private final EventStream eventStream;
    private volatile InstancePlayStep currentStep = InstancePlayStep.AUTHENTICATING;

    public InstancePlayReporter(EventStream eventStream) {
        this.eventStream = eventStream;
    }

    public static InstancePlayReporter noOp() {
        return NO_OP;
    }

    public InstancePlayStep currentStep() {
        return this.currentStep;
    }

    public void progress(InstancePlayStep step, String detail, double progress) {
        this.currentStep = step;
        if (this.eventStream != null) {
            this.eventStream.emit(new InstancePlayProgressEvent(step, detail, progress));
        }
    }

    public void progress(InstancePlayStep step, String detail, int completed, int total) {
        double progress = total <= 0 ? 0D : Math.max(0D, Math.min(1D, (double) completed / total));
        progress(step, detail, progress);
    }

    public void log(InstancePlayStep step, String message) {
        this.currentStep = step;
        if (this.eventStream != null) {
            this.eventStream.emit(new InstancePlayLogEvent(step, message, false));
        }
    }

    public void error(InstancePlayStep step, String message) {
        this.currentStep = step;
        if (this.eventStream != null) {
            this.eventStream.emit(new InstancePlayLogEvent(step, message, true));
        }
    }

    public void completed(String message, ProcessMonitorHandle monitorHandle, String logsWindowTitle, Mappings mappings) {
        if (this.eventStream != null) {
            this.eventStream.emit(new InstancePlayCompletedEvent(message, monitorHandle, logsWindowTitle, mappings));
        }
    }

    public void failed(InstancePlayStep step, String message, Throwable cause) {
        this.currentStep = step;
        if (this.eventStream != null) {
            this.eventStream.emit(new InstancePlayFailedEvent(step, message, cause));
        }
    }
}
