package com.example.security.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO لعنصر القائمة مع دعم البنية الشجرية (Tree Structure)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MenuItemDto {

    private Long id;
    
    /** كود الصفحة (من SEC_PAGES) */
    private String pageCode;
    
    /** اسم العنصر بالعربية */
    private String nameAr;
    
    /** اسم العنصر بالإنجليزية */
    private String nameEn;
    
    /** المسار الخاص بالـ Angular Route */
    private String routePath;
    
    /** رقم الأب */
    private Long parentId;
    
    /** كود الصلاحية */
    private String permCode;
    
    /** Module grouping (e.g., SECURITY, FINANCE, HR) */
    private String module;
    
    /** ترتيب الظهور */
    private Integer displayOrder;
    
    /** الأيقونة */
    private String icon;
    
    /** حالة العنصر */
    private Boolean isActive;
    
    /** وصف */
    private String description;
    
    /** العناصر الفرعية (Children) */
    @Builder.Default
    private List<MenuItemDto> children = new ArrayList<>();
}
