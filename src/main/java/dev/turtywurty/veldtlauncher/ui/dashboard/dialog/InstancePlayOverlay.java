package dev.turtywurty.veldtlauncher.ui.dashboard.dialog;

import dev.turtywurty.veldtlauncher.event.EventListener;
import dev.turtywurty.veldtlauncher.event.EventStream;
import dev.turtywurty.veldtlauncher.event.SimpleEventStream;
import dev.turtywurty.veldtlauncher.instance.StoredInstanceMetadata;
import dev.turtywurty.veldtlauncher.instance.play.InstancePlayService;
import dev.turtywurty.veldtlauncher.instance.play.InstancePlayStep;
import dev.turtywurty.veldtlauncher.instance.play.event.InstancePlayCompletedEvent;
import dev.turtywurty.veldtlauncher.instance.play.event.InstancePlayFailedEvent;
import dev.turtywurty.veldtlauncher.instance.play.event.InstancePlayLogEvent;
import dev.turtywurty.veldtlauncher.instance.play.event.InstancePlayProgressEvent;
import dev.turtywurty.veldtlauncher.ui.logs.InstanceLogsWindow;
import dev.turtywurty.veldtlauncher.ui.Stylesheets;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class InstancePlayOverlay extends StackPane {
    private static final int MAX_LOG_LINES = 400;
    private static final int MAX_EVENTS_PER_FLUSH = 32;
    private static final String STATUS_SUCCESS_CLASS = "instance-play-overlay-status-success";
    private static final String STATUS_ERROR_CLASS = "instance-play-overlay-status-error";

    private final Runnable onClose;
    private final EventStream eventStream = new SimpleEventStream();
    private final List<EventListener<?>> listeners = new ArrayList<>();
    private final ConcurrentLinkedQueue<Object> pendingEvents = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean flushScheduled = new AtomicBoolean();
    private final ArrayDeque<String> logLines = new ArrayDeque<>();

    private final Label stepLabel = new Label("Preparing launch");
    private final Label detailLabel = new Label("Waiting for launcher steps...");
    private final Label percentLabel = new Label("0%");
    private final Label statusLabel = new Label("Working...");
    private final ProgressBar progressBar = new ProgressBar(0D);
    private final TextArea logArea = new TextArea();
    private final Button closeButton = new Button("Close");

    private boolean closeEnabled;

    public InstancePlayOverlay(StoredInstanceMetadata instance, Runnable onClose) {
        this.onClose = Objects.requireNonNull(onClose, "onClose");

        Stylesheets.addAll(this, "shared-controls.css", "instance-play-overlay.css");
        getStyleClass().add("instance-play-overlay");
        setPickOnBounds(true);

        Region backdrop = new Region();
        backdrop.getStyleClass().add("instance-play-overlay-backdrop");
        backdrop.setOnMouseClicked(_ -> {
            if (this.closeEnabled) {
                close();
            }
        });

        VBox card = createCard(instance);
        StackPane.setAlignment(card, Pos.CENTER);
        StackPane.setMargin(card, new Insets(32));
        getChildren().addAll(backdrop, card);

        this.closeButton.setDisable(true);
        this.closeButton.setOnAction(_ -> close());

        this.logArea.setEditable(false);
        this.logArea.setWrapText(false);
        this.logArea.setFocusTraversable(false);

        setFocusTraversable(true);
        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE && this.closeEnabled) {
                event.consume();
                close();
            }
        });

        registerListeners();
        InstancePlayService.INSTANCE.start(instance, this.eventStream);
    }

    public void dispose() {
        for (EventListener<?> listener : this.listeners) {
            unregisterListener(listener);
        }

        this.listeners.clear();
    }

    private VBox createCard(StoredInstanceMetadata instance) {
        Label titleLabel = new Label("Launching " + getDisplayName(instance));
        titleLabel.getStyleClass().add("instance-play-overlay-title");

        Label subtitleLabel = new Label("Install and launch progress for this instance appears here.");
        subtitleLabel.getStyleClass().add("instance-play-overlay-subtitle");
        subtitleLabel.setWrapText(true);

        this.stepLabel.getStyleClass().add("instance-play-overlay-step");
        this.detailLabel.getStyleClass().add("instance-play-overlay-detail");
        this.detailLabel.setWrapText(true);

        this.percentLabel.getStyleClass().add("instance-play-overlay-percent");

        HBox stepHeader = new HBox(12, this.stepLabel, spacer(), this.percentLabel);
        stepHeader.setAlignment(Pos.CENTER_LEFT);

        this.progressBar.getStyleClass().add("instance-play-overlay-progress");
        this.progressBar.setMaxWidth(Double.MAX_VALUE);

        this.statusLabel.getStyleClass().add("instance-play-overlay-status");

        Label logsLabel = new Label("Logs");
        logsLabel.getStyleClass().add("instance-play-overlay-logs-label");

        this.logArea.getStyleClass().add("instance-play-overlay-log-area");
        VBox.setVgrow(this.logArea, Priority.ALWAYS);

        this.closeButton.getStyleClass().addAll("veldt-back-button", "instance-play-overlay-close-button");

        HBox actionBar = new HBox(spacer(), this.closeButton);
        actionBar.setAlignment(Pos.CENTER_RIGHT);
        actionBar.getStyleClass().add("instance-play-overlay-action-bar");

        VBox card = new VBox(16,
                titleLabel,
                subtitleLabel,
                stepHeader,
                this.detailLabel,
                this.progressBar,
                this.statusLabel,
                logsLabel,
                this.logArea,
                actionBar
        );
        card.getStyleClass().add("instance-play-overlay-card");
        card.setMaxWidth(720);
        card.setPrefWidth(720);
        card.setMaxHeight(640);
        return card;
    }

    private void registerListeners() {
        this.listeners.add(this.eventStream.registerListener(InstancePlayProgressEvent.class,
                this::enqueueEvent));
        this.listeners.add(this.eventStream.registerListener(InstancePlayLogEvent.class,
                this::enqueueEvent));
        this.listeners.add(this.eventStream.registerListener(InstancePlayCompletedEvent.class,
                this::enqueueEvent));
        this.listeners.add(this.eventStream.registerListener(InstancePlayFailedEvent.class,
                this::enqueueEvent));
    }

    private void enqueueEvent(Object event) {
        this.pendingEvents.add(event);
        scheduleFlush();
    }

    private void scheduleFlush() {
        if (this.flushScheduled.compareAndSet(false, true)) {
            Platform.runLater(this::flushPendingEvents);
        }
    }

    private void flushPendingEvents() {
        try {
            Object event;
            int processed = 0;
            while (processed < MAX_EVENTS_PER_FLUSH && (event = this.pendingEvents.poll()) != null) {
                processed++;
                if (event instanceof InstancePlayProgressEvent progressEvent) {
                    handleProgress(progressEvent);
                    continue;
                }

                if (event instanceof InstancePlayLogEvent logEvent) {
                    handleLog(logEvent);
                    continue;
                }

                if (event instanceof InstancePlayCompletedEvent completedEvent) {
                    handleCompleted(completedEvent);
                    continue;
                }

                if (event instanceof InstancePlayFailedEvent failedEvent) {
                    handleFailed(failedEvent);
                }
            }
        } finally {
            this.flushScheduled.set(false);
            if (!this.pendingEvents.isEmpty()) {
                scheduleFlush();
            }
        }
    }

    private void handleProgress(InstancePlayProgressEvent event) {
        this.stepLabel.setText(event.step().displayName());
        this.detailLabel.setText(normalizeDetail(event.step(), event.detail()));
        this.progressBar.setProgress(clamp(event.progress()));
        this.percentLabel.setText(formatProgress(event.progress()));
        setStatus("Working...", null);
    }

    private void handleLog(InstancePlayLogEvent event) {
        appendLog(event.step(), event.message(), event.error());
    }

    private void handleCompleted(InstancePlayCompletedEvent event) {
        enableClose();
        this.progressBar.setProgress(1D);
        this.percentLabel.setText("100%");
        setStatus(event.message(), STATUS_SUCCESS_CLASS);
        close();
        if (event.monitorHandle() != null && event.logsWindowTitle() != null && !event.logsWindowTitle().isBlank()) {
            Platform.runLater(() -> InstanceLogsWindow.show(
                    event.logsWindowTitle(),
                    event.monitorHandle(),
                    event.mappings()
            ));
        }
    }

    private void handleFailed(InstancePlayFailedEvent event) {
        enableClose();
        setStatus(event.message(), STATUS_ERROR_CLASS);
        appendLog(event.step(), event.message(), true);
        if (event.cause() != null && event.cause().getMessage() != null && !event.cause().getMessage().isBlank()) {
            appendRawLog("Cause: " + event.cause().getMessage());
        }
    }

    private void setStatus(String message, String extraClass) {
        this.statusLabel.setText(message == null || message.isBlank() ? "Working..." : message);
        this.statusLabel.getStyleClass().removeAll(STATUS_SUCCESS_CLASS, STATUS_ERROR_CLASS);
        if (extraClass != null && !this.statusLabel.getStyleClass().contains(extraClass)) {
            this.statusLabel.getStyleClass().add(extraClass);
        }
    }

    private void enableClose() {
        this.closeEnabled = true;
        this.closeButton.setDisable(false);
    }

    private void appendLog(InstancePlayStep step, String message, boolean error) {
        String prefix = step == null ? "[Play]" : "[" + step.displayName() + "]";
        appendRawLog(prefix + " " + message);
        if (error) {
            this.statusLabel.getStyleClass().remove(STATUS_SUCCESS_CLASS);
            if (!this.statusLabel.getStyleClass().contains(STATUS_ERROR_CLASS)) {
                this.statusLabel.getStyleClass().add(STATUS_ERROR_CLASS);
            }
        }
    }

    private void appendRawLog(String message) {
        if (message == null || message.isBlank())
            return;

        boolean shouldTrim = this.logLines.size() >= MAX_LOG_LINES;
        this.logLines.addLast(message);
        while (this.logLines.size() > MAX_LOG_LINES) {
            this.logLines.removeFirst();
        }

        if (shouldTrim) {
            this.logArea.setText(String.join(System.lineSeparator(), this.logLines));
        } else {
            if (!this.logArea.getText().isEmpty()) {
                this.logArea.appendText(System.lineSeparator());
            }

            this.logArea.appendText(message);
        }

        this.logArea.positionCaret(this.logArea.getLength());
    }

    private void close() {
        if (!this.closeEnabled)
            return;

        this.onClose.run();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void unregisterListener(EventListener<?> listener) {
        this.eventStream.unregisterListener((EventListener) listener);
    }

    private Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private String getDisplayName(StoredInstanceMetadata instance) {
        String name = instance.name();
        return name == null || name.isBlank() ? instance.id() : name;
    }

    private String normalizeDetail(InstancePlayStep step, String detail) {
        if (detail != null && !detail.isBlank())
            return detail;

        return step == null ? "Working..." : step.displayName();
    }

    private String formatProgress(double progress) {
        return Math.round(clamp(progress) * 100D) + "%";
    }

    private double clamp(double progress) {
        return Math.max(0D, Math.min(1D, progress));
    }
}
