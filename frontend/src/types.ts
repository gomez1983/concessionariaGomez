export type PerfilOperador = 'VENDEDOR' | 'GERENTE' | 'ADMIN';

export interface Carro {
  id: number;
  marca: string;
  modelo: string;
  ano: number;
  compra?: number;
  venda: number;
  precoAnunciado?: number;
  zeroKm: boolean;
  quilometragem: number;
  ativo: boolean;
  disponibilidadeComercial?: 'DISPONIVEL' | 'RESERVADO' | 'VENDIDO';
  reservaValidaAte?: string;
  reservaResponsavel?: string;
  dataCompra?: string;
  diasEmEstoque?: number;
}
export type CarroInput = Pick<Carro, 'marca' | 'modelo' | 'ano' | 'quilometragem'> & {
  compra: number;
};
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
  origemLegada?: boolean;
  cancelada?: boolean;
  motivoCancelamento?: string;
}
export interface VendaInput {
  carroId: number;
  clienteId: number;
  valorVenda: number;
  observacoes: string;
  propostaId?: number;
  sequenciaRevisaoAutorizada?: number;
}
export interface Dashboard {
  totalVeiculos: number;
  ativos: number;
  zeroKm: number;
  valorInvestido?: number;
  vendaSugerida: number;
  margemPrevista?: number;
  marcas: Record<string, number>;
  propostasEmNegociacao?: number;
  propostasPendentesAprovacao?: number;
  propostasAprovadas?: number;
  propostasConvertidas?: number;
  faturamentoRealizado?: number;
  contribuicaoRealizada?: number;
}
export interface Foto {
  id?: number;
  descricao: string;
  contentType: string;
  tamanho: number;
  principal?: boolean;
}

export type StatusProposta =
  | 'RASCUNHO'
  | 'EM_NEGOCIACAO'
  | 'PENDENTE_APROVACAO'
  | 'APROVADA'
  | 'REJEITADA'
  | 'EXPIRADA'
  | 'CANCELADA'
  | 'CONVERTIDA';

export type AlcadaAnalise =
  | 'DENTRO_AUTONOMIA'
  | 'EXIGE_APROVACAO_GERENCIAL'
  | 'BLOQUEADA_ABAIXO_PISO'
  | 'PENDENTE_DADOS_OBRIGATORIOS'
  | 'CENARIO_MATEMATICO_INVIAVEL';

export interface SimulacaoProposta {
  precoAnunciado: number;
  precoNegociado: number;
  descontoReais: number;
  descontoPercentual: number;
  alcada: AlcadaAnalise;
  possuiCustosPendentes: boolean;
  motivosAlcada: string[];
  pisoAutonomia: number;
  custoAquisicao?: number;
  custosFixosOperacao?: number;
  somaTaxasProporcionais?: number;
  receitasAdicionaisElegiveis?: number;
  contribuicaoEstimada?: number;
  margemContribuicaoEstimada?: number;
  markupAquisicao?: number;
  pisoDesejado?: number;
  pisoAbsoluto?: number;
}

export interface PropostaComercialInput {
  carroId: number;
  clienteId: number;
  precoNegociado: number;
  justificativaVendedor?: string;
  submeter?: boolean;
  avaliacaoTroca?: {
    identificacaoVeiculoUsado: string;
    valorAvaliacaoTecnica: number;
    valorComercialAceito: number;
    saldoQuitacaoVerificado: number;
    possuiPendenciaDocumental?: boolean;
    descricaoPendencias?: string;
  };
  financiamento?: {
    instituicaoFinanceira: string;
    principalFinanciado: number;
    entradaMonetariaRelacionada?: number;
    prazoMeses: number;
    taxaMensal?: number;
    cetAnualInformado?: number;
    valorParcela?: number;
    apenasEstimativa?: boolean;
  };
}

export interface PropostaComercial {
  id: number;
  numero: string;
  carro: Pick<Carro, 'id' | 'marca' | 'modelo' | 'ano'>;
  cliente: Pick<Cliente, 'id' | 'nome' | 'cpfCnpj'>;
  vendedorIdentificador: string;
  status: StatusProposta;
  revisaoAtualSequencia: number;
  validaAte: string;
  dataCriacao: string;
  revisaoAtual?: {
    id: number;
    sequencia: number;
    precoAnunciado: number;
    precoSugerido: number;
    precoNegociado: number;
    descontoReais: number;
    descontoPercentual: number;
    alcadaAnalise: AlcadaAnalise;
    possuiCustosPendentes: boolean;
    submetida: boolean;
    justificativaVendedor?: string;
    pisoAutonomia?: number;
    custoAquisicao?: number;
    custosFixosOperacao?: number;
    contribuicaoEstimada?: number;
    margemContribuicaoEstimada?: number;
    pisoAbsoluto?: number;
    avaliacaoTroca?: {
      identificacaoVeiculoUsado: string;
      valorAvaliacaoTecnica?: number;
      valorComercialAceito: number;
      saldoQuitacaoVerificado: number;
      creditoLiquido: number;
    };
    financiamento?: {
      instituicaoFinanceira: string;
      principalFinanciado: number;
      prazoMeses: number;
      cetAnualInformado?: number;
      valorParcela?: number;
    };
  };
  resumoParaCliente?: {
    numeroProposta: string;
    veiculo: string;
    cliente: string;
    precoAnunciado: number;
    descontoConcedido: number;
    precoFinalProposto: number;
    creditoLiquidoUsadoNaTroca?: number;
    valorFinanciado?: number;
    prazoFinanciamentoMeses?: number;
    validaAte: string;
  };
}
