package com.nexamarket.common.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FrontendMobileExperienceTest {

    @Test
    void customerHasACompactFiveActionMobileNavigation() throws IOException {
        String markup = resource("/static/index.html");
        String script = resource("/static/store.js");

        assertThat(markup)
                .contains("name=\"viewport\"")
                .contains("id=\"mobileBottomNav\"")
                .contains("data-mobile-action=\"home\"", "data-mobile-action=\"search\"")
                .contains("data-mobile-action=\"orders\"", "data-mobile-action=\"cart\"")
                .contains("data-mobile-action=\"account\"")
                .contains("id=\"mobileCartCount\"");
        assertThat(script)
                .contains("function handleMobileAction(action)")
                .contains("#mobileBottomNav")
                .contains("#mobileCartCount");
    }

    @Test
    void mobileBreakpointsPreventGlobalOverflowAndRespectSafeAreas() throws IOException {
        String styles = resource("/static/store-extra.css");

        assertThat(styles)
                .contains("@media (max-width: 640px)")
                .contains("@media (max-width: 360px)")
                .contains("overflow-x: clip")
                .contains("env(safe-area-inset-bottom)")
                .contains("font-size: 16px")
                .contains("min-height: 44px");
    }

    @Test
    void mobileCartUsesBottomSheetAndAccessibleQuantityTargets() throws IOException {
        String styles = resource("/static/store-extra.css");
        String script = resource("/static/store.js");

        assertThat(styles)
                .contains("height: min(92dvh, 760px)")
                .contains("transform: translateY(105%)")
                .contains(".cart-drawer.open { transform: translateY(0); }")
                .contains("grid-template-columns: 44px minmax(44px, auto) 44px")
                .contains(".cart-summary .primary-button { min-height: 50px; }");
        assertThat(script)
                .contains("document.body.classList.add(\"cart-open\")")
                .contains("document.body.classList.remove(\"cart-open\")");
    }

    @Test
    void rolePortalsUseRoleSpecificMobileLabelsAndLargeCourierActions() throws IOException {
        String styles = resource("/static/store-extra.css");
        String script = resource("/static/store.js");

        assertThat(script)
                .contains("[\"assigned\", \"◌\", \"Yeni atananlar\", \"Yeni\"]")
                .contains("[\"products\", \"◇\", \"Ürün yönetimi\", \"Ürünlerim\"]")
                .contains("[\"store\", \"✦\", \"Mağazam\", \"Mağazam\"]")
                .contains("[\"users\", \"◎\", \"Kullanıcılar\", \"Kullanıcılar\"]")
                .contains("data-mobile-label");
        assertThat(styles)
                .contains("#portalNavigation .portal-nav-text::after")
                .contains(".delivery-actions { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr))")
                .contains(".delivery-action { min-height: 48px")
                .contains(".role-order-card,", ".admin-user-row,", ".inventory-row,", ".delivery-card");
    }

    @Test
    void mobileAccountAndFormsFitTheViewport() throws IOException {
        String styles = resource("/static/store-extra.css");
        String markup = resource("/static/index.html");

        assertThat(styles)
                .contains(".account-center-modal { width: 100vw; height: 100dvh")
                .contains(".account-center-layout { height: 100%; display: flex; flex-direction: column")
                .contains(".account-center-content { flex: 1")
                .contains("max-height: calc(100dvh - 16px)")
                .contains("overflow-wrap: anywhere");
        assertThat(markup)
                .contains("inputmode=\"decimal\"")
                .contains("inputmode=\"numeric\"");
    }

    @Test
    void catalogImagesStayResponsiveAndLoadLazily() throws IOException {
        String styles = resource("/static/store-extra.css");
        String script = resource("/static/store.js");

        assertThat(styles)
                .contains(".product-grid { grid-template-columns: 1fr")
                .contains(".product-image { aspect-ratio: 16 / 10")
                .contains(".product-name { min-height: 44px")
                .contains(".price { display: block; max-width: 100%");
        assertThat(script)
                .contains("loading=\"lazy\"")
                .contains("decoding=\"async\"");
    }

    private String resource(String path) throws IOException {
        try (var stream = getClass().getResourceAsStream(path)) {
            assertThat(stream).as("classpath resource %s", path).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
