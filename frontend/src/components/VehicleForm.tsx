import { useState } from 'react';
import type { FormEvent } from 'react';
import { CarFront, ArrowRight } from 'lucide-react';
import type { Carro, CarroInput } from '../types';
import { Button } from './ui/button';
import { Dialog, DialogContent, DialogDescription, DialogTitle } from './ui/dialog';

export function VehicleForm({
  vehicle,
  open,
  onClose,
  onSave,
}: {
  vehicle: Carro | null;
  open: boolean;
  onClose: () => void;
  onSave: (input: CarroInput, id?: number) => void;
}) {
  const [error, setError] = useState('');
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const input: CarroInput = {
      marca: String(data.get('marca')).trim(),
      modelo: String(data.get('modelo')).trim(),
      ano: Number(data.get('ano')),
      compra: Number(String(data.get('compra')).replace(',', '.')),
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
    if (
      !Number.isFinite(input.compra) ||
      input.compra < 0 ||
      input.compra > 99999999.99 ||
      !/^\d+(?:[.,]\d{1,2})?$/.test(String(data.get('compra')))
    ) {
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
    onSave(input, vehicle?.id);
  }
  return (
    <Dialog
      open={open}
      onOpenChange={(value) => {
        if (!value) onClose();
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
            : 'Preencha os dados para adicionar ao estoque de demonstração.'}
        </DialogDescription>
        <form onSubmit={submit} className="vehicle-form">
          <div className="form-grid">
            <label>
              Marca
              <input
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
              <input
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
              <input
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
              <input
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
              <input
                name="compra"
                inputMode="decimal"
                placeholder="Ex.: 85000,00"
                defaultValue={vehicle?.compra.toFixed(2).replace('.', ',')}
                required
              />
            </label>
          </div>
          <p className="field-hint">
            Use 0 km para um veículo novo. Novos cadastros começam ativos.
          </p>
          {error && (
            <p className="form-error" role="alert">
              {error}
            </p>
          )}
          <div className="form-footer">
            <Button variant="outline" onClick={onClose}>
              Cancelar
            </Button>
            <Button type="submit">
              {vehicle ? 'Salvar alterações' : 'Adicionar veículo'}
              <ArrowRight size={17} />
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
