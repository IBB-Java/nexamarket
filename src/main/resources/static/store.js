const state = {
    token: localStorage.getItem("nexa_access_token") || "",
    refreshToken: localStorage.getItem("nexa_refresh_token") || "",
    user: JSON.parse(localStorage.getItem("nexa_user") || "null"),
    catalog: [], cart: JSON.parse(localStorage.getItem("nexa_cart") || "[]"),
    orders: JSON.parse(localStorage.getItem("nexa_orders") || "[]"),
    favorites: new Set(JSON.parse(localStorage.getItem("nexa_favorites") || "[]").map(String)),
    sellerProducts: [], authMode: "login", activeCategory: "all", sort: "featured",
    favoriteOnly: false, coupon: "", lastOrder: null, selectedProduct: null,
    catalogLoading: true, confirmAction: null
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
function saveOrders() { localStorage.setItem("nexa_orders", JSON.stringify(state.orders)); }
function saveFavorites() { localStorage.setItem("nexa_favorites", JSON.stringify([...state.favorites])); }

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
    return {id: product.id, sellerId: product.sellerId, name: product.name,
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
        const haystack = `${product.name} ${product.category} ${product.description}`.toLocaleLowerCase("tr");
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
                <button class="product-name" data-view-product="${product.id}">${html(product.name)}</button><p class="product-excerpt">${html(product.description)}</p>
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
    Object.assign(product, normaliseProduct(await api(`/api/v1/products/${product.id}`, {headers: {Authorization: ""}})));
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
    if (state.user?.role === "SELLER") { state.cart = []; saveCart(); renderCart(); return; }
    try { applyCartResponse(cart || await api("/api/v1/cart/items")); } catch { toast("Sepetin şu an güncellenemedi.", "error"); }
}

async function addToCart(productId, button = null) {
    const product = state.catalog.find(item => String(item.id) === String(productId));
    if (!product) return;
    if (!state.token) { openModal("authModal"); $("#authMessage").textContent = "Sepete eklemek için önce giriş yapmalısın."; return; }
    if (state.user?.role === "SELLER") { toast("SELLER hesapları ürün satın alamaz.", "error"); return; }
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
}
async function openAccount() {
    if (!state.token) return openModal("authModal");
    $("#accountName").textContent = `Merhaba, ${state.user?.email?.split("@")[0] || "NexaMarketli"}`; $("#orderCount").textContent = state.orders.length;
    $("#recentOrders").innerHTML = state.orders.length ? state.orders.slice(0, 3).map(order => `<div class="recent-order"><div><b>#${html(order.id.slice(0, 8).toUpperCase())}</b><small>${html(order.date)}</small></div><strong>${currency(order.total)}</strong></div>`).join("") : "<span>Henüz bir siparişin yok.</span>";
    try { $("#loyaltyPoints").textContent = (await api("/api/v1/loyalty/me")).points ?? 0; } catch { $("#loyaltyPoints").textContent = "0"; }
    openModal("accountModal");
}
function clearLocalSession() {
    state.token = ""; state.refreshToken = ""; state.user = null; state.cart = [];
    saveSession(); saveCart(); renderCart(); updateAuthUI(); closeModals();
}
async function logout() {
    const refreshToken = state.refreshToken;
    try { if (refreshToken) await api("/api/v1/auth/logout", {method: "POST", body: JSON.stringify({refreshToken})}); }
    catch { /* The local session is still cleared if the server token has expired. */ }
    finally { clearLocalSession(); toast("Hesabından güvenle çıkış yaptın.", "success"); }
}
async function hydrateUser() {
    if (!state.token) return;
    try { state.user = await api("/api/v1/auth/me"); saveSession(); } catch { clearLocalSession(); }
    updateAuthUI();
}

function setAuthMode(mode) {
    state.authMode = mode; $$("[data-auth-tab]").forEach(button => button.classList.toggle("active", button.dataset.authTab === mode));
    $("#authTitle").textContent = mode === "login" ? "Alışverişe başla" : "Hesabını oluştur";
    $("#authSubtitle").textContent = mode === "login" ? "Sepete eklemek ve sipariş vermek için giriş yap." : "NexaMarket deneyimine birkaç saniyede katıl.";
    $("#authSubmit").innerHTML = mode === "login" ? "Giriş yap <span>→</span>" : "Hesap oluştur <span>→</span>"; $("#authMessage").textContent = "";
}
async function submitAuth(event) {
    event.preventDefault(); const email = $("#authEmail").value.trim(), password = $("#authPassword").value, submit = $("#authSubmit");
    $("#authMessage").textContent = ""; setBusy(submit, true, state.authMode === "login" ? "Giriş yapılıyor" : "Hesap oluşturuluyor");
    try {
        if (state.authMode === "register") await api("/api/v1/auth/register", {method: "POST", body: JSON.stringify({email, password})});
        const login = await api("/api/v1/auth/login", {method: "POST", body: JSON.stringify({email, password})});
        state.token = login.accessToken; state.refreshToken = login.refreshToken || ""; state.user = await api("/api/v1/auth/me");
        saveSession(); updateAuthUI(); closeModals(); await syncCart(); toast("Hoş geldin! Alışverişe devam edebilirsin.", "success");
    } catch (error) { $("#authMessage").textContent = error.message; } finally { setBusy(submit, false); }
}

async function openSellerArea() {
    if (!state.token) { openModal("authModal"); $("#authMessage").textContent = "Ürün eklemek için önce giriş yapmalısın."; return; }
    if (state.user?.role !== "SELLER") { toast("Ürün eklemek için ADMIN tarafından SELLER rolüne yükseltilmelisin.", "error"); return; }
    openModal("sellerModal"); await loadSellerProducts();
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
    if (state.user?.role === "SELLER") { toast("SELLER hesapları sipariş veremez.", "error"); return; }
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
        state.orders.unshift({id: state.lastOrder.id, total: state.lastOrder.total, date: new Intl.DateTimeFormat("tr-TR", {day:"numeric", month:"long"}).format(new Date())}); saveOrders();
        $("#paymentPanel").hidden = true; $("#successPanel").hidden = false; state.cart = []; saveCart(); renderCart();
    } catch (error) { $("#checkoutMessage").textContent = error.message; } finally { setBusy(button, false); }
}

