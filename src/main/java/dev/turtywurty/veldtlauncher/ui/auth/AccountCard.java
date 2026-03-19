package dev.turtywurty.veldtlauncher.ui.auth;

import dev.turtywurty.veldtlauncher.auth.session.StoredSessionMetadata;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class AccountCard extends VBox {
    private static final DateTimeFormatter LAST_ACCESSED_TOOLTIP_FORMATTER =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm").withZone(ZoneId.systemDefault());

    public AccountCard(StoredSessionMetadata session) {
        setSpacing(8);
        setAlignment(Pos.CENTER);
        setFocusTraversable(true);
        getStyleClass().add("account-card");

        var avatarText = new Label(extractInitial(session.username()));
        avatarText.getStyleClass().add("account-card-avatar-text");

        var avatar = new StackPane(avatarText);
        avatar.getStyleClass().add("account-card-avatar");
        avatar.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        avatar.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        avatar.setStyle(buildAvatarStyle(session.userId(), session.username()));

        var username = new Label(defaultText(session.username(), "Unknown player"));
        username.getStyleClass().add("account-card-title");
        username.setWrapText(true);
        username.setMaxWidth(Double.MAX_VALUE);
        username.setAlignment(Pos.CENTER);

        var lastAccessed = new Label(formatLastAccessed(session.lastAccessedAt()));
        lastAccessed.getStyleClass().add("account-card-last-accessed");
        lastAccessed.setWrapText(true);
        lastAccessed.setMaxWidth(Double.MAX_VALUE);
        lastAccessed.setAlignment(Pos.CENTER);
        Tooltip.install(lastAccessed, new Tooltip(formatLastAccessedTooltip(session.lastAccessedAt())));

        getChildren().addAll(avatar, username, lastAccessed);
    }

    public void setSelected(boolean selected) {
        if (selected) {
            if (!getStyleClass().contains("account-card-selected")) {
                getStyleClass().add("account-card-selected");
            }

            return;
        }

        getStyleClass().remove("account-card-selected");
    }

    private static String extractInitial(String username) {
        if (username == null || username.isBlank())
            return "?";

        return String.valueOf(Character.toUpperCase(username.trim().charAt(0)));
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String formatLastAccessed(long lastAccessedAt) {
        if (lastAccessedAt <= 0L)
            return "Last accessed unknown";

        Instant instant = Instant.ofEpochMilli(lastAccessedAt);
        Instant now = Instant.now();
        if (instant.isAfter(now))
            return "Last accessed just now";

        long minutes = ChronoUnit.MINUTES.between(instant, now);
        if (minutes <= 0L)
            return "Last accessed just now";
        if (minutes == 1L)
            return "Last accessed 1 minute ago";
        if (minutes < 60L)
            return "Last accessed " + minutes + " minutes ago";

        long hours = ChronoUnit.HOURS.between(instant, now);
        if (hours == 1L)
            return "Last accessed 1 hour ago";
        if (hours < 24L)
            return "Last accessed " + hours + " hours ago";

        long days = ChronoUnit.DAYS.between(
                instant.atZone(ZoneId.systemDefault()).toLocalDate(),
                LocalDateTime.now().atZone(ZoneId.systemDefault()).toLocalDate()
        );
        if (days == 1L)
            return "Last accessed yesterday";
        if (days < 7L)
            return "Last accessed " + days + " days ago";

        long weeks = days / 7L;
        if (weeks == 1L)
            return "Last accessed 1 week ago";
        if (weeks < 5L)
            return "Last accessed " + weeks + " weeks ago";

        long months = ChronoUnit.MONTHS.between(
                instant.atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(1),
                LocalDateTime.now().atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(1)
        );
        if (months <= 1L)
            return "Last accessed 1 month ago";
        if (months < 12L)
            return "Last accessed " + months + " months ago";

        long years = Math.max(1L, months / 12L);
        if (years == 1L)
            return "Last accessed 1 year ago";

        return "Last accessed " + years + " years ago";
    }

    private static String formatLastAccessedTooltip(long lastAccessedAt) {
        if (lastAccessedAt <= 0L)
            return "Last accessed time unavailable";

        return LAST_ACCESSED_TOOLTIP_FORMATTER.format(Instant.ofEpochMilli(lastAccessedAt));
    }

    private static String buildAvatarStyle(String userId, String username) {
        String seed = userId;
        if (seed == null || seed.isBlank())
            seed = username == null || username.isBlank() ? "default-avatar" : username;

        int hash = Math.abs(Objects.hashCode(seed));
        double hue = ((hash & 0xFFFF) / 65535.0) * 360.0;
        double saturation = 0.32 + (((hash >> 16) & 0xFF) / 255.0) * 0.5;
        double brightness = 0.42 + (((hash >> 24) & 0x7F) / 127.0) * 0.46;
        double hueShift = 12.0 + (((hash >> 9) & 0x1F) / 31.0) * 24.0;

        Color start = Color.hsb(hue, saturation, brightness);
        Color end = Color.hsb(
                (hue + hueShift) % 360.0,
                Math.max(0.2, Math.min(0.9, saturation - 0.08)),
                Math.max(0.28, Math.min(0.88, brightness - 0.08))
        );

        return "-fx-background-color: linear-gradient(to bottom right, "
               + toHex(start) + ", "
               + toHex(end) + ");";
    }

    private static String toHex(Color color) {
        int red = (int) Math.round(color.getRed() * 255);
        int green = (int) Math.round(color.getGreen() * 255);
        int blue = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02x%02x%02x", red, green, blue);
    }
}
