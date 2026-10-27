import { useState } from 'react';
import { CustomerForm } from './CustomerForm';
import { formatCep, formatDocument, formatPhone } from '../lib/inputMasks';
import {
  Banknote,
  Calendar,
  Eye,
  FileSpreadsheet,
  Mail,
  MapPin,
  Pencil,
  Phone,
  Power,
  Trash2,
  UserCheck,
  Users,
} from 'lucide-react';
import type { Cliente, Venda } from '../types';
import { allPages, api } from '../lib/api';
import { currency } from '../lib/estoque';
import { useResource } from '../lib/useResource';
import { Button } from './ui/button';
import { Dialog, DialogContent, DialogDescription, DialogTitle } from './ui/dialog';
import { ErrorState, Loading } from './Feedback';
import { ConfirmAction, type Confirmation } from './ConfirmAction';
import { Pagination } from './Pagination';
import { Status } from './Stock';
import { statusLabel } from './Proposals';

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
  const [viewing, setViewing] = useState<Cliente | null>(null);
  const [editing, setEditing] = useState<Cliente>();
  const [confirmation, setConfirmation] = useState<Confirmation | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const resource = useResource((signal) => api.clientes(page, signal), [page, revision]);

  return (
    <>
      <section className="panel">
        <div className="panel-heading">
          <div>
            <h2>Clientes</h2>
            <p className="muted" style={{ fontSize: '0.84rem', marginTop: '0.15rem' }}>
              Clique em um cliente para abrir a ficha completa (endereço, propostas e histórico de
              compras).
            </p>
          </div>
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
                  <tr
                    key={cliente.id}
                    style={{ cursor: 'pointer' }}
                    onClick={() => setViewing(cliente)}
                  >
                    <td>
                      <button
                        type="button"
                        className="vehicle-name"
                        onClick={(event) => {
                          event.stopPropagation();
                          setViewing(cliente);
                        }}
                      >
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
                    <td onClick={(event) => event.stopPropagation()}>
                      <div className="vehicle-actions">
                        <Button
                          variant="ghost"
                          size="icon"
                          title="Ver ficha completa do cliente"
                          aria-label={`Ver detalhes de ${cliente.nome}`}
                          onClick={() => setViewing(cliente)}
                        >
                          <Eye size={15} />
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

      {viewing && (
        <CustomerDetailsModal
          cliente={viewing}
          onClose={() => setViewing(null)}
          onEdit={() => {
            const target = viewing;
            setViewing(null);
            setEditing(target);
          }}
        />
      )}

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

function CustomerDetailsModal({
  cliente,
  onClose,
  onEdit,
}: {
  cliente: Cliente;
  onClose: () => void;
  onEdit: () => void;
}) {
  const history = useResource(async (signal) => {
    const [propostasPage, todasVendas] = await Promise.all([
      api.propostasPorCliente(cliente.id, signal),
      allPages<Venda>('vendas', signal),
    ]);
    return {
      propostas: propostasPage.content,
      vendas: todasVendas.filter((v) => v.cliente.id === cliente.id),
    };
  }, [cliente.id]);

  const end = cliente.endereco;
  const totalComprado =
    history.data?.vendas
      .filter((v) => !v.cancelada)
      .reduce((acc, v) => acc + v.valorVenda, 0) ?? 0;

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent style={{ maxWidth: '720px', maxHeight: '88vh', overflowY: 'auto' }}>
        <div className="dialog-symbol">
          <UserCheck size={24} />
        </div>
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            gap: '1rem',
            flexWrap: 'wrap',
          }}
        >
          <div>
            <DialogTitle className="dialog-title">{cliente.nome}</DialogTitle>
            <DialogDescription className="muted">
              Documento: <strong>{formatDocument(cliente.cpfCnpj)}</strong> · Código #{cliente.id}
            </DialogDescription>
          </div>
          <Status active={cliente.ativo} />
        </div>

        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
            gap: '0.85rem',
            marginTop: '0.5rem',
          }}
        >
          <div
            style={{
              padding: '0.9rem',
              borderRadius: '0.75rem',
              border: '1px solid var(--border, #334155)',
              background: 'rgba(148, 163, 184, 0.05)',
              display: 'grid',
              gap: '0.45rem',
              fontSize: '0.88rem',
            }}
          >
            <strong style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <Phone size={15} /> Contato & Cadastro
            </strong>
            <div>
              <span className="muted">E-mail: </span>
              <strong>{cliente.email || 'Não informado'}</strong>
            </div>
            <div>
              <span className="muted">Telefone: </span>
              <strong>{formatPhone(cliente.telefone || '') || 'Não informado'}</strong>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
              <Calendar size={14} className="muted" />
              <span className="muted">Cliente desde: </span>
              <strong>
                {cliente.dataCadastro
                  ? new Date(cliente.dataCadastro).toLocaleDateString('pt-BR')
                  : '—'}
              </strong>
            </div>
          </div>

          <div
            style={{
              padding: '0.9rem',
              borderRadius: '0.75rem',
              border: '1px solid var(--border, #334155)',
              background: 'rgba(148, 163, 184, 0.05)',
              display: 'grid',
              gap: '0.45rem',
              fontSize: '0.88rem',
            }}
          >
            <strong style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <MapPin size={15} /> Endereço Completo
            </strong>
            {end && (end.logradouro || end.cidade) ? (
              <>
                <div>
                  <strong>
                    {end.logradouro || 'Logradouro não informado'}
                    {end.numero ? `, ${end.numero}` : ''}
                  </strong>
                  {end.complemento ? ` · ${end.complemento}` : ''}
                </div>
                <div>
                  {end.bairro ? `${end.bairro} — ` : ''}
                  {end.cidade || '—'} {end.estado ? `/ ${end.estado}` : ''}
                </div>
                <div>
                  <span className="muted">CEP: </span>
                  <strong>{end.cep ? formatCep(end.cep) : 'Não informado'}</strong>
                </div>
              </>
            ) : (
              <p className="muted" style={{ margin: 0 }}>
                Nenhum endereço cadastrado para este cliente.
              </p>
            )}
          </div>
        </div>

        <div style={{ marginTop: '0.75rem', display: 'grid', gap: '0.9rem' }}>
          <div>
            <h4
              style={{
                margin: '0 0 0.45rem 0',
                display: 'flex',
                alignItems: 'center',
                gap: '0.45rem',
                fontSize: '0.94rem',
              }}
            >
              <FileSpreadsheet size={16} />
              Propostas Comerciais ({history.data?.propostas.length ?? 0})
            </h4>
            {history.loading ? (
              <p className="muted" style={{ fontSize: '0.85rem' }}>
                Carregando histórico comercial…
              </p>
            ) : !history.data?.propostas.length ? (
              <p className="muted" style={{ fontSize: '0.85rem', margin: 0 }}>
                Nenhuma proposta comercial registrada para este cliente.
              </p>
            ) : (
              <div
                style={{
                  display: 'grid',
                  gap: '0.45rem',
                  maxHeight: '170px',
                  overflowY: 'auto',
                }}
              >
                {history.data.propostas.map((prop) => (
                  <div
                    key={prop.id}
                    style={{
                      padding: '0.6rem 0.8rem',
                      borderRadius: '0.6rem',
                      border: '1px solid var(--border, #334155)',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      gap: '0.5rem',
                      fontSize: '0.84rem',
                    }}
                  >
                    <div>
                      <strong>{prop.numero}</strong> · {prop.carro.marca} {prop.carro.modelo} (
                      {prop.carro.ano})
                      <div className="muted" style={{ fontSize: '0.78rem' }}>
                        Rev. #{prop.revisaoAtualSequencia} ·{' '}
                        {new Date(prop.dataCriacao).toLocaleDateString('pt-BR')}
                      </div>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <strong>
                        {prop.revisaoAtual ? currency(prop.revisaoAtual.precoNegociado) : '—'}
                      </strong>
                      <div className="muted" style={{ fontSize: '0.78rem' }}>
                        {statusLabel(prop.status)}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div>
            <h4
              style={{
                margin: '0 0 0.45rem 0',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                fontSize: '0.94rem',
              }}
            >
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.45rem' }}>
                <Banknote size={16} />
                Histórico de Compras na Concessionária ({history.data?.vendas.length ?? 0})
              </span>
              {totalComprado > 0 && (
                <span style={{ fontSize: '0.82rem', color: '#10b981' }}>
                  Total adquirido: {currency(totalComprado)}
                </span>
              )}
            </h4>
            {history.loading ? (
              <p className="muted" style={{ fontSize: '0.85rem' }}>
                Consultando vendas…
              </p>
            ) : !history.data?.vendas.length ? (
              <p className="muted" style={{ fontSize: '0.85rem', margin: 0 }}>
                Nenhuma compra concluída até o momento.
              </p>
            ) : (
              <div
                style={{
                  display: 'grid',
                  gap: '0.45rem',
                  maxHeight: '170px',
                  overflowY: 'auto',
                }}
              >
                {history.data.vendas.map((venda) => (
                  <div
                    key={venda.id}
                    style={{
                      padding: '0.6rem 0.8rem',
                      borderRadius: '0.6rem',
                      border: '1px solid var(--border, #334155)',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      gap: '0.5rem',
                      fontSize: '0.84rem',
                    }}
                  >
                    <div>
                      <strong>
                        Venda #{venda.id} · {venda.carro.marca} {venda.carro.modelo} (
                        {venda.carro.ano})
                      </strong>
                      <div className="muted" style={{ fontSize: '0.78rem' }}>
                        Data: {new Date(venda.dataVenda).toLocaleDateString('pt-BR')}{' '}
                        {venda.observacoes ? `· ${venda.observacoes}` : ''}
                      </div>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <strong>{currency(venda.valorVenda)}</strong>
                      {venda.cancelada && (
                        <div style={{ fontSize: '0.75rem', color: '#ef4444' }}>Cancelada</div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <div
          className="form-footer"
          style={{ marginTop: '0.75rem', justifyContent: 'space-between', flexWrap: 'wrap' }}
        >
          <Button type="button" variant="outline" onClick={onEdit}>
            <Pencil size={15} />
            Editar cadastro do cliente
          </Button>
          <Button type="button" onClick={onClose}>
            Fechar ficha
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
