import { useEffect, useState } from 'react';
import {
  Camera,
  ChevronDown,
  LayoutGrid,
  List,
  Pencil,
  Plus,
  Power,
  Search,
  SlidersHorizontal,
  Trash2,
  X,
} from 'lucide-react';
import type { Carro, Filters, Sort } from '../types';
import { api, carroQuery } from '../lib/api';
import { currency, emptyFilters, number } from '../lib/estoque';
import { useResource } from '../lib/useResource';
import { Button } from './ui/button';
import { VehicleForm } from './VehicleForm';
import { VehicleImage } from './VehicleImage';
import { VehicleGallery } from './VehicleGallery';
import { ConfirmAction, type Confirmation } from './ConfirmAction';
import { ErrorState, Loading } from './Feedback';
import { Pagination } from './Pagination';
import { BrandLogo } from './BrandLogo';
import { MoneyInput } from './ui/money-input';

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
  const [editing, setEditing] = useState<Carro | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [gallery, setGallery] = useState<Carro | null>(null);
  const [confirmation, setConfirmation] = useState<Confirmation | null>(null);
  const [busyId, setBusyId] = useState<number>();
  const [actionError, setActionError] = useState('');
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
    <div className="vehicle-actions">
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
              : 'Estoque atualizado pela API'}
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
              <article className="vehicle-card" key={vehicle.id}>
                <button
                  className="card-art photo-trigger"
                  aria-label={`Abrir galeria de ${vehicle.marca} ${vehicle.modelo}`}
                  onClick={() => setGallery(vehicle)}
                >
                  <VehicleImage
                    id={vehicle.id}
                    marca={vehicle.marca}
                    modelo={vehicle.modelo}
                    revision={revision}
                    large
                  />
                  <span className="photo-overlay">
                    <Camera size={15} />
                    Ver fotos
                  </span>
                </button>
                <div className="card-body">
                  <div className="card-name-line">
                    <BrandLogo marca={vehicle.marca} />
                    <button
                      className="vehicle-name"
                      onClick={() => edit(vehicle)}
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
                    <span>Compra: {currency(vehicle.compra)}</span>
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
                  <tr key={vehicle.id}>
                    <td>
                      <div className="vehicle-cell">
                        <button
                          className="vehicle-thumb photo-trigger"
                          aria-label={`Abrir galeria de ${vehicle.marca} ${vehicle.modelo}`}
                          onClick={() => setGallery(vehicle)}
                        >
                          <VehicleImage
                            id={vehicle.id}
                            marca={vehicle.marca}
                            modelo={vehicle.modelo}
                            revision={revision}
                          />
                        </button>
                        <button className="vehicle-name" onClick={() => edit(vehicle)}>
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
                      {currency(vehicle.compra)}
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
export function Status({ active }: { active: boolean }) {
  return (
    <span className={`status-badge ${active ? 'active' : 'inactive'}`}>
      <span />
      {active ? 'Ativo' : 'Inativo'}
    </span>
  );
}
