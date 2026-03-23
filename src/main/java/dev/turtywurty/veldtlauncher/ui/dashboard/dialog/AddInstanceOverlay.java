package dev.turtywurty.veldtlauncher.ui.dashboard.dialog;

import dev.turtywurty.veldtlauncher.config.FileConfig;
import dev.turtywurty.veldtlauncher.instance.*;
import dev.turtywurty.veldtlauncher.ui.Images;
import dev.turtywurty.veldtlauncher.minecraft.manifest.MojangVersionManifestService;
import dev.turtywurty.veldtlauncher.minecraft.manifest.VersionManifestEntry;
import dev.turtywurty.veldtlauncher.ui.Stylesheets;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class AddInstanceOverlay extends StackPane {
    private static final String DEFAULT_INSTANCE_IMAGE = "minecraft_cover_art.png";
    private final Runnable onClose;
    private final Runnable onCreated;

    private int currentStep = 1;
    private final ToggleGroup typeToggleGroup = new ToggleGroup();
    private final VBox contentContainer = new VBox(18);
    private final StackPane formContainer = new StackPane();
    private final Label titleLabel = new Label();
    private final Label subtitleLabel = new Label();
    private final Label errorLabel = new Label();
    private final VBox typeSelectionBar = new VBox(10);
    private final CheckBox includeSnapshotsCheckBox = new CheckBox("Include snapshots and pre-releases");
    private List<VersionManifestEntry> availableVersions = List.of();
    private final ObservableList<String> selectedSupportedInstanceIds = FXCollections.observableArrayList();
    private final ComboBox<String> supportedInstanceIdChoice = createSupportedInstanceChoice();
    private final FlowPane supportedInstanceChipPane = new FlowPane(8, 8);
    private final Label supportedInstancePlaceholder = new Label("No supported instances selected");
    private final VBox supportedInstancePicker = new VBox(10);
    private final StackPane instanceImagePicker = new StackPane();
    private final StackPane instanceImagePreview = new StackPane();
    private final ImageView instanceImagePreviewView = new ImageView();
    private final FontIcon instanceImagePreviewFallback = new FontIcon(FontAwesomeSolid.IMAGE);
    private final Button editImageButton = new Button();
    private String selectedIconPath;

    private final TextField vanillaNameField = createInput("Name");
    private final ComboBox<String> vanillaVersionChoice = createVersionChoice(false);

    private final TextField modpackNameField = createInput("Name");
    private final ComboBox<String> modpackVersionChoice = createVersionChoice(false);
    private final ComboBox<ModLoader> modpackLoaderChoice = createModLoaderChoice();

    private final TextField serverNameField = createInput("Name");
    private final TextField serverAddressField = createInput("Address");
    private final ComboBox<String> serverVersionChoice = createVersionChoice(true);
    private final Button cancelButton = new Button("Cancel");
    private final Button backButton = new Button("Back");
    private final Button nextButton = new Button("Next");
    private final Button createButton = new Button("Create");

    public AddInstanceOverlay(Runnable onClose, Runnable onCreated) {
        this.onClose = Objects.requireNonNull(onClose, "onClose");
        this.onCreated = Objects.requireNonNull(onCreated, "onCreated");

        Stylesheets.addAll(this, "shared-controls.css", "add-instance-overlay.css");
        getStyleClass().add("add-instance-overlay");
        setPickOnBounds(true);

        Region backdrop = new Region();
        backdrop.getStyleClass().add("add-instance-overlay-backdrop");
        backdrop.setOnMouseClicked(_ -> close());

        VBox card = createCard();
        StackPane.setAlignment(card, Pos.CENTER);
        StackPane.setMargin(card, new Insets(32));

        getChildren().addAll(backdrop, card);
        configureSupportedInstancePicker();
        configureInstanceImagePicker();
        loadVersionsAsync();

        setFocusTraversable(true);
        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                close();
            }
        });
    }

    private VBox createCard() {
        this.titleLabel.getStyleClass().add("add-instance-overlay-title");

        this.subtitleLabel.getStyleClass().add("add-instance-overlay-subtitle");
        this.subtitleLabel.setWrapText(true);

        this.typeSelectionBar.getChildren().setAll(
                createTypeToggle(
                        "Vanilla",
                        "A clean Minecraft installation for a single version profile.",
                        InstanceType.VANILLA,
                        FontAwesomeSolid.CUBE
                ),
                createTypeToggle(
                        "Modpack",
                        "A modded setup with a specific loader and Minecraft version.",
                        InstanceType.MODPACK,
                        FontAwesomeSolid.BOX_OPEN
                ),
                createTypeToggle(
                        "Server",
                        "A multiplayer entry with supported local instance ids attached.",
                        InstanceType.SERVER,
                        FontAwesomeSolid.SERVER
                )
        );
        this.typeSelectionBar.getStyleClass().add("add-instance-overlay-type-bar");

        configureVersionFilterCheckBox();

        this.formContainer.getStyleClass().add("add-instance-overlay-form-container");
        this.contentContainer.getStyleClass().add("add-instance-overlay-content");

        this.errorLabel.getStyleClass().add("add-instance-overlay-error");
        this.errorLabel.setWrapText(true);
        this.errorLabel.setManaged(false);
        this.errorLabel.setVisible(false);

        this.cancelButton.getStyleClass().addAll("veldt-back-button", "add-instance-overlay-cancel-button");
        this.cancelButton.setOnAction(_ -> close());

        this.backButton.getStyleClass().addAll("veldt-back-button", "add-instance-overlay-back-button");
        this.backButton.setOnAction(_ -> showStep(1));

        this.nextButton.getStyleClass().addAll("veldt-back-button", "add-instance-overlay-next-button");
        this.nextButton.setOnAction(_ -> showStep(2));

        this.createButton.getStyleClass().addAll("veldt-back-button", "add-instance-overlay-create-button");
        this.createButton.setOnAction(_ -> createInstance());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actionBar = new HBox(10, this.cancelButton, this.backButton, spacer, this.nextButton, this.createButton);
        actionBar.setAlignment(Pos.CENTER_RIGHT);
        actionBar.getStyleClass().add("add-instance-overlay-action-bar");

        refreshStep();

        VBox card = new VBox(18, this.titleLabel, this.subtitleLabel, this.contentContainer, this.errorLabel, actionBar);
        card.getStyleClass().add("add-instance-overlay-card");
        card.setMaxWidth(560);
        card.setFillWidth(true);
        return card;
    }

    private ToggleButton createTypeToggle(String title, String description, InstanceType type, FontAwesomeSolid iconGlyph) {
        FontIcon icon = new FontIcon(iconGlyph);
        icon.getStyleClass().add("add-instance-overlay-type-icon");

        StackPane iconContainer = new StackPane(icon);
        iconContainer.getStyleClass().add("add-instance-overlay-type-icon-wrap");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("add-instance-overlay-type-title");

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("add-instance-overlay-type-description");
        descriptionLabel.setWrapText(true);

        VBox textContent = new VBox(4, titleLabel, descriptionLabel);
        textContent.getStyleClass().add("add-instance-overlay-type-copy");
        textContent.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textContent, Priority.ALWAYS);

        HBox content = new HBox(14, iconContainer, textContent);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getStyleClass().add("add-instance-overlay-type-content");
        content.setMaxWidth(Double.MAX_VALUE);

        ToggleButton button = new ToggleButton();
        button.getStyleClass().add("add-instance-overlay-type-toggle");
        button.setGraphic(content);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setUserData(type);
        button.setToggleGroup(this.typeToggleGroup);
        if (type == InstanceType.VANILLA) {
            this.typeToggleGroup.selectToggle(button);
        }
        return button;
    }

    private void refreshStep() {
        clearError();
        if (this.currentStep == 1) {
            this.titleLabel.setText("Choose Instance Type");
            this.subtitleLabel.setText("Start by picking whether you're creating a vanilla instance, a modpack profile, or a server entry.");
            this.contentContainer.getChildren().setAll(createStepOneContent());
            this.backButton.setManaged(false);
            this.backButton.setVisible(false);
            this.nextButton.setManaged(true);
            this.nextButton.setVisible(true);
            this.createButton.setManaged(false);
            this.createButton.setVisible(false);
            return;
        }

        this.titleLabel.setText("Create " + getSelectedTypeLabel() + " Instance");
        this.subtitleLabel.setText("Step 2 of 2. Fill in the details for your " + getSelectedTypeLabel().toLowerCase() + " instance.");
        this.formContainer.getChildren().setAll(createFormForSelectedType());
        this.contentContainer.getChildren().setAll(createStepTwoContent());
        this.backButton.setManaged(true);
        this.backButton.setVisible(true);
        this.nextButton.setManaged(false);
        this.nextButton.setVisible(false);
        this.createButton.setManaged(true);
        this.createButton.setVisible(true);
    }

    private VBox createStepOneContent() {
        Label promptLabel = new Label("Instance type");
        promptLabel.getStyleClass().add("add-instance-overlay-field-label");

        VBox content = new VBox(10, promptLabel, this.typeSelectionBar);
        content.getStyleClass().add("add-instance-overlay-step-content");
        return content;
    }

    private VBox createStepTwoContent() {
        VBox content = new VBox(14, this.formContainer);
        content.getStyleClass().add("add-instance-overlay-step-content");
        return content;
    }

    private Node createFormForSelectedType() {
        return switch (getSelectedType()) {
            case MODPACK -> createModpackForm();
            case SERVER -> createServerForm();
            case VANILLA -> createVanillaForm();
        };
    }

    private void showStep(int step) {
        this.currentStep = step;
        refreshStep();
    }

    private void configureVersionFilterCheckBox() {
        this.includeSnapshotsCheckBox.getStyleClass().add("add-instance-overlay-checkbox");
        this.includeSnapshotsCheckBox.setSelected(false);
        this.includeSnapshotsCheckBox.selectedProperty().addListener((_, _, _) -> refreshVersionChoices());
    }

    private VBox createVanillaForm() {
        return createForm(
                this.includeSnapshotsCheckBox,
                createFieldGroup("Minecraft Version", this.vanillaVersionChoice)
        );
    }

    private VBox createModpackForm() {
        return createForm(
                this.includeSnapshotsCheckBox,
                createFieldGroup("Minecraft Version", this.modpackVersionChoice),
                createFieldGroup("Mod Loader", this.modpackLoaderChoice)
        );
    }

    private VBox createServerForm() {
        return createForm(
                createFieldGroup("Server Address", this.serverAddressField),
                this.includeSnapshotsCheckBox,
                createFieldGroup("Minecraft Version", this.serverVersionChoice),
                createFieldGroup("Supported Instance Ids", this.supportedInstancePicker)
        );
    }

    private VBox createForm(Node... nodes) {
        VBox form = new VBox(16);
        form.getStyleClass().add("add-instance-overlay-form");
        form.getChildren().add(createIdentitySection(getActiveNameField()));
        form.getChildren().addAll(nodes);
        return form;
    }

    private HBox createIdentitySection(TextField nameField) {
        this.instanceImagePicker.setMinSize(88, 88);
        this.instanceImagePicker.setPrefSize(88, 88);
        this.instanceImagePicker.setMaxSize(88, 88);

        VBox nameGroup = createFieldGroup("Instance Name", nameField);
        nameGroup.getStyleClass().add("add-instance-overlay-name-group");
        HBox.setHgrow(nameGroup, Priority.ALWAYS);

        HBox header = new HBox(16, this.instanceImagePicker, nameGroup);
        header.setAlignment(Pos.TOP_LEFT);
        header.getStyleClass().add("add-instance-overlay-identity-row");
        return header;
    }

    private VBox createFieldGroup(String labelText, Node control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("add-instance-overlay-field-label");

        VBox group = new VBox(6, label, control);
        group.getStyleClass().add("add-instance-overlay-field-group");
        return group;
    }

    private TextField createInput(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.getStyleClass().add("add-instance-overlay-input");
        return field;
    }

    private ComboBox<ModLoader> createModLoaderChoice() {
        ComboBox<ModLoader> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(ModLoader.values());
        comboBox.getSelectionModel().selectFirst();
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.getStyleClass().add("add-instance-overlay-choice");

        StringConverter<ModLoader> converter = new StringConverter<>() {
            @Override
            public String toString(ModLoader modLoader) {
                return modLoader == null ? "" : modLoader.getDisplayName();
            }

            @Override
            public ModLoader fromString(String string) {
                return null;
            }
        };
        comboBox.setConverter(converter);
        comboBox.setButtonCell(createComboListCell(converter));
        comboBox.setCellFactory(_ -> createComboListCell(converter));
        return comboBox;
    }

    private ComboBox<String> createVersionChoice(boolean optional) {
        ComboBox<String> choiceBox = new ComboBox<>();
        choiceBox.setMaxWidth(Double.MAX_VALUE);
        choiceBox.setPromptText(optional ? "Optional version" : "Loading versions...");
        choiceBox.setDisable(true);
        choiceBox.getStyleClass().add("add-instance-overlay-choice");
        return choiceBox;
    }

    private <T> ListCell<T> createComboListCell(StringConverter<T> converter) {
        return new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : converter.toString(item));
            }
        };
    }

    private ComboBox<String> createSupportedInstanceChoice() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.setPromptText("Select supported instance");
        comboBox.getStyleClass().add("add-instance-overlay-choice");
        comboBox.setOnAction(_ -> {
            String selectedId = comboBox.getValue();
            if (selectedId == null || selectedId.isBlank())
                return;

            if (!this.selectedSupportedInstanceIds.contains(selectedId)) {
                this.selectedSupportedInstanceIds.add(selectedId);
            }

            comboBox.getSelectionModel().clearSelection();
            refreshSupportedInstanceChoices();
        });
        return comboBox;
    }

    private void configureSupportedInstancePicker() {
        this.supportedInstancePicker.getStyleClass().add("add-instance-overlay-multi-select");
        this.supportedInstanceChipPane.getStyleClass().add("add-instance-overlay-chip-pane");
        this.supportedInstanceChipPane.setPrefWrapLength(420);

        this.supportedInstancePlaceholder.getStyleClass().add("add-instance-overlay-multi-select-placeholder");
        this.selectedSupportedInstanceIds.addListener((ListChangeListener<String>) _ -> {
            refreshSupportedInstanceChoices();
            refreshSupportedInstanceChips();
        });

        refreshSupportedInstanceChoices();
        refreshSupportedInstanceChips();
        this.supportedInstancePicker.getChildren().setAll(this.supportedInstanceChipPane, this.supportedInstanceIdChoice);
    }

    private void configureInstanceImagePicker() {
        this.instanceImagePicker.getStyleClass().add("add-instance-overlay-image-picker");
        this.instanceImagePreview.getStyleClass().add("add-instance-overlay-image-preview");
        this.instanceImagePreview.setMinSize(88, 88);
        this.instanceImagePreview.setPrefSize(88, 88);
        this.instanceImagePreview.setMaxSize(88, 88);
        this.instanceImagePreview.setClip(createInstanceImagePreviewClip());

        this.instanceImagePreviewView.getStyleClass().add("add-instance-overlay-image-preview-view");
        this.instanceImagePreviewView.setFitWidth(88);
        this.instanceImagePreviewView.setFitHeight(88);
        this.instanceImagePreviewView.setPreserveRatio(true);
        this.instanceImagePreviewView.setManaged(false);
        this.instanceImagePreviewView.setVisible(false);

        this.instanceImagePreviewFallback.getStyleClass().add("add-instance-overlay-image-preview-fallback");
        this.instanceImagePreview.getChildren().setAll(this.instanceImagePreviewView, this.instanceImagePreviewFallback);
        this.editImageButton.setGraphic(createEditImageIcon());
        this.editImageButton.setText(null);
        this.editImageButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        this.editImageButton.setTooltip(new Tooltip("Choose image"));
        this.editImageButton.getStyleClass().addAll("veldt-back-button", "add-instance-overlay-image-edit-button");
        this.editImageButton.setOnAction(_ -> chooseInstanceImage());

        StackPane.setAlignment(this.editImageButton, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(this.editImageButton, new Insets(0, 6, 6, 0));

        this.instanceImagePicker.getChildren().setAll(this.instanceImagePreview, this.editImageButton);
        updateInstanceImageState();
    }

    private Rectangle createInstanceImagePreviewClip() {
        Rectangle clip = new Rectangle(88, 88);
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        clip.widthProperty().bind(this.instanceImagePreview.widthProperty());
        clip.heightProperty().bind(this.instanceImagePreview.heightProperty());
        return clip;
    }

    private Node createEditImageIcon() {
        FontIcon icon = new FontIcon(FontAwesomeSolid.PENCIL_ALT);
        icon.getStyleClass().add("add-instance-overlay-image-edit-icon");
        return icon;
    }

    private void refreshSupportedInstanceChoices() {
        List<String> options = JsonInstanceStore.INSTANCE.loadAll().stream()
                .filter(instance -> instance.type() == InstanceType.VANILLA || instance.type() == InstanceType.MODPACK)
                .map(StoredInstanceMetadata::id)
                .filter(id -> !this.selectedSupportedInstanceIds.contains(id))
                .sorted()
                .toList();

        String previousValue = this.supportedInstanceIdChoice.getValue();
        this.supportedInstanceIdChoice.getItems().setAll(options);

        if (previousValue != null && options.contains(previousValue)) {
            this.supportedInstanceIdChoice.setValue(previousValue);
        } else {
            this.supportedInstanceIdChoice.getSelectionModel().clearSelection();
        }

        this.supportedInstanceIdChoice.setDisable(options.isEmpty());
        if (options.isEmpty()) {
            this.supportedInstanceIdChoice.setPromptText("No vanilla or modpack instances available");
        } else {
            this.supportedInstanceIdChoice.setPromptText("Select supported instance");
        }
    }

    private void refreshSupportedInstanceChips() {
        this.supportedInstanceChipPane.getChildren().clear();
        if (this.selectedSupportedInstanceIds.isEmpty()) {
            this.supportedInstanceChipPane.getChildren().add(this.supportedInstancePlaceholder);
            return;
        }

        for (String instanceId : this.selectedSupportedInstanceIds) {
            this.supportedInstanceChipPane.getChildren().add(createSupportedInstanceChip(instanceId));
        }
    }

    private HBox createSupportedInstanceChip(String instanceId) {
        Label chipLabel = new Label(instanceId);
        chipLabel.getStyleClass().add("add-instance-overlay-chip-label");

        Button removeButton = new Button("x");
        removeButton.getStyleClass().add("add-instance-overlay-chip-remove");
        removeButton.setOnAction(_ -> this.selectedSupportedInstanceIds.remove(instanceId));

        HBox chip = new HBox(8, chipLabel, removeButton);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("add-instance-overlay-chip");
        return chip;
    }

    private void chooseInstanceImage() {
        if (getScene() == null || getScene().getWindow() == null)
            return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Instance Image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        String currentPath = trimToNull(this.selectedIconPath);
        if (currentPath != null) {
            try {
                Path currentFile = Path.of(currentPath);
                Path parent = currentFile.getParent();
                if (parent != null && Files.isDirectory(parent)) {
                    chooser.setInitialDirectory(parent.toFile());
                }
            } catch (RuntimeException ignored) {
            }
        }

        File chosenFile = chooser.showOpenDialog(getScene().getWindow());
        if (chosenFile == null)
            return;

        String chosenPath = chosenFile.getAbsolutePath();
        Image previewImage = loadInstanceImagePreview(chosenPath);
        if (previewImage == null || previewImage.isError()) {
            showError("Selected image could not be loaded.");
            return;
        }

        this.selectedIconPath = chosenPath;
        clearError();
        updateInstanceImageState();
    }

    private void updateInstanceImageState() {
        String iconPath = trimToNull(this.selectedIconPath);

        Image previewImage = iconPath == null ? loadDefaultInstanceImagePreview() : loadInstanceImagePreview(iconPath);
        boolean hasPreview = previewImage != null && !previewImage.isError();
        this.instanceImagePreviewView.setImage(previewImage);
        this.instanceImagePreviewView.setManaged(hasPreview);
        this.instanceImagePreviewView.setVisible(hasPreview);
        this.instanceImagePreviewFallback.setManaged(!hasPreview);
        this.instanceImagePreviewFallback.setVisible(!hasPreview);
    }

    private Image loadInstanceImagePreview(String iconPath) {
        try {
            return new Image(Path.of(iconPath).toUri().toString(), 88, 88, true, true, false);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Image loadDefaultInstanceImagePreview() {
        try {
            return Images.load(DEFAULT_INSTANCE_IMAGE, 88, 88, true, true, false);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void loadVersionsAsync() {
        Thread loaderThread = new Thread(() -> {
            try {
                List<VersionManifestEntry> versions = MojangVersionManifestService.INSTANCE.fetchVersions();
                Platform.runLater(() -> applyLoadedVersions(versions));
            } catch (Exception exception) {
                Platform.runLater(() -> applyVersionLoadFailure(exception));
            }
        }, "add-instance-version-loader");
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    private void applyLoadedVersions(List<VersionManifestEntry> versions) {
        this.availableVersions = versions == null ? List.of() : List.copyOf(versions);
        refreshVersionChoices();

        this.vanillaVersionChoice.setDisable(false);
        this.modpackVersionChoice.setDisable(false);
        this.serverVersionChoice.setDisable(false);

        this.vanillaVersionChoice.setPromptText("Select version");
        this.modpackVersionChoice.setPromptText("Select version");
        this.serverVersionChoice.setPromptText("Optional version");
    }

    private void refreshVersionChoices() {
        String selectedVanillaVersion = this.vanillaVersionChoice.getValue();
        String selectedModpackVersion = this.modpackVersionChoice.getValue();
        String selectedServerVersion = this.serverVersionChoice.getValue();

        List<String> versions = filteredVersionIds();
        this.vanillaVersionChoice.getItems().setAll(versions);
        this.modpackVersionChoice.getItems().setAll(versions);
        this.serverVersionChoice.getItems().setAll(versions);

        restoreSelection(this.vanillaVersionChoice, selectedVanillaVersion, false);
        restoreSelection(this.modpackVersionChoice, selectedModpackVersion, false);
        restoreSelection(this.serverVersionChoice, selectedServerVersion, true);
    }

    private List<String> filteredVersionIds() {
        boolean includeSnapshots = this.includeSnapshotsCheckBox.isSelected();
        return this.availableVersions.stream()
                .filter(entry -> entry.getVersionType() == VersionManifestEntry.VersionType.RELEASE
                        || includeSnapshots && entry.getVersionType() == VersionManifestEntry.VersionType.SNAPSHOT)
                .map(VersionManifestEntry::id)
                .toList();
    }

    private void restoreSelection(ComboBox<String> choiceBox, String previousValue, boolean allowBlank) {
        if (previousValue != null && choiceBox.getItems().contains(previousValue)) {
            choiceBox.setValue(previousValue);
            return;
        }

        if (allowBlank) {
            choiceBox.getSelectionModel().clearSelection();
            choiceBox.setValue(null);
            return;
        }

        if (!choiceBox.getItems().isEmpty()) {
            choiceBox.getSelectionModel().selectFirst();
        } else {
            choiceBox.setValue(null);
        }
    }

    private void applyVersionLoadFailure(Exception exception) {
        this.vanillaVersionChoice.setPromptText("Failed to load versions");
        this.modpackVersionChoice.setPromptText("Failed to load versions");
        this.serverVersionChoice.setPromptText("Failed to load versions");
        showError("Failed to load Minecraft versions: " + exception.getMessage());
    }

    private void createInstance() {
        clearError();

        try {
            StoredInstanceMetadata instance = switch (getSelectedType()) {
                case MODPACK -> buildModpackInstance();
                case SERVER -> buildServerInstance();
                case VANILLA -> buildVanillaInstance();
            };

            JsonInstanceStore.INSTANCE.save(instance);
            this.onCreated.run();
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private StoredVanillaInstanceMetadata buildVanillaInstance() {
        String version = requireSelection(this.vanillaVersionChoice, "Minecraft version is required.");
        String name = requireValue(this.vanillaNameField.getText(), "Instance name is required.");
        String id = uniqueId(name);
        Instant now = Instant.now();
        Path instanceDirectory = FileConfig.resolveInstanceDirectory(id);
        return new StoredVanillaInstanceMetadata(
                id,
                name,
                version,
                instanceDirectory,
                true,
                persistSelectedIcon(instanceDirectory),
                now,
                null
        );
    }

    private StoredModpackInstanceMetadata buildModpackInstance() {
        String version = requireSelection(this.modpackVersionChoice, "Minecraft version is required.");
        ModLoader modLoader = this.modpackLoaderChoice.getValue();
        if (modLoader == null)
            throw new IllegalArgumentException("A mod loader is required.");

        String name = requireValue(this.modpackNameField.getText(), "Instance name is required.");
        String id = uniqueId(name);
        Instant now = Instant.now();
        Path instanceDirectory = FileConfig.resolveInstanceDirectory(id);
        return new StoredModpackInstanceMetadata(
                id,
                name,
                version,
                modLoader,
                instanceDirectory,
                true,
                persistSelectedIcon(instanceDirectory),
                now,
                null
        );
    }

    private StoredServerInstanceMetadata buildServerInstance() {
        String name = requireValue(this.serverNameField.getText(), "Instance name is required.");
        String address = requireServerAddress(this.serverAddressField.getText());
        List<String> supportedIds = List.copyOf(this.selectedSupportedInstanceIds);
        if (supportedIds.isEmpty())
            throw new IllegalArgumentException("At least one supported instance id is required.");

        String version = trimToNull(this.serverVersionChoice.getValue());
        String id = uniqueId(name);
        Instant now = Instant.now();
        Path instanceDirectory = FileConfig.resolveInstanceDirectory(id);
        return new StoredServerInstanceMetadata(
                id,
                name,
                address,
                version,
                supportedIds,
                instanceDirectory,
                true,
                persistSelectedIcon(instanceDirectory),
                now,
                null
        );
    }

    private String persistSelectedIcon(Path instanceDirectory) {
        String iconPath = trimToNull(this.selectedIconPath);
        if (iconPath == null)
            return null;

        try {
            Files.createDirectories(instanceDirectory);

            Path sourcePath = Path.of(iconPath);
            String fileName = sourcePath.getFileName() == null ? "icon" : sourcePath.getFileName().toString();
            String extension = "";
            int extensionIndex = fileName.lastIndexOf('.');
            if (extensionIndex >= 0) {
                extension = fileName.substring(extensionIndex);
            }

            Path targetPath = instanceDirectory.resolve("icon" + extension);
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toString();
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException("Failed to store instance image: " + exception.getMessage(), exception);
        }
    }

    private InstanceType getSelectedType() {
        Toggle selectedToggle = this.typeToggleGroup.getSelectedToggle();
        if (selectedToggle == null)
            return InstanceType.VANILLA;

        return (InstanceType) selectedToggle.getUserData();
    }

    private String getSelectedTypeLabel() {
        return switch (getSelectedType()) {
            case MODPACK -> "Modpack";
            case SERVER -> "Server";
            case VANILLA -> "Vanilla";
        };
    }

    private TextField getActiveNameField() {
        return switch (getSelectedType()) {
            case MODPACK -> this.modpackNameField;
            case SERVER -> this.serverNameField;
            case VANILLA -> this.vanillaNameField;
        };
    }

    private String uniqueId(String seed) {
        String base = slugify(seed);
        if (base.isBlank()) {
            base = "instance";
        }

        List<String> existingIds = JsonInstanceStore.INSTANCE.loadAll().stream()
                .map(StoredInstanceMetadata::id)
                .toList();

        String candidate = base;
        int suffix = 2;
        while (existingIds.contains(candidate)) {
            candidate = base + "-" + suffix++;
        }

        return candidate;
    }

    private String slugify(String value) {
        String normalized = trimToNull(value);
        if (normalized == null)
            return "";

        String slug = normalized.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return slug;
    }

    private String requireValue(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null)
            throw new IllegalArgumentException(message);

        return trimmed;
    }

    private String requireSelection(ComboBox<String> comboBox, String message) {
        if (comboBox.isDisabled())
            throw new IllegalArgumentException("Minecraft versions are still loading.");

        String value = trimToNull(comboBox.getValue());
        if (value == null)
            throw new IllegalArgumentException(message);

        return value;
    }

    private String requireServerAddress(String value) {
        String address = requireValue(value, "Server address is required.");
        if (address.contains("://"))
            throw new IllegalArgumentException("Server address must be in the format host or host:port.");

        if (address.chars().anyMatch(Character::isWhitespace))
            throw new IllegalArgumentException("Server address cannot contain spaces.");

        HostAndPort hostAndPort = parseHostAndPort(address);
        if (!isValidHost(hostAndPort.host()))
            throw new IllegalArgumentException("Server address must use a valid host name, IPv4 address, or localhost.");

        if (hostAndPort.port() != null && !isValidPort(hostAndPort.port()))
            throw new IllegalArgumentException("Server port must be a number between 1 and 65535.");

        return address;
    }

    private HostAndPort parseHostAndPort(String value) {
        if (value.startsWith("[")) {
            int closingBracket = value.indexOf(']');
            if (closingBracket <= 1 || closingBracket != value.lastIndexOf(']'))
                throw new IllegalArgumentException("Server address must be in the format host or host:port.");

            String host = value.substring(1, closingBracket);
            if (closingBracket == value.length() - 1)
                return new HostAndPort(host, null);

            if (value.charAt(closingBracket + 1) != ':')
                throw new IllegalArgumentException("Server address must be in the format host or host:port.");

            String port = value.substring(closingBracket + 2);
            return new HostAndPort(host, port);
        }

        int firstColon = value.indexOf(':');
        if (firstColon < 0)
            return new HostAndPort(value, null);

        if (firstColon != value.lastIndexOf(':'))
            throw new IllegalArgumentException("IPv6 server addresses must be wrapped in brackets, for example [::1]:25565.");

        String host = value.substring(0, firstColon);
        String port = value.substring(firstColon + 1);
        return new HostAndPort(host, port);
    }

    private boolean isValidHost(String host) {
        if (host == null || host.isBlank())
            return false;

        if ("localhost".equalsIgnoreCase(host))
            return true;

        if (host.contains(":"))
            return isValidIpv6(host);

        if (host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+"))
            return isValidIpv4(host);

        if (host.startsWith(".") || host.endsWith(".") || host.length() > 253)
            return false;

        String[] labels = host.split("\\.");
        for (String label : labels) {
            if (label.isBlank() || label.length() > 63)
                return false;

            if (!Character.isLetterOrDigit(label.charAt(0)) || !Character.isLetterOrDigit(label.charAt(label.length() - 1)))
                return false;

            for (int index = 0; index < label.length(); index++) {
                char character = label.charAt(index);
                if (!Character.isLetterOrDigit(character) && character != '-')
                    return false;
            }
        }

        return true;
    }

    private boolean isValidIpv4(String host) {
        String[] parts = host.split("\\.");
        if (parts.length != 4)
            return false;

        for (String part : parts) {
            if (part.isBlank() || part.length() > 3)
                return false;

            try {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255)
                    return false;
            } catch (NumberFormatException exception) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidIpv6(String host) {
        if (!host.matches("[0-9a-fA-F:.]+"))
            return false;

        long colonCount = host.chars().filter(character -> character == ':').count();
        return colonCount >= 2;
    }

    private boolean isValidPort(String port) {
        if (port == null || port.isBlank() || !port.chars().allMatch(Character::isDigit))
            return false;

        try {
            int value = Integer.parseInt(port);
            return value >= 1 && value <= 65535;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private String trimToNull(String value) {
        if (value == null)
            return null;

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void showError(String message) {
        this.errorLabel.setText(message);
        this.errorLabel.setManaged(true);
        this.errorLabel.setVisible(true);
    }

    private void clearError() {
        this.errorLabel.setText("");
        this.errorLabel.setManaged(false);
        this.errorLabel.setVisible(false);
    }

    private void close() {
        this.onClose.run();
    }

    private record HostAndPort(String host, String port) {
    }
}
