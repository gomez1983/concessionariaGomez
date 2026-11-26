import type {
  Carro,
  CarroInput,
  Cliente,
  ClienteInput,
  Dashboard,
  Filters,
  Foto,
  Page,
  PropostaComercial,
  PropostaComercialInput,
  SimulacaoProposta,
  Sort,
  StatusProposta,
  Venda,
  VendaInput,
} from '../types';

import {
  API_BASE,
  getAccessToken,
  MSG_SESSAO_EXPIRADA,
  renovarSessao,
  sessaoExpirada,
} from './session';

export { API_BASE };

export class ApiError extends Error {
  constructor(
    message: string,
    public status = 0,
    public fields: Record<string, string> = {},
  ) {
    super(message);
  }
}
export function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : 'Não foi possível concluir a operação.';
}

async function enviar(path: string, init: RequestInit, token: string | null) {
  const headers = new Headers(init.headers);
  if (!headers.has('Accept')) headers.set('Accept', 'application/json');
  if (token) headers.set('Authorization', `Bearer ${token}`);
  if (init.body && !(init.body instanceof FormData))
    headers.set('Content-Type', 'application/json');
  try {
    return await fetch(`${API_BASE}${path}`, {
      ...init,
      headers,
      signal: init.signal
        ? AbortSignal.any([init.signal, AbortSignal.timeout(30000)])
        : AbortSignal.timeout(30000),
    });
  } catch (error) {
    if (init.signal?.aborted) throw error;
    throw new ApiError(
      'Não foi possível acessar a API. Verifique se o Spring Boot e o banco de dados estão disponíveis.',
    );
  }
}

/**
 * Envia com o access token. Em 401, renova a sessão uma única vez (renovação compartilhada entre
 * requisições simultâneas) e repete a requisição; se a renovação falhar, encerra a sessão local.
 */
export async function fetchAutenticado(path: string, init: RequestInit = {}): Promise<Response> {
  const tokenUsado = getAccessToken();
  const response = await enviar(path, init, tokenUsado);
  if (response.status !== 401) return response;

  const tokenAtual = getAccessToken();
  // Outra requisição já renovou enquanto esta estava em voo: basta repetir com o token novo
  const renovado = tokenAtual !== null && tokenAtual !== tokenUsado ? true : await renovarSessao();
  if (!renovado) {
    sessaoExpirada();
    throw new ApiError(MSG_SESSAO_EXPIRADA, 401);
  }
  return enviar(path, init, getAccessToken());
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetchAutenticado(path, init);
  if (!response.ok) {
    const problem = await response.json().catch(() => null);
    const fields = Object.fromEntries(
      (problem?.objects ?? [])
        .filter((f: { name?: string }) => f.name)
        .map((f: { name: string; userMessage: string }) => [
          f.name,
          userMessageOrDefault(f.userMessage),
        ]),
    );
    throw new ApiError(
      problem?.userMessage ||
        problem?.detail ||
        (response.status === 413
          ? 'A imagem excede o limite de 10 MB.'
          : response.status >= 500
            ? 'A API está indisponível. Verifique o servidor e tente novamente.'
            : `Não foi possível concluir a operação (${response.status}).`),
      response.status,
      fields,
    );
  }
  if (response.status === 204) return undefined as T;
  return response.json();
}

/**
 * Baixa uma imagem protegida (o <img src> não envia o cabeçalho Authorization).
 * Devolve null quando o veículo não tem a foto (404), para a tela mostrar a imagem de referência.
 */
export async function imagem(path: string, signal?: AbortSignal): Promise<Blob | null> {
  // image/* primeiro escolhe o conteúdo binário; */* deixa o erro chegar como Problem JSON
  const response = await fetchAutenticado(path, {
    signal,
    headers: { Accept: 'image/*,*/*;q=0.1' },
  });
  if (response.status === 404) return null;
  if (!response.ok)
    throw new ApiError(`Não foi possível carregar a imagem (${response.status}).`, response.status);
  return response.blob();
}

function userMessageOrDefault(msg?: string) {
  return msg || 'Campo inválido.';
}

