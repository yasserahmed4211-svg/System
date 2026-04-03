---
description: "Generates a JPA entity class following the canonical pattern. Phase 1, Step 1 — MUST be completed before any other backend artifact. Enforces AuditableEntity, @SuperBuilder, BooleanNumberConverter, @Formula counts, and all DB naming conventions."
---

# Skill: create-entity

## Name
`create-entity`

## Description
Generates a JPA entity class for the ERP system following the canonical MasterLookup pattern. This is **Phase 1, Step 1** of the execution template and MUST be completed before any other backend artifact.

## When to Use
- When implementing a new feature/domain entity in any backend module
- When the execution template Phase 1, Step 1.1 is being started
- BEFORE creating repository, DTO, mapper, service, or controller

## Variables (Must Be Defined First)

| Variable | Example | Description |
|----------|---------|-------------|
| `MODULE_NAME` | `masterdata` | Maven module suffix |
| `MODULE_PREFIX` | `MD` | DB table prefix |
| `ENTITY_NAME` | `Activity` | PascalCase Java class name |
| `ENTITY_TABLE` | `MD_ACTIVITY` | UPPER_SNAKE DB table name |
| `ENTITY_SEQ` | `MD_ACTIVITY_SEQ` | Sequence name |
| `PARENT_ENTITY` | *(optional)* | Parent entity if child |
| `PARENT_FK_COL` | *(optional)* | e.g., `ACTIVITY_ID_FK` |

---

## Steps

### 1. Create Entity File
- **Location:** `backend/erp-<MODULE_NAME>/src/main/java/com/example/<module>/entity/Md<ENTITY_NAME>.java`

### 2. Class-Level Setup
```java
@Entity
@Table(name = "<ENTITY_TABLE>",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_<ENTITY_TABLE>_<FIELD>", columnNames = {"<FIELD>"})
    },
    indexes = {
        @Index(name = "IDX_<ENTITY_TABLE>_<COLUMN>", columnList = "<COLUMN>")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Md<ENTITY_NAME> extends AuditableEntity {
```

### 3. Primary Key
```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "<entity>_seq")
@SequenceGenerator(name = "<entity>_seq", sequenceName = "<ENTITY_SEQ>", allocationSize = 1)
@Column(name = "ID_PK")
private Long id;
```

### 4. Business Fields
```java
@NotBlank(message = "{validation.required}")
@Size(max = 50, message = "{validation.size}")
@Column(name = "<COLUMN_NAME>", length = 50, nullable = false)
private String fieldName;
```

### 5. Boolean Fields
```java
@Column(name = "IS_ACTIVE", nullable = false)
@Builder.Default
@Convert(converter = BooleanNumberConverter.class)
private Boolean isActive = Boolean.TRUE;
```

### 6. FK Relationships (if child entity)
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "<PARENT>_ID_FK", nullable = false,
    foreignKey = @ForeignKey(name = "FK_<ENTITY_TABLE>_<PARENT>"))
