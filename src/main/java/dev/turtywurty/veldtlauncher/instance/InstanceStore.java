package dev.turtywurty.veldtlauncher.instance;

import java.util.List;
import java.util.Optional;

public interface InstanceStore {
    List<StoredInstanceMetadata> loadAll();

    default Optional<StoredInstanceMetadata> load() {
        return loadLastPlayed().or(() -> {
            List<StoredInstanceMetadata> instances = loadAll();
            if (instances.isEmpty())
                return Optional.empty();

            return Optional.of(instances.getLast());
        });
    }

    default Optional<StoredInstanceMetadata> load(String instanceId) {
        if (instanceId == null || instanceId.isBlank())
            return Optional.empty();

        return loadAll().stream()
                .filter(instance -> instanceId.equals(instance.id()))
                .findFirst();
    }

    Optional<StoredInstanceMetadata> loadLastPlayed();

    boolean hasLastPlayed();

    void save(StoredInstanceMetadata instance);

    void setLastPlayed(String instanceId);

    void clearLastPlayed();

    void delete(String instanceId);

    void clear();
}
