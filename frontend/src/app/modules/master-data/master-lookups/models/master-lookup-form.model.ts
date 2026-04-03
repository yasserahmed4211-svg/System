import {
  MasterLookupDto,
  CreateMasterLookupRequest,
  UpdateMasterLookupRequest
} from './master-lookup.model';

// ============================================
// FORM MODEL
// ============================================

/**
 * Shared form model for Create and Edit master lookup.
 * Decouples form state from domain DTOs and eliminates
 * duplicate patchValue / mapping logic in the component.
 */
export interface MasterLookupFormModel {
  lookupKey: string;
  lookupName: string;
  lookupNameEn: string;
  description: string;
}

// ============================================
// FACTORY / MAPPER
// ============================================

export const MasterLookupFormMapper = {

  /** Empty form model for Create mode */
  createEmpty(): MasterLookupFormModel {
    return {
      lookupKey: '',
      lookupName: '',
      lookupNameEn: '',
      description: ''
    };
  },

  /** Map domain DTO → form model for Edit mode */
  fromDomain(dto: MasterLookupDto): MasterLookupFormModel {
    return {
      lookupKey: dto.lookupKey,
      lookupName: dto.lookupName,
      lookupNameEn: dto.lookupNameEn ?? '',
      description: dto.description ?? ''
    };
  },

  /** Map form model → CreateMasterLookupRequest */
  toCreateRequest(model: MasterLookupFormModel): CreateMasterLookupRequest {
    return {
      lookupKey: model.lookupKey.toUpperCase(),
      lookupName: model.lookupName,
      lookupNameEn: model.lookupNameEn.trim() || undefined,
      description: model.description.trim() || undefined
    };
  },

  /** Map form model → UpdateMasterLookupRequest */
  toUpdateRequest(model: MasterLookupFormModel): UpdateMasterLookupRequest {
    return {
      lookupName: model.lookupName,
      lookupNameEn: model.lookupNameEn.trim() || undefined,
      description: model.description.trim() || undefined
    };
  }
};