private Md<PARENT_ENTITY> parentEntity;
```

### 7. Parent → Child Collection (if parent entity)
```java
@OneToMany(mappedBy = "<childField>", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
private List<Md<CHILD_ENTITY>> children = new ArrayList<>();

@Formula("(SELECT COUNT(*) FROM <CHILD_TABLE> c WHERE c.<PARENT>_ID_FK = ID_PK)")
private Integer childCount;
```

### 8. Lifecycle Hooks
```java
@PrePersist
protected void onCreate() {
    if (isActive == null) {
        isActive = Boolean.TRUE;
    }
    if (naturalKey != null) {
        naturalKey = naturalKey.toUpperCase();
    }
}

@PreUpdate
protected void onUpdate() {
    if (naturalKey != null) {
        naturalKey = naturalKey.toUpperCase();
    }
}
```

### 9. Helper Methods
```java
public void activate() {
    this.isActive = Boolean.TRUE;
}

public void deactivate() {
    this.isActive = Boolean.FALSE;
}
```

---

## SHARED LAYER MANDATE (`erp-common-utils`)

Before creating a new entity, verify the following shared resources from `erp-common-utils` are consumed — do NOT reinvent:

| # | Requirement | Shared Class | Package |
|---|-------------|-------------|--------|
| SH.1 | MUST extend `AuditableEntity` for audit fields | `AuditableEntity` | `com.example.erp.common.domain` |
| SH.2 | MUST use `BooleanNumberConverter` for all boolean columns (NUMBER(1)) | `BooleanNumberConverter` | `com.example.erp.common.converter` |
| SH.3 | Audit fields auto-populated by `AuditEntityListener` — do NOT set manually | `AuditEntityListener` | `com.example.erp.common.audit` |
| SH.4 | Multi-tenant entities extend `TenantAuditableEntity` if applicable | `TenantAuditableEntity` | `com.example.erp.common.domain` |
| SH.5 | Use `@SuperBuilder` due to `AuditableEntity` inheritance — NEVER `@Builder` | — | Lombok |

**Rules:**
- NEVER create a custom audit base class — use `AuditableEntity`
- NEVER create a custom boolean converter — use `BooleanNumberConverter` (or `BooleanCharYNConverter` for CHAR(1) columns)
- NEVER set `createdAt/createdBy/updatedAt/updatedBy` manually — `AuditEntityListener` handles it

> **Cross-reference:** After creating the entity, run [`enforce-backend-contract`](../enforce-backend-contract/SKILL.md) to verify compliance.

---

## Rules (STRICT — from implementation-contract.md)

| Rule ID | Rule | MUST |
|---------|------|------|
| A.1.1 | Extends `AuditableEntity` | YES |
| A.1.2 | PK column is `ID_PK` with `@Column(name = "ID_PK")` | YES |
| A.1.3 | PK uses `GenerationType.SEQUENCE` with explicit `@SequenceGenerator` | YES |
| A.1.4 | `allocationSize = 1` on `@SequenceGenerator` | YES |
| A.1.5 | FK columns end with `_ID_FK` | YES |
| A.1.6 | Booleans stored via `BooleanNumberConverter` | YES |
| A.1.7 | Boolean default: `@Builder.Default Boolean isActive = Boolean.TRUE` | YES |
| A.1.8 | Every `@ManyToOne` uses `fetch = FetchType.LAZY` | YES |
| A.1.9 | `@OneToMany` uses `cascade = ALL, orphanRemoval = false, fetch = LAZY` | YES |
| A.1.10 | Uses `@SuperBuilder` (NOT `@Builder`) due to `AuditableEntity` | YES |
| A.1.11 | Table name: UPPER_SNAKE_CASE with module prefix | YES |
| A.1.12 | `@UniqueConstraint` and `@Index` declared in `@Table` | YES |
| A.1.13 | Unique constraints: `UK_<TABLE>_<DESC>` | YES |
| A.1.14 | Indexes: `IDX_<TABLE>_<COLUMN>` | YES |
| A.1.15 | FK constraints: `FK_<TABLE>_<REF>` via `@ForeignKey(name)` | YES |
| A.1.16 | Use `@Formula` for computed counts, NOT collection `.size()` | YES |
| A.1.17 | `@PrePersist` is the SOLE canonical location for uppercase normalization | YES |
| A.1.18 | Entity has `activate()` and `deactivate()` helpers | YES |
| A.1.19 | No helper methods that iterate/filter lazy `@OneToMany` collections — use repository count queries | YES |

---

## Violations (MUST NOT)

- ❌ `@Builder` instead of `@SuperBuilder`
- ❌ `GenerationType.IDENTITY` or `GenerationType.AUTO`
- ❌ `allocationSize` != 1
- ❌ `@Column(name = "ID")` — must be `ID_PK`
- ❌ FK columns without `_ID_FK` suffix
- ❌ `boolean` primitive for boolean fields — must be `Boolean` wrapper
- ❌ `fetch = FetchType.EAGER` on relationships
- ❌ `orphanRemoval = true` without explicit governance approval
- ❌ Uppercase normalization in mapper or service
- ❌ `entity.setIsActive(true)` in service — must use `activate()` / `deactivate()`
- ❌ Defining `createdAt`/`createdBy` directly (they come from `AuditableEntity`)
- ❌ Using `entity.getChildren().size()` instead of `@Formula`
- ❌ Entity helper methods that iterate/filter lazy collections (e.g., `hasActiveDetails()` using `stream().anyMatch()`)
- ❌ Lowercase or camelCase table names

---

## Example (Real ERP — MasterLookup)

```java
@Entity
@Table(name = "MD_MASTER_LOOKUP",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_MD_MASTER_LOOKUP_KEY", columnNames = {"LOOKUP_KEY"})
    },
    indexes = {
        @Index(name = "IDX_MD_MASTER_LOOKUP_ACTIVE", columnList = "IS_ACTIVE")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class MdMasterLookup extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "master_lookup_seq")
    @SequenceGenerator(name = "master_lookup_seq", sequenceName = "MD_MASTER_LOOKUP_SEQ", allocationSize = 1)
    @Column(name = "ID_PK")
    private Long id;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "LOOKUP_KEY", length = 50, nullable = false)
    private String lookupKey;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Column(name = "DESCRIPTION_EN", length = 200, nullable = false)
    private String descriptionEn;

    @Size(max = 200, message = "{validation.size}")
    @Column(name = "DESCRIPTION_AR", length = 200)
    private String descriptionAr;

    @Column(name = "IS_ACTIVE", nullable = false)
    @Builder.Default
    @Convert(converter = BooleanNumberConverter.class)
    private Boolean isActive = Boolean.TRUE;

    @OneToMany(mappedBy = "masterLookup", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    private List<MdLookupDetail> lookupDetails = new ArrayList<>();

    @Formula("(SELECT COUNT(*) FROM MD_LOOKUP_DETAIL d WHERE d.MASTER_LOOKUP_ID_FK = ID_PK)")
    private Integer detailCount;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) isActive = Boolean.TRUE;
        if (lookupKey != null) lookupKey = lookupKey.toUpperCase();
    }

    @PreUpdate
    protected void onUpdate() {
        if (lookupKey != null) lookupKey = lookupKey.toUpperCase();
    }

    public void activate() { this.isActive = Boolean.TRUE; }
    public void deactivate() { this.isActive = Boolean.FALSE; }
}
```
