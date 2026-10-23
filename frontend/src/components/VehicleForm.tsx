import { useState } from 'react';
import type { FormEvent } from 'react';
import { CarFront, ArrowRight } from 'lucide-react';
import type { Carro, CarroInput } from '../types';
import { Button } from './ui/button';
import { Dialog, DialogContent, DialogDescription, DialogTitle } from './ui/dialog';
import { FormError } from './Feedback';
import { ValidatedInput } from './ui/validated-input';
import { MoneyInput } from './ui/money-input';
import { parseMoney } from '../lib/inputMasks';

export function VehicleForm({
  vehicle,
  open,
  onClose,
  onSave,
}: {
  vehicle: Carro | null;
  open: boolean;
  onClose: () => void;
  onSave: (input: CarroInput, id?: number) => Promise<void>;
}) {
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const input: CarroInput = {
      marca: String(data.get('marca')).trim(),
      modelo: String(data.get('modelo')).trim(),
      ano: Number(data.get('ano')),
      compra: parseMoney(String(data.get('compra'))),
      quilometragem: Number(data.get('quilometragem')),
    };
    if (!input.marca || !input.modelo) {
      setError('Preencha a marca e o modelo.');
      return;
    }
    if (
      !Number.isInteger(input.ano) ||
      input.ano < 1886 ||
      input.ano > new Date().getFullYear() + 1
    ) {
      setError('Informe um ano válido, até o próximo ano.');
      return;
    }
    if (!Number.isFinite(input.compra) || input.compra < 0 || input.compra > 99999999.99) {
      setError('Informe um valor de compra válido, com até duas casas decimais.');
      return;
    }
    if (
      !Number.isSafeInteger(input.quilometragem) ||
      input.quilometragem < 0 ||
      input.quilometragem > 2147483647
    ) {
      setError('Informe uma quilometragem inteira entre 0 e 2.147.483.647.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await onSave(input, vehicle?.id);
    } catch (error) {
      setError(error);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Dialog
      open={open}
      onOpenChange={(value) => {
        if (!value && !busy) onClose();
      }}
    >
      <DialogContent>
        <div className="dialog-symbol">
          <CarFront size={25} />
        </div>
        <span className="eyebrow">SEU ESTOQUE, EM DIA</span>
        <DialogTitle className="dialog-title">
          {vehicle ? 'Editar veículo' : 'Um novo veículo por aqui.'}
        </DialogTitle>
        <DialogDescription className="muted">
          {vehicle
            ? 'Atualize os dados do veículo selecionado.'
            : 'Preencha os dados para adicionar ao estoque.'}
        </DialogDescription>
        <form onSubmit={submit} className="vehicle-form">
          <fieldset disabled={busy} className="form-fieldset">
            <div className="form-grid">
              <label>
                Marca
                <ValidatedInput
                  name="marca"
                  placeholder="Ex.: Volkswagen"
                  defaultValue={vehicle?.marca}
                  maxLength={60}
                  required
                  autoComplete="off"
                />
              </label>
              <label>
                Modelo
                <ValidatedInput
                  name="modelo"
                  placeholder="Ex.: Polo"
                  defaultValue={vehicle?.modelo}
                  maxLength={60}
                  required
                  autoComplete="off"
                />
              </label>
              <label>
                Ano
                <ValidatedInput
                  name="ano"
                  type="number"
                  placeholder="2024"
                  min={1886}
                  max={new Date().getFullYear() + 1}
                  defaultValue={vehicle?.ano}
                  required
                />
              </label>
              <label>
                Quilometragem (km)
                <ValidatedInput
                  name="quilometragem"
                  type="number"
                  placeholder="0"
                  min="0"
                  max="2147483647"
                  step="1"
                  defaultValue={vehicle?.quilometragem}
                  required
                />
              </label>
              <label className="span-two">
                Valor de compra (R$)
                <MoneyInput
                  name="compra"
                  placeholder="R$ 0,00"
                  defaultValue={vehicle?.compra}
                  required
                />
              </label>
            </div>
            <p className="field-hint">
              Use 0 km para um veículo novo. Novos cadastros começam ativos.
            </p>
            <FormError error={typeof error === 'string' ? new Error(error) : error} />
            <div className="form-footer">
              <Button variant="outline" onClick={onClose}>
                Cancelar
              </Button>
              <Button type="submit">
                {busy ? 'Salvando…' : vehicle ? 'Salvar alterações' : 'Adicionar veículo'}
                <ArrowRight size={17} />
              </Button>
            </div>
          </fieldset>
        </form>
      </DialogContent>
    </Dialog>
  );
}
