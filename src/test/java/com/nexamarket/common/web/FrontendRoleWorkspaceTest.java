package com.nexamarket.common.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FrontendRoleWorkspaceTest {

    @Test
    void roleRoutesForwardToTheSinglePageApplication() {
        assertThat(new FrontendRouteController().roleWorkspace()).isEqualTo("forward:/index.html");
    }

    @Test
    void frontendContainsRoleSpecificRoutesAndNavigation() throws IOException {
        String script = resource("/static/store.js");

        assertThat(script)
                .contains("/seller/dashboard", "/courier/deliveries", "/admin/dashboard")
                .contains("Yeni atananlar", "Devam edenler", "Teslimat geçmişi")
                .contains("protectedRoleForPath", "applyRoleRoute");
    }

    @Test
    void courierUiUsesDeliveryAssignmentEndpointsInsteadOfOrderStatusShortcut() throws IOException {
        String script = resource("/static/store.js");

        assertThat(script)
                .contains("/api/v1/courier/deliveries")
                .doesNotContain("/api/v1/courier/orders/${subOrderId}/status");
    }

    @Test
    void modalUtilityActionsReserveSpaceForTheCloseControl() throws IOException {
        String markup = resource("/static/index.html");
        String styles = resource("/static/store-extra.css");

        assertThat(markup)
                .contains("aria-label=\"Siparişleri yenile\"")
                .contains("aria-label=\"İade taleplerini yenile\"")
                .contains("aria-label=\"Yönetilebilir iade taleplerini yenile\"");
        assertThat(styles)
                .contains(".modal .account-panel-heading")
                .contains("padding-right: 66px")
                .contains(".modal-close:focus-visible");
    }

    private String resource(String path) throws IOException {
        try (var stream = getClass().getResourceAsStream(path)) {
            assertThat(stream).as("classpath resource %s", path).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
