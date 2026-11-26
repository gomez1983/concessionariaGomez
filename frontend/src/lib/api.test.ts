import { afterEach, describe, expect, it, vi } from 'vitest';
import { api, ApiError, carroQuery, imagem, request } from './api';
import { emptyFilters } from './estoque';
import {
  entrar,
  getAccessToken,
  onSessao,
  redefinirSessaoParaTestes,
  sair,
  verificarSessaoInicial,
} from './session';

afterEach(() => {
  vi.unstubAllGlobals();
  redefinirSessaoParaTestes();
});

const usuario = { nome: 'Maria Souza', email: 'maria@teste.gomez', perfil: 'VENDEDOR' as const };
function token(valor: string) {
  return new Response(
    JSON.stringify({
      accessToken: valor,
      tokenType: 'Bearer',
      expiraEm: '2026-11-25T12:00:00Z',
      usuario,
    }),
  );
}
function cabecalhos(init?: RequestInit) {
  return new Headers(init?.headers);
}
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
  it('cancela venda pelo endpoint auditado, com motivo codificado na URL', async () => {
    const fetch = vi
      .fn()
      .mockResolvedValue(new Response(JSON.stringify({ id: 5, cancelada: true })));
    vi.stubGlobal('fetch', fetch);
    await api.cancelarVenda(5, 'Desistência & troca');
    const [url, init] = fetch.mock.calls[0];
    const parsed = new URL(url, 'http://localhost');
    expect(init.method).toBe('POST');
    expect(parsed.pathname).toMatch(/\/vendas\/5\/cancelamento-auditado$/);
    expect(parsed.searchParams.get('motivo')).toBe('Desistência & troca');
    expect(parsed.searchParams.get('reativarVeiculo')).toBe('true');
  });
  it('explica falha de rede em português', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
    await expect(request('/carros')).rejects.toThrow('Não foi possível acessar a API');
  });
});

