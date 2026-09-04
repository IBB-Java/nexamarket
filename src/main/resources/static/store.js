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
    sellerProducts: [], sellerCategories: [], sellerImagePreviewUrl: "", sellerOrders: [], courierOrders: [], adminUsers: [], adminOrders: [], adminDeliveries: [], authMode: "login", activeCategory: "all", sort: "featured",
    favoriteOnly: false, coupon: "", lastOrder: null, selectedProduct: null, courierFilter: "all",
    catalogLoading: true, confirmAction: null, pendingVerificationEmail: ""
};

const demoProducts = [
    {name: "Nova Kablosuz Kulaklık", category: "Teknoloji", price: 1499.90, stock: 14, sku: "NEXA-NOVA-001", description: "Aktif gürültü engelleme ve 30 saat pil ömrü."},
    {name: "Kum Seramik Fincan", category: "Yaşam", price: 349.90, stock: 18, sku: "NEXA-KUM-002", description: "El yapımı, günlük ritüeller için tasarlandı."},
    {name: "Luma Masa Lambası", category: "Ev", price: 899.90, stock: 9, sku: "NEXA-LUMA-003", description: "Yumuşak ışığıyla çalışma alanına sakinlik katar."},
    {name: "Terra Günlük Çanta", category: "Yaşam", price: 1199.90, stock: 7, sku: "NEXA-TERRA-004", description: "Şehir hayatına uyumlu, hafif ve dayanıklı."}
];

const curatedProductImages = Object.freeze({
    "Nova Kablosuz Kulaklık": "/images/products/nova-kablosuz-kulaklik.png",
    "Kum Seramik Fincan": "/images/products/kum-seramik-fincan.png",
    "Luma Masa Lambası": "/images/products/luma-masa-lambasi.png",
    "Terra Günlük Çanta": "/images/products/terra-gunluk-canta.png",
    "telefon": "/images/products/telefon.png",
    "Aria Paslanmaz Su Matarası": "/images/products/aria-su-matarasi.png",
    "Mira Keten Defter": "/images/products/mira-keten-defter.png",
    "Solis Bluetooth Hoparlör": "/images/products/solis-hoparlor.png",
    "Orbit Masa Saati": "/images/products/orbit-masa-saati.png",
    "Pera Mum Seti": "/images/products/pera-mum-seti.png",
    "Atlas Sırt Çantası": "/images/products/atlas-sirt-cantasi.png",
    "Lento Kablosuz Şarj Standı": "/images/products/lento-kablosuz-sarj-standi.png",
    "Noma Cam Sürahi": "/images/products/noma-cam-surahi.png",
    "Vela Bitki Saksısı": "/images/products/vela-bitki-saksisi.png",
    "Riva Yoga Matı": "/images/products/riva-yoga-mati.png",
    "Pixel Mekanik Klavye": "/images/products/pixel-mekanik-klavye.png",
    "Sera Pamuklu Battaniye": "/images/products/sera-pamuklu-battaniye.png",
    "Eko Ahşap Kesme Tahtası": "/images/products/eko-ahsap-kesme-tahtasi.png",
    "Aura Aromaterapi Difüzörü": "/images/products/aura-difuzor.png",
    "Comet Akıllı Bileklik": "/images/products/comet-akilli-bileklik.png",
    "Cielo Kahve Öğütücü": "/images/products/cielo-kahve-ogutucu.png",
    "Kora Seramik Tabak Seti": "/images/products/kora-seramik-tabak-seti.png",
    "Dune Güneş Gözlüğü": "/images/products/dune-gunes-gozlugu.png",
    "Halo Taşınabilir Projektör": "/images/products/halo-tasinabilir-projektor.png",
    "Fika Çay Demliği": "/images/products/fika-cay-demligi.png"
});

const $ = selector => document.querySelector(selector);
const $$ = selector => [...document.querySelectorAll(selector)];
const currency = value => new Intl.NumberFormat("tr-TR", {style: "currency", currency: "TRY"}).format(Number(value || 0));
const html = value => String(value ?? "").replace(/[&<>'"]/g, char => ({"&":"&amp;","<":"&lt;",">":"&gt;","'":"&#39;","\"":"&quot;"}[char]));
const roleLabel = role => ({ADMIN: "ADMIN · Yönetici", SELLER: "SELLER · Satıcı", COURIER: "COURIER · Kurye", CUSTOMER: "CUSTOMER · Alıcı"}[role] || "");

async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (options.body && !(options.body instanceof FormData) && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
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

function curatedProductImage(product) {
    return curatedProductImages[product.name] || null;
}

function normaliseProduct(product) {
    const category = product.categoryNames?.[0] || product.categories?.[0]?.name || "Seçki";
    const variant = product.variants?.[0] || null;
    return {id: product.id, sellerId: product.sellerId, sellerName: product.sellerName || `Satıcı #${product.sellerId}`, name: product.name,
        description: product.description || "NexaMarket seçkisinden özenle seçildi.", category,
        price: Number(product.minPrice ?? product.basePrice ?? variant?.price ?? 0),
        inStock: product.inStock ?? Number(product.totalStock ?? variant?.stockQuantity ?? 0) > 0,
        stock: Number(product.totalStock ?? variant?.stockQuantity ?? 0), variantId: variant?.id || null,
        status: product.status || "ACTIVE", imageUrl: product.imageUrl || product.primaryImageUrl || curatedProductImage(product),
        emoji: emojiFor(`${product.name} ${category}`)};
}

function productVisual(product) {
    if (product.imageUrl) return `<img class="product-photo" src="${html(product.imageUrl)}" alt="${html(product.name)}" loading="lazy" decoding="async">`;
    return `<span class="product-symbol" aria-hidden="true">${product.emoji}</span>`;
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
    const query = $("#globalSearchInput").value.trim().toLocaleLowerCase("tr");
    const products = state.catalog.filter(product => {
        const matchesCategory = state.activeCategory === "all" || product.category.toLocaleLowerCase("tr") === state.activeCategory.toLocaleLowerCase("tr");
        const matchesFavorite = !state.favoriteOnly || state.favorites.has(String(product.id));
        const haystack = `${product.name} ${product.category} ${product.sellerName}`.toLocaleLowerCase("tr");
        return matchesCategory && matchesFavorite && (!query || haystack.includes(query));
    });
    if (state.sort === "price-asc") products.sort((a, b) => a.price - b.price);
    if (state.sort === "price-desc") products.sort((a, b) => b.price - a.price);
    if (state.sort === "name") products.sort((a, b) => a.name.localeCompare(b.name, "tr"));
    return products;
}

function hideSearchSuggestions() {
    const suggestions = $("#searchSuggestions");
    suggestions.hidden = true;
    suggestions.innerHTML = "";
    $("#globalSearchInput").setAttribute("aria-expanded", "false");
}

function renderSearchSuggestions() {
    const input = $("#globalSearchInput"), suggestions = $("#searchSuggestions");
    const query = input.value.trim().toLocaleLowerCase("tr");
    if (!query || state.catalogLoading) return hideSearchSuggestions();

    const includesQuery = value => String(value || "").toLocaleLowerCase("tr").includes(query);
    const unique = values => [...new Map(values.map(value => [String(value).toLocaleLowerCase("tr"), value])).values()];
    const categories = unique(state.catalog.map(product => product.category)).filter(includesQuery).slice(0, 3);
    const sellers = unique(state.catalog.map(product => product.sellerName)).filter(includesQuery).slice(0, 3);
    const products = state.catalog.filter(product => includesQuery(`${product.name} ${product.category} ${product.sellerName}`)).slice(0, 6);
    const sections = [];
    if (products.length) sections.push(`<div class="suggestion-group"><small>ÜRÜNLER</small>${products.map(product => `<button type="button" class="search-suggestion" role="option" data-search-suggestion="product" data-product-id="${product.id}"><span class="suggestion-icon">${product.imageUrl ? "▣" : product.emoji}</span><span><b>${html(product.name)}</b><em>${html(product.sellerName)} · ${currency(product.price)}</em></span><i>Ürün</i></button>`).join("")}</div>`);
    if (categories.length) sections.push(`<div class="suggestion-group"><small>KATEGORİLER</small>${categories.map(category => `<button type="button" class="search-suggestion" role="option" data-search-suggestion="category" data-category="${html(category)}"><span class="suggestion-icon">◇</span><span><b>${html(category)}</b><em>Bu kategorideki ürünleri gör</em></span><i>Kategori</i></button>`).join("")}</div>`);
    if (sellers.length) sections.push(`<div class="suggestion-group"><small>SATICILAR</small>${sellers.map(seller => `<button type="button" class="search-suggestion" role="option" data-search-suggestion="seller" data-seller-name="${html(seller)}"><span class="suggestion-icon">◦</span><span><b>${html(seller)}</b><em>Bu satıcının ürünlerini gör</em></span><i>Satıcı</i></button>`).join("")}</div>`);
    suggestions.innerHTML = sections.length ? sections.join("") : `<div class="search-suggestion-empty">“${html(input.value.trim())}” için öneri bulunamadı.</div>`;
    suggestions.hidden = false;
    input.setAttribute("aria-expanded", "true");
    $$('[data-search-suggestion]').forEach(button => button.addEventListener("click", () => {
        const type = button.dataset.searchSuggestion;
        if (type === "product") {
            hideSearchSuggestions();
            return openProductDetail(button.dataset.productId);
        }
        if (type === "category") {
            state.activeCategory = button.dataset.category;
            input.value = "";
            renderCategoryPills();
        } else {
            state.activeCategory = "all";
            input.value = button.dataset.sellerName;
            renderCategoryPills();
        }
        state.favoriteOnly = false;
        updateFavoritesUI();
        renderCatalog();
        hideSearchSuggestions();
        $("#discover").scrollIntoView({behavior: "smooth"});
    }));
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
            <button class="product-image ${product.imageUrl ? "has-photo" : "no-photo"}" data-view-product="${product.id}" aria-label="${html(product.name)} ürününü incele">${product.imageUrl ? "" : `<span class="tag">${html(product.category).toUpperCase()}</span>`}${productVisual(product)}<small>İNCELE →</small></button>
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
    const sellerName = product.sellerName, imageUrl = product.imageUrl;
    Object.assign(product, normaliseProduct(await api(`/api/v1/products/${product.id}`, {headers: {Authorization: ""}})));
    product.sellerName = sellerName || product.sellerName; product.imageUrl = imageUrl || product.imageUrl;
    return product;
}

async function refreshCatalogProducts(productIds) {
    const ids = [...new Set((productIds || []).filter(Boolean).map(String))];
    if (!ids.length) return;
    const results = await Promise.allSettled(ids.map(id => api(`/api/v1/products/${id}`, {headers: {Authorization: ""}})));
    results.forEach((result, index) => {
        if (result.status !== "fulfilled") return;
        const id = ids[index], current = state.catalog.find(item => String(item.id) === id);
        if (!current) return;
        const fresh = normaliseProduct(result.value);
        fresh.sellerName = current.sellerName || fresh.sellerName;
        fresh.imageUrl = current.imageUrl || fresh.imageUrl;
        Object.assign(current, fresh);
        if (state.selectedProduct && String(state.selectedProduct.id) === id) state.selectedProduct = current;
    });
    renderCategoryPills(); renderCatalog();
    if (state.selectedProduct && $("#productModal").open) updateProductDetailStock(state.selectedProduct);
}

function updateProductDetailStock(product) {
    $("#detailStock").innerHTML = product.inStock ? `<i></i><b>Stokta</b><span>${product.stock || "Sınırlı"} adet, gönderime hazır</span>` : `<i class="out"></i><b>Stokta yok</b><span>Bu ürün şu an siparişe kapalı</span>`;
    $("#detailAddButton").disabled = !product.inStock;
}

async function openProductDetail(productId) {
    const product = state.catalog.find(item => String(item.id) === String(productId));
    if (!product) return;
    try { await resolveProduct(product); } catch (error) { return toast(error.message, "error"); }
    state.selectedProduct = product;
    $("#detailArt").classList.toggle("has-photo", Boolean(product.imageUrl));
    $("#detailArt").innerHTML = product.imageUrl
        ? `<img src="${html(product.imageUrl)}" alt="${html(product.name)}" loading="eager"><small id="detailCategory">${html(product.category).toUpperCase()}</small>`
        : `<span id="detailEmoji">${product.emoji}</span><small id="detailCategory">${html(product.category).toUpperCase()}</small>`;
    $("#detailName").textContent = product.name; $("#detailDescription").textContent = product.description;
    $("#detailPrice").textContent = currency(product.price);
    updateProductDetailStock(product); updateDetailFavorite(); openModal("productModal");
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
            category: product?.category || "Seçki", imageUrl: product?.imageUrl || null, emoji: product?.emoji || emojiFor(name)};
    });
    saveCart(); renderCart();
}
async function syncCart(cart = null) {
    if (!state.token) return;
    if (state.user?.role !== "CUSTOMER") { state.cart = []; saveCart(); renderCart(); return; }
    try {
        applyCartResponse(cart || await api("/api/v1/cart/items"));
        await refreshCatalogProducts(state.cart.map(item => item.id));
    } catch { toast("Sepetin şu an güncellenemedi.", "error"); }
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
        await refreshCatalogProducts([product.id]);
        toast(`${product.name} sepete eklendi.`, "success"); if ($("#productModal").open) $("#productModal").close();
    } catch (error) { toast(error.message, "error"); } finally { setBusy(button, false); }
}

