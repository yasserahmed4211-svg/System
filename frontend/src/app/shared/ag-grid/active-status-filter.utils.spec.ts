/**
 * Unit tests for Active Status Filter Utilities
 *
 * Tests the core conversion and utility functions for Active/Inactive filtering.
 */

import {
  activeFilterToApi,
  apiToActiveFilter,
  normalizeActiveValue,
  createActiveColumnDef,
  createActiveFilterOptions,
  createActiveSearchFilter,
  getActiveFilterFromSearchFilters,
  ActiveFilterValue,
  ActiveStatus,
  ActiveColumnLabels,
  DEFAULT_ACTIVE_LABELS,
  DEFAULT_ACTIVE_FILTER
} from './active-status-filter.utils';

describe('Active Status Filter Utilities', () => {
  describe('activeFilterToApi', () => {
    it('should convert "active" to true', () => {
      expect(activeFilterToApi('active')).toBe(true);
    });

    it('should convert "inactive" to false', () => {
      expect(activeFilterToApi('inactive')).toBe(false);
    });

    it('should convert "all" to null', () => {
      expect(activeFilterToApi('all')).toBeNull();
    });
  });

  describe('apiToActiveFilter', () => {
    it('should convert true to "active"', () => {
      expect(apiToActiveFilter(true)).toBe('active');
    });

    it('should convert false to "inactive"', () => {
      expect(apiToActiveFilter(false)).toBe('inactive');
    });

    it('should convert null to "all"', () => {
      expect(apiToActiveFilter(null)).toBe('all');
    });
  });

  describe('normalizeActiveValue', () => {
    it('should return boolean as-is', () => {
      expect(normalizeActiveValue(true)).toBe(true);
      expect(normalizeActiveValue(false)).toBe(false);
    });

    it('should return null for null/undefined', () => {
      expect(normalizeActiveValue(null)).toBeNull();
      expect(normalizeActiveValue(undefined)).toBeNull();
    });

    it('should convert "true" string variants to true', () => {
      expect(normalizeActiveValue('true')).toBe(true);
      expect(normalizeActiveValue('TRUE')).toBe(true);
      expect(normalizeActiveValue('active')).toBe(true);
      expect(normalizeActiveValue('ACTIVE')).toBe(true);
      expect(normalizeActiveValue('1')).toBe(true);
      expect(normalizeActiveValue('yes')).toBe(true);
      expect(normalizeActiveValue('YES')).toBe(true);
    });

    it('should convert "false" string variants to false', () => {
      expect(normalizeActiveValue('false')).toBe(false);
      expect(normalizeActiveValue('FALSE')).toBe(false);
      expect(normalizeActiveValue('inactive')).toBe(false);
      expect(normalizeActiveValue('INACTIVE')).toBe(false);
      expect(normalizeActiveValue('0')).toBe(false);
      expect(normalizeActiveValue('no')).toBe(false);
      expect(normalizeActiveValue('NO')).toBe(false);
    });

    it('should convert "all" and empty string to null', () => {
      expect(normalizeActiveValue('all')).toBeNull();
      expect(normalizeActiveValue('')).toBeNull();
      expect(normalizeActiveValue('unknown')).toBeNull();
    });

    it('should handle number values defensively', () => {
      expect(normalizeActiveValue(1)).toBe(true);
      expect(normalizeActiveValue(0)).toBe(false);
      expect(normalizeActiveValue(2)).toBeNull();
      expect(normalizeActiveValue(-1)).toBeNull();
    });
  });

  describe('createActiveColumnDef', () => {
    const labels: ActiveColumnLabels = {
      active: 'Active',
      inactive: 'Inactive',
      all: 'All'
    };

    it('should create column definition with correct field', () => {
      const colDef = createActiveColumnDef(labels);
      expect(colDef.field).toBe('active');
    });

    it('should use agTextColumnFilter (not Enterprise SetFilter)', () => {
      const colDef = createActiveColumnDef(labels);
      expect(colDef.filter).toBe('agTextColumnFilter');
    });

    it('should be sortable', () => {
      const colDef = createActiveColumnDef(labels);
      expect(colDef.sortable).toBe(true);
    });

    it('should allow override options', () => {
      const colDef = createActiveColumnDef(labels, {
        field: 'enabled',
        maxWidth: 100
      });
      expect(colDef.field).toBe('enabled');
      expect(colDef.maxWidth).toBe(100);
    });

    it('should have cell renderer function', () => {
      const colDef = createActiveColumnDef(labels);
      expect(colDef.cellRenderer).toBeDefined();
      expect(typeof colDef.cellRenderer).toBe('function');
    });

    it('should have value formatter function', () => {
      const colDef = createActiveColumnDef(labels);
      expect(colDef.valueFormatter).toBeDefined();
      expect(typeof colDef.valueFormatter).toBe('function');
    });
  });

  describe('createActiveFilterOptions', () => {
    const labels: ActiveColumnLabels = {
      active: 'نشط',
      inactive: 'غير نشط',
      all: 'الكل'
    };

    it('should create three filter options', () => {
      const options = createActiveFilterOptions(labels);
      expect(options.length).toBe(3);
    });

    it('should have correct values and labels', () => {
      const options = createActiveFilterOptions(labels);
      
      expect(options[0]).toEqual({ value: 'active', label: 'نشط', apiValue: true });
      expect(options[1]).toEqual({ value: 'inactive', label: 'غير نشط', apiValue: false });
      expect(options[2]).toEqual({ value: 'all', label: 'الكل', apiValue: null });
    });
  });

  describe('createActiveSearchFilter', () => {
    it('should return filter for "active"', () => {
      const filter = createActiveSearchFilter('active');
      expect(filter).toEqual({
        field: 'active',
        op: 'EQ',
        value: true
      });
    });

    it('should return filter for "inactive"', () => {
      const filter = createActiveSearchFilter('inactive');
      expect(filter).toEqual({
        field: 'active',
        op: 'EQ',
        value: false
      });
    });

    it('should return null for "all"', () => {
      const filter = createActiveSearchFilter('all');
      expect(filter).toBeNull();
    });

    it('should use custom field name', () => {
      const filter = createActiveSearchFilter('active', 'enabled');
      expect(filter?.field).toBe('enabled');
    });
  });

  describe('getActiveFilterFromSearchFilters', () => {
    it('should return "active" when filter has value true', () => {
      const filters = [
        { field: 'active', op: 'EQ', value: true }
      ];
      expect(getActiveFilterFromSearchFilters(filters)).toBe('active');
    });

    it('should return "inactive" when filter has value false', () => {
      const filters = [
        { field: 'active', op: 'EQ', value: false }
      ];
      expect(getActiveFilterFromSearchFilters(filters)).toBe('inactive');
    });

    it('should return "all" when no filter exists', () => {
      const filters: Array<{ field: string; op: string; value?: unknown }> = [];
      expect(getActiveFilterFromSearchFilters(filters)).toBe('all');
    });

    it('should return "all" when filter has different field', () => {
      const filters = [
        { field: 'enabled', op: 'EQ', value: true }
      ];
      expect(getActiveFilterFromSearchFilters(filters)).toBe('all');
    });

    it('should use custom field name', () => {
      const filters = [
        { field: 'enabled', op: 'EQ', value: true }
      ];
      expect(getActiveFilterFromSearchFilters(filters, 'enabled')).toBe('active');
    });
  });

  describe('Constants', () => {
    it('should have correct default labels', () => {
      expect(DEFAULT_ACTIVE_LABELS.active).toBe('Active');
      expect(DEFAULT_ACTIVE_LABELS.inactive).toBe('Inactive');
      expect(DEFAULT_ACTIVE_LABELS.all).toBe('All');
    });

    it('should have correct default filter', () => {
      expect(DEFAULT_ACTIVE_FILTER).toBe('active');
    });
  });

  describe('Round-trip conversions', () => {
    it('should round-trip UI → API → UI correctly', () => {
      const uiValues: ActiveFilterValue[] = ['active', 'inactive', 'all'];
      
      for (const uiValue of uiValues) {
        const apiValue = activeFilterToApi(uiValue);
        const backToUi = apiToActiveFilter(apiValue);
        expect(backToUi).toBe(uiValue);
      }
    });

    it('should round-trip API → UI → API correctly', () => {
      const apiValues: ActiveStatus[] = [true, false, null];
      
      for (const apiValue of apiValues) {
        const uiValue = apiToActiveFilter(apiValue);
        const backToApi = activeFilterToApi(uiValue);
        expect(backToApi).toBe(apiValue);
      }
    });
  });
});
