import { describe, expect, it } from 'vitest';
import { seed } from '../data/seed';
import { emptyFilters, filterCarros, parseStoredCarros } from './estoque';

describe('busca e filtros de estoque', () => {
  it('combina busca, marca, status, ano, preço e km com limites inclusivos', () => {
    const result = filterCarros(
      seed,
      {
        ...emptyFilters,
        search: '  JÉTTA ',
        marca: 'Volkswagen',
        ativo: 'true',
        ano: '2019',
        minValor: '112900',
        maxValor: '112900',
        minKm: '35000',
        maxKm: '35000',
      },
      'recentes',
    );
    expect(result.map((c) => c.id)).toEqual([11]);
  });
  it('distingue filtro vazio de zero km e retorna vazio para faixa invertida', () => {
    expect(
      filterCarros(seed, { ...emptyFilters, maxKm: '0' }, 'recentes').map((c) => c.id),
    ).toEqual([13]);
    expect(
      filterCarros(seed, { ...emptyFilters, minValor: '100', maxValor: '50' }, 'recentes'),
    ).toEqual([]);
  });
  it('filtra inativos e ordena sem modificar a coleção original', () => {
    const copy = [...seed];
    expect(
      filterCarros(seed, { ...emptyFilters, ativo: 'false' }, 'valor-asc').map((c) => c.id),
    ).toEqual([12, 8]);
    expect(filterCarros(seed, emptyFilters, 'ano-desc')[0].ano).toBe(2021);
    expect(seed).toEqual(copy);
  });
});
describe('dados persistidos no navegador', () => {
  it('preserva um estoque vazio e os dados válidos', () => {
    expect(parseStoredCarros('[]')).toEqual([]);
    expect(parseStoredCarros(JSON.stringify(seed))).toEqual(seed);
  });
  it('descarta JSON inválido, estruturas incompletas e IDs repetidos', () => {
    for (const raw of [
      '{',
      '{}',
      '[null]',
      '[{}]',
      JSON.stringify([seed[0], seed[0]]),
      JSON.stringify([{ ...seed[0], quilometragem: -1 }]),
    ]) {
      expect(parseStoredCarros(raw)).toBeNull();
    }
  });
});
