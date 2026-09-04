package com.nexamarket.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Rol bazlı tarayıcı adresleri yenilendiğinde tek sayfalık mağaza arayüzünü yeniden açar.
 * Yetkilendirme arayüze bırakılmaz; bütün rol API'leri ayrıca Spring Security ile korunur.
 */
@Controller
public class FrontendRouteController {

    @GetMapping({
            "/customer", "/customer/", "/customer/{path:[^\\.]*}",
            "/seller", "/seller/", "/seller/{path:[^\\.]*}",
            "/courier", "/courier/", "/courier/{path:[^\\.]*}",
            "/admin", "/admin/", "/admin/{path:[^\\.]*}"
    })
    public String roleWorkspace() {
        return "forward:/index.html";
    }
}
