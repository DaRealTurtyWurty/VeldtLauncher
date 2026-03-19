package dev.turtywurty.veldtlauncher.ui.auth;

import dev.turtywurty.veldtlauncher.auth.session.JsonSessionStore;
import dev.turtywurty.veldtlauncher.auth.session.StoredSessionMetadata;
import dev.turtywurty.veldtlauncher.ui.Stylesheets;
import dev.turtywurty.veldtlauncher.ui.WindowChrome;
import dev.turtywurty.veldtlauncher.ui.dashboard.shell.DashboardShell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PickAccountPane extends AnchorPane {
    private static final StoredSessionMetadata SAMPLE_SESSION = new StoredSessionMetadata(
            UUID.randomUUID().toString(),
            "Turtywurty",
            System.currentTimeMillis() + 86_400_000L * 14,
            "sample.account.preview@veldtlauncher.dev",
            System.currentTimeMillis() - ThreadLocalRandom.current().nextLong(86_400_000L)
    );

    private final ObservableList<StoredSessionMetadata> sessions = FXCollections.observableArrayList();
    private final FlowPane accounts = new FlowPane(Orientation.HORIZONTAL);
    private String selectedUserId;

    public PickAccountPane() {
        Stylesheets.addAll(this, "pick-account-pane.css", "shared-controls.css");
        getStyleClass().add("pick-account-pane");

        var content = new VBox();
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(48));
        content.getStyleClass().add("pick-account-content");
        AnchorPane.setTopAnchor(content, 0.0);
        AnchorPane.setBottomAnchor(content, 0.0);
        AnchorPane.setLeftAnchor(content, 0.0);
        AnchorPane.setRightAnchor(content, 0.0);

        var title = new Text("Pick an account");
        title.getStyleClass().add("pick-account-title");

        var subtitle = new Text("Select an account to log in with");
        subtitle.getStyleClass().add("pick-account-subtitle");

        this.accounts.getStyleClass().add("pick-account-accounts");
        this.accounts.setAlignment(Pos.CENTER);
        this.accounts.setColumnHalignment(HPos.CENTER);
        this.accounts.setRowValignment(VPos.CENTER);
        this.accounts.setHgap(18);
        this.accounts.setVgap(18);
        this.accounts.setPrefWrapLength(920);

        content.getChildren().addAll(title, subtitle, this.accounts);
        getChildren().add(content);

        var windowBar = new HBox(WindowChrome.createWindowControls(this));
        windowBar.setAlignment(Pos.CENTER_RIGHT);
        windowBar.setPadding(new Insets(12, 12, 0, 12));
        WindowChrome.installDragSupport(windowBar);
        AnchorPane.setTopAnchor(windowBar, 0.0);
        AnchorPane.setLeftAnchor(windowBar, 0.0);
        AnchorPane.setRightAnchor(windowBar, 0.0);
        getChildren().add(windowBar);

        refreshAccounts();
    }

    private Node createAddAccount() {
        var addAccount = new AddAccountCard();
        addAccount.setOnMouseClicked(_ -> {
            var authPane = new AuthenticatePane();
            Scene scene = getScene();
            if (scene != null) {
                scene.setRoot(authPane);
            }
        });

        return addAccount;
    }

    private void refreshAccounts() {
        var storedSessions = JsonSessionStore.INSTANCE.loadAll();
        this.selectedUserId = JsonSessionStore.INSTANCE.loadLastSession()
                .map(StoredSessionMetadata::userId)
                .orElse(null);
        if (storedSessions.isEmpty()) {
            this.sessions.setAll(SAMPLE_SESSION);
            this.selectedUserId = SAMPLE_SESSION.userId();
        } else {
            this.sessions.setAll(storedSessions);
        }

        renderAccounts();
    }

    private void renderAccounts() {
        this.accounts.getChildren().clear();
        for (StoredSessionMetadata session : this.sessions) {
            var account = new AccountCard(session);
            account.setSelected(Objects.equals(session.userId(), this.selectedUserId));
            account.setOnMouseClicked(_ -> selectAccount(session));
            this.accounts.getChildren().add(account);
        }

        this.accounts.getChildren().add(createAddAccount());
    }

    private void selectAccount(StoredSessionMetadata session) {
        if (session == null || session.userId() == null || session.userId().isBlank())
            return;

        if (!isSampleSession(session)) {
            JsonSessionStore.INSTANCE.setLastSession(session.userId());
        }
        this.selectedUserId = session.userId();
        Scene scene = getScene();
        if (scene != null) {
            DashboardShell.show(scene);
            return;
        }

        renderAccounts();
    }

    private static boolean isSampleSession(StoredSessionMetadata session) {
        return Objects.equals(SAMPLE_SESSION.userId(), session.userId());
    }
}
