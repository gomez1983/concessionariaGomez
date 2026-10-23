import { digits } from './inputMasks';

export interface CepAddress {
  cep: string;
  logradouro: string;
  bairro: string;
  cidade: string;
  estado: string;
}
export async function lookupCep(value: string, signal?: AbortSignal): Promise<CepAddress> {
  const cep = digits(value);
  if (!/^\d{8}$/.test(cep)) throw new Error('Informe os oito dígitos do CEP.');
  try {
    const response = await fetch(`https://viacep.com.br/ws/${cep}/json/`, {
      signal: signal
        ? AbortSignal.any([signal, AbortSignal.timeout(10000)])
        : AbortSignal.timeout(10000),
    });
    if (!response.ok) throw new Error('Consulta indisponível');
    const data = await response.json();
    if (data.erro)
      throw new Error('CEP não encontrado. Confira o número ou preencha o endereço manualmente.');
    if (typeof data.localidade !== 'string' || typeof data.uf !== 'string')
      throw new Error('Resposta inválida');
    return {
      cep,
      logradouro: data.logradouro || '',
      bairro: data.bairro || '',
      cidade: data.localidade,
      estado: data.uf,
    };
  } catch (error) {
    if (signal?.aborted) throw error;
    if (error instanceof Error && error.message.startsWith('CEP não encontrado')) throw error;
    throw new Error(
      'Não foi possível consultar o CEP agora. Você pode preencher o endereço manualmente.',
    );
  }
}
