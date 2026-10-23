import type { Carro, Filters, Sort } from '../types';

export const emptyFilters: Filters = {
  search: '',
  marca: '',
  ativo: '',
  ano: '',
  minValor: '',
  maxValor: '',
  minKm: '',
  maxKm: '',
};
export const currency = (value: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
export const number = (value: number) => new Intl.NumberFormat('pt-BR').format(value);
const normalize = (value: string) =>
  value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim();

export function filterCarros(carros: Carro[], filters: Filters, sort: Sort): Carro[] {
  const search = normalize(filters.search);
  return carros
    .filter(
      (c) =>
        (!search || normalize(`${c.marca} ${c.modelo} ${c.ano}`).includes(search)) &&
        (!filters.marca || c.marca === filters.marca) &&
        (!filters.ativo || c.ativo === (filters.ativo === 'true')) &&
        (!filters.ano || c.ano === Number(filters.ano)) &&
        (!filters.minValor || c.compra >= Number(filters.minValor)) &&
        (!filters.maxValor || c.compra <= Number(filters.maxValor)) &&
        (!filters.minKm || c.quilometragem >= Number(filters.minKm)) &&
        (!filters.maxKm || c.quilometragem <= Number(filters.maxKm)),
    )
    .sort((a, b) => {
      if (sort === 'valor-asc') return a.compra - b.compra || b.id - a.id;
      if (sort === 'valor-desc') return b.compra - a.compra || b.id - a.id;
      if (sort === 'ano-desc') return b.ano - a.ano || b.id - a.id;
      return b.dataCompra.localeCompare(a.dataCompra) || b.id - a.id;
    });
}

export function isCarro(value: unknown): value is Carro {
  if (!value || typeof value !== 'object') return false;
  const c = value as Record<string, unknown>;
  return (
    typeof c.id === 'number' &&
    Number.isSafeInteger(c.id) &&
    c.id > 0 &&
    typeof c.marca === 'string' &&
    c.marca.trim().length > 0 &&
    typeof c.modelo === 'string' &&
    c.modelo.trim().length > 0 &&
    typeof c.ano === 'number' &&
    Number.isInteger(c.ano) &&
    typeof c.compra === 'number' &&
    Number.isFinite(c.compra) &&
    c.compra >= 0 &&
    typeof c.quilometragem === 'number' &&
    Number.isSafeInteger(c.quilometragem) &&
    c.quilometragem >= 0 &&
    typeof c.ativo === 'boolean' &&
    typeof c.dataCompra === 'string' &&
    Number.isFinite(Date.parse(c.dataCompra))
  );
}

export function parseStoredCarros(raw: string | null): Carro[] | null {
  if (!raw) return null;
  try {
    const parsed: unknown = JSON.parse(raw);
    if (!Array.isArray(parsed) || !parsed.every(isCarro)) return null;
    if (new Set(parsed.map((c) => c.id)).size !== parsed.length) return null;
    return parsed;
  } catch {
    return null;
  }
}
