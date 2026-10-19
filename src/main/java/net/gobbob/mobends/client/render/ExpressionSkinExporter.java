package net.gobbob.mobends.client.render;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import industries.angelic.salamander.cem.client.CemSkinImage;

/** Shared Just Expressions 1.2.1 / Fresh Moves 3.1.1 skin layout. No pack assets or legacy formats. */
public final class ExpressionSkinExporter {
    public static final class EditorData {
        public final byte[][] eyes = new byte[8][8], brows = new byte[8][8], overlayBrows = new byte[8][8];
        public final boolean[][] pupils = new boolean[8][8];
        public final int[][] lids = new int[8][8], whites = new int[8][8], pupilColors = new int[8][8];
        public final int[][] browColors = new int[8][8], browBackgrounds = new int[8][8];
        public final int[][] overlayColors = new int[8][8], lashes = new int[8][8];

        /** Brows are optional. Remove both layers together so no hidden companion brow survives. */
        public void clearBrows() {
            for (int x = 0; x < 8; x++) {
                java.util.Arrays.fill(brows[x], (byte) 0);
                java.util.Arrays.fill(overlayBrows[x], (byte) 0);
                java.util.Arrays.fill(browColors[x], 0);
                java.util.Arrays.fill(browBackgrounds[x], 0);
                java.util.Arrays.fill(overlayColors[x], 0);
            }
        }

        public EditorData copy() {
            EditorData result = new EditorData();
            result.copyFrom(this);
            return result;
        }

        public void copyFrom(EditorData source) {
            for (int x = 0; x < 8; x++) {
                System.arraycopy(source.eyes[x], 0, eyes[x], 0, 8);
                System.arraycopy(source.brows[x], 0, brows[x], 0, 8);
                System.arraycopy(source.overlayBrows[x], 0, overlayBrows[x], 0, 8);
                System.arraycopy(source.pupils[x], 0, pupils[x], 0, 8);
                System.arraycopy(source.lids[x], 0, lids[x], 0, 8);
                System.arraycopy(source.whites[x], 0, whites[x], 0, 8);
                System.arraycopy(source.pupilColors[x], 0, pupilColors[x], 0, 8);
                System.arraycopy(source.browColors[x], 0, browColors[x], 0, 8);
                System.arraycopy(source.browBackgrounds[x], 0, browBackgrounds[x], 0, 8);
                System.arraycopy(source.overlayColors[x], 0, overlayColors[x], 0, 8);
                System.arraycopy(source.lashes[x], 0, lashes[x], 0, 8);
            }
        }
    }

    public enum EyeType {
        NORMAL_2X1(1, 2, 1, 3, 6),
        NORMAL_2X2(1, 2, 2, 2, 5),
        NORMAL_2X3(1, 2, 3, 4, 4),
        DERP(1, 2, 1, 3, 6),
        ENDERMAN(0, 3, 1, 4, 5),
        WIDE(0, 2, 1, 5, 5),
        SIMPLE_1X1(2, 1, 1, 3, 4),
        SIMPLE_WIDE(1, 1, 1, 3, 4),
        SIMPLE_1X2(2, 1, 2, 3, 4),
        SIMPLE_1X3(2, 1, 3, 2, 3);
        public final int x, width, height, minRow, maxRow;
        EyeType(int x, int width, int height, int minRow, int maxRow) {
            this.x = x;
            this.width = width;
            this.height = height;
            this.minRow = minRow;
            this.maxRow = maxRow;
        }
    }

