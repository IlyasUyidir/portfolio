import { describe, it, expect } from 'vitest';
import { formatCurrency, toCentimes, fromCentimes } from '../formatCurrency';

describe('formatCurrency Utility', () => {
  describe('formatCurrency', () => {
    it('formatCurrency_shouldDivideByHundred_andApplyFrMaFormat', () => {
      // Arrange
      const amount = 50000; // 500.00 DH

      // Act
      const result = formatCurrency(amount);

      // Assert
      // Use a regex to be flexible with different space characters (non-breaking space vs regular space)
      expect(result).toMatch(/500,00\sDH/);
    });

    it('formatCurrency_withNegativeAmount_shouldShowMinusSign', () => {
      // Arrange
      const amount = -32000; // -320.00 DH

      // Act
      const result = formatCurrency(amount, false);

      // Assert
      expect(result).toMatch(/-320,00\sDH/);
    });

    it('formatCurrency_withShowSignTrue_shouldShowPlusForPositive', () => {
      // Arrange
      const amount = 10000; // +100.00 DH

      // Act
      const result = formatCurrency(amount, true);

      // Assert
      expect(result).toMatch(/\+100,00\sDH/);
    });

    it('formatCurrency_withZero_shouldShowZero', () => {
      // Act & Assert
      expect(formatCurrency(0)).toMatch(/0,00\sDH/);
    });

    it('formatCurrency_shouldHandleLargeNumbersWithThousandSeparators', () => {
      // Arrange
      const amount = 1250000; // 12 500.00 DH

      // Act
      const result = formatCurrency(amount);

      // fr-MA uses space (often non-breaking) or dot as thousand separator depending on env
      expect(result).toMatch(/12[.\s]500,00\sDH/);
    });
  });

  describe('toCentimes', () => {
    it('toCentimes_shouldMultiplyByHundred', () => {
      expect(toCentimes(100)).toBe(10000);
      expect(toCentimes("100")).toBe(10000);
    });

    it('toCentimes_shouldHandleStringInputWithDecimals', () => {
      expect(toCentimes("320.50")).toBe(32050);
      expect(toCentimes("0.01")).toBe(1);
    });

    it('toCentimes_shouldRoundCorrectly', () => {
      // 1.999 * 100 = 199.9 -> 200
      expect(toCentimes("1.999")).toBe(200);
      // 1.994 * 100 = 199.4 -> 199
      expect(toCentimes("1.994")).toBe(199);
    });

    it('toCentimes_withEmptyString_shouldReturnNaN', () => {
      expect(toCentimes("")).toBeNaN();
    });
  });

  describe('fromCentimes', () => {
    it('fromCentimes_shouldDivideByHundred', () => {
      expect(fromCentimes(32050)).toBe("320.50");
    });

    it('fromCentimes_shouldPadDecimalPlaces', () => {
      expect(fromCentimes(100)).toBe("1.00");
      expect(fromCentimes(0)).toBe("0.00");
    });

    it('fromCentimes_shouldHandleNegativeAmounts', () => {
      expect(fromCentimes(-5000)).toBe("-50.00");
    });
  });

  describe('Roundtrip', () => {
    it('formatCurrency_centimesRoundtrip_shouldBeLossless', () => {
      // Arrange
      const amount = 32050;

      // Act
      const formatted = fromCentimes(amount);
      const result = toCentimes(formatted);

      // Assert
      expect(formatted).toBe("320.50");
      expect(result).toBe(amount);
    });
  });
});