function clearFilters() {
    state.activeCategory = "all"; state.favoriteOnly = false; $("#searchInput").value = ""; $("#sortSelect").value = "featured"; state.sort = "featured";
    renderCategoryPills(); updateFavoritesUI(); renderCatalog();
}
function initEvents() {
    $("#cartButton").addEventListener("click", openCart); $$('[data-close-cart]').forEach(button => button.addEventListener("click", closeCart)); $("#overlay").addEventListener("click", closeCart);
    $$('[data-open-seller]').forEach(button => button.addEventListener("click", openSellerArea)); $$('[data-close-modal]').forEach(button => button.addEventListener("click", closeModals));
    $("#authButton").addEventListener("click", event => { event.stopPropagation(); toggleAccountMenu(); }); $("#accountOverviewButton").addEventListener("click", openAccount);
    $("#logoutButton").addEventListener("click", logout); $("#headerLogoutButton").addEventListener("click", logout);
    $("#favoritesButton").addEventListener("click", () => { state.favoriteOnly = !state.favoriteOnly; updateFavoritesUI(); renderCatalog(); $("#discover").scrollIntoView({behavior: "smooth"}); });
    $$("[data-auth-tab]").forEach(button => button.addEventListener("click", () => setAuthMode(button.dataset.authTab)));
    $("#authForm").addEventListener("submit", submitAuth); $("#sellerForm").addEventListener("submit", submitSeller); $("#refreshSellerButton").addEventListener("click", loadSellerProducts);
    $("#checkoutButton").addEventListener("click", checkout); $("#payButton").addEventListener("click", pay);
    $("#detailAddButton").addEventListener("click", () => state.selectedProduct && addToCart(state.selectedProduct.id, $("#detailAddButton")));
    $("#detailFavoriteButton").addEventListener("click", () => state.selectedProduct && toggleFavorite(state.selectedProduct.id));
    $("#confirmCancelButton").addEventListener("click", () => { state.confirmAction = null; $("#confirmModal").close(); }); $("#confirmActionButton").addEventListener("click", runConfirmedAction);
    $("#applyCouponButton").addEventListener("click", () => { const code = $("#couponInput").value.trim().toUpperCase(); state.coupon = code; $("#couponInput").value = code; $("#couponNote").textContent = code ? `${code} ödeme adımında uygulanacak.` : ""; });
    $("#emptySeedButton").addEventListener("click", seedCatalog); $("#refreshButton").addEventListener("click", loadProducts); $("#searchInput").addEventListener("input", renderCatalog);
    $("#sortSelect").addEventListener("change", event => { state.sort = event.target.value; renderCatalog(); }); $("#clearFiltersButton").addEventListener("click", clearFilters);
    document.addEventListener("click", event => { if (!event.target.closest(".account-shell")) closeAccountMenu(); });
}

async function boot() {
    initEvents(); renderCart(); updateAuthUI(); updateFavoritesUI(); renderCatalog();
    await hydrateUser(); await loadProducts(); await syncCart();
}
boot();
