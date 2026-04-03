import {
  AccRuleHdrDto,
  CreateRuleRequest,
  UpdateRuleRequest,
  RuleLineRequest
} from './gl.model';

// ============================================
// FORM MODEL
// ============================================

/**
 * Shared form model for Create and Edit accounting rule.
 * Decouples form state from domain DTOs and eliminates
 * duplicate patchValue / mapping logic in the component.
 */
export interface RuleFormModel {
  companyId: number | null;
  sourceUnit: string;
  documentType: string;
  isActive: boolean;
  debitLines: RuleLineFormModel[];
  creditLines: RuleLineFormModel[];
}

export interface RuleLineFormModel {
  accountIdFk: number | null;
  entrySide: string;
  priority: number;
  amountSourceType: string;
  amountSourceValue: number | null;
  paymentTypeCode: string | null;
  entityType: string | null;
}

// ============================================
// FACTORY / MAPPER
// ============================================

export const RuleFormMapper = {

  /** Empty form model for Create mode */
  createEmpty(): RuleFormModel {
    return {
      companyId: null,
      sourceUnit: '',
      documentType: '',
      isActive: true,
      debitLines: [],
      creditLines: []
    };
  },

  /** Map domain DTO → form model for Edit mode */
  fromDomain(dto: AccRuleHdrDto): RuleFormModel {
    return {
      companyId: dto.companyIdFk,
      sourceUnit: dto.sourceModule,
      documentType: dto.sourceDocType,
      isActive: dto.isActive,
      debitLines: (dto.lines ?? [])
        .filter(line => line.entrySide === 'DEBIT')
        .map(line => ({
          accountIdFk: line.accountIdFk,
          entrySide: line.entrySide,
          priority: line.priority,
          amountSourceType: line.amountSourceType,
          amountSourceValue: line.amountSourceValue ?? null,
          paymentTypeCode: line.paymentTypeCode ?? null,
          entityType: line.entityType ?? null
        })),
      creditLines: (dto.lines ?? [])
        .filter(line => line.entrySide === 'CREDIT')
        .map(line => ({
        accountIdFk: line.accountIdFk,
        entrySide: line.entrySide,
        priority: line.priority,
        amountSourceType: line.amountSourceType,
        amountSourceValue: line.amountSourceValue ?? null,
        paymentTypeCode: line.paymentTypeCode ?? null,
        entityType: line.entityType ?? null
      }))
    };
  },

  /** Map form model → CreateRuleRequest */
  toCreateRequest(model: RuleFormModel): CreateRuleRequest {
    return {
      companyId: model.companyId!,
      sourceUnit: model.sourceUnit,
      documentType: model.documentType,
      isActive: model.isActive,
      debitLines: model.debitLines.map(RuleFormMapper.toLineRequest),
      creditLines: model.creditLines.map(RuleFormMapper.toLineRequest)
    };
  },

  /** Map form model → UpdateRuleRequest */
  toUpdateRequest(model: RuleFormModel): UpdateRuleRequest {
    return {
      companyId: model.companyId!,
      sourceUnit: model.sourceUnit,
      documentType: model.documentType,
      isActive: model.isActive,
      debitLines: model.debitLines.map(RuleFormMapper.toLineRequest),
      creditLines: model.creditLines.map(RuleFormMapper.toLineRequest)
    };
  },

  /** Map a single line form model → RuleLineRequest */
  toLineRequest(line: RuleLineFormModel): RuleLineRequest {
    return {
      accountIdFk: line.accountIdFk!,
      entrySide: line.entrySide,
      priority: line.priority,
      amountSourceType: line.amountSourceType,
      amountSourceValue: line.amountSourceValue ?? undefined,
      paymentTypeCode: line.paymentTypeCode ?? undefined,
      entityType: line.entityType || undefined
    };
  }
};
