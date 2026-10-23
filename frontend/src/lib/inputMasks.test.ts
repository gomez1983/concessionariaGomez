import { describe, expect, it } from 'vitest';
import { formatDocument, formatPhone, formatCep, parseMoney } from './inputMasks';

describe('formatação brasileira', () => {
  it('formata CPF parcial, completo e CNPJ sem perder dígitos', () => {
    expect(formatDocument('1234')).toBe('123.4');
    expect(formatDocument('12345678901')).toBe('123.456.789-01');
    expect(formatDocument('12345678000199')).toBe('12.345.678/0001-99');
    expect(formatDocument('12.345.678/0001-99')).toBe('12.345.678/0001-99');
    expect(formatDocument('')).toBe('');
  });
  it('suporta telefone fixo, celular, prefixo zero e código do Brasil', () => {
    expect(formatPhone('1132145500')).toBe('(11) 3214-5500');
    expect(formatPhone('11987654321')).toBe('(11) 98765-4321');
    expect(formatPhone('011987654321')).toBe('(11) 98765-4321');
    expect(formatPhone('+55 (11) 98765-4321')).toBe('(11) 98765-4321');
    expect(formatPhone('11')).toBe('(11)');
  });
  it('converte milhões e centavos corretamente para o contrato da API', () => {
    expect(parseMoney('R$ 1.500.000,00')).toBe(1500000);
    expect(parseMoney('R$ 86.990,75')).toBe(86990.75);
    expect(parseMoney('0,01')).toBe(0.01);
    for (const invalid of ['', '-10', '1.50', '1,234', 'abc'])
      expect(parseMoney(invalid)).toBeNaN();
  });
  it('formata CEP sem ultrapassar oito dígitos', () => {
    expect(formatCep('01001000')).toBe('01001-000');
    expect(formatCep('01001-0009')).toBe('01001-000');
  });
});
