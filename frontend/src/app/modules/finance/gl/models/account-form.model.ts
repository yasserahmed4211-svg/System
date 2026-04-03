import {
  AccountChartDto,
  CreateAccountRequest,
  UpdateAccountRequest
} from './gl.model';

// ============================================
// FORM MODEL
// ============================================

/**
 * Shared form model for Create and Edit account.
 * Decouples form state from domain DTOs and eliminates
 * duplicate patchValue / mapping logic in the component.
 */
export interface AccountFormModel {
  accountChartName: string;
  accountType: string | null;
  accountChartFk: number | null;
  organizationFk: number | null;
  organizationSubFk: number | null;
  isActive: boolean;
}

// ============================================
// FACTORY / MAPPER
// ============================================

export const AccountFormMapper = {

  /** Empty form model for Create mode */
  createEmpty(): AccountFormModel {
    return {
      accountChartName: '',
      accountType: null,
      accountChartFk: null,
      organizationFk: null,
      organizationSubFk: null,
      isActive: true
    };
  },

  /** Map domain DTO → form model for Edit mode */
  fromDomain(dto: AccountChartDto): AccountFormModel {
    return {
      accountChartName: dto.accountChartName,
      accountType: dto.accountType,
      accountChartFk: dto.accountChartFk,
      organizationFk: dto.organizationFk,
      organizationSubFk: dto.organizationSubFk ?? null,
      isActive: dto.isActive
    };
  },

  /** Map form model → CreateAccountRequest */
  toCreateRequest(model: AccountFormModel): CreateAccountRequest {
    return {
      accountChartName: model.accountChartName,
      accountType: model.accountType!,
      accountChartFk: model.accountChartFk ?? undefined,
      organizationFk: model.organizationFk!,
      organizationSubFk: model.organizationSubFk ?? undefined,
      isActive: model.isActive
    };
  },

  /** Map form model → UpdateAccountRequest */
  toUpdateRequest(model: AccountFormModel): UpdateAccountRequest {
    return {
      accountChartName: model.accountChartName,
      accountType: model.accountType!,
      accountChartFk: model.accountChartFk ?? undefined,
      organizationFk: model.organizationFk!,
      organizationSubFk: model.organizationSubFk ?? undefined,
      isActive: model.isActive
    };
  }
};
