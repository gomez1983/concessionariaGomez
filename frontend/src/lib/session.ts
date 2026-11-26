import type { TokenAcesso, UsuarioLogado } from '../types';

/**
 * Sessão do painel.
 * - O access token (JWT, 15 min) fica só em memória: some ao recarregar a página.
 * - O refresh token fica num cookie HttpOnly que o JavaScript não lê; /auth/refresh o renova.
 * - /auth/refresh e /auth/logout exigem o cabeçalho X-Requested-With (proteção contra CSRF).
 */
export const API_BASE = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '');

export const MSG_SESSAO_EXPIRADA = 'Sua sessão expirou. Entre novamente.';
const MSG_API_INACESSIVEL =
  'Não foi possível acessar a API. Verifique se o Spring Boot e o banco de dados estão disponíveis.';
const CABECALHO_ANTI_CSRF = { 'X-Requested-With': 'gomez-spa' };

export type MotivoFimSessao = 'logout' | 'expirada';
type Ouvinte = (usuario: UsuarioLogado | null, motivo?: MotivoFimSessao) => void;

let accessToken: string | null = null;
let usuarioAtual: UsuarioLogado | null = null;
let renovacaoEmAndamento: Promise<boolean> | null = null;
let verificacaoInicial: Promise<UsuarioLogado | null> | null = null;
const ouvintes = new Set<Ouvinte>();

export function getAccessToken() {
  return accessToken;
}

export function getUsuario() {
  return usuarioAtual;
}

/** Avisa quando o usuário entra, troca de token ou sai. Devolve a função que cancela a inscrição. */
export function onSessao(ouvinte: Ouvinte) {
  ouvintes.add(ouvinte);
  return () => {
    ouvintes.delete(ouvinte);
  };
}

function aplicar(token: TokenAcesso) {
  accessToken = token.accessToken;
  usuarioAtual = token.usuario;
  ouvintes.forEach((ouvinte) => ouvinte(usuarioAtual));
}

function encerrarLocalmente(motivo: MotivoFimSessao) {
  const estavaLogado = accessToken !== null || usuarioAtual !== null;
  accessToken = null;
  usuarioAtual = null;
  if (estavaLogado || motivo === 'logout') ouvintes.forEach((ouvinte) => ouvinte(null, motivo));
}

export async function entrar(email: string, senha: string): Promise<UsuarioLogado> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ email, senha }),
      signal: AbortSignal.timeout(30000),
    });
  } catch {
    throw new Error(MSG_API_INACESSIVEL);
  }
  if (!response.ok) {
    const problem = await response.json().catch(() => null);
    throw new Error(
      response.status >= 500
        ? 'A API está indisponível. Tente novamente em instantes.'
        : response.status === 400
          ? 'Informe o e-mail e a senha.'
          : problem?.userMessage || 'E-mail ou senha inválidos.',
    );
  }
  aplicar(await response.json());
  return usuarioAtual!;
}

/**
 * Renova o access token pelo cookie. Chamadas simultâneas (várias requisições recebendo 401 juntas)
 * compartilham a mesma renovação: o servidor rotaciona o cookie só uma vez.
 */
export function renovarSessao(): Promise<boolean> {
  renovacaoEmAndamento ??= (async () => {
    try {
      const response = await fetch(`${API_BASE}/auth/refresh`, {
        method: 'POST',
        credentials: 'include',
        headers: { ...CABECALHO_ANTI_CSRF, Accept: 'application/json' },
        signal: AbortSignal.timeout(30000),
      });
      if (!response.ok) return false;
      aplicar(await response.json());
      return true;
    } catch {
      return false;
    } finally {
      renovacaoEmAndamento = null;
    }
  })();
  return renovacaoEmAndamento;
}

/**
 * Verificação feita ao abrir o painel. É uma promessa única do módulo: o StrictMode do React monta os
 * componentes duas vezes em desenvolvimento, e duas renovações em paralelo rotacionariam o cookie à toa.
 */
export function verificarSessaoInicial(): Promise<UsuarioLogado | null> {
  verificacaoInicial ??= renovarSessao().then((ok) => (ok ? usuarioAtual : null));
  return verificacaoInicial;
}

export async function sair() {
  try {
    await fetch(`${API_BASE}/auth/logout`, {
      method: 'POST',
      credentials: 'include',
      headers: CABECALHO_ANTI_CSRF,
      signal: AbortSignal.timeout(10000),
    });
  } catch {
    /* Mesmo sem resposta da API, a sessão local é encerrada; o cookie expira sozinho. */
  }
  encerrarLocalmente('logout');
}

/** Chamado quando a API recusa o token e a renovação também falha. */
export function sessaoExpirada() {
  encerrarLocalmente('expirada');
}

/** Somente para testes: volta o módulo ao estado inicial. */
export function redefinirSessaoParaTestes() {
  accessToken = null;
  usuarioAtual = null;
  renovacaoEmAndamento = null;
  verificacaoInicial = null;
  ouvintes.clear();
}
