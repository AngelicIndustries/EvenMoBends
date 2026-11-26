package net.gobbob.mobends.client.gui;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.gobbob.mobends.AnimatedEntity;
import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.client.animation.AnimationPreview;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import net.gobbob.mobends.client.renderer.entity.RenderBendsPlayer;
import net.gobbob.mobends.client.renderer.entity.RenderBendsSpider;
import net.gobbob.mobends.client.renderer.entity.RenderBendsZombie;
import net.gobbob.mobends.compat.CemCompat;
import net.gobbob.mobends.compat.CompatibilityPolicy;
import net.gobbob.mobends.compat.salamander.PackFaces;
import net.gobbob.mobends.config.PlayerAnimationConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/**
 * The Mo' Bends menu, opened from the config screen: the animated mobs, each with a preview, whether to animate it,
 * and the command that writes its Blockbench project. The player's page also lists its animations, each to preview
 * and switch on or off. Without a mob chosen, a player shows off the animations. Animations themselves are Blockbench
 * files, edited outside the game; the expression editor is Salamander's.
 */
public class GuiMBMenu extends GuiScreen {
    public static final ResourceLocation TITLE = new ResourceLocation("mobends", "title.png");
    public static final ResourceLocation LOGO = new ResourceLocation("mobends", "textures/gui/logo.png");
    public static final ResourceLocation PREVIEW_BACKGROUND = new ResourceLocation("mobends", "textures/gui/displayBG.png");
    private static final int DONE = 0, BACK = 1, ANIMATE = 2, MOB = 100;
    /** No page switch waiting; see {@link #pendingPage}. */
    private static final int NO_PAGE = Integer.MIN_VALUE;
    /** Title texture size, for its width at a given height. */
    private static final float TITLE_ASPECT = 1024.0f / 122.0f;
    /** What the main page's player plays, a new one every {@link #SHOWCASE_SECONDS}: file and animation. */
    private static final String[][] SHOWCASE = {
        {"player/stand", "stand"}, {"player/walk", "walk"}, {"player/sprint", "sprint"}, {"player/sneak", "sneak_walk"},
        {"player/swim", "swim"}, {"player/fly", "fly_forward"}, {"player/climb", "climb_move"}, {"player/dive", "dive"},
        {"player/sword", "sword_stance"}, {"player/mine", "mine_swing"}, {"player/axe", "axe_swing"},
        {"player/eating", "eating"}, {"player/bow", "bow_aim"}};
    private static final double SHOWCASE_SECONDS = 10.0;
    /**
     * What shows each player animation switch on the player's page: file and animation. Crawling and rowing are drawn
     * in code, without a file of their own: the swimming stroke and the riding pose stand in for them.
     */
    private static final Map<String, String[]> SWITCH_PREVIEWS = new HashMap<String, String[]>();
    static {
        String[][] previews = {
            {"stand", "player/stand", "stand"}, {"walk", "player/walk", "walk"}, {"sneak", "player/sneak", "sneak_walk"},
            {"sprint", "player/sprint", "sprint"}, {"jump", "player/jump", "jump"}, {"falling", "player/fall", "fall"},
            {"flying", "player/fly", "fly_forward"}, {"climbing", "player/climb", "climb_move"},
            {"crawling", "player/swim", "swim"}, {"swimming", "player/swim", "swim"}, {"diving", "player/dive", "dive"},
            {"bow", "player/bow", "bow_aim"}, {"eating", "player/eating", "eating"}, {"riding", "player/ride", "ride"},
            {"rowing", "player/ride", "ride_move"}, {"mining", "player/mine", "mine_swing"},
            {"axe", "player/axe", "axe_swing"}, {"attack", "player/sword", "sword_1"},
            {"swordCombo1", "player/sword", "sword_1"}, {"swordCombo2", "player/sword", "sword_2"},
            {"swordCombo3", "player/sword", "sword_3"}, {"swordStance", "player/sword", "sword_stance"},
            {"punch", "player/punch", "punch_right"}, {"punchStance", "player/punch", "punch_left"}};
        for (String[] preview : previews) SWITCH_PREVIEWS.put(preview[0], new String[] {preview[1], preview[2]});
    }
    private static final int ROW_HEIGHT = 16, TOGGLE_WIDTH = 30, SCROLLBAR_WIDTH = 6;

