function favoritesStorageKey(user) {
    const identity = user?.id ?? user?.email;
    return identity ? `nexa_favorites_${String(identity).replace(/[^a-zA-Z0-9_.-]/g, "_")}` : "nexa_favorites_guest";
}

function readFavorites(user) {
    try {
        const raw = localStorage.getItem(favoritesStorageKey(user));
        return new Set(JSON.parse(raw || "[]").map(String));
    } catch { return new Set(); }
}

const initialUser = JSON.parse(localStorage.getItem("nexa_user") || "null");
const state = {
    token: localStorage.getItem("nexa_access_token") || "",
    refreshToken: localStorage.getItem("nexa_refresh_token") || "",
    user: initialUser,
    catalog: [], cart: JSON.parse(localStorage.getItem("nexa_cart") || "[]"),
    orders: [], returns: [], manageableReturns: [], selectedReturnSubOrder: null,
    favorites: readFavorites(initialUser),
    sellerProducts: [], courierOrders: [], adminUsers: [], authMode: "login", activeCategory: "all", sort: "featured",
    favoriteOnly: false, coupon: "", lastOrder: null, selectedProduct: null,
    catalogLoading: true, confirmAction: null, pendingVerificationEmail: ""
};

const demoProducts = [
    {name: "Nova Kablosuz Kulaklık", category: "Teknoloji", price: 1499.90, stock: 14, sku: "NEXA-NOVA-001", description: "Aktif gürültü engelleme ve 30 saat pil ömrü."},
    {name: "Kum Seramik Fincan", category: "Yaşam", price: 349.90, stock: 18, sku: "NEXA-KUM-002", description: "El yapımı, günlük ritüeller için tasarlandı."},
    {name: "Luma Masa Lambası", category: "Ev", price: 899.90, stock: 9, sku: "NEXA-LUMA-003", description: "Yumuşak ışığıyla çalışma alanına sakinlik katar."},
    {name: "Terra Günlük Çanta", category: "Yaşam", price: 1199.90, stock: 7, sku: "NEXA-TERRA-004", description: "Şehir hayatına uyumlu, hafif ve dayanıklı."}
];

