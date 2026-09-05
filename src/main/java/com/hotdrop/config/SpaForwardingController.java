package com.hotdrop.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardingController {

    @GetMapping(value = {
            "/",
            "/waiting-room/{id}",
            "/queue/{id}",
            "/checkout/{id}",
            "/my-bookings",
            "/admin-dashboard"
    })
    public String forwardSpa() {
        return "forward:/index.html";
    }
}
