package dev.turtywurty.veldtlauncher.instance.play.event;

import dev.turtywurty.veldtlauncher.event.Event;
import dev.turtywurty.veldtlauncher.instance.play.InstancePlayStep;

public record InstancePlayFailedEvent(
        InstancePlayStep step,
        String message,
        Throwable cause
) implements Event {
}
