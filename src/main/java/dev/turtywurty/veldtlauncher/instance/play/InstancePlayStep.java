package dev.turtywurty.veldtlauncher.instance.play;

public enum InstancePlayStep {
    AUTHENTICATING("Authenticating"),
    FETCHING_METADATA("Fetching Metadata"),
    PREPARING_DIRECTORIES("Preparing Directories"),
    SAVING_METADATA("Saving Version Metadata"),
    INSTALLING_CLIENT("Installing Client"),
    INSTALLING_LIBRARIES("Installing Libraries"),
    INSTALLING_ASSET_INDEX("Installing Asset Index"),
    INSTALLING_ASSETS("Installing Assets"),
    INSTALLING_LOGGING("Installing Logging Config"),
    LAUNCHING("Launching");

    private final String displayName;

    InstancePlayStep(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return this.displayName;
    }
}