function renderCart() {
    const total = state.cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
    $("#cartCount").textContent = state.cart.reduce((sum, item) => sum + item.quantity, 0);
    $("#cartItems").innerHTML = state.cart.map(item => `<div class="cart-row"><div class="cart-row-art">${productVisual(item)}</div><div class="cart-row-copy"><small>${html(item.category)}</small><h3>${html(item.name)}</h3><p>${currency(item.price)} / adet</p></div><div class="cart-row-controls"><strong>${currency(item.price * item.quantity)}</strong><div class="cart-quantity" role="group" aria-label="${html(item.name)} adedi"><button type="button" data-decrease-cart="${item.cartItemId || item.id}" aria-label="${html(item.name)} ürününü bir adet azalt" title="${item.quantity === 1 ? "Sepetten çıkar" : "Bir adet azalt"}">−</button><output aria-live="polite"><b>${item.quantity}</b><span>adet</span></output><button type="button" data-increase-cart="${item.cartItemId || item.id}" aria-label="${html(item.name)} ürününü bir adet artır" title="Bir adet artır">+</button></div></div></div>`).join("");
    $("#cartTotal").textContent = currency(total); $("#cartEmpty").hidden = state.cart.length > 0; $("#cartSummary").hidden = state.cart.length === 0;
    $$('[data-decrease-cart]').forEach(button => button.addEventListener("click", () => requestCartDecrease(button.dataset.decreaseCart, button)));
    $$('[data-increase-cart]').forEach(button => button.addEventListener("click", () => increaseCartItem(button.dataset.increaseCart, button)));
}
function requestCartDecrease(itemKey, button) {
    const item = state.cart.find(candidate => String(candidate.cartItemId || candidate.id) === String(itemKey));
    if (!item) return;
    if (item.quantity > 1) return removeFromCart(item, button);
    askConfirmation("Sepetten çıkarılsın mı?", `${item.name} sepetinden kaldırılacak ve ayrılan stok geri bırakılacak.`,
        "Sepetten çıkar", () => removeFromCart(item));
}
async function increaseCartItem(itemKey, button) {
    const item = state.cart.find(candidate => String(candidate.cartItemId || candidate.id) === String(itemKey));
    if (!item?.variantId) return;
    setBusy(button, true, "");
    try {
        applyCartResponse(await api("/api/v1/cart/items", {method: "POST", body: JSON.stringify({productVariantId: item.variantId, quantity: 1})}));
        await refreshCatalogProducts([item.id]);
    } catch (error) { toast(error.message, "error"); } finally { setBusy(button, false); }
}
async function removeFromCart(item, button = null) {
    if (!state.token || !item.cartItemId) { state.cart = state.cart.filter(candidate => candidate !== item); saveCart(); renderCart(); return; }
    setBusy(button, true, "");
    try {
        applyCartResponse(await api(`/api/v1/cart/items/${item.cartItemId}`, {method: "DELETE"}));
        await refreshCatalogProducts([item.id]);
        toast(item.quantity > 1 ? `${item.name} ürününden bir adet çıkarıldı.` : `${item.name} sepetten çıkarıldı.`, "success");
    } catch (error) { toast(error.message, "error"); } finally { setBusy(button, false); }
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
    const role = state.token ? roleLabel(state.user?.role) : "";
    $("#authButtonLabel").textContent = state.token ? name : "Giriş yap"; $("#menuUserName").textContent = state.token ? name : "NexaMarketli";
    $("#authButtonRole").textContent = role; $("#authButtonRole").hidden = !role;
    $("#menuUserRole").textContent = role; $("#menuUserRole").hidden = !role;
    $("#menuUserEmail").textContent = state.user?.email || "Hesabına giriş yap"; $("#authButton").classList.toggle("signed-in", Boolean(state.token)); closeAccountMenu();
    $("#courierAreaButton").hidden = state.user?.role !== "COURIER";
    $("#sellerAreaButton").hidden = state.user?.role !== "SELLER";
    $("#adminPanelButton").hidden = state.user?.role !== "ADMIN";
    $("#returnManagementButton").hidden = !["SELLER", "ADMIN"].includes(state.user?.role);
    $$('[data-open-seller]').forEach(button => {
        if (button.id !== "sellerAreaButton") button.hidden = Boolean(state.token);
    });
    renderRolePortal();
}

const rolePortalSettings = {
    SELLER: {
        route: "/seller/dashboard", icon: "◇", label: "Satıcı paneli", title: "Mağaza merkezi",
        subtitle: "Ürünlerini, siparişlerini ve iadelerini tek çalışma alanından yönet.",
        navigation: [["overview", "⌂", "Genel bakış"], ["products", "◇", "Ürün yönetimi"], ["orders", "▤", "Mağaza siparişleri"], ["returns", "↺", "İade talepleri"], ["profile", "◎", "Profil"]]
    },
    COURIER: {
        route: "/courier/deliveries", icon: "▣", label: "Kurye paneli", title: "Teslimat merkezi",
        subtitle: "Sadece sana atanan teslimatları güvenli durum adımlarıyla yönet.",
        navigation: [["overview", "⌂", "Genel bakış"], ["assigned", "◌", "Yeni atananlar"], ["active", "▣", "Devam edenler"], ["history", "✓", "Teslimat geçmişi"], ["profile", "◎", "Profil"]]
    },
    ADMIN: {
        route: "/admin/dashboard", icon: "◫", label: "Yönetim paneli", title: "Platform merkezi",
        subtitle: "Kullanıcıları, siparişleri ve teslimat geçmişini denetle.",
        navigation: [["overview", "⌂", "Genel bakış"], ["users", "◎", "Kullanıcılar"], ["orders", "▤", "Siparişler"], ["deliveries", "▣", "Teslimat atamaları"], ["returns", "↺", "İade yönetimi"], ["profile", "◦", "Profil"]]
    }
};

