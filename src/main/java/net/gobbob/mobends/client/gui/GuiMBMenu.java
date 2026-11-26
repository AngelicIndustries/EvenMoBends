package net.gobbob.mobends.client.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.client.animation.AnimationPreview;
import net.gobbob.mobends.client.renderer.entity.RenderBendsPlayer;
import net.gobbob.mobends.client.renderer.entity.RenderBendsZombie;
import net.gobbob.mobends.compat.CemCompat;
import net.gobbob.mobends.compat.CompatibilityPolicy;
import net.gobbob.mobends.compat.salamander.PackFaces;
import net.gobbob.mobends.config.MobSwitches;
import net.gobbob.mobends.config.PlayerAnimationConfig;
import net.gobbob.mobends.util.BendsLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/**
 * The Mo' Bends menu, opened from the config screen. On the left, a search field over every mob Mo' Bends animates,
 * vanilla's and other mods', listed by mod; mobs of those kinds that their mod draws its own way are listed greyed
 * out, with why. On the right, the chosen mob: its preview, its own switch, the command that writes its Blockbench
 * project, and for the player its animations, each to preview and switch on or off. Without a mob chosen, a player
 * shows off the animations. Animations themselves are Blockbench files, edited outside the game; the expression
 * editor is Salamander's.
 */
public class GuiMBMenu extends GuiScreen {
    public static final ResourceLocation TITLE = new ResourceLocation("mobends", "title.png");
    public static final ResourceLocation LOGO = new ResourceLocation("mobends", "textures/gui/logo.png");
    public static final ResourceLocation PREVIEW_BACKGROUND = new ResourceLocation("mobends", "textures/gui/displayBG.png");
    private static final int DONE = 0, ANIMATE = 2;
    /** No selection change waiting; see {@link #pendingPage}. */
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
    private static final int ROW_HEIGHT = 16, TOGGLE_WIDTH = 30, SCROLLBAR_WIDTH = 6, MOB_ROW = 13, SEARCH_HEIGHT = 14;
    /** The detail's top: below its header line. */
    private static final int DETAIL_TOP = 26, CLOSE_SIZE = 11;
    private static final int HEADER_COLOR = 0xFFD080, KEPT_COLOR = 0x707070, PLACEHOLDER_COLOR = 0x909090;

    private final GuiScreen parent;
    private final long openedNanos = System.nanoTime();
    private final Random random = new Random();
    /** The mobs listed, and the list's rows matching the search: a mod's name (String) or a mob (its Integer index). */
    private final List<MobRoster.Entry> roster = MobRoster.build();
    private final List<Object> rows = new ArrayList<Object>();
    private GuiTextField search;
    /** The mob list as last drawn, and its first row shown. */
    private int mobsX, mobsY, mobsWidth, mobsRows;
    private int mobsScroll;
    private boolean draggingMobsScrollbar;
    /** The chosen mob's index in {@link #roster}, or -1 for none: the showcase. */
    private int selected = -1;
    /**
     * The selection a click asked for, made before the next frame. Making it at once would rebuild the buttons while
     * the click still goes through them, and a new button under the pointer would take it too.
     */
    private int pendingPage = NO_PAGE;
    /** When the selection last changed, for the title's and previews' motion. */
    private long selectedNanos = System.nanoTime();
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
    /** The pointer as last drawn, for the lists' hovers and the mouse wheel. */
    private int lastMouseX, lastMouseY;
    /** The detail's close box as last drawn. */
    private int closeX, closeY;

    public GuiMBMenu(GuiScreen parent) {
        this.parent = parent;
        ++MoBends.refreshModel;
        this.filter("");
    }

    /** The rows of the mobs whose name or mod contains {@code query}, case aside, each under its mod's name. */
    private void filter(String query) {
        String wanted = query.trim().toLowerCase(java.util.Locale.ROOT);
        this.rows.clear();
        String mod = null;
        for (int i = 0; i < this.roster.size(); ++i) {
            MobRoster.Entry entry = this.roster.get(i);
            if (!wanted.isEmpty() && !entry.name.toLowerCase(java.util.Locale.ROOT).contains(wanted)
                && !entry.mod.toLowerCase(java.util.Locale.ROOT).contains(wanted)) continue;
            if (!entry.mod.equals(mod)) this.rows.add(mod = entry.mod);
            this.rows.add(i);
        }
        this.mobsScroll = 0;
    }

