import type {
  Carro,
  CarroInput,
  Cliente,
  ClienteInput,
  Dashboard,
  Filters,
  Foto,
  Page,
  Sort,
  Venda,
  VendaInput,
} from '../types';

export const API_BASE = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '');
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

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');
  if (init.body && !(init.body instanceof FormData))
    headers.set('Content-Type', 'application/json');
  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
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
  if (!response.ok) {
    const problem = await response.json().catch(() => null);
    const fields = Object.fromEntries(
      (problem?.objects ?? [])
        .filter((f: { name?: string }) => f.name)
        .map((f: { name: string; userMessage: string }) => [f.name, f.userMessage]),
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
  remove: (collection: 'carros' | 'clientes' | 'vendas', id: number) =>
    request<void>(`/${collection}/${id}`, { method: 'DELETE' }),
  clientes: (page: number, signal?: AbortSignal) =>
    request<Page<Cliente>>(`/clientes?page=${page}&size=10&sort=id,desc`, { signal }),
  saveCliente: (input: ClienteInput, id?: number) =>
    request<Cliente>(`/clientes${id ? `/${id}` : ''}`, {
      method: id ? 'PUT' : 'POST',
      body: json(input),
    }),
  vendas: (page: number, signal?: AbortSignal) =>
    request<Page<Venda>>(`/vendas?page=${page}&size=10&sort=id,desc`, { signal }),
  saveVenda: (input: VendaInput) =>
    request<Venda>('/vendas', { method: 'POST', body: json(input) }),
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
};
export function photoUrl(carroId: number, foto?: Foto, revision = 0) {
  return `${API_BASE}/carros/${carroId}/${foto?.id ? `fotos/${foto.id}/conteudo` : 'foto'}?v=${revision}`;
}
export async function allPages<T>(
  collection: 'carros' | 'clientes',
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