function protectedRoleForPath(path = location.pathname) {
    if (path.startsWith("/customer")) return "CUSTOMER";
    if (path.startsWith("/seller")) return "SELLER";
    if (path.startsWith("/courier")) return "COURIER";
    if (path.startsWith("/admin")) return "ADMIN";
    return null;
}

function applyRoleRoute(replace = true) {
    const expected = rolePortalSettings[state.user?.role];
    const requestedRole = protectedRoleForPath();
    let destination = location.pathname;
    if (!state.token && requestedRole) destination = "/";
    else if (state.token && expected && requestedRole !== state.user.role) destination = expected.route;
    else if (state.token && !expected && requestedRole) destination = "/";
    if (destination !== location.pathname) history[replace ? "replaceState" : "pushState"]({}, "", destination);
}

function portalMetric(value, label, icon) {
    return `<article class="portal-metric"><span>${icon}</span><strong>${value}</strong><small>${label}</small></article>`;
}

function renderRolePortal() {
    applyRoleRoute();
    const config = state.token ? rolePortalSettings[state.user?.role] : null;
    const portal = $("#rolePortal");
    portal.hidden = !config;
    document.body.classList.toggle("role-portal-active", Boolean(config));
    if (!config) return;
    const displayName = state.user.email.split("@")[0];
    $("#portalRoleIcon").textContent = config.icon; $("#portalRoleLabel").textContent = config.label;
    $("#portalTitle").textContent = config.title; $("#portalSubtitle").textContent = config.subtitle;
    $("#portalUserName").textContent = displayName; $("#portalUserEmail").textContent = state.user.email;
    $("#portalNavigation").innerHTML = config.navigation.map(([action, icon, label], index) => `<button class="${index === 0 ? "active" : ""}" data-portal-action="${action}"><span>${icon}</span>${label}<i>›</i></button>`).join("");
    updatePortalOverview();
}

function updatePortalOverview() {
    if (!state.token || !rolePortalSettings[state.user?.role]) return;
    let metrics = "", quickActions = "", recentTitle = "", recent = "";
    if (state.user.role === "SELLER") {
        metrics = portalMetric(state.sellerProducts.length, "Toplam ürün", "◇") + portalMetric(state.sellerProducts.filter(item => item.status === "ACTIVE").length, "Yayındaki ürün", "✦") + portalMetric(state.sellerOrders.length, "Mağaza siparişi", "▤");
        quickActions = `<button data-portal-action="products"><span>＋</span><b>Ürün ekle veya düzenle</b><small>Fiyat, stok, görsel ve yayın durumunu yönet</small></button><button data-portal-action="orders"><span>▤</span><b>Siparişleri hazırla</b><small>Sadece kendi mağazana ait siparişleri gör</small></button><button data-portal-action="returns"><span>↺</span><b>İadeleri değerlendir</b><small>Bekleyen talepleri incele</small></button>`;
        recentTitle = "Son mağaza siparişleri";
        recent = state.sellerOrders.slice(0, 4).map(order => `<div><span class="status-badge status-${String(order.status).toLowerCase()}">${html(orderStatusLabels[order.status] || order.status)}</span><b>#${html(String(order.subOrderId).slice(0, 8).toUpperCase())}</b><small>${Number(order.itemCount || 0)} ürün · ${html(formatOrderDate(order.createdAt))}</small></div>`).join("");
    } else if (state.user.role === "COURIER") {
        const active = state.courierOrders.filter(item => !["REJECTED", "DELIVERED", "DELIVERY_FAILED"].includes(item.status));
        metrics = portalMetric(active.length, "Aktif teslimat", "▣") + portalMetric(state.courierOrders.filter(item => item.status === "ASSIGNED").length, "Onay bekleyen", "◌") + portalMetric(state.courierOrders.filter(item => item.status === "DELIVERED").length, "Teslim edilen", "✓");
        quickActions = `<button data-portal-action="deliveries"><span>▣</span><b>Teslimatları yönet</b><small>Kabul, teslim alma ve teslim adımlarını ilerlet</small></button>`;
        recentTitle = "Son teslimat atamaları";
        recent = state.courierOrders.slice(0, 5).map(item => `<div><span class="status-badge delivery-${String(item.status).toLowerCase()}">${html(deliveryStatusLabels[item.status] || item.status)}</span><b>#${html(String(item.subOrderId).slice(0, 8).toUpperCase())}</b><small>${item.packageCount} paket · ${html(formatOrderDate(item.assignedAt))}</small></div>`).join("");
    } else {
        const active = state.adminDeliveries.filter(item => item.active);
        metrics = portalMetric(state.adminUsers.length, "Platform kullanıcısı", "◎") + portalMetric(state.adminOrders.length, "Alt sipariş", "▤") + portalMetric(active.length, "Aktif teslimat", "▣");
        quickActions = `<button data-portal-action="users"><span>◎</span><b>Kullanıcı ve rolleri yönet</b><small>Hesapları etkinleştir, rol ata veya sil</small></button><button data-portal-action="orders"><span>▤</span><b>Kurye ata</b><small>Uygun siparişe aktif kurye ata</small></button><button data-portal-action="deliveries"><span>▣</span><b>Teslimat geçmişini denetle</b><small>Aktif ve tamamlanmış atamaları gör</small></button>`;
        recentTitle = "Son teslimat hareketleri";
        recent = state.adminDeliveries.slice(0, 5).map(item => `<div><span class="status-badge delivery-${String(item.status).toLowerCase()}">${html(deliveryStatusLabels[item.status] || item.status)}</span><b>#${html(String(item.subOrderId).slice(0, 8).toUpperCase())}</b><small>Kurye #${item.courierId} · ${html(formatOrderDate(item.assignedAt))}</small></div>`).join("");
    }
    $("#portalContent").innerHTML = `<section class="portal-welcome"><div><p class="eyebrow">BUGÜNÜN ÖZETİ</p><h2>Merhaba, ${html(state.user.email.split("@")[0])}</h2><p>Rolüne ait bütün işlemler burada; diğer çalışma alanları hesabından tamamen ayrıdır.</p></div><span>${rolePortalSettings[state.user.role].icon}</span></section><section class="portal-metrics">${metrics}</section><section class="portal-grid"><article class="portal-panel"><header><div><p class="eyebrow">HIZLI İŞLEMLER</p><h3>Ne yapmak istersin?</h3></div></header><div class="portal-quick-actions">${quickActions}</div></article><article class="portal-panel"><header><div><p class="eyebrow">GÜNCEL DURUM</p><h3>${recentTitle}</h3></div><button data-portal-action="overview">↻</button></header><div class="portal-recent">${recent || `<div class="portal-empty"><span>◌</span><small>Henüz gösterilecek kayıt yok.</small></div>`}</div></article></section>`;
}

async function loadRolePortalData() {
    if (state.user?.role === "SELLER") await Promise.all([loadSellerProducts(), loadSellerOrders()]);
    else if (state.user?.role === "COURIER") await loadCourierOrders();
    else if (state.user?.role === "ADMIN") await Promise.all([loadAdminUsers(), loadAdminOrders(), loadAdminDeliveries()]);
    updatePortalOverview();
}
const orderStatusLabels = {PAYMENT_PENDING: "Ödeme bekleniyor", PAID: "Ödeme alındı", PROCESSING: "Hazırlanıyor", SHIPPED: "Kargoda", DELIVERED: "Teslim edildi", CANCELLED: "İptal edildi", RETURN_REQUESTED: "İade inceleniyor", RETURN_APPROVED: "İade onaylandı", RETURN_REJECTED: "İade reddedildi"};
const returnStatusLabels = {REQUESTED: "İnceleniyor", APPROVED: "Onaylandı", REJECTED: "Reddedildi"};
const deliveryStatusLabels = {ASSIGNED: "Atandı", ACCEPTED: "Kabul edildi", REJECTED: "Reddedildi", PICKED_UP: "Teslim alındı", IN_TRANSIT: "Dağıtımda", DELIVERED: "Teslim edildi", DELIVERY_FAILED: "Teslim edilemedi"};

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
    state.token = ""; state.refreshToken = ""; state.user = null; state.cart = []; state.orders = []; state.returns = []; state.manageableReturns = []; state.sellerOrders = []; state.courierOrders = []; state.adminOrders = []; state.adminDeliveries = []; state.adminUsers = []; state.favorites = readFavorites(null);
    saveSession(); saveCart(); renderCart(); updateFavoritesUI(); renderCatalog(); updateAuthUI(); closeModals();
    applyRoleRoute();
}
async function logout() {
    const refreshToken = state.refreshToken;
    try { if (refreshToken) await api("/api/v1/auth/logout", {method: "POST", body: JSON.stringify({refreshToken})}); }
    catch { /* The local session is still cleared if the server token has expired. */ }
    finally { clearLocalSession(); await loadProducts(); toast("Hesabından güvenle çıkış yaptın.", "success"); }
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
    $("#authRoleField").hidden = mode !== "register";
    $("#authRole").disabled = mode !== "register";
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
    $("#verificationCode").value = "";
    $("#authTabs").hidden = true;
    $("#authForm").hidden = true;
    $("#verificationPanel").hidden = false;
    $("#authTitle").textContent = "Kodunu gir, hesabını tamamla";
    $("#authSubtitle").textContent = "E-posta bağlantısı yerine, aynı ekranda kullanacağın güvenli bir kod gönderiyoruz.";
    setAuthMessage("");
    setTimeout(() => $("#verificationCode").focus(), 50);
}

