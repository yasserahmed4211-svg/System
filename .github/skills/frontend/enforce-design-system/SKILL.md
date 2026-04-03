---
description: "DESIGN SYSTEM ENFORCER — validates and guides CSS token usage in frontend SCSS. Use when creating/editing component styles, reviewing SCSS for hardcoded values, or adding new design tokens. Provides full token reference, mapping tables, and examples."
---

# Skill: enforce-design-system

## Name
`enforce-design-system`

## Description
Enforces the ERP design system by providing the complete token reference, usage examples, mapping tables, and validation checks for all SCSS in the frontend. This skill is the authoritative guide for converting hardcoded CSS values to `--erp-*` tokens.

## When to Use
- When creating new component SCSS files
- When editing existing component styles
- When reviewing SCSS for design system compliance
- When adding new tokens to `erp-tokens.scss`
- When unsure which token maps to a specific value

---

## TOKEN REFERENCE

### File Location
`src/scss/erp/erp-tokens.scss` — defines all tokens inside `:root {}`.

### Import Chain
`styles.scss` imports `scss/erp/erp-tokens` after the Mantis theme, before body utilities.

---

## BASE TOKENS

### Spacing

| Token | Value | Px Equivalent |
|---|---|---|
| `--erp-spacing-xxs` | `0.25rem` | 4px |
| `--erp-spacing-xs` | `0.5rem` | 8px |
| `--erp-spacing-sm` | `0.75rem` | 12px |
| `--erp-spacing-md` | `1rem` | 16px |
| `--erp-spacing-lg` | `1.5rem` | 24px |
| `--erp-spacing-xl` | `2rem` | 32px |
| `--erp-spacing-xxl` | `3rem` | 48px |

**Mapping guide** — when you see a hardcoded value, use the closest token:

| Hardcoded | Use Token |
|---|---|
| `4px`, `0.25rem` | `--erp-spacing-xxs` |
| `8px`, `0.5rem` | `--erp-spacing-xs` |
| `10px`, `12px`, `0.75rem` | `--erp-spacing-sm` |
| `15px`, `16px`, `1rem` | `--erp-spacing-md` |
| `20px`, `24px`, `1.5rem` | `--erp-spacing-lg` |
| `25px`, `32px`, `2rem` | `--erp-spacing-xl` |
| `48px`, `3rem` | `--erp-spacing-xxl` |

### Typography — Font Size

| Token | Value | Px Equivalent |
|---|---|---|
| `--erp-font-size-xs` | `0.75rem` | 12px |
| `--erp-font-size-sm` | `0.875rem` | 14px |
| `--erp-font-size-md` | `1rem` | 16px |
| `--erp-font-size-lg` | `1.125rem` | 18px |
| `--erp-font-size-xl` | `1.25rem` | 20px |
| `--erp-font-size-xxl` | `1.5rem` | 24px |
| `--erp-font-size-heading` | `1.75rem` | 28px |

### Typography — Font Weight

| Token | Value |
|---|---|
| `--erp-font-weight-normal` | `400` |
| `--erp-font-weight-medium` | `500` |
| `--erp-font-weight-semibold` | `600` |
| `--erp-font-weight-bold` | `700` |

### Typography — Line Height

| Token | Value |
|---|---|
| `--erp-line-height-tight` | `1.25` |
| `--erp-line-height-normal` | `1.5` |
| `--erp-line-height-relaxed` | `1.75` |

### Border Radius

| Token | Value | Px Equivalent |
|---|---|---|
| `--erp-radius-none` | `0` | 0 |
| `--erp-radius-sm` | `0.25rem` | 4px |
| `--erp-radius-md` | `0.375rem` | 6px |
| `--erp-radius-lg` | `0.5rem` | 8px |
| `--erp-radius-xl` | `0.75rem` | 12px |
| `--erp-radius-full` | `9999px` | pill |

**Mapping guide:**

