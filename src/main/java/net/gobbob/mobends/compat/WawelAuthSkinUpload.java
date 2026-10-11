package net.gobbob.mobends.compat;

import java.awt.image.BufferedImage;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import net.gobbob.mobends.compat.wawelauth.WawelAuthBridge;
import net.minecraft.client.resources.I18n;

/** The editor's skin upload to the active Wawel Auth account. Safe to load without Wawel Auth. */
public final class WawelAuthSkinUpload {

    private WawelAuthSkinUpload() {}

    public static Account activeAccount() {
        return WawelAuthCompat.isLoaded() ? WawelAuthBridge.activeAccount() : null;
    }

    /**
     * Uploads the skin, or stores it as the local skin of an offline account. Wawel Auth refreshes its caches itself.
     * The completion runs on the client thread.
     */
    public static void upload(Account account, BufferedImage image, boolean slim, Completion completion) {
        if (!WawelAuthCompat.isLoaded() || account == null || image == null) {
            completion.finished(null, I18n.format("mobends.upload.unavailable"));
            return;
        }
        WawelAuthBridge.uploadSkin(account, image, slim)
            .whenComplete((message, error) -> {
                if (error == null) {
                    completion.finished(message == null ? I18n.format("mobends.upload.completed") : message, null);
                } else {
                    completion.finished(null, usefulMessage(unwrap(error)));
                }
            });
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String usefulMessage(Throwable error) {
        if (error == null) return I18n.format("mobends.upload.unknownError");
        String message = error.getMessage();
        return message == null || message.trim().isEmpty() ? error.getClass().getSimpleName() : message;
    }

    public interface Completion {
        void finished(String result, String error);
    }

    public static final class Account {
        public final long id;
        public final String name;
        public final UUID uuid;
        public final String provider;
        public final boolean offline;

        public Account(long id, String name, UUID uuid, String provider, boolean offline) {
            this.id = id;
            this.name = name == null ? "?" : name;
            this.uuid = uuid;
            this.provider = provider == null ? "?" : provider;
            this.offline = offline;
        }
    }
}
