import { useState } from 'react';
import { CustomerForm } from './CustomerForm';
import { formatDocument, formatPhone } from '../lib/inputMasks';
import { Pencil, Power, Trash2, Users } from 'lucide-react';
import type { Cliente } from '../types';
import { api } from '../lib/api';
import { useResource } from '../lib/useResource';
import { Button } from './ui/button';
import { ErrorState, Loading } from './Feedback';
import { ConfirmAction, type Confirmation } from './ConfirmAction';
import { Pagination } from './Pagination';
import { Status } from './Stock';

export function Customers({
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
  const [editing, setEditing] = useState<Cliente>();
  const [confirmation, setConfirmation] = useState<Confirmation | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const resource = useResource((signal) => api.clientes(page, signal), [page, revision]);
  return (
    <>
      <section className="panel">
        <div className="panel-heading">
          <h2>Clientes</h2>
          <span className="count-pill">{resource.data?.totalElements ?? '—'} cadastros</span>
        </div>
        {error && <ErrorState message={error} />}
        {resource.loading ? (
          <Loading />
        ) : resource.error ? (
          <ErrorState message={resource.error} retry={resource.reload} />
        ) : !resource.data?.content.length ? (
          <div className="empty-state">
            <Users size={32} />
            <h3>Nenhum cliente nesta página.</h3>
            {page > 0 && <Button onClick={() => setPage(0)}>Voltar ao início</Button>}
          </div>
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>NOME / DOCUMENTO</th>
                  <th>CONTATO</th>
                  <th>CIDADE / UF</th>
                  <th>STATUS</th>
                  <th>AÇÕES</th>
                </tr>
              </thead>
              <tbody>
                {resource.data.content.map((cliente) => (
                  <tr key={cliente.id}>
                    <td>
                      <button className="vehicle-name" onClick={() => setEditing(cliente)}>
                        {cliente.nome}
                      </button>
                      <span className="vehicle-subtitle">{formatDocument(cliente.cpfCnpj)}</span>
                    </td>
                    <td>
                      {cliente.email || 'Sem e-mail'}
                      <span className="vehicle-subtitle">
                        {formatPhone(cliente.telefone || '') || 'Sem telefone'}
                      </span>
                    </td>
                    <td>
                      {cliente.endereco?.cidade || '—'}{' '}
                      {cliente.endereco?.estado && `/ ${cliente.endereco.estado}`}
                    </td>
                    <td>
                      <Status active={cliente.ativo} />
                    </td>
                    <td>
                      <div className="vehicle-actions">
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label={`Editar ${cliente.nome}`}
                          onClick={() => setEditing(cliente)}
                        >
                          <Pencil size={15} />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          disabled={busy}
                          aria-label={`${cliente.ativo ? 'Inativar' : 'Ativar'} ${cliente.nome}`}
                          onClick={async () => {
                            setBusy(true);
                            setError('');
                            try {
                              await api.toggle('clientes', cliente.id, cliente.ativo);
                              onChanged('Status do cliente atualizado.');
                            } catch (error) {
                              setError(
                                error instanceof Error
                                  ? error.message
                                  : 'Não foi possível atualizar.',
                              );
                            } finally {
                              setBusy(false);
                            }
                          }}
                        >
                          <Power size={15} />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="delete-action"
                          aria-label={`Excluir ${cliente.nome}`}
                          onClick={() =>
                            setConfirmation({
                              title: 'Excluir cliente?',
                              description: `${cliente.nome} será removido. Clientes com vendas vinculadas não podem ser excluídos.`,
                              label: 'Excluir cliente',
                              action: async () => {
                                await api.remove('clientes', cliente.id);
                                setPage(0);
                                onChanged('Cliente excluído.');
                              },
                            })
                          }
                        >
                          <Trash2 size={15} />
                        </Button>
                      </div>
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
      {(formRequested || editing) && (
        <CustomerForm
          key={editing?.id ?? 'new'}
          cliente={editing}
          onClose={() => {
            setEditing(undefined);
            closeForm();
          }}
          onSave={async (input) => {
            await api.saveCliente(input, editing?.id);
            setEditing(undefined);
            closeForm();
            setPage(0);
            onChanged('Cliente salvo.');
          }}
        />
      )}
      {confirmation && (
        <ConfirmAction confirmation={confirmation} onClose={() => setConfirmation(null)} />
      )}
    </>
  );
}
