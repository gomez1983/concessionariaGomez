import type { PerfilOperador, UsuarioLogado } from '../types';

/**
 * O que cada perfil vê na tela (seção 8.5 do design da Etapa 3). O backend continua sendo a fonte da
 * verdade e responde 403; aqui apenas deixamos de oferecer ações que o perfil não pode concluir.
 */

/** GERENTE e ADMIN: cadastro e edição de veículos, ativação/inativação, vendas diretas, aprovações. */
export function ehGestao(perfil: PerfilOperador) {
  return perfil === 'GERENTE' || perfil === 'ADMIN';
}

/** Somente ADMIN: exclusões definitivas. */
export function ehAdmin(perfil: PerfilOperador) {
  return perfil === 'ADMIN';
}

/** Revisar, reservar e converter: o vendedor dono da proposta, ou GERENTE/ADMIN. */
export function podeConduzirProposta(
  usuario: UsuarioLogado,
  vendedorIdentificador?: string | null,
) {
  if (ehGestao(usuario.perfil)) return true;
  return (
    !!vendedorIdentificador && vendedorIdentificador.toLowerCase() === usuario.email.toLowerCase()
  );
}