const $ = selector => document.querySelector(selector);
const $$ = selector => [...document.querySelectorAll(selector)];
const currency = value => new Intl.NumberFormat("tr-TR", {style: "currency", currency: "TRY"}).format(Number(value || 0));
const html = value => String(value ?? "").replace(/[&<>'"]/g, char => ({"&":"&amp;","<":"&lt;",">":"&gt;","'":"&#39;","\"":"&quot;"}[char]));

async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (options.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
    if (state.token && !headers.has("Authorization")) headers.set("Authorization", `Bearer ${state.token}`);
    const response = await fetch(path, {...options, headers});
    const raw = await response.text();
    let body = null;
    try { body = raw ? JSON.parse(raw) : null; } catch { body = raw; }
    if (!response.ok) throw new Error(body?.message || body?.detail || body?.error || raw || "İşlem tamamlanamadı.");
    return body;
}

function saveSession() {
    if (state.token) localStorage.setItem("nexa_access_token", state.token); else localStorage.removeItem("nexa_access_token");
    if (state.refreshToken) localStorage.setItem("nexa_refresh_token", state.refreshToken); else localStorage.removeItem("nexa_refresh_token");
    if (state.user) localStorage.setItem("nexa_user", JSON.stringify(state.user)); else localStorage.removeItem("nexa_user");
}
function saveCart() { localStorage.setItem("nexa_cart", JSON.stringify(state.cart)); }
function saveFavorites() { localStorage.setItem(favoritesStorageKey(state.user), JSON.stringify([...state.favorites])); }
function loadFavoritesForCurrentUser() { state.favorites = readFavorites(state.user); updateFavoritesUI(); renderCatalog(); updateDetailFavorite(); }

function toast(message, type = "info") {
    const target = $("#toast");
    target.textContent = message; target.dataset.type = type; target.classList.add("show");
    clearTimeout(window.toastTimer);
    window.toastTimer = setTimeout(() => target.classList.remove("show"), 3600);
}

function setBusy(button, busy, busyLabel = "İşleniyor…") {
    if (!button) return;
    if (busy) {
        button.dataset.originalLabel = button.innerHTML; button.disabled = true;
        button.innerHTML = `<span class="button-spinner"></span>${busyLabel}`;
    } else {
        button.disabled = false;
        if (button.dataset.originalLabel) button.innerHTML = button.dataset.originalLabel;
        delete button.dataset.originalLabel;
    }
}

function emojiFor(name = "") {
    const lower = name.toLocaleLowerCase("tr");
    if (lower.includes("kulak") || lower.includes("teknoloji") || lower.includes("şarj")) return "◖";
    if (lower.includes("lamba") || lower.includes("ışık")) return "◉";
    if (lower.includes("çanta")) return "◡";
    if (lower.includes("fincan") || lower.includes("seramik")) return "◒";
    if (lower.includes("bitki")) return "✿";
    if (lower.includes("kitap")) return "▤";
    return "✦";
}

function normaliseProduct(product) {
    const category = product.categoryNames?.[0] || product.categories?.[0]?.name || "Seçki";
    const variant = product.variants?.[0] || null;
    return {id: product.id, sellerId: product.sellerId, sellerName: product.sellerName || `Satıcı #${product.sellerId}`, name: product.name,
        description: product.description || "NexaMarket seçkisinden özenle seçildi.", category,
        price: Number(product.minPrice ?? product.basePrice ?? variant?.price ?? 0),
        inStock: product.inStock ?? Number(product.totalStock ?? variant?.stockQuantity ?? 0) > 0,
        stock: Number(product.totalStock ?? variant?.stockQuantity ?? 0), variantId: variant?.id || null,
        status: product.status || "ACTIVE", emoji: emojiFor(`${product.name} ${category}`)};
}

function renderCategoryPills() {
    const categories = [...new Set(state.catalog.map(product => product.category))].sort((a, b) => a.localeCompare(b, "tr"));
    if (state.activeCategory !== "all" && !categories.includes(state.activeCategory)) state.activeCategory = "all";
    $("#categoryPills").innerHTML = ["all", ...categories].map(category => `<button class="pill ${state.activeCategory === category ? "active" : ""}" data-category="${html(category)}">${category === "all" ? "Tümü" : html(category)}</button>`).join("");
    $$("#categoryPills .pill").forEach(button => button.addEventListener("click", () => {
        state.activeCategory = button.dataset.category; renderCategoryPills(); renderCatalog();
    }));
}

function productsForView() {
    const query = $("#searchInput").value.trim().toLocaleLowerCase("tr");
    const products = state.catalog.filter(product => {
        const matchesCategory = state.activeCategory === "all" || product.category.toLocaleLowerCase("tr") === state.activeCategory.toLocaleLowerCase("tr");
        const matchesFavorite = !state.favoriteOnly || state.favorites.has(String(product.id));
        const haystack = `${product.name} ${product.category} ${product.sellerName} ${product.description}`.toLocaleLowerCase("tr");
        return matchesCategory && matchesFavorite && (!query || haystack.includes(query));
    });
    if (state.sort === "price-asc") products.sort((a, b) => a.price - b.price);
    if (state.sort === "price-desc") products.sort((a, b) => b.price - a.price);
    if (state.sort === "name") products.sort((a, b) => a.name.localeCompare(b.name, "tr"));
    return products;
}

function renderCatalogSkeleton() {
    $("#productGrid").innerHTML = Array.from({length: 4}, () => `<article class="product-card skeleton-card"><div class="product-image"></div><div class="product-info"><i></i><i></i><i></i></div></article>`).join("");
    $("#emptyCatalog").hidden = true; $("#searchEmpty").hidden = true;
}

function renderCatalog() {
    if (state.catalogLoading) return renderCatalogSkeleton();
    const products = productsForView();
    $("#productCount").textContent = state.favoriteOnly ? `${products.length} favorin gösteriliyor` : `${products.length} ürün gösteriliyor`;
    $("#emptyCatalog").hidden = state.catalog.length !== 0;
    $("#searchEmpty").hidden = products.length !== 0 || state.catalog.length === 0;
    $("#productGrid").innerHTML = products.map((product, index) => {
        const favorite = state.favorites.has(String(product.id));
        return `<article class="product-card" style="--card-delay:${Math.min(index, 8) * 45}ms">
            <button class="favorite-button ${favorite ? "active" : ""}" data-favorite-product="${product.id}" aria-label="${html(product.name)} favorilere ${favorite ? "çıkar" : "ekle"}" aria-pressed="${favorite}">${favorite ? "♥" : "♡"}</button>
            <button class="product-image" data-view-product="${product.id}" aria-label="${html(product.name)} ürününü incele"><span class="tag">${html(product.category).toUpperCase()}</span><span class="product-symbol">${product.emoji}</span><small>İNCELE →</small></button>
            <div class="product-info"><p class="stock-line ${product.inStock ? "" : "out"}"><i></i>${product.inStock ? `${product.stock || "Sınırlı"} adet stokta` : "Stokta yok"}</p>
                <button class="product-name" data-view-product="${product.id}">${html(product.name)}</button><p class="product-excerpt">${html(product.description)}</p><p class="product-seller">Satıcı: <b>${html(product.sellerName)}</b></p>
                <div class="product-bottom"><div><small>Bugünün fiyatı</small><strong class="price">${currency(product.price)}</strong></div><button class="add-button" data-add-product="${product.id}" aria-label="${html(product.name)} sepete ekle" ${product.inStock ? "" : "disabled"}><span>+</span><em>Sepete ekle</em></button></div>
            </div></article>`;
    }).join("");
    $$('[data-add-product]').forEach(button => button.addEventListener("click", () => addToCart(button.dataset.addProduct, button)));
    $$('[data-view-product]').forEach(button => button.addEventListener("click", () => openProductDetail(button.dataset.viewProduct)));
    $$('[data-favorite-product]').forEach(button => button.addEventListener("click", () => toggleFavorite(button.dataset.favoriteProduct)));
}

async function loadProducts() {
    state.catalogLoading = true; $("#productCount").textContent = "Katalog yenileniyor…"; renderCatalog();
    try {
        const response = await api("/api/v1/products/search?page=0&size=48", {headers: {Authorization: ""}});
        state.catalog = (response.items || []).map(normaliseProduct); renderCategoryPills();
    } catch { toast("Katalog yüklenemedi. Uygulamanın açık olduğundan emin ol.", "error"); }
    finally { state.catalogLoading = false; renderCatalog(); }
}

async function resolveProduct(product) {
    if (product.variantId) return product;
    const sellerName = product.sellerName;
    Object.assign(product, normaliseProduct(await api(`/api/v1/products/${product.id}`, {headers: {Authorization: ""}})));
    product.sellerName = sellerName || product.sellerName;
    return product;
}

async function openProductDetail(productId) {
    const product = state.catalog.find(item => String(item.id) === String(productId));
    if (!product) return;
    try { await resolveProduct(product); } catch (error) { return toast(error.message, "error"); }
    state.selectedProduct = product;
    $("#detailEmoji").textContent = product.emoji; $("#detailCategory").textContent = product.category.toUpperCase();
    $("#detailName").textContent = product.name; $("#detailDescription").textContent = product.description;
    $("#detailPrice").textContent = currency(product.price);
    $("#detailStock").innerHTML = product.inStock ? `<i></i><b>Stokta</b><span>${product.stock || "Sınırlı"} adet, gönderime hazır</span>` : `<i class="out"></i><b>Stokta yok</b><span>Bu ürün şu an siparişe kapalı</span>`;
    $("#detailAddButton").disabled = !product.inStock; updateDetailFavorite(); openModal("productModal");
}

function toggleFavorite(productId) {
    const key = String(productId), product = state.catalog.find(item => String(item.id) === key);
    if (state.favorites.has(key)) { state.favorites.delete(key); toast(`${product?.name || "Ürün"} favorilerden çıkarıldı.`); }
    else { state.favorites.add(key); toast(`${product?.name || "Ürün"} favorilerine eklendi.`, "success"); }
    saveFavorites(); updateFavoritesUI(); updateDetailFavorite(); renderCatalog();
}
function updateFavoritesUI() {
    $("#favoriteCount").textContent = state.favorites.size; $("#favoritesButton").classList.toggle("active", state.favoriteOnly);
    $("#favoritesButton").setAttribute("aria-pressed", String(state.favoriteOnly));
}
function updateDetailFavorite() {
    if (!state.selectedProduct) return;
    const favorite = state.favorites.has(String(state.selectedProduct.id));
    $("#detailFavoriteButton").textContent = favorite ? "♥ Favorilerimde" : "♡ Favorilerime ekle";
    $("#detailFavoriteButton").classList.toggle("active", favorite);
}

function applyCartResponse(cart) {
    state.cart = (cart?.items || []).map(item => {
        const product = state.catalog.find(candidate => String(candidate.id) === String(item.productId) || String(candidate.variantId) === String(item.productVariantId));
        const name = item.productName || product?.name || "Sepetteki ürün";
        return {cartItemId: item.id, id: item.productId ?? product?.id ?? item.productVariantId, variantId: item.productVariantId,
            name, price: Number(item.unitPrice ?? product?.price ?? 0), quantity: item.quantity,
            category: product?.category || "Seçki", emoji: product?.emoji || emojiFor(name)};
    });
    saveCart(); renderCart();
}
async function syncCart(cart = null) {
    if (!state.token) return;
    if (state.user?.role !== "CUSTOMER") { state.cart = []; saveCart(); renderCart(); return; }
    try { applyCartResponse(cart || await api("/api/v1/cart/items")); } catch { toast("Sepetin şu an güncellenemedi.", "error"); }
}

async function addToCart(productId, button = null) {
    const product = state.catalog.find(item => String(item.id) === String(productId));
    if (!product) return;
    if (!state.token) { openModal("authModal"); $("#authMessage").textContent = "Sepete eklemek için önce giriş yapmalısın."; return; }
    if (state.user?.role !== "CUSTOMER") { toast("Yalnızca CUSTOMER hesapları ürün satın alabilir.", "error"); return; }
    setBusy(button, true, "Ekleniyor");
    try {
        await resolveProduct(product);
        if (!product.variantId) throw new Error("Bu ürünün sipariş edilebilir bir seçeneği bulunamadı.");
        applyCartResponse(await api("/api/v1/cart/items", {method: "POST", body: JSON.stringify({productVariantId: product.variantId, quantity: 1})}));
        toast(`${product.name} sepete eklendi.`, "success"); if ($("#productModal").open) $("#productModal").close();
    } catch (error) { toast(error.message, "error"); } finally { setBusy(button, false); }
}

function renderCart() {
    const total = state.cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
    $("#cartCount").textContent = state.cart.reduce((sum, item) => sum + item.quantity, 0);
    $("#cartItems").innerHTML = state.cart.map(item => `<div class="cart-row"><div class="cart-row-art">${item.emoji}</div><div><small>${html(item.category)}</small><h3>${html(item.name)}</h3><p>${item.quantity} adet · ${currency(item.price)}</p></div><div><strong>${currency(item.price * item.quantity)}</strong><button class="remove-item" data-remove-cart="${item.cartItemId || item.id}" aria-label="${html(item.name)} ürününü sepetten çıkar">Sil</button></div></div>`).join("");
    $("#cartTotal").textContent = currency(total); $("#cartEmpty").hidden = state.cart.length > 0; $("#cartSummary").hidden = state.cart.length === 0;
    $$('[data-remove-cart]').forEach(button => button.addEventListener("click", () => requestCartRemoval(button.dataset.removeCart)));
}
function requestCartRemoval(itemKey) {
    const item = state.cart.find(candidate => String(candidate.cartItemId || candidate.id) === String(itemKey));
    if (item) askConfirmation("Sepetten çıkarılsın mı?", `${item.name} sepetinden kaldırılacak ve ayrılan stok geri bırakılacak.`, "Sepetten çıkar", () => removeFromCart(item));
}
async function removeFromCart(item) {
    if (!state.token || !item.cartItemId) { state.cart = state.cart.filter(candidate => candidate !== item); saveCart(); renderCart(); return; }
    applyCartResponse(await api(`/api/v1/cart/items/${item.cartItemId}`, {method: "DELETE"}));
    toast(`${item.name} sepetten çıkarıldı.`, "success");
}

function openCart() { closeAccountMenu(); $("#cartDrawer").classList.add("open"); $("#cartDrawer").setAttribute("aria-hidden", "false"); $("#overlay").hidden = false; }
function closeCart() { $("#cartDrawer").classList.remove("open"); $("#cartDrawer").setAttribute("aria-hidden", "true"); $("#overlay").hidden = true; }
function closeModals() { $$("dialog[open]").forEach(dialog => dialog.close()); }
function openModal(id) { closeCart(); closeAccountMenu(); closeModals(); document.getElementById(id).showModal(); }
function closeAccountMenu() { $("#accountMenu").hidden = true; $("#authButton").setAttribute("aria-expanded", "false"); }
function toggleAccountMenu() {
    if (!state.token) return openModal("authModal");
    const menu = $("#accountMenu"); menu.hidden = !menu.hidden; $("#authButton").setAttribute("aria-expanded", String(!menu.hidden));
}

function updateAuthUI() {
    const name = state.user?.email?.split("@")[0] || "Hesabım";
    $("#authButtonLabel").textContent = state.token ? name : "Giriş yap"; $("#menuUserName").textContent = state.token ? name : "NexaMarketli";
    $("#menuUserEmail").textContent = state.user?.email || "Hesabına giriş yap"; $("#authButton").classList.toggle("signed-in", Boolean(state.token)); closeAccountMenu();
    $("#courierAreaButton").hidden = state.user?.role !== "COURIER";
    $("#adminPanelButton").hidden = state.user?.role !== "ADMIN";
    $("#returnManagementButton").hidden = !["SELLER", "ADMIN"].includes(state.user?.role);
}
const orderStatusLabels = {PAYMENT_PENDING: "Ödeme bekleniyor", PAID: "Ödeme alındı", PROCESSING: "Hazırlanıyor", SHIPPED: "Kargoda", DELIVERED: "Teslim edildi", CANCELLED: "İptal edildi", RETURN_REQUESTED: "İade inceleniyor", RETURN_APPROVED: "İade onaylandı", RETURN_REJECTED: "İade reddedildi"};
const returnStatusLabels = {REQUESTED: "İnceleniyor", APPROVED: "Onaylandı", REJECTED: "Reddedildi"};

function switchAccountTab(tab) {
    $$("[data-account-tab]").forEach(button => button.classList.toggle("active", button.dataset.accountTab === tab));
    $$("[data-account-panel]").forEach(panel => panel.classList.toggle("active", panel.dataset.accountPanel === tab));
}

async function openAccount(initialTab = "overview") {
    if (!state.token) return openModal("authModal");
    const displayName = state.user?.email?.split("@")[0] || "NexaMarketli";
    $("#accountName").textContent = `Merhaba, ${displayName}`; $("#accountNavName").textContent = displayName; $("#accountNavEmail").textContent = state.user?.email || "";
    $$("[data-account-tab='orders'], [data-account-tab='returns']").forEach(button => button.hidden = state.user?.role !== "CUSTOMER");
    $$("[data-account-target]").forEach(button => button.hidden = state.user?.role !== "CUSTOMER");
    if (state.user?.role !== "CUSTOMER") {
        $("#loyaltyPoints").textContent = "—"; $("#loyaltyUnit").textContent = ""; $("#loyaltyLabel").textContent = "Sadakat programı CUSTOMER hesapları içindir";
        $("#orderCount").textContent = "—"; $("#orderUnit").textContent = ""; $("#orderLabel").textContent = "Alışveriş yalnızca CUSTOMER hesapları içindir";
        $("#returnCount").textContent = "—"; switchAccountTab("overview"); return openModal("accountModal");
    }
    $("#loyaltyUnit").textContent = "puan"; $("#loyaltyLabel").textContent = "Sadakat bakiyen";
    $("#orderUnit").textContent = "sipariş"; $("#orderLabel").textContent = "Toplam alışverişin";
    openModal("accountModal"); switchAccountTab(initialTab);
    await Promise.all([loadOrders(), loadCustomerReturns()]);
    $("#orderCount").textContent = state.orders.length;
    $("#returnCount").textContent = state.returns.length;
    try { $("#loyaltyPoints").textContent = (await api("/api/v1/loyalty/me")).points ?? 0; } catch { $("#loyaltyPoints").textContent = "0"; }
}

async function loadOrders() {
    $("#accountOrderList").innerHTML = `<div class="inventory-loading"><span class="button-spinner"></span>Siparişlerin hazırlanıyor…</div>`;
    try { state.orders = await api("/api/v1/orders/me"); renderAccountOrders(); }
    catch (error) { state.orders = []; $("#accountOrderList").innerHTML = `<div class="inventory-empty"><b>Siparişler yüklenemedi</b><small>${html(error.message)}</small></div>`; }
}

function renderAccountOrders() {
    if (!state.orders.length) {
        $("#accountOrderList").innerHTML = `<div class="inventory-empty"><span>▤</span><b>Henüz siparişin yok</b><small>Yeni favorilerini keşfettiğinde siparişlerini buradan takip edebilirsin.</small><button class="primary-button small" data-close-modal onclick="document.querySelector('#discover').scrollIntoView({behavior:'smooth'})">Alışverişe başla</button></div>`;
        return;
    }
    $("#accountOrderList").innerHTML = state.orders.map(order => {
        const subOrders = order.subOrders || [];
        const latestStatus = subOrders[0]?.status || order.status;
        const progress = {PAYMENT_PENDING: 1, PAID: 2, PROCESSING: 2, SHIPPED: 3, DELIVERED: 4, RETURN_REQUESTED: 4, RETURN_APPROVED: 4, RETURN_REJECTED: 4}[latestStatus] || 1;
        const parts = subOrders.map(subOrder => {
            const canReturn = ["PAID", "PROCESSING", "SHIPPED", "DELIVERED"].includes(subOrder.status);
            const returnAction = canReturn ? `<button class="order-return-button" data-return-sub-order="${subOrder.subOrderId}" data-return-order="${order.orderId}">↺ İade talebi oluştur</button>` : `<span class="suborder-status">${html(orderStatusLabels[subOrder.status] || subOrder.status)}</span>`;
            return `<div class="suborder-row"><div><span>${subOrder.itemCount} ürün · Satıcı #${subOrder.sellerId}</span><b>${currency(subOrder.subtotal)}</b></div>${returnAction}</div>`;
        }).join("");
        return `<article class="account-order-card"><header><div><span>Sipariş #${html(String(order.orderId).slice(0, 8).toUpperCase())}</span><small>${html(formatOrderDate(order.createdAt))}</small></div><strong>${currency(order.totalAmount)}</strong></header><div class="order-progress progress-${progress}"><i></i><i></i><i></i><i></i></div><div class="order-progress-labels"><span>Alındı</span><span>Hazırlanıyor</span><span>Kargoda</span><span>Teslim</span></div><div class="suborder-list">${parts || `<span>${html(orderStatusLabels[order.status] || order.status)}</span>`}</div></article>`;
    }).join("");
    $$('[data-return-sub-order]').forEach(button => button.addEventListener("click", () => openReturnRequest(button.dataset.returnSubOrder, button.dataset.returnOrder)));
}

async function loadCustomerReturns() {
    $("#customerReturnList").innerHTML = `<div class="inventory-loading"><span class="button-spinner"></span>İadelerin hazırlanıyor…</div>`;
    try { state.returns = await api("/api/v1/returns/me"); renderCustomerReturns(); }
    catch (error) { state.returns = []; $("#customerReturnList").innerHTML = `<div class="inventory-empty"><b>İadeler yüklenemedi</b><small>${html(error.message)}</small></div>`; }
}

function renderCustomerReturns() {
    $("#returnCount").textContent = state.returns.length;
    if (!state.returns.length) { $("#customerReturnList").innerHTML = `<div class="inventory-empty"><span>↺</span><b>Henüz iade talebin yok</b><small>İade edilebilir siparişlerini “Siparişlerim” alanından seçebilirsin.</small></div>`; return; }
    $("#customerReturnList").innerHTML = state.returns.map(item => `<article class="return-card"><div class="return-card-icon">↺</div><div><span class="status-badge return-${item.status.toLowerCase()}">${html(returnStatusLabels[item.status] || item.status)}</span><b>Sipariş #${html(String(item.orderId).slice(0, 8).toUpperCase())}</b><small>${html(formatOrderDate(item.createdAt))} · ${currency(item.amount)}</small><p>${html(item.reason)}</p></div></article>`).join("");
}

function openReturnRequest(subOrderId, orderId) {
    state.selectedReturnSubOrder = subOrderId; $("#returnRequestForm").reset(); $("#returnRequestMessage").textContent = "";
    $("#returnOrderSummary").textContent = `#${String(orderId).slice(0, 8).toUpperCase()} numaralı siparişindeki ürünler için iade nedeni paylaş.`;
    openModal("returnRequestModal");
}

async function submitReturnRequest(event) {
    event.preventDefault(); const button = $("#submitReturnButton"), preset = $("#returnReasonPreset").value, detail = $("#returnReasonDetail").value.trim();
    const reason = preset === "other" ? detail : [preset, detail].filter(Boolean).join(" — ");
    if (!reason) { $("#returnRequestMessage").textContent = "Lütfen iade nedenini yaz."; return; }
    setBusy(button, true, "Talep oluşturuluyor");
    try {
        await api("/api/v1/returns", {method: "POST", body: JSON.stringify({subOrderId: state.selectedReturnSubOrder, reason})});
        toast("İade talebin oluşturuldu. Durumunu iade merkezinden takip edebilirsin.", "success"); await openAccount("returns");
    } catch (error) { $("#returnRequestMessage").textContent = error.message; }
    finally { setBusy(button, false); }
}

async function openReturnManagement() {
    if (!["SELLER", "ADMIN"].includes(state.user?.role)) return toast("Bu alan yalnızca satıcı ve yönetici hesapları içindir.", "error");
    openModal("returnManagementModal"); await loadManageableReturns();
}

async function loadManageableReturns() {
    $("#manageableReturnList").innerHTML = `<div class="inventory-loading"><span class="button-spinner"></span>Talepler hazırlanıyor…</div>`;
    try { state.manageableReturns = await api("/api/v1/returns/manageable"); renderManageableReturns(); }
    catch (error) { $("#manageableReturnList").innerHTML = `<div class="inventory-empty"><b>Talepler yüklenemedi</b><small>${html(error.message)}</small></div>`; }
}

function renderManageableReturns() {
    if (!state.manageableReturns.length) { $("#manageableReturnList").innerHTML = `<div class="inventory-empty"><span>✓</span><b>Bekleyen iade yok</b><small>Yeni bir talep geldiğinde burada görüntülenecek.</small></div>`; return; }
    $("#manageableReturnList").innerHTML = state.manageableReturns.map(item => `<article class="return-card manageable"><div class="return-card-icon">↺</div><div><span class="status-badge return-${item.status.toLowerCase()}">${html(returnStatusLabels[item.status] || item.status)}</span><b>#${html(String(item.orderId).slice(0, 8).toUpperCase())} · Satıcı #${item.sellerId}</b><small>${html(formatOrderDate(item.createdAt))} · ${currency(item.amount)}</small><p>${html(item.reason)}</p></div>${item.status === "REQUESTED" ? `<div class="return-actions"><button data-resolve-return="${item.id}" data-return-status="REJECTED">Reddet</button><button class="approve" data-resolve-return="${item.id}" data-return-status="APPROVED">Onayla</button></div>` : ""}</article>`).join("");
    $$('[data-resolve-return]').forEach(button => button.addEventListener("click", () => resolveReturn(button.dataset.resolveReturn, button.dataset.returnStatus, button)));
}

async function resolveReturn(returnId, status, button) {
    setBusy(button, true, status === "APPROVED" ? "Onaylanıyor" : "Reddediliyor");
    try { await api(`/api/v1/returns/${returnId}`, {method: "PATCH", body: JSON.stringify({status})}); toast(status === "APPROVED" ? "İade talebi onaylandı." : "İade talebi reddedildi.", "success"); await loadManageableReturns(); }
    catch (error) { toast(error.message, "error"); setBusy(button, false); }
}
function clearLocalSession() {
    state.token = ""; state.refreshToken = ""; state.user = null; state.cart = []; state.orders = []; state.returns = []; state.manageableReturns = []; state.favorites = readFavorites(null);
    saveSession(); saveCart(); renderCart(); updateFavoritesUI(); renderCatalog(); updateAuthUI(); closeModals();
}
async function logout() {
    const refreshToken = state.refreshToken;
    try { if (refreshToken) await api("/api/v1/auth/logout", {method: "POST", body: JSON.stringify({refreshToken})}); }
    catch { /* The local session is still cleared if the server token has expired. */ }
    finally { clearLocalSession(); toast("Hesabından güvenle çıkış yaptın.", "success"); }
}
async function hydrateUser() {
    if (!state.token) return;
    try { state.user = await api("/api/v1/auth/me"); saveSession(); loadFavoritesForCurrentUser(); } catch { clearLocalSession(); }
    updateAuthUI();
}

function setAuthMode(mode) {
    hideVerificationPanel();
    state.authMode = mode; $$("[data-auth-tab]").forEach(button => button.classList.toggle("active", button.dataset.authTab === mode));
    $("#authTitle").textContent = mode === "login" ? "Alışverişe başla" : "Hesabını oluştur";
    $("#authSubtitle").textContent = mode === "login" ? "Sepete eklemek ve sipariş vermek için giriş yap." : "NexaMarket deneyimine birkaç saniyede katıl.";
    $("#authSubmit").innerHTML = mode === "login" ? "Giriş yap <span>→</span>" : "Hesap oluştur <span>→</span>"; setAuthMessage("");
}

function setAuthMessage(message, success = false) {
    const target = $("#authMessage");
    target.textContent = message;
    target.classList.toggle("success", success);
}

function hideVerificationPanel() {
    $("#verificationPanel").hidden = true;
    $("#authTabs").hidden = false;
    $("#authForm").hidden = false;
}

function showVerificationPanel(email) {
    state.pendingVerificationEmail = email;
    $("#verificationEmail").textContent = email;
    $("#authTabs").hidden = true;
    $("#authForm").hidden = true;
    $("#verificationPanel").hidden = false;
    $("#authTitle").textContent = "Son bir adım kaldı";
    $("#authSubtitle").textContent = "E-posta adresini doğruladığında hesabın alışverişe hazır olacak.";
    setAuthMessage("");
}

async function resendVerification() {
    const button = $("#resendVerificationButton");
    if (!state.pendingVerificationEmail) return;
    setBusy(button, true, "Gönderiliyor");
    try {
        await api("/api/v1/auth/resend-verification", {method: "POST", body: JSON.stringify({email: state.pendingVerificationEmail})});
        toast("Doğrulama e-postası yeniden gönderildi.", "success");
    } catch (error) { toast(error.message, "error"); }
    finally { setBusy(button, false); }
}
async function submitAuth(event) {
    event.preventDefault(); const email = $("#authEmail").value.trim(), password = $("#authPassword").value, submit = $("#authSubmit");
    $("#authMessage").textContent = ""; setBusy(submit, true, state.authMode === "login" ? "Giriş yapılıyor" : "Hesap oluşturuluyor");
    try {
        if (state.authMode === "register") {
            await api("/api/v1/auth/register", {method: "POST", body: JSON.stringify({email, password})});
            showVerificationPanel(email);
            return;
        }
        const login = await api("/api/v1/auth/login", {method: "POST", body: JSON.stringify({email, password})});
        state.token = login.accessToken; state.refreshToken = login.refreshToken || ""; state.user = await api("/api/v1/auth/me");
        saveSession(); loadFavoritesForCurrentUser(); updateAuthUI(); closeModals(); await syncCart(); toast("Hoş geldin! Alışverişe devam edebilirsin.", "success");
    } catch (error) { setAuthMessage(error.message); } finally { setBusy(submit, false); }
}

async function openSellerArea() {
    if (!state.token) { openModal("authModal"); $("#authMessage").textContent = "Ürün eklemek için önce giriş yapmalısın."; return; }
    if (state.user?.role !== "SELLER") { toast("Ürün eklemek için ADMIN tarafından SELLER rolüne yükseltilmelisin.", "error"); return; }
    openModal("sellerModal"); await loadSellerProducts();
}
async function openCourierArea() {
    if (!state.token) { openModal("authModal"); $("#authMessage").textContent = "Kurye alanı için önce giriş yapmalısın."; return; }
    if (state.user?.role !== "COURIER") { toast("Bu alan yalnızca COURIER hesapları içindir.", "error"); return; }
    openModal("courierModal"); await loadCourierOrders();
}
async function openAdminPanel() {
    if (!state.token) { openModal("authModal"); $("#authMessage").textContent = "Yönetim paneli için önce giriş yapmalısın."; return; }
    if (state.user?.role !== "ADMIN") { toast("Bu alan yalnızca ADMIN hesapları içindir.", "error"); return; }
    openModal("adminModal"); await loadAdminUsers();
}
async function loadAdminUsers() {
    $("#adminUsers").innerHTML = `<div class="inventory-loading"><span class="button-spinner"></span>Kullanıcılar hazırlanıyor…</div>`;
    try {
        const users = await api("/api/v1/admin/auth/users");
        state.adminUsers = users.filter(user => ["CUSTOMER", "SELLER", "COURIER"].includes(user.role));
        renderAdminUsers();
    } catch (error) { $("#adminUsers").innerHTML = `<div class="inventory-empty"><b>Kullanıcılar yüklenemedi</b><small>${html(error.message)}</small></div>`; }
}
function renderAdminUsers() {
    const users = state.adminUsers || [];
    $("#adminUserCount").textContent = `${users.length} yönetilebilir kullanıcı`;
    if (!users.length) { $("#adminUsers").innerHTML = `<div class="inventory-empty"><span>◫</span><b>Yönetilecek kullanıcı yok</b><small>Soldaki formdan ilk kullanıcıyı ekleyebilirsin.</small></div>`; return; }
    const roleLabels = {CUSTOMER: "Alıcı", SELLER: "Satıcı", COURIER: "Kurye"};
    $("#adminUsers").innerHTML = users.map(user => {
        const nextStatus = user.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
        const statusAction = user.status === "ACTIVE" ? "Devre dışı bırak" : "Etkinleştir";
        const roleActions = user.role === "CUSTOMER" ? `<button data-admin-role="SELLER" data-admin-user="${user.id}">Satıcı yap</button><button data-admin-role="COURIER" data-admin-user="${user.id}">Kurye yap</button>` : "";
        return `<article class="admin-user-row"><div class="admin-user-avatar">${html(user.email.charAt(0).toUpperCase())}</div><div class="admin-user-copy"><div><span class="status-badge status-${user.status.toLowerCase()}">${user.status === "ACTIVE" ? "Aktif" : "Devre dışı"}</span><small>${html(roleLabels[user.role] || user.role)}</small></div><b>${html(user.email)}</b><strong>ID #${user.id}</strong></div><div class="admin-user-actions">${roleActions}<button data-admin-status="${nextStatus}" data-admin-user="${user.id}">${statusAction}</button><button class="delete-product" data-admin-delete="${user.id}">Sil</button></div></article>`;
    }).join("");
    $$('[data-admin-role]').forEach(button => button.addEventListener("click", () => updateAdminUserRole(button.dataset.adminUser, button.dataset.adminRole, button)));
    $$('[data-admin-status]').forEach(button => button.addEventListener("click", () => updateAdminUserStatus(button.dataset.adminUser, button.dataset.adminStatus, button)));
    $$('[data-admin-delete]').forEach(button => button.addEventListener("click", () => requestAdminUserDeletion(button.dataset.adminDelete)));
}
async function updateAdminUserRole(userId, role, button) {
    setBusy(button, true, "Güncelleniyor");
    try { await api(`/api/v1/admin/users/${userId}/role`, {method: "PATCH", body: JSON.stringify({role})}); toast("Kullanıcı rolü güncellendi.", "success"); await loadAdminUsers(); }
    catch (error) { toast(error.message, "error"); setBusy(button, false); }
}
async function updateAdminUserStatus(userId, status, button) {
    setBusy(button, true, "Güncelleniyor");
    try { await api(`/api/v1/admin/users/${userId}/status`, {method: "PATCH", body: JSON.stringify({status})}); toast("Kullanıcı durumu güncellendi.", "success"); await loadAdminUsers(); }
    catch (error) { toast(error.message, "error"); setBusy(button, false); }
}
function requestAdminUserDeletion(userId) {
    const user = state.adminUsers.find(item => String(item.id) === String(userId));
    if (user) askConfirmation("Kullanıcı silinsin mi?", `${user.email} hesabı ve oturumları silinecek. Geçmiş sipariş kayıtları korunur.`, "Kullanıcıyı sil", () => deleteAdminUser(user.id));
}
async function deleteAdminUser(userId) {
    await api(`/api/v1/admin/users/${userId}`, {method: "DELETE"});
    toast("Kullanıcı silindi.", "success"); await loadAdminUsers();
}
async function submitAdminUser(event) {
    event.preventDefault(); const submit = $("#adminUserSubmitButton");
    $("#adminUserMessage").textContent = ""; setBusy(submit, true, "Kullanıcı ekleniyor");
    try {
        await api("/api/v1/admin/auth/users", {method: "POST", body: JSON.stringify({email: $("#adminUserEmail").value.trim(), password: $("#adminUserPassword").value, role: $("#adminUserRole").value})});
        $("#adminUserForm").reset(); $("#adminUserMessage").textContent = "Kullanıcı oluşturuldu."; $("#adminUserMessage").classList.add("success");
        await loadAdminUsers();
    } catch (error) { $("#adminUserMessage").classList.remove("success"); $("#adminUserMessage").textContent = error.message; }
    finally { setBusy(submit, false); }
}
async function loadCourierOrders() {
    $("#courierOrders").innerHTML = `<div class="inventory-loading"><span class="button-spinner"></span>Siparişlerin hazırlanıyor…</div>`;
    try { state.courierOrders = await api("/api/v1/courier/orders"); renderCourierOrders(); }
    catch (error) { $("#courierOrders").innerHTML = `<div class="inventory-empty"><b>Siparişler yüklenemedi</b><small>${html(error.message)}</small></div>`; }
}
function formatOrderDate(value) {
    return value ? new Intl.DateTimeFormat("tr-TR", {dateStyle: "medium", timeStyle: "short"}).format(new Date(value)) : "Tarih bilgisi yok";
}
function renderCourierOrders() {
    const orders = state.courierOrders || [];
    $("#courierOrderCount").textContent = `${orders.length} atanmış sipariş`;
    if (!orders.length) {
        $("#courierOrders").innerHTML = `<div class="inventory-empty"><span>▣</span><b>Henüz sana atanmış sipariş yok</b><small>Yönetici bir siparişi sana atadığında burada görünecek.</small></div>`;
        return;
    }
    const labels = {PAID: "Ödeme alındı", PROCESSING: "Hazırlanıyor", SHIPPED: "Kargoda", DELIVERED: "Teslim edildi", CANCELLED: "İptal edildi"};
    $("#courierOrders").innerHTML = orders.map(order => {
        const status = labels[order.status] || order.status;
        const action = order.status === "PROCESSING"
            ? `<button class="primary-button small" data-courier-status="SHIPPED" data-sub-order-id="${order.subOrderId}">Kargoya ver</button>`
            : order.status === "SHIPPED"
                ? `<button class="primary-button small" data-courier-status="DELIVERED" data-sub-order-id="${order.subOrderId}">Teslim edildi</button>`
                : order.status === "PAID"
                    ? `<small class="courier-note">Satıcının hazırlaması bekleniyor.</small>`
                    : `<small class="courier-note">Bu sipariş için işlem tamamlandı.</small>`;
        return `<article class="courier-order-row"><div class="courier-order-icon">▣</div><div class="courier-order-copy"><div><span class="status-badge status-${String(order.status).toLowerCase()}">${status}</span><small>${formatOrderDate(order.createdAt)}</small></div><b>#${html(String(order.subOrderId).slice(0, 8).toUpperCase())}</b><strong>${currency(order.subtotal)}</strong></div><div class="courier-order-action">${action}</div></article>`;
    }).join("");
    $$('[data-courier-status]').forEach(button => button.addEventListener("click", () => updateCourierOrderStatus(button.dataset.subOrderId, button.dataset.courierStatus, button)));
}
async function updateCourierOrderStatus(subOrderId, status, button) {
    setBusy(button, true, status === "SHIPPED" ? "Kargoya veriliyor" : "Güncelleniyor");
    try {
        await api(`/api/v1/courier/orders/${subOrderId}/status`, {method: "PATCH", body: JSON.stringify({status})});
        toast(status === "SHIPPED" ? "Sipariş kargoya verildi." : "Sipariş teslim edildi.", "success");
        await loadCourierOrders();
    } catch (error) { toast(error.message, "error"); setBusy(button, false); }
}
async function ensureCategory(name) {
    const categories = await api("/api/v1/categories");
    const existing = categories.find(category => category.name.toLocaleLowerCase("tr") === name.toLocaleLowerCase("tr"));
    if (existing) return existing.id;
    throw new Error(`“${name}” kategorisi bulunamadı. Önce ADMIN tarafından oluşturulmalı.`);
}
function generatedSku(name) {
    const slug = name.toLocaleUpperCase("tr").replace(/İ/g, "I").replace(/Ş/g, "S").replace(/Ğ/g, "G").replace(/Ü/g, "U").replace(/Ö/g, "O").replace(/Ç/g, "C").replace(/[^A-Z0-9]+/g, "-").replace(/^-|-$/g, "").slice(0, 22) || "URUN";
    return `NX-${slug}-${Date.now().toString(36).slice(-5).toUpperCase()}`;
}
async function createProduct(payload, silent = false) {
    const categoryId = await ensureCategory(payload.category);
    let product = await api("/api/v1/products", {method: "POST", body: JSON.stringify({name: payload.name, description: payload.description,
        basePrice: payload.price, categoryIds: [categoryId], variants: [{sku: payload.sku || generatedSku(payload.name), attributes: {"Seçenek": "Standart"}, price: payload.price, stockQuantity: payload.stock}]})});
    if (payload.publish !== false) product = await api(`/api/v1/products/${product.id}/publication`, {method: "PATCH", body: JSON.stringify({status: "ACTIVE"})});
    const normalised = normaliseProduct(product);
    if (normalised.status === "ACTIVE") state.catalog = [normalised, ...state.catalog.filter(item => String(item.id) !== String(normalised.id))];
    renderCategoryPills(); renderCatalog();
    if (!silent) toast(normalised.status === "ACTIVE" ? "Ürün vitrinde yayına alındı." : "Ürün taslak olarak kaydedildi.", "success");
    return normalised;
}
async function seedCatalog() {
    if (state.user?.role !== "SELLER") { toast("Demo ürünleri eklemek için SELLER hesabı gerekir.", "error"); return; }
    const button = $("#emptySeedButton"); setBusy(button, true, "Seçki hazırlanıyor");
    try {
        for (const product of demoProducts) { try { await createProduct(product, true); } catch (error) { if (!error.message.includes("SKU zaten")) throw error; } }
        await loadProducts(); toast("Mağaza keşfe hazır.", "success");
    } catch (error) { toast(error.message, "error"); } finally { setBusy(button, false); }
}
async function submitSeller(event) {
    event.preventDefault(); const submit = $("#sellerSubmitButton");
    $("#sellerMessage").textContent = ""; setBusy(submit, true, "Ürün kaydediliyor");
    try {
        if (state.user?.role !== "SELLER") throw new Error("Bu işlem yalnızca SELLER rolündeki kullanıcılar içindir.");
        const product = await createProduct({name: $("#sellerProductName").value.trim(), category: $("#sellerCategory").value.trim(), price: Number($("#sellerPrice").value), stock: Number($("#sellerStock").value), sku: $("#sellerSku").value.trim(), description: $("#sellerDescription").value.trim() || "NexaMarket satıcısından yeni ürün.", publish: $("#sellerPublish").checked}, true);
        $("#sellerMessage").textContent = product.status === "ACTIVE" ? `${product.name} vitrinde yayına alındı.` : `${product.name} taslak olarak kaydedildi.`; $("#sellerMessage").classList.add("success");
        $("#sellerForm").reset(); $("#sellerCategory").value = "Yaşam"; $("#sellerPrice").value = "249.90"; $("#sellerStock").value = "12"; $("#sellerPublish").checked = true;
        await loadSellerProducts(); renderCategoryPills(); renderCatalog();
    } catch (error) { $("#sellerMessage").classList.remove("success"); $("#sellerMessage").textContent = error.message; } finally { setBusy(submit, false); }
}

async function loadSellerProducts() {
    $("#sellerInventory").innerHTML = `<div class="inventory-loading"><span class="button-spinner"></span>Ürünlerin hazırlanıyor…</div>`;
    try { state.sellerProducts = await api("/api/v1/products/seller"); renderSellerProducts(); }
    catch (error) { $("#sellerInventory").innerHTML = `<div class="inventory-empty"><b>Ürünler yüklenemedi</b><small>${html(error.message)}</small></div>`; }
}
function renderSellerProducts() {
    if (!state.sellerProducts.length) { $("#sellerInventory").innerHTML = `<div class="inventory-empty"><span>◇</span><b>Henüz ürünün yok</b><small>Soldaki formdan ilk ürününü ekleyebilirsin.</small></div>`; return; }
    const labels = {ACTIVE: "Yayında", DRAFT: "Taslak", PASSIVE: "Satışta değil"};
    $("#sellerInventory").innerHTML = state.sellerProducts.map(product => {
        const stock = (product.variants || []).reduce((sum, variant) => sum + Number(variant.stockQuantity || 0), 0), active = product.status === "ACTIVE";
        return `<article class="inventory-row"><div class="inventory-art">${emojiFor(product.name)}</div><div class="inventory-copy"><div><span class="status-badge status-${product.status.toLowerCase()}">${labels[product.status] || product.status}</span><small>${stock} stok</small></div><b>${html(product.name)}</b><strong>${currency(product.basePrice)}</strong></div><div class="inventory-actions"><button data-toggle-product="${product.id}" data-target-status="${active ? "PASSIVE" : "ACTIVE"}">${active ? "Yayından kaldır" : "Yayınla"}</button><button class="delete-product" data-delete-product="${product.id}">Sil</button></div></article>`;
    }).join("");
    $$('[data-toggle-product]').forEach(button => button.addEventListener("click", () => changeProductPublication(button.dataset.toggleProduct, button.dataset.targetStatus, button)));
    $$('[data-delete-product]').forEach(button => button.addEventListener("click", () => requestProductDelete(button.dataset.deleteProduct)));
}
async function changeProductPublication(productId, status, button) {
    setBusy(button, true, status === "ACTIVE" ? "Yayınlanıyor" : "Kaldırılıyor");
    try {
        const updated = await api(`/api/v1/products/${productId}/publication`, {method: "PATCH", body: JSON.stringify({status})});
        state.sellerProducts = state.sellerProducts.map(product => String(product.id) === String(productId) ? updated : product);
        if (status === "ACTIVE") { const item = normaliseProduct(updated); state.catalog = [item, ...state.catalog.filter(product => String(product.id) !== String(productId))]; }
        else state.catalog = state.catalog.filter(product => String(product.id) !== String(productId));
        renderSellerProducts(); renderCategoryPills(); renderCatalog(); toast(status === "ACTIVE" ? "Ürün vitrinde yayınlandı." : "Ürün satıştan kaldırıldı.", "success");
    } catch (error) { toast(error.message, "error"); setBusy(button, false); }
}
function requestProductDelete(productId) {
    const product = state.sellerProducts.find(item => String(item.id) === String(productId));
    if (product) askConfirmation("Ürün silinsin mi?", `${product.name} vitrinden ve ürün listenizden kaldırılacak. Geçmiş sipariş kayıtları korunur.`, "Ürünü sil", () => deleteSellerProduct(product));
}
async function deleteSellerProduct(product) {
    await api(`/api/v1/products/${product.id}`, {method: "DELETE"});
    state.sellerProducts = state.sellerProducts.filter(item => String(item.id) !== String(product.id)); state.catalog = state.catalog.filter(item => String(item.id) !== String(product.id));
    state.favorites.delete(String(product.id)); saveFavorites(); updateFavoritesUI();
    for (const item of state.cart.filter(item => String(item.id) === String(product.id))) {
        if (state.token && item.cartItemId) { try { applyCartResponse(await api(`/api/v1/cart/items/${item.cartItemId}`, {method: "DELETE"})); } catch { /* Cart may have expired. */ } }
        else state.cart = state.cart.filter(candidate => candidate !== item);
    }
    saveCart(); renderCart(); renderSellerProducts(); renderCategoryPills(); renderCatalog(); toast(`${product.name} güvenle silindi.`, "success");
}

function askConfirmation(title, text, actionLabel, action) {
    state.confirmAction = action; $("#confirmTitle").textContent = title; $("#confirmText").textContent = text; $("#confirmActionButton").textContent = actionLabel; $("#confirmModal").showModal();
}
async function runConfirmedAction() {
    const button = $("#confirmActionButton"), action = state.confirmAction;
    if (!action) return $("#confirmModal").close();
    setBusy(button, true, "İşleniyor");
    try { await action(); $("#confirmModal").close(); } catch (error) { toast(error.message, "error"); }
    finally { state.confirmAction = null; setBusy(button, false); }
}

async function checkout() {
    if (!state.token) { openModal("authModal"); $("#authMessage").textContent = "Sipariş oluşturmak için giriş yapmalısın."; return; }
    if (state.user?.role !== "CUSTOMER") { toast("Yalnızca CUSTOMER hesapları sipariş verebilir.", "error"); return; }
    const button = $("#checkoutButton"); setBusy(button, true, "Sipariş hazırlanıyor");
    try {
        const order = await api("/api/v1/cart/items/checkout", {method: "POST", body: JSON.stringify({promotionCodes: state.coupon ? [state.coupon] : []})});
        state.lastOrder = {id: order.orderId, total: state.cart.reduce((sum, item) => sum + item.price * item.quantity, 0), itemCount: state.cart.reduce((sum, item) => sum + item.quantity, 0)};
        $("#orderRecap").innerHTML = `<div><span>Ürünler</span><b>${state.lastOrder.itemCount} ürün</b></div><div><span>Sipariş no</span><b>${html(state.lastOrder.id.slice(0, 8).toUpperCase())}</b></div><div class="recap-total"><span>Ödenecek tutar</span><strong>${currency(state.lastOrder.total)}</strong></div>`;
        $("#paymentPanel").hidden = false; $("#successPanel").hidden = true; $("#checkoutMessage").textContent = ""; $("#checkoutDescription").textContent = "Siparişin oluşturuldu. Şimdi güvenli ödemeyi tamamla."; openModal("checkoutModal");
    } catch (error) { toast(error.message, "error"); } finally { setBusy(button, false); }
}
async function pay() {
    if (!state.lastOrder) return;
    const button = $("#payButton"); setBusy(button, true, "Ödeme işleniyor"); $("#checkoutMessage").textContent = "";
    try {
        const payment = await api("/api/v1/payments", {method: "POST", body: JSON.stringify({orderId: state.lastOrder.id, idempotencyKey: `web-${crypto.randomUUID()}`, walletAmount: 0, cardAmount: state.lastOrder.total.toFixed(2)})});
        if (payment.providerPaymentId) await api(`/mock-payment-provider/payments/${payment.providerPaymentId}/outcomes`, {method: "POST", headers: {Authorization: ""}, body: JSON.stringify({status: "SUCCEEDED", failureReason: null, callbackDelaySeconds: 0, duplicateDeliveries: 1})});
        $("#paymentPanel").hidden = true; $("#successPanel").hidden = false; state.cart = []; saveCart(); renderCart();
    } catch (error) { $("#checkoutMessage").textContent = error.message; } finally { setBusy(button, false); }
}

function clearFilters() {
    state.activeCategory = "all"; state.favoriteOnly = false; $("#searchInput").value = ""; $("#sortSelect").value = "featured"; state.sort = "featured";
    $("#globalSearchInput").value = "";
    renderCategoryPills(); updateFavoritesUI(); renderCatalog();
}
function shopCategory(category) {
    const available = [...new Set(state.catalog.map(product => product.category))];
    state.activeCategory = category === "all" ? "all" : (available.find(item => item.toLocaleLowerCase("tr").includes(category.toLocaleLowerCase("tr"))) || "all");
    state.favoriteOnly = false; updateFavoritesUI(); renderCategoryPills(); renderCatalog(); $("#discover").scrollIntoView({behavior: "smooth"});
}
function initEvents() {
    $("#cartButton").addEventListener("click", openCart); $$('[data-close-cart]').forEach(button => button.addEventListener("click", closeCart)); $("#overlay").addEventListener("click", closeCart);
    $$('[data-open-seller]').forEach(button => button.addEventListener("click", openSellerArea)); $$('[data-close-modal]').forEach(button => button.addEventListener("click", closeModals));
    $("#authButton").addEventListener("click", event => { event.stopPropagation(); toggleAccountMenu(); }); $("#accountOverviewButton").addEventListener("click", () => openAccount("overview"));
    $("#ordersReturnsButton").addEventListener("click", () => openAccount("orders")); $("#footerReturnsButton").addEventListener("click", () => openAccount("returns"));
    $$("[data-account-tab]").forEach(button => button.addEventListener("click", () => switchAccountTab(button.dataset.accountTab)));
    $$("[data-account-target]").forEach(button => button.addEventListener("click", () => switchAccountTab(button.dataset.accountTarget)));
    $("#refreshOrdersButton").addEventListener("click", loadOrders); $("#refreshReturnsButton").addEventListener("click", loadCustomerReturns);
    $("#returnRequestForm").addEventListener("submit", submitReturnRequest); $("#returnManagementButton").addEventListener("click", openReturnManagement); $("#refreshManageableReturnsButton").addEventListener("click", loadManageableReturns);
    $("#courierAreaButton").addEventListener("click", openCourierArea); $("#refreshCourierButton").addEventListener("click", loadCourierOrders);
    $("#adminPanelButton").addEventListener("click", openAdminPanel); $("#refreshAdminUsersButton").addEventListener("click", loadAdminUsers); $("#adminUserForm").addEventListener("submit", submitAdminUser);
    $("#logoutButton").addEventListener("click", logout); $("#headerLogoutButton").addEventListener("click", logout);
    $("#favoritesButton").addEventListener("click", () => { state.favoriteOnly = !state.favoriteOnly; updateFavoritesUI(); renderCatalog(); $("#discover").scrollIntoView({behavior: "smooth"}); });
    $$("[data-auth-tab]").forEach(button => button.addEventListener("click", () => setAuthMode(button.dataset.authTab)));
    $("#resendVerificationButton").addEventListener("click", resendVerification);
    $("#verificationLoginButton").addEventListener("click", () => { setAuthMode("login"); $("#authEmail").value = state.pendingVerificationEmail; });
    $("#authForm").addEventListener("submit", submitAuth); $("#sellerForm").addEventListener("submit", submitSeller); $("#refreshSellerButton").addEventListener("click", loadSellerProducts);
    $("#checkoutButton").addEventListener("click", checkout); $("#payButton").addEventListener("click", pay);
    $("#detailAddButton").addEventListener("click", () => state.selectedProduct && addToCart(state.selectedProduct.id, $("#detailAddButton")));
    $("#detailFavoriteButton").addEventListener("click", () => state.selectedProduct && toggleFavorite(state.selectedProduct.id));
    $("#confirmCancelButton").addEventListener("click", () => { state.confirmAction = null; $("#confirmModal").close(); }); $("#confirmActionButton").addEventListener("click", runConfirmedAction);
    $("#applyCouponButton").addEventListener("click", () => { const code = $("#couponInput").value.trim().toUpperCase(); state.coupon = code; $("#couponInput").value = code; $("#couponNote").textContent = code ? `${code} ödeme adımında uygulanacak.` : ""; });
    $("#emptySeedButton").addEventListener("click", seedCatalog); $("#refreshButton").addEventListener("click", loadProducts); $("#searchInput").addEventListener("input", renderCatalog);
    $("#globalSearchInput").addEventListener("input", event => { $("#searchInput").value = event.target.value; renderCatalog(); });
    $("#globalSearchInput").addEventListener("keydown", event => { if (event.key === "Enter") $("#discover").scrollIntoView({behavior: "smooth"}); });
    $("#searchInput").addEventListener("input", event => { $("#globalSearchInput").value = event.target.value; });
    $$('[data-shop-category]').forEach(button => button.addEventListener("click", () => shopCategory(button.dataset.shopCategory)));
    $$('[data-campaign-category]').forEach(button => button.addEventListener("click", () => shopCategory(button.dataset.campaignCategory)));
    document.addEventListener("keydown", event => { if ((event.metaKey || event.ctrlKey) && event.key.toLocaleLowerCase("tr") === "k") { event.preventDefault(); $("#globalSearchInput").focus(); } });
    $("#sortSelect").addEventListener("change", event => { state.sort = event.target.value; renderCatalog(); }); $("#clearFiltersButton").addEventListener("click", clearFilters);
    document.addEventListener("click", event => { if (!event.target.closest(".account-shell")) closeAccountMenu(); });
}

async function boot() {
    initEvents(); renderCart(); updateAuthUI(); updateFavoritesUI(); renderCatalog();
    if (new URLSearchParams(window.location.search).get("verification") === "success") {
        window.history.replaceState({}, document.title, window.location.pathname);
        setAuthMode("login");
        openModal("authModal");
        setAuthMessage("E-posta adresin doğrulandı. Artık güvenle giriş yapabilirsin.", true);
        setTimeout(() => toast("E-posta adresin başarıyla doğrulandı.", "success"), 200);
    }
    await hydrateUser(); await loadProducts(); await syncCart();
}
boot();
