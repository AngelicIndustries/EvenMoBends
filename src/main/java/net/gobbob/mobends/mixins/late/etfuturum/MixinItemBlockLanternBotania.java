package net.gobbob.mobends.mixins.late.etfuturum;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.gobbob.mobends.client.render.WornLanternRenderer;

/**
 * Et Futurum draws a worn lantern as a flat icon through Botania's bauble renderer; with Mo' Bends' worn lanterns on,
 * that steps aside so only the hanging one shows. Applied only when Botania is loaded, which the method needs.
 */
@Pseudo
@Mixin(targets = "ganymedes01.etfuturum.blocks.itemblocks.ItemBlockLantern", remap = false)
public abstract class MixinItemBlockLanternBotania {

    @Inject(method = "onPlayerBaubleRender", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void mobends$hangInstead(CallbackInfo ci) {
        if (WornLanternRenderer.enabled) ci.cancel();
    }
}