export function carroQuery(filters: Filters, page: number, sort: Sort) {
  const query = new URLSearchParams({
    page: String(page),
    size: '20',
    sort: {
      recentes: 'id,desc',
      'valor-asc': 'compra,asc',
      'valor-desc': 'compra,desc',
      'ano-desc': 'ano,desc',
    }[sort],
  });
  const mapped = {
    marca: filters.marca,
    modelo: filters.search,
    ativo: filters.ativo,
    ano: filters.ano,
    minValor: filters.minValor,
    maxValor: filters.maxValor,
    minQuilometragem: filters.minKm,
    maxQuilometragem: filters.maxKm,
  };
  Object.entries(mapped).forEach(([key, value]) => {
    if (value.trim() !== '') query.set(key, value.trim());
  });
  return query.toString();
}
const json = (body: unknown) => JSON.stringify(body);
export const api = {
  carros: (query: string, signal?: AbortSignal) =>
    request<Page<Carro>>(`/carros?${query}`, { signal }),
  dashboard: (signal?: AbortSignal) => request<Dashboard>('/dashboard', { signal }),
  saveCarro: (input: CarroInput, id?: number) =>
    request<Carro>(`/carros${id ? `/${id}` : ''}`, {
      method: id ? 'PUT' : 'POST',
      body: json(input),
    }),
  toggle: (collection: 'carros' | 'clientes', id: number, ativo: boolean) =>
    request<void>(`/${collection}/${id}/ativo`, { method: ativo ? 'DELETE' : 'PUT' }),
  remove: (collection: 'carros' | 'clientes', id: number) =>
    request<void>(`/${collection}/${id}`, { method: 'DELETE' }),
  /** Cancelamento auditado: preserva a venda com motivo, responsável e data (substitui o DELETE legado). */
  cancelarVenda: (id: number, motivo: string, reativarVeiculo = true) =>
    request<Venda>(
      `/vendas/${id}/cancelamento-auditado?${new URLSearchParams({
        motivo,
        reativarVeiculo: String(reativarVeiculo),
      })}`,
      { method: 'POST' },
    ),
  clientes: (page: number, signal?: AbortSignal) =>
    request<Page<Cliente>>(`/clientes?page=${page}&size=10&sort=id,desc`, { signal }),
  saveCliente: (input: ClienteInput, id?: number) =>
    request<Cliente>(`/clientes${id ? `/${id}` : ''}`, {
      method: id ? 'PUT' : 'POST',
      body: json(input),
    }),
  vendas: (page: number, signal?: AbortSignal) =>
    request<Page<Venda>>(`/vendas?page=${page}&size=10&sort=id,desc`, { signal }),
  saveVenda: (input: VendaInput, id?: number) =>
    request<Venda>(`/vendas${id ? `/${id}` : ''}`, {
      method: id ? 'PUT' : 'POST',
      body: json(input),
    }),
  propostas: (page: number, status?: StatusProposta | '', signal?: AbortSignal) =>
    request<Page<PropostaComercial>>(
      `/propostas?page=${page}&size=10&sort=id,desc${status ? `&status=${status}` : ''}`,
      { signal },
    ),
  propostasPorCliente: (clienteId: number, signal?: AbortSignal) =>
    request<Page<PropostaComercial>>(
      `/propostas?page=0&size=50&sort=id,desc&clienteId=${clienteId}`,
      { signal },
    ),
  propostasPorCarro: (carroId: number, signal?: AbortSignal) =>
    request<Page<PropostaComercial>>(`/propostas?page=0&size=50&sort=id,desc&carroId=${carroId}`, {
      signal,
    }),
  simularProposta: (input: PropostaComercialInput, signal?: AbortSignal) =>
    request<SimulacaoProposta>('/propostas/simulacoes', {
      method: 'POST',
      body: json(input),
      signal,
    }),
  criarProposta: (input: PropostaComercialInput) =>
    request<PropostaComercial>('/propostas', {
      method: 'POST',
      body: json(input),
    }),
  criarRevisaoProposta: (propostaId: number, input: PropostaComercialInput) =>
    request<PropostaComercial>(`/propostas/${propostaId}/revisoes`, {
      method: 'POST',
      body: json(input),
    }),
  decidirAprovacaoProposta: (
    propostaId: number,
    sequenciaRevisao: number,
    aprovar: boolean,
    motivo: string,
  ) =>
    request<PropostaComercial>(`/propostas/${propostaId}/aprovacoes`, {
      method: 'POST',
      body: json({ sequenciaRevisao, aprovar, motivo }),
    }),
  converterPropostaEmVenda: (propostaId: number, sequenciaRevisao: number) =>
    request<Venda>(
      `/propostas/${propostaId}/converter-venda?sequenciaRevisao=${sequenciaRevisao}`,
      { method: 'POST' },
    ),
  async fotos(carroId: number, signal?: AbortSignal): Promise<Foto[]> {
    const [principal, galeria] = await Promise.all([
      request<Foto>(`/carros/${carroId}/foto`, { signal }).catch((error) => {
        if (error instanceof ApiError && error.status === 404) return null;
        throw error;
      }),
      request<Foto[]>(`/carros/${carroId}/fotos`, { signal }),
    ]);
    return [...(principal ? [{ ...principal, principal: true }] : []), ...galeria];
  },
  upload(carroId: number, file: File, descricao: string, foto?: Foto) {
    const form = new FormData();
    form.append('arquivo', file);
    form.append('descricao', descricao);
    return request<Foto>(
      `/carros/${carroId}/${foto?.principal ? 'foto' : `fotos${foto?.id ? `/${foto.id}` : ''}`}`,
      { method: foto?.principal || foto?.id ? 'PUT' : 'POST', body: form },
    );
  },
  deleteFoto: (carroId: number, foto: Foto) =>
    request<void>(`/carros/${carroId}/${foto.principal ? 'foto' : `fotos/${foto.id}`}`, {
      method: 'DELETE',
    }),
  /** Troca a própria senha. O cookie da sessão vai junto (mesma origem) e mantém este navegador logado. */
  trocarSenha: (senhaAtual: string, novaSenha: string) =>
    request<void>('/auth/senha', {
      method: 'PUT',
      // O cookie de sessão identifica qual sessão manter; explícito como em session.ts
      credentials: 'include',
      body: json({ senhaAtual, novaSenha }),
    }),
};
/** Caminho do arquivo da foto na API; é baixado com autenticação por useImagemAutenticada. */
export function photoPath(carroId: number, foto?: Foto, revision = 0) {
  return `/carros/${carroId}/${foto?.id ? `fotos/${foto.id}/conteudo` : 'foto'}?v=${revision}`;
}
export async function allPages<T>(
  collection: 'carros' | 'clientes' | 'vendas',
  signal?: AbortSignal,
): Promise<T[]> {
  let page = 0;
  const result: T[] = [];
  while (true) {
    const response = await request<Page<T>>(
      `/${collection}?page=${page}&size=100&sort=id,asc${collection === 'carros' ? '&ativo=true' : ''}`,
      { signal },
    );
    result.push(...response.content);
    if (++page >= response.totalPages) return result;
  }
}
