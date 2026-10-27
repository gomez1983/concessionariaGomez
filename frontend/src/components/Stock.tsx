import { useEffect, useState } from 'react';
import {
  Calendar,
  Camera,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Clock,
  Eye,
  FileSpreadsheet,
  Gauge,
  LayoutGrid,
  List,
  Pencil,
  Power,
  Search,
  ShieldCheck,
  SlidersHorizontal,
  Trash2,
  Wallet,
  X,
} from 'lucide-react';
import type { Carro, Filters, PerfilOperador, Sort, Venda } from '../types';
import { allPages, api, carroQuery, getOperadorContext, photoUrl } from '../lib/api';
import { currency, emptyFilters, number } from '../lib/estoque';
import { useResource } from '../lib/useResource';
import { Button } from './ui/button';
import { Dialog, DialogContent, DialogDescription, DialogTitle } from './ui/dialog';
import { VehicleForm } from './VehicleForm';
import { VehicleImage } from './VehicleImage';
import { VehicleGallery } from './VehicleGallery';
import { ConfirmAction, type Confirmation } from './ConfirmAction';
import { ErrorState, Loading } from './Feedback';
import { Pagination } from './Pagination';
import { BrandLogo } from './BrandLogo';
import { MoneyInput } from './ui/money-input';
import { ProposalSimulatorDialog, statusLabel } from './Proposals';