    private MobRoster.Entry entry() {
        return this.selected >= 0 ? this.roster.get(this.selected) : null;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        String query = this.search == null ? "" : this.search.getText();
        this.search = new GuiTextField(this.fontRendererObj, 11, 11, this.mobsListWidth() - 2, SEARCH_HEIGHT - 2);
        this.search.setMaxStringLength(40);
        this.search.setText(query);
        // Typing always searches: nothing else on the screen takes text.
        this.search.setFocused(true);
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(DONE, 10, this.height - 30, this.mobsListWidth(), 20, I18n.format("gui.done")));
        this.animate = null;
        // Only mobs Mo' Bends draws have a switch; the others say who draws them.
        if (this.selected >= 0 && this.entry().kind == MobRoster.Kind.ANIMATED) {
            int box = this.previewSize();
            boolean beside = this.switchBesidePreview();
            int besideX = this.right() + box + 12;
            int width = beside ? Math.min(100, this.width - 10 - besideX) : box;
            this.animate = new GuiToggleButton(ANIMATE, beside ? besideX : this.right(),
                beside ? DETAIL_TOP : DETAIL_TOP + box + 6, this.entry().animated())
                .setTitle(I18n.format("mobends.menu.animate"), Math.max(0, width - 40));
            this.buttonList.add(this.animate);
        }
    }

    /** The mob list's column: wide enough for mobs' names, a third of the screen at most. */
    private int mobsListWidth() {
        return Math.max(110, Math.min(170, (this.width - 30) / 3));
    }

    /** Where the mob list starts: below the search field. */
    private int listTop() {
        return 10 + SEARCH_HEIGHT + 4;
    }

    /** The right side, beside the mob list: the chosen mob, or the showcase. */
    private int right() {
        return 10 + this.mobsListWidth() + 10;
    }

    private int rightWidth() {
        return this.width - 10 - this.right();
    }

    /** The title's height on the showcase: as on the main menus, narrower where the right side is. */
    private int titleHeight() {
        int height = this.height < 260 ? 16 : 20;
        return Math.max(8, Math.min(height, Math.round((this.rightWidth() - 4) / (TITLE_ASPECT + 1.0f))));
    }

    /** How wide the text beside the preview must be for the longest Blockbench command to show whole. */
    private int commandRoom() {
        int command = Math.max(this.fontRendererObj.getStringWidth("/evenmobends blockbench zombie"),
            this.fontRendererObj.getStringWidth("/evenmobends blockbench spider"));
        return Math.max(140, command + 10);
    }

    /**
     * Too narrow for the text beside the preview: the preview and its switch side by side on top, the rest under them
     * across the right side. The same for every mob at a given size, so choosing another doesn't move things about.
     */
    private boolean stacked() {
        return this.rightWidth() - 12 - this.commandRoom() < 64;
    }

    /** The chosen mob's preview box: as large as fits, up to 128, leaving its text room beside it. */
    private int previewSize() {
        if (this.stacked()) return Math.max(40, Math.min(72, Math.min(this.rightWidth() - 112, this.height - 150)));
        return Math.max(48, Math.min(128, Math.min(this.height - 104, this.rightWidth() - 12 - this.commandRoom())));
    }

    /** Where the preview is small, its switch sits beside it rather than under it. */
    private boolean switchBesidePreview() {
        return this.stacked() || this.previewSize() < 80;
    }

    /** The chosen mob's text: beside its preview, or under it when stacked. */
    private int textX() {
        return this.stacked() ? this.right() : this.right() + this.previewSize() + 12;
    }

    private boolean playerPage() {
        return this.selected >= 0 && this.entry().player();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == DONE) {
            this.mc.displayGuiScreen(this.parent);
        } else if (button.id == ANIMATE) {
            // Takes effect at once: Mo' Bends' renderers ask the switch as they draw.
            MobRoster.Entry entry = this.entry();
            boolean on = !entry.animated();
            MobSwitches.set(MoBends.config, entry.key, on);
            this.animate.toggleState = on;
            this.animate.displayString = I18n.format(on ? "mobends.gui.on" : "mobends.gui.off");
        }
    }

    private void switchPage() {
        if (this.pendingPage == NO_PAGE) return;
        int page = this.pendingPage;
        this.pendingPage = NO_PAGE;
        // The showcase starts over once nothing is chosen; the player plays the animation picked in its list.
        AnimationPreview.stopLoop(MenuPreview.showcasePlayer());
        this.showcase = -1;
        this.selected = page;
        this.selectedNanos = System.nanoTime();
        this.scroll = 0;
        this.draggingScrollbar = false;
        this.draggingMobsScrollbar = false;
        if (this.playerPage()) this.preview(0);
        this.initGui();
    }

    @Override
    public void updateScreen() {
        this.switchPage();
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            // A search to clear first; then back to the config screen.
            if (this.search.getText().isEmpty()) {
                this.mc.displayGuiScreen(this.parent);
            } else {
                this.search.setText("");
                this.filter("");
            }
        } else if (this.search.textboxKeyTyped(character, key)) {
            this.filter(this.search.getText());
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
        if (inside(x, y, 10, 10, this.mobsListWidth(), SEARCH_HEIGHT)) {
            this.search.mouseClicked(x, y, button);
            this.search.setFocused(true);
        }
        if (button != 0 || this.pendingPage != NO_PAGE) return;
        if (inside(x, y, this.mobsX, this.mobsY, this.mobsWidth, this.mobsRows * MOB_ROW)) {
            this.clickMobs(x, y);
            return;
        }
        if (this.selected < 0) return;
        if (inside(x, y, this.closeX, this.closeY, CLOSE_SIZE, CLOSE_SIZE)) {
            this.mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0f));
            this.pendingPage = -1;
        } else if (this.overCommand(x, y)) {
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
        if (this.draggingMobsScrollbar) this.dragMobsScrollbar(y);
    }

    @Override
    protected void mouseMovedOrUp(int x, int y, int button) {
        super.mouseMovedOrUp(x, y, button);
        if (button == 0) this.draggingScrollbar = this.draggingMobsScrollbar = false;
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        // Whichever list the pointer is over.
        int x = this.lastMouseX, y = this.lastMouseY;
        if (inside(x, y, this.mobsX, this.mobsY, this.mobsWidth, this.mobsRows * MOB_ROW))
            this.scrollMobs(this.mobsScroll - Integer.signum(wheel) * 3);
        else if (this.playerPage() && inside(x, y, this.listX, this.listY, this.listWidth, this.listRows * ROW_HEIGHT))
            this.scrollTo(this.scroll - Integer.signum(wheel));
    }

    private boolean overCommand(int x, int y) {
        return x >= this.commandX && y >= this.commandY && x < this.commandX + this.commandWidth
            && y < this.commandY + this.commandHeight;
    }

    /** The command that writes the Blockbench project of this mob's rig: the player's, the zombie's or the spider's. */
    private String command() {
        String family = this.entry().family;
        return "/evenmobends blockbench" + (family.equals("player") ? "" : " " + (family.equals("cave_spider") ? "spider" : family));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        AnimationPreview.stopLoop(MenuPreview.showcasePlayer());
        MoBends.saveConfig();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.switchPage();
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        // In a world the menu lies over it; opened from the mods list nothing else clears the screen.
        if (this.mc.theWorld == null) this.drawDefaultBackground();
        else this.drawGradientRect(0, 0, this.width, this.height, 0xA0101010, 0xC0101010);
        double seconds = (System.nanoTime() - this.openedNanos) / 1.0e9;
        double sinceChosen = (System.nanoTime() - this.selectedNanos) / 1.0e9;
        this.drawSearch();
        this.drawMobs(seconds);
        if (this.selected < 0) this.drawShowcasePage(sinceChosen);
        else this.drawMobPage(mouseX, mouseY, sinceChosen);
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (this.selected >= 0 && this.overCommand(mouseX, mouseY)) {
            boolean copied = Minecraft.getSystemTime() < this.copiedUntilMillis;
            List<String> tooltip = Collections.singletonList(I18n.format(copied ? "mobends.menu.copied" : "mobends.menu.copy"));
            this.func_146283_a(tooltip, mouseX, mouseY);
        } else if (this.playerPage() && this.rowAt(mouseX, mouseY) >= 0) {
            this.drawListTooltip(mouseX, mouseY);
        } else {
            this.drawMobsTooltip(mouseX, mouseY);
        }
    }

    /** The search field over the list, saying what it's for while empty. */
    private void drawSearch() {
        this.search.drawTextBox();
        if (this.search.getText().isEmpty()) {
            this.drawString(this.fontRendererObj, this.fontRendererObj.trimStringToWidth(
                I18n.format("mobends.menu.search"), this.mobsListWidth() - 12), 15, 13, PLACEHOLDER_COLOR);
        }
    }

    /** Nothing chosen: the title and logo over the showcase player, on the right. */
    private void drawShowcasePage(double seconds) {
        int left = this.right(), width = this.rightWidth();
        int titleHeight = this.titleHeight();
        int titleWidth = Math.round(titleHeight * TITLE_ASPECT);
        int logoSize = titleHeight;
        int groupX = left + (width - logoSize - 4 - titleWidth) / 2;
        int titleY = 10 - Math.round((1.0f - ease(seconds / 0.4)) * (titleHeight + 10));
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        this.mc.getTextureManager().bindTexture(LOGO);
        Gui.func_146110_a(groupX, titleY, 0, 0, logoSize, logoSize, logoSize, logoSize);
        this.mc.getTextureManager().bindTexture(TITLE);
        Gui.func_146110_a(groupX + logoSize + 4, titleY, 0, 0, titleWidth, titleHeight, titleWidth, titleHeight);
        this.drawShowcase(seconds, left, 10 + titleHeight + 8);
        String owner = CompatibilityPolicy.playerOwner();
        if (owner != null) {
            int noteY = this.height - 26;
            for (Object line : this.fontRendererObj.listFormattedStringToWidth(
                I18n.format("mobends.menu.override.player", owner), width)) {
                this.drawString(this.fontRendererObj, (String)line, left, noteY, OVERRIDE_COLOR);
                noteY += 10;
            }
        }
    }

    /**
     * The mobs matching the search, a row each under their mod's name, sliding in one after another; the chosen one
     * stands out, those switched off say so, and those their mod draws are greyed out. Scrolls when they don't all fit.
     */
    private void drawMobs(double seconds) {
        int x = 10, y = this.listTop();
        int width = this.mobsListWidth();
        this.mobsX = x;
        this.mobsY = y;
        this.mobsWidth = width;
        this.mobsRows = Math.max(1, (this.height - 36 - y) / MOB_ROW);
        this.scrollMobs(this.mobsScroll);
        boolean scrolls = this.rows.size() > this.mobsRows;
        int rowsWidth = width - (scrolls ? SCROLLBAR_WIDTH + 2 : 0);
        int mouseX = this.lastMouseX, mouseY = this.lastMouseY;
        if (this.rows.isEmpty()) {
            this.drawString(this.fontRendererObj, this.fontRendererObj.trimStringToWidth(I18n.format("mobends.menu.noMatch"),
                width - 4), x + 2, y + 3, PLACEHOLDER_COLOR);
        }
        for (int i = 0; i < this.mobsRows && this.mobsScroll + i < this.rows.size(); ++i) {
            Object row = this.rows.get(this.mobsScroll + i);
            int rowY = y + i * MOB_ROW;
            int rowX = x - Math.round((1.0f - ease((seconds - 0.15 - Math.min(i, 12) * 0.03) / 0.35)) * (rowsWidth + 10));
            if (row instanceof String) {
                this.drawString(this.fontRendererObj, this.fontRendererObj.trimStringToWidth((String)row, rowsWidth - 2),
                    rowX + 1, rowY + 3, HEADER_COLOR);
                continue;
            }
            MobRoster.Entry entry = this.roster.get((Integer)row);
            boolean chosen = (Integer)row == this.selected;
            boolean over = rowX == x && inside(mouseX, mouseY, x, rowY, rowsWidth, MOB_ROW - 1);
            drawRect(rowX, rowY, rowX + rowsWidth, rowY + MOB_ROW - 1, chosen ? 0x90FFFFFF : over ? 0x50FFFFFF : 0x60000000);
            if (chosen) drawRect(rowX, rowY, rowX + 2, rowY + MOB_ROW - 1, 0xFFFFFFA0);
            boolean ours = entry.kind == MobRoster.Kind.ANIMATED;
            String tag = ours && !entry.animated() ? I18n.format("mobends.gui.off") : null;
            int tagWidth = tag == null ? 0 : this.fontRendererObj.getStringWidth(tag) + 6;
            int color = chosen ? 0xFFFFA0 : !ours ? KEPT_COLOR : tag != null ? 0xA0A0A0 : 0xFFFFFF;
            this.drawString(this.fontRendererObj,
                this.fontRendererObj.trimStringToWidth(entry.name, rowsWidth - 10 - tagWidth), rowX + 6, rowY + 2, color);
            if (tag != null) this.drawString(this.fontRendererObj, tag, rowX + rowsWidth - tagWidth + 2, rowY + 2, 0xD05050);
        }
        if (scrolls) {
            int trackX = x + width - SCROLLBAR_WIDTH, rowsHeight = this.mobsRows * MOB_ROW;
            int thumb = Math.max(10, rowsHeight * this.mobsRows / this.rows.size());
            int thumbY = y + (rowsHeight - thumb) * this.mobsScroll / (this.rows.size() - this.mobsRows);
            drawRect(trackX, y, trackX + SCROLLBAR_WIDTH, y + rowsHeight, 0xFF202020);
            drawRect(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumb, 0xFF909090);
        }
    }

    /** The mob row under the pointer, or -1; a mod's name row counts as none. */
    private int mobAt(int x, int y) {
        int rowsWidth = this.mobsWidth - (this.rows.size() > this.mobsRows ? SCROLLBAR_WIDTH + 2 : 0);
        if (!inside(x, y, this.mobsX, this.mobsY, rowsWidth, this.mobsRows * MOB_ROW)) return -1;
        int row = this.mobsScroll + (y - this.mobsY) / MOB_ROW;
        return row < this.rows.size() && this.rows.get(row) instanceof Integer ? (Integer)this.rows.get(row) : -1;
    }

    private void clickMobs(int x, int y) {
        boolean scrolls = this.rows.size() > this.mobsRows;
        if (scrolls && inside(x, y, this.mobsX + this.mobsWidth - SCROLLBAR_WIDTH, this.mobsY, SCROLLBAR_WIDTH,
            this.mobsRows * MOB_ROW)) {
            this.draggingMobsScrollbar = true;
            this.dragMobsScrollbar(y);
            return;
        }
        int mob = this.mobAt(x, y);
        if (mob < 0) return;
        this.mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0f));
        // The chosen mob again puts the showcase back.
        this.pendingPage = mob == this.selected ? -1 : mob;
    }

    private void dragMobsScrollbar(int y) {
        int rowsHeight = this.mobsRows * MOB_ROW;
        if (rowsHeight <= 0 || this.rows.size() <= this.mobsRows) return;
        this.scrollMobs(Math.round((float)(y - this.mobsY) / rowsHeight * this.rows.size() - this.mobsRows / 2.0f));
    }

    private void scrollMobs(int row) {
        this.mobsScroll = Math.max(0, Math.min(row, this.rows.size() - this.mobsRows));
    }

    /** Why a greyed-out mob is drawn by its mod, or that a mob is switched off. */
    private void drawMobsTooltip(int mouseX, int mouseY) {
        int mob = this.mobAt(mouseX, mouseY);
        if (mob < 0) return;
        Owner note = this.why(this.roster.get(mob));
        if (note != null) this.func_146283_a(this.fontRendererObj.listFormattedStringToWidth(note.text, 200), mouseX, mouseY);
    }

    /** A player spinning once every 10 seconds, playing another of its animations each time round. */
    private void drawShowcase(double seconds, int left, int top) {
        EntityLivingBase player = MenuPreview.showcasePlayer();
        if (seconds >= this.showcaseUntil || this.showcase < 0) {
            int next = this.random.nextInt(SHOWCASE.length);
            if (next == this.showcase) next = (next + 1) % SHOWCASE.length;
            this.showcase = next;
            this.showcaseUntil = seconds + SHOWCASE_SECONDS;
            AnimationPreview.loopOn(player, new ResourceLocation("mobends", SHOWCASE[next][0]), SHOWCASE[next][1]);
            MenuPreview.motion(SHOWCASE[next][1]);
        }
        int scale = Math.max(20, Math.min(70, Math.min((this.height - top - 20) / 3, (this.width - 10 - left) / 2)));
        int x = (left + this.width - 10) / 2, y = this.height - Math.max(30, (this.height - top - scale * 2) / 2);
        float yaw = (float)(seconds * 36.0 % 360.0);
        MenuPreview.draw(player, () -> renderLivingEntity(x, y, scale, yaw, player));
    }

    /**
     * The chosen mob, on the right: its name over its preview, its switch, the command that writes its rig's
     * Blockbench project, who draws it when Mo' Bends doesn't, and for the player its animations. The box at the
     * header's end puts the showcase back.
     */
    private void drawMobPage(int mouseX, int mouseY, double seconds) {
        MobRoster.Entry entry = this.entry();
        boolean player = this.playerPage();
        int left = this.right(), width = this.rightWidth();
        boolean vanilla = entry.vanilla != null || entry.type != null && entry.type.getName().startsWith("net.minecraft.");
        String title = vanilla ? entry.name : entry.name + " - " + entry.mod;
        this.drawCenteredString(this.fontRendererObj, this.fontRendererObj.trimStringToWidth(title, width - 2 * CLOSE_SIZE - 8),
            left + width / 2, 10, 0xFFFFFF);
        this.closeX = left + width - CLOSE_SIZE;
        this.closeY = 8;
        boolean overClose = inside(mouseX, mouseY, this.closeX, this.closeY, CLOSE_SIZE, CLOSE_SIZE);
        drawRect(this.closeX, this.closeY, this.closeX + CLOSE_SIZE, this.closeY + CLOSE_SIZE, overClose ? 0xFFA0A0A0 : 0xFF606060);
        drawRect(this.closeX + 1, this.closeY + 1, this.closeX + CLOSE_SIZE - 1, this.closeY + CLOSE_SIZE - 1, 0xFF101010);
        this.drawCenteredString(this.fontRendererObj, "x", this.closeX + CLOSE_SIZE / 2 + 1, this.closeY + 1,
            overClose ? 0xFFFFA0 : 0xE0E0E0);
        int box = this.previewSize();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        this.mc.getTextureManager().bindTexture(PREVIEW_BACKGROUND);
        Gui.func_146110_a(left, DETAIL_TOP, 0, 0, box, box, box, box);
        EntityLivingBase shown = entry.preview();
        if (shown != null) this.drawPreview(entry, shown, left, DETAIL_TOP, box, seconds);
        else this.drawCenteredString(this.fontRendererObj, I18n.format("mobends.menu.noPreview"), left + box / 2,
            DETAIL_TOP + box / 2 - 4, PLACEHOLDER_COLOR);

        int textX = this.textX(), textWidth = this.width - 10 - textX;
        // Under the preview when stacked; else beside it, under the switch when that sits there too.
        int textY = this.stacked() ? DETAIL_TOP + box + 6
            : this.animate != null && this.switchBesidePreview() ? DETAIL_TOP + 26 : DETAIL_TOP + 2;
        int noteY = textY;
        // A mob its own mod draws takes no Mo' Bends animations: no Blockbench project to offer.
        if (entry.keeper == null) {
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
            noteY = this.commandY + this.commandHeight + 8;
        } else {
            this.commandWidth = this.commandHeight = 0;
        }
        Owner note = this.why(entry);
        if (note == null && shown != null) note = this.override(entry, shown);
        if (note != null) {
            noteY = this.drawLines(note.text, textX, noteY, textWidth, this.height - 10,
                note.combined ? COMBINED_COLOR : OVERRIDE_COLOR) + 4;
        }
        if (player) this.drawList(mouseX, mouseY, textX, noteY, textWidth, shown);
    }

    /**
     * The mob turning in its box, sized from how much its renderer scales it: a giant shrinks to fit, a small spider
     * grows. A mob that fails to draw here shows the placeholder from then on.
     */
    private void drawPreview(MobRoster.Entry entry, EntityLivingBase shown, int boxX, int boxY, int box, double seconds) {
        boolean player = entry.player();
        // Mobs walk on the spot, as they always have here.
        if (!player) shown.moveForward = 1.0f;
        // Spiders are wide and low: smaller, and higher in the box, for their legs to stay inside it as they turn.
        boolean spider = entry.spider();
        float fit = player ? 1.0f : Math.max(0.35f, Math.min(1.6f, 1.0f / entry.renderScale(shown)));
        int scale = Math.max(8, Math.round(box * (spider ? 38 : 50) / 128.0f * fit));
        int baseY = boxY + box - box * (spider ? 44 : 22) / 128;
        float yaw = (float)(seconds * (player ? 36.0 : 72.0) % 360.0);
        try {
            MenuPreview.draw(shown, () -> renderLivingEntity(boxX + box / 2, baseY, scale, yaw, shown));
        } catch (RuntimeException failed) {
            entry.previewFailed();
            BendsLogger.log("The menu couldn't draw " + entry.key + ": " + failed, BendsLogger.ERROR);
        }
    }

    /** Wraps {@code text} to {@code width} from {@code y}, stopping above {@code bottom}; returns the next line's y. */
    private int drawLines(String text, int x, int y, int width, int bottom, int color) {
        List<?> lines = this.fontRendererObj.listFormattedStringToWidth(text, width);
        for (int i = 0; i < lines.size() && y + 9 <= bottom; ++i) {
            String line = (String)lines.get(i);
            // The last line that fits says there is more.
            if (y + 19 > bottom && i < lines.size() - 1) line = this.fontRendererObj.trimStringToWidth(line, width - 12) + "...";
            this.drawString(this.fontRendererObj, line, x, y, color);
            y += 10;
        }
        return y;
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
     * Why a listed mob isn't Mo' Bends', from the list alone: its mod draws it its own way, the General setting leaves
     * other mods' mobs to them, or its switch is off. Null when Mo' Bends draws it.
     */
    private Owner why(MobRoster.Entry entry) {
        if (entry.kind == MobRoster.Kind.SETTING_OFF) return new Owner(I18n.format("mobends.menu.override.setting"), false);
        if (entry.kind == MobRoster.Kind.OWN_DRAWING)
            return new Owner(I18n.format("mobends.menu.override.renderer", entry.keeper), false);
        if (!entry.animated()) return new Owner(I18n.format("mobends.menu.override.off"), false);
        return null;
    }

    /**
     * Why Mo' Bends isn't what draws a mob it should: another mod's renderer took its place since, or a resource
     * pack's model draws it; or that a face-only pack draws the player's face. Null when Mo' Bends draws it.
     */
    private Owner override(MobRoster.Entry entry, EntityLivingBase shown) {
        boolean player = entry.player();
        Render render = RenderManager.instance.getEntityRenderObject(shown);
        if (render != null && !render.getClass().getName().startsWith("net.gobbob.mobends."))
            return new Owner(I18n.format("mobends.menu.override.renderer", MobRoster.owner(render)), false);
        if (player && shown instanceof AbstractClientPlayer && render instanceof RenderBendsPlayer) {
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
            ? ((RenderBendsZombie)render).packModelSource((EntityZombie)shown) : CemCompat.source(shown);
        return source == null ? null : new Owner(I18n.format("mobends.menu.override.pack", packName(source)), false);
    }

    /** A resource pack's name as the pack list shows it, without the archive's extension. */
    private static String packName(String pack) {
        if (pack == null) return I18n.format("mobends.menu.override.somePack");
        return pack.toLowerCase(java.util.Locale.ROOT).endsWith(".zip") ? pack.substring(0, pack.length() - 4) : pack;
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
        this.listRows = Math.max(0, (this.height - 10 - y) / ROW_HEIGHT);
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
        MenuPreview.motion(preview == null ? null : preview[1]);
        if (preview == null || AnimationPreview.loopOn(player, new ResourceLocation("mobends", preview[0]), preview[1]) == null) {
            AnimationPreview.stopLoop(player);
            MenuPreview.motion(null);
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
        try {
            RenderManager.instance.renderEntityWithPosYaw(entity, 0.0, 0.0, 0.0, 0.0f, 1.0f);
        } finally {
            // Another mod's mob may fail to draw here: leave the screen as it was for the menu to go on.
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
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}
