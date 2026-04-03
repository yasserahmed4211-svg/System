# ERP System — Copilot Global Instructions

## MANDATORY: Read Skills Before Code Generation

Before generating, modifying, or reviewing ANY code, you MUST read the relevant SKILL.md file(s) from `.github/skills/`.

### Backend Tasks (any work under `backend/`)

When creating or modifying backend artifacts, read the matching skill BEFORE writing code:

| Task | Skill to Read |
|------|--------------|
| Creating/modifying an Entity | `.github/skills/backend/create-entity/SKILL.md` |
| Creating/modifying a Repository | `.github/skills/backend/create-repository/SKILL.md` |
| Creating/modifying DTOs | `.github/skills/backend/create-dto/SKILL.md` |
| Creating/modifying a Mapper | `.github/skills/backend/create-mapper/SKILL.md` |
| Creating/modifying a Service | `.github/skills/backend/create-service/SKILL.md` |
| Creating/modifying a Controller | `.github/skills/backend/create-controller/SKILL.md` |
| Reviewing/validating backend code | `.github/skills/backend/enforce-backend-contract/SKILL.md` |
| Adding/reviewing caching | `.github/skills/backend/enforce-caching-rules/SKILL.md` |
| Adding/reviewing error handling | `.github/skills/backend/enforce-error-handling/SKILL.md` |
| Validating a complete feature | `.github/skills/backend/validate-backend-feature/SKILL.md` |

### Frontend Tasks (any work under `frontend/`)

When creating or modifying frontend artifacts, read the matching skill BEFORE writing code:

| Task | Skill to Read |
|------|--------------|
| Creating/modifying models/DTOs | `.github/skills/frontend/create-models/SKILL.md` |
| Creating/modifying API service | `.github/skills/frontend/create-api-service/SKILL.md` |
| Creating/modifying a Facade | `.github/skills/frontend/create-facade/SKILL.md` |
| Creating/modifying components | `.github/skills/frontend/create-components/SKILL.md` |
| Creating/modifying routing | `.github/skills/frontend/create-routing/SKILL.md` |
| Reviewing frontend architecture | `.github/skills/frontend/enforce-frontend-architecture/SKILL.md` |
| Reviewing UI/UX & data display | `.github/skills/frontend/enforce-ui-ux/SKILL.md` |
| Reviewing code reusability | `.github/skills/frontend/enforce-reusability/SKILL.md` |
| Reviewing permissions | `.github/skills/frontend/enforce-permissions/SKILL.md` |
| Reviewing state management | `.github/skills/frontend/enforce-state-management/SKILL.md` |
| Validating a complete feature | `.github/skills/frontend/validate-frontend-feature/SKILL.md` |

### DevOps Tasks (any work under `deploy/`)

When creating or modifying deployment artifacts, read the matching skill BEFORE writing code:

| Task | Skill to Read |
|------|--------------|
| Creating/modifying deployment setup | `.github/skills/devops/deploy/SKILL.md` |
| Creating/modifying Dockerfiles | `.github/skills/devops/deploy/SKILL.md` |
| Creating/modifying docker-compose | `.github/skills/devops/deploy/SKILL.md` |
| Creating/modifying nginx config | `.github/skills/devops/deploy/SKILL.md` |

## Execution Order

### Backend Feature Implementation (STRICT order)
```
1. Entity          → read create-entity/SKILL.md
2. Repository      → read create-repository/SKILL.md
3. DTOs            → read create-dto/SKILL.md
4. Mapper          → read create-mapper/SKILL.md
5. Error Codes     → read enforce-error-handling/SKILL.md
6. Permissions     → (defined in SecurityPermissions.java)
7. Service         → read create-service/SKILL.md
8. Controller      → read create-controller/SKILL.md
9. Unit Tests      → read validate-backend-feature/SKILL.md
```

### Frontend Feature Implementation (STRICT order)
```
1. Models          → read create-models/SKILL.md
2. API Service     → read create-api-service/SKILL.md
3. Facade          → read create-facade/SKILL.md
4. Routing         → read create-routing/SKILL.md
5. Components      → read create-components/SKILL.md
6. Validation      → read validate-frontend-feature/SKILL.md
```

### Deployment (run AFTER backend + frontend are complete)
```
1. Deploy           → read devops/deploy/SKILL.md
```

## Rules

- NEVER generate backend code without first reading the corresponding SKILL.md
- NEVER generate frontend code without first reading the corresponding SKILL.md
- When implementing a full feature, read ALL relevant skills in execution order
- When a task spans multiple layers (e.g., entity + repository + DTOs), read ALL relevant skills
- After completing a feature, run the validation skill to verify compliance
- Reference existing implementations in the codebase as canonical examples
