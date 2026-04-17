package com.example.security.service;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.example.erp.common.util.SecurityContextHelper;
import com.example.security.domain.UserAccount;
import com.example.security.dto.*;
import com.example.security.exception.SecurityErrorCodes;
import com.example.security.repo.PageRepository;
import com.example.security.repo.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service لبناء القائمة من SEC_PAGES بناءً على صلاحيات VIEW
 * Menu Service - builds user menu dynamically from SEC_PAGES based on PERM_*_VIEW permissions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MenuService {

    private final PageRepository pageRepository;
    private final UserAccountRepository userAccountRepository;

    // Whitelist of allowed sort fields (Rule 17.3)
    private static final Set<String> ALLOWED_MENU_SORT_FIELDS = Set.of(
        "id", "nameEn", "nameAr", "displayOrder", "createdAt", "updatedAt"
    );

    /**
     * جلب القائمة الكاملة للمستخدم الحالي بناءً على صلاحيات VIEW من SEC_PAGES
     * يبني شجرة Menu (Tree Structure)
     * 
     * يستخدم SEC_PAGES مباشرة - لا حاجة لـ SEC_MENU_ITEM
     */
    @Transactional(readOnly = true)
    // @Cacheable(cacheNames = "userMenus", key = "T(com.example.erp.common.util.SecurityContextHelper).getUsername() + '_' + T(com.example.erp.common.multitenancy.TenantContext).getTenantId()")
    public ServiceResult<List<MenuItemDto>> getUserMenu() {
        SecurityContextHelper.TenantUserContext context = SecurityContextHelper.requireTenantAndUser();
        String tenantId = context.getTenantId();
        String username = context.getUsername();

        log.debug("Building menu from SEC_PAGES for user: {} in tenant: {}", username, tenantId);

        // 1. جلب المستخدم مع أدواره وصلاحياته
        UserAccount user = userAccountRepository.findByUsernameWithRoles(username, tenantId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.USER_NOT_FOUND, username));

        return ServiceResult.success(buildUserMenuFromPermissions(user, tenantId));
    }

    /**
     * جلب القائمة لمستخدم محدد (Admin use)
     */
    @Transactional(readOnly = true)
    public ServiceResult<List<MenuItemDto>> getUserMenu(Long userId) {
        com.example.erp.common.multitenancy.TenantHelper.requireTenant();
        String tenantId = com.example.erp.common.multitenancy.TenantContext.getTenantId();

        log.debug("Building menu from SEC_PAGES for userId: {} in tenant: {}", userId, tenantId);

        // 1. جلب المستخدم مع أدواره وصلاحياته
        UserAccount user = userAccountRepository.findByIdWithRoles(userId, tenantId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.USER_ENTITY_NOT_FOUND, userId));

        return ServiceResult.success(buildUserMenuFromPermissions(user, tenantId));
    }

    /**
     * منطق مشترك لبناء القائمة من صلاحيات المستخدم
     */
    private List<MenuItemDto> buildUserMenuFromPermissions(UserAccount user, String tenantId) {
        // 2. جمع جميع الصلاحيات من جميع الأدوار
        Set<String> userPermissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getName())
                .collect(Collectors.toSet());

        log.debug("User {} has {} permissions", user.getUsername(), userPermissions.size());

        // 3. استخراج PAGE_CODES من صلاحيات VIEW فقط
        // Permission format: PERM_<CODE>_VIEW
        Set<String> allowedPageCodes = userPermissions.stream()
                .filter(perm -> perm.startsWith("PERM_") && perm.endsWith("_VIEW"))
                .map(perm -> {
                    // Extract CODE from PERM_<CODE>_VIEW
                    String withoutPrefix = perm.substring(5); // Remove "PERM_"
                    String withoutSuffix = withoutPrefix.substring(0, withoutPrefix.length() - 5); // Remove "_VIEW"
                    return withoutSuffix;
                })
                .collect(Collectors.toSet());

        log.debug("User has VIEW permission for {} pages", allowedPageCodes.size());

        if (allowedPageCodes.isEmpty()) {
            log.info("User {} has no VIEW permissions - returning empty menu", user.getUsername());
            return new ArrayList<>();
        }

        // 4. جلب الصفحات من SEC_PAGES مباشرة
        List<com.example.security.domain.Page> pages = pageRepository
                .findByPageCodeInAndTenantIdAndActive(allowedPageCodes, tenantId, true);

        log.debug("Found {} active pages for user", pages.size());

        // 5. تحويل إلى DTOs
        List<MenuItemDto> allMenuDtos = pages.stream()
                .map(page -> MenuItemDto.builder()
                        .id(page.getId())
                        .pageCode(page.getPageCode()) // Add pageCode for testing
                        .nameAr(page.getNameAr())
                        .nameEn(page.getNameEn())
                        .routePath(page.getRoute())
                        .icon(page.getIcon())
                        .parentId(page.getParentId())
                        .module(page.getModule())
                        .displayOrder(page.getDisplayOrder())
                        .isActive(page.getActiveStatus())
                        .children(new ArrayList<>())
                        .build())
                .collect(Collectors.toList());

        // 6. بناء الشجرة (Tree Structure)
        List<MenuItemDto> menuTree = buildMenuTree(allMenuDtos);

        log.info("Menu tree built successfully from SEC_PAGES for user: {} with {} root items", 
                 user.getUsername(), menuTree.size());
        return menuTree;
    }

    /**
     * بناء شجرة القائمة (Parent → Children)
     */
    private List<MenuItemDto> buildMenuTree(List<MenuItemDto> allMenuItems) {
        // 1. إنشاء Map للوصول السريع للعناصر حسب ID
        Map<Long, MenuItemDto> menuMap = allMenuItems.stream()
                .collect(Collectors.toMap(MenuItemDto::getId, item -> item));

        // 2. العناصر الرئيسية (Root Items)
        List<MenuItemDto> rootItems = new ArrayList<>();

        // 3. بناء العلاقات
        for (MenuItemDto item : allMenuItems) {
            if (item.getParentId() == null) {
                // عنصر رئيسي
                rootItems.add(item);
            } else {
                // عنصر فرعي - إضافته للأب
                MenuItemDto parent = menuMap.get(item.getParentId());
                if (parent != null) {
                    parent.getChildren().add(item);
                } else {
                    // الأب غير موجود (ربما لا يملك صلاحيته)
                    // نضيف العنصر كـ root
                    rootItems.add(item);
                    log.warn("Parent ID {} not found for menu item: {} - adding as root", 
                            item.getParentId(), item.getNameEn());
                }
            }
        }

        // 4. ترتيب العناصر حسب displayOrder
        rootItems.sort(Comparator.comparing(MenuItemDto::getDisplayOrder, 
                                           Comparator.nullsLast(Comparator.naturalOrder())));
        
        // ترتيب الأطفال أيضاً
        for (MenuItemDto root : rootItems) {
            sortChildren(root);
        }

        return rootItems;
    }

    /**
     * ترتيب العناصر الفرعية بشكل recursive
     */
    private void sortChildren(MenuItemDto parent) {
        if (parent.getChildren() != null && !parent.getChildren().isEmpty()) {
            parent.getChildren().sort(Comparator.comparing(MenuItemDto::getDisplayOrder,
                                                          Comparator.nullsLast(Comparator.naturalOrder())));
            
            for (MenuItemDto child : parent.getChildren()) {
                sortChildren(child);
            }
        }
    }
}
