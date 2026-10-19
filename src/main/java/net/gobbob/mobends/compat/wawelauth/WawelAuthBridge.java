package net.gobbob.mobends.compat.wawelauth;

import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

import net.gobbob.mobends.compat.WawelAuthSkinUpload.Account;
import net.minecraft.client.entity.AbstractClientPlayer;

import industries.angelic.wawelauth.api.account.AccountInfo;
import industries.angelic.wawelauth.api.account.WawelAccounts;
import industries.angelic.wawelauth.api.skin.PlayerSkin;
import industries.angelic.wawelauth.api.skin.PlayerSkins;
import industries.angelic.wawelauth.wawelcore.data.SkinModel;

/** Wawel Auth's skin and account API. Loaded only when Wawel Auth is present. */
public final class WawelAuthBridge {

    private WawelAuthBridge() {}

    public static boolean isSlim(AbstractClientPlayer player) {
        return PlayerSkins.get(player).model() == SkinModel.SLIM;
    }

    public static BufferedImage originalSkin(AbstractClientPlayer player) {
        PlayerSkin skin = PlayerSkins.get(player);
        return skin.available() ? skin.originalImage() : null;
    }

    public static Account activeAccount() {
        AccountInfo account = WawelAccounts.active();
        if (account == null) return null;
        return new Account(account.id(), account.profileName(), account.profileUuid(), account.providerName(),
            account.offline());
    }

    /** Completes on the client thread. */
    public static CompletableFuture<String> uploadSkin(Account account, BufferedImage image, boolean slim) {
        AccountInfo info = new AccountInfo(account.id, account.name, account.uuid, account.provider, account.offline);
        return WawelAccounts.uploadSkin(info, image, slim ? SkinModel.SLIM : SkinModel.CLASSIC);
    }
}
