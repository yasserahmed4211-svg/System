import {
  UserDto,
  CreateUserRequest,
  UpdateUserRequest
} from './user.model';

// ============================================
// FORM MODEL
// ============================================

/**
 * Shared form model for Create and Edit user.
 * Decouples form state from domain DTOs and eliminates
 * duplicate patchValue / mapping logic in the component.
 */
export interface UserFormModel {
  username: string;
  password: string;
  tenantId: string;
  enabled: boolean;
  roles: string[];
}

// ============================================
// FACTORY / MAPPER
// ============================================

export const UserFormMapper = {

  /** Empty form model for Create mode */
  createEmpty(): UserFormModel {
    return {
      username: '',
      password: '',
      tenantId: '',
      enabled: true,
      roles: []
    };
  },

  /** Map domain DTO → form model for Edit mode */
  fromDomain(dto: UserDto): UserFormModel {
    const normalizedRoles = Array.isArray(dto.roles)
      ? dto.roles
          .map(r => {
            if (typeof r === 'string') return r;
            const obj = r as any;
            return (obj.roleCode ?? obj.roleName ?? obj.name) as string | undefined;
          })
          .filter((v): v is string => typeof v === 'string' && v.trim().length > 0)
      : [];

    return {
      username: dto.username,
      password: '',
      tenantId: dto.tenantId || '',
      enabled: dto.enabled,
      roles: normalizedRoles
    };
  },

  /** Map form model → CreateUserRequest */
  toCreateRequest(model: UserFormModel): CreateUserRequest {
    return {
      username: model.username,
      password: model.password,
      enabled: model.enabled,
      tenantId: model.tenantId || undefined,
      roleNames: model.roles.length > 0 ? model.roles : undefined
    };
  },

  /** Map form model → UpdateUserRequest */
  toUpdateRequest(model: UserFormModel): UpdateUserRequest {
    const request: UpdateUserRequest = {
      username: model.username,
      enabled: model.enabled,
      tenantId: model.tenantId || undefined,
      roleNames: model.roles.length > 0 ? model.roles : undefined
    };
    if (model.password) request.password = model.password;
    return request;
  }
};
