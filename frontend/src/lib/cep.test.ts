import { afterEach, describe, expect, it, vi } from 'vitest';
import { lookupCep } from './cep';

afterEach(() => vi.unstubAllGlobals());
describe('consulta de CEP', () => {
  it('envia somente o CEP e converte os campos do ViaCEP', async () => {
    const request = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        logradouro: 'Praça da Sé',
        bairro: 'Sé',
        localidade: 'São Paulo',
        uf: 'SP',
      }),
    });
    vi.stubGlobal('fetch', request);
    await expect(lookupCep('01001-000')).resolves.toEqual({
      cep: '01001000',
      logradouro: 'Praça da Sé',
      bairro: 'Sé',
      cidade: 'São Paulo',
      estado: 'SP',
    });
    expect(request).toHaveBeenCalledWith(
      'https://viacep.com.br/ws/01001000/json/',
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    );
  });
  it('não envia CEP incompleto ao serviço externo', async () => {
    const request = vi.fn();
    vi.stubGlobal('fetch', request);
    await expect(lookupCep('01001')).rejects.toThrow('oito dígitos');
    expect(request).not.toHaveBeenCalled();
  });
  it('orienta preenchimento manual em CEP inexistente ou falha de rede', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({ ok: true, json: async () => ({ erro: true }) }),
    );
    await expect(lookupCep('00000000')).rejects.toThrow('CEP não encontrado');
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('offline')));
    await expect(lookupCep('01001000')).rejects.toThrow('manualmente');
  });
  it('aceita CEP geral que não informa rua e bairro', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValue({ ok: true, json: async () => ({ localidade: 'Cidade', uf: 'SP' }) }),
    );
    await expect(lookupCep('01001000')).resolves.toMatchObject({
      logradouro: '',
      bairro: '',
      cidade: 'Cidade',
    });
  });
});