    private static final class Layout {
        final int x, y, width, height, u, v;
        Layout(int x, int y, int width, int height, int u, int v) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.u = u;
            this.v = v;
        }
        int faceX(int side) {
            return side == 0 ? x : 8 - x - width;
        }
        int skinU(int side) {
            return u + side * (width == 3 ? 5 : width);
        }
        int pupilX(int side, boolean derp) {
            return faceX(side) + (width == 3 ? 1 : derp ? (side == 0 ? 0 : 1) : (side == 0 ? 1 : 0));
        }
        int pupilU(int side, boolean derp) {
            return width == 3 ? 35 + side : derp ? 2 + side : u + side * 2 + (side == 0 ? 1 : 0);
        }
        int whiteU(int side) {
            return width == 3 ? skinU(side) : u + side * 2 + (side == 0 ? 0 : 1);
        }
    }
    private static final List<Layout> EYES = new ArrayList<>();
    static {
        for (int row = 0; row < 4; row++) EYES.add(new Layout(1, 6 - row, 2, 1, 4, 6 - row * 2));
        for (int row = 0; row < 4; row++)
            EYES.add(new Layout(1, 5 - row, 2, 2, row < 2 ? 24 : 28, row % 2 == 0 ? 5 : 2));
        EYES.add(new Layout(1, 4, 2, 3, 32, 4));
        EYES.add(new Layout(0, 5, 3, 1, 32, 2));
        EYES.add(new Layout(0, 4, 3, 1, 32, 0));
        EYES.add(new Layout(0, 5, 2, 1, 36, 6));
        EYES.add(new Layout(1, 4, 1, 1, 0, 7));
        EYES.add(new Layout(1, 3, 1, 1, 0, 6));
        EYES.add(new Layout(2, 4, 1, 1, 0, 5));
        EYES.add(new Layout(2, 3, 1, 1, 0, 4));
        EYES.add(new Layout(2, 4, 1, 2, 0, 3));
        EYES.add(new Layout(2, 3, 1, 2, 0, 2));
        EYES.add(new Layout(2, 3, 1, 3, 0, 1));
        EYES.add(new Layout(2, 2, 1, 3, 0, 0));
    }
    private ExpressionSkinExporter() {}

    public static EyeType eyeType(EditorData data, int side) {
        Layout eye = find(data.eyes, side);
        if (eye == null)
            return null;
        if (eye.width == 2 && eye.height == 1 && eye.x == 1
            && data.pupils[eye.faceX(side) + (side == 0 ? 0 : 1)][eye.y])
            return EyeType.DERP;
        for (EyeType type : EyeType.values()) {
            if (type.x == eye.x && type.width == eye.width && type.height == eye.height)
                return type;
        }
        return null;
    }

    public static int eyeRow(EditorData data, int side) {
        Layout eye = find(data.eyes, side);
        return eye == null ? 4 : eye.y;
    }

    public static void eyePreset(EditorData data, BufferedImage skin, EyeType type, int row, int side) {
        row = Math.max(type.minRow, Math.min(type.maxRow, row));
        for (int x = side * 4; x < side * 4 + 4; x++)
            for (int y = 0; y < 8; y++) {
                data.eyes[x][y] = 0;
                data.pupils[x][y] = false;
            }
        int start = side == 0 ? type.x : 8 - type.x - type.width;
        int pupil = start + (type.width == 3 ? 1 : type == EyeType.DERP ? (side == 0 ? 0 : 1) : (side == 0 ? 1 : 0));
        int white =
            type == EyeType.DERP ? (side == 0 ? start + 1 : start) : (side == 0 ? start : start + type.width - 1);
        for (int dx = 0; dx < type.width; dx++)
            for (int dy = 0; dy < type.height; dy++) {
                int x = start + dx, y = row + dy;
                data.eyes[x][y] = (byte) (side + 1);
                data.pupils[x][y] = type.width > 1 && x == pupil && dy < Math.min(type.height, 2);
                data.lids[x][y] = base(skin, x, row - 1);
                data.pupilColors[x][y] = base(skin, x, y);
                data.whites[x][y] = base(skin, type.width == 3 ? (x == pupil ? start : x) : white, y);
            }
    }

    public static BufferedImage export(BufferedImage source, EditorData data) {
        if (source == null || source.getWidth() != 64 || source.getHeight() != 32 && source.getHeight() != 64)
            throw new IllegalArgumentException("Use a 64x64 or 64x32 skin for the expression editor.");
        BufferedImage result = CemSkinImage.normalize(source);
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 64; x++)
                if (reserved(x))
                    result.setRGB(x, y, 0);
        for (int side = 0; side < 2; side++) writeEye(result, source, data, side);
        writeBrows(result, data);
        writeOverlays(result, data);
        return result;
    }

    private static void writeEye(BufferedImage out, BufferedImage source, EditorData data, int side) {
        Layout eye = find(data.eyes, side);
        if (eye == null)
            return;
        int x = eye.faceX(side), u = eye.skinU(side);
        boolean derp = eye.width == 2 && eye.height == 1 && eye.x == 1 && data.pupils[x + (side == 0 ? 0 : 1)][eye.y];
        int pupil = eye.pupilX(side, derp);
        for (int dx = 0; dx < eye.width; dx++) {
            int lid = opaque(data.lids[x + dx][eye.y]);
            for (int dy = 0; dy < eye.height; dy++) {
                int y = eye.y + dy;
                if (opaque(data.lids[x + dx][y]) != lid)
                    throw new IllegalArgumentException(
                        "Each eyelid column needs one color. Paint a column in the Eyelids step.");
                if ((source.getRGB(40 + x + dx, 8 + y) >>> 24) != 0)
                    throw new IllegalArgumentException(
                        "Move eyes to the base face; these packs only support eyebrows on the outer layer.");
                boolean expected = eye.width > 1 && x + dx == pupil && dy < Math.min(eye.height, 2);
                if (data.pupils[x + dx][y] != expected)
                    throw new IllegalArgumentException(
                        "The pupil shape does not match this eye type. Apply an eye preset first.");
            }
            out.setRGB(u + dx, eye.v, lid);
        }
        if (eye.width == 1)
            return;
        for (int dy = 0; dy < eye.height; dy++) {
            int y = eye.y + dy;
            for (int dx = 0; dx < (eye.width == 3 ? 3 : 1); dx++) {
                int white = opaque(data.whites[x + dx][y]);
                if (eye.width == 2 && white != opaque(data.whites[x + 1][y]))
                    throw new IllegalArgumentException("Two-pixel-wide eyes use one eye-white color per row.");
                out.setRGB(eye.whiteU(side) + dx, eye.v + 1 + dy, white);
            }
            if (dy < Math.min(eye.height, 2))
                out.setRGB(eye.pupilU(side, derp), eye.v + 1 + dy, data.pupilColors[pupil][y]);
        }
    }

    private static Layout find(byte[][] mask, int side) {
        int count = 0;
        for (int x = side * 4; x < side * 4 + 4; x++)
            for (int y = 0; y < 8; y++)
                if (mask[x][y] != 0)
                    count++;
        if (count == 0)
            return null;
        for (Layout eye : EYES) {
            if (count != eye.width * eye.height)
                continue;
            int x = eye.faceX(side);
            boolean matches = true;
            for (int dx = 0; dx < eye.width; dx++)
                for (int dy = 0; dy < eye.height; dy++)
                    if (mask[x + dx][eye.y + dy] == 0)
                        matches = false;
            if (matches)
                return eye;
        }
        throw new IllegalArgumentException(
            "Unsupported eye position. Choose an eye type and move it with the preset controls.");
    }

    private static void writeBrows(BufferedImage out, EditorData data) {
        boolean joined = true;
        int total = 0;
        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++)
                if (data.brows[x][y] != 0) {
                    total++;
                    joined &= x >= 1 && x <= 6 && y == 3;
                }
        if (total == 6 && joined) {
            if (data.browColors[3][3] != data.browColors[4][3]
                || data.browBackgrounds[3][3] != data.browBackgrounds[4][3])
                throw new IllegalArgumentException(
                    "The joined brow shares one center texel. Paint its two center pixels the same color.");
            int[] columns = {1, 2, 3, 5, 6};
            for (int i = 0; i < columns.length; i++) {
                out.setRGB(24 + i, 0, opaque(data.browBackgrounds[columns[i]][3]));
                out.setRGB(24 + i, 1, data.browColors[columns[i]][3]);
            }
            return;
        }
        for (int side = 0; side < 2; side++) {
            int count = 0, row = -1;
            for (int x = side * 4; x < side * 4 + 4; x++)
                for (int y = 0; y < 8; y++)
                    if (data.brows[x][y] != 0) {
                        count++;
                        row = y;
                    }
            if (count == 0)
                continue;
            int x = side == 0 ? 1 : 5, u = 60 + side * 2, v = (row - 2) * 2;
            if (row == 3 && data.brows[side == 0 ? 0 : 7][3] != 0) {
                x = side == 0 ? 0 : 6;
                u = 36 + side * 2;
                v = 4;
            }
            if (count != 2 || row < 2 || row > 5 || data.brows[x][row] == 0 || data.brows[x + 1][row] == 0)
                throw new IllegalArgumentException(
                    "Choose paired, wide-set or joined eyebrows and a supported height.");
            for (int dx = 0; dx < 2; dx++) {
                out.setRGB(u + dx, v, opaque(data.browBackgrounds[x + dx][row]));
                out.setRGB(u + dx, v + 1, data.browColors[x + dx][row]);
            }
        }
    }

    private static void writeOverlays(BufferedImage out, EditorData data) {
        for (int side = 0; side < 2; side++) {
            int count = 0, row = -1, x = side == 0 ? 1 : 5;
            for (int xx = side * 4; xx < side * 4 + 4; xx++)
                for (int y = 0; y < 8; y++)
                    if (data.overlayBrows[xx][y] != 0) {
                        count++;
                        row = y;
                    }
            if (count != 0) {
                if (count != 2 || row < 2 || row > 5 || data.overlayBrows[x][row] == 0
                    || data.overlayBrows[x + 1][row] == 0)
                    throw new IllegalArgumentException("Overlay eyebrows need a two-pixel pair at a supported height.");
                for (int dx = 0; dx < 2; dx++) out.setRGB(56 + side * 2 + dx, row + 2, data.overlayColors[x + dx][row]);
            }
            Layout eye = find(data.eyes, side);
            for (int xx = side * 4; xx < side * 4 + 4; xx++)
                for (int y = 0; y < 8; y++)
                    if ((data.lashes[xx][y] >>> 24) != 0) {
                        boolean top = (xx == x || xx == x + 1) && y == 4;
                        boolean edge = xx == (side == 0 ? 0 : 7) && (y == 5 || y == 6);
                        if ((!top && !edge) || eye == null || eye.width != 2 || eye.height != 2 || eye.y != 5)
                            throw new IllegalArgumentException("Eyelashes need bottom 2x2 eyes: paint the two pixels "
                                + "above the eye and the two along its outer edge.");
                        out.setRGB(
                            top ? 56 + side * 2 + xx - x : (side == 0 ? 56 : 59), top ? 0 : y - 4, data.lashes[xx][y]);
                    }
        }
    }

    /** Only accepts an unambiguous current pack layout; unrelated corner pixels are never imported. */
    public static EditorData readEditorData(BufferedImage skin) {
        if (skin == null || skin.getWidth() != 64 || skin.getHeight() != 32 && skin.getHeight() != 64)
            return null;
        EditorData data = new EditorData();
        boolean any = false;
        for (int side = 0; side < 2; side++) {
            Layout found = null;
            for (Layout eye : EYES) {
                int u = eye.skinU(side);
                if (!filled(skin, u, eye.v, eye.width, 1))
                    continue;
                if (eye.width > 1 && !filled(skin, eye.whiteU(side), eye.v + 1, eye.width == 3 ? 3 : 1, eye.height))
                    continue;
                if (found != null)
                    return null;
                found = eye;
            }
            if (found != null) {
                any = true;
                Layout eye = found;
                int x = eye.faceX(side), u = eye.skinU(side);
                boolean derp =
                    eye.width == 2 && eye.height == 1 && eye.x == 1 && (skin.getRGB(2 + side, eye.v + 1) >>> 24) != 0;
                int pupil = eye.pupilX(side, derp);
                for (int dx = 0; dx < eye.width; dx++)
                    for (int dy = 0; dy < eye.height; dy++) {
                        int xx = x + dx, y = eye.y + dy;
                        data.eyes[xx][y] = (byte) (side + 1);
                        data.lids[xx][y] = skin.getRGB(u + dx, eye.v);
                        data.pupils[xx][y] = eye.width > 1 && xx == pupil && dy < Math.min(eye.height, 2);
                        if (eye.width > 1)
                            data.whites[xx][y] =
                                skin.getRGB(eye.whiteU(side) + (eye.width == 3 ? dx : 0), eye.v + 1 + dy);
                        if (data.pupils[xx][y])
                            data.pupilColors[xx][y] = skin.getRGB(eye.pupilU(side, derp), eye.v + 1 + dy);
                    }
            }
            int count = 0;
            for (int row = 2; row <= 5; row++) {
                int u = 60 + side * 2, v = (row - 2) * 2;
                if (!filled(skin, u, v, 2, 1))
                    continue;
                if (++count > 1)
                    return null;
                readBrow(skin, data, side == 0 ? 1 : 5, row, u, v, side);
                any = true;
            }
            if (filled(skin, 36 + side * 2, 4, 2, 1)) {
                if (count != 0)
                    return null;
                readBrow(skin, data, side == 0 ? 0 : 6, 3, 36 + side * 2, 4, side);
                any = true;
            }
            int overlays = 0;
            for (int row = 2; row <= 5; row++) {
                int u = 56 + side * 2, v = row + 2;
                if (!painted(skin, u, v, 2, 1))
                    continue;
                if (++overlays > 1)
                    return null;
                for (int dx = 0; dx < 2; dx++) {
                    int x = (side == 0 ? 1 : 5) + dx;
                    data.overlayBrows[x][row] = (byte) (side + 1);
                    data.overlayColors[x][row] = skin.getRGB(u + dx, v);
                }
                any = true;
            }
            for (int dx = 0; dx < 2; dx++) {
                data.lashes[(side == 0 ? 1 : 5) + dx][4] = skin.getRGB(56 + side * 2 + dx, 0);
                data.lashes[side == 0 ? 0 : 7][5 + dx] = skin.getRGB(side == 0 ? 56 : 59, 1 + dx);
                any |= (data.lashes[(side == 0 ? 1 : 5) + dx][4] >>> 24) != 0
                    || (data.lashes[side == 0 ? 0 : 7][5 + dx] >>> 24) != 0;
            }
        }
        if (filled(skin, 24, 0, 5, 1)) {
            for (int x = 0; x < 8; x++)
                for (int y = 0; y < 8; y++)
                    if (data.brows[x][y] != 0)
                        return null;
            int[] texels = {0, 1, 2, 2, 3, 4};
            for (int x = 1; x <= 6; x++) {
                data.brows[x][3] = 1;
                data.browBackgrounds[x][3] = skin.getRGB(24 + texels[x - 1], 0);
                data.browColors[x][3] = skin.getRGB(24 + texels[x - 1], 1);
            }
            any = true;
        }
        if (!any)
            return null;
        try {
            BufferedImage canonical = export(skin, data);
            for (int x = 0; x < 64; x++)
                if (reserved(x))
                    for (int y = 0; y < 8; y++) {
                        int a = skin.getRGB(x, y), b = canonical.getRGB(x, y);
                        if ((a >>> 24) != 0 || (b >>> 24) != 0)
                            if (a != b)
                                return null;
                    }
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return data;
    }

    private static void readBrow(BufferedImage skin, EditorData data, int x, int row, int u, int v, int side) {
        for (int dx = 0; dx < 2; dx++) {
            data.brows[x + dx][row] = (byte) (side + 1);
            data.browBackgrounds[x + dx][row] = skin.getRGB(u + dx, v);
            data.browColors[x + dx][row] = skin.getRGB(u + dx, v + 1);
        }
    }
    private static boolean filled(BufferedImage image, int x, int y, int width, int height) {
        for (int dx = 0; dx < width; dx++)
            for (int dy = 0; dy < height; dy++)
                if ((image.getRGB(x + dx, y + dy) >>> 24) != 255)
                    return false;
        return true;
    }
    private static boolean painted(BufferedImage image, int x, int y, int width, int height) {
        for (int dx = 0; dx < width; dx++)
            for (int dy = 0; dy < height; dy++)
                if ((image.getRGB(x + dx, y + dy) >>> 24) != 0)
                    return true;
        return false;
    }
    public static int base(BufferedImage skin, int x, int y) {
        return skin == null ? 0xffaaaaaa : opaque(skin.getRGB(8 + x, 8 + y));
    }
    private static boolean reserved(int x) {
        return x < 8 || x >= 24 && x < 40 || x >= 56;
    }
    private static int opaque(int color) {
        return 0xff000000 | color & 0xffffff;
    }
}
