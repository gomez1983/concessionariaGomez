import { describe, expect, it } from 'vitest';
import { ehAdmin, ehGestao, podeConduzirProposta } from './permissoes';
import { validarNovaSenha } from './senha';
import type { UsuarioLogado } from '../types';

const vendedora: UsuarioLogado = { nome: 'Maria', email: 'maria@gomez.com', perfil: 'VENDEDOR' };
const gerente: UsuarioLogado = { nome: 'Gil', email: 'gil@gomez.com', perfil: 'GERENTE' };

describe('permissões por perfil (seção 8.5 do design)', () => {
  it('gestão é GERENTE e ADMIN; exclusão definitiva só ADMIN', () => {
    expect(ehGestao('VENDEDOR')).toBe(false);
    expect(ehGestao('GERENTE')).toBe(true);
    expect(ehGestao('ADMIN')).toBe(true);
    expect(ehAdmin('GERENTE')).toBe(false);
    expect(ehAdmin('ADMIN')).toBe(true);
  });
  it('vendedor só conduz a própria proposta (sem diferenciar maiúsculas); gestão conduz qualquer uma', () => {
    expect(podeConduzirProposta(vendedora, 'MARIA@gomez.com')).toBe(true);
    expect(podeConduzirProposta(vendedora, 'joao@gomez.com')).toBe(false);
    expect(podeConduzirProposta(vendedora, null)).toBe(false);
    expect(podeConduzirProposta(gerente, 'joao@gomez.com')).toBe(true);
  });
});

describe('validação da nova senha (mesma política do backend)', () => {
  it('exige de 10 a 72 caracteres e no máximo 72 bytes', () => {
    expect(validarNovaSenha('atual-123456', 'curta', 'curta')).toMatch(/pelo menos 10/);
    expect(validarNovaSenha('atual-123456', 'ç'.repeat(37), 'ç'.repeat(37))).toMatch(
      /longa demais/,
    );
    expect(validarNovaSenha('atual-123456', 'ç'.repeat(36), 'ç'.repeat(36))).toBe('');
  });
  it('recusa espaços, senha igual à atual e confirmação diferente', () => {
    expect(validarNovaSenha('atual-123456', ' '.repeat(12), ' '.repeat(12))).toMatch(/espaços/);
    expect(validarNovaSenha('atual-123456', 'atual-123456', 'atual-123456')).toMatch(/diferente/);
    expect(validarNovaSenha('atual-123456', 'nova-senha-123', 'nova-senha-124')).toMatch(
      /confirmação/,
    );
    expect(validarNovaSenha('atual-123456', 'nova-senha-123', 'nova-senha-123')).toBe('');
  });
});
