package com.nexamarket.catalog.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexamarket.auth.entity.UserAccount;
import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.entity.UserStatus;
import com.nexamarket.auth.repository.UserAccountRepository;
import com.nexamarket.catalog.entity.Product;
import com.nexamarket.catalog.entity.ProductImage;
import com.nexamarket.catalog.entity.ProductImageStatus;
import com.nexamarket.catalog.entity.ProductStatus;
import com.nexamarket.catalog.repository.ProductImageRepository;
import com.nexamarket.catalog.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductImageApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String sellerToken;
    private long sellerId;

    @BeforeEach
    void setUpSeller() throws Exception {
        String email = "seller-image-" + System.nanoTime() + "@nexamarket.test";
        UserAccount seller = userAccountRepository.save(UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("StrongPass!2026"))
                .role(UserRole.SELLER)
                .status(UserStatus.ACTIVE)
                .build());
        sellerId = seller.getId();
        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"StrongPass!2026\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        sellerToken = objectMapper.readTree(response).get("accessToken").asText();
    }

    @Test
    void uploadsOriginalAndGeneratesThumbnailAsynchronously() throws Exception {
        Product product = createProduct("Görselli Ürün");
        MockMultipartFile file = new MockMultipartFile(
                "file", "product.png", MediaType.IMAGE_PNG_VALUE, createPng(640, 320));

        String response = mockMvc.perform(multipart("/api/v1/products/{productId}/images", product.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isAccepted())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("PENDING_THUMBNAIL"))
                .andExpect(jsonPath("$.originalUrl").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        long imageId = objectMapper.readTree(response).get("id").asLong();
        ProductImage image = waitForTerminalStatus(imageId);
        assertThat(image.getStatus()).isEqualTo(ProductImageStatus.READY);
        assertThat(image.getThumbnailObjectKey()).endsWith("/thumbnail.jpg");

        byte[] thumbnail = mockMvc.perform(
                        get("/api/v1/products/{productId}/images/{imageId}/thumbnail", product.getId(), imageId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(thumbnail).isNotEmpty();
    }

    @Test
    void marksSpoofedImageAsFailedWithoutFailingUploadRequest() throws Exception {
        Product product = createProduct("Hatalı Görselli Ürün");
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.png", MediaType.IMAGE_PNG_VALUE, "not-an-image".getBytes());

        String response = mockMvc.perform(multipart("/api/v1/products/{productId}/images", product.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        long imageId = objectMapper.readTree(response).get("id").asLong();
        ProductImage image = waitForTerminalStatus(imageId);
        assertThat(image.getStatus()).isEqualTo(ProductImageStatus.FAILED);
        assertThat(image.getFailureReason()).isNotBlank();
    }

    @Test
    void rejectsUnsupportedContentTypeBeforeStorage() throws Exception {
        Product product = createProduct("Metin Dosyalı Ürün");
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", MediaType.TEXT_PLAIN_VALUE, "hello".getBytes());

        mockMvc.perform(multipart("/api/v1/products/{productId}/images", product.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Geçersiz ürün görseli"));
    }

    private Product createProduct(String name) {
        return productRepository.save(Product.builder()
                .sellerId(sellerId)
                .name(name)
                .basePrice(new BigDecimal("100.00"))
                .status(ProductStatus.ACTIVE)
                .build());
    }

    private ProductImage waitForTerminalStatus(long imageId) throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            ProductImage image = productImageRepository.findById(imageId).orElseThrow();
            if (image.getStatus() != ProductImageStatus.PENDING_THUMBNAIL) {
                return image;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("Thumbnail işlemi zamanında tamamlanmadı: " + imageId);
    }

    private byte[] createPng(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.ORANGE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }
}
