package net.gobbob.mobends.client.render;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.fentanylsolutions.salamander.cem.client.CemSkinImage;

/** Shared Just Expressions 1.2.1 / Fresh Moves expressive skin layout (64px). No pack assets are bundled. */
public final class ExpressionSkinExporter {
    /** Face rectangle and its reserved skin strip. Simple eyes only need a stationary eyelid color. */
    private static final class Layout {
        final int x, y, width, height, u, v;
        Layout(int x, int y, int width, int height, int u, int v) {
            this.x=x; this.y=y; this.width=width; this.height=height; this.u=u; this.v=v;
        }
    }
    private static final List<Layout> EYES = new ArrayList<>();
    static {
        for (int row=0; row<4; row++) EYES.add(new Layout(1, 6-row, 2, 1, 4, 6-row*2));
        for (int row=0; row<4; row++) EYES.add(new Layout(1, 5-row, 2, 2, row<2 ? 24 : 28, row%2==0 ? 5 : 2));
        EYES.add(new Layout(1, 4, 2, 3, 32, 4));
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

    public static BufferedImage export(BufferedImage source, byte[][] eyes, boolean[][] pupils, int[][] lids,
        int[][] backgrounds, byte[][] brows, int[][] browColors) {
        if (source == null || source.getWidth()!=64 || source.getHeight()!=32 && source.getHeight()!=64)
            throw new IllegalArgumentException("Use a 64x64 or 64x32 skin for the expression editor.");
        BufferedImage result = CemSkinImage.normalize(source);
        // These are unused by the normal skin. Clear every supported pack slot, including previous formats.
        for (int y=0; y<8; y++) for (int x=0; x<64; x++) if (x<8 || x>=24 && x<40 || x>=56) result.setRGB(x,y,0);
        if (BlinkSkinExporter.hasBlinkData(source)) {
            for (int x=0; x<4; x++) for (int y=16; y<20; y++) result.setRGB(x,y,0);
            result.setRGB(52,16,0); result.setRGB(52,19,0);
        }
        for (int side=0; side<2; side++) {
            Layout eye = find(eyes, side);
            if (eye == null) continue;
            int x = side==0 ? eye.x : 8-eye.x-eye.width;
            int u = eye.u + side*eye.width;
            for (int dx=0; dx<eye.width; dx++) {
                int lid = opaque(lids[x+dx][eye.y]);
                for (int dy=0; dy<eye.height; dy++) {
                    if (opaque(lids[x+dx][eye.y+dy]) != lid)
                        throw new IllegalArgumentException("These packs use one eyelid color per column. Paint each column the same color.");
                    if ((source.getRGB(40+x+dx,8+eye.y+dy)>>>24)!=0)
                        throw new IllegalArgumentException("Eyes on the hat layer are not supported by this editor. Use base-face eyes.");
                }
                result.setRGB(u+dx,eye.v,lid);
            }
            if (eye.width == 1) {
                for (int dy=0; dy<eye.height; dy++) if (pupils[x][eye.y+dy])
                    throw new IllegalArgumentException("Simple 1-pixel-wide eyes blink without a moving pupil; clear their pupil selection.");
                continue;
            }
            int pupilX = side==0 ? x+1 : x;
            int whiteX = side==0 ? x : x+1;
            for (int dy=0; dy<eye.height; dy++) {
                int y = eye.y+dy;
                boolean pupil = dy<Math.min(eye.height,2);
                if (pupils[pupilX][y]!=pupil || pupils[whiteX][y])
                    throw new IllegalArgumentException("Mark the inner column as the pupil (top two pixels for 2x3 eyes). Other pupil layouts need manual pack preparation.");
                int white = opaque(source.getRGB(8+whiteX,8+y));
                if (pupil && opaque(backgrounds[pupilX][y])!=white)
                    throw new IllegalArgumentException("Set each pupil background to the neighboring eye-white color; the pack stretches that color across the eye.");
                result.setRGB(eye.u+side*2+(side==0 ? 0 : 1),eye.v+1+dy,white);
                // In the 2x3 format the third pupil texel is unused, so keep it transparent.
                if (pupil) result.setRGB(eye.u+side*2+(side==0 ? 1 : 0),eye.v+1+dy,
                    opaque(source.getRGB(8+pupilX,8+y)));
            }
        }
        writeBrows(result, source, brows, browColors, lids);
        return result;
    }

    private static Layout find(byte[][] mask, int side) {
        int count=0;
        for (int x=side*4; x<side*4+4; x++) for (int y=0; y<8; y++) if (mask[x][y]!=0) count++;
        if (count==0) return null;
        for (Layout eye : EYES) {
            int x = side==0 ? eye.x : 8-eye.x-eye.width;
            if (count!=eye.width*eye.height) continue;
            boolean matches=true;
            for (int dx=0; dx<eye.width; dx++) for (int dy=0; dy<eye.height; dy++) if (mask[x+dx][eye.y+dy]==0) matches=false;
            if (matches) return eye;
        }
        throw new IllegalArgumentException("Unsupported eye shape/position. Use the presets, then move them to match your face; see the pack's skin guide for other layouts.");
    }

    private static void writeBrows(BufferedImage result, BufferedImage source, byte[][] mask, int[][] colors, int[][] backgrounds) {
        for (int side=0; side<2; side++) {
            int count=0, row=-1, x=side==0 ? 1 : 5;
            for (int xx=side*4; xx<side*4+4; xx++) for (int y=0; y<8; y++) if (mask[xx][y]!=0) { count++; row=y; }
            if (count==0) continue;
            if (count!=2 || row<2 || row>5 || mask[x][row]==0 || mask[x+1][row]==0)
                throw new IllegalArgumentException("Each eyebrow must be two pixels wide at columns 2-3 or 6-7, in face rows 3-6.");
            int v=(row-2)*2, u=60+side*2;
            for (int dx=0; dx<2; dx++) {
                if ((source.getRGB(40+x+dx,8+row)>>>24)!=0)
                    throw new IllegalArgumentException("Use base-face eyebrows; hat-layer eyebrows need manual pack preparation.");
                result.setRGB(u+dx,v,opaque(backgrounds[x+dx][row]));
                result.setRGB(u+dx,v+1,opaque(colors[x+dx][row]));
            }
        }
    }

    /** Import the same layout for subsequent edits. Ambiguous unprepared corners are not treated as metadata. */
    public static BlinkSkinExporter.EditorData readEditorData(BufferedImage skin) {
        if (skin==null || skin.getWidth()!=64 || skin.getHeight()<32 || BlinkSkinExporter.hasBlinkData(skin)) return null;
        BlinkSkinExporter.EditorData data = new BlinkSkinExporter.EditorData();
        int eyes=0;
        for (int side=0; side<2; side++) {
            int found=0;
            for (Layout eye : EYES) {
                int u=eye.u+side*eye.width;
                if (!filled(skin,u,eye.v,eye.width,eye.width==1 ? 1 : 2)) continue;
                if (++found>1) return null;
                eyes++;
                int x=side==0 ? eye.x : 8-eye.x-eye.width;
                for (int dx=0; dx<eye.width; dx++) for (int dy=0; dy<eye.height; dy++) {
                    data.eyes[x+dx][eye.y+dy]=true;
                    data.closedColors[x+dx][eye.y+dy]=skin.getRGB(u+dx,eye.v);
                    boolean pupil=eye.width==2 && dx==(side==0 ? 1 : 0) && dy<Math.min(2,eye.height);
                    data.pupils[x+dx][eye.y+dy]=pupil;
                    if (pupil) data.pupilBackgroundColors[x+dx][eye.y+dy]=skin.getRGB(eye.u+side*2+(side==0 ? 0 : 1),eye.v+1+dy);
                }
            }
            int browCount=0;
            for (int row=2; row<=5; row++) {
                int u=60+side*2,v=(row-2)*2;
                if (!filled(skin,u,v,2,2)) continue;
                if (++browCount>1) return null;
                for (int dx=0; dx<2; dx++) {
                    int x=(side==0 ? 1 : 5)+dx;
                    data.eyebrowGroups[x][row]=(byte)(side+1);
                    data.eyebrowColors[x][row]=skin.getRGB(u+dx,v+1);
                    data.closedColors[x][row]=skin.getRGB(u+dx,v);
                }
            }
        }
        data.customExpressions=true;
        return eyes==0 ? null : data;
    }

    private static boolean filled(BufferedImage image,int x,int y,int width,int height) {
        for (int dx=0; dx<width; dx++) for (int dy=0; dy<height; dy++) if ((image.getRGB(x+dx,y+dy)>>>24)<128) return false;
        return true;
    }

    private static int opaque(int color) { return 0xff000000 | color & 0xffffff; }
}
