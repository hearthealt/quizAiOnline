export const formatCount = (count?: number) => {
  const value = Number(count || 0);
  if (value >= 10000) {
    const num = value / 10000;
    return `${num % 1 === 0 ? num.toFixed(0) : num.toFixed(1)}w`;
  }
  if (value >= 1000) {
    const num = value / 1000;
    return `${num % 1 === 0 ? num.toFixed(0) : num.toFixed(1)}k`;
  }
  return `${value}`;
};
