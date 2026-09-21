package net.gobbob.mobends.client.animation;

import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.animation.Animation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.gobbob.mobends.MoBends;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;

/**
 * {@code /evenmobends play <file> <animation>} loops an animation on yourself; {@code /evenmobends stop} ends it.
 * Files are resource paths under {@code geckolib/animations/}, e.g. {@code player/item_test}.
 */
public class CommandEvenMoBends extends CommandBase {
    @Override
    public String getCommandName() {
        return "evenmobends";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/evenmobends play <file> <animation> | /evenmobends stop | /evenmobends blockbench [slim|classic]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length >= 1 && args[0].equals("blockbench")) {
            Boolean slim = args.length > 1 ? Boolean.valueOf(args[1].equals("slim")) : null;
            try {
                java.io.File file = BlockbenchExport.export(Minecraft.getMinecraft().thePlayer, slim);
                sender.addChatMessage(new ChatComponentText("Wrote " + file.getAbsolutePath()));
            } catch (java.io.IOException e) {
                sender.addChatMessage(new ChatComponentText("Couldn't write the Blockbench project: " + e.getMessage()));
            }
            return;
        }
        if (args.length == 1 && args[0].equals("stop")) {
            AnimationPreview.stop();
            sender.addChatMessage(new ChatComponentText("Stopped the animation preview."));
            return;
        }
        if (args.length != 3 || !args[0].equals("play")) throw new WrongUsageException(this.getCommandUsage(sender));
        ResourceLocation file = file(args[1]);
        String name = AnimationPreview.start(file, args[2]);
        if (name == null) {
            sender.addChatMessage(new ChatComponentText("No animation '" + args[2] + "' in " + file + "."));
            return;
        }
        sender.addChatMessage(new ChatComponentText("Playing " + name + " from " + file + ". /evenmobends stop to end."));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "play", "stop", "blockbench");
        if (args.length == 2 && args[0].equals("blockbench")) return getListOfStringsMatchingLastWord(args, "slim", "classic");
        if (args.length == 2 && args[0].equals("play")) return getListOfStringsMatchingLastWord(args, files());
        if (args.length == 3 && args[0].equals("play")) {
            List<String> names = new ArrayList<String>();
            Map<String, Animation> animations = GeckoLibResources.getBakedAnimations().getAnimations(file(args[1]), null).animations();
            for (String name : animations.keySet()) names.add(name.substring(name.lastIndexOf('.') + 1));
            return getListOfStringsMatchingLastWord(args, names.toArray(new String[names.size()]));
        }
        return null;
    }

    private static ResourceLocation file(String path) {
        return path.contains(":") ? new ResourceLocation(path) : new ResourceLocation(MoBends.MODID, path);
    }

    private static String[] files() {
        List<String> files = new ArrayList<String>();
        for (ResourceLocation location : GeckoLibResources.getBakedAnimations().cache().keySet()) {
            if (!location.getResourceDomain().equals(MoBends.MODID)) continue;
            String path = GeckoLibResources.stripPrefixAndSuffix(location).getResourcePath();
            if (!files.contains(path)) files.add(path);
        }
        return files.toArray(new String[files.size()]);
    }
}
