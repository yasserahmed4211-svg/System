import { themeQuartz } from 'ag-grid-community';

export function createAgGridTheme(isDark: boolean) {
  return themeQuartz.withParams({
    backgroundColor: isDark ? '#1a1d29' : '#ffffff',
    foregroundColor: isDark ? '#e4e7eb' : '#212529',
    borderColor: isDark ? '#2d3139' : '#dee2e6',
    headerBackgroundColor: isDark ? '#252833' : '#f8f9fa',
    headerTextColor: isDark ? '#e4e7eb' : '#495057',
    oddRowBackgroundColor: isDark ? '#1e2029' : '#ffffff',
    rowHoverColor: isDark ? '#252833' : '#f8f9fa',
    selectedRowBackgroundColor: isDark ? '#2d3139' : '#e9ecef',
    accentColor: '#5b6ecc',
    spacing: 8,
    borderRadius: 4
  });
}