| Hardcoded | Use Token |
|---|---|
| `2px`, `4px`, `0.25rem` | `--erp-radius-sm` |
| `5px`, `6px`, `0.375rem` | `--erp-radius-md` |
| `8px`, `0.5rem` | `--erp-radius-lg` |
| `10px`, `12px`, `0.75rem` | `--erp-radius-xl` |
| `20px`, `50%`, `9999px` | `--erp-radius-full` |

### Shadows

| Token | Value |
|---|---|
| `--erp-shadow-sm` | `0 1px 2px 0 rgb(0 0 0 / 0.05)` |
| `--erp-shadow-md` | `0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)` |
| `--erp-shadow-lg` | `0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1)` |

### Transitions

| Token | Value |
|---|---|
| `--erp-transition-fast` | `150ms ease-in-out` |
| `--erp-transition-normal` | `250ms ease-in-out` |
| `--erp-transition-slow` | `350ms ease-in-out` |

### Z-Index

| Token | Value |
|---|---|
| `--erp-z-dropdown` | `1000` |
| `--erp-z-sticky` | `1020` |
| `--erp-z-fixed` | `1030` |
| `--erp-z-modal-backdrop` | `1040` |
| `--erp-z-modal` | `1050` |
| `--erp-z-popover` | `1060` |
| `--erp-z-tooltip` | `1070` |
| `--erp-z-toast` | `1080` |

### Semantic Colors

| Token | Delegates To |
|---|---|
| `--erp-color-primary` | `var(--bs-primary, #0d6efd)` |
| `--erp-color-secondary` | `var(--bs-secondary, #6c757d)` |
| `--erp-color-success` | `var(--bs-success, #198754)` |
| `--erp-color-warning` | `var(--bs-warning, #ffc107)` |
| `--erp-color-danger` | `var(--bs-danger, #dc3545)` |
| `--erp-color-info` | `var(--bs-info, #0dcaf0)` |
| `--erp-color-text` | `var(--bs-body-color, #212529)` |
| `--erp-color-text-muted` | `var(--bs-secondary-color, #6c757d)` |
| `--erp-color-text-light` | `var(--bs-tertiary-color, #adb5bd)` |
| `--erp-color-bg` | `var(--bs-body-bg, #ffffff)` |
| `--erp-color-bg-subtle` | `var(--bs-tertiary-bg, #f8f9fa)` |
| `--erp-color-bg-muted` | `var(--bs-secondary-bg, #e9ecef)` |
| `--erp-color-border` | `var(--bs-border-color, #dee2e6)` |
| `--erp-color-border-light` | `var(--bs-border-color-translucent, rgba(0,0,0,0.175))` |

---

## COMPONENT-SPECIFIC TOKENS

These tokens are defined once in `erp-tokens.scss` and used by individual components. Their values MUST reference base tokens.

