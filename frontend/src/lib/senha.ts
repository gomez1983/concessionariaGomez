/** Política de senha do painel, igual à do backend (PoliticaSenha). */
export const SENHA_MINIMO = 10;
export const SENHA_MAXIMO = 72;

/** O BCrypt aceita até 72 bytes: letras acentuadas ocupam 2 bytes em UTF-8. */
function bytesUtf8(texto: string) {
  return new TextEncoder().encode(texto).length;
}

/** Devolve a mensagem do primeiro problema encontrado, ou string vazia se a troca pode ser enviada. */
export function validarNovaSenha(atual: string, nova: string, confirmacao: string) {
  if (nova.length < SENHA_MINIMO)
    return `A nova senha deve ter pelo menos ${SENHA_MINIMO} caracteres.`;
  if (nova.length > SENHA_MAXIMO || bytesUtf8(nova) > SENHA_MAXIMO)
    return 'A nova senha é longa demais (máximo de 72 caracteres; acentos contam em dobro).';
  if (!nova.trim()) return 'A nova senha não pode ser só espaços.';
  if (nova === atual) return 'A nova senha deve ser diferente da atual.';
  if (nova !== confirmacao) return 'A confirmação não confere com a nova senha.';
  return '';
}
