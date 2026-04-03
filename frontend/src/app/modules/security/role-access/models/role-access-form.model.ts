import {
  RoleDto,
  CreateRoleRequest
} from './role-access.model';

// ============================================
// FORM MODEL
// ============================================

/**
 * Shared form model for Create role.
 * Decouples form state from domain DTOs and eliminates
 * duplicate patchValue / mapping logic in the component.
 */
export interface RoleAccessFormModel {
  name: string;
  active: boolean;
}

// ============================================
// FACTORY / MAPPER
// ============================================

export const RoleAccessFormMapper = {

  /** Empty form model for Create mode */
  createEmpty(): RoleAccessFormModel {
    return {
      name: '',
      active: true
    };
  },

  /** Map domain DTO → form model for Edit mode */
  fromDomain(dto: RoleDto): RoleAccessFormModel {
    return {
      name: dto.roleName,
      active: dto.active
    };
  },

  /** Derive roleCode from roleName */
  deriveRoleCode(roleName: string): string {
    return String(roleName ?? '')
      .trim()
      .toUpperCase()
      .replace(/\s+/g, '_')
      .replace(/[^A-Z0-9_]/g, '');
  },

  /** Map form model → CreateRoleRequest */
  toCreateRequest(model: RoleAccessFormModel): CreateRoleRequest {
    return {
      roleName: model.name,
      roleCode: RoleAccessFormMapper.deriveRoleCode(model.name),
      active: model.active
    };
  }
};
