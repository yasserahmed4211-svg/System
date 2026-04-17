import { LegalEntityDto, CreateLegalEntityRequest, UpdateLegalEntityRequest } from './legal-entity.model';

export class LegalEntityFormMapper {
  static createEmpty(): Record<string, unknown> {
    return {
      legalEntityPk:         null,
      legalEntityCode:       '',
      legalEntityNameAr:     '',
      legalEntityNameEn:     '',
      countryFk:             null,
      functionalCurrencyFk:  null,
      taxNumber:             '',
      commercialRegNumber:   '',
      fiscalYearStartMonth:  null,
      addressLine1:          '',
      addressLine2:          '',
      cityName:              '',
      phone:                 '',
      email:                 '',
      website:               '',
      statusId:              'ACTIVE'
    };
  }

  static fromDomain(entity: LegalEntityDto): Record<string, unknown> {
    return {
      legalEntityPk:         entity.legalEntityPk,
      legalEntityCode:       entity.legalEntityCode,
      legalEntityNameAr:     entity.legalEntityNameAr ?? '',
      legalEntityNameEn:     entity.legalEntityNameEn ?? '',
      countryFk:             entity.countryFk ?? null,
      functionalCurrencyFk:  entity.functionalCurrencyFk ?? null,
      taxNumber:             entity.taxNumber ?? '',
      commercialRegNumber:   entity.commercialRegNumber ?? '',
      fiscalYearStartMonth:  entity.fiscalYearStartMonth ?? null,
      addressLine1:          entity.addressLine1 ?? '',
      addressLine2:          entity.addressLine2 ?? '',
      cityName:              entity.cityName ?? '',
      phone:                 entity.phone ?? '',
      email:                 entity.email ?? '',
      website:               entity.website ?? '',
      statusId:              entity.statusId ?? 'ACTIVE'
    };
  }

  static toCreateRequest(rawValue: Record<string, unknown>): CreateLegalEntityRequest {
    return {
      legalEntityNameAr:    rawValue['legalEntityNameAr'] as string,
      legalEntityNameEn:    rawValue['legalEntityNameEn'] as string,
      countryFk:            rawValue['countryFk'] as number,
      functionalCurrencyFk: rawValue['functionalCurrencyFk'] as number,
      taxNumber:            (rawValue['taxNumber'] as string) || undefined,
      commercialRegNumber:  (rawValue['commercialRegNumber'] as string) || undefined,
      fiscalYearStartMonth: (rawValue['fiscalYearStartMonth'] as number | null) ?? undefined,
      addressLine1:         (rawValue['addressLine1'] as string) || undefined,
      addressLine2:         (rawValue['addressLine2'] as string) || undefined,
      cityName:             (rawValue['cityName'] as string) || undefined,
      phone:                (rawValue['phone'] as string) || undefined,
      email:                (rawValue['email'] as string) || undefined,
      website:              (rawValue['website'] as string) || undefined,
      statusId:             rawValue['statusId'] as string
    };
  }

  static toUpdateRequest(rawValue: Record<string, unknown>): UpdateLegalEntityRequest {
    return {
      legalEntityNameAr:    rawValue['legalEntityNameAr'] as string,
      legalEntityNameEn:    rawValue['legalEntityNameEn'] as string,
      taxNumber:            (rawValue['taxNumber'] as string) || undefined,
      commercialRegNumber:  (rawValue['commercialRegNumber'] as string) || undefined,
      fiscalYearStartMonth: (rawValue['fiscalYearStartMonth'] as number | null) ?? undefined,
      addressLine1:         (rawValue['addressLine1'] as string) || undefined,
      addressLine2:         (rawValue['addressLine2'] as string) || undefined,
      cityName:             (rawValue['cityName'] as string) || undefined,
      phone:                (rawValue['phone'] as string) || undefined,
      email:                (rawValue['email'] as string) || undefined,
      website:              (rawValue['website'] as string) || undefined,
      statusId:             rawValue['statusId'] as string
    };
  }
}
