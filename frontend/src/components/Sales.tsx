import { useState } from 'react';
import type { FormEvent } from 'react';
import { Banknote, Undo2 } from 'lucide-react';
import type { Carro, Cliente, VendaInput } from '../types';
import { allPages, api } from '../lib/api';
import { currency } from '../lib/estoque';
import { useResource } from '../lib/useResource';
import { Button } from './ui/button';
import { Dialog, DialogContent, DialogDescription, DialogTitle } from './ui/dialog';
import { ErrorState, FormError, Loading } from './Feedback';
import { ConfirmAction, type Confirmation } from './ConfirmAction';
import { Pagination } from './Pagination';
import { MoneyInput } from './ui/money-input';
import { formatDocument, parseMoney } from '../lib/inputMasks';

export function Sales({
  revision,
  onChanged,
  formRequested,
  closeForm,
}: {
  revision: number;
  onChanged: (message?: string) => void;
  formRequested: boolean;
  closeForm: () => void;
}) {
  const [page, setPage] = useState(0);
  const [confirmation, setConfirmation] = useState<Confirmation | null>(null);
  const resource = useResource((signal) => api.vendas(page, signal), [page, revision]);
  return (
    <>
      <section className="panel">
        <div className="panel-heading">
          <h2>Vendas realizadas</h2>
          <span className="count-pill">{resource.data?.totalElements ?? '—'} registros</span>
        </div>
        {resource.loading ? (
          <Loading />
        ) : resource.error ? (
          <ErrorState message={resource.error} retry={resource.reload} />
        ) : !resource.data?.content.length ? (
          <div className="empty-state">
            <Banknote size={34} />
            <h3>Novas conquistas vêm por aí.</h3>
            <p>Nenhuma venda nesta página.</p>
            {page > 0 && <Button onClick={() => setPage(0)}>Voltar ao início</Button>}
          </div>
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>VEÍCULO</th>
                  <th>CLIENTE</th>
                  <th>DATA</th>
                  <th>VALOR NEGOCIADO</th>
                  <th>AÇÕES</th>
                </tr>
              </thead>
              <tbody>
                {resource.data.content.map((venda) => (
                  <tr key={venda.id}>
                    <td>
                      <strong>
                        {venda.carro.marca} {venda.carro.modelo}
                      </strong>
                      <span className="vehicle-subtitle">{venda.carro.ano}</span>
                    </td>
                    <td>
                      {venda.cliente.nome}
                      <span className="vehicle-subtitle">
                        {formatDocument(venda.cliente.cpfCnpj)}
                      </span>
                    </td>
                    <td>{new Date(venda.dataVenda).toLocaleDateString('pt-BR')}</td>
                    <td>
                      {currency(venda.valorVenda)}
                      {venda.observacoes && (
                        <span className="vehicle-subtitle sale-note" title={venda.observacoes}>
                          {venda.observacoes}
                        </span>
                      )}
                    </td>
                    <td>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() =>
                          setConfirmation({
                            title: 'Cancelar esta venda?',
                            description: `A venda de ${venda.carro.marca} ${venda.carro.modelo} para ${venda.cliente.nome} será cancelada. O backend reativará o veículo no estoque.`,
                            label: 'Cancelar venda',
                            action: async () => {
                              await api.remove('vendas', venda.id);
                              setPage(0);
                              onChanged('Venda cancelada. Veículo reativado no estoque.');
                            },
                          })
                        }
                      >
                        <Undo2 size={14} />
                        Cancelar
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {!resource.loading && !resource.error && (
          <Pagination
            page={page}
            total={resource.data?.totalElements ?? 0}
            size={10}
            onChange={setPage}
          />
        )}
      </section>
      {formRequested && (
        <SaleForm
          onClose={closeForm}
          onSave={async (input) => {
            await api.saveVenda(input);
            closeForm();
            setPage(0);
            onChanged('Venda registrada. Veículo inativado no estoque.');
          }}
        />
      )}
      {confirmation && (
        <ConfirmAction confirmation={confirmation} onClose={() => setConfirmation(null)} />
      )}
    </>
  );
}
function SaleForm({
  onClose,
  onSave,
}: {
  onClose: () => void;
  onSave: (input: VendaInput) => Promise<void>;
}) {
  const options = useResource(async (signal) => {
    const [carros, clientes] = await Promise.all([
      allPages<Carro>('carros', signal),
      allPages<Cliente>('clientes', signal),
    ]);
    return { carros, clientes: clientes.filter((cliente) => cliente.ativo) };
  }, []);
  const [carroId, setCarroId] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const selected = options.data?.carros.find((carro) => carro.id === +carroId);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const raw = String(data.get('valorVenda'));
    const valorVenda = parseMoney(raw);
    if (!Number.isFinite(valorVenda) || valorVenda <= 0 || valorVenda > 99999999.99) {
      setError(new Error('Informe um valor de venda positivo com até duas casas decimais.'));
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await onSave({
        carroId: +carroId,
        clienteId: Number(data.get('clienteId')),
        valorVenda,
        observacoes: String(data.get('observacoes') ?? '').trim(),
      });
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
      <DialogContent>
        <div className="dialog-symbol">
          <Banknote size={25} />
        </div>
        <DialogTitle className="dialog-title">Um novo negócio fechado.</DialogTitle>
        <DialogDescription className="muted">
          Escolha o veículo e o cliente. A disponibilidade será validada pela API.
        </DialogDescription>
        {options.loading ? (
          <Loading />
        ) : options.error ? (
          <ErrorState message={options.error} retry={options.reload} />
        ) : (
          <form className="vehicle-form" onSubmit={submit}>
            <fieldset disabled={busy} className="form-fieldset">
              <div className="form-grid">
                <label className="span-two">
                  Veículo disponível
                  <select
                    required
                    value={carroId}
                    onChange={(event) => setCarroId(event.target.value)}
                  >
                    <option value="">Selecione um veículo</option>
                    {options.data?.carros.map((carro) => (
                      <option key={carro.id} value={carro.id}>
                        {carro.marca} {carro.modelo} · {carro.ano} · #{carro.id}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="span-two">
                  Cliente ativo
                  <select name="clienteId" required defaultValue="">
                    <option value="">Selecione um cliente</option>
                    {options.data?.clientes.map((cliente) => (
                      <option key={cliente.id} value={cliente.id}>
                        {cliente.nome} · {formatDocument(cliente.cpfCnpj)}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="span-two">
                  Valor negociado (R$)
                  <MoneyInput name="valorVenda" required placeholder="R$ 0,00" />
                </label>
                {selected && (
                  <p className="field-hint span-two">
                    Venda sugerida pela API: <strong>{currency(selected.venda ?? 0)}</strong>
                  </p>
                )}
                <label className="span-two">
                  Observações
                  <textarea name="observacoes" maxLength={255} rows={3} />
                </label>
              </div>
              {(!options.data?.carros.length || !options.data?.clientes.length) && (
                <p className="form-error">
                  Cadastre um veículo e um cliente ativos para registrar uma venda.
                </p>
              )}
              <FormError error={error} />
              <div className="form-footer">
                <Button variant="outline" onClick={onClose}>
                  Cancelar
                </Button>
                <Button
                  type="submit"
                  disabled={!options.data?.carros.length || !options.data?.clientes.length}
                >
                  {busy ? 'Registrando…' : 'Registrar venda'}
                </Button>
              </div>
            </fieldset>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
}
