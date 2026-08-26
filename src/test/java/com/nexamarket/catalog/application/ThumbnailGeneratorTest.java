package com.nexamarket.catalog.application;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThumbnailGeneratorTest {

    private final ThumbnailGenerator generator = new ThumbnailGenerator();

    @Test
    void createsAspectRatioPreservingJpeg() throws Exception {
        byte[] thumbnail = generator.generateJpeg(createPng(640, 320));

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(thumbnail));
        assertThat(result.getWidth()).isEqualTo(320);
        assertThat(result.getHeight()).isEqualTo(160);
    }

    @Test
    void rejectsNonImageContent() {
        assertThatThrownBy(() -> generator.generateJpeg("not-an-image".getBytes()))
                .isInstanceOf(InvalidProductImageException.class)
                .hasMessage("Dosya geçerli bir görsel değil.");
    }

    private byte[] createPng(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }
}
