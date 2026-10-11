package net.gobbob.mobends.client.gui;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import javax.imageio.ImageIO;
import net.gobbob.mobends.client.render.ExpressionSkinExporter;
import net.gobbob.mobends.client.render.PlayerSkinImages;
import net.gobbob.mobends.compat.WawelAuthCompat;
import net.gobbob.mobends.compat.WawelAuthSkinUpload;
import net.gobbob.mobends.compat.WawelAuthSkinUpload.Account;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

/** Prepares a skin for Just Expressions or Fresh Moves through Salamander. */
public class GuiExpressionSkinEditor extends GuiScreen {
    private static final int PICKER_WIDTH = 104;
    private static final int PICKER_HEIGHT = 36;
    private static final int HUE_WIDTH = 12;
    private static final long BLINK_DURATION_MS = 520L;

    private enum Tool {
        EYE_REGION,
        PUPIL_COLORS,
        EYE_WHITES,
        PAINT_EYELIDS,
        EYEBROWS,
        BROW_COLORS,
        BROW_BACKGROUND,
        OVERLAY_BROWS,
        OVERLAY_COLORS,
        EYELASHES
    }

    private final GuiScreen parent;
    private final ExpressionSkinExporter.EditorData data = new ExpressionSkinExporter.EditorData();
    private final byte[][] eyeGroups = data.eyes;
    private final boolean[][] pupils = data.pupils;
    private final int[][] closedColors = data.lids;
    private final int[][] pupilBackgroundColors = data.whites;
    private final byte[][] eyebrowGroups = data.brows;
    private final int[][] eyebrowColors = data.browColors;
    private final java.util.Deque<ExpressionSkinExporter.EditorData> undo = new java.util.ArrayDeque<>();
    private final java.util.Deque<ExpressionSkinExporter.EditorData> redo = new java.util.ArrayDeque<>();
    private int previewMode;
    private int browPreset;
    private int browRow = 4;
    private BufferedImage skin;
    private GuiTextField hexField;
    private boolean statusSuccess;
    private String validationError;
    private String status = I18n.format("mobends.expression.instructions");
    private Tool tool = Tool.EYE_REGION;
    private boolean eyedropperArmed;
    private boolean draggingCanvas;
    private boolean dragValue;
    private int pickerDrag;
    private int activeGroup;
    private float hue;
    private float saturation;
    private float brightness = 1.0f;
    private long blinkStartedAt = Long.MIN_VALUE;
    private boolean editorDataLoaded;
    private boolean uploading;
    private int preset;
    private int presetRow = 4;

