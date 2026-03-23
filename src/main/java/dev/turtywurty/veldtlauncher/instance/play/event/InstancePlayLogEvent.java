package dev.turtywurty.veldtlauncher.instance.play.event;

import dev.turtywurty.veldtlauncher.event.Event;
import dev.turtywurty.veldtlauncher.instance.play.InstancePlayStep;

public record InstancePlayLogEvent(
        InstancePlayStep step,
        String message,
        boolean error
) implements Event {
}