function showVerificationReminder() {
    if (!state.user || state.user.emailVerified) return;
    state.pendingVerificationEmail = state.user.email;
    $("#verificationReminderEmail").textContent = state.user.email;
    $("#verificationReminderCode").value = "";
    openModal("verificationReminderModal");
    setTimeout(() => $("#verificationReminderCode").focus(), 50);
}

async function resendVerification(email, button) {
    if (!email) return;
    setBusy(button, true, "Gönderiliyor");
    try {
        await api("/api/v1/auth/resend-verification", {method: "POST", body: JSON.stringify({email})});
        toast("Yeni doğrulama kodu e-posta adresine gönderildi.", "success");
    } catch (error) { toast(error.message, "error"); }
    finally { setBusy(button, false); }
}

async function submitVerificationCode(event, source) {
    event.preventDefault();
    const isReminder = source === "reminder";
    const email = isReminder ? state.user?.email : state.pendingVerificationEmail;
    const input = $(isReminder ? "#verificationReminderCode" : "#verificationCode");
    const button = $(isReminder ? "#verifyReminderCodeButton" : "#verifyCodeButton");
    const code = input.value.replace(/\D/g, "").slice(0, 6);
    if (!email) return toast("Doğrulanacak e-posta adresi bulunamadı.", "error");
    if (code.length !== 6) return toast("Lütfen e-postadaki 6 haneli kodu gir.", "error");
    setBusy(button, true, "Doğrulanıyor");
    try {
        await api("/api/v1/auth/verify-email-code", {method: "POST", body: JSON.stringify({email, code})});
        if (state.user?.email?.toLocaleLowerCase("tr") === email.toLocaleLowerCase("tr")) {
            state.user.emailVerified = true;
            saveSession();
            updateAuthUI();
        }
        input.value = "";
        if ($("#verificationReminderModal").open) $("#verificationReminderModal").close();
        if ($("#authModal").open) {
            setAuthMode("login");
            $("#authEmail").value = email;
            $("#authPassword").value = "";
            setAuthMessage("E-posta adresin doğrulandı. İstersen şimdi giriş yapabilirsin.", true);
        }
        toast("E-posta adresin başarıyla doğrulandı.", "success");
    } catch (error) { toast(error.message, "error"); }
    finally { setBusy(button, false); }
}

async function submitAuth(event) {
    event.preventDefault(); const email = $("#authEmail").value.trim(), password = $("#authPassword").value, submit = $("#authSubmit");
    $("#authMessage").textContent = ""; setBusy(submit, true, state.authMode === "login" ? "Giriş yapılıyor" : "Hesap oluşturuluyor");
    try {
        if (state.authMode === "register") {
            await api("/api/v1/auth/register", {method: "POST", body: JSON.stringify({email, password, role: $("#authRole").value})});
            showVerificationPanel(email);
            return;
        }
        const login = await api("/api/v1/auth/login", {method: "POST", body: JSON.stringify({email, password})});
        state.token = login.accessToken; state.refreshToken = login.refreshToken || ""; state.user = await api("/api/v1/auth/me");
        saveSession(); loadFavoritesForCurrentUser(); applyRoleRoute(false); updateAuthUI(); closeModals();
        if (state.user.role === "CUSTOMER") await syncCart(); else await loadRolePortalData();
        toast(state.user.role === "CUSTOMER" ? "Hoş geldin! Alışverişe devam edebilirsin." : "Çalışma alanın hazır.", "success");
        if (!state.user.emailVerified) showVerificationReminder();
    } catch (error) { setAuthMessage(error.message); } finally { setBusy(submit, false); }
}

