package dev.turtywurty.veldtlauncher.auth.session.secret;

import com.github.javakeyring.Keyring;

import java.util.Optional;

public class OSCredentialSecretStore implements SecretStore {
    public static final OSCredentialSecretStore INSTANCE = new OSCredentialSecretStore();

    private final String serviceName;

    public OSCredentialSecretStore(String serviceName) {
        this.serviceName = serviceName;
    }

    public OSCredentialSecretStore() {
        this("veldt-launcher");
    }

    @Override
    public Optional<String> load(String key) {
        if(key == null || key.isEmpty())
            return Optional.empty();

        try(Keyring keyring = Keyring.create()) {
            return Optional.ofNullable(keyring.getPassword(this.serviceName, key));
        } catch (Exception exception) {
            throw new KeystoreException("Failed to load secret for key: " + key, exception);
        }
    }

    @Override
    public void save(String key, String secret) {
        if(key == null || key.isEmpty())
            throw new KeystoreException("Key cannot be null or empty");

        if(secret == null)
            throw new KeystoreException("Secret cannot be null");

        try(Keyring keyring = Keyring.create()) {
            keyring.setPassword(this.serviceName, key, secret);
        } catch (Exception exception) {
            throw new KeystoreException("Failed to save secret for key: " + key, exception);
        }
    }

    @Override
    public void delete(String key) {
        if(key == null || key.isEmpty())
            throw new KeystoreException("Key cannot be null or empty");

        try(Keyring keyring = Keyring.create()) {
            keyring.deletePassword(this.serviceName, key);
        } catch (Exception exception) {
            throw new KeystoreException("Failed to delete secret for key: " + key, exception);
        }
    }
}