export function Stock({
  revision,
  brands,
  onChanged,
  formRequested,
  closeForm,
  search,
  onSearchChange,
}: {
  revision: number;
  brands: string[];
  onChanged: (message?: string) => void;
  formRequested: boolean;
  closeForm: () => void;
  search: string;
  onSearchChange: (value: string) => void;
}) {
  const [filters, setFilters] = useState<Filters>(emptyFilters);
  const [sort, setSort] = useState<Sort>('recentes');
  const [page, setPage] = useState(0);
  const [view, setView] = useState<'list' | 'grid'>('grid');
  const [advanced, setAdvanced] = useState(false);
  const [viewing, setViewing] = useState<Carro | null>(null);
  const [simulatingCarro, setSimulatingCarro] = useState<Carro | null>(null);
  const [editing, setEditing] = useState<Carro | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [gallery, setGallery] = useState<Carro | null>(null);
  const [confirmation, setConfirmation] = useState<Confirmation | null>(null);
  const [busyId, setBusyId] = useState<number>();
  const [actionError, setActionError] = useState('');
  const perfil: PerfilOperador = getOperadorContext().perfil;

  useEffect(() => setPage(0), [search]);
  const query = carroQuery({ ...filters, search }, page, sort);
  const invalid =
    (filters.minValor !== '' && filters.maxValor !== '' && +filters.minValor > +filters.maxValor) ||
    (filters.minKm !== '' && filters.maxKm !== '' && +filters.minKm > +filters.maxKm);
  const resource = useResource((signal) => api.carros(query, signal), [query, revision]);
  const vehicles = resource.data?.content ?? [];
  const count = Object.values({ ...filters, search }).filter(Boolean).length;

  function filter(key: keyof Filters, value: string) {
    setFilters((previous) => ({ ...previous, [key]: value }));
    setPage(0);
  }
  function clear() {
    onSearchChange('');
    setFilters(emptyFilters);
    setPage(0);
  }
  function edit(vehicle: Carro) {
    setEditing(vehicle);
    setFormOpen(true);
  }
  function close() {
    setFormOpen(false);
    setEditing(null);
    closeForm();
  }
  function remove(vehicle: Carro) {
    setConfirmation({
      title: 'Excluir este veículo?',
      description: `${vehicle.marca} ${vehicle.modelo} será excluído. Veículos com vendas ou fotos vinculadas precisam ter esses vínculos resolvidos antes da exclusão.`,
      label: 'Excluir veículo',
      action: async () => {
        await api.remove('carros', vehicle.id);
        setPage(0);
        onChanged('Veículo excluído.');
      },
    });
  }
  async function toggle(vehicle: Carro) {
    setBusyId(vehicle.id);
    setActionError('');
    try {
      await api.toggle('carros', vehicle.id, vehicle.ativo);
      onChanged(`Veículo ${vehicle.ativo ? 'inativado' : 'ativado'}.`);
    } catch (error) {
      setActionError(
        error instanceof Error ? error.message : 'Não foi possível atualizar o status.',
      );
    } finally {
      setBusyId(undefined);
    }
  }
  const actions = (vehicle: Carro) => (
    <div className="vehicle-actions" onClick={(e) => e.stopPropagation()}>
      <Button
        size="icon"
        variant="ghost"
        aria-label={`Detalhes de ${vehicle.marca} ${vehicle.modelo}`}
        title="Ver ficha completa do veículo"
        onClick={() => setViewing(vehicle)}
      >
        <Eye size={16} />
      </Button>
      <Button
        size="icon"
        variant="ghost"
        aria-label={`Fotos de ${vehicle.marca} ${vehicle.modelo}`}
        title="Gerenciar fotos"
        onClick={() => setGallery(vehicle)}
      >
        <Camera size={16} />
      </Button>
      <Button
        size="icon"
        variant="ghost"
        aria-label={`Editar ${vehicle.marca} ${vehicle.modelo}`}
        title="Editar veículo"
        onClick={() => edit(vehicle)}
      >
        <Pencil size={15} />
      </Button>
      <Button
        size="icon"
        variant="ghost"
        disabled={busyId !== undefined}
        aria-label={`${vehicle.ativo ? 'Inativar' : 'Ativar'} ${vehicle.marca} ${vehicle.modelo}`}
        title={vehicle.ativo ? 'Inativar' : 'Ativar'}
        onClick={() => void toggle(vehicle)}
      >
        <Power size={15} />
      </Button>
      <Button
        size="icon"
        variant="ghost"
        className="delete-action"
        aria-label={`Excluir ${vehicle.marca} ${vehicle.modelo}`}
        title="Excluir veículo"
        onClick={() => remove(vehicle)}
      >
        <Trash2 size={15} />
      </Button>
    </div>
  );
  return (
    <>
      <section className="panel stock-panel" aria-labelledby="inventory-title">
        <div className="panel-heading">
          <div className="inventory-title">
            <h2 id="inventory-title">Veículos</h2>
            <span className="count-pill">{resource.data?.totalElements ?? '—'}</span>
          </div>
          <div className="view-switch">
            <button
              aria-label="Visualizar em lista"
              aria-pressed={view === 'list'}
              onClick={() => setView('list')}
            >
              <List size={17} />
            </button>
            <button
              aria-label="Visualizar em cards"
              aria-pressed={view === 'grid'}
              onClick={() => setView('grid')}
            >
              <LayoutGrid size={16} />
            </button>
          </div>
        </div>
        <div className="filter-toolbar">
          <label className="select-wrap">
            <span className="sr-only">Filtrar por marca</span>
            <select value={filters.marca} onChange={(event) => filter('marca', event.target.value)}>
              <option value="">Todas as marcas</option>
              {brands.map((brand) => (
                <option key={brand}>{brand}</option>
              ))}
            </select>
            <ChevronDown size={14} />
          </label>
          <label className="select-wrap">
            <span className="sr-only">Filtrar por ano</span>
            <select value={filters.ano} onChange={(event) => filter('ano', event.target.value)}>
              <option value="">Todos os anos</option>
              {Array.from(
                { length: new Date().getFullYear() + 2 - 1886 },
                (_, index) => new Date().getFullYear() + 1 - index,
              ).map((year) => (
                <option key={year}>{year}</option>
              ))}
            </select>
            <ChevronDown size={14} />
          </label>
          <label className="select-wrap">
            <span className="sr-only">Filtrar por status</span>
            <select value={filters.ativo} onChange={(event) => filter('ativo', event.target.value)}>
              <option value="">Todos os status</option>
              <option value="true">Ativos</option>
              <option value="false">Inativos</option>
            </select>
            <ChevronDown size={14} />
          </label>
          <Button
            variant="outline"
            aria-expanded={advanced}
            aria-controls="advanced-filters"
            onClick={() => setAdvanced(!advanced)}
          >
            <SlidersHorizontal size={16} />
            Filtros{count > 0 && <span className="filter-count">{count}</span>}
          </Button>
        </div>
        {advanced && (
          <div className="advanced-filters" id="advanced-filters">
            {(
              [
                { key: 'minValor', label: 'Compra mínima (R$)' },
                { key: 'maxValor', label: 'Compra máxima (R$)' },
                { key: 'minKm', label: 'Km mínimo' },
                { key: 'maxKm', label: 'Km máximo' },
              ] as { key: keyof Filters; label: string }[]
            ).map((field) => (
              <label key={field.key}>
                {field.label}
                {field.key.includes('Valor') ? (
                  <MoneyInput
                    aria-label={field.label}
                    value={filters[field.key]}
                    valueIsNumericString
                    placeholder="Sem limite"
                    onValueChange={(values) => filter(field.key, values.value)}
                  />
                ) : (
                  <input
                    type="number"
                    min="0"
                    step={field.key.includes('Valor') ? '.01' : '1'}
                    placeholder="Sem limite"
                    value={filters[field.key]}
                    onChange={(event) => filter(field.key, event.target.value)}
                  />
                )}
              </label>
            ))}
            {invalid && (
              <p className="form-error range-error">O mínimo deve ser menor ou igual ao máximo.</p>
            )}
          </div>
        )}
        <div className="results-toolbar">
          <span>
            {count
              ? `${resource.data?.totalElements ?? 0} resultados`
              : 'Clique em qualquer veículo para ver a ficha completa e todas as fotos'}
            {count > 0 && (
              <button onClick={clear}>
                Limpar filtros
                <X size={12} />
              </button>
            )}
          </span>
          <label>
            Ordenar por:
            <select
              aria-label="Ordenar veículos"
              value={sort}
              onChange={(event) => {
                setSort(event.target.value as Sort);
                setPage(0);
              }}
            >
              <option value="recentes">Mais recentes</option>
              <option value="valor-asc">Menor valor de compra</option>
              <option value="valor-desc">Maior valor de compra</option>
              <option value="ano-desc">Ano mais novo</option>
            </select>
          </label>
        </div>
        {actionError && <ErrorState message={actionError} />}
        {resource.loading ? (
          <Loading />
        ) : resource.error ? (
          <ErrorState message={resource.error} retry={resource.reload} />
        ) : !vehicles.length ? (
          <div className="empty-state">
            <Search size={30} />
            <h3>Nenhum veículo por aqui.</h3>
            <p>Adicione um veículo ou ajuste os filtros de busca.</p>
            {page > 0 ? (
              <Button onClick={() => setPage(0)}>Voltar ao início</Button>
            ) : (
              count > 0 && (
                <Button variant="outline" onClick={clear}>
                  Limpar filtros
                </Button>
              )
            )}
          </div>
        ) : view === 'grid' ? (
          <div className="vehicle-grid">
            {vehicles.map((vehicle) => (
              <article
                className="vehicle-card"
                key={vehicle.id}
                style={{ cursor: 'pointer' }}
                onClick={() => setViewing(vehicle)}
              >
                <button
                  type="button"
                  className="card-art photo-trigger"
                  aria-label={`Ver ficha completa de ${vehicle.marca} ${vehicle.modelo}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    setViewing(vehicle);
                  }}
                >
                  <VehicleImage
                    id={vehicle.id}
                    marca={vehicle.marca}
                    modelo={vehicle.modelo}
                    revision={revision}
                    large
                  />
                  <span className="photo-overlay">
                    <Eye size={15} />
                    Ver ficha e fotos
                  </span>
                </button>
                <div className="card-body">
                  <div className="card-name-line">
                    <BrandLogo marca={vehicle.marca} />
                    <button
                      type="button"
                      className="vehicle-name"
                      onClick={(e) => {
                        e.stopPropagation();
                        setViewing(vehicle);
                      }}
                      title={`${vehicle.marca} ${vehicle.modelo}`}
                    >
                      {vehicle.marca} {vehicle.modelo}
                    </button>
                    <span className="year-chip">{vehicle.ano}</span>
                  </div>
                  <div className="card-meta-line">
                    <span>
                      {vehicle.zeroKm ? 'Zero km' : `${number(vehicle.quilometragem)} km`}
                    </span>
                    <Status active={vehicle.ativo} />
                  </div>
                  <div className="sale-price">
                    <span>
                      Compra:{' '}
                      {vehicle.compra != null ? currency(vehicle.compra) : 'Restrito'}
                    </span>
                    <strong>
                      <small>Venda:</small> {currency(vehicle.venda ?? 0)}
                    </strong>
                  </div>
                  <div className="card-actions-footer">{actions(vehicle)}</div>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>VEÍCULO</th>
                  <th>ANO / KM</th>
                  <th>COMPRA / VENDA SUGERIDA</th>
                  <th>STATUS</th>
                  <th>AÇÕES</th>
                </tr>
              </thead>
              <tbody>
                {vehicles.map((vehicle) => (
                  <tr
                    key={vehicle.id}
                    style={{ cursor: 'pointer' }}
                    onClick={() => setViewing(vehicle)}
                  >
                    <td>
                      <div className="vehicle-cell">
                        <button
                          type="button"
                          className="vehicle-thumb photo-trigger"
                          aria-label={`Ver ficha completa de ${vehicle.marca} ${vehicle.modelo}`}
                          onClick={(e) => {
                            e.stopPropagation();
                            setViewing(vehicle);
                          }}
                        >
                          <VehicleImage
                            id={vehicle.id}
                            marca={vehicle.marca}
                            modelo={vehicle.modelo}
                            revision={revision}
                          />
                        </button>
                        <button
                          type="button"
                          className="vehicle-name"
                          onClick={(e) => {
                            e.stopPropagation();
                            setViewing(vehicle);
                          }}
                        >
                          <BrandLogo marca={vehicle.marca} />
                          {vehicle.marca} {vehicle.modelo}
                        </button>
                      </div>
                    </td>
                    <td>
                      {vehicle.ano}
                      <span className="vehicle-subtitle">{number(vehicle.quilometragem)} km</span>
                    </td>
                    <td>
                      {vehicle.compra != null ? currency(vehicle.compra) : 'Restrito'}
                      <span className="vehicle-subtitle">
                        Venda: {currency(vehicle.venda ?? 0)}
                      </span>
                    </td>
                    <td>
                      <Status active={vehicle.ativo} />
                    </td>
                    <td>{actions(vehicle)}</td>
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
            size={20}
            onChange={setPage}
          />
        )}
      </section>

      {viewing && (
        <VehicleDetailsModal
          vehicle={viewing}
          revision={revision}
          onClose={() => setViewing(null)}
          onOpenGallery={() => {
            const target = viewing;
            setViewing(null);
            setGallery(target);
          }}
          onEdit={() => {
            const target = viewing;
            setViewing(null);
            edit(target);
          }}
          onSimulateProposal={() => {
            const target = viewing;
            setViewing(null);
            setSimulatingCarro(target);
          }}
        />
      )}

      {simulatingCarro && (
        <ProposalSimulatorDialog
          initialCarroId={simulatingCarro.id}
          perfil={perfil}
          onClose={() => setSimulatingCarro(null)}
          onSaved={(msg) => {
            setSimulatingCarro(null);
            onChanged(msg);
          }}
        />
      )}

      {(formOpen || formRequested) && (
        <VehicleForm
          key={editing?.id ?? 'new'}
          vehicle={editing}
          open
          onClose={close}
          onSave={async (input, id) => {
            await api.saveCarro(input, id);
            close();
            if (!id) {
              clear();
              setSort('recentes');
            }
            onChanged(
              id
                ? 'Veículo atualizado.'
                : 'Veículo cadastrado. Abra a galeria para adicionar fotos.',
            );
          }}
        />
      )}
      {gallery && (
        <VehicleGallery
          vehicle={gallery}
          onClose={() => setGallery(null)}
          onChanged={() => onChanged()}
        />
      )}
      {confirmation && (
        <ConfirmAction confirmation={confirmation} onClose={() => setConfirmation(null)} />
      )}
    </>
  );
}

function VehicleDetailsModal({
  vehicle,
  revision,
  onClose,
  onOpenGallery,
  onEdit,
  onSimulateProposal,
}: {
  vehicle: Carro;
  revision: number;
  onClose: () => void;
  onOpenGallery: () => void;
  onEdit: () => void;
  onSimulateProposal: () => void;
}) {
  const [selectedFotoIdx, setSelectedFotoIdx] = useState(0);

  const details = useResource(async (signal) => {
    const [fotos, propostasPage, todasVendas] = await Promise.all([
      api.fotos(vehicle.id, signal),
      api.propostasPorCarro(vehicle.id, signal),
      allPages<Venda>('vendas', signal),
    ]);
    return {
      fotos,
      propostas: propostasPage.content,
      vendas: todasVendas.filter((v) => v.carro.id === vehicle.id),
    };
  }, [vehicle.id, revision]);

  const fotos = details.data?.fotos ?? [];
  const activeFoto = fotos[selectedFotoIdx] ?? fotos[0];
  const margemBrutaProjetada =
    vehicle.compra != null && vehicle.venda != null ? vehicle.venda - vehicle.compra : undefined;
  const margemPercentualSobreVenda =
    margemBrutaProjetada !== undefined && vehicle.venda > 0
      ? (margemBrutaProjetada / vehicle.venda) * 100
      : undefined;

  const disponibilidadeLabel =
    vehicle.disponibilidadeComercial === 'VENDIDO' || !vehicle.ativo
      ? { text: 'Vendido / Indisponível', color: '#ef4444', bg: 'rgba(239, 68, 68, 0.14)' }
      : vehicle.disponibilidadeComercial === 'RESERVADO'
        ? {
            text: `Reservado (${vehicle.reservaResponsavel ?? 'Proposta ativa'})`,
            color: '#f59e0b',
            bg: 'rgba(245, 158, 11, 0.14)',
          }
        : { text: 'Disponível para negociação', color: '#10b981', bg: 'rgba(16, 185, 129, 0.14)' };

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent style={{ maxWidth: '780px', maxHeight: '90vh', overflowY: 'auto' }}>
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            gap: '1rem',
            flexWrap: 'wrap',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <BrandLogo marca={vehicle.marca} />
            <div>
              <DialogTitle className="dialog-title">
                {vehicle.marca} {vehicle.modelo} ({vehicle.ano})
              </DialogTitle>
              <DialogDescription className="muted">
                Código #{vehicle.id} ·{' '}
                {vehicle.zeroKm ? 'Veículo 0 km' : `${number(vehicle.quilometragem)} km rodados`}
              </DialogDescription>
            </div>
          </div>
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
            <span
              style={{
                padding: '0.25rem 0.65rem',
                borderRadius: '999px',
                fontSize: '0.76rem',
                fontWeight: 600,
                color: disponibilidadeLabel.color,
                background: disponibilidadeLabel.bg,
              }}
            >
              {disponibilidadeLabel.text}
            </span>
            <Status active={vehicle.ativo} />
          </div>
        </div>

        {/* Galeria completa de fotos */}
        <div
          style={{
            marginTop: '0.5rem',
            borderRadius: '0.85rem',
            border: '1px solid var(--border, #334155)',
            overflow: 'hidden',
            background: 'rgba(15, 23, 42, 0.4)',
          }}
        >
          {fotos.length > 0 && activeFoto ? (
            <div>
              <div style={{ position: 'relative', height: '300px', background: '#0b1120' }}>
                <img
                  src={photoUrl(vehicle.id, activeFoto, revision)}
                  alt={activeFoto.descricao || `${vehicle.marca} ${vehicle.modelo}`}
                  style={{
                    width: '100%',
                    height: '100%',
                    objectFit: 'contain',
                    display: 'block',
                  }}
                />
                {fotos.length > 1 && (
                  <>
                    <button
                      type="button"
                      aria-label="Foto anterior"
                      onClick={() =>
                        setSelectedFotoIdx((idx) => (idx - 1 + fotos.length) % fotos.length)
                      }
                      style={{
                        position: 'absolute',
                        left: '0.6rem',
                        top: '50%',
                        transform: 'translateY(-50%)',
                        background: 'rgba(15, 23, 42, 0.75)',
                        color: '#fff',
                        border: '1px solid rgba(255,255,255,0.2)',
                        borderRadius: '999px',
                        width: '34px',
                        height: '34px',
                        display: 'grid',
                        placeItems: 'center',
                        cursor: 'pointer',
                      }}
                    >
                      <ChevronLeft size={18} />
                    </button>
                    <button
                      type="button"
                      aria-label="Próxima foto"
                      onClick={() => setSelectedFotoIdx((idx) => (idx + 1) % fotos.length)}
                      style={{
                        position: 'absolute',
                        right: '0.6rem',
                        top: '50%',
                        transform: 'translateY(-50%)',
                        background: 'rgba(15, 23, 42, 0.75)',
                        color: '#fff',
                        border: '1px solid rgba(255,255,255,0.2)',
                        borderRadius: '999px',
                        width: '34px',
                        height: '34px',
                        display: 'grid',
                        placeItems: 'center',
                        cursor: 'pointer',
                      }}
                    >
                      <ChevronRight size={18} />
                    </button>
                  </>
                )}
                <div
                  style={{
                    position: 'absolute',
                    bottom: '0.55rem',
                    left: '0.75rem',
                    right: '0.75rem',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '0.3rem 0.65rem',
                    borderRadius: '0.5rem',
                    background: 'rgba(15, 23, 42, 0.78)',
                    color: '#f8fafc',
                    fontSize: '0.78rem',
                  }}
                >
                  <span>
                    {activeFoto.principal ? '★ Foto principal' : 'Ângulo complementar'}
                    {activeFoto.descricao ? ` · ${activeFoto.descricao}` : ''}
                  </span>
                  <span>
                    {selectedFotoIdx + 1} de {fotos.length} fotos
                  </span>
                </div>
              </div>

              {/* Miniaturas de todas as fotos */}
              <div
                style={{
                  display: 'flex',
                  gap: '0.5rem',
                  padding: '0.65rem',
                  overflowX: 'auto',
                  alignItems: 'center',
                }}
              >
                {fotos.map((f, index) => (
                  <button
                    key={f.id ?? 'principal'}
                    type="button"
                    onClick={() => setSelectedFotoIdx(index)}
                    style={{
                      width: '68px',
                      height: '48px',
                      borderRadius: '0.45rem',
                      overflow: 'hidden',
                      border:
                        index === selectedFotoIdx
                          ? '2px solid #06b6d4'
                          : '1px solid rgba(148,163,184,0.3)',
                      padding: 0,
                      cursor: 'pointer',
                      flexShrink: 0,
                      background: '#0f172a',
                    }}
                  >
                    <img
                      src={photoUrl(vehicle.id, f, revision)}
                      alt={f.descricao || `Foto ${index + 1}`}
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />
                  </button>
                ))}
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={onOpenGallery}
                  style={{ marginLeft: 'auto', flexShrink: 0 }}
                >
                  <Camera size={14} />
                  Gerenciar fotos ({fotos.length})
                </Button>
              </div>
            </div>
          ) : (
            <div style={{ padding: '1.2rem', textAlign: 'center' }}>
              <div style={{ maxWidth: '280px', margin: '0 auto 0.75rem' }}>
                <VehicleImage
                  id={vehicle.id}
                  marca={vehicle.marca}
                  modelo={vehicle.modelo}
                  revision={revision}
                  large
                />
              </div>
              <Button type="button" variant="outline" size="sm" onClick={onOpenGallery}>
                <Camera size={14} />
                Adicionar fotos do veículo
              </Button>
            </div>
          )}
        </div>

        {/* Ficha Técnica, Pátio e Financeiro */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(230px, 1fr))',
            gap: '0.85rem',
            marginTop: '0.6rem',
          }}
        >
          <div
            style={{
              padding: '0.9rem',
              borderRadius: '0.75rem',
              border: '1px solid var(--border, #334155)',
              background: 'rgba(148, 163, 184, 0.05)',
              display: 'grid',
              gap: '0.42rem',
              fontSize: '0.86rem',
            }}
          >
            <strong style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <Clock size={15} /> Ficha Técnica & Pátio
            </strong>
            <div>
              <span className="muted">Marca / Modelo: </span>
              <strong>
                {vehicle.marca} {vehicle.modelo}
              </strong>
            </div>
            <div>
              <span className="muted">Ano / Condição: </span>
              <strong>
                {vehicle.ano} · {vehicle.zeroKm ? '0 km (Novo)' : `${number(vehicle.quilometragem)} km`}
              </strong>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
              <Calendar size={14} className="muted" />
              <span className="muted">Entrada no estoque: </span>
              <strong>
                {vehicle.dataCompra
                  ? new Date(vehicle.dataCompra).toLocaleDateString('pt-BR')
                  : '—'}
              </strong>
            </div>
            <div>
              <span className="muted">Tempo de pátio: </span>
              <strong>
                {vehicle.diasEmEstoque !== undefined
                  ? `${vehicle.diasEmEstoque} ${vehicle.diasEmEstoque === 1 ? 'dia' : 'dias'}`
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
              gap: '0.42rem',
              fontSize: '0.86rem',
            }}
          >
            <strong style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <Wallet size={15} /> Condições Financeiras & Margem
            </strong>
            <div>
              <span className="muted">Preço sugerido / anunciado: </span>
              <strong style={{ color: '#06b6d4' }}>
                {currency(vehicle.precoAnunciado ?? vehicle.venda ?? 0)}
              </strong>
            </div>
            <div>
              <span className="muted">Custo de aquisição: </span>
              <strong>
                {vehicle.compra != null
                  ? currency(vehicle.compra)
                  : 'Restrito (Visível para Gerente/Admin)'}
              </strong>
            </div>
            {margemBrutaProjetada !== undefined && (
              <div>
                <span className="muted">Margem bruta projetada: </span>
                <strong style={{ color: '#10b981' }}>
                  {currency(margemBrutaProjetada)} (
                  {margemPercentualSobreVenda?.toFixed(2)}% sobre venda)
                </strong>
              </div>
            )}
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.78rem' }} className="muted">
              <ShieldCheck size={13} />
              Política comercial da loja aplicada nas simulações.
            </div>
          </div>
        </div>

        {/* Histórico Comercial do Veículo (Propostas e Vendas) */}
        <div style={{ marginTop: '0.6rem', display: 'grid', gap: '0.75rem' }}>
          <div>
            <h4
              style={{
                margin: '0 0 0.4rem 0',
                fontSize: '0.92rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.4rem',
              }}
            >
              <FileSpreadsheet size={16} />
              Propostas Comerciais vinculadas a este veículo ({details.data?.propostas.length ?? 0})
            </h4>
            {details.loading ? (
              <p className="muted" style={{ fontSize: '0.84rem', margin: 0 }}>
                Carregando propostas…
              </p>
            ) : !details.data?.propostas.length ? (
              <p className="muted" style={{ fontSize: '0.84rem', margin: 0 }}>
                Nenhuma proposta registrada para este veículo até o momento.
              </p>
            ) : (
              <div style={{ display: 'grid', gap: '0.4rem', maxHeight: '140px', overflowY: 'auto' }}>
                {details.data.propostas.map((prop) => (
                  <div
                    key={prop.id}
                    style={{
                      padding: '0.55rem 0.75rem',
                      borderRadius: '0.55rem',
                      border: '1px solid var(--border, #334155)',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      fontSize: '0.83rem',
                    }}
                  >
                    <div>
                      <strong>{prop.numero}</strong> · Cliente: {prop.cliente.nome}
                      <div className="muted" style={{ fontSize: '0.76rem' }}>
                        Rev. #{prop.revisaoAtualSequencia} · Vendedor: {prop.vendedorIdentificador}
                      </div>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <strong>
                        {prop.revisaoAtual ? currency(prop.revisaoAtual.precoNegociado) : '—'}
                      </strong>
                      <div className="muted" style={{ fontSize: '0.76rem' }}>
                        {statusLabel(prop.status)}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {details.data?.vendas && details.data.vendas.length > 0 && (
            <div>
              <h4
                style={{
                  margin: '0 0 0.4rem 0',
                  fontSize: '0.92rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.4rem',
                }}
              >
                <Gauge size={16} />
                Registro de Venda deste Veículo
              </h4>
              {details.data.vendas.map((v) => (
                <div
                  key={v.id}
                  style={{
                    padding: '0.6rem 0.8rem',
                    borderRadius: '0.55rem',
                    border: '1px solid rgba(16, 185, 129, 0.35)',
                    background: 'rgba(16, 185, 129, 0.08)',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    fontSize: '0.84rem',
                  }}
                >
                  <div>
                    <strong>Venda #{v.id}</strong> · Comprador: <strong>{v.cliente.nome}</strong>
                    <div className="muted" style={{ fontSize: '0.76rem' }}>
                      Data: {new Date(v.dataVenda).toLocaleDateString('pt-BR')}{' '}
                      {v.observacoes ? `· ${v.observacoes}` : ''}
                    </div>
                  </div>
                  <strong>{currency(v.valorVenda)}</strong>
                </div>
              ))}
            </div>
          )}
        </div>

        <div
          className="form-footer"
          style={{ marginTop: '0.75rem', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.5rem' }}
        >
          <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
            <Button type="button" variant="outline" onClick={onEdit}>
              <Pencil size={15} />
              Editar veículo
            </Button>
            <Button type="button" variant="outline" onClick={onOpenGallery}>
              <Camera size={15} />
              Fotos ({fotos.length})
            </Button>
          </div>
          <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
            {vehicle.ativo && (
              <Button type="button" onClick={onSimulateProposal}>
                <FileSpreadsheet size={15} />
                Simular proposta para este veículo
              </Button>
            )}
            <Button type="button" variant="outline" onClick={onClose}>
              Fechar
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}

export function Status({ active }: { active: boolean }) {
  return (
    <span className={`status-badge ${active ? 'active' : 'inactive'}`}>
      <span />
      {active ? 'Ativo' : 'Inativo'}
    </span>
  );
}