async function openSellerArea() {
    if (!state.token) { setAuthMode("register"); $("#authRole").value = "SELLER"; openModal("authModal"); $("#authMessage").textContent = "Mağazan için SELLER hesabı oluşturabilirsin."; return; }
    if (state.user?.role !== "SELLER") { toast("Bu çalışma alanı yalnızca SELLER hesapları içindir.", "error"); return; }
    openModal("sellerModal"); await Promise.all([loadSellerProducts(), loadSellerCategories(), loadSellerOrders()]);
}
async function openCourierArea() {
    if (!state.token) { openModal("authModal"); $("#authMessage").textContent = "Kurye alanı için önce giriş yapmalısın."; return; }
    if (state.user?.role !== "COURIER") { toast("Bu alan yalnızca COURIER hesapları içindir.", "error"); return; }
    openModal("courierModal"); await loadCourierOrders();
}
async function openAdminPanel() {
    if (!state.token) { openModal("authModal"); $("#authMessage").textContent = "Yönetim paneli için önce giriş yapmalısın."; return; }
    if (state.user?.role !== "ADMIN") { toast("Bu alan yalnızca ADMIN hesapları içindir.", "error"); return; }
    openModal("adminModal"); await Promise.all([loadAdminUsers(), loadAdminOrders(), loadAdminDeliveries()]);
}
async function loadAdminUsers() {
    $("#adminUsers").innerHTML = `<div class="inventory-loading"><span class="button-spinner"></span>Kullanıcılar hazırlanıyor…</div>`;
    try {
        const users = await api("/api/v1/admin/auth/users");
        state.adminUsers = users;
        renderAdminUsers();
        if (state.adminOrders.length) renderAdminOrders();
    } catch (error) { $("#adminUsers").innerHTML = `<div class="inventory-empty"><b>Kullanıcılar yüklenemedi</b><small>${html(error.message)}</small></div>`; }
}
function renderAdminUsers() {
    const users = state.adminUsers || [];
    $("#adminUserCount").textContent = `${users.length} kullanıcı`;
    if (!users.length) { $("#adminUsers").innerHTML = `<div class="inventory-empty"><span>◫</span><b>Yönetilecek kullanıcı yok</b><small>Soldaki formdan ilk kullanıcıyı ekleyebilirsin.</small></div>`; return; }
    const roleLabels = {CUSTOMER: "Alıcı", SELLER: "Satıcı", COURIER: "Kurye", ADMIN: "Yönetici"};
    $("#adminUsers").innerHTML = users.map(user => {
        const isDeleted = user.status === "DELETED", isSelf = String(user.id) === String(state.user?.id);
        const nextStatus = user.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
        const statusAction = user.status === "ACTIVE" ? "Devre dışı bırak" : "Etkinleştir";
        const roleActions = !isDeleted && !isSelf ? ["CUSTOMER", "SELLER", "COURIER", "ADMIN"].filter(role => role !== user.role).map(role => `<button data-admin-role="${role}" data-admin-user="${user.id}">${roleLabels[role]} yap</button>`).join("") : "";
        const statusButton = !isDeleted && !isSelf ? `<button data-admin-status="${nextStatus}" data-admin-user="${user.id}">${statusAction}</button>` : "";
        const deleteButton = !isDeleted && !isSelf && user.role !== "ADMIN" ? `<button class="delete-product" data-admin-delete="${user.id}">Sil</button>` : "";
        const statusLabel = {ACTIVE: "Aktif", DISABLED: "Devre dışı", LOCKED: "Kilitli", DELETED: "Silinmiş"}[user.status] || user.status;
        return `<article class="admin-user-row"><div class="admin-user-avatar">${html(user.email.charAt(0).toUpperCase())}</div><div class="admin-user-copy"><div><span class="status-badge status-${user.status.toLowerCase()}">${statusLabel}</span><small>${html(roleLabels[user.role] || user.role)}</small></div><b>${html(user.email)}</b><strong>ID #${user.id}${isSelf ? " · Sen" : ""}</strong></div><div class="admin-user-actions">${roleActions}${statusButton}${deleteButton}</div></article>`;
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
    if (user) askConfirmation("Kullanıcı silinsin mi?", `${user.email} hesabı erişime kapatılacak ve silinmiş olarak işaretlenecek. Geçmiş sipariş kayıtları korunur.`, "Kullanıcıyı sil", () => deleteAdminUser(user.id));
}
async function deleteAdminUser(userId) {
    await api(`/api/v1/admin/users/${userId}`, {method: "DELETE"});
    toast("Kullanıcı güvenle silinmiş olarak işaretlendi.", "success"); await loadAdminUsers();
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

async function loadAdminOrders() {
    $("#adminOrders").innerHTML = `<div class="inventory-loading"><span class="button-spinner"></span>Siparişler hazırlanıyor…</div>`;
    try { state.adminOrders = await api("/api/v1/orders"); renderAdminOrders(); }
    catch (error) { state.adminOrders = []; $("#adminOrders").innerHTML = `<div class="inventory-empty"><b>Siparişler yüklenemedi</b><small>${html(error.message)}</small></div>`; }
}

function renderAdminOrders() {
    const orders = state.adminOrders || [];
    $("#adminOrderCount").textContent = `${orders.length} alt sipariş`;
    if (!orders.length) {
        $("#adminOrders").innerHTML = `<div class="inventory-empty"><span>▤</span><b>Henüz sipariş yok</b><small>Müşteriler sipariş verdiğinde tüm satıcı parçaları burada görünür.</small></div>`;
        return;
    }
    const couriers = (state.adminUsers || []).filter(user => user.role === "COURIER" && user.status === "ACTIVE");
    $("#adminOrders").innerHTML = orders.map(order => {
        const terminal = ["DELIVERED", "CANCELLED", "RETURN_APPROVED", "RETURN_REJECTED"].includes(order.status);
        const options = `<option value="">Kurye seç</option>${couriers.map(courier => `<option value="${courier.id}" ${String(courier.id) === String(order.courierId) ? "selected" : ""}>${html(courier.email)}</option>`).join("")}`;
        const assignment = terminal
            ? `<small class="role-order-note">Kapalı sipariş</small>`
            : order.courierId
                ? `<small class="role-order-note"><b>Aktif atama</b><br>Kurye #${order.courierId}<br>Kurye sonuçlandırana kadar değiştirilemez.</small>`
            : couriers.length
                ? `<div class="courier-assignment"><select data-courier-select="${order.subOrderId}" aria-label="Kurye seç">${options}</select><button data-assign-courier="${order.subOrderId}">Kurye ata</button></div>`
                : `<small class="role-order-note">Aktif kurye hesabı yok</small>`;
        const customerIdentity = order.customerEmail || "Silinmiş kullanıcı";
        return roleOrderCard(order, `Müşteri: ${customerIdentity} · Satıcı #${order.sellerId}`, assignment);
    }).join("");
    $$('[data-assign-courier]').forEach(button => button.addEventListener("click", () => assignCourier(button.dataset.assignCourier, button)));
}

async function assignCourier(subOrderId, button) {
    const courierId = Number($(`[data-courier-select="${subOrderId}"]`)?.value);
    if (!Number.isInteger(courierId) || courierId <= 0) return toast("Önce aktif bir kurye seç.", "error");
    setBusy(button, true, "Atanıyor");
    try {
        await api("/api/v1/admin/deliveries/assign", {method: "POST", body: JSON.stringify({subOrderId, courierId})});
        toast("Kurye ataması oluşturuldu.", "success"); await Promise.all([loadAdminOrders(), loadAdminDeliveries()]);
    } catch (error) { toast(error.message, "error"); setBusy(button, false); }
}

async function loadAdminDeliveries() {
    $("#adminDeliveries").innerHTML = `<div class="inventory-loading"><span class="button-spinner"></span>Teslimat geçmişi hazırlanıyor…</div>`;
    try { state.adminDeliveries = await api("/api/v1/admin/deliveries"); renderAdminDeliveries(); updatePortalOverview(); }
    catch (error) { state.adminDeliveries = []; $("#adminDeliveries").innerHTML = `<div class="inventory-empty"><b>Teslimatlar yüklenemedi</b><small>${html(error.message)}</small></div>`; }
}

function renderAdminDeliveries() {
    const deliveries = state.adminDeliveries || [];
    $("#adminDeliveryCount").textContent = `${deliveries.length} atama kaydı`;
    if (!deliveries.length) {
        $("#adminDeliveries").innerHTML = `<div class="inventory-empty"><span>▣</span><b>Henüz teslimat ataması yok</b><small>Bir siparişe kurye atandığında tüm yaşam döngüsü burada korunur.</small></div>`;
        return;
    }
    $("#adminDeliveries").innerHTML = deliveries.map(item => {
        const reason = item.rejectionReason || (item.failureReasonCode ? `${failureReasonLabels[item.failureReasonCode] || item.failureReasonCode}: ${item.failureDescription || ""}` : "");
        const customerIdentity = item.customerEmail || "Silinmiş kullanıcı";
        return `<article class="delivery-history-card"><div class="delivery-history-head"><span class="status-badge delivery-${String(item.status).toLowerCase()}">${html(deliveryStatusLabels[item.status] || item.status)}</span><small>${item.active ? "Aktif atama" : "Geçmiş kayıt"}</small></div><b>Alt sipariş #${html(String(item.subOrderId).slice(0, 8).toUpperCase())}</b><p>Müşteri: ${html(customerIdentity)} · Satıcı #${item.sellerId} · Kurye #${item.courierId}</p><small>${html(formatOrderDate(item.assignedAt))} · Sipariş: ${html(orderStatusLabels[item.orderStatus] || item.orderStatus)}</small>${reason ? `<em>${html(reason)}</em>` : ""}</article>`;
    }).join("");
}

async function loadCourierOrders() {
    $("#courierOrders").innerHTML = `<div class="inventory-loading"><span class="button-spinner"></span>Teslimatların hazırlanıyor…</div>`;
    try { state.courierOrders = await api("/api/v1/courier/deliveries"); renderCourierOrders(); updatePortalOverview(); }
    catch (error) { state.courierOrders = []; $("#courierOrders").innerHTML = `<div class="inventory-empty"><b>Teslimatlar yüklenemedi</b><small>${html(error.message)}</small></div>`; }
}
function formatOrderDate(value) {
    return value ? new Intl.DateTimeFormat("tr-TR", {dateStyle: "medium", timeStyle: "short"}).format(new Date(value)) : "Tarih bilgisi yok";
}
function renderCourierOrders() {
    const allDeliveries = state.courierOrders || [];
    const deliveries = allDeliveries.filter(item => {
        if (state.courierFilter === "assigned") return item.status === "ASSIGNED";
        if (state.courierFilter === "active") return ["ACCEPTED", "PICKED_UP", "IN_TRANSIT"].includes(item.status);
        if (state.courierFilter === "history") return ["REJECTED", "DELIVERED", "DELIVERY_FAILED"].includes(item.status);
        return true;
    });
    $$("[data-courier-filter]").forEach(button => button.classList.toggle("active", button.dataset.courierFilter === state.courierFilter));
    $("#courierOrderCount").textContent = `${deliveries.length} gösteriliyor · toplam ${allDeliveries.length}`;
    if (!deliveries.length) {
        $("#courierOrders").innerHTML = `<div class="inventory-empty"><span>▣</span><b>Henüz sana atanmış sipariş yok</b><small>Yönetici bir siparişi sana atadığında burada görünecek.</small></div>`;
        return;
    }
    $("#courierOrders").innerHTML = deliveries.map(item => {
        let actions = "";
        if (item.status === "ASSIGNED") actions = `<button class="delivery-action accept" data-delivery-action="accept" data-assignment-id="${item.assignmentId}">Atamayı kabul et</button><button class="delivery-action secondary" data-delivery-form="reject" data-assignment-id="${item.assignmentId}">Reddet</button>`;
        if (item.status === "ACCEPTED") actions = `${item.orderStatus === "PROCESSING" || item.orderStatus === "SHIPPED" ? `<button class="delivery-action accept" data-delivery-action="pickup" data-assignment-id="${item.assignmentId}">Paketi teslim aldım</button>` : `<small class="courier-note">Satıcının hazırlaması bekleniyor.</small>`}<button class="delivery-action danger" data-delivery-form="fail" data-assignment-id="${item.assignmentId}">Sorun bildir</button>`;
        if (item.status === "PICKED_UP") actions = `<button class="delivery-action accept" data-delivery-action="start" data-assignment-id="${item.assignmentId}">Dağıtıma çık</button><button class="delivery-action danger" data-delivery-form="fail" data-assignment-id="${item.assignmentId}">Sorun bildir</button>`;
        if (item.status === "IN_TRANSIT") actions = `<button class="delivery-action accept" data-delivery-action="deliver" data-assignment-id="${item.assignmentId}">Teslim edildi</button><button class="delivery-action danger" data-delivery-form="fail" data-assignment-id="${item.assignmentId}">Teslim edilemedi</button>`;
        if (["REJECTED", "DELIVERED", "DELIVERY_FAILED"].includes(item.status)) actions = `<small class="courier-note">Bu atama kapandı; geçmiş kaydı korunuyor.</small>`;
        const detail = item.rejectionReason || item.failureDescription;
        return `<article class="courier-order-row delivery-card"><div class="courier-order-icon">▣</div><div class="courier-order-copy"><div><span class="status-badge delivery-${String(item.status).toLowerCase()}">${html(deliveryStatusLabels[item.status] || item.status)}</span><small>${html(formatOrderDate(item.assignedAt))}</small></div><b>Alt sipariş #${html(String(item.subOrderId).slice(0, 8).toUpperCase())}</b><span>${item.packageCount} paket · Sipariş ${html(orderStatusLabels[item.orderStatus] || item.orderStatus)}</span>${detail ? `<em>${html(detail)}</em>` : ""}</div><div class="courier-order-action delivery-actions">${actions}</div><form class="delivery-inline-form" data-reject-form="${item.assignmentId}" hidden><label>Red gerekçesi<input maxlength="500" required placeholder="Örn. bölge dışında"></label><button type="submit">Reddi kaydet</button><button type="button" data-close-delivery-form>Vazgeç</button></form><form class="delivery-inline-form" data-fail-form="${item.assignmentId}" hidden><label>Sorun türü<select required>${Object.entries(failureReasonLabels).map(([value, label]) => `<option value="${value}">${label}</option>`).join("")}</select></label><label>Açıklama<input maxlength="1000" required placeholder="Teslimat neden tamamlanamadı?"></label><button type="submit">Sorunu kaydet</button><button type="button" data-close-delivery-form>Vazgeç</button></form></article>`;
    }).join("");
    $$('[data-delivery-action]').forEach(button => button.addEventListener("click", () => performDeliveryAction(button.dataset.assignmentId, button.dataset.deliveryAction, button)));
    $$('[data-delivery-form]').forEach(button => button.addEventListener("click", () => {
        const form = $(`[data-${button.dataset.deliveryForm}-form="${button.dataset.assignmentId}"]`);
        form.hidden = false; button.closest(".delivery-actions").hidden = true; form.querySelector("input")?.focus();
    }));
    $$('[data-close-delivery-form]').forEach(button => button.addEventListener("click", () => { const form = button.closest("form"); form.hidden = true; form.closest(".delivery-card").querySelector(".delivery-actions").hidden = false; }));
    $$('[data-reject-form]').forEach(form => form.addEventListener("submit", event => submitDeliveryIssue(event, "reject")));
    $$('[data-fail-form]').forEach(form => form.addEventListener("submit", event => submitDeliveryIssue(event, "fail")));
}
const failureReasonLabels = {VEHICLE_BREAKDOWN: "Araç arızası", ACCIDENT: "Kaza", HEALTH_ISSUE: "Sağlık sorunu", CUSTOMER_UNREACHABLE: "Müşteriye ulaşılamadı", ADDRESS_PROBLEM: "Adres sorunu", PACKAGE_DAMAGED: "Paket hasarlı", SECURITY_ISSUE: "Güvenlik sorunu", OTHER: "Diğer"};

function setCourierFilter(filter) {
    state.courierFilter = ["all", "assigned", "active", "history"].includes(filter) ? filter : "all";
    renderCourierOrders();
}

async function performDeliveryAction(assignmentId, action, button, body = null) {
    const labels = {accept: "Kabul ediliyor", reject: "Reddediliyor", pickup: "Teslim alınıyor", start: "Dağıtıma çıkarılıyor", deliver: "Tamamlanıyor", fail: "Kaydediliyor"};
    setBusy(button, true, labels[action] || "Güncelleniyor");
    try {
        await api(`/api/v1/courier/deliveries/${assignmentId}/${action}`, {method: "PATCH", body: body ? JSON.stringify(body) : undefined});
        toast({accept: "Atama kabul edildi.", reject: "Atama reddedildi; yönetici yeniden atayabilir.", pickup: "Paket teslim alındı.", start: "Teslimat dağıtıma çıktı.", deliver: "Teslimat tamamlandı.", fail: "Teslimat sorunu kaydedildi; sipariş iptal edilmedi."}[action], "success");
        await loadCourierOrders();
    } catch (error) { toast(error.message, "error"); setBusy(button, false); }
}

async function submitDeliveryIssue(event, action) {
    event.preventDefault();
    const form = event.currentTarget, assignmentId = form.dataset[action + "Form"], button = form.querySelector('button[type="submit"]');
    const body = action === "reject"
        ? {reason: form.querySelector("input").value.trim()}
        : {reasonCode: form.querySelector("select").value, description: form.querySelector("input").value.trim()};
    await performDeliveryAction(assignmentId, action, button, body);
}
async function ensureCategory(name) {
    const categories = await api("/api/v1/categories");
    const existing = categories.find(category => category.name.toLocaleLowerCase("tr") === name.toLocaleLowerCase("tr"));
    if (existing) return existing.id;
    throw new Error(`“${name}” kategorisi bulunamadı. Önce ADMIN tarafından oluşturulmalı.`);
}
async function loadSellerCategories() {
    const field = $("#sellerCategory"), selected = field.value;
    field.disabled = true;
    field.innerHTML = `<option value="">Kategoriler yükleniyor…</option>`;
    try {
        state.sellerCategories = await api("/api/v1/categories", {headers: {Authorization: ""}});
        if (!state.sellerCategories.length) throw new Error("Henüz ürün eklenebilecek kategori yok.");
        field.innerHTML = `<option value="" disabled ${selected ? "" : "selected"}>Kategori seçin</option>${state.sellerCategories.map(category => `<option value="${category.id}">${html(category.name)}</option>`).join("")}`;
        if (state.sellerCategories.some(category => String(category.id) === selected)) field.value = selected;
        field.disabled = false;
    } catch (error) {
        state.sellerCategories = [];
        field.innerHTML = `<option value="">Kategoriler yüklenemedi</option>`;
        field.disabled = true;
        toast(error.message, "error");
    }
}

function clearSellerImagePreview() {
    if (state.sellerImagePreviewUrl) URL.revokeObjectURL(state.sellerImagePreviewUrl);
    state.sellerImagePreviewUrl = "";
    const preview = $("#sellerImagePreview");
    preview.hidden = true; preview.innerHTML = "";
}

function updateSellerImagePreview() {
    clearSellerImagePreview();
    const file = $("#sellerImage").files?.[0];
    if (!file) return;
    const preview = $("#sellerImagePreview");
    state.sellerImagePreviewUrl = URL.createObjectURL(file);
    preview.innerHTML = `<img src="${state.sellerImagePreviewUrl}" alt="Seçilen ürün görseli ön izlemesi"><span>${html(file.name)}</span>`;
    preview.hidden = false;
}

function resetSellerForm() {
    $("#sellerForm").reset();
    $("#sellerPrice").value = "249.90"; $("#sellerStock").value = "12"; $("#sellerPublish").checked = true;
    $("#sellerCategory").value = "";
    clearSellerImagePreview();
}

async function uploadProductImage(productId, file) {
    if (!file) return null;
    if (!["image/jpeg", "image/png"].includes(file.type)) throw new Error("Görsel JPG veya PNG biçiminde olmalı.");
    if (file.size > 5 * 1024 * 1024) throw new Error("Görsel en fazla 5 MB olabilir.");
    const formData = new FormData(); formData.append("file", file);
    return api(`/api/v1/products/${productId}/images`, {method: "POST", body: formData});
}

function generatedSku(name) {
    const slug = name.toLocaleUpperCase("tr").replace(/İ/g, "I").replace(/Ş/g, "S").replace(/Ğ/g, "G").replace(/Ü/g, "U").replace(/Ö/g, "O").replace(/Ç/g, "C").replace(/[^A-Z0-9]+/g, "-").replace(/^-|-$/g, "").slice(0, 22) || "URUN";
    return `NX-${slug}-${Date.now().toString(36).slice(-5).toUpperCase()}`;
}
async function createProduct(payload, silent = false) {
    const categoryId = payload.categoryId ?? await ensureCategory(payload.category);
    if (!Number.isInteger(Number(categoryId))) throw new Error("Lütfen listeden geçerli bir kategori seç.");
    let product = await api("/api/v1/products", {method: "POST", body: JSON.stringify({name: payload.name, description: payload.description,
        basePrice: payload.price, categoryIds: [categoryId], variants: [{sku: payload.sku || generatedSku(payload.name), attributes: {"Seçenek": "Standart"}, price: payload.price, stockQuantity: payload.stock}]})});
    const image = await uploadProductImage(product.id, payload.imageFile);
    if (payload.publish !== false) product = await api(`/api/v1/products/${product.id}/publication`, {method: "PATCH", body: JSON.stringify({status: "ACTIVE"})});
    const normalised = normaliseProduct(product);
    normalised.imageUrl = image?.thumbnailUrl || image?.originalUrl || normalised.imageUrl;
    if (normalised.status === "ACTIVE") state.catalog = [normalised, ...state.catalog.filter(item => String(item.id) !== String(normalised.id))];
    renderCategoryPills(); renderCatalog();
    if (!silent) toast(normalised.status === "ACTIVE" ? "Ürün vitrinde yayına alındı." : "Ürün taslak olarak kaydedildi.", "success");
    return normalised;
}

function roleOrderCard(order, ownerText, action = "") {
    const statusLabel = orderStatusLabels[order.status] || order.status;
    const courierText = order.courierId ? `Kurye #${order.courierId}` : "Kurye bekleniyor";
    return `<article class="role-order-card"><div class="role-order-icon">▤</div><div class="role-order-copy"><div><span class="status-badge status-${String(order.status).toLowerCase()}">${html(statusLabel)}</span><small>${html(formatOrderDate(order.createdAt))}</small></div><b>Sipariş #${html(String(order.orderId).slice(0, 8).toUpperCase())}</b><span>Alt sipariş #${html(String(order.subOrderId).slice(0, 8).toUpperCase())} · ${Number(order.itemCount || 0)} ürün</span><small>${html(ownerText)} · ${html(courierText)}</small><strong>${currency(order.subtotal)}</strong></div><div class="role-order-action">${action}</div></article>`;
}

async function loadSellerOrders() {
    $("#sellerOrders").innerHTML = `<div class="inventory-loading"><span class="button-spinner"></span>Siparişler hazırlanıyor…</div>`;
    try { state.sellerOrders = await api("/api/v1/orders"); renderSellerOrders(); updatePortalOverview(); }
    catch (error) { state.sellerOrders = []; $("#sellerOrders").innerHTML = `<div class="inventory-empty"><b>Siparişler yüklenemedi</b><small>${html(error.message)}</small></div>`; }
}

function renderSellerOrders() {
    const orders = state.sellerOrders || [];
    $("#sellerOrderCount").textContent = `${orders.length} mağaza siparişi`;
    if (!orders.length) {
        $("#sellerOrders").innerHTML = `<div class="inventory-empty"><span>▤</span><b>Henüz mağaza siparişin yok</b><small>Müşteriler ürünlerini satın aldığında yalnızca sana ait alt siparişler burada görünür.</small></div>`;
        return;
    }
    $("#sellerOrders").innerHTML = orders.map(order => {
        const action = order.status === "PAID"
            ? `<button class="primary-button small" data-seller-status="PROCESSING" data-sub-order-id="${order.subOrderId}">Hazırlamaya başla</button>`
            : `<small class="role-order-note">${order.status === "PROCESSING" ? "Kurye teslim alacak" : "Durum güncel"}</small>`;
        return roleOrderCard(order, `Satıcı #${order.sellerId}`, action);
    }).join("");
    $$('[data-seller-status]').forEach(button => button.addEventListener("click", () => updateSellerOrderStatus(button.dataset.subOrderId, button.dataset.sellerStatus, button)));
}

async function updateSellerOrderStatus(subOrderId, status, button) {
    setBusy(button, true, "Güncelleniyor");
    try {
        await api(`/api/v1/orders/${subOrderId}/status`, {method: "PATCH", body: JSON.stringify({status})});
        toast("Sipariş hazırlanmaya alındı.", "success"); await loadSellerOrders();
    } catch (error) { toast(error.message, "error"); setBusy(button, false); }
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
        const product = await createProduct({name: $("#sellerProductName").value.trim(), categoryId: Number($("#sellerCategory").value), price: Number($("#sellerPrice").value), stock: Number($("#sellerStock").value), sku: $("#sellerSku").value.trim(), description: $("#sellerDescription").value.trim() || "NexaMarket satıcısından yeni ürün.", imageFile: $("#sellerImage").files?.[0] || null, publish: $("#sellerPublish").checked}, true);
        $("#sellerMessage").textContent = product.status === "ACTIVE" ? `${product.name} vitrinde yayına alındı.` : `${product.name} taslak olarak kaydedildi.`; $("#sellerMessage").classList.add("success");
        resetSellerForm();
        await loadSellerProducts(); renderCategoryPills(); renderCatalog();
    } catch (error) { $("#sellerMessage").classList.remove("success"); $("#sellerMessage").textContent = error.message; } finally { setBusy(submit, false); }
}

async function loadSellerProducts() {
    $("#sellerInventory").innerHTML = `<div class="inventory-loading"><span class="button-spinner"></span>Ürünlerin hazırlanıyor…</div>`;
    try { state.sellerProducts = await api("/api/v1/products/seller"); renderSellerProducts(); updatePortalOverview(); }
    catch (error) { $("#sellerInventory").innerHTML = `<div class="inventory-empty"><b>Ürünler yüklenemedi</b><small>${html(error.message)}</small></div>`; }
}
function renderSellerProducts() {
    if (!state.sellerProducts.length) { $("#sellerInventory").innerHTML = `<div class="inventory-empty"><span>◇</span><b>Henüz ürünün yok</b><small>Soldaki formdan ilk ürününü ekleyebilirsin.</small></div>`; return; }
    const labels = {ACTIVE: "Yayında", DRAFT: "Taslak", PASSIVE: "Satışta değil"};
    $("#sellerInventory").innerHTML = state.sellerProducts.map(product => {
        const stock = (product.variants || []).reduce((sum, variant) => sum + Number(variant.stockQuantity || 0), 0), active = product.status === "ACTIVE";
        const visual = normaliseProduct(product);
        const variants = product.variants || [];
        const variantEditors = variants.map((variant, index) => `<div class="inventory-variant-editor"><small>${variants.length > 1 ? `Seçenek ${index + 1}` : "Ürün bilgileri"}</small><label>Fiyat (₺)<input type="number" min="0.01" step="0.01" value="${Number(variant.price ?? product.basePrice ?? 0).toFixed(2)}" data-variant-price="${variant.id}" aria-label="${html(product.name)} fiyatı"></label><label>Stok<input type="number" min="0" step="1" value="${Number(variant.stockQuantity ?? 0)}" data-variant-stock="${variant.id}" aria-label="${html(product.name)} stok adedi"></label></div>`).join("");
        return `<article class="inventory-row"><div class="inventory-art">${visual.imageUrl ? `<img src="${html(visual.imageUrl)}" alt="${html(product.name)}" loading="lazy">` : emojiFor(product.name)}</div><div class="inventory-copy"><div><span class="status-badge status-${product.status.toLowerCase()}">${labels[product.status] || product.status}</span><small>${stock} stok</small></div><b>${html(product.name)}</b><div class="inventory-product-editor">${variantEditors}<label class="inventory-image-editor">Yeni görsel <input type="file" accept="image/jpeg,image/png" data-product-image="${product.id}"></label><button class="product-save-button" data-save-product="${product.id}">Değişiklikleri kaydet</button></div></div><div class="inventory-actions"><button data-toggle-product="${product.id}" data-target-status="${active ? "PASSIVE" : "ACTIVE"}">${active ? "Yayından kaldır" : "Yayınla"}</button><button class="delete-product" data-delete-product="${product.id}">Sil</button></div></article>`;
    }).join("");
    $$('[data-toggle-product]').forEach(button => button.addEventListener("click", () => changeProductPublication(button.dataset.toggleProduct, button.dataset.targetStatus, button)));
    $$('[data-delete-product]').forEach(button => button.addEventListener("click", () => requestProductDelete(button.dataset.deleteProduct)));
    $$('[data-save-product]').forEach(button => button.addEventListener("click", () => updateSellerProduct(button.dataset.saveProduct, button)));
}
async function updateSellerProduct(productId, button) {
    const product = state.sellerProducts.find(item => String(item.id) === String(productId));
    const variants = product.variants || [];
    if (!variants.length) { toast("Bu ürünün güncellenecek varyantı bulunamadı.", "error"); return; }
    const updates = variants.map(variant => ({
        id: variant.id,
        price: Number($(`[data-variant-price="${variant.id}"]`)?.value),
        stockQuantity: Number($(`[data-variant-stock="${variant.id}"]`)?.value)
    }));
    const invalid = updates.find(update => !Number.isFinite(update.price) || update.price <= 0 || !Number.isInteger(update.stockQuantity) || update.stockQuantity < 0);
    if (!product || invalid) { toast("Her seçenek için geçerli fiyat ve sıfırdan küçük olmayan stok gir.", "error"); return; }
    const imageFile = $(`[data-product-image="${productId}"]`)?.files?.[0] || null;
    setBusy(button, true, "Kaydediliyor");
    try {
        const updated = await api(`/api/v1/products/${productId}`, {method: "PATCH", body: JSON.stringify({basePrice: Math.min(...updates.map(update => update.price)), variants: updates})});
        const uploadedImage = await uploadProductImage(productId, imageFile);
        const imageUrl = uploadedImage?.originalUrl || product.imageUrl || state.catalog.find(item => String(item.id) === String(productId))?.imageUrl || null;
        const updatedSellerProduct = {...updated, imageUrl};
        state.sellerProducts = state.sellerProducts.map(item => String(item.id) === String(productId) ? updatedSellerProduct : item);
        const existing = state.catalog.find(item => String(item.id) === String(productId));
        if (updated.status === "ACTIVE") {
            const item = normaliseProduct(updatedSellerProduct); item.imageUrl = imageUrl || existing?.imageUrl || item.imageUrl;
            state.catalog = [item, ...state.catalog.filter(item => String(item.id) !== String(productId))];
        }
        renderSellerProducts(); renderCatalog(); toast(`${updated.name} güncellendi.`, "success");
    } catch (error) { toast(error.message, "error"); setBusy(button, false); }
}
async function changeProductPublication(productId, status, button) {
    setBusy(button, true, status === "ACTIVE" ? "Yayınlanıyor" : "Kaldırılıyor");
    try {
        const updated = await api(`/api/v1/products/${productId}/publication`, {method: "PATCH", body: JSON.stringify({status})});
        state.sellerProducts = state.sellerProducts.map(product => String(product.id) === String(productId) ? updated : product);
        if (status === "ACTIVE") { const existing = state.catalog.find(product => String(product.id) === String(productId)); const item = normaliseProduct(updated); item.imageUrl = existing?.imageUrl || item.imageUrl; state.catalog = [item, ...state.catalog.filter(product => String(product.id) !== String(productId))]; }
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
        const purchasedProductIds = state.cart.map(item => item.id);
        const payment = await api("/api/v1/payments", {method: "POST", body: JSON.stringify({orderId: state.lastOrder.id, idempotencyKey: `web-${crypto.randomUUID()}`, walletAmount: 0, cardAmount: state.lastOrder.total.toFixed(2)})});
        if (payment.providerPaymentId) await api(`/mock-payment-provider/payments/${payment.providerPaymentId}/outcomes`, {method: "POST", headers: {Authorization: ""}, body: JSON.stringify({status: "SUCCEEDED", failureReason: null, callbackDelaySeconds: 0, duplicateDeliveries: 1})});
        $("#paymentPanel").hidden = true; $("#successPanel").hidden = false; state.cart = []; saveCart(); renderCart();
        await refreshCatalogProducts(purchasedProductIds);
    } catch (error) { $("#checkoutMessage").textContent = error.message; } finally { setBusy(button, false); }
}

function clearFilters() {
    state.activeCategory = "all"; state.favoriteOnly = false; $("#sortSelect").value = "featured"; state.sort = "featured";
    $("#globalSearchInput").value = "";
    renderCategoryPills(); updateFavoritesUI(); renderCatalog();
}
function shopCategory(category) {
    const available = [...new Set(state.catalog.map(product => product.category))];
    state.activeCategory = category === "all" ? "all" : (available.find(item => item.toLocaleLowerCase("tr").includes(category.toLocaleLowerCase("tr"))) || "all");
    state.favoriteOnly = false; updateFavoritesUI(); renderCategoryPills(); renderCatalog(); $("#discover").scrollIntoView({behavior: "smooth"});
}

const helpTopics = {
    discover: {
        title: "Ürünleri keşfetmek çok kolay",
        lead: "Katalogdaki her ürün, güncel stok ve fiyat bilgisiyle listelenir.",
        content: `<ol class="help-list"><li><b>Arama yap</b><span>Ürün adı, kategori veya satıcı ile hızlıca filtrele.</span></li><li><b>İncele</b><span>Karta dokunarak açıklama, stok ve ürün görselini görüntüle.</span></li><li><b>Favorile</b><span>Beğendiğin ürünleri kalp simgesiyle daha sonra bulmak üzere kaydet.</span></li></ol>`
    },
    checkout: {
        title: "Sepet ve ödeme akışı",
        lead: "Satın alma adımları açık ve her aşamada kontrol sende.",
        content: `<ol class="help-list"><li><b>Sepete ekle</b><span>Stokta olan bir ürünü sepetine ekle; adet ve toplam tutar anında güncellenir.</span></li><li><b>Siparişi oluştur</b><span>Sepeti onayladığında sipariş oluşturulur ve stok ayrılır.</span></li><li><b>Güvenli ödeme</b><span>Ödemeyi tamamladığında sipariş durumunu hesabındaki Siparişlerim alanından takip edebilirsin.</span></li></ol>`
    },
    delivery: {
        title: "Teslimat, takip ve iade",
        lead: "Siparişin her adımı hesabındaki Siparişlerim ve İadelerim alanında görünür.",
        content: `<ol class="help-list"><li><b>Hazırlanma ve kargo</b><span>Satıcı siparişini hazırlar; kurye kargoya verildi ve teslim edildi durumlarını günceller.</span></li><li><b>İade talebi</b><span>Uygun siparişlerde Hesabım → İadelerim ekranından iade nedenini seçerek talep oluşturabilirsin.</span></li><li><b>Sonuç bildirimi</b><span>İade onay veya red kararı hesabında görünür; e-posta bildirimleri de hesabındaki gelişmelerden haberdar eder.</span></li></ol>`
    }
};

function openHelp(topic) {
    const help = helpTopics[topic] || helpTopics.discover;
    $("#helpTitle").textContent = help.title; $("#helpLead").textContent = help.lead; $("#helpContent").innerHTML = help.content;
    openModal("helpModal");
}

async function handlePortalAction(action) {
    $$("#portalNavigation button").forEach(button => button.classList.toggle("active", button.dataset.portalAction === action));
    if (action === "profile") return openAccount("overview");
    if (action === "overview") return loadRolePortalData();
    if (state.user?.role === "SELLER") {
        if (action === "returns") return openReturnManagement();
        return openSellerArea();
    }
    if (state.user?.role === "COURIER") {
        setCourierFilter(action === "deliveries" ? "all" : action);
        return openCourierArea();
    }
    if (state.user?.role === "ADMIN") {
        if (action === "returns") return openReturnManagement();
        return openAdminPanel();
    }
}

function initEvents() {
    $("#cartButton").addEventListener("click", openCart); $$('[data-close-cart]').forEach(button => button.addEventListener("click", closeCart)); $("#overlay").addEventListener("click", closeCart);
    $$('[data-open-seller]').forEach(button => button.addEventListener("click", openSellerArea)); $$('[data-close-modal]').forEach(button => button.addEventListener("click", closeModals));
    $("#authButton").addEventListener("click", event => { event.stopPropagation(); toggleAccountMenu(); }); $("#accountOverviewButton").addEventListener("click", () => openAccount("overview"));
    $("#footerReturnsButton").addEventListener("click", () => openAccount("returns"));
    $("#footerHowItWorksButton").addEventListener("click", () => openHelp("discover")); $("#footerDeliveryButton").addEventListener("click", () => openHelp("delivery"));
    $$("[data-account-tab]").forEach(button => button.addEventListener("click", () => switchAccountTab(button.dataset.accountTab)));
    $$("[data-account-target]").forEach(button => button.addEventListener("click", () => switchAccountTab(button.dataset.accountTarget)));
    $("#refreshOrdersButton").addEventListener("click", loadOrders); $("#refreshReturnsButton").addEventListener("click", loadCustomerReturns);
    $("#returnRequestForm").addEventListener("submit", submitReturnRequest); $("#returnManagementButton").addEventListener("click", openReturnManagement); $("#refreshManageableReturnsButton").addEventListener("click", loadManageableReturns);
    $("#courierAreaButton").addEventListener("click", openCourierArea); $("#refreshCourierButton").addEventListener("click", loadCourierOrders);
    $$("[data-courier-filter]").forEach(button => button.addEventListener("click", () => setCourierFilter(button.dataset.courierFilter)));
    $("#adminPanelButton").addEventListener("click", openAdminPanel); $("#refreshAdminUsersButton").addEventListener("click", loadAdminUsers); $("#refreshAdminOrdersButton").addEventListener("click", loadAdminOrders); $("#refreshAdminDeliveriesButton").addEventListener("click", loadAdminDeliveries); $("#adminUserForm").addEventListener("submit", submitAdminUser);
    $("#logoutButton").addEventListener("click", logout); $("#headerLogoutButton").addEventListener("click", logout);
    $("#portalLogoutButton").addEventListener("click", logout); $("#portalProfileButton").addEventListener("click", () => openAccount("overview"));
    $("#rolePortal").addEventListener("click", event => { const target = event.target.closest("[data-portal-action]"); if (target) handlePortalAction(target.dataset.portalAction); });
    window.addEventListener("popstate", () => { applyRoleRoute(); renderRolePortal(); });
    $("#favoritesButton").addEventListener("click", () => { state.favoriteOnly = !state.favoriteOnly; updateFavoritesUI(); renderCatalog(); $("#discover").scrollIntoView({behavior: "smooth"}); });
    $$("[data-auth-tab]").forEach(button => button.addEventListener("click", () => setAuthMode(button.dataset.authTab)));
    $("#resendVerificationButton").addEventListener("click", () => resendVerification(state.pendingVerificationEmail, $("#resendVerificationButton")));
    $("#resendReminderButton").addEventListener("click", () => resendVerification(state.user?.email, $("#resendReminderButton")));
    $("#verificationCodeForm").addEventListener("submit", event => submitVerificationCode(event, "registration"));
    $("#verificationReminderForm").addEventListener("submit", event => submitVerificationCode(event, "reminder"));
    $("#verificationLoginButton").addEventListener("click", () => { setAuthMode("login"); $("#authEmail").value = state.pendingVerificationEmail; });
    $("#authForm").addEventListener("submit", submitAuth); $("#sellerForm").addEventListener("submit", submitSeller); $("#sellerImage").addEventListener("change", updateSellerImagePreview); $("#refreshSellerButton").addEventListener("click", loadSellerProducts); $("#refreshSellerOrdersButton").addEventListener("click", loadSellerOrders);
    $$('[data-password-toggle]').forEach(button => button.addEventListener("click", () => {
        const input = document.getElementById(button.dataset.passwordToggle);
        const isVisible = input.type === "text";
        input.type = isVisible ? "password" : "text";
        button.classList.toggle("is-visible", !isVisible);
        button.querySelector("span").textContent = isVisible ? "◉" : "◌";
        const label = isVisible ? "Parolayı göster" : "Parolayı gizle";
        button.setAttribute("aria-label", label); button.setAttribute("title", label);
    }));
    $("#checkoutButton").addEventListener("click", checkout); $("#payButton").addEventListener("click", pay);
    $("#detailAddButton").addEventListener("click", () => state.selectedProduct && addToCart(state.selectedProduct.id, $("#detailAddButton")));
    $("#detailFavoriteButton").addEventListener("click", () => state.selectedProduct && toggleFavorite(state.selectedProduct.id));
    $("#confirmCancelButton").addEventListener("click", () => { state.confirmAction = null; $("#confirmModal").close(); }); $("#confirmActionButton").addEventListener("click", runConfirmedAction);
    $("#applyCouponButton").addEventListener("click", () => { const code = $("#couponInput").value.trim().toUpperCase(); state.coupon = code; $("#couponInput").value = code; $("#couponNote").textContent = code ? `${code} ödeme adımında uygulanacak.` : ""; });
    $("#emptySeedButton").addEventListener("click", seedCatalog); $("#refreshButton").addEventListener("click", loadProducts);
    $("#globalSearchInput").addEventListener("input", () => { renderCatalog(); renderSearchSuggestions(); });
    $("#globalSearchInput").addEventListener("keydown", event => { if (event.key === "Enter") { hideSearchSuggestions(); $("#discover").scrollIntoView({behavior: "smooth"}); } if (event.key === "Escape") hideSearchSuggestions(); });
    $$('[data-shop-category]').forEach(button => button.addEventListener("click", () => shopCategory(button.dataset.shopCategory)));
    $$('[data-campaign-category]').forEach(button => button.addEventListener("click", () => shopCategory(button.dataset.campaignCategory)));
    $$('[data-open-help]').forEach(button => button.addEventListener("click", () => openHelp(button.dataset.openHelp)));
    document.addEventListener("keydown", event => { if ((event.metaKey || event.ctrlKey) && event.key.toLocaleLowerCase("tr") === "k") { event.preventDefault(); $("#globalSearchInput").focus(); } });
    $("#sortSelect").addEventListener("change", event => { state.sort = event.target.value; renderCatalog(); }); $("#clearFiltersButton").addEventListener("click", clearFilters);
    document.addEventListener("click", event => { if (!event.target.closest(".account-shell")) closeAccountMenu(); if (!event.target.closest(".header-search")) hideSearchSuggestions(); });
}

async function boot() {
    initEvents(); renderCart(); updateAuthUI(); updateFavoritesUI(); renderCatalog();
    await hydrateUser();
    if (state.user && rolePortalSettings[state.user.role]) await loadRolePortalData();
    else { await loadProducts(); if (state.user?.role === "CUSTOMER") await syncCart(); }
    if (state.user && !state.user.emailVerified) showVerificationReminder();
}
boot();
