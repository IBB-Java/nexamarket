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

    @Test
    void cartUsesAccessibleQuantityStepperAndRefreshesServerStock() throws IOException {
        String script = resource("/static/store.js");
        String styles = resource("/static/store-extra.css");

        assertThat(script)
                .contains("data-decrease-cart", "data-increase-cart", "refreshCatalogProducts")
                .contains("aria-live=\"polite\"")
                .doesNotContain(">1 adet azalt<");
        assertThat(styles)
                .contains(".cart-quantity")
                .contains(".cart-quantity button:focus-visible")
                .contains("grid-template-columns: 54px minmax(0, 1fr)");
    }

    @Test
    void adminOrderViewsRenderCustomerEmailInsteadOfTechnicalCustomerId() throws IOException {
        String script = resource("/static/store.js");

        assertThat(script)
                .contains("order.customerEmail", "item.customerEmail", "Müşteri: ${customerIdentity}")
                .doesNotContain("Müşteri #${order.customerId}", "Müşteri #${item.customerId}");
    }

    @Test
    void accountCenterUsesRoleAwareSectionsInsteadOfEmptyCustomerCards() throws IOException {
        String markup = resource("/static/index.html");
        String script = resource("/static/store.js");

        assertThat(markup)
                .contains("id=\"accountNavigation\"", "id=\"accountProfileFields\"", "id=\"accountRoleOverview\"")
                .doesNotContain("id=\"loyaltyPoints\"", "id=\"orderCount\"", "id=\"returnCount\"");
        assertThat(script)
                .contains("accountRoleSettings", "renderAccountIdentity", "renderCustomerAccountOverview", "renderOperationalAccountOverview")
                .contains("if (state.user?.role === \"CUSTOMER\") renderCustomerAccountOverview(); else renderOperationalAccountOverview(config)")
                .doesNotContain("Sadakat programı CUSTOMER hesapları içindir", "Alışveriş yalnızca CUSTOMER hesapları içindir");
    }

    @Test
    void everyOperationalRoleGetsOnlyItsProfileDescriptionAndDashboardLink() throws IOException {
        String script = resource("/static/store.js");

        assertThat(script)
                .contains("Alışverişlerin, iadelerin ve hesabın tek yerde.")
                .contains("Teslimat hesabını ve profil bilgilerini buradan yönetebilirsin.")
                .contains("Satıcı hesabını ve mağaza bilgilerini buradan yönetebilirsin.")
                .contains("Yönetici hesabını ve güvenlik bilgilerini buradan yönetebilirsin.")
                .contains("Satıcı Paneline Git", "Kurye Paneline Git", "Admin Paneline Git")
                .contains("openAccountDashboard", "data-account-dashboard")
                .contains("if (button.id !== \"sellerAreaButton\") button.hidden = Boolean(state.token)");
    }

    @Test
    void accountCenterUsesRealProfileApisAndOmitsUnavailableOptionalData() throws IOException {
        String script = resource("/static/store.js");

        assertThat(script)
                .contains("/api/v1/users/me", "/api/v1/sellers/me")
                .contains("if (fullName)", "if (profile?.phoneNumber)", "if (state.user?.role === \"SELLER\" && state.accountSellerProfile)")
                .contains("if (state.accountSellerProfile.storeName)", "if (state.accountSellerProfile.status)")
                .contains("state.accountProfile = null; state.accountSellerProfile = null")
                .contains("#logoutButton\").addEventListener(\"click\", logout)");
    }

    @Test
    void roleAwareAccountLayoutRemainsResponsive() throws IOException {
        String styles = resource("/static/store-extra.css");

        assertThat(styles)
                .contains(".account-profile-fields")
                .contains(".account-dashboard-card")
                .contains("grid-template-columns: 1fr 1fr")
                .contains("grid-template-columns: 1fr;");
    }

    private String resource(String path) throws IOException {
        try (var stream = getClass().getResourceAsStream(path)) {
            assertThat(stream).as("classpath resource %s", path).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
