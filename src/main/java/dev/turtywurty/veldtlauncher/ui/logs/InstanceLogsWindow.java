package dev.turtywurty.veldtlauncher.ui.logs;

import dev.turtywurty.veldtlauncher.event.EventListener;
import dev.turtywurty.veldtlauncher.event.EventStream;
import dev.turtywurty.veldtlauncher.minecraft.launch.monitor.ProcessMonitorHandle;
import dev.turtywurty.veldtlauncher.minecraft.launch.monitor.ProcessOutputLine;
import dev.turtywurty.veldtlauncher.minecraft.launch.monitor.event.ProcessExitEvent;
import dev.turtywurty.veldtlauncher.minecraft.launch.monitor.event.ProcessMonitoringFailedEvent;
import dev.turtywurty.veldtlauncher.minecraft.launch.monitor.event.ProcessOutputEvent;
import dev.turtywurty.veldtlauncher.minecraft.mapping.Mappings;
import dev.turtywurty.veldtlauncher.ui.Stylesheets;
import dev.turtywurty.veldtlauncher.ui.WindowChrome;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InstanceLogsWindow {
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("(logger|level|thread|timestamp)=\"([^\"]*)\"");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("<log4j:Message><!\\[CDATA\\[(.*)]]></log4j:Message>", Pattern.DOTALL);
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
    private static final String INFO_LINE_STYLE = "-fx-fill: #8fb7ff; -fx-font-family: 'Consolas'; -fx-font-size: 12px;";
    private static final String WARN_LINE_STYLE = "-fx-fill: #ffbf66; -fx-font-family: 'Consolas'; -fx-font-size: 12px;";
    private static final String ERROR_LINE_STYLE = "-fx-fill: #ff8c8c; -fx-font-family: 'Consolas'; -fx-font-size: 12px;";
    private static final String DEBUG_LINE_STYLE = "-fx-fill: rgba(235,235,235,0.82); -fx-font-family: 'Consolas'; -fx-font-size: 12px;";

    private final Stage stage;
    private final EventStream eventStream;
    private final List<EventListener<?>> listeners = new ArrayList<>();
    private final InlineCssTextArea logArea = new InlineCssTextArea();
    private final VirtualizedScrollPane<InlineCssTextArea> logScrollPane = new VirtualizedScrollPane<>(this.logArea);
    private final List<String> pendingLog4jEventLines = new ArrayList<>();
    private final Mappings mappings;
    private boolean crashBlockActive;

    private InstanceLogsWindow(String title, ProcessMonitorHandle monitorHandle, Mappings mappings) {
        this.eventStream = Objects.requireNonNull(monitorHandle, "monitorHandle").eventStream();
        this.mappings = mappings == null ? Mappings.empty() : mappings;
        this.stage = new Stage();
        this.stage.initStyle(StageStyle.TRANSPARENT);
        this.stage.setTitle(Objects.requireNonNull(title, "title"));
        this.stage.setMinWidth(720);
        this.stage.setMinHeight(420);
        this.stage.setWidth(920);
        this.stage.setHeight(580);

        this.logArea.getStyleClass().add("instance-logs-window-log-area");
        this.logArea.setEditable(false);
        this.logArea.setWrapText(false);
        this.logArea.setFocusTraversable(false);
        this.logArea.setMouseTransparent(false);
        this.logArea.setContextMenu(null);
        this.logScrollPane.getStyleClass().add("instance-logs-window-log-scroll");

        Label heading = new Label(title);
        heading.getStyleClass().add("instance-logs-window-title");

        Label subtitle = new Label("Live process output");
        subtitle.getStyleClass().add("instance-logs-window-subtitle");

        VBox titleBlock = new VBox(4, heading, subtitle);
        titleBlock.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(12, titleBlock, spacer, WindowChrome.createWindowControls(this.logArea));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(14, 16, 12, 16));
        topBar.getStyleClass().add("instance-logs-window-top-bar");
        WindowChrome.installDragSupport(topBar);

        Label logsLabel = new Label("Process Output");
        logsLabel.getStyleClass().add("instance-logs-window-section-label");
        logsLabel.setContentDisplay(ContentDisplay.TEXT_ONLY);

        BorderPane content = new BorderPane();
        content.getStyleClass().add("instance-logs-window-content");
        content.setTop(logsLabel);
        content.setCenter(this.logScrollPane);
        BorderPane.setMargin(logsLabel, new Insets(0, 0, 10, 0));
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        VBox shell = new VBox(topBar, content);
        shell.getStyleClass().add("instance-logs-window-shell");
        shell.setFillWidth(true);
        VBox.setVgrow(content, Priority.ALWAYS);

        StackPane root = new StackPane(shell);
        root.getStyleClass().add("instance-logs-window-root");
        root.setPadding(new Insets(10));
        Stylesheets.addAll(root, "shared-controls.css", "instance-logs-window.css");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        WindowChrome.installResizeSupport(scene);
        this.stage.setScene(scene);
        this.stage.setOnHidden(_ -> dispose());
        registerListeners();
    }

    public static void show(String title, ProcessMonitorHandle monitorHandle, Mappings mappings) {
        new InstanceLogsWindow(title, monitorHandle, mappings).stage.show();
    }

    private void registerListeners() {
        this.listeners.add(this.eventStream.registerListener(ProcessOutputEvent.class, event ->
                Platform.runLater(() -> appendOutput(event.line()))));
        this.listeners.add(this.eventStream.registerListener(ProcessMonitoringFailedEvent.class, event ->
                Platform.runLater(() -> appendEntry(new LogEntry(
                        "",
                        "MONITOR",
                        "Process Monitor",
                        "",
                        describeFailure(event),
                        "warn"
                )))));
        this.listeners.add(this.eventStream.registerListener(ProcessExitEvent.class, event ->
                Platform.runLater(() -> {
                    appendEntry(new LogEntry(
                            "",
                            event.exitCode() == 0 ? "EXIT" : "ERROR",
                            "Minecraft Process",
                            "",
                            "Exited with code " + event.exitCode(),
                            event.exitCode() == 0 ? "info" : "error"
                    ));
                    this.crashBlockActive = false;
                })));
    }

    private void appendOutput(ProcessOutputLine line) {
        if (line == null || line.line() == null || line.line().isBlank())
            return;

        String rawLine = line.line();
        if (isLog4jEventStart(rawLine) || !this.pendingLog4jEventLines.isEmpty()) {
            handleLog4jEventLine(rawLine, line.streamType());
            return;
        }

        appendEntry(createPlainEntry(rawLine, line.streamType()));
    }

    private void appendEntry(LogEntry entry) {
        if (entry == null || entry.message() == null || entry.message().isBlank())
            return;

        String line = formatEntry(entry);
        appendSegment(line, lineStyleCss(entry.styleClass()));
        appendSegment(System.lineSeparator(), lineStyleCss(entry.styleClass()));
        Platform.runLater(() -> {
            int lastParagraph = Math.max(0, this.logArea.getParagraphs().size() - 1);
            this.logArea.showParagraphAtBottom(lastParagraph);
        });
    }

    private void handleLog4jEventLine(String line, ProcessOutputLine.StreamType streamType) {
        this.pendingLog4jEventLines.add(line);
        if (!line.contains("</log4j:Event>"))
            return;

        LogEntry rendered = parseLog4jEvent(this.pendingLog4jEventLines, streamType);
        this.pendingLog4jEventLines.clear();
        appendEntry(rendered);
    }

    private boolean isLog4jEventStart(String line) {
        return line != null && line.contains("<log4j:Event");
    }

    private LogEntry parseLog4jEvent(List<String> lines, ProcessOutputLine.StreamType streamType) {
        if (lines == null || lines.isEmpty())
            return null;

        String xml = String.join("", lines);
        String logger = "";
        String level = streamType == ProcessOutputLine.StreamType.STDERR ? "ERROR" : "INFO";
        String thread = "";
        String timestamp = "";

        Matcher attributeMatcher = ATTRIBUTE_PATTERN.matcher(xml);
        while (attributeMatcher.find()) {
            String key = attributeMatcher.group(1);
            String value = attributeMatcher.group(2);
            switch (key) {
                case "logger" -> logger = value;
                case "level" -> level = value;
                case "thread" -> thread = value;
                case "timestamp" -> timestamp = value;
                default -> {
                }
            }
        }

        String message = extractMessage(xml);
        logger = this.mappings.mapLoggerName(logger);
        message = this.mappings.remapStackTraceClasses(message);
        return new LogEntry(
                formatTimestamp(timestamp),
                level,
                logger,
                thread,
                message,
                classifyStyle(level, logger, message, streamType)
        );
    }

    private String extractMessage(String xml) {
        Matcher matcher = MESSAGE_PATTERN.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1).strip();
        }

        return xml.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    private LogEntry createPlainEntry(String line, ProcessOutputLine.StreamType streamType) {
        String remappedLine = this.mappings.remapStackTraceClasses(line);
        return new LogEntry(
                "",
                "",
                "",
                "",
                remappedLine,
                classifyStyle("", "", remappedLine, streamType)
        );
    }

    private String describeFailure(ProcessMonitoringFailedEvent event) {
        Throwable cause = event == null ? null : event.cause();
        if (cause == null || cause.getMessage() == null || cause.getMessage().isBlank())
            return "Process monitoring failed.";

        return "Process monitoring failed: " + cause.getMessage();
    }

    private String formatEntry(LogEntry entry) {
        StringBuilder builder = new StringBuilder();
        boolean hasMetadata = false;

        if (entry.timeText() != null && !entry.timeText().isBlank()) {
            builder.append('[').append(entry.timeText()).append(']');
            hasMetadata = true;
        }

        if (entry.levelText() != null && !entry.levelText().isBlank()) {
            if (hasMetadata) {
                builder.append(' ');
            }

            builder.append('[').append(entry.levelText()).append(']');
            hasMetadata = true;
        }

        if (entry.source() != null && !entry.source().isBlank()) {
            if (hasMetadata) {
                builder.append(' ');
            }

            builder.append(entry.source());
            hasMetadata = true;
        }

        if (entry.thread() != null && !entry.thread().isBlank()) {
            if (hasMetadata) {
                builder.append(' ');
            }

            builder.append(" {").append(entry.thread()).append('}');
            hasMetadata = true;
        }

        if (hasMetadata) {
            builder.append(" - ");
        }

        builder.append(entry.message());
        return builder.toString();
    }

    private void appendSegment(String text, String style) {
        if (text == null || text.isEmpty())
            return;

        int start = this.logArea.getLength();
        this.logArea.appendText(text);
        int end = this.logArea.getLength();
        if (end > start && style != null && !style.isBlank()) {
            this.logArea.setStyle(start, end, style);
        }
    }

    private String formatTimestamp(String rawTimestamp) {
        if (rawTimestamp == null || rawTimestamp.isBlank())
            return "";

        try {
            long epochMillis = Long.parseLong(rawTimestamp);
            return TIME_FORMATTER.format(Instant.ofEpochMilli(epochMillis));
        } catch (NumberFormatException ignored) {
            return rawTimestamp;
        }
    }

    private String levelStyle(String level) {
        if (level == null || level.isBlank())
            return "plain";

        String normalized = level.trim().toLowerCase();
        return switch (normalized) {
            case "error", "fatal" -> "error";
            case "warn", "warning" -> "warn";
            case "debug", "trace" -> "debug";
            default -> "info";
        };
    }

    private String classifyStyle(String level, String source, String message, ProcessOutputLine.StreamType streamType) {
        if (streamType == ProcessOutputLine.StreamType.STDERR)
            return "stderr";

        if (this.crashBlockActive)
            return "error";

        if (looksLikeCrashReport(source, message)) {
            this.crashBlockActive = true;
            return "error";
        }

        return levelStyle(level);
    }

    private boolean looksLikeCrashReport(String source, String message) {
        String haystack = ((source == null ? "" : source) + " " + (message == null ? "" : message)).toLowerCase();
        return haystack.contains("crash report")
                || haystack.contains("crash-reports")
                || haystack.contains("saved this report to")
                || haystack.contains("unexpected error")
                || haystack.contains("this crash report has been saved to")
                || haystack.contains("latest.log")
                || haystack.contains("game crashed")
                || haystack.contains("---- minecraft crash report ----")
                || haystack.contains("a detailed walkthrough of the error")
                || haystack.contains("exception in server tick loop")
                || haystack.contains("unexpected error occurred")
                || haystack.contains("fml has detected a problem with your minecraft installation");
    }

    private String lineStyleCss(String styleClass) {
        return switch (styleClass) {
            case "error", "stderr" -> ERROR_LINE_STYLE;
            case "warn" -> WARN_LINE_STYLE;
            case "debug" -> DEBUG_LINE_STYLE;
            default -> INFO_LINE_STYLE;
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void dispose() {
        for (EventListener<?> listener : this.listeners) {
            this.eventStream.unregisterListener((EventListener) listener);
        }

        this.listeners.clear();
    }

    private record LogEntry(
            String timeText,
            String levelText,
            String source,
            String thread,
            String message,
            String styleClass
    ) {
    }
}
