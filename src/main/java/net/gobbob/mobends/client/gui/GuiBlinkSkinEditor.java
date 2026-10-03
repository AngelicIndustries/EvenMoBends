package net.gobbob.mobends.client.gui;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import javax.imageio.ImageIO;
import net.gobbob.mobends.client.render.BlinkSkinExporter;
import net.gobbob.mobends.client.render.PlayerSkinImages;
import net.gobbob.mobends.client.render.ExpressionSkinExporter;
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
public class GuiBlinkSkinEditor extends GuiScreen {
    private static final int CANVAS_SIZE = 128;
    private static final int PIXEL_SIZE = CANVAS_SIZE / 8;
    private static final int PICKER_WIDTH = 104;
    private static final int PICKER_HEIGHT = 45;
    private static final int HUE_WIDTH = 12;
    private static final long BLINK_DURATION_MS = 520L;

    private enum Tool {
        EYE_REGION,
        PUPILS,
        PUPIL_BACKGROUND,
        PAINT_EYELIDS,
        EYEBROWS
    }

    private final GuiScreen parent;
    /** Four-directionally connected nonzero pixels are inferred as coherent eye components. */
    private final byte[][] eyeGroups = new byte[8][8];
    private final boolean[][] pupils = new boolean[8][8];
    private final int[][] closedColors = new int[8][8];
    private final int[][] pupilBackgroundColors = new int[8][8];
    /** Eyebrows are independent overlays, grouped so every pixel moves as one piece. */
    private final byte[][] eyebrowGroups = new byte[8][8];
    private final int[][] eyebrowColors = new int[8][8];
    private BufferedImage skin;
    private GuiTextField hexField;
    private boolean statusSuccess;
    private String status = I18n.format("mobends.blink.instructions");
    private Tool tool = Tool.EYE_REGION;
    private boolean eyedropperArmed;
    private boolean draggingCanvas;
    private boolean dragValue;
    private int pickerDrag;
    private int activeGroup = 1;
    private float hue;
    private float saturation;
    private float brightness = 1.0f;
    private long blinkStartedAt = Long.MIN_VALUE;
    private boolean replaceArmed;
    private boolean editorDataLoaded;
    private boolean uploading;
    private int preset;
    private int presetRow = 4;

