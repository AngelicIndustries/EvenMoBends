package net.gobbob.mobends;

import cpw.mods.fml.client.registry.RenderingRegistry;
import java.util.ArrayList;
import java.util.List;
import net.gobbob.mobends.animation.Animation;
import net.gobbob.mobends.animation.player.Animation_Attack;
import net.gobbob.mobends.animation.player.Animation_Axe;
import net.gobbob.mobends.animation.player.Animation_Bow;
import net.gobbob.mobends.animation.player.Animation_Climbing;
import net.gobbob.mobends.animation.player.Animation_Diving;
import net.gobbob.mobends.animation.player.Animation_Falling;
import net.gobbob.mobends.animation.player.Animation_Flying;
import net.gobbob.mobends.animation.player.Animation_Jump;
import net.gobbob.mobends.animation.player.Animation_Mining;
import net.gobbob.mobends.animation.player.Animation_Riding;
import net.gobbob.mobends.animation.player.Animation_Rowing;
import net.gobbob.mobends.animation.player.Animation_Sneak;
import net.gobbob.mobends.animation.player.Animation_Sprint;
import net.gobbob.mobends.animation.player.Animation_Swimming;
import net.gobbob.mobends.animation.zombie.Animation_Stand;
import net.gobbob.mobends.animation.zombie.Animation_Walk;
import net.gobbob.mobends.client.renderer.entity.RenderBendsCaveSpider;
import net.gobbob.mobends.client.renderer.entity.RenderBendsPlayer;
import net.gobbob.mobends.client.renderer.entity.RenderBendsSpider;
import net.gobbob.mobends.client.renderer.entity.RenderBendsZombie;
import net.gobbob.mobends.compat.CompatibilityPolicy;
import net.gobbob.mobends.util.BendsLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityCaveSpider;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;

public class AnimatedEntity {
    public static final AnimatedEntity[] animatedEntities = createAnimatedEntities();
    public String id;
    public String displayName;
    public Entity entity;
    public Class<? extends Entity> entityClass;
    public Render renderer;
    public List<Animation> animations = new ArrayList<Animation>();
    public boolean animate = true;

    public AnimatedEntity(String argID, String argDisplayName, Entity argEntity, Class<? extends Entity> argClass, Render argRenderer) {
        this.id = argID;
        this.displayName = argDisplayName;
        this.entityClass = argClass;
        this.renderer = argRenderer;
        this.entity = argEntity;
        this.animate = true;
    }

    public AnimatedEntity add(Animation argGroup) {
        this.animations.add(argGroup);
        return this;
    }

    private static AnimatedEntity[] createAnimatedEntities() {
        List<AnimatedEntity> entities = new ArrayList<AnimatedEntity>();

        if (CompatibilityPolicy.arePlayerAnimationsEnabled()) {
            entities.add(
                new AnimatedEntity(
                    "player",
                    "Player",
                    Minecraft.getMinecraft().thePlayer,
                    EntityPlayer.class,
                    new RenderBendsPlayer())
                        .add(new net.gobbob.mobends.animation.player.Animation_Stand())
                        .add(new net.gobbob.mobends.animation.player.Animation_Walk())
                        .add(new Animation_Sneak())
                        .add(new Animation_Sprint())
                        .add(new Animation_Jump())
                        .add(new Animation_Falling())
                        .add(new Animation_Flying())
                        .add(new Animation_Climbing())
                        .add(new Animation_Diving())
                        .add(new Animation_Attack())
                        .add(new Animation_Swimming())
                        .add(new Animation_Bow())
                        .add(new net.gobbob.mobends.animation.player.Animation_Eating())
                        .add(new Animation_Riding())
                        .add(new Animation_Rowing())
                        .add(new Animation_Mining())
                        .add(new Animation_Axe()));
        }

        entities.add(
            new AnimatedEntity(
                "zombie", "Zombie", new EntityZombie(null), EntityZombie.class, new RenderBendsZombie())
                    .add(new Animation_Stand())
                    .add(new Animation_Walk()));
        // Subclasses must precede their parent for getByEntity's first-match lookup.
        entities.add(
            new AnimatedEntity(
                "cave_spider", "Cave Spider", new EntityCaveSpider(null), EntityCaveSpider.class, new RenderBendsCaveSpider()));
        entities.add(
            new AnimatedEntity(
                "spider", "Spider", new EntitySpider(null), EntitySpider.class, new RenderBendsSpider()));

        return entities.toArray(new AnimatedEntity[entities.size()]);
    }

    public static void registerRendering() {
        for (int i = 0; i < animatedEntities.length; ++i) {
            if (!AnimatedEntity.animatedEntities[i].animate) continue;
            RenderingRegistry.registerEntityRenderingHandler(AnimatedEntity.animatedEntities[i].entityClass, AnimatedEntity.animatedEntities[i].renderer);
        }
        BendsLogger.log("Registering Animated Entities...", BendsLogger.INFO);
    }

    public Animation get(String argName) {
        for (int i = 0; i < this.animations.size(); ++i) {
            if (!this.animations.get(i).getName().equalsIgnoreCase(argName)) continue;
            return this.animations.get(i);
        }
        return null;
    }

    public static AnimatedEntity getByEntity(Entity argEntity) {
        for (int i = 0; i < animatedEntities.length; ++i) {
            if (!AnimatedEntity.animatedEntities[i].entityClass.isInstance(argEntity)) continue;
            return animatedEntities[i];
        }
        return null;
    }
}
