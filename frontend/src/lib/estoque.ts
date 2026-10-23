import type { Filters } from '../types';

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
