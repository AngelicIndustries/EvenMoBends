package net.gobbob.mobends.client.render;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class ExpressionSkinExporterTest {
    private final BufferedImage skin = new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
    private final byte[][] eyes = new byte[8][8], brows = new byte[8][8];
    private final boolean[][] pupils = new boolean[8][8];
    private final int[][] lids = new int[8][8], backgrounds = new int[8][8], browColors = new int[8][8];

    private BufferedImage export() { return ExpressionSkinExporter.export(skin,eyes,pupils,lids,backgrounds,brows,browColors); }

    private void select(int height,int row) {
        for (int x=0;x<8;x++) for (int y=0;y<8;y++) { eyes[x][y]=0; pupils[x][y]=false; }
        for (int x : new int[] {1,2,5,6}) for (int y=row;y<row+height;y++) {
            eyes[x][y]=1;
            pupils[x][y]=(x==2 || x==5) && y<row+Math.min(height,2);
            skin.setRGB(8+x,8+y,pupils[x][y] ? (x==2 ? 0xff00aaff : 0xffaaff00) : 0xffeeeeee);
            backgrounds[x][y]=0xffeeeeee;
            lids[x][y]=0xffaabbcc+x;
        }
    }

    @Test
    void standardEyesUseTheDocumentedSharedSlotsAndRoundTrip() {
        int[][] layouts={{1,6,4,6},{1,5,4,4},{1,4,4,2},{1,3,4,0},
            {2,5,24,5},{2,4,24,2},{2,3,28,5},{2,2,28,2},{3,4,32,4}};
        for (int[] layout : layouts) {
            select(layout[0],layout[1]);
            BufferedImage out=export(); int u=layout[2],v=layout[3];
            assertEquals(lids[1][layout[1]],out.getRGB(u,v));
            assertEquals(lids[6][layout[1]],out.getRGB(u+3,v));
            assertEquals(0xffeeeeee,out.getRGB(u,v+1));
            assertEquals(0xff00aaff,out.getRGB(u+1,v+1));
            assertEquals(0xffaaff00,out.getRGB(u+2,v+1));
            assertEquals(0xffeeeeee,out.getRGB(u+3,v+1));
            BlinkSkinExporter.EditorData data=ExpressionSkinExporter.readEditorData(out);
            assertNotNull(data);
            for (int x=0;x<8;x++) for (int y=0;y<8;y++) {
                assertEquals(eyes[x][y]!=0,data.eyes[x][y]);
                assertEquals(pupils[x][y],data.pupils[x][y]);
                if (data.eyes[x][y]) assertEquals(lids[x][y],data.closedColors[x][y]);
            }
        }
    }

    @Test
    void browsPreserveTheirMaskAndColors() {
        select(1,6);
        for (int x : new int[] {1,2,5,6}) { brows[x][5]=1; browColors[x][5]=0xff765400+x; lids[x][5]=0xffaabb00+x; }
        BufferedImage out=export();
        assertEquals(lids[1][5],out.getRGB(60,6));
        assertEquals(browColors[1][5],out.getRGB(60,7));
        BlinkSkinExporter.EditorData data=ExpressionSkinExporter.readEditorData(out);
        assertNotNull(data);
        for (int x : new int[] {1,2,5,6}) {
            assertNotEquals(0,data.eyebrowGroups[x][5]);
            assertEquals(lids[x][5],data.closedColors[x][5]);
            assertEquals(browColors[x][5],data.eyebrowColors[x][5]);
        }
    }

    @Test
    void visibleSkinIsUntouchedAndStaleUnusedSlotsAreCleared() {
        for (int x=0;x<64;x++) for (int y=0;y<64;y++) skin.setRGB(x,y,0xff000000|x*255+y);
        for (int x=40;x<48;x++) for (int y=8;y<16;y++) skin.setRGB(x,y,0);
        select(1,6);
        BufferedImage out=export();
        for (int x=0;x<64;x++) for (int y=0;y<64;y++) {
            if (y<8 && (x<8 || x>=24 && x<40 || x>=56)) continue;
            assertEquals(skin.getRGB(x,y),out.getRGB(x,y),x+","+y);
        }
        assertEquals(0,out.getRGB(24,5));
        assertEquals(0,out.getRGB(60,7));
        assertNotNull(ExpressionSkinExporter.readEditorData(out));
    }

    @Test
    void refusesLossySelectionsInsteadOfInventingAWorkingPackSkin() {
        select(1,6); eyes[3][6]=1;
        assertThrows(IllegalArgumentException.class,this::export);
        eyes[3][6]=0; backgrounds[2][6]=0xff123456;
        assertThrows(IllegalArgumentException.class,this::export);
        backgrounds[2][6]=0xffeeeeee; skin.setRGB(41,14,0xff123456);
        assertThrows(IllegalArgumentException.class,this::export);
        assertThrows(IllegalArgumentException.class,()->ExpressionSkinExporter.export(new BufferedImage(128,128,2),eyes,pupils,lids,backgrounds,brows,browColors));
    }

    @Test
    void simpleEyesOnlyWriteEyelidsAndOldDataCanBeMigrated() {
        eyes[2][4]=eyes[5][4]=1; lids[2][4]=lids[5][4]=0xffabcdef;
        BufferedImage out=export();
        assertEquals(0xffabcdef,out.getRGB(0,5));
        assertEquals(0xffabcdef,out.getRGB(1,5));
        assertTrue(ExpressionSkinExporter.readEditorData(out).eyes[2][4]);
        BufferedImage old=BlinkSkinExporter.export(skin,eyes,pupils,lids,backgrounds,brows,browColors);
        assertNotNull(BlinkSkinExporter.readEditorData(old));
        BufferedImage migrated=ExpressionSkinExporter.export(old,eyes,pupils,lids,backgrounds,brows,browColors);
        assertFalse(BlinkSkinExporter.hasBlinkData(migrated));
        assertNotNull(ExpressionSkinExporter.readEditorData(migrated));
    }
}