    private final GuiScreen parent;
    private final long openedNanos = System.nanoTime();
    private final Random random = new Random();
    /** The chosen mob, or -1 for the main page. */
    private int selected = -1;
    /**
     * The page a button asked for, switched to before the next frame. Switching at once would rebuild the buttons
     * while the click still goes through them, and the new page's button under the pointer would take it too.
     */
    private int pendingPage = NO_PAGE;
    private long selectedNanos;
    private int showcase = -1;
    private double showcaseUntil;
    private GuiToggleButton animate;
    /** Where the command box was last drawn, for clicks; and until when it shows that it was copied. */
    private int commandX, commandY, commandWidth, commandHeight;
    private long copiedUntilMillis;
    /** The player page's animation list as last drawn, the first row shown, and the row the preview plays. */
    private int listX, listY, listWidth, listRows;
    private int scroll;
    private int previewing;
    private boolean draggingScrollbar;

    public GuiMBMenu(GuiScreen parent) {
        this.parent = parent;
        ++MoBends.refreshModel;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        if (this.selected < 0) {
            for (int i = 0; i < AnimatedEntity.animatedEntities.length; ++i) {
                this.buttonList.add(new GuiButton(MOB + i, 10, this.top() + 6 + i * 24, 90, 20,
                    I18n.format("mobends.entity." + AnimatedEntity.animatedEntities[i].id)));
            }
            this.buttonList.add(new GuiButton(DONE, 10, this.height - 30, 90, 20, I18n.format("gui.done")));
        } else {
            int box = this.previewSize();
            this.animate = new GuiToggleButton(ANIMATE, 10, 30 + box + 6, AnimatedEntity.animatedEntities[this.selected].animate)
                .setTitle(I18n.format("mobends.menu.animate"), Math.max(0, box - 40));
            this.buttonList.add(this.animate);
            this.buttonList.add(new GuiButton(BACK, 10, this.height - 30, 90, 20, I18n.format("mobends.gui.back")));
        }
    }

    /** Where the main page's content starts: below the title. */
    private int top() {
        return 10 + this.titleHeight() + 8;
    }

    private int titleHeight() {
        return this.height < 260 ? 16 : 20;
    }

    /** The mob page's preview box: as large as fits, up to 128. */
    private int previewSize() {
        return Math.max(48, Math.min(128, Math.min(this.height - 110, (this.width - 30) / 2)));
    }

