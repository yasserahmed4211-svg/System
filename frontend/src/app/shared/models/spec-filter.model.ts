export type SpecOperator = 'eq' | 'like' | 'gt' | 'lt' | 'in' | 'isNull' | 'isNotNull';

export interface SpecFilter {
  field: string;
  operator: SpecOperator;
  value?: string | number | boolean | Array<string | number> | null;
}

export interface SpecValueOption {
  value: string | number | boolean;
  label: string;
}

export interface SpecFieldOption {
  value: string;
  label: string;
  /** Optional typed options. When provided, the filter UI will render a dropdown instead of free text. */
  options?: SpecValueOption[];
}

export interface SpecOperatorOption {
  value: SpecOperator;
  label: string;
}
