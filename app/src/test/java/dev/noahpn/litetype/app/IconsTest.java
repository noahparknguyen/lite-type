package dev.noahpn.litetype.app;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class IconsTest {

    @Test
    void everyWindowIconIsThereAtItsSize() throws IOException {
        for (int size : LiteTypeApp.ICON_SIZES) {
            URL url = LiteTypeApp.class.getResource(LiteTypeApp.iconPath(size));
            assertNotNull(url, "missing " + LiteTypeApp.iconPath(size));
            BufferedImage icon = ImageIO.read(url);
            assertEquals(size, icon.getWidth());
            assertEquals(size, icon.getHeight());
        }
    }

    @Test
    void theLinuxInstallerHasItsIcon() throws IOException {
        // Tests run from the module's folder, which is where jpackage will look.
        BufferedImage icon = ImageIO.read(Path.of("packaging/linux/lite-type.png").toFile());
        assertEquals(256, icon.getWidth());
        assertEquals(256, icon.getHeight());
    }

    @Test
    void theWindowsIconHoldsBitmapsUpTo128AndAPngAt256() throws IOException {
        byte[] file = Files.readAllBytes(Path.of("packaging/windows/lite-type.ico"));
        ByteBuffer ico = ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0, ico.getShort(0));
        assertEquals(1, ico.getShort(2), "not an icon file");
        int count = ico.getShort(4);
        int[] sizes = {16, 24, 32, 48, 64, 128, 256};
        assertEquals(sizes.length, count);

        for (int i = 0; i < count; i++) {
            int entry = 6 + 16 * i;
            // A stored width of 0 means 256, which doesn't fit in a byte.
            int width = ico.get(entry) == 0 ? 256 : ico.get(entry) & 0xff;
            assertEquals(sizes[i], width);

            int offset = ico.getInt(entry + 12);
            boolean png = ico.getInt(offset) == 0x474e5089;
            // .NET's System.Drawing can't read a PNG entry smaller than 256, so those are bitmaps.
            assertEquals(width == 256, png, width + " px stored the wrong way");
        }
    }
}
