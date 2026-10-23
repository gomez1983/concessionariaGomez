import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { LoaderCircle, MapPin, Search, Users } from 'lucide-react';
import type { Cliente, ClienteInput, Endereco } from '../types';
import { lookupCep } from '../lib/cep';
import { digits, formatCep, formatDocument, formatPhone } from '../lib/inputMasks';
import { Button } from './ui/button';
import { MaskedInput } from './ui/masked-input';
import { ValidatedInput } from './ui/validated-input';
import { Dialog, DialogContent, DialogDescription, DialogTitle } from './ui/dialog';
import { FormError } from './Feedback';

const emptyAddress: Endereco = {
  cep: '',
  logradouro: '',
  numero: '',
  complemento: '',
  bairro: '',
  cidade: '',
  estado: '',
};
const automaticFields = ['logradouro', 'bairro', 'cidade', 'estado'] as const;

export function CustomerForm({
  cliente,
  onClose,
  onSave,
}: {
  cliente?: Cliente;
  onClose: () => void;
  onSave: (input: ClienteInput) => Promise<void>;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [document, setDocument] = useState(digits(cliente?.cpfCnpj ?? ''));
  const [phone, setPhone] = useState(digits(cliente?.telefone ?? ''));
  const [address, setAddress] = useState<Endereco>({
    ...emptyAddress,
    ...cliente?.endereco,
    cep: digits(cliente?.endereco?.cep ?? ''),
  });
  const [cepStatus, setCepStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle');
  const [cepMessage, setCepMessage] = useState('');
  const [lookupRevision, setLookupRevision] = useState(0);
  const cepChanged = useRef(false);
  const lastAutoAddress = useRef(address);
  useEffect(() => {
    if (address.cep.length !== 8 || (!cepChanged.current && lookupRevision === 0)) return;
    const controller = new AbortController();
    const initial = { ...address };
    const timer = window.setTimeout(async () => {
      setCepStatus('loading');
      setCepMessage('Buscando endereço…');
      try {
        const found = await lookupCep(initial.cep, controller.signal);
        if (controller.signal.aborted) return;
        setAddress((current) => {
          const next = { ...current };
          for (const field of automaticFields)
            if (current[field] === initial[field]) next[field] = found[field];
          lastAutoAddress.current = next;
          return next;
        });
        setCepStatus('success');
        setCepMessage(
          found.logradouro && found.bairro
            ? 'Endereço encontrado. Informe o número e o complemento, se necessário.'
            : 'CEP encontrado. Complete os campos que não foram informados pelo serviço.',
        );
      } catch (error) {
        if (!controller.signal.aborted) {
          setCepStatus('error');
          setCepMessage(
            error instanceof Error ? error.message : 'Preencha o endereço manualmente.',
          );
        }
      }
    }, 350);
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [address.cep, lookupRevision]);

  function changeCep(value: string) {
    if (value === address.cep) return;
    cepChanged.current = true;
    setCepStatus('idle');
    setCepMessage('');
    setAddress((current) => {
      const next = { ...current, cep: value };
      for (const field of automaticFields)
        if (current[field] === lastAutoAddress.current[field]) next[field] = '';
      return next;
    });
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    if (![11, 14].includes(document.length)) {
      setError(new Error('Informe os 11 dígitos do CPF ou os 14 dígitos do CNPJ.'));
      return;
    }
    if (phone && ![10, 11].includes(digits(formatPhone(phone)).length)) {
      setError(new Error('Informe o telefone com DDD e oito ou nove dígitos.'));
      return;
    }
    const endereco = Object.values(address).some(Boolean)
      ? { ...address, cep: formatCep(address.cep), estado: address.estado.toUpperCase() }
      : null;
    if (endereco?.cep && digits(endereco.cep).length !== 8) {
      setError(new Error('Informe um CEP completo com oito dígitos.'));
      return;
    }
    if (endereco && !/^[A-Z]{2}$/.test(endereco.estado)) {
      setError(new Error('Informe a UF com duas letras para salvar o endereço.'));
      return;
    }
    const input: ClienteInput = {
      nome: String(data.get('nome') ?? '').trim(),
      cpfCnpj: formatDocument(document),
      telefone: formatPhone(phone),
      email: String(data.get('email') ?? '').trim(),
      endereco,
    };
    setBusy(true);
    setError(null);
    try {
      await onSave(input);
    } catch (error) {
      setError(error);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Dialog
      open
      onOpenChange={(open) => {
        if (!open && !busy) onClose();
      }}
    >
      <DialogContent className="customer-dialog">
        <div className="dialog-symbol">
          <Users size={24} />
        </div>
        <DialogTitle className="dialog-title">
          {cliente ? 'Editar cliente' : 'Novo cliente'}
        </DialogTitle>
        <DialogDescription className="muted">
          Dados de contato e endereço para os próximos negócios.
        </DialogDescription>
        <form className="vehicle-form" onSubmit={submit}>
          <fieldset disabled={busy} className="form-fieldset">
            <div className="form-grid">
              <label>
                Nome / razão social
                <ValidatedInput
                  name="nome"
                  required
                  maxLength={80}
                  defaultValue={cliente?.nome ?? ''}
                  autoComplete="name"
                />
              </label>
              <label>
                CPF / CNPJ
                <MaskedInput
                  name="cpfCnpj"
                  required
                  value={document}
                  onValueChange={setDocument}
                  format={formatDocument}
                  placeholder="000.000.000-00"
                />
              </label>
              <label>
                Telefone
                <MaskedInput
                  name="telefone"
                  value={phone}
                  onValueChange={setPhone}
                  format={formatPhone}
                  placeholder="(11) 99999-9999"
                  autoComplete="tel-national"
                />
              </label>
              <label>
                E-mail
                <ValidatedInput
                  name="email"
                  type="email"
                  maxLength={100}
                  defaultValue={cliente?.email ?? ''}
                  autoComplete="email"
                />
              </label>
              <h3 className="span-two form-subtitle">
                <MapPin size={15} />
                Endereço <small>Busca automática por CEP</small>
              </h3>
              <label className="span-two">
                CEP
                <div className="cep-input-row">
                  <MaskedInput
                    name="cep"
                    value={address.cep}
                    onValueChange={changeCep}
                    format={formatCep}
                    placeholder="00000-000"
                    autoComplete="postal-code"
                  />
                  <Button
                    variant="outline"
                    disabled={address.cep.length !== 8 || cepStatus === 'loading'}
                    onClick={() => setLookupRevision((value) => value + 1)}
                    aria-label="Consultar CEP"
                  >
                    {cepStatus === 'loading' ? (
                      <LoaderCircle className="spin" size={17} />
                    ) : (
                      <Search size={17} />
                    )}
                  </Button>
                </div>
              </label>
              {cepMessage && (
                <p className={`span-two cep-message ${cepStatus}`} role="status">
                  {cepMessage}
                </p>
              )}
              {(
                [
                  { name: 'logradouro', label: 'Rua / logradouro', max: 100 },
                  { name: 'numero', label: 'Número', max: 20 },
                  { name: 'complemento', label: 'Complemento', max: 60 },
                  { name: 'bairro', label: 'Bairro', max: 60 },
                  { name: 'cidade', label: 'Cidade', max: 60 },
                  { name: 'estado', label: 'Estado / UF', max: 2 },
                ] as { name: keyof Endereco; label: string; max: number }[]
              ).map((field) => (
                <label key={field.name}>
                  {field.label}
                  <ValidatedInput
                    name={field.name}
                    value={address[field.name]}
                    maxLength={field.max}
                    onChange={(event) =>
                      setAddress((current) => ({
                        ...current,
                        [field.name]:
                          field.name === 'estado'
                            ? event.target.value.toUpperCase()
                            : event.target.value,
                      }))
                    }
                  />
                </label>
              ))}
              <label>
                País
                <input aria-label="País" value="Brasil" readOnly />
              </label>
            </div>
            <FormError error={error} />
            <div className="form-footer">
              <Button variant="outline" onClick={onClose}>
                Cancelar
              </Button>
              <Button type="submit" disabled={cepStatus === 'loading'}>
                {busy ? 'Salvando…' : 'Salvar cliente'}
              </Button>
            </div>
          </fieldset>
        </form>
      </DialogContent>
    </Dialog>
  );
}
