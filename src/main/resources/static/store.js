const state = {
    token: localStorage.getItem("nexa_access_token") || "",
    user: JSON.parse(localStorage.getItem("nexa_user") || "null"),
    catalog: [],
    cart: JSON.parse(localStorage.getItem("nexa_cart") || "[]"),
    orders: JSON.parse(localStorage.getItem("nexa_orders") || "[]"),
    authMode: "login",
    activeCategory: "all",
    coupon: "",
    lastOrder: null
};

const demoProducts = [
    {name: "Nova Kablosuz Kulaklık", category: "Teknoloji", price: 1499.90, stock: 14, sku: "NEXA-NOVA-001", emoji: "◖", description: "Aktif gürültü engelleme ve 30 saat pil ömrü."},
    {name: "Kum Seramik Fincan", category: "Yaşam", price: 349.90, stock: 18, sku: "NEXA-KUM-002", emoji: "◒", description: "El yapımı, günlük ritüeller için tasarlandı."},
    {name: "Luma Masa Lambası", category: "Ev", price: 899.90, stock: 9, sku: "NEXA-LUMA-003", emoji: "◉", description: "Yumuşak ışığıyla çalışma alanına sakinlik katar."},
    {name: "Terra Günlük Çanta", category: "Yaşam", price: 1199.90, stock: 7, sku: "NEXA-TERRA-004", emoji: "◡", description: "Şehir hayatına uyumlu, hafif ve dayanıklı."}
];

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];
const currency = (value) => new Intl.NumberFormat("tr-TR", {style: "currency", currency: "TRY"}).format(Number(value || 0));
const html = (value) => String(value ?? "").replace(/[&<>'"]/g, char => ({"&":"&amp;","<":"&lt;",">":"&gt;","'":"&#39;","\"":"&quot;"}[char]));

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
    localStorage.setItem("nexa_access_token", state.token);
    localStorage.setItem("nexa_user", JSON.stringify(state.user));
}

function saveCart() {
    localStorage.setItem("nexa_cart", JSON.stringify(state.cart));
}

function saveOrders() {
    localStorage.setItem("nexa_orders", JSON.stringify(state.orders));
}

function toast(message) {
    const target = $("#toast");
    target.textContent = message;
    target.classList.add("show");
    clearTimeout(window.toastTimer);
    window.toastTimer = setTimeout(() => target.classList.remove("show"), 3400);
}

function emojiFor(name = "") {
    const lower = name.toLocaleLowerCase("tr");
    if (lower.includes("kulak") || lower.includes("teknoloji") || lower.includes("şarj")) return "◖";
    if (lower.includes("lamba") || lower.includes("ışık")) return "◉";
    if (lower.includes("çanta")) return "◡";
    if (lower.includes("fincan") || lower.includes("seramik")) return "◒";
    if (lower.includes("bitki")) return "✿";
    return "✦";
}

function normaliseProduct(product) {
    const category = product.categoryNames?.[0] || product.categories?.[0]?.name || "Seçki";
    const variant = product.variants?.[0] || null;
    return {
        id: product.id,
        sellerId: product.sellerId,
        name: product.name,
        description: product.description || "NexaMarket seçkisinden özenle seçildi.",
        category,
        price: Number(product.minPrice ?? product.basePrice ?? variant?.price ?? 0),
        inStock: product.inStock ?? Number(product.totalStock ?? variant?.stockQuantity ?? 0) > 0,
        stock: Number(product.totalStock ?? variant?.stockQuantity ?? 0),
        variantId: variant?.id || null,
        emoji: emojiFor(`${product.name} ${category}`)
    };
}

function productsForView() {
    const query = $("#searchInput").value.trim().toLocaleLowerCase("tr");
    return state.catalog.filter(product => {
        const matchesCategory = state.activeCategory === "all" || product.category.toLocaleLowerCase("tr") === state.activeCategory.toLocaleLowerCase("tr");
        const haystack = `${product.name} ${product.category} ${product.description}`.toLocaleLowerCase("tr");
        return matchesCategory && (!query || haystack.includes(query));
    });
}

function renderCatalog() {
    const products = productsForView();
    const grid = $("#productGrid");
    $("#productCount").textContent = state.catalog.length ? `${products.length} ürün gösteriliyor` : "Katalog henüz boş";
    $("#emptyCatalog").hidden = state.catalog.length !== 0;
    grid.innerHTML = products.map(product => `
        <article class="product-card">
            <div class="product-image"><span class="tag">${html(product.category).toUpperCase()}</span><span>${product.emoji}</span></div>
            <div class="product-info"><p>${product.inStock ? `${product.stock || ""} adet stokta` : "Stokta yok"}</p><h3>${html(product.name)}</h3>
                <div class="product-bottom"><strong class="price">${currency(product.price)}</strong><button class="add-button" data-add-product="${product.id}" aria-label="${html(product.name)} sepete ekle" ${product.inStock ? "" : "disabled"}>+</button></div>
            </div>
        </article>`).join("");
    $$('[data-add-product]').forEach(button => button.addEventListener("click", () => addToCart(button.dataset.addProduct)));
}

async function loadProducts() {
    $("#productCount").textContent = "Katalog yenileniyor…";
    try {
        const response = await api("/api/v1/products/search?page=0&size=48", {headers: {Authorization: ""}});
        const searched = (response.items || []).map(normaliseProduct);
        const locallyCreated = state.catalog.filter(product => !searched.some(item => String(item.id) === String(product.id)));
        state.catalog = [...searched, ...locallyCreated];
    } catch (error) {
        toast("Katalog şu an yüklenemedi. Sunucunun açık olduğundan emin ol.");
    }
    renderCatalog();
}

async function resolveProduct(product) {
    if (product.variantId) return product;
    const detail = await api(`/api/v1/products/${product.id}`, {headers: {Authorization: ""}});
    const resolved = normaliseProduct(detail);
    Object.assign(product, resolved);
    return product;
}

async function addToCart(productId) {
    const product = state.catalog.find(item => String(item.id) === String(productId));
    if (!product) return;
    if (!state.token) {
        openModal("authModal");
        $("#authMessage").textContent = "Bu ürünü sepete eklemek için önce giriş yapmalısın.";
        return;
    }
    try {
        await resolveProduct(product);
        if (!product.variantId) throw new Error("Bu ürünün sipariş edilebilir bir varyantı bulunamadı.");
        await api("/api/v1/cart/items", {method: "POST", body: JSON.stringify({productVariantId: product.variantId, quantity: 1})});
        const existing = state.cart.find(item => String(item.id) === String(product.id));
        if (existing) existing.quantity += 1;
        else state.cart.push({...product, quantity: 1});
        saveCart(); renderCart(); toast(`${product.name} sepete eklendi.`);
    } catch (error) { toast(error.message); }
}

function renderCart() {
    const total = state.cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
    $("#cartCount").textContent = state.cart.reduce((sum, item) => sum + item.quantity, 0);
    $("#cartItems").innerHTML = state.cart.map(item => `<div class="cart-row"><div class="cart-row-art">${item.emoji}</div><div><h3>${html(item.name)}</h3><p>${item.quantity} adet · ${currency(item.price)}</p></div><div><strong>${currency(item.price * item.quantity)}</strong><button class="remove-item" data-remove-product="${item.id}" aria-label="${html(item.name)} ürünü sil">×</button></div></div>`).join("");
    $("#cartTotal").textContent = currency(total);
    $("#cartEmpty").hidden = state.cart.length > 0;
    $("#cartSummary").hidden = state.cart.length === 0;
    $$('[data-remove-product]').forEach(button => button.addEventListener("click", () => {
        state.cart = state.cart.filter(item => String(item.id) !== String(button.dataset.removeProduct)); saveCart(); renderCart();
    }));
}

function openCart() { $("#cartDrawer").classList.add("open"); $("#cartDrawer").setAttribute("aria-hidden", "false"); $("#overlay").hidden = false; }
function closeCart() { $("#cartDrawer").classList.remove("open"); $("#cartDrawer").setAttribute("aria-hidden", "true"); $("#overlay").hidden = true; }
function openModal(id) { closeCart(); const modal = document.getElementById(id); modal.showModal(); }
function closeModals() { $$("dialog[open]").forEach(dialog => dialog.close()); }

function updateAuthUI() {
    $("#authButton").textContent = state.token ? `${state.user?.email?.split("@")[0] || "Hesabım"} · Hesabım` : "Giriş yap";
}

async function openAccount() {
    if (!state.token) return openModal("authModal");
    $("#accountName").textContent = `Merhaba, ${state.user?.email?.split("@")[0] || "NexaMarketli"}`;
    $("#orderCount").textContent = state.orders.length;
    $("#recentOrders").innerHTML = state.orders.length ? state.orders.slice(0, 3).map(order => `<div class="recent-order"><div><b>#${html(order.id.slice(0, 8).toUpperCase())}</b><small>${html(order.date)}</small></div><strong>${currency(order.total)}</strong></div>`).join("") : "<span>Henüz bir siparişin yok.</span>";
    try { const loyalty = await api("/api/v1/loyalty/me"); $("#loyaltyPoints").textContent = loyalty.points ?? 0; } catch { $("#loyaltyPoints").textContent = "0"; }
    openModal("accountModal");
}

function logout() {
    state.token = ""; state.user = null; saveSession(); updateAuthUI(); closeModals(); toast("Güvenle çıkış yaptın.");
}

async function hydrateUser() {
    if (!state.token) return;
    try { state.user = await api("/api/v1/auth/me"); saveSession(); updateAuthUI(); }
    catch { state.token = ""; state.user = null; saveSession(); updateAuthUI(); }
}

function setAuthMode(mode) {
    state.authMode = mode;
    $$("[data-auth-tab]").forEach(button => button.classList.toggle("active", button.dataset.authTab === mode));
    $("#authTitle").textContent = mode === "login" ? "Alışverişe başla" : "Hesabını oluştur";
    $("#authSubtitle").textContent = mode === "login" ? "Sepete eklemek ve sipariş vermek için giriş yap." : "NexaMarket deneyimine birkaç saniyede katıl.";
    $("#authSubmit").innerHTML = mode === "login" ? "Giriş yap <span>→</span>" : "Hesap oluştur <span>→</span>";
    $("#authMessage").textContent = "";
}

async function submitAuth(event) {
    event.preventDefault();
    const email = $("#authEmail").value.trim(); const password = $("#authPassword").value;
    const message = $("#authMessage"); message.textContent = "İşleniyor…";
    try {
        if (state.authMode === "register") await api("/api/v1/auth/register", {method: "POST", body: JSON.stringify({email, password})});
        const login = await api("/api/v1/auth/login", {method: "POST", body: JSON.stringify({email, password})});
        state.token = login.accessToken; state.user = await api("/api/v1/auth/me"); saveSession(); updateAuthUI(); closeModals(); toast("Hoş geldin! Artık sipariş verebilirsin.");
    } catch (error) { message.textContent = error.message; }
}

async function ensureCategory(name) {
    const categories = await api("/api/v1/categories", {headers: {Authorization: ""}});
    const existing = categories.find(category => category.name.toLocaleLowerCase("tr") === name.toLocaleLowerCase("tr"));
    if (existing) return existing.id;
    const created = await api("/api/v1/categories", {method: "POST", headers: {Authorization: ""}, body: JSON.stringify({name, description: `${name} seçkisi`, parentCategoryId: null})});
    return created.id;
}

async function createProduct(payload, silent = false) {
    const categoryId = await ensureCategory(payload.category);
    const product = await api("/api/v1/products", {method: "POST", headers: {"X-Seller-Id": String(payload.sellerId || 1), Authorization: ""}, body: JSON.stringify({
        name: payload.name, description: payload.description, basePrice: payload.price, categoryIds: [categoryId], variants: [{sku: payload.sku, attributes: {"Seçenek": "Standart"}, price: payload.price, stockQuantity: payload.stock}]
    })});
    const normalised = normaliseProduct(product);
    state.catalog = [normalised, ...state.catalog.filter(item => String(item.id) !== String(normalised.id))];
    renderCatalog();
    if (!silent) toast("Ürün vitrine eklendi.");
    return normalised;
}

async function seedCatalog() {
    const button = $("#seedButton"); button.disabled = true; button.textContent = "Mağaza hazırlanıyor…";
    $("#emptySeedButton").disabled = true;
    try {
        for (const product of demoProducts) {
            try { await createProduct({...product, sellerId: 1}, true); } catch (error) {
                if (!error.message.includes("SKU zaten")) throw error;
            }
        }
        await loadProducts(); toast("Mağaza keşfe hazır. Beğendiğin ürünleri sepete ekleyebilirsin.");
    } catch (error) { toast(error.message); }
    finally { button.disabled = false; button.textContent = "Demo mağazayı hazırla"; $("#emptySeedButton").disabled = false; }
}

async function submitSeller(event) {
    event.preventDefault(); const message = $("#sellerMessage"); message.textContent = "Ürün ekleniyor…";
    try {
        const product = await createProduct({name: $("#sellerProductName").value.trim(), category: $("#sellerCategory").value.trim(), price: Number($("#sellerPrice").value), stock: Number($("#sellerStock").value), sku: $("#sellerSku").value.trim(), sellerId: Number($("#sellerId").value), description: $("#sellerDescription").value.trim() || "NexaMarket satıcısından yeni ürün."});
        message.textContent = ""; closeModals(); toast(`${product.name} mağazaya eklendi.`);
    } catch (error) { message.textContent = error.message; }
}

async function checkout() {
    if (!state.token) { openModal("authModal"); $("#authMessage").textContent = "Sipariş oluşturmak için giriş yapmalısın."; return; }
    try {
        const order = await api("/api/v1/cart/items/checkout", {method: "POST", body: JSON.stringify({promotionCodes: state.coupon ? [state.coupon] : []})});
        state.lastOrder = {id: order.orderId, total: state.cart.reduce((sum, item) => sum + item.price * item.quantity, 0), itemCount: state.cart.reduce((sum, item) => sum + item.quantity, 0)};
        $("#orderRecap").innerHTML = `<div><span>Ürünler</span><b>${state.lastOrder.itemCount} ürün</b></div><div><span>Sipariş no</span><b>${html(state.lastOrder.id.slice(0, 8).toUpperCase())}</b></div><div class="recap-total"><span>Ödenecek tutar</span><strong>${currency(state.lastOrder.total)}</strong></div>`;
        $("#paymentPanel").hidden = false; $("#successPanel").hidden = true; $("#checkoutMessage").textContent = ""; $("#checkoutDescription").textContent = "Siparişin oluşturuldu. Şimdi güvenli ödemeyi tamamla."; openModal("checkoutModal");
    } catch (error) { toast(error.message); }
}

async function pay() {
    if (!state.lastOrder) return;
    const message = $("#checkoutMessage"); message.textContent = "Ödeme güvenle işleniyor…";
    try {
        const payment = await api("/api/v1/payments", {method: "POST", body: JSON.stringify({orderId: state.lastOrder.id, idempotencyKey: `web-${crypto.randomUUID()}`, walletAmount: 0, cardAmount: state.lastOrder.total.toFixed(2)})});
        if (payment.providerPaymentId) await api(`/mock-payment-provider/payments/${payment.providerPaymentId}/outcomes`, {method: "POST", headers: {Authorization: ""}, body: JSON.stringify({status: "SUCCEEDED", failureReason: null, callbackDelaySeconds: 0, duplicateDeliveries: 1})});
        state.orders.unshift({id: state.lastOrder.id, total: state.lastOrder.total, date: new Intl.DateTimeFormat("tr-TR", {day:"numeric", month:"long"}).format(new Date())}); saveOrders();
        $("#paymentPanel").hidden = true; $("#successPanel").hidden = false; message.textContent = ""; state.cart = []; saveCart(); renderCart();
    } catch (error) { message.textContent = error.message; }
}

function initEvents() {
    $("#cartButton").addEventListener("click", openCart); $$('[data-close-cart]').forEach(button => button.addEventListener("click", closeCart)); $("#overlay").addEventListener("click", closeCart);
    $$('[data-open-seller]').forEach(button => button.addEventListener("click", () => openModal("sellerModal")));
    $$('[data-close-modal]').forEach(button => button.addEventListener("click", closeModals));
    $("#authButton").addEventListener("click", openAccount); $("#logoutButton").addEventListener("click", logout);
    $$("[data-auth-tab]").forEach(button => button.addEventListener("click", () => setAuthMode(button.dataset.authTab)));
    $("#authForm").addEventListener("submit", submitAuth); $("#sellerForm").addEventListener("submit", submitSeller); $("#checkoutButton").addEventListener("click", checkout); $("#payButton").addEventListener("click", pay);
    $("#applyCouponButton").addEventListener("click", () => { const code = $("#couponInput").value.trim().toUpperCase(); state.coupon = code; $("#couponInput").value = code; $("#couponNote").textContent = code ? `${code} ödeme adımında uygulanacak.` : ""; });
    $("#seedButton").addEventListener("click", seedCatalog); $("#emptySeedButton").addEventListener("click", seedCatalog); $("#refreshButton").addEventListener("click", loadProducts);
    $("#searchInput").addEventListener("input", renderCatalog);
    $$(".pill").forEach(button => button.addEventListener("click", () => { state.activeCategory = button.dataset.category; $$(".pill").forEach(item => item.classList.toggle("active", item === button)); renderCatalog(); }));
}

async function boot() {
    initEvents(); renderCart(); updateAuthUI(); await hydrateUser(); await loadProducts();
}

boot();
