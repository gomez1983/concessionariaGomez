export interface Carro {
  id: number;
  marca: string;
  modelo: string;
  ano: number;
  compra: number; // Somente demonstração. Cálculos oficiais pertencem à API Java.
  quilometragem: number;
  ativo: boolean;
  dataCompra: string;
}
export type CarroInput = Pick<Carro, 'marca' | 'modelo' | 'ano' | 'compra' | 'quilometragem'>;
export interface Filters {
  search: string;
  marca: string;
  ativo: string;
  ano: string;
  minValor: string;
  maxValor: string;
  minKm: string;
  maxKm: string;
}
export type Sort = 'recentes' | 'valor-asc' | 'valor-desc' | 'ano-desc';
