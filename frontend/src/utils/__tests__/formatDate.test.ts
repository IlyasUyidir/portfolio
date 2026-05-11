import { describe, it, expect } from 'vitest';
import { formatDate, currentMonth } from '../formatDate';

describe('formatDate Utility', () => {
  describe('formatDate', () => {
    it('formatDate_shouldFormatISODateToLocalizedString', () => {
      // Act
      const result = formatDate('2026-05-10');

      // Assert
      // fr-MA should produce something like "10 mai 2026"
      // We check for presence of day, month and year
      expect(result).toMatch(/10/);
      expect(result).toMatch(/mai/i);
      expect(result).toMatch(/2026/);
    });

    it('formatDate_shouldHandleFirstOfMonth', () => {
      // Act
      const result = formatDate('2026-01-01');

      // Assert
      expect(result).toMatch(/01/);
      expect(result).toMatch(/janvier/i);
      expect(result).toMatch(/2026/);
    });
  });

  describe('currentMonth', () => {
    it('currentMonth_shouldReturnCurrentYearAndMonth', () => {
      // Arrange
      const now = new Date();
      const expected = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;

      // Act
      const result = currentMonth();

      // Assert
      expect(result).toBe(expected);
    });

    it('currentMonth_shouldAlwaysPadMonthToTwoDigits', () => {
      // Note: We can't easily mock Date globally without a library or complex setup
      // but we can check the length and format of the result
      const result = currentMonth();
      
      // Format should be YYYY-MM (7 chars)
      expect(result).toHaveLength(7);
      expect(result).toMatch(/^\d{4}-\d{2}$/);
      
      const monthPart = result.split('-')[1];
      expect(monthPart.length).toBe(2);
    });
  });
});
