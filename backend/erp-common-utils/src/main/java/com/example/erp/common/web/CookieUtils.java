package com.example.erp.common.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.Optional;

/**
 * Utility class للتعامل مع Cookies
 */
public final class CookieUtils {
    
    private CookieUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * إنشاء cookie بسيطة
     */
    public static Cookie createCookie(String name, String value, int maxAge, String path) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge);
        cookie.setPath(path);
        cookie.setHttpOnly(true);
        return cookie;
    }
    
    /**
     * إنشاء cookie مع SameSite و Secure
     */
    public static void addCookie(HttpServletResponse response,
                                  String name,
                                  String value,
                                  int maxAge,
                                  String domain,
                                  String path,
                                  boolean secure,
                                  SameSite sameSite) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value).append("; ");
        sb.append("Max-Age=").append(maxAge).append("; ");
        sb.append("Path=").append(path).append("; ");
        
        if (domain != null && !domain.isBlank()) {
            sb.append("Domain=").append(domain).append("; ");
        }
        
        sb.append("HttpOnly; ");
        
        if (secure) {
            sb.append("Secure; ");
        }
        
        if (sameSite != null) {
            sb.append("SameSite=").append(sameSite.getValue());
        }
        
        response.addHeader("Set-Cookie", sb.toString());
    }
    
    /**
     * الحصول على cookie من request
     */
    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(c -> name.equals(c.getName()))
                    .findFirst();
        }
        return Optional.empty();
    }
    
    /**
     * حذف cookie
     */
    public static void deleteCookie(HttpServletResponse response,
                                     String name,
                                     String domain,
                                     String path) {
        addCookie(response, name, "", 0, domain, path, false, null);
    }
}
