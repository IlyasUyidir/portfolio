/**
 * "2026-06-14" → "14 juin 2026"
 */
export const formatDate = (isoDate: string): string => {
  return new Date(isoDate).toLocaleDateString('fr-MA', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  });
};

/**
 * Returns current month as "2026-06"
 */
export const currentMonth = (): string => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
};