    public GuiExpressionSkinEditor(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        if (!this.editorDataLoaded) {
            this.skin = PlayerSkinImages.copyPlayerSkin(this.mc.thePlayer);
            loadExistingData();
            this.editorDataLoaded = true;
        }
        int x = controlsX();
        int y = controlsY();
        this.buttonList.add(new GuiButton(12, x, y, 24, 20, "<"));
        this.buttonList.add(new GuiButton(13, x + 158, y, 24, 20, ">"));
        this.buttonList.add(new GuiButton(9, x, y + 24, 58, 20, I18n.format("mobends.expression.left")));
        this.buttonList.add(new GuiButton(10, x + 62, y + 24, 58, 20, I18n.format("mobends.expression.right")));
        this.buttonList.add(new GuiButton(18, x + 124, y + 24, 58, 20, I18n.format("mobends.expression.both")));
        this.buttonList.add(new GuiButton(19, x, this.height - 72, 182, 20, ""));
        this.buttonList.add(new GuiButton(15, x, y + 50, 182, 20, ""));
        this.buttonList.add(new GuiButton(16, x, y + 74, 88, 20, I18n.format("mobends.expression.up")));
        this.buttonList.add(new GuiButton(17, x + 94, y + 74, 88, 20, I18n.format("mobends.expression.down")));
        this.buttonList.add(new GuiButton(20, x, y + 98, 182, 20, I18n.format("mobends.expression.apply")));
        this.buttonList.add(new GuiButton(3, x, y + 107, 182, 20, I18n.format("mobends.expression.pickColor")));
        this.buttonList.add(new GuiButton(4, x, this.height - 50, 88, 20, I18n.format("mobends.expression.blink")));
        this.buttonList.add(
            new GuiButton(5, x + 94, this.height - 50, 88, 20, I18n.format("mobends.expression.clear")));
        int bottomY = this.height - 28;
        int bottomX = this.width / 2 - 116;
        GuiButton upload = new GuiWawelAuthIconButton(14, bottomX, bottomY);
        upload.visible = WawelAuthCompat.isLoaded();
        upload.enabled = this.skin != null && !this.uploading;
        this.buttonList.add(upload);
        this.buttonList.add(new GuiButton(7, bottomX + 24, bottomY, 88, 20, I18n.format("mobends.gui.back")));
        GuiButton export = new GuiButton(6, bottomX + 116, bottomY, 116, 20, I18n.format("mobends.expression.export"));
        export.enabled = this.skin != null;
        this.buttonList.add(export);
        this.hexField = new GuiTextField(this.fontRendererObj, x + 42, pickerY() + PICKER_HEIGHT + 4, 78, 18);
        this.hexField.setMaxStringLength(7);
        updateHex();
        updateToolControls();
        updateExportButton();
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 12 || button.id == 13) {
            Tool[] tools = Tool.values();
            int direction = button.id == 12 ? -1 : 1;
            setTool(tools[(this.tool.ordinal() + direction + tools.length) % tools.length]);
        } else if (button.id == 9 || button.id == 10 || button.id == 18) {
            this.activeGroup = button.id == 18 ? 0 : button.id == 9 ? 1 : 2;
            syncPresets();
            updateToolControls();
        } else if (button.id >= 15 && button.id <= 17) {
            remember();
            if (this.tool == Tool.EYE_REGION) {
                if (button.id == 15)
                    this.preset = (this.preset + 1) % ExpressionSkinExporter.EyeType.values().length;
                else
                    this.presetRow += button.id == 16 ? -1 : 1;
                applyPreset();
            } else {
                if (button.id == 15 && this.tool == Tool.EYEBROWS)
                    this.browPreset = (this.browPreset + 1) % 3;
                else if (button.id != 15)
                    this.browRow += button.id == 16 ? -1 : 1;
                applyBrowPreset();
            }
        } else if (button.id == 20) {
            remember();
            if (this.tool == Tool.EYE_REGION)
                applyPreset();
            else
                applyBrowPreset();
        } else if (button.id == 19) {
            this.previewMode = (this.previewMode + 1) % 7;
            updateToolControls();
        } else if (button.id == 3) {
            this.eyedropperArmed = !this.eyedropperArmed;
            updateToolControls();
        } else if (button.id == 4) {
            this.blinkStartedAt = System.currentTimeMillis();
        } else if (button.id == 5) {
            remember();
            clearSelection();
            this.statusSuccess = true;
            this.status =
                I18n.format(isBrowTool() ? "mobends.expression.browsRemoved" : "mobends.expression.featureCleared");
        } else if (button.id == 6) {
            exportSkin();
        } else if (button.id == 14) {
            confirmWawelAuthUpload();
        } else if (button.id == 7) {
            this.mc.displayGuiScreen(this.parent);
        }
        updateExportButton();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        this.hexField.mouseClicked(mouseX, mouseY, button);
        if (this.hexField.isFocused() && this.eyedropperArmed) {
            this.eyedropperArmed = false;
            updateToolControls();
        }
        if (isColorTool() && inside(mouseX, mouseY, pickerX(), pickerY(), PICKER_WIDTH, PICKER_HEIGHT)) {
            this.eyedropperArmed = false;
            this.pickerDrag = 1;
            updateSaturationBrightness(mouseX, mouseY);
            updateToolControls();
            return;
        }
        if (isColorTool()
            && inside(mouseX, mouseY, pickerX() + PICKER_WIDTH + 5, pickerY(), HUE_WIDTH, PICKER_HEIGHT)) {
            this.eyedropperArmed = false;
            this.pickerDrag = 2;
            updateHue(mouseY);
            updateToolControls();
            return;
        }

        int x = faceX(mouseX);
        int y = faceY(mouseY);
        if (x < 0 || y < 0) {
            return;
        }
        if (this.eyedropperArmed) {
            setCurrentColor(compositeFacePixel(this.skin, x, y));
            this.eyedropperArmed = false;
            updateToolControls();
        } else if (isColorTool()) {
            remember();
            this.dragValue = button != 1;
            this.draggingCanvas = true;
            paintPixel(x, y, this.dragValue);
        } else {
            this.activeGroup = x < 4 ? 1 : 2;
            syncPresets();
            updateToolControls();
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long elapsed) {
        if (this.pickerDrag == 1) {
            updateSaturationBrightness(mouseX, mouseY);
            return;
        }
        if (this.pickerDrag == 2) {
            updateHue(mouseY);
            return;
        }
        if (!this.draggingCanvas) {
            return;
        }
        int x = faceX(mouseX);
        int y = faceY(mouseY);
        if (x >= 0 && y >= 0) {
            if (isColorTool()) {
                paintPixel(x, y, this.dragValue);
            }
        }
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int button) {
        super.mouseMovedOrUp(mouseX, mouseY, button);
        if (button >= 0) {
            this.draggingCanvas = false;
            this.pickerDrag = 0;
        }
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (isCtrlKeyDown() && !this.hexField.isFocused() && (key == Keyboard.KEY_Z || key == Keyboard.KEY_Y)) {
            undo(key == Keyboard.KEY_Y || isShiftKeyDown());
            return;
        }

        if (key == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(this.parent);
            return;
        }
        if (this.hexField.isFocused()) {
            this.hexField.textboxKeyTyped(character, key);
            String value = this.hexField.getText().trim();
            if (value.matches("#[0-9a-fA-F]{6}")) {
                setCurrentColor(0xff000000 | Integer.parseInt(value.substring(1), 16), false);
            }
        }
    }