| Token | Value | Used By |
|---|---|---|
| `--erp-page-header-padding-block` | `var(--erp-spacing-md)` | Page header |
| `--erp-page-header-padding-inline` | `var(--erp-spacing-lg)` | Page header |
| `--erp-page-header-gap` | `var(--erp-spacing-md)` | Page header |
| `--erp-form-gap` | `var(--erp-spacing-md)` | Form layout |
| `--erp-form-label-gap` | `var(--erp-spacing-xs)` | Form field |
| `--erp-form-group-gap` | `var(--erp-spacing-lg)` | Form groups |
| `--erp-card-padding` | `var(--erp-spacing-lg)` | Card/Panel |
| `--erp-card-radius` | `var(--erp-radius-lg)` | Card/Panel |
| `--erp-table-cell-padding-block` | `var(--erp-spacing-sm)` | Table cells |
| `--erp-table-cell-padding-inline` | `var(--erp-spacing-md)` | Table cells |
| `--erp-btn-padding-block` | `var(--erp-spacing-xs)` | Buttons |
| `--erp-btn-padding-inline` | `var(--erp-spacing-md)` | Buttons |
| `--erp-btn-gap` | `var(--erp-spacing-xs)` | Buttons |
| `--erp-dialog-padding` | `var(--erp-spacing-lg)` | Dialog |
| `--erp-dialog-header-gap` | `var(--erp-spacing-md)` | Dialog |
| `--erp-dialog-footer-gap` | `var(--erp-spacing-sm)` | Dialog |
| `--erp-section-padding` | `var(--erp-spacing-md)` | Section |
| `--erp-section-radius` | `var(--erp-radius-lg)` | Section |
| `--erp-section-header-gap` | `var(--erp-spacing-sm)` | Section |
| `--erp-section-title-size` | `var(--erp-font-size-lg)` | Section |
| `--erp-empty-state-padding` | `var(--erp-spacing-lg)` | Empty state |
| `--erp-empty-state-radius` | `var(--erp-radius-xl)` | Empty state |
| `--erp-empty-state-gap` | `var(--erp-spacing-sm)` | Empty state |
| `--erp-empty-state-icon-size` | `var(--erp-font-size-xxl)` | Empty state |
| `--erp-notification-offset` | `var(--erp-spacing-md)` | Notification |
| `--erp-notification-z` | `var(--erp-z-toast)` | Notification |
| `--erp-notification-gap` | `var(--erp-spacing-sm)` | Notification |
| `--erp-autocomplete-z` | `var(--erp-z-modal)` | Autocomplete |
| `--erp-autocomplete-max-height` | `280px` | Autocomplete |
| `--erp-autocomplete-radius` | `var(--erp-radius-md)` | Autocomplete |
| `--erp-autocomplete-item-padding-block` | `var(--erp-spacing-xs)` | Autocomplete |
| `--erp-autocomplete-item-padding-inline` | `var(--erp-spacing-sm)` | Autocomplete |
| `--erp-lookup-max-height` | `400px` | Lookup dialog |
| `--erp-readonly-padding-block` | `var(--erp-spacing-xs)` | Readonly hint |
| `--erp-readonly-padding-inline` | `var(--erp-spacing-sm)` | Readonly hint |
| `--erp-readonly-radius` | `var(--erp-radius-lg)` | Readonly hint |

---

## USAGE EXAMPLES

### ✅ Correct — token with fallback

```scss
.my-component {
  padding: var(--erp-spacing-md, 1rem);
  font-size: var(--erp-font-size-sm, 0.875rem);
  font-weight: var(--erp-font-weight-semibold, 600);
  border-radius: var(--erp-radius-lg, 0.5rem);
  color: var(--erp-color-text-muted, rgba(33, 37, 41, 0.75));
  border: 1px solid var(--erp-color-border, #dee2e6);
  z-index: var(--erp-z-modal, 1050);
  transition: opacity var(--erp-transition-fast, 150ms ease-in-out);
  box-shadow: var(--erp-shadow-sm, 0 1px 2px 0 rgb(0 0 0 / 0.05));
}
```

### ❌ Wrong — hardcoded values

```scss
.my-component {
  padding: 1rem;                    // ❌ use --erp-spacing-md
  font-size: 14px;                  // ❌ use --erp-font-size-sm
  font-weight: 600;                 // ❌ use --erp-font-weight-semibold
  border-radius: 8px;               // ❌ use --erp-radius-lg
  color: #6c757d;                   // ❌ use --erp-color-text-muted
  border: 1px solid #dee2e6;        // ❌ use --erp-color-border
  z-index: 1050;                    // ❌ use --erp-z-modal
}
```

### ❌ Wrong — no fallback

```scss
.my-component {
  padding: var(--erp-spacing-md);   // ❌ missing fallback
}
```

---

## HOW TO ADD A NEW COMPONENT TOKEN

1. Open `src/scss/erp/erp-tokens.scss`.
2. Find the `COMPONENT-SPECIFIC TOKENS` section inside `:root {}`.
3. Add the token using this pattern:

```scss
  // MyWidget
  --erp-mywidget-padding: var(--erp-spacing-md);
  --erp-mywidget-radius: var(--erp-radius-lg);
```

4. In your component SCSS, use it with a fallback:

```scss
.erp-mywidget {
  padding: var(--erp-mywidget-padding, 1rem);
  border-radius: var(--erp-mywidget-radius, 0.5rem);
}
```

### Naming Convention

`--erp-<component>-<property>`

