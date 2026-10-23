import { afterEach, describe, expect, it, vi } from 'vitest';
import { api, ApiError, carroQuery, request } from './api';
import { emptyFilters } from './estoque';

afterEach(() => vi.unstubAllGlobals());
describe('contrato da API', () => {
  it('envia filtros, zeros e paginação com os nomes do backend', () => {
    const query = new URLSearchParams(
      carroQuery(
        { ...emptyFilters, search: 'Classe A', maxKm: '0', minValor: '0', ativo: 'false' },
        2,
        'valor-desc',
      ),
    );
    expect(Object.fromEntries(query)).toEqual({
      page: '2',
      size: '20',
      sort: 'compra,desc',
      modelo: 'Classe A',
      ativo: 'false',
      minValor: '0',
      maxQuilometragem: '0',
    });
    expect(query.has('search')).toBe(false);
  });
  it('preserva mensagens e campos do Problem Details', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            userMessage: 'Confira os dados.',
            objects: [{ name: 'nome', userMessage: 'O nome é obrigatório' }],
          }),
          { status: 400 },
        ),
      ),
    );
    await expect(request('/clientes')).rejects.toMatchObject({
      status: 400,
      fields: { nome: 'O nome é obrigatório' },
      message: 'Confira os dados.',
    });
  });
  it('não define Content-Type manualmente no multipart e aceita 204', async () => {
    const fetch = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal('fetch', fetch);
    const body = new FormData();
    body.append('descricao', 'Interior');
    await expect(request('/carros/1/fotos', { method: 'POST', body })).resolves.toBeUndefined();
    expect(new Headers(fetch.mock.calls[0][1].headers).has('Content-Type')).toBe(false);
  });
  it('trata ausência da foto principal sem esconder falhas da galeria', async () => {
    const fetch = vi
      .fn()
      .mockImplementation((url: string) =>
        Promise.resolve(
          url.includes('/fotos')
            ? new Response(JSON.stringify([{ id: 7, descricao: 'Interior' }]))
            : new Response('{}', { status: 404 }),
        ),
      );
    vi.stubGlobal('fetch', fetch);
    await expect(api.fotos(1)).resolves.toEqual([{ id: 7, descricao: 'Interior' }]);
    fetch.mockResolvedValue(new Response('{}', { status: 500 }));
    await expect(api.fotos(1)).rejects.toBeInstanceOf(ApiError);
  });
  it('explica falha de rede em português', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
    await expect(request('/carros')).rejects.toThrow('Não foi possível acessar a API');
  });
});