describe('sessão e autenticação', () => {
  it('login guarda o token só em memória e as requisições enviam Bearer, sem os cabeçalhos antigos', async () => {
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(token('token-a'))
      .mockResolvedValueOnce(new Response('{"content":[]}'));
    vi.stubGlobal('fetch', fetch);

    await expect(entrar('maria@teste.gomez', 'segredo-123')).resolves.toEqual(usuario);
    const [urlLogin, initLogin] = fetch.mock.calls[0];
    expect(urlLogin).toMatch(/\/auth\/login$/);
    expect(initLogin.credentials).toBe('include');
    expect(cabecalhos(initLogin).has('Authorization')).toBe(false);

    await request('/carros');
    const headers = cabecalhos(fetch.mock.calls[1][1]);
    expect(headers.get('Authorization')).toBe('Bearer token-a');
    expect(headers.has('X-Operador-Perfil')).toBe(false);
    expect(headers.has('X-Operador-Id')).toBe(false);
  });

  it('login recusado mostra a mensagem genérica da API e não guarda token', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValue(
          new Response(JSON.stringify({ userMessage: 'E-mail ou senha inválidos.' }), {
            status: 401,
          }),
        ),
    );
    await expect(entrar('maria@teste.gomez', 'errada')).rejects.toThrow(
      'E-mail ou senha inválidos.',
    );
    expect(getAccessToken()).toBeNull();
  });

  it('em 401 renova a sessão pelo cookie (com X-Requested-With) e repete a requisição com o token novo', async () => {
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(token('token-a'))
      .mockResolvedValueOnce(new Response('{}', { status: 401 }))
      .mockResolvedValueOnce(token('token-b'))
      .mockResolvedValueOnce(new Response('{"ok":true}'));
    vi.stubGlobal('fetch', fetch);
    await entrar('maria@teste.gomez', 'segredo-123');

    await expect(request('/dashboard')).resolves.toEqual({ ok: true });

    const [urlRefresh, initRefresh] = fetch.mock.calls[2];
    expect(urlRefresh).toMatch(/\/auth\/refresh$/);
    expect(initRefresh.method).toBe('POST');
    expect(initRefresh.credentials).toBe('include');
    expect(cabecalhos(initRefresh).get('X-Requested-With')).toBe('gomez-spa');
    expect(cabecalhos(fetch.mock.calls[3][1]).get('Authorization')).toBe('Bearer token-b');
  });

  it('requisições simultâneas que recebem 401 compartilham uma única renovação', async () => {
    let renovacoes = 0;
    const fetch = vi.fn().mockImplementation((url: string, init: RequestInit) => {
      if (url.endsWith('/auth/refresh')) {
        renovacoes++;
        return new Promise((resolve) => setTimeout(() => resolve(token('token-novo')), 10));
      }
      const autorizado = cabecalhos(init).get('Authorization') === 'Bearer token-novo';
      return Promise.resolve(new Response('{}', { status: autorizado ? 200 : 401 }));
    });
    vi.stubGlobal('fetch', fetch);

    await Promise.all([
      request('/carros'),
      request('/clientes'),
      request('/vendas'),
      request('/dashboard'),
    ]);
    expect(renovacoes).toBe(1);
  });

  it('se a renovação falhar, encerra a sessão como expirada e avisa a tela', async () => {
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(token('token-a'))
      .mockResolvedValueOnce(new Response('{}', { status: 401 }))
      .mockResolvedValueOnce(new Response('{}', { status: 401 }));
    vi.stubGlobal('fetch', fetch);
    await entrar('maria@teste.gomez', 'segredo-123');
    const eventos: unknown[] = [];
    onSessao((u, motivo) => eventos.push([u, motivo]));

    await expect(request('/carros')).rejects.toMatchObject({
      status: 401,
      message: 'Sua sessão expirou. Entre novamente.',
    });
    expect(getAccessToken()).toBeNull();
    expect(eventos).toEqual([[null, 'expirada']]);
    expect(fetch).toHaveBeenCalledTimes(3); // sem nova tentativa depois da renovação recusada
  });

  it('a verificação inicial é única mesmo chamada duas vezes (StrictMode)', async () => {
    const fetch = vi.fn().mockImplementation(() => Promise.resolve(token('token-a')));
    vi.stubGlobal('fetch', fetch);

    const [primeira, segunda] = await Promise.all([
      verificarSessaoInicial(),
      verificarSessaoInicial(),
    ]);
    expect(primeira).toEqual(usuario);
    expect(segunda).toEqual(usuario);
    expect(fetch).toHaveBeenCalledTimes(1);
  });

  it('sem cookie de sessão, a verificação inicial devolve null sem lançar erro', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{}', { status: 401 })));
    await expect(verificarSessaoInicial()).resolves.toBeNull();
  });

  it('logout chama a API com X-Requested-With e limpa o token mesmo se a rede falhar', async () => {
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(token('token-a'))
      .mockRejectedValueOnce(new TypeError('Failed to fetch'));
    vi.stubGlobal('fetch', fetch);
    await entrar('maria@teste.gomez', 'segredo-123');

    await sair();
    const [url, init] = fetch.mock.calls[1];
    expect(url).toMatch(/\/auth\/logout$/);
    expect(cabecalhos(init).get('X-Requested-With')).toBe('gomez-spa');
    expect(getAccessToken()).toBeNull();
  });

  it('foto protegida é baixada com o token; 404 vira "sem foto" (null)', async () => {
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(token('token-a'))
      .mockResolvedValueOnce(new Response(new Blob(['png'], { type: 'image/png' })))
      .mockResolvedValueOnce(new Response('{}', { status: 404 }));
    vi.stubGlobal('fetch', fetch);
    await entrar('maria@teste.gomez', 'segredo-123');

    await expect(imagem('/carros/1/foto?v=0')).resolves.toBeInstanceOf(Blob);
    const headers = cabecalhos(fetch.mock.calls[1][1]);
    expect(headers.get('Authorization')).toBe('Bearer token-a');
    expect(headers.get('Accept')).toMatch(/^image\/\*/);
    await expect(imagem('/carros/2/foto?v=0')).resolves.toBeNull();
  });
});