    public GuiBlinkSkinEditor(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.skin = PlayerSkinImages.copyPlayerSkin(this.mc.thePlayer);
        if (!this.editorDataLoaded) {
            loadExistingData();
            this.editorDataLoaded = true;
        }
        int x = controlsX();
        int y = controlsY();
        this.buttonList.add(new GuiButton(12, x, y, 24, 20, "<"));
        this.buttonList.add(new GuiButton(13, x + 158, y, 24, 20, ">"));
        this.buttonList.add(new GuiButton(9, x, y + 24, 88, 20, I18n.format("mobends.blink.leftEye")));
        this.buttonList.add(new GuiButton(10, x + 94, y + 24, 88, 20, I18n.format("mobends.blink.rightEye")));
        this.buttonList.add(new GuiButton(15, x, y + 50, 182, 20, ""));
        this.buttonList.add(new GuiButton(16, x, y + 74, 88, 20, I18n.format("mobends.expression.up")));
        this.buttonList.add(new GuiButton(17, x + 94, y + 74, 88, 20, I18n.format("mobends.expression.down")));
        this.buttonList.add(new GuiButton(3, x, y + 119, 182, 20, I18n.format("mobends.blink.pickColor")));
        this.buttonList.add(new GuiButton(4, x, this.height - 50, 88, 20, I18n.format("mobends.blink.blink")));
        this.buttonList.add(new GuiButton(5, x + 94, this.height - 50, 88, 20, I18n.format("mobends.blink.clear")));
        int bottomY = this.height - 28;
        int bottomX = this.width / 2 - 116;
        GuiButton upload = new GuiWawelAuthIconButton(14, bottomX, bottomY);
        upload.visible = WawelAuthCompat.isLoaded();
        upload.enabled = this.skin != null && hasSelection() && !this.uploading;
        this.buttonList.add(upload);
        this.buttonList.add(new GuiButton(7, bottomX + 24, bottomY, 88, 20, I18n.format("mobends.gui.back")));
        GuiButton export = new GuiButton(6, bottomX + 116, bottomY, 116, 20, I18n.format("mobends.blink.export"));
        export.enabled = this.skin != null && hasSelection();
        this.buttonList.add(export);
        this.hexField = new GuiTextField(this.fontRendererObj, x + 42, pickerY() + PICKER_HEIGHT + 4, 78, 18);
        this.hexField.setMaxStringLength(7);
        this.hexField.setText("#FFFFFF");
        updateToolControls();
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 12 || button.id == 13) {
            Tool[] tools = Tool.values();
            int direction = button.id == 12 ? -1 : 1;
            setTool(tools[(this.tool.ordinal() + direction + tools.length) % tools.length]);
        } else if (button.id == 9 || button.id == 10) {
            this.activeGroup = button.id == 9 ? 1 : 2;
            this.statusSuccess = false;
            this.status = this.activeGroup == 1 ? I18n.format("mobends.blink.paintingLeft")
                : I18n.format("mobends.blink.paintingRight");
            updateToolControls();
        } else if (button.id >= 15 && button.id <= 17) {
            if (button.id == 15) this.preset = (this.preset + 1) % 6;
            else this.presetRow += button.id == 16 ? -1 : 1;
            applyPreset();
        } else if (button.id == 3) {
            this.eyedropperArmed = !this.eyedropperArmed;
            updateToolControls();
        } else if (button.id == 4) {
            this.blinkStartedAt = System.currentTimeMillis();
        } else if (button.id == 5) {
            clearSelection();
            this.statusSuccess = false;
            this.status = I18n.format("mobends.blink.instructions");
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
        if (isColorTool()
            && inside(mouseX, mouseY, pickerX(), pickerY(), PICKER_WIDTH, PICKER_HEIGHT)) {
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
            setCurrentColor(compositeFacePixel(this.skin, x, y, false));
            this.eyedropperArmed = false;
            updateToolControls();
        } else if (isColorTool()) {
            this.dragValue = button != 1;
            this.draggingCanvas = true;
            paintPixel(x, y, this.dragValue);
        } else {
            this.dragValue = button != 1;
            this.draggingCanvas = true;
            applyMaskTool(x, y, this.dragValue);
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
            } else {
                applyMaskTool(x, y, this.dragValue);
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
        if (key == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(this.parent);
            return;
        }
        if (this.hexField.isFocused()) {
            this.hexField.textboxKeyTyped(character, key);
            String value = this.hexField.getText().trim();
            if (value.matches("#[0-9a-fA-F]{6}")) {
                setCurrentColor(0xff000000 | Integer.parseInt(value.substring(1), 16));
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
                setCurrentColor(compositeFacePixel(this.skin, hoverX, hoverY, false));
            }
        }
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, I18n.format("mobends.blink.title"), this.width / 2, 12, 0xffffff);
        this.drawCenteredString(this.fontRendererObj, stepName(), controlsX() + 91, controlsY() + 6, 0xffffff);
        drawAnimatedFace();
        if (isColorTool()) {
            drawColorPicker();
            this.drawString(this.fontRendererObj, I18n.format("mobends.blink.hex"), controlsX(), pickerY() + PICKER_HEIGHT + 9, 0xbfbfbf);
            this.hexField.drawTextBox();
        }
        this.fontRendererObj.drawSplitString(this.status, canvasX(), canvasY() + 145, 165,
            this.statusSuccess ? 0x55ff55 : 0xbfbfbf);
        super.drawScreen(mouseX, mouseY, partialTicks);
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
                int color = this.skin == null ? 0xff202020 : compositeFacePixel(this.skin, x, y, false);
                if (this.pupils[x][y]) {
                    color = 0xff000000 | this.pupilBackgroundColors[x][y] & 0xffffff;
                }
                drawPixel(canvasX, canvasY, x, y, 0, 0, color);
            }
        }

        long now = System.currentTimeMillis();
        float blink = this.tool == Tool.PAINT_EYELIDS
            ? 1.0f - blinkProgress(now)
            : blinkProgress(now);
        int pupilOffsetX = Math.round((float)Math.sin(now / 760.0) * 3.0f * (1.0f - blink));
        int pupilOffsetY = Math.round((float)Math.sin(now / 1130.0) * 2.0f * (1.0f - blink));
        for (int x = 0; x < 8; ++x) {
            for (int y = 0; y < 8; ++y) {
                if (!this.pupils[x][y]) {
                    continue;
                }
                drawClippedPupil(canvasX, canvasY, x, y, pupilOffsetX, pupilOffsetY,
                    compositeFacePixel(this.skin, x, y, false), this.eyeGroups[x][y]);
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
                    int boundary = canvasY + top * PIXEL_SIZE
                        + Math.round((bottom - top + 1) * PIXEL_SIZE * blink);
                    int pixelTop = canvasY + y * PIXEL_SIZE;
                    int pixelBottom = Math.min(canvasY + (y + 1) * PIXEL_SIZE, boundary);
                    if (pixelBottom > pixelTop) {
                        Gui.drawRect(canvasX + x * PIXEL_SIZE, pixelTop,
                            canvasX + (x + 1) * PIXEL_SIZE, pixelBottom, this.closedColors[x][y]);
                    }
                }
            }
        }

        // Fresh Moves-style brows are independent pixels rendered over the face, not holes cut from it.
        int leftBrowY = Math.round((float)Math.sin(now / 920.0) * 2.0f);
        int rightBrowY = Math.round((float)Math.sin(now / 1030.0 + 0.7) * 2.0f);
        for (int x = 0; x < 8; ++x) {
            for (int y = 0; y < 8; ++y) {
                int group = this.eyebrowGroups[x][y];
                if (group != 0) {
                    drawPixel(canvasX, canvasY, x, y, 0, group == 1 ? leftBrowY : rightBrowY,
                        this.eyebrowColors[x][y]);
                }
            }
        }

        for (int x = 0; x < 8; ++x) {
            for (int y = 0; y < 8; ++y) {
                boolean outlined = isOutlined(x, y);
                if (!outlined) {
                    continue;
                }
                int left = canvasX + x * PIXEL_SIZE;
                int top = canvasY + y * PIXEL_SIZE;
                int group = this.tool == Tool.EYEBROWS ? this.eyebrowGroups[x][y] : this.eyeGroups[x][y];
                int outline = this.tool == Tool.PUPILS ? 0xff4499ff
                    : this.tool == Tool.PUPIL_BACKGROUND ? 0xffff66cc
                        : componentColor(group);
                Gui.drawRect(left, top, left + PIXEL_SIZE, top + 1, outline);
                Gui.drawRect(left, top + PIXEL_SIZE - 1, left + PIXEL_SIZE, top + PIXEL_SIZE, outline);
                Gui.drawRect(left, top, left + 1, top + PIXEL_SIZE, outline);
                Gui.drawRect(left + PIXEL_SIZE - 1, top, left + PIXEL_SIZE, top + PIXEL_SIZE, outline);
            }
        }
        for (int i = 0; i <= 8; ++i) {
            Gui.drawRect(canvasX + i * PIXEL_SIZE, canvasY, canvasX + i * PIXEL_SIZE + 1,
                canvasY + CANVAS_SIZE, 0x44000000);
            Gui.drawRect(canvasX, canvasY + i * PIXEL_SIZE, canvasX + CANVAS_SIZE,
                canvasY + i * PIXEL_SIZE + 1, 0x44000000);
        }
        String legend = this.tool == Tool.EYE_REGION
            ? I18n.format("mobends.blink.legend.regions")
            : this.tool == Tool.PUPILS ? I18n.format("mobends.blink.legend.pupils")
                : this.tool == Tool.PUPIL_BACKGROUND ? I18n.format("mobends.blink.legend.background")
                    : this.tool == Tool.PAINT_EYELIDS
                        ? I18n.format("mobends.blink.legend.eyelid")
                        : I18n.format("mobends.blink.legend.eyebrow");
        this.drawString(this.fontRendererObj, legend, canvasX, canvasY + 133, 0xbfbfbf);
    }

    private void drawColorPicker() {
        int x0 = pickerX();
        int y0 = pickerY();
        for (int x = 0; x < PICKER_WIDTH; x += 2) {
            float sat = (float)x / (PICKER_WIDTH - 1);
            for (int y = 0; y < PICKER_HEIGHT; y += 2) {
                float val = 1.0f - (float)y / (PICKER_HEIGHT - 1);
                int color = 0xff000000 | Color.HSBtoRGB(this.hue, sat, val) & 0xffffff;
                Gui.drawRect(x0 + x, y0 + y, x0 + Math.min(x + 2, PICKER_WIDTH),
                    y0 + Math.min(y + 2, PICKER_HEIGHT), color);
            }
        }
        int hueX = x0 + PICKER_WIDTH + 5;
        for (int y = 0; y < PICKER_HEIGHT; ++y) {
            int color = 0xff000000 | Color.HSBtoRGB((float)y / (PICKER_HEIGHT - 1), 1.0f, 1.0f) & 0xffffff;
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
        int left = Math.max(canvasX, canvasX + x * PIXEL_SIZE + offsetX);
        int top = Math.max(canvasY, canvasY + y * PIXEL_SIZE + offsetY);
        int right = Math.min(canvasX + CANVAS_SIZE, canvasX + (x + 1) * PIXEL_SIZE + offsetX);
        int bottom = Math.min(canvasY + CANVAS_SIZE, canvasY + (y + 1) * PIXEL_SIZE + offsetY);
        if (right > left && bottom > top) {
            Gui.drawRect(left, top, right, bottom, color);
        }
    }

    private void drawClippedPupil(int canvasX, int canvasY, int x, int y, int offsetX, int offsetY,
        int color, int group) {
        if (group == 0) {
            return;
        }
        int sourceLeft = canvasX + x * PIXEL_SIZE + offsetX;
        int sourceTop = canvasY + y * PIXEL_SIZE + offsetY;
        int sourceRight = sourceLeft + PIXEL_SIZE;
        int sourceBottom = sourceTop + PIXEL_SIZE;
        for (int clipX = 0; clipX < 8; ++clipX) {
            for (int clipY = 0; clipY < 8; ++clipY) {
                if (this.eyeGroups[clipX][clipY] != group) {
                    continue;
                }
                int left = Math.max(sourceLeft, canvasX + clipX * PIXEL_SIZE);
                int top = Math.max(sourceTop, canvasY + clipY * PIXEL_SIZE);
                int right = Math.min(sourceRight, canvasX + (clipX + 1) * PIXEL_SIZE);
                int bottom = Math.min(sourceBottom, canvasY + (clipY + 1) * PIXEL_SIZE);
                if (right > left && bottom > top) {
                    Gui.drawRect(left, top, right, bottom, color);
                }
            }
        }
    }

    private float blinkProgress(long now) {
        long elapsed = now - this.blinkStartedAt;
        if (elapsed < 0 || elapsed >= BLINK_DURATION_MS) {
            return 0.0f;
        }
        float phase = (float)elapsed / BLINK_DURATION_MS;
        float linear = phase < 0.5f ? phase * 2.0f : (1.0f - phase) * 2.0f;
        return linear * linear * (3.0f - 2.0f * linear);
    }

    private void exportSkin() {
        if (this.skin == null || !hasSelection()) {
            this.statusSuccess = false;
            this.status = I18n.format("mobends.blink.selectionRequired");
            return;
        }
        BufferedImage output = generatedSkin();
        if (output == null) return;
        if ((BlinkSkinExporter.hasBlinkData(this.skin) || ExpressionSkinExporter.readEditorData(this.skin) != null) && !this.replaceArmed) {
            this.replaceArmed = true;
            this.statusSuccess = false;
            this.status = I18n.format("mobends.blink.replaceWarning");
            return;
        }
        FileDialog dialog = new FileDialog((Frame)null, I18n.format("mobends.blink.saveDialog"), FileDialog.SAVE);
        String playerName = this.mc.thePlayer == null ? "player" : this.mc.thePlayer.getCommandSenderName();
        dialog.setFile(safeName(playerName) + "_expressions.png");
        dialog.setVisible(true);
        if (dialog.getFile() == null) {
            this.statusSuccess = false;
            this.status = I18n.format("mobends.blink.cancelled");
            return;
        }
        File file = new File(dialog.getDirectory(), dialog.getFile());
        if (!file.getName().toLowerCase().endsWith(".png")) {
            file = new File(file.getParentFile(), file.getName() + ".png");
        }
        try {
            ImageIO.write(output, "png", file);
            this.status = I18n.format("mobends.blink.saved", file.getName());
            this.statusSuccess = true;
        } catch (IOException exception) {
            this.statusSuccess = false;
            this.status = I18n.format("mobends.blink.saveFailed", exception.getMessage());
        }
    }

    private BufferedImage generatedSkin() {
        try {
            return ExpressionSkinExporter.export(this.skin, this.eyeGroups, this.pupils, this.closedColors,
                this.pupilBackgroundColors, this.eyebrowGroups, this.eyebrowColors);
        } catch (IllegalArgumentException exception) {
            this.statusSuccess = false;
            this.status = exception.getMessage();
            return null;
        }
    }

    private void confirmWawelAuthUpload() {
        if (generatedSkin() == null) return;
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
        if (output == null) return;
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
        this.eyedropperArmed = false;
        this.statusSuccess = false;
        this.status = tool == Tool.EYE_REGION
            ? I18n.format("mobends.blink.help.regions")
            : tool == Tool.PUPILS
                ? I18n.format("mobends.blink.help.pupils")
                : tool == Tool.PUPIL_BACKGROUND
                    ? I18n.format("mobends.blink.help.background")
                    : tool == Tool.PAINT_EYELIDS
                        ? I18n.format("mobends.blink.help.eyelids")
                        : I18n.format("mobends.blink.help.eyebrows");
        updateToolControls();
    }

    private void updateToolControls() {
        for (int id=15; id<=17; id++) if (getButton(id)!=null) getButton(id).visible = this.tool == Tool.EYE_REGION;
        if (getButton(15)!=null) getButton(15).displayString = I18n.format("mobends.expression.preset", presetName());
        GuiButton leftGroup = getButton(9);
        GuiButton rightGroup = getButton(10);
        GuiButton pick = getButton(3);
        GuiButton blink = getButton(4);
        boolean grouped = this.tool == Tool.EYEBROWS;
        if (leftGroup != null) {
            leftGroup.visible = grouped;
            leftGroup.displayString = this.activeGroup == 1 ? I18n.format("mobends.blink.leftGroup.selected") : I18n.format("mobends.blink.leftGroup");
        }
        if (rightGroup != null) {
            rightGroup.visible = grouped;
            rightGroup.displayString = this.activeGroup == 2 ? I18n.format("mobends.blink.rightGroup.selected") : I18n.format("mobends.blink.rightGroup");
        }
        if (pick != null) {
            pick.visible = isColorTool();
            pick.displayString = this.eyedropperArmed ? I18n.format("mobends.blink.picking") : I18n.format("mobends.blink.pickColor");
        }
        if (blink != null) {
            blink.visible = this.tool == Tool.PAINT_EYELIDS || this.tool == Tool.EYEBROWS;
            blink.displayString = this.tool == Tool.PAINT_EYELIDS ? I18n.format("mobends.blink.openEyes") : I18n.format("mobends.blink.blink");
        }
        if (this.hexField != null) {
            this.hexField.setVisible(isColorTool());
        }
    }

    private void applyMaskTool(int x, int y, boolean value) {
        if (this.tool == Tool.EYE_REGION) {
            setEyePixel(x, y, value);
            return;
        }
        if (this.tool == Tool.PUPILS) {
            if (value && this.eyeGroups[x][y] == 0) {
                this.statusSuccess = false;
                this.status = I18n.format("mobends.blink.pupilsRequired");
                return;
            }
            boolean wasPupil = this.pupils[x][y];
            this.pupils[x][y] = value;
            if (value && !wasPupil) {
                this.pupilBackgroundColors[x][y] = compositeFacePixel(this.skin, x, y, false);
            }
            this.replaceArmed = false;
        }
    }

    private void paintPixel(int x, int y, boolean value) {
        if (this.tool == Tool.PUPIL_BACKGROUND) {
            if (!this.pupils[x][y]) {
                this.statusSuccess = false;
                this.status = I18n.format("mobends.blink.backgroundRequired");
                return;
            }
            if (value) {
                this.pupilBackgroundColors[x][y] = currentColor();
            }
        } else if (this.tool == Tool.PAINT_EYELIDS) {
            if (this.eyeGroups[x][y] == 0 && this.eyebrowGroups[x][y] == 0) {
                this.statusSuccess = false;
                this.status = I18n.format("mobends.blink.eyelidsRequired");
                return;
            }
            this.closedColors[x][y] = value ? currentColor()
                : compositeFacePixel(this.skin, x, y, false);
        } else if (this.tool == Tool.EYEBROWS) {
            if (value && this.eyebrowGroups[x][y] == 0) this.closedColors[x][y] = compositeFacePixel(this.skin, x, Math.max(0,y-1), false);
            this.eyebrowGroups[x][y] = value ? (byte)this.activeGroup : 0;
            this.eyebrowColors[x][y] = value ? currentColor() : 0;
        }
        this.replaceArmed = false;
    }

    private String presetName() {
        return new String[] {"2x1", "2x2", "2x3", "1x1", "1x2", "1x3"}[this.preset];
    }

    private void applyPreset() {
        if (this.skin == null) return;
        int w=this.preset<3 ? 2 : 1, h=this.preset%3+1;
        int min=this.preset==0 ? 3 : this.preset==1 ? 2 : this.preset==2 ? 4 : this.preset==5 ? 2 : 3;
        int max=this.preset==0 ? 6 : this.preset==1 ? 5 : this.preset==2 ? 4 : this.preset==5 ? 3 : 4;
        this.presetRow=Math.max(min,Math.min(max,this.presetRow));
        for (int x=0; x<8; x++) for (int y=0; y<8; y++) { this.eyeGroups[x][y]=0; this.pupils[x][y]=false; }
        for (int side=0; side<2; side++) {
            int x=w==2 ? (side==0 ? 1 : 5) : (side==0 ? 2 : 5);
            int whiteX=side==0 ? x : x+w-1;
            for (int dx=0; dx<w; dx++) for (int dy=0; dy<h; dy++) {
                int xx=x+dx, yy=this.presetRow+dy;
                this.eyeGroups[xx][yy]=(byte)(side+1);
                this.pupils[xx][yy]=w==2 && dx==(side==0 ? 1 : 0) && dy<Math.min(h,2);
                this.closedColors[xx][yy]=compositeFacePixel(this.skin,xx,this.presetRow-1,false);
                this.pupilBackgroundColors[xx][yy]=compositeFacePixel(this.skin,whiteX,yy,false);
            }
        }
        this.replaceArmed=false;
        this.statusSuccess=false;
        this.status=I18n.format("mobends.expression.presetHelp");
        updateToolControls();
    }

    private void setEyePixel(int x, int y, boolean value) {
        if (value && this.eyeGroups[x][y] == 0) {
            this.closedColors[x][y] = compositeFacePixel(this.skin, x, y, false);
        }
        this.eyeGroups[x][y] = value ? (byte)1 : 0;
        if (!value) {
            this.pupils[x][y] = false;
            this.pupilBackgroundColors[x][y] = 0xff000000;
        }
        inferEyeComponents();
        this.replaceArmed = false;
        updateExportButton();
    }

    private void clearSelection() {
        for (int x = 0; x < 8; ++x) {
            for (int y = 0; y < 8; ++y) {
                this.eyeGroups[x][y] = 0;
                this.pupils[x][y] = false;
                this.closedColors[x][y] = 0xff000000;
                this.pupilBackgroundColors[x][y] = 0xff000000;
                this.eyebrowGroups[x][y] = 0;
                this.eyebrowColors[x][y] = 0;
            }
        }
        this.replaceArmed = false;
        this.eyedropperArmed = false;
    }

    private void updateSaturationBrightness(int mouseX, int mouseY) {
        this.saturation = clamp((float)(mouseX - pickerX()) / (PICKER_WIDTH - 1));
        this.brightness = 1.0f - clamp((float)(mouseY - pickerY()) / (PICKER_HEIGHT - 1));
        updateHex();
    }

    private void updateHue(int mouseY) {
        this.hue = clamp((float)(mouseY - pickerY()) / (PICKER_HEIGHT - 1));
        updateHex();
    }

    private void setCurrentColor(int color) {
        float[] hsb = Color.RGBtoHSB(color >> 16 & 255, color >> 8 & 255, color & 255, null);
        if (hsb[1] > 0.0f) {
            this.hue = hsb[0];
        }
        this.saturation = hsb[1];
        this.brightness = hsb[2];
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
        return this.tool == Tool.PAINT_EYELIDS
            || this.tool == Tool.PUPIL_BACKGROUND || this.tool == Tool.EYEBROWS;
    }

    private int compositeFacePixel(BufferedImage image, int x, int y, boolean closed) {
        if (image == null) {
            return 0xff202020;
        }
        int base = image.getRGB((closed ? 0 : 8) + x, (closed ? 0 : 8) + y);
        int overlay = image.getRGB((closed ? 32 : 40) + x, (closed ? 0 : 8) + y);
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

    private boolean hasSelection() {
        for (int x = 0; x < 8; ++x) {
            for (int y = 0; y < 8; ++y) {
                if (this.eyeGroups[x][y] != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isOutlined(int x, int y) {
        if (this.tool == Tool.PUPILS || this.tool == Tool.PUPIL_BACKGROUND) {
            return this.pupils[x][y];
        }
        if (this.tool == Tool.EYEBROWS) {
            return this.eyebrowGroups[x][y] == this.activeGroup;
        }
        if (this.tool == Tool.PAINT_EYELIDS) {
            return this.eyeGroups[x][y] != 0 || this.eyebrowGroups[x][y] != 0;
        }
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
        return I18n.format("mobends.blink.step", this.tool.ordinal() + 1, Tool.values().length,
            (this.tool == Tool.EYE_REGION ? I18n.format("mobends.blink.tool.regions")
                : this.tool == Tool.PUPILS ? I18n.format("mobends.blink.tool.pupils")
                    : this.tool == Tool.PUPIL_BACKGROUND ? I18n.format("mobends.blink.tool.background")
                        : this.tool == Tool.PAINT_EYELIDS ? I18n.format("mobends.blink.tool.eyelids") : I18n.format("mobends.blink.tool.eyebrows")));
    }

    private void inferEyeComponents() {
        boolean[][] mask = new boolean[8][8];
        for (int x = 0; x < 8; ++x) {
            for (int y = 0; y < 8; ++y) {
                mask[x][y] = this.eyeGroups[x][y] != 0;
                this.eyeGroups[x][y] = 0;
            }
        }
        int group = 0;
        int[] queueX = new int[64];
        int[] queueY = new int[64];
        for (int startX = 0; startX < 8; ++startX) {
            for (int startY = 0; startY < 8; ++startY) {
                if (!mask[startX][startY] || this.eyeGroups[startX][startY] != 0) continue;
                ++group;
                int first = 0;
                int last = 1;
                queueX[0] = startX;
                queueY[0] = startY;
                this.eyeGroups[startX][startY] = (byte)group;
                while (first < last) {
                    int x = queueX[first];
                    int y = queueY[first++];
                    int[] dx = {-1, 1, 0, 0};
                    int[] dy = {0, 0, -1, 1};
                    for (int i = 0; i < 4; ++i) {
                        int nx = x + dx[i];
                        int ny = y + dy[i];
                        if (nx >= 0 && nx < 8 && ny >= 0 && ny < 8
                            && mask[nx][ny] && this.eyeGroups[nx][ny] == 0) {
                            this.eyeGroups[nx][ny] = (byte)group;
                            queueX[last] = nx;
                            queueY[last++] = ny;
                        }
                    }
                }
            }
        }
    }

    private void loadExistingData() {
        BlinkSkinExporter.EditorData data = ExpressionSkinExporter.readEditorData(this.skin);
        if (data == null) data = BlinkSkinExporter.readEditorData(this.skin);
        if (data == null) return;
        for (int x = 0; x < 8; ++x) {
            for (int y = 0; y < 8; ++y) {
                this.eyeGroups[x][y] = data.eyes[x][y] ? (byte)1 : 0;
                this.pupils[x][y] = data.pupils[x][y];
                this.closedColors[x][y] = data.closedColors[x][y];
                this.pupilBackgroundColors[x][y] = data.pupilBackgroundColors[x][y];
                this.eyebrowGroups[x][y] = data.eyebrowGroups[x][y];
                this.eyebrowColors[x][y] = data.eyebrowColors[x][y];
            }
        }
        inferEyeComponents();
        this.replaceArmed = true;
        this.statusSuccess = false;
        this.status = data.customExpressions
            ? I18n.format("mobends.blink.loaded")
            : I18n.format("mobends.blink.recovered");
    }

    private void updateExportButton() {
        GuiButton button = getButton(6);
        if (button != null) {
            button.enabled = this.skin != null && hasSelection() && !this.uploading;
        }
        GuiButton upload = getButton(14);
        if (upload != null) {
            upload.enabled = this.skin != null && hasSelection() && !this.uploading;
        }
    }

    private GuiButton getButton(int id) {
        for (Object object : this.buttonList) {
            GuiButton button = (GuiButton)object;
            if (button.id == id) {
                return button;
            }
        }
        return null;
    }

    private int faceX(int mouseX) {
        int relative = mouseX - canvasX();
        return relative >= 0 && relative < CANVAS_SIZE ? relative / PIXEL_SIZE : -1;
    }

    private int faceY(int mouseY) {
        int relative = mouseY - canvasY();
        return relative >= 0 && relative < CANVAS_SIZE ? relative / PIXEL_SIZE : -1;
    }

    private int canvasX() {
        return this.width / 2 - 185;
    }

    private int canvasY() {
        return Math.max(30, this.height / 2 - 85);
    }

    private int controlsX() {
        return this.width / 2 + 5;
    }

    private int controlsY() {
        return Math.max(34, this.height / 2 - 105);
    }

    private int pickerX() {
        return controlsX();
    }

    private int pickerY() {
        return controlsY() + 48;
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