    private boolean playerPage() {
        return this.selected >= 0 && AnimatedEntity.animatedEntities[this.selected].id.equals("player");
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id >= MOB) {
            this.pendingPage = button.id - MOB;
        } else if (button.id == DONE) {
            this.mc.displayGuiScreen(this.parent);
        } else if (button.id == BACK) {
            this.pendingPage = -1;
        } else if (button.id == ANIMATE) {
            AnimatedEntity entity = AnimatedEntity.animatedEntities[this.selected];
            entity.animate = !entity.animate;
            this.animate.toggleState = entity.animate;
            this.animate.displayString = I18n.format(entity.animate ? "mobends.gui.on" : "mobends.gui.off");
        }
    }

    private void switchPage() {
        if (this.pendingPage == NO_PAGE) return;
        int page = this.pendingPage;
        this.pendingPage = NO_PAGE;
        // The showcase starts over on the way back; the player's page plays the animation picked there.
        AnimationPreview.stopLoop(MenuPreview.showcasePlayer());
        this.showcase = -1;
        this.selected = page;
        this.selectedNanos = System.nanoTime();
        this.scroll = 0;
        this.draggingScrollbar = false;
        if (this.playerPage()) this.preview(0);
        this.initGui();
    }

    @Override
    public void updateScreen() {
        this.switchPage();
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (key != org.lwjgl.input.Keyboard.KEY_ESCAPE) return;
        // As Back does on a mob's page; from the main page, back to the config screen.
        if (this.selected >= 0) this.pendingPage = -1;
        else this.mc.displayGuiScreen(this.parent);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
        if (this.selected < 0 || button != 0 || this.pendingPage != NO_PAGE) return;
        if (this.overCommand(x, y)) {
            GuiScreen.setClipboardString(this.command());
            this.copiedUntilMillis = Minecraft.getSystemTime() + 2000L;
        } else if (this.playerPage()) {
            this.clickList(x, y);
        }
    }

    @Override
    protected void mouseClickMove(int x, int y, int button, long held) {
        super.mouseClickMove(x, y, button, held);
        if (this.draggingScrollbar) this.dragScrollbar(y);
    }

    @Override
    protected void mouseMovedOrUp(int x, int y, int button) {
        super.mouseMovedOrUp(x, y, button);
        if (button == 0) this.draggingScrollbar = false;
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && this.playerPage()) this.scrollTo(this.scroll - Integer.signum(wheel));
    }

    private boolean overCommand(int x, int y) {
        return x >= this.commandX && y >= this.commandY && x < this.commandX + this.commandWidth
            && y < this.commandY + this.commandHeight;
    }

    private String command() {
        String id = AnimatedEntity.animatedEntities[this.selected].id;
        return "/evenmobends blockbench" + (id.equals("player") ? "" : " " + (id.equals("cave_spider") ? "spider" : id));
    }

    @Override
    public void onGuiClosed() {
        AnimationPreview.stopLoop(MenuPreview.showcasePlayer());
        MoBends.saveConfig();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.switchPage();
        // In a world the menu lies over it; opened from the mods list nothing else clears the screen.
        if (this.mc.theWorld == null) this.drawDefaultBackground();
        else this.drawGradientRect(0, 0, this.width, this.height, 0xA0101010, 0xC0101010);
        double seconds = (System.nanoTime() - this.openedNanos) / 1.0e9;
        if (this.selected < 0) this.drawMainPage(seconds);
        else this.drawMobPage(mouseX, mouseY, (System.nanoTime() - this.selectedNanos) / 1.0e9);
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (this.selected >= 0 && this.overCommand(mouseX, mouseY)) {
            boolean copied = Minecraft.getSystemTime() < this.copiedUntilMillis;
            List<String> tooltip = Collections.singletonList(I18n.format(copied ? "mobends.menu.copied" : "mobends.menu.copy"));
            this.func_146283_a(tooltip, mouseX, mouseY);
        } else if (this.playerPage()) {
            this.drawListTooltip(mouseX, mouseY);
        }
    }

    /** The title and logo, the mob buttons sliding in one after another, and the showcase player. */
    private void drawMainPage(double seconds) {
        int titleHeight = this.titleHeight();
        int titleWidth = Math.round(titleHeight * TITLE_ASPECT);
        int logoSize = titleHeight;
        int groupX = (this.width - logoSize - 4 - titleWidth) / 2;
        int titleY = 10 - Math.round((1.0f - ease(seconds / 0.4)) * (titleHeight + 10));
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        this.mc.getTextureManager().bindTexture(LOGO);
        Gui.func_146110_a(groupX, titleY, 0, 0, logoSize, logoSize, logoSize, logoSize);
        this.mc.getTextureManager().bindTexture(TITLE);
        Gui.func_146110_a(groupX + logoSize + 4, titleY, 0, 0, titleWidth, titleHeight, titleWidth, titleHeight);
        for (Object object : this.buttonList) {
            GuiButton button = (GuiButton)object;
            if (button.id < MOB) continue;
            double start = 0.15 + (button.id - MOB) * 0.06;
            button.xPosition = Math.round(-100 + 110 * ease((seconds - start) / 0.35));
        }
        this.drawShowcase(seconds);
        String owner = CompatibilityPolicy.playerOwner();
        if (owner != null) {
            int noteY = this.height - 26;
            for (Object line : this.fontRendererObj.listFormattedStringToWidth(
                I18n.format("mobends.menu.override.player", owner), this.width - 120)) {
                this.drawString(this.fontRendererObj, (String)line, 110, noteY, OVERRIDE_COLOR);
                noteY += 10;
            }
        }
    }

    /** A player spinning once every 10 seconds, playing another of its animations each time round. */
    private void drawShowcase(double seconds) {
        EntityLivingBase player = MenuPreview.showcasePlayer();
        if (seconds >= this.showcaseUntil || this.showcase < 0) {
            int next = this.random.nextInt(SHOWCASE.length);
            if (next == this.showcase) next = (next + 1) % SHOWCASE.length;
            this.showcase = next;
            this.showcaseUntil = seconds + SHOWCASE_SECONDS;
            AnimationPreview.loopOn(player, new ResourceLocation("mobends", SHOWCASE[next][0]), SHOWCASE[next][1]);
        }
        int left = 110, top = this.top();
        int scale = Math.max(20, Math.min(70, (this.height - top - 20) / 3));
        int x = (left + this.width) / 2, y = this.height - Math.max(30, (this.height - top - scale * 2) / 2);
        float yaw = (float)(seconds * 36.0 % 360.0);
        MenuPreview.draw(player, () -> renderLivingEntity(x, y, scale, yaw, player));
    }

    /** The mob's preview box and toggle, the command that writes its Blockbench project, and the player's animations. */
    private void drawMobPage(int mouseX, int mouseY, double seconds) {
        AnimatedEntity entity = AnimatedEntity.animatedEntities[this.selected];
        boolean player = this.playerPage();
        this.drawCenteredString(this.fontRendererObj, I18n.format("mobends.menu.customization"), this.width / 2, 10, 0xFFFFFF);
        int box = this.previewSize();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        this.mc.getTextureManager().bindTexture(PREVIEW_BACKGROUND);
        Gui.func_146110_a(10, 30, 0, 0, box, box, box, box);
        // The player's page plays the picked animation on the stand-in, never on the player in the world.
        EntityLivingBase shown = player ? MenuPreview.showcasePlayer() : (EntityLivingBase)entity.entity;
        // Mobs walk on the spot, as they always have here.
        if (!player) shown.moveForward = 1.0f;
        // Spiders are wide and low: smaller, and higher in the box, for their legs to stay inside it as they turn.
        boolean spider = shown instanceof EntitySpider;
        int scale = Math.max(12, box * (spider ? 38 : 50) / 128);
        int baseY = 30 + box - box * (spider ? 44 : 22) / 128;
        float yaw = (float)(seconds * (player ? 36.0 : 72.0) % 360.0);
        MenuPreview.draw(shown, () -> renderLivingEntity(10 + box / 2, baseY, scale, yaw, shown));

        int textX = 10 + box + 12, textWidth = this.width - textX - 10;
        if (textWidth < 120) {
            textX = 10;
            textWidth = this.width - 20;
        }
        int textY = textX == 10 ? 30 + box + 34 : 34;
        for (Object line : this.fontRendererObj.listFormattedStringToWidth(I18n.format("mobends.menu.blockbench"), textWidth)) {
            this.drawString(this.fontRendererObj, (String)line, textX, textY, 0xFFFFFF);
            textY += 10;
        }
        String command = this.command();
        this.commandX = textX;
        this.commandY = textY + 4;
        this.commandWidth = Math.min(textWidth, this.fontRendererObj.getStringWidth(command) + 10);
        this.commandHeight = 16;
        boolean hover = this.overCommand(mouseX, mouseY);
        drawRect(this.commandX, this.commandY, this.commandX + this.commandWidth, this.commandY + this.commandHeight,
            hover ? 0xFFA0A0A0 : 0xFF606060);
        drawRect(this.commandX + 1, this.commandY + 1, this.commandX + this.commandWidth - 1,
            this.commandY + this.commandHeight - 1, 0xFF101010);
        this.drawString(this.fontRendererObj, this.fontRendererObj.trimStringToWidth(command, this.commandWidth - 10),
            this.commandX + 5, this.commandY + 4, hover ? 0xFFFFA0 : 0xFFFF55);

        int hintX = 110;
        int hintY = this.height - 26;
        for (Object line : this.fontRendererObj.listFormattedStringToWidth(I18n.format("mobends.menu.restart"), this.width - hintX - 10)) {
            this.drawString(this.fontRendererObj, (String)line, hintX, hintY, 0xA0A0A0);
            hintY += 10;
        }
        int noteY = this.commandY + this.commandHeight + 8;
        Owner note = this.override(entity, shown);
        if (note != null) {
            for (Object line : this.fontRendererObj.listFormattedStringToWidth(note.text, textWidth)) {
                this.drawString(this.fontRendererObj, (String)line, textX, noteY, note.combined ? COMBINED_COLOR : OVERRIDE_COLOR);
                noteY += 10;
            }
            noteY += 4;
        }
        if (player) this.drawList(mouseX, mouseY, textX, noteY, textWidth, shown);
    }

    private static final int OVERRIDE_COLOR = 0xFFC060, COMBINED_COLOR = 0x90E090;

    /** What the mob page says about who draws its mob, when Mo' Bends doesn't alone. */
    private static final class Owner {
        final String text;
        /** A face-only pack sharing the player with Mo' Bends, rather than drawing it in its place. */
        final boolean combined;

        Owner(String text, boolean combined) {
            this.text = text;
            this.combined = combined;
        }
    }

    /**
     * Why Mo' Bends isn't what draws this page's mob: its switch is off, another mod's renderer took its place, or a
     * resource pack's model draws it; or that a face-only pack draws the player's face. Null when Mo' Bends draws it.
     */
    private Owner override(AnimatedEntity entity, EntityLivingBase shown) {
        if (!entity.animate) return new Owner(I18n.format("mobends.menu.override.off"), false);
        boolean player = this.playerPage();
        Render render = RenderManager.instance.getEntityRenderObject(shown);
        Class<?> own = player ? RenderBendsPlayer.class
            : shown instanceof EntitySpider ? RenderBendsSpider.class : RenderBendsZombie.class;
        if (render != null && !own.isInstance(render))
            return new Owner(I18n.format("mobends.menu.override.renderer", owner(render)), false);
        if (player && shown instanceof AbstractClientPlayer) {
            PackFaces.Cover cover = PackFaces.cover(shown);
            String pack = packName(PackFaces.source(shown));
            if (cover == PackFaces.Cover.FACE && PackFaces.combine)
                return new Owner(I18n.format("mobends.menu.override.face", pack), true);
            if (cover == null ? ((RenderBendsPlayer)render).packDraws((AbstractClientPlayer)shown)
                : cover != PackFaces.Cover.NONE)
                return new Owner(I18n.format("mobends.menu.override.playerPack", pack), false);
            return null;
        }
        String source = render instanceof RenderBendsZombie && shown instanceof EntityZombie
            ? ((RenderBendsZombie)render).packModelSource((EntityZombie)shown)
            : render instanceof RenderBendsSpider ? CemCompat.source(shown) : null;
        return source == null ? null : new Owner(I18n.format("mobends.menu.override.pack", packName(source)), false);
    }

    /** A resource pack's name as the pack list shows it, without the archive's extension. */
    private static String packName(String pack) {
        if (pack == null) return I18n.format("mobends.menu.override.somePack");
        return pack.toLowerCase(java.util.Locale.ROOT).endsWith(".zip") ? pack.substring(0, pack.length() - 4) : pack;
    }

    /** The mod a renderer comes from, by its package, else its class's name. */
    private static String owner(Render render) {
        String type = render.getClass().getName();
        String pkg = type.lastIndexOf('.') < 0 ? "" : type.substring(0, type.lastIndexOf('.'));
        for (ModContainer mod : Loader.instance().getActiveModList()) {
            if (mod.getOwnedPackages().contains(pkg)) return mod.getName();
        }
        return render.getClass().getSimpleName();
    }

    /**
     * The player's animations, a row each: click its name to preview it, its switch to turn it on or off. The page
     * says above it when something else draws the player.
     */
    private void drawList(int mouseX, int mouseY, int x, int y, int width, EntityLivingBase shown) {
        List<String> names = PlayerAnimationConfig.names();
        this.listX = x;
        this.listY = y;
        this.listWidth = width;
        this.listRows = Math.max(0, (this.height - 36 - y) / ROW_HEIGHT);
        this.scrollTo(this.scroll);
        if (this.listRows == 0) return;
        boolean scrolls = names.size() > this.listRows;
        int rowsWidth = width - (scrolls ? SCROLLBAR_WIDTH + 2 : 0);
        drawRect(x, y, x + width, y + this.listRows * ROW_HEIGHT, 0x60000000);
        for (int i = 0; i < this.listRows && this.scroll + i < names.size(); ++i) {
            int index = this.scroll + i;
            String name = names.get(index);
            boolean enabled = PlayerAnimationConfig.isEnabled(name);
            int rowY = y + i * ROW_HEIGHT;
            int toggleX = x + rowsWidth - TOGGLE_WIDTH - 2;
            boolean overRow = inside(mouseX, mouseY, x, rowY, rowsWidth, ROW_HEIGHT);
            boolean overToggle = overRow && mouseX >= toggleX;
            if (index == this.previewing) drawRect(x, rowY, x + rowsWidth, rowY + ROW_HEIGHT, 0x50FFFFFF);
            else if (overRow && !overToggle) drawRect(x, rowY, x + rowsWidth, rowY + ROW_HEIGHT, 0x28FFFFFF);
            int color = enabled ? (overToggle ? 0xFF3FA046 : 0xFF2E7D32) : (overToggle ? 0xFFB03434 : 0xFF8E2424);
            drawRect(toggleX, rowY + 2, toggleX + TOGGLE_WIDTH, rowY + ROW_HEIGHT - 2, color);
            this.drawCenteredString(this.fontRendererObj, I18n.format(enabled ? "mobends.gui.on" : "mobends.gui.off"),
                toggleX + TOGGLE_WIDTH / 2, rowY + 4, 0xFFFFFF);
            String label = this.fontRendererObj.trimStringToWidth(I18n.format("mobends.config.player." + name),
                toggleX - x - 8);
            this.drawString(this.fontRendererObj, label, x + 4, rowY + 4, enabled ? 0xFFFFFF : 0x909090);
        }
        if (scrolls) {
            int trackX = x + width - SCROLLBAR_WIDTH, rowsHeight = this.listRows * ROW_HEIGHT;
            int thumb = Math.max(10, rowsHeight * this.listRows / names.size());
            int thumbY = y + (rowsHeight - thumb) * this.scroll / (names.size() - this.listRows);
            drawRect(trackX, y, trackX + SCROLLBAR_WIDTH, y + rowsHeight, 0xFF202020);
            drawRect(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumb, 0xFF909090);
        }
    }

    /** The hovered animation's description. */
    private void drawListTooltip(int mouseX, int mouseY) {
        int row = this.rowAt(mouseX, mouseY);
        if (row < 0) return;
        String name = PlayerAnimationConfig.names().get(row);
        String key = "mobends.config.player." + name + ".tooltip";
        String text = I18n.format(key);
        if (text.equals(key)) text = PlayerAnimationConfig.comment(name);
        this.func_146283_a(this.fontRendererObj.listFormattedStringToWidth(text, 200), mouseX, mouseY);
    }

    /** The animation row under the pointer, its switch excluded, or -1. */
    private int rowAt(int x, int y) {
        List<String> names = PlayerAnimationConfig.names();
        int rowsWidth = this.listWidth - (names.size() > this.listRows ? SCROLLBAR_WIDTH + 2 : 0);
        if (!inside(x, y, this.listX, this.listY, rowsWidth - TOGGLE_WIDTH - 2, this.listRows * ROW_HEIGHT)) return -1;
        int row = this.scroll + (y - this.listY) / ROW_HEIGHT;
        return row < names.size() ? row : -1;
    }

    private void clickList(int x, int y) {
        List<String> names = PlayerAnimationConfig.names();
        boolean scrolls = names.size() > this.listRows;
        int rowsHeight = this.listRows * ROW_HEIGHT;
        if (scrolls && inside(x, y, this.listX + this.listWidth - SCROLLBAR_WIDTH, this.listY, SCROLLBAR_WIDTH, rowsHeight)) {
            this.draggingScrollbar = true;
            this.dragScrollbar(y);
            return;
        }
        int rowsWidth = this.listWidth - (scrolls ? SCROLLBAR_WIDTH + 2 : 0);
        if (!inside(x, y, this.listX, this.listY, rowsWidth, rowsHeight)) return;
        int row = this.scroll + (y - this.listY) / ROW_HEIGHT;
        if (row >= names.size()) return;
        this.mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0f));
        if (x >= this.listX + rowsWidth - TOGGLE_WIDTH - 2) {
            String name = names.get(row);
            PlayerAnimationConfig.setEnabled(name, !PlayerAnimationConfig.isEnabled(name));
            PlayerAnimationConfig.save(MoBends.config);
            MoBends.config.save();
        } else {
            this.preview(row);
        }
    }

    /** Plays the animation of the list's {@code row} on the stand-in player. */
    private void preview(int row) {
        this.previewing = row;
        String[] preview = SWITCH_PREVIEWS.get(PlayerAnimationConfig.names().get(row));
        EntityLivingBase player = MenuPreview.showcasePlayer();
        if (preview == null || AnimationPreview.loopOn(player, new ResourceLocation("mobends", preview[0]), preview[1]) == null) {
            AnimationPreview.stopLoop(player);
        }
    }

    private void dragScrollbar(int y) {
        int size = PlayerAnimationConfig.names().size();
        int rowsHeight = this.listRows * ROW_HEIGHT;
        if (rowsHeight <= 0 || size <= this.listRows) return;
        this.scrollTo(Math.round((float)(y - this.listY) / rowsHeight * size - this.listRows / 2.0f));
    }

    private void scrollTo(int row) {
        this.scroll = Math.max(0, Math.min(row, PlayerAnimationConfig.names().size() - this.listRows));
    }

    private static boolean inside(int x, int y, int left, int top, int width, int height) {
        return x >= left && y >= top && x < left + width && y < top + height;
    }

    /** Eases in over 0..1, and holds at 1 after. */
    private static float ease(double progress) {
        double t = Math.max(0.0, Math.min(1.0, progress));
        return (float)(1.0 - Math.pow(1.0 - t, 3.0));
    }

    private static void renderLivingEntity(int x, int y, int scale, float yaw, EntityLivingBase entity) {
        // Screens drawn before this one may leave depth testing off, as the main menu does: every box would then show
        // the face drawn last, its back, over its front.
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glPushMatrix();
        GL11.glTranslatef((float)x, (float)y, 50.0f);
        GL11.glScalef((float)(-scale), (float)scale, (float)scale);
        GL11.glRotatef(180.0f, 0.0f, 0.0f, 1.0f);
        GL11.glRotatef(yaw, 0.0f, 1.0f, 0.0f);
        float renderYaw = entity.renderYawOffset, rotationYaw = entity.rotationYaw, pitch = entity.rotationPitch;
        float previousHeadYaw = entity.prevRotationYawHead, headYaw = entity.rotationYawHead;
        GL11.glRotatef(135.0f, 0.0f, 1.0f, 0.0f);
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        RenderHelper.enableStandardItemLighting();
        GL11.glRotatef(-135.0f, 0.0f, 1.0f, 0.0f);
        entity.renderYawOffset = 0.0f;
        entity.rotationYaw = 0.0f;
        entity.rotationPitch = 0.0f;
        entity.rotationYawHead = 0.0f;
        entity.prevRotationYawHead = 0.0f;
        GL11.glTranslatef(0.0f, entity.yOffset, 0.0f);
        RenderManager.instance.playerViewY = 180.0f;
        RenderManager.instance.renderEntityWithPosYaw(entity, 0.0, 0.0, 0.0, 0.0f, 1.0f);
        entity.renderYawOffset = renderYaw;
        entity.rotationYaw = rotationYaw;
        entity.rotationPitch = pitch;
        entity.prevRotationYawHead = previousHeadYaw;
        entity.rotationYawHead = headYaw;
        GL11.glPopMatrix();
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        if (!depthTest) GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}