    @Override
    public void updateScreen() {
        this.hexField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (this.eyedropperArmed) {
            int hoverX = faceX(mouseX);
            int hoverY = faceY(mouseY);
            if (hoverX >= 0 && hoverY >= 0) {
                setCurrentColor(compositeFacePixel(this.skin, hoverX, hoverY));
            }
        }
        this.drawDefaultBackground();
        this.drawCenteredString(
            this.fontRendererObj, I18n.format("mobends.expression.title"), this.width / 2, 12, 0xffffff);
        this.drawCenteredString(this.fontRendererObj, stepName(), controlsX() + 91, controlsY() + 6, 0xffffff);
        drawAnimatedFace();
        if (isColorTool()) {
            drawColorPicker();
            this.drawString(this.fontRendererObj, I18n.format("mobends.expression.hex"), controlsX(),
                pickerY() + PICKER_HEIGHT + 9, 0xbfbfbf);
            this.hexField.drawTextBox();
        }
        String message = this.validationError == null ? this.status : this.validationError;
        java.util.List<String> lines = this.fontRendererObj.listFormattedStringToWidth(message, panelWidth() - 15);
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            this.drawString(this.fontRendererObj, lines.get(i), canvasX(), canvasY() + canvasSize() + 17 + i * 9,
                this.validationError != null ? 0xffaa55
                    : this.statusSuccess     ? 0x55ff55
                                             : 0xbfbfbf);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (inside(mouseX, mouseY, canvasX(), canvasY() + canvasSize() + 17, panelWidth() - 15, 27)) {
            this.func_146283_a(this.fontRendererObj.listFormattedStringToWidth(message, 260), mouseX, mouseY);
        }
        GuiButton upload = getButton(14);
        if (upload != null && upload.visible && mouseX >= upload.xPosition && mouseX < upload.xPosition + upload.width
            && mouseY >= upload.yPosition && mouseY < upload.yPosition + upload.height) {
            this.func_146283_a(Collections.singletonList(I18n.format("mobends.upload.tooltip")), mouseX, mouseY);
        }
    }

