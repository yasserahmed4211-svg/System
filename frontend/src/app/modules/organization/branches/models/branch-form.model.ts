import { BranchDto, CreateBranchRequest, UpdateBranchRequest } from './branch.model';

export class BranchFormMapper {
  static createEmpty(): Record<string, unknown> {
    return {
      branchPk:        null,
      branchCode:      '',
      legalEntityFk:   null,
      regionFk:        null,
      branchNameAr:    '',
      branchNameEn:    '',
      branchTypeId:    '',
      isHeadquarterFl: 0,
      statusId:        'ACTIVE',
      addressLine1:    '',
      addressLine2:    '',
      cityName:        '',
      phone:           '',
      email:           ''
    };
  }

  static fromDomain(entity: BranchDto): Record<string, unknown> {
    return {
      branchPk:        entity.branchPk,
      branchCode:      entity.branchCode,
      legalEntityFk:   entity.legalEntityFk ?? null,
      regionFk:        entity.regionFk ?? null,
      branchNameAr:    entity.branchNameAr ?? '',
      branchNameEn:    entity.branchNameEn ?? '',
      branchTypeId:    entity.branchTypeId ?? '',
      isHeadquarterFl: entity.isHeadquarterFl ?? 0,
      statusId:        entity.statusId ?? 'ACTIVE',
      addressLine1:    entity.addressLine1 ?? '',
      addressLine2:    entity.addressLine2 ?? '',
      cityName:        entity.cityName ?? '',
      phone:           entity.phone ?? '',
      email:           entity.email ?? ''
    };
  }

  static toCreateRequest(rawValue: Record<string, unknown>): CreateBranchRequest {
    return {
      legalEntityFk:   rawValue['legalEntityFk'] as number,
      regionFk:        (rawValue['regionFk'] as number | null) ?? undefined,
      branchNameAr:    rawValue['branchNameAr'] as string,
      branchNameEn:    rawValue['branchNameEn'] as string,
      branchTypeId:    rawValue['branchTypeId'] as string,
      isHeadquarterFl: (rawValue['isHeadquarterFl'] as number) ?? 0,
      statusId:        rawValue['statusId'] as string,
      addressLine1:    (rawValue['addressLine1'] as string) || undefined,
      addressLine2:    (rawValue['addressLine2'] as string) || undefined,
      cityName:        (rawValue['cityName'] as string) || undefined,
      phone:           (rawValue['phone'] as string) || undefined,
      email:           (rawValue['email'] as string) || undefined
    };
  }

  static toUpdateRequest(rawValue: Record<string, unknown>): UpdateBranchRequest {
    return {
      branchNameAr:    rawValue['branchNameAr'] as string,
      branchNameEn:    rawValue['branchNameEn'] as string,
      branchTypeId:    rawValue['branchTypeId'] as string,
      isHeadquarterFl: (rawValue['isHeadquarterFl'] as number) ?? 0,
      statusId:        rawValue['statusId'] as string,
      addressLine1:    (rawValue['addressLine1'] as string) || undefined,
      addressLine2:    (rawValue['addressLine2'] as string) || undefined,
      cityName:        (rawValue['cityName'] as string) || undefined,
      phone:           (rawValue['phone'] as string) || undefined,
      email:           (rawValue['email'] as string) || undefined
    };
  }
}
