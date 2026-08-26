package com.nexamarket.catalog.application;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Component
public class ThumbnailGenerator {

    static final int MAX_WIDTH = 320;
    static final long MAX_PIXELS = 40_000_000L;

    public byte[] generateJpeg(byte[] sourceBytes) {
        BufferedImage source = readValidated(sourceBytes);
        int targetWidth = Math.min(MAX_WIDTH, source.getWidth());
        int targetHeight = Math.max(1, (int) Math.round(source.getHeight() * (targetWidth / (double) source.getWidth())));

        BufferedImage thumbnail = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = thumbnail.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, targetWidth, targetHeight);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(thumbnail, "jpg", output)) {
                throw new InvalidProductImageException("JPEG thumbnail kodlayıcısı bulunamadı.");
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new InvalidProductImageException("Thumbnail üretilemedi.", exception);
        }
    }

    private BufferedImage readValidated(byte[] sourceBytes) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(sourceBytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new InvalidProductImageException("Dosya geçerli bir görsel değil.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
                    throw new InvalidProductImageException("Görsel boyutları desteklenen sınırların dışında.");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new InvalidProductImageException("Görsel çözümlenemedi.", exception);
        }
    }
}
