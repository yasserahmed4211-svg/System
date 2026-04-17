import { RegionDto, CreateRegionRequest, UpdateRegionRequest } from './region.model';

export class RegionFormMapper {
  static createEmpty(): Record<string, unknown> {
    return {
      regionPk:      null,
      regionCode:    '',
      legalEntityFk: null,
      regionNameAr:  '',
      regionNameEn:  '',
      descriptionAr: '',
      statusId:      'ACTIVE'
    };
  }

  static fromDomain(region: RegionDto): Record<string, unknown> {
    return {
      regionPk:      region.regionPk,
      regionCode:    region.regionCode,
      legalEntityFk: region.legalEntityFk ?? null,
      regionNameAr:  region.regionNameAr ?? '',
      regionNameEn:  region.regionNameEn ?? '',
      descriptionAr: region.descriptionAr ?? '',
      statusId:      region.statusId ?? 'ACTIVE'
    };
  }

  static toCreateRequest(rawValue: Record<string, unknown>): CreateRegionRequest {
    return {
      legalEntityFk: rawValue['legalEntityFk'] as number,
      regionNameAr:  rawValue['regionNameAr'] as string,
      regionNameEn:  rawValue['regionNameEn'] as string,
      descriptionAr: (rawValue['descriptionAr'] as string) || undefined,
      statusId:      rawValue['statusId'] as string
    };
  }

  static toUpdateRequest(rawValue: Record<string, unknown>): UpdateRegionRequest {
    return {
      regionNameAr:  rawValue['regionNameAr'] as string,
      regionNameEn:  rawValue['regionNameEn'] as string,
      descriptionAr: (rawValue['descriptionAr'] as string) || undefined,
      statusId:      rawValue['statusId'] as string
    };
  }
}
