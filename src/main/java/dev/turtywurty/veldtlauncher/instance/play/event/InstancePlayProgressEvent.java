package dev.turtywurty.veldtlauncher.instance.play.event;

import dev.turtywurty.veldtlauncher.event.Event;
import dev.turtywurty.veldtlauncher.instance.play.InstancePlayStep;

public record InstancePlayProgressEvent(
        InstancePlayStep step,
        String detail,
        double progress
) implements Event {
}