Examples:
- `--erp-dialog-padding`
- `--erp-section-radius`
- `--erp-notification-offset`

---

## VALIDATION CHECKLIST

When reviewing or generating SCSS, check each property:

| # | Check | Pass | Fail |
|---|---|---|---|
| DS.1 | Spacing values use `--erp-spacing-*` | `var(--erp-spacing-md, 1rem)` | `1rem` or `16px` |
| DS.2 | Font sizes use `--erp-font-size-*` | `var(--erp-font-size-sm, 0.875rem)` | `14px` or `0.875rem` |
| DS.3 | Font weights use `--erp-font-weight-*` | `var(--erp-font-weight-semibold, 600)` | `600` |
| DS.4 | Border radius uses `--erp-radius-*` | `var(--erp-radius-lg, 0.5rem)` | `8px` or `0.5rem` |
| DS.5 | Colors use `--erp-color-*` | `var(--erp-color-border, #dee2e6)` | `#dee2e6` or `rgb(...)` |
| DS.6 | Z-index uses `--erp-z-*` | `var(--erp-z-modal, 1050)` | `1050` |
| DS.7 | Shadows use `--erp-shadow-*` | `var(--erp-shadow-sm, ...)` | `box-shadow: 0 1px 2px...` |
| DS.8 | Transitions use `--erp-transition-*` | `var(--erp-transition-fast, ...)` | `150ms ease-in-out` |
| DS.9 | Every `var()` has a fallback | `var(--erp-spacing-md, 1rem)` | `var(--erp-spacing-md)` |
| DS.10 | No duplicate `erp-ui.scss` classes | Uses existing `.erp-card` | New `.my-card` duplicating layout |
| DS.11 | No Mantis styles removed | Tokens added alongside | Existing rules deleted |
| DS.12 | Component tokens reference base tokens | `var(--erp-spacing-lg)` | `1.5rem` hardcoded in token def |
| DS.13 | No inline styles in templates | SCSS file with tokens | `[style]="..."` or `style="..."` |

---

## ARCHITECTURE NOTES

- Tokens are a **control layer** — they sit on top of the Mantis/Bootstrap theme.
- The Mantis theme is NOT replaced — tokens are additive.
- `--erp-color-*` delegates to `--bs-*` Bootstrap variables, so theme changes propagate automatically.
- Dark mode is handled by the Mantis `dark.scss` — tokens inherit via the `--bs-*` delegation chain.
- `erp-ui.scss` provides optional layout utility classes (`.erp-page`, `.erp-card`, `.erp-form`, etc.) — use them instead of creating new layout classes.

---

## SHARED SCSS & UTILITY CLASS CONSUMPTION

When writing or reviewing SCSS, ensure shared resources are consumed instead of creating duplicates:

| # | Check | Shared Resource | Violation |
|---|-------|----------------|-----------|
| DS.14 | Layout utility classes | Use `.erp-page`, `.erp-card`, `.erp-form` from `erp-ui.scss` | Creating new `.my-card`, `.feature-container` |
| DS.15 | All tokens in single file | All custom properties defined in `erp-tokens.scss` | Feature-specific `_my-tokens.scss` files |
| DS.16 | No duplicate utility classes | Verify against existing `.erp-*` classes before adding | Duplicating an existing `.erp-*` layout class |
| DS.17 | Shared component styles | Shared components (`erp-form-field`, `erp-section`, etc.) style via tokens — do not override | Feature CSS overriding shared component styles |

**Rule:** Every new SCSS class or token must be verified against `erp-tokens.scss` and `erp-ui.scss` first. If an equivalent exists, use it — do NOT create a new one.

> **Cross-reference:** This skill validates SCSS token and utility consumption. For TypeScript/component code reuse, run [`enforce-reusability`](../enforce-reusability/SKILL.md).

---

## RELATED SKILLS

| Skill | Purpose |
|-------|---------|
| `enforce-reusability` | Validates that shared TypeScript code (`shared/`, `core/`) is consumed — no duplicated logic across features |
| `enforce-ui-ux` | Validates UI/UX display patterns, readability, and i18n compliance |
