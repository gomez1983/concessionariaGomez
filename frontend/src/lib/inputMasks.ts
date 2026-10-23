export const digits = (value: string) => value.replace(/\D/g, '');

export function formatDocument(value: string) {
  const raw = digits(value).slice(0, 14);
  const pattern = raw.length <= 11 ? '###.###.###-##' : '##.###.###/####-##';
  let index = 0,
    formatted = '';
  for (const char of pattern) {
    if (index >= raw.length) break;
    formatted += char === '#' ? raw[index++] : char;
  }
  return formatted;
}

export function formatPhone(value: string) {
  let raw = digits(value);
  if (raw.length > 11 && raw.startsWith('55')) raw = raw.slice(2);
  if (raw.startsWith('0')) raw = raw.slice(1);
  raw = raw.slice(0, 11);
  if (!raw) return '';
  if (raw.length < 2) return `(${raw}`;
  const subscriber = raw.slice(2);
  const split = subscriber.length > 8 ? 5 : 4;
  return `(${raw.slice(0, 2)})${subscriber ? ` ${subscriber.slice(0, split)}${subscriber.length > split ? `-${subscriber.slice(split)}` : ''}` : ''}`;
}

export const formatCep = (value: string) =>
  digits(value)
    .slice(0, 8)
    .replace(/^(\d{5})(\d)/, '$1-$2');

export function parseMoney(value: string) {
  const clean = value.replace(/R\$|\s/g, '');
  if (!/^(?:\d{1,3}(?:\.\d{3})+|\d+)(?:,\d{1,2})?$/.test(clean)) return NaN;
  return Number(clean.replace(/\./g, '').replace(',', '.'));
}
