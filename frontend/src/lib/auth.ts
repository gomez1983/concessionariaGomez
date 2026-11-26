import { createContext, useContext } from 'react';
import type { UsuarioLogado } from '../types';
import type { MotivoFimSessao } from './session';

export type EstadoAutenticacao =
  | { status: 'verificando' }
  | { status: 'anonimo'; motivo?: MotivoFimSessao }
  | { status: 'autenticado'; usuario: UsuarioLogado };

export interface ContextoAutenticacao {
  estado: EstadoAutenticacao;
  entrar: (email: string, senha: string) => Promise<void>;
  sair: () => Promise<void>;
}

export const AuthContext = createContext<ContextoAutenticacao | null>(null);

export function useAuth() {
  const contexto = useContext(AuthContext);
  if (!contexto) throw new Error('useAuth deve ser usado dentro de <AuthProvider>.');
  return contexto;
}

/** Usuário logado; só pode ser usado nas telas protegidas (dentro do AuthGate). */
export function useUsuario(): UsuarioLogado {
  const { estado } = useAuth();
  if (estado.status !== 'autenticado') throw new Error('Nenhum usuário autenticado.');
  return estado.usuario;
}

export function iniciais(nome: string) {
  const partes = nome.trim().split(/\s+/).filter(Boolean);
  if (partes.length === 0) return '?';
  const primeira = partes[0][0];
  const ultima = partes.length > 1 ? partes[partes.length - 1][0] : (partes[0][1] ?? '');
  return (primeira + ultima).toUpperCase();
}

export const NOME_PERFIL: Record<UsuarioLogado['perfil'], string> = {
  VENDEDOR: 'Vendedor',
  GERENTE: 'Gerente',
  ADMIN: 'Administrador',
};
