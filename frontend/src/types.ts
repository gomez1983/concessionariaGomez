export interface Carro {
  id: number;
  marca: string;
  modelo: string;
  ano: number;
  compra: number;
  venda: number;
  zeroKm: boolean;
  quilometragem: number;
  ativo: boolean;
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

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
export interface Endereco {
  cep: string;
  logradouro: string;
  numero: string;
  complemento: string;
  bairro: string;
  cidade: string;
  estado: string;
}
export interface Cliente {
  id: number;
  nome: string;
  cpfCnpj: string;
  telefone: string;
  email: string;
  ativo: boolean;
  endereco: Endereco | null;
  dataCadastro: string;
}
export type ClienteInput = Pick<Cliente, 'nome' | 'cpfCnpj' | 'telefone' | 'email' | 'endereco'>;
export interface Venda {
  id: number;
  carro: Pick<Carro, 'id' | 'marca' | 'modelo' | 'ano'>;
  cliente: Pick<Cliente, 'id' | 'nome' | 'cpfCnpj'>;
  valorVenda: number;
  dataVenda: string;
  observacoes: string;
}
export interface VendaInput {
  carroId: number;
  clienteId: number;
  valorVenda: number;
  observacoes: string;
}
export interface Dashboard {
  totalVeiculos: number;
  ativos: number;
  zeroKm: number;
  valorInvestido: number;
  vendaSugerida: number;
  margemPrevista?: number;
  marcas: Record<string, number>;
}
export interface Foto {
  id?: number;
  descricao: string;
  contentType: string;
  tamanho: number;
  principal?: boolean;
}
