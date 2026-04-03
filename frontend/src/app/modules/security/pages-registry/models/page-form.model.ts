import {
  PageDto,
  CreatePageRequest,
  UpdatePageRequest
} from './page.model';

// ============================================
// FORM MODEL
// ============================================

/**
 * Shared form model for Create and Edit page.
 * Decouples form state from domain DTOs and eliminates
 * duplicate patchValue / mapping logic in the component.
 */
export interface PageFormModel {
  pageCode: string;
  nameAr: string;
  nameEn: string;
  route: string;
  icon: string;
  module: string;
  parentId: number | null;
  displayOrder: number;
  active: boolean;
}

// ============================================
// FACTORY / MAPPER
// ============================================

export const PageFormMapper = {

  /** Empty form model for Create mode */
  createEmpty(): PageFormModel {
    return {
      pageCode: '',
      nameAr: '',
      nameEn: '',
      route: '',
      icon: '',
      module: '',
      parentId: null,
      displayOrder: 0,
      active: true
    };
  },

  /** Map domain DTO → form model for Edit mode */
  fromDomain(dto: PageDto): PageFormModel {
    return {
      pageCode: dto.pageCode,
      nameAr: dto.nameAr,
      nameEn: dto.nameEn,
      route: dto.route,
      icon: dto.icon || '',
      module: dto.module || '',
      parentId: dto.parentId || null,
      displayOrder: dto.displayOrder,
      active: dto.active
    };
  },

  /** Map form model → CreatePageRequest */
  toCreateRequest(model: PageFormModel): CreatePageRequest {
    return {
      pageCode: model.pageCode,
      nameAr: model.nameAr,
      nameEn: model.nameEn,
      route: model.route,
      icon: model.icon.trim() || undefined,
      module: model.module.trim() || undefined,
      parentId: model.parentId ?? undefined,
      displayOrder: model.displayOrder,
      active: model.active
    };
  },

  /** Map form model → UpdatePageRequest */
  toUpdateRequest(model: PageFormModel): UpdatePageRequest {
    return {
      nameAr: model.nameAr,
      nameEn: model.nameEn,
      route: model.route,
      icon: model.icon.trim() || undefined,
      module: model.module.trim() || undefined,
      parentId: model.parentId ?? undefined,
      displayOrder: model.displayOrder,
      active: model.active
    };
  }
};
