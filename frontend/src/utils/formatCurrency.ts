/**
 * Converts centimes to display string.
 * 1250000 → "+12 500,00 DH"
 * -32000  → "-320,00 DH"
 */
export const formatCurrency = (centimes: number, showSign = false): string => {
  const value = centimes / 100;
  const formatted = new Intl.NumberFormat('fr-MA', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Math.abs(value));

  const sign = centimes < 0 ? '-' : showSign ? '+' : '';
  return `${sign}${formatted} DH`;
};

/**
 * Converts user input (e.g. "320.50") to centimes for the API.
 * "320.50" → 32050
 */
export const toCentimes = (input: string | number): number => {
  return Math.round(parseFloat(String(input)) * 100);
};

/**
 * Converts centimes back to input value string for form fields.
 * 32050 → "320.50"
 */
export const fromCentimes = (centimes: number): string => {
  return (centimes / 100).toFixed(2);
};
