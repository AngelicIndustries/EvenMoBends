package net.gobbob.mobends.event;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

/**
 * On the server: a zombie (a zombie pigman, say) that hits with something in its hand swings it, so clients see the
 * swing and animate it. Zombies using vanilla's AI already do; zombie pigmen attack the old way, without swinging.
 */
public class EventHandler_MobSwing {
    @SubscribeEvent
    public void onAttack(LivingAttackEvent event) {
        if (event.entity.worldObj.isRemote || !(event.source.getEntity() instanceof EntityZombie)) return;
        EntityLivingBase attacker = (EntityLivingBase)event.source.getEntity();
        // Melee only: the attacker itself dealt the blow, and it holds something to swing.
        if (event.source.getSourceOfDamage() == attacker && attacker.getHeldItem() != null && !attacker.isSwingInProgress) {
            attacker.swingItem();
        }
    }
}