    private void drawAnimatedFace() {
        int canvasX = canvasX();
        int canvasY = canvasY();
        for (int x = 0; x < 8; ++x) {
            for (int y = 0; y < 8; ++y) {
                int color = ExpressionSkinExporter.base(this.skin, x, y);
                if (this.eyeGroups[x][y] != 0 && hasMovingPupil(this.eyeGroups[x][y])) {
                    color = 0xff000000 | this.pupilBackgroundColors[x][y] & 0xffffff;
                }
                drawPixel(canvasX, canvasY, x, y, 0, 0, color);
            }
        }

        long now = System.currentTimeMillis();
        // The Eyelids step keeps the eyes closed, so each painted lid shows at once.
        float blink = this.previewMode == 2 || this.tool == Tool.PAINT_EYELIDS ? 1 : blinkProgress(now);
        if (this.tool != Tool.PAINT_EYELIDS && this.previewMode == 0 && now % 4200 < BLINK_DURATION_MS)
            blink = blinkPhase(now % 4200);
        int pupilOffsetX = this.previewMode == 3 ? -6
            : this.previewMode == 4              ? 6
            : this.previewMode == 0              ? Math.round((float) Math.sin(now / 760.0) * 3)
                                                 : 0;
        int pupilOffsetY = this.previewMode == 5 ? -5
            : this.previewMode == 6              ? 5
            : this.previewMode == 0              ? Math.round((float) Math.sin(now / 1130.0) * 2)
                                                 : 0;
        for (int x = 0; x < 8; ++x) {
            for (int y = 0; y < 8; ++y) {
                if (!this.pupils[x][y]) {
                    continue;
                }
                drawClippedPupil(canvasX, canvasY, x, y, pupilOffsetX, pupilOffsetY, this.data.pupilColors[x][y],
                    this.eyeGroups[x][y]);
            }
        }
        for (int x = 0; x < 8; ++x) {
            for (int y = 0; y < 8; ++y) {
                if (this.eyeGroups[x][y] == 0) {
                    continue;
                }
                if (blink > 0.0f) {
                    int group = this.eyeGroups[x][y];
                    int top = groupTop(group);
                    int bottom = groupBottom(group);
                    int boundary = canvasY + top * pixelSize() + Math.round((bottom - top + 1) * pixelSize() * blink);
                    int pixelTop = canvasY + y * pixelSize();
                    int pixelBottom = Math.min(canvasY + (y + 1) * pixelSize(), boundary);
                    if (pixelBottom > pixelTop) {
                        Gui.drawRect(canvasX + x * pixelSize(), pixelTop, canvasX + (x + 1) * pixelSize(), pixelBottom,
                            this.closedColors[x][y]);
                    }
                }
            }
        }

        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++) {
                if (this.eyebrowGroups[x][y] != 0) {
                    drawPixel(canvasX, canvasY, x, y, 0, 0, this.data.browBackgrounds[x][y]);
                    if (this.tool != Tool.BROW_BACKGROUND)
                        drawPixel(canvasX, canvasY, x, y, 0,
                            this.previewMode == 0 ? Math.round((float) Math.sin(now / 900.0) * 2) : 0,
                            this.eyebrowColors[x][y]);
                }
            }
        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++) {
                int hat = this.skin == null ? 0 : this.skin.getRGB(40 + x, 8 + y);
                drawPixel(canvasX, canvasY, x, y, 0, 0, hat);
                if (this.data.overlayBrows[x][y] != 0)
                    drawPixel(canvasX, canvasY, x, y, 0, 0, this.data.overlayColors[x][y]);
                drawPixel(canvasX, canvasY, x, y, 0, 0, this.data.lashes[x][y]);
            }

        for (int x = 0; x < 8; ++x) {
            for (int y = 0; y < 8; ++y) {
                boolean outlined = isOutlined(x, y);
                if (!outlined) {
                    continue;
                }
                int left = canvasX + x * pixelSize();
                int top = canvasY + y * pixelSize();
                int group = x < 4 ? 1 : 2;
                int outline = this.tool == Tool.PUPIL_COLORS ? 0xff4499ff : componentColor(group);
                Gui.drawRect(left, top, left + pixelSize(), top + 1, outline);
                Gui.drawRect(left, top + pixelSize() - 1, left + pixelSize(), top + pixelSize(), outline);
                Gui.drawRect(left, top, left + 1, top + pixelSize(), outline);
                Gui.drawRect(left + pixelSize() - 1, top, left + pixelSize(), top + pixelSize(), outline);
            }
        }
        for (int i = 0; i <= 8; ++i) {
            Gui.drawRect(
                canvasX + i * pixelSize(), canvasY, canvasX + i * pixelSize() + 1, canvasY + canvasSize(), 0x44000000);
            Gui.drawRect(
                canvasX, canvasY + i * pixelSize(), canvasX + canvasSize(), canvasY + i * pixelSize() + 1, 0x44000000);
        }
        this.drawString(this.fontRendererObj, I18n.format("mobends.expression.canvasHint"), canvasX,
            canvasY + canvasSize() + 5, 0xbfbfbf);
    }

    private void drawColorPicker() {
        int x0 = pickerX();
        int y0 = pickerY();
        for (int x = 0; x < PICKER_WIDTH; x += 2) {
            float sat = (float) x / (PICKER_WIDTH - 1);
            for (int y = 0; y < PICKER_HEIGHT; y += 2) {
                float val = 1.0f - (float) y / (PICKER_HEIGHT - 1);
                int color = 0xff000000 | Color.HSBtoRGB(this.hue, sat, val) & 0xffffff;
                Gui.drawRect(
                    x0 + x, y0 + y, x0 + Math.min(x + 2, PICKER_WIDTH), y0 + Math.min(y + 2, PICKER_HEIGHT), color);
            }
        }
        int hueX = x0 + PICKER_WIDTH + 5;
        for (int y = 0; y < PICKER_HEIGHT; ++y) {
            int color = 0xff000000 | Color.HSBtoRGB((float) y / (PICKER_HEIGHT - 1), 1.0f, 1.0f) & 0xffffff;
            Gui.drawRect(hueX, y0 + y, hueX + HUE_WIDTH, y0 + y + 1, color);
        }
        int markerX = x0 + Math.round(this.saturation * (PICKER_WIDTH - 1));
        int markerY = y0 + Math.round((1.0f - this.brightness) * (PICKER_HEIGHT - 1));
        Gui.drawRect(markerX - 2, markerY - 2, markerX + 3, markerY + 3, 0xffffffff);
        Gui.drawRect(markerX - 1, markerY - 1, markerX + 2, markerY + 2, currentColor());
        int hueY = y0 + Math.round(this.hue * (PICKER_HEIGHT - 1));
        Gui.drawRect(hueX - 1, hueY - 1, hueX + HUE_WIDTH + 1, hueY + 2, 0xffffffff);
        Gui.drawRect(hueX + HUE_WIDTH + 6, y0, hueX + HUE_WIDTH + 31, y0 + 25, 0xffffffff);
        Gui.drawRect(hueX + HUE_WIDTH + 8, y0 + 2, hueX + HUE_WIDTH + 29, y0 + 23, currentColor());
    }

    private void drawPixel(int canvasX, int canvasY, int x, int y, int offsetX, int offsetY, int color) {
        int left = Math.max(canvasX, canvasX + x * pixelSize() + offsetX);
        int top = Math.max(canvasY, canvasY + y * pixelSize() + offsetY);
        int right = Math.min(canvasX + canvasSize(), canvasX + (x + 1) * pixelSize() + offsetX);
        int bottom = Math.min(canvasY + canvasSize(), canvasY + (y + 1) * pixelSize() + offsetY);
        if (right > left && bottom > top) {
            Gui.drawRect(left, top, right, bottom, color);
        }
    }

    private void drawClippedPupil(
        int canvasX, int canvasY, int x, int y, int offsetX, int offsetY, int color, int group) {
        if (group == 0) {
            return;
        }
        int sourceLeft = canvasX + x * pixelSize() + offsetX;
        int sourceTop = canvasY + y * pixelSize() + offsetY;
        int sourceRight = sourceLeft + pixelSize();
        int sourceBottom = sourceTop + pixelSize();
        for (int clipX = 0; clipX < 8; ++clipX) {
            for (int clipY = 0; clipY < 8; ++clipY) {
                if (this.eyeGroups[clipX][clipY] != group) {
                    continue;
                }
                int left = Math.max(sourceLeft, canvasX + clipX * pixelSize());
                int top = Math.max(sourceTop, canvasY + clipY * pixelSize());
                int right = Math.min(sourceRight, canvasX + (clipX + 1) * pixelSize());
                int bottom = Math.min(sourceBottom, canvasY + (clipY + 1) * pixelSize());
                if (right > left && bottom > top) {
                    Gui.drawRect(left, top, right, bottom, color);
                }
            }
        }
    }

    private boolean hasMovingPupil(int group) {
        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++)
                if (this.eyeGroups[x][y] == group && this.pupils[x][y])
                    return true;
        return false;
    }

    private float blinkProgress(long now) {
        long elapsed = now - this.blinkStartedAt;
        if (elapsed < 0 || elapsed >= BLINK_DURATION_MS) {
            return 0.0f;
        }
        return blinkPhase(elapsed);
    }

    private float blinkPhase(long elapsed) {
        float phase = (float) elapsed / BLINK_DURATION_MS;
        float linear = phase < 0.5f ? phase * 2.0f : (1.0f - phase) * 2.0f;
        return linear * linear * (3.0f - 2.0f * linear);
    }

    private void exportSkin() {
        if (this.skin == null) {
            this.statusSuccess = false;
            this.status = I18n.format("mobends.expression.selectionRequired");
            return;
        }
        BufferedImage output = generatedSkin();
        if (output == null)
            return;
        FileDialog dialog = new FileDialog((Frame) null, I18n.format("mobends.expression.saveDialog"), FileDialog.SAVE);
        String playerName = this.mc.thePlayer == null ? "player" : this.mc.thePlayer.getCommandSenderName();
        dialog.setFile(safeName(playerName) + "_expressions.png");
        dialog.setVisible(true);
        String filename = dialog.getFile();
        String directory = dialog.getDirectory();
        dialog.dispose();
        if (filename == null) {
            this.statusSuccess = false;
            this.status = I18n.format("mobends.expression.cancelled");
            return;
        }
        File file = new File(directory, filename);
        if (!file.getName().toLowerCase().endsWith(".png")) {
            file = new File(file.getParentFile(), file.getName() + ".png");
        }
        try {
            ImageIO.write(output, "png", file);
            this.status = I18n.format("mobends.expression.saved", file.getName());
            this.statusSuccess = true;
        } catch (IOException exception) {
            this.statusSuccess = false;
            this.status = I18n.format("mobends.expression.saveFailed", exception.getMessage());
        }
    }

    private BufferedImage generatedSkin() {
        try {
            return ExpressionSkinExporter.export(this.skin, this.data);
        } catch (IllegalArgumentException exception) {
            this.statusSuccess = false;
            this.status = exception.getMessage();
            return null;
        }
    }

    private void confirmWawelAuthUpload() {
        if (generatedSkin() == null)
            return;
        Account account = WawelAuthSkinUpload.activeAccount();
        if (account == null) {
            this.statusSuccess = false;
            this.status = I18n.format("mobends.upload.noAccount");
            return;
        }
        this.mc.displayGuiScreen(new GuiWawelAuthUploadConfirm(account, confirmed -> {
            this.mc.displayGuiScreen(this);
            if (!confirmed) {
                this.statusSuccess = false;
                this.status = I18n.format("mobends.upload.cancelled");
                return;
            }
            startWawelAuthUpload(account);
        }));
    }

    private void startWawelAuthUpload(Account account) {
        BufferedImage output = generatedSkin();
        if (output == null)
            return;
        this.uploading = true;
        this.statusSuccess = false;
        this.status = I18n.format("mobends.upload.progress", account.name, account.provider);
        updateExportButton();
        boolean slim = WawelAuthCompat.isSlim(this.mc.thePlayer);
        WawelAuthSkinUpload.upload(account, output, slim, (result, error) -> {
            this.uploading = false;
            if (error != null) {
                this.statusSuccess = false;
                this.status = I18n.format("mobends.upload.failed", error);
            } else {
                this.statusSuccess = false;
                this.status = result == null ? I18n.format("mobends.upload.success") : result;
            }
            updateExportButton();
        });
    }

    private void setTool(Tool tool) {
        this.tool = tool;
        syncPresets();
        this.eyedropperArmed = false;
        this.statusSuccess = false;
        this.status = I18n.format("mobends.expression.help." + tool.name().toLowerCase(java.util.Locale.ROOT));
        updateToolControls();
    }

    private void updateToolControls() {
        boolean eye = this.tool == Tool.EYE_REGION;
        boolean brow = this.tool == Tool.EYEBROWS || this.tool == Tool.OVERLAY_BROWS;
        for (int id = 15; id <= 17; id++)
            if (getButton(id) != null)
                getButton(id).visible = eye || brow;
        if (getButton(5) != null)
            getButton(5).displayString = I18n.format(
                isBrowTool() ? "mobends.expression.removeBrows" : "mobends.expression.clear." + this.activeGroup);
        if (getButton(20) != null)
            getButton(20).visible = eye || brow;
        if (getButton(15) != null)
            getButton(15).displayString = eye ? I18n.format("mobends.expression.preset", presetName())
                                              : I18n.format("mobends.expression.browPreset",
                                                    I18n.format("mobends.expression.brow."
                                                        + (this.tool == Tool.OVERLAY_BROWS ? 0 : this.browPreset)));
        for (int id : new int[] {9, 10, 18}) {
            GuiButton button = getButton(id);
            if (button != null)
                button.enabled = (id == 18 ? 0 : id == 9 ? 1 : 2) != this.activeGroup;
        }
        if (getButton(3) != null) {
            getButton(3).visible = isColorTool() && !brow;
            getButton(3).displayString = this.eyedropperArmed ? I18n.format("mobends.expression.picking")
                                                              : I18n.format("mobends.expression.pickColor");
        }
        if (getButton(19) != null)
            getButton(19).displayString = I18n.format(
                "mobends.expression.preview", I18n.format("mobends.expression.preview." + this.previewMode));
        if (getButton(4) != null)
            getButton(4).displayString = I18n.format("mobends.expression.blink");
        if (this.hexField != null) {
            this.hexField.setVisible(isColorTool());
            if (!isColorTool())
                this.hexField.setFocused(false);
        }
    }

    private void remember() {
        this.undo.push(this.data.copy());
        while (this.undo.size() > 40) this.undo.removeLast();
        this.redo.clear();
    }

    private void undo(boolean forward) {
        java.util.Deque<ExpressionSkinExporter.EditorData> from = forward ? this.redo : this.undo;
        java.util.Deque<ExpressionSkinExporter.EditorData> to = forward ? this.undo : this.redo;
        if (from.isEmpty())
            return;
        to.push(this.data.copy());
        this.data.copyFrom(from.pop());
        syncPresets();
        updateToolControls();
        edited();
    }

    private void paintPixel(int x, int y, boolean value) {
        if (this.activeGroup != 0 && this.activeGroup != (x < 4 ? 1 : 2))
            return;
        int color = value ? currentColor() : 0;
        if (this.eyedropperArmed) {
            setCurrentColor(compositeFacePixel(this.skin, x, y));
            return;
        }
        if (this.tool == Tool.PUPIL_COLORS && this.pupils[x][y])
            this.data.pupilColors[x][y] = color;
        else if (this.tool == Tool.EYE_WHITES && isOutlined(x, y)) {
            int group = this.eyeGroups[x][y], count = 0;
            for (int xx = 0; xx < 8; xx++)
                if (this.eyeGroups[xx][y] == group)
                    count++;
            for (int xx = 0; xx < 8; xx++)
                if (this.eyeGroups[xx][y] == group && (count == 2 || xx == x))
                    this.pupilBackgroundColors[xx][y] = value ? color : ExpressionSkinExporter.base(this.skin, xx, y);
        } else if (this.tool == Tool.PAINT_EYELIDS && this.eyeGroups[x][y] != 0) {
            for (int yy = 0; yy < 8; yy++)
                if (this.eyeGroups[x][yy] == this.eyeGroups[x][y])
                    this.closedColors[x][yy] = value ? color : ExpressionSkinExporter.base(this.skin, x, y);
        } else if (this.tool == Tool.BROW_COLORS && this.eyebrowGroups[x][y] != 0)
            this.eyebrowColors[x][y] = color;
        else if (this.tool == Tool.BROW_BACKGROUND && this.eyebrowGroups[x][y] != 0)
            this.data.browBackgrounds[x][y] = value ? color : ExpressionSkinExporter.base(this.skin, x, y);
        else if (this.tool == Tool.OVERLAY_COLORS && this.data.overlayBrows[x][y] != 0)
            this.data.overlayColors[x][y] = color;
        else if (this.tool == Tool.EYELASHES && isOutlined(x, y))
            this.data.lashes[x][y] = color;
        if (joinedBrows() && (x == 3 || x == 4) && y == 3) {
            if (this.tool == Tool.BROW_COLORS)
                this.eyebrowColors[7 - x][y] = color;
            if (this.tool == Tool.BROW_BACKGROUND)
                this.data.browBackgrounds[7 - x][y] = this.data.browBackgrounds[x][y];
        }
        edited();
    }

    private void syncPresets() {
        int side = this.activeGroup == 2 ? 1 : 0;
        ExpressionSkinExporter.EyeType type = ExpressionSkinExporter.eyeType(this.data, side);
        if (type != null) {
            this.preset = type.ordinal();
            this.presetRow = ExpressionSkinExporter.eyeRow(this.data, side);
        }
        byte[][] brows = this.tool == Tool.OVERLAY_BROWS ? this.data.overlayBrows : this.eyebrowGroups;
        for (int x = side * 4; x < side * 4 + 4; x++)
            for (int y = 0; y < 8; y++)
                if (brows[x][y] != 0) {
                    this.browRow = y;
                    this.browPreset = joinedBrows() ? 2 : brows[side == 0 ? 0 : 7][3] != 0 ? 1 : 0;
                    return;
                }
    }

    private String presetName() {
        return I18n.format("mobends.expression.eye."
            + ExpressionSkinExporter.EyeType.values()[this.preset].name().toLowerCase(java.util.Locale.ROOT));
    }

    private void applyPreset() {
        if (this.skin == null)
            return;
        ExpressionSkinExporter.EyeType type = ExpressionSkinExporter.EyeType.values()[this.preset];
        this.presetRow = Math.max(type.minRow, Math.min(type.maxRow, this.presetRow));
        for (int side = 0; side < 2; side++)
            if (this.activeGroup == 0 || this.activeGroup == side + 1)
                ExpressionSkinExporter.eyePreset(this.data, this.skin, type, this.presetRow, side);
        updateToolControls();
        edited();
    }

    private void applyBrowPreset() {
        boolean overlay = this.tool == Tool.OVERLAY_BROWS;
        byte[][] mask = overlay ? this.data.overlayBrows : this.eyebrowGroups;
        int[][] colors = overlay ? this.data.overlayColors : this.eyebrowColors;
        int type = overlay ? 0 : this.browPreset;
        boolean replaceJoined = !overlay && joinedBrows();
        this.browRow = type == 0 ? Math.max(2, Math.min(5, this.browRow)) : 3;
        for (int x = 0; x < 8; x++)
            if (type == 2 || replaceJoined || this.activeGroup == 0 || this.activeGroup == (x < 4 ? 1 : 2)) {
                for (int y = 0; y < 8; y++) mask[x][y] = 0;
                boolean selected = type == 2 ? x >= 1 && x <= 6
                    : type == 1              ? x < 2 || x > 5
                                             : x == 1 || x == 2 || x == 5 || x == 6;
                if (selected) {
                    mask[x][this.browRow] = (byte) (type == 2 ? 1 : x < 4 ? 1 : 2);
                    colors[x][this.browRow] = ExpressionSkinExporter.base(this.skin, x, this.browRow);
                    if (!overlay)
                        this.data.browBackgrounds[x][this.browRow] =
                            ExpressionSkinExporter.base(this.skin, x, this.browRow - 1);
                }
            }
        if (type == 2) {
            colors[4][3] = colors[3][3];
            this.data.browBackgrounds[4][3] = this.data.browBackgrounds[3][3];
        }
        updateToolControls();
        edited();
    }

    private boolean isBrowTool() {
        return this.tool == Tool.EYEBROWS || this.tool == Tool.BROW_COLORS || this.tool == Tool.BROW_BACKGROUND
            || this.tool == Tool.OVERLAY_BROWS || this.tool == Tool.OVERLAY_COLORS;
    }

    private void clearSelection() {
        if (isBrowTool()) {
            this.data.clearBrows();
            return;
        }
        for (int x = 0; x < 8; x++)
            if (this.activeGroup == 0 || this.activeGroup == (x < 4 ? 1 : 2))
                for (int y = 0; y < 8; y++) {
                    if (this.tool == Tool.EYE_REGION) {
                        this.eyeGroups[x][y] = 0;
                        this.pupils[x][y] = false;
                    } else if (this.tool == Tool.EYELASHES)
                        this.data.lashes[x][y] = 0;
                    else if (this.tool == Tool.PUPIL_COLORS)
                        this.data.pupilColors[x][y] = 0;
                    else if (this.tool == Tool.EYE_WHITES)
                        this.pupilBackgroundColors[x][y] = 0xffffffff;
                    else
                        this.closedColors[x][y] =
                            ExpressionSkinExporter.base(this.skin, x, Math.max(0, groupTop(x < 4 ? 1 : 2) - 1));
                }
    }

    private void updateSaturationBrightness(int mouseX, int mouseY) {
        this.saturation = clamp((float) (mouseX - pickerX()) / (PICKER_WIDTH - 1));
        this.brightness = 1.0f - clamp((float) (mouseY - pickerY()) / (PICKER_HEIGHT - 1));
        updateHex();
    }

    private void updateHue(int mouseY) {
        this.hue = clamp((float) (mouseY - pickerY()) / (PICKER_HEIGHT - 1));
        updateHex();
    }

    private void setCurrentColor(int color) {
        setCurrentColor(color, true);
    }

    private void setCurrentColor(int color, boolean updateField) {
        float[] hsb = Color.RGBtoHSB(color >> 16 & 255, color >> 8 & 255, color & 255, null);
        if (hsb[1] > 0.0f) {
            this.hue = hsb[0];
        }
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        if (updateField)
            updateHex();
    }

    private int currentColor() {
        return 0xff000000 | Color.HSBtoRGB(this.hue, this.saturation, this.brightness) & 0xffffff;
    }

    private void updateHex() {
        if (this.hexField != null) {
            this.hexField.setText(String.format("#%06X", currentColor() & 0xffffff));
        }
    }

    private boolean isColorTool() {
        return this.tool != Tool.EYE_REGION && this.tool != Tool.EYEBROWS && this.tool != Tool.OVERLAY_BROWS;
    }

    private boolean joinedBrows() {
        for (int x = 1; x <= 6; x++)
            if (this.eyebrowGroups[x][3] == 0)
                return false;
        return true;
    }

    private void edited() {
        this.status = I18n.format("mobends.expression.ready");
        this.statusSuccess = true;
        updateExportButton();
    }

    private int compositeFacePixel(BufferedImage image, int x, int y) {
        if (image == null) {
            return 0xff202020;
        }
        int base = image.getRGB(8 + x, 8 + y);
        int overlay = image.getRGB(40 + x, 8 + y);
        int alpha = overlay >>> 24;
        if (alpha == 0) {
            return 0xff000000 | base & 0xffffff;
        }
        if (alpha == 255) {
            return overlay;
        }
        int inverse = 255 - alpha;
        int red = (((overlay >> 16) & 255) * alpha + ((base >> 16) & 255) * inverse) / 255;
        int green = (((overlay >> 8) & 255) * alpha + ((base >> 8) & 255) * inverse) / 255;
        int blue = ((overlay & 255) * alpha + (base & 255) * inverse) / 255;
        return 0xff000000 | red << 16 | green << 8 | blue;
    }

    private boolean isOutlined(int x, int y) {
        if (this.activeGroup != 0 && this.activeGroup != (x < 4 ? 1 : 2))
            return false;
        if (this.tool == Tool.PUPIL_COLORS)
            return this.pupils[x][y];
        if (this.tool == Tool.EYE_WHITES)
            return this.eyeGroups[x][y] != 0 && hasMovingPupil(this.eyeGroups[x][y]);
        if (this.tool == Tool.EYEBROWS || this.tool == Tool.BROW_COLORS || this.tool == Tool.BROW_BACKGROUND)
            return this.eyebrowGroups[x][y] != 0;
        if (this.tool == Tool.OVERLAY_BROWS || this.tool == Tool.OVERLAY_COLORS)
            return this.data.overlayBrows[x][y] != 0;
        if (this.tool == Tool.EYELASHES)
            return ((x == 1 || x == 2 || x == 5 || x == 6) && y == 4) || ((x == 0 || x == 7) && (y == 5 || y == 6));
        return this.eyeGroups[x][y] != 0;
    }

    private int groupTop(int group) {
        for (int y = 0; y < 8; ++y) {
            for (int x = 0; x < 8; ++x) {
                if (this.eyeGroups[x][y] == group) {
                    return y;
                }
            }
        }
        return 0;
    }

    private int groupBottom(int group) {
        for (int y = 7; y >= 0; --y) {
            for (int x = 0; x < 8; ++x) {
                if (this.eyeGroups[x][y] == group) {
                    return y;
                }
            }
        }
        return 0;
    }

    private String stepName() {
        return I18n.format("mobends.expression.step", this.tool.ordinal() + 1, Tool.values().length,
            I18n.format("mobends.expression.tool." + this.tool.name().toLowerCase(java.util.Locale.ROOT)));
    }

    private void loadExistingData() {
        ExpressionSkinExporter.EditorData loaded = ExpressionSkinExporter.readEditorData(this.skin);
        if (loaded == null) {
            this.status = I18n.format("mobends.expression.notPrepared");
            return;
        }
        this.data.copyFrom(loaded);
        syncPresets();
        this.statusSuccess = true;
        this.status = I18n.format("mobends.expression.loaded");
    }

    private void updateExportButton() {
        this.validationError = null;
        boolean valid = this.skin != null;
        if (valid) {
            try {
                ExpressionSkinExporter.export(this.skin, this.data);
            } catch (IllegalArgumentException error) {
                valid = false;
                this.validationError = error.getMessage();
            }
        }
        for (int id : new int[] {6, 14})
            if (getButton(id) != null)
                getButton(id).enabled = valid && !this.uploading;
    }

    private GuiButton getButton(int id) {
        for (Object object : this.buttonList) {
            GuiButton button = (GuiButton) object;
            if (button.id == id) {
                return button;
            }
        }
        return null;
    }

    private int faceX(int mouseX) {
        int relative = mouseX - canvasX();
        return relative >= 0 && relative < canvasSize() ? relative / pixelSize() : -1;
    }

    private int faceY(int mouseY) {
        int relative = mouseY - canvasY();
        return relative >= 0 && relative < canvasSize() ? relative / pixelSize() : -1;
    }

    private int panelWidth() {
        return this.width >= 380 ? 190 : 124;
    }
    private int pixelSize() {
        return this.width >= 380 ? 16 : 12;
    }
    private int canvasSize() {
        return pixelSize() * 8;
    }

    private int canvasX() {
        return (this.width - panelWidth() - 182) / 2;
    }

    private int canvasY() {
        return Math.max(30, this.height / 2 - 85);
    }

    private int controlsX() {
        return canvasX() + panelWidth();
    }

    private int controlsY() {
        return Math.max(34, this.height / 2 - 105);
    }

    private int pickerX() {
        return controlsX();
    }

    private int pickerY() {
        return controlsY() + 46;
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static int componentColor(int group) {
        int[] colors = {0xff55ff55, 0xffffaa33, 0xff55ddff, 0xffff66cc, 0xffcc77ff, 0xffffff55};
        return colors[Math.max(0, group - 1) % colors.length];
    }

    private static String safeName(String name) {
        return name == null ? "player" : name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}
