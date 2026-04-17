import { AbstractControl, ValidationErrors } from '@angular/forms';

/**
 * Shared Form Error Resolver Utility
 * 
 * Maps Angular validator errors to translation keys.
 * Ensures consistent, localized error messages across all forms.
 * 
 * @requirement FE-REQ-SHARED-001 (Form Error Handling & Localization)
 * @task TASK-FE-SHARED-001
 * 
 * RULES:
 * - Returns ONLY translation keys, never English strings
 * - No UI rendering logic
 * - Stateless function
 * - Reusable across all features
 */

/**
 * Result of error resolution.
 */
export interface FormErrorResult {
  /** Translation key for the error message */
  key: string;
  /** Optional parameters for translation interpolation */
  params?: Record<string, unknown>;
}

/**
 * Get the first error message translation key for a form control.
 * 
 * @param control - The AbstractControl (FormControl/FormGroup/FormArray)
 * @param fieldLabel - Optional human-readable field name for error messages
 * @returns Translation key with optional params, or null if no errors
 * 
 * @example
 * ```typescript
 * // In component
 * const error = getFormFieldError(this.form.get('username'));
 * if (error) {
 *   this.errorMessage = this.translate.instant(error.key, error.params);
 * }
 * ```
 */
export function getFormFieldError(
  control: AbstractControl | null,
  fieldLabel?: string
): FormErrorResult | null {
  if (!control || !control.errors || !(control.touched || control.dirty)) {
    return null;
  }

  const errors: ValidationErrors = control.errors;

  // Angular built-in validators
  if (errors['required']) {
    return { key: 'VALIDATION.REQUIRED' };
  }

  if (errors['min']) {
    return {
      key: 'VALIDATION.MIN_VALUE',
      params: { min: errors['min'].min }
    };
  }

  if (errors['max']) {
    return {
      key: 'VALIDATION.MAX_VALUE',
      params: { max: errors['max'].max }
    };
  }

  if (errors['minlength']) {
    return {
      key: 'VALIDATION.MIN_LENGTH',
      params: { min: errors['minlength'].requiredLength }
    };
  }

  if (errors['maxlength']) {
    return {
      key: 'VALIDATION.MAX_LENGTH',
      params: { max: errors['maxlength'].requiredLength }
    };
  }

  if (errors['email']) {
    return { key: 'VALIDATION.EMAIL_INVALID' };
  }

  if (errors['pattern']) {
    // Pattern errors can have context-specific messages
    // Try to detect common patterns
    const patternValue = errors['pattern'].requiredPattern;
    
    // Check for common patterns (optional: extend as needed)
    if (typeof patternValue === 'string') {
      if (patternValue.includes('[A-Z]') && patternValue.includes('[0-9]')) {
        return { key: 'VALIDATION.PATTERN_UPPERCASE_ALPHANUMERIC' };
      }
      if (patternValue.includes('\\/')) {
        return { key: 'VALIDATION.PATTERN_ROUTE' };
      }
    }
    
    // Fallback for any pattern error
    return { key: 'VALIDATION.PATTERN_INVALID' };
  }

  // Custom validators (extend as needed)
  if (errors['passwordsMismatch']) {
    return { key: 'VALIDATION.PASSWORDS_MISMATCH' };
  }

  if (errors['unique']) {
    return { key: 'VALIDATION.UNIQUE' };
  }

  if (errors['urlPattern']) {
    return { key: 'VALIDATION.URL_INVALID' };
  }

  // Fallback for unknown errors
  return { key: 'VALIDATION.INVALID_VALUE' };
}

/**
 * Validator that checks a URL format (optional field — passes when empty).
 * Returns { urlPattern: true } if the value is present but not a valid URL.
 */
export function urlValidator(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;
  const pattern = /^https?:\/\/(www\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)$/;
  return pattern.test(control.value) ? null : { urlPattern: true };
}

/**
 * Check if a form field has errors and should display error UI.
 * 
 * @param control - The form control to check
 * @returns True if control is invalid and has been touched
 */
export function isFormFieldInvalid(control: AbstractControl | null): boolean {
  return !!(control && control.invalid && (control.dirty || control.touched));
}

/**
 * Get all errors for a form control as an array of translation keys.
 * Useful for displaying multiple errors at once.
 * 
 * @param control - The form control
 * @returns Array of error results
 */
export function getAllFormFieldErrors(
  control: AbstractControl | null
): FormErrorResult[] {
  if (!control || !control.errors) {
    return [];
  }

  const errors: ValidationErrors = control.errors;
  const results: FormErrorResult[] = [];

  // Process each error type
  Object.keys(errors).forEach((errorKey) => {
    const errorValue = errors[errorKey];
    
    switch (errorKey) {
      case 'required':
        results.push({ key: 'VALIDATION.REQUIRED' });
        break;
      case 'min':
        results.push({
          key: 'VALIDATION.MIN_VALUE',
          params: { min: errorValue.min }
        });
        break;
      case 'max':
        results.push({
          key: 'VALIDATION.MAX_VALUE',
          params: { max: errorValue.max }
        });
        break;
      case 'minlength':
        results.push({
          key: 'VALIDATION.MIN_LENGTH',
          params: { min: errorValue.requiredLength }
        });
        break;
      case 'maxlength':
        results.push({
          key: 'VALIDATION.MAX_LENGTH',
          params: { max: errorValue.requiredLength }
        });
        break;
      case 'email':
        results.push({ key: 'VALIDATION.EMAIL_INVALID' });
        break;
      case 'pattern':
        results.push({ key: 'VALIDATION.PATTERN_INVALID' });
        break;
      default:
        results.push({ key: 'VALIDATION.INVALID_VALUE' });
    }
  });

  return results;
}
