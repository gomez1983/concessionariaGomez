import { useEffect, useMemo, useState } from 'react';
import {
  ArrowDownLeft,
  ArrowUpRight,
  Banknote,
  CarFront,
  ChartNoAxesCombined,
  Check,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  CircleHelp,
  Gauge,
  LayoutDashboard,
  LayoutGrid,
  List,
  Menu,
  Pencil,
  Plus,
  Power,
  RotateCcw,
  Search,
  ShieldCheck,
  SlidersHorizontal,
  Sparkles,
  Tag,
  Trash2,
  Users,
  Wallet,
  X,
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { seed } from './data/seed';
import type { Carro, CarroInput, Filters, Sort } from './types';
import { currency, emptyFilters, filterCarros, number, parseStoredCarros } from './lib/estoque';
import { Button } from './components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogTitle } from './components/ui/dialog';
import { CarIllustration } from './components/CarIllustration';
import { VehicleForm } from './components/VehicleForm';

const STORAGE_KEY = 'gomez:estoque-demo:v1';
const PAGE_SIZE = 6;
type Confirm = { type: 'delete'; vehicle: Carro } | { type: 'reset' } | null;

export default function App() {
  const [carros, setCarros] = useState<Carro[]>(() => {
    try {
      return parseStoredCarros(localStorage.getItem(STORAGE_KEY)) ?? seed;
    } catch {
      return seed;
    }
  });
  const [storageError, setStorageError] = useState(false);
  const [section, setSection] = useState<'estoque' | 'overview'>('estoque');
  const [filters, setFilters] = useState<Filters>(emptyFilters);
  const [sort, setSort] = useState<Sort>('recentes');
  const [page, setPage] = useState(1);
  const [view, setView] = useState<'list' | 'grid'>('list');
  const [advanced, setAdvanced] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Carro | null>(null);
  const [confirm, setConfirm] = useState<Confirm>(null);
  const [infoOpen, setInfoOpen] = useState(false);
  const [mobileNav, setMobileNav] = useState(false);
  const [toast, setToast] = useState('');

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(carros));
      setStorageError(false);
    } catch {
      setStorageError(true);
    }
  }, [carros]);
  useEffect(() => {
    if (!toast) return;
    const timeout = window.setTimeout(() => setToast(''), 4500);
    return () => window.clearTimeout(timeout);
  }, [toast]);
  useEffect(() => {
    document.title = `${section === 'estoque' ? 'Estoque' : 'Visão geral'} · Gomez`;
  }, [section]);

  const results = useMemo(() => filterCarros(carros, filters, sort), [carros, filters, sort]);
  const totalPages = Math.max(1, Math.ceil(results.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const visible = results.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);
  const active = carros.filter((c) => c.ativo);
  const invested = active.reduce((sum, c) => sum + c.compra, 0);
  const brands = [...new Set(carros.map((c) => c.marca))].sort();
  const activeFilterCount = Object.values(filters).filter(Boolean).length;
  const invalidRange =
    (filters.minValor !== '' &&
      filters.maxValor !== '' &&
      Number(filters.minValor) > Number(filters.maxValor)) ||
    (filters.minKm !== '' && filters.maxKm !== '' && Number(filters.minKm) > Number(filters.maxKm));

  function changeFilter(key: keyof Filters, value: string) {
    setFilters((previous) => ({ ...previous, [key]: value }));
    setPage(1);
  }
  function clearFilters() {
    setFilters(emptyFilters);
    setPage(1);
  }
  function newVehicle() {
    setEditing(null);
    setFormOpen(true);
  }
  function editVehicle(vehicle: Carro) {
    setEditing(vehicle);
    setFormOpen(true);
  }
  function saveVehicle(input: CarroInput, id?: number) {
    setCarros((previous) =>
      id !== undefined
        ? previous.map((c) => (c.id === id ? { ...c, ...input } : c))
        : [
            {
              ...input,
              id: Math.max(0, ...previous.map((c) => c.id)) + 1,
              ativo: true,
              dataCompra: new Date().toISOString(),
            },
            ...previous,
          ],
    );
    setFormOpen(false);
    setToast(id !== undefined ? 'Dados do veículo atualizados.' : 'Veículo adicionado ao estoque.');
  }
  function toggleVehicle(vehicle: Carro) {
    setCarros((previous) =>
      previous.map((c) => (c.id === vehicle.id ? { ...c, ativo: !c.ativo } : c)),
    );
    setToast(`${vehicle.marca} ${vehicle.modelo} ${vehicle.ativo ? 'inativado' : 'ativado'}.`);
  }
  function confirmAction() {
    if (confirm?.type === 'delete') {
      setCarros((previous) => previous.filter((c) => c.id !== confirm.vehicle.id));
      setToast('Veículo excluído da demonstração.');
    } else if (confirm?.type === 'reset') {
      setCarros(seed);
      clearFilters();
      setToast('Dados de demonstração restaurados.');
    }
    setConfirm(null);
  }
  function navigate(next: typeof section) {
    setSection(next);
    setMobileNav(false);
  }

  return (
    <div className="app-shell">
      {mobileNav && (
        <button
          className="nav-backdrop"
          aria-label="Fechar navegação"
          onClick={() => setMobileNav(false)}
        />
      )}
      <aside className={`sidebar ${mobileNav ? 'is-open' : ''}`}>
        <a
          href="#estoque"
          className="brand"
          onClick={(event) => {
            event.preventDefault();
            navigate('estoque');
          }}
          aria-label="Gomez, ir para estoque"
        >
          <span className="brand-mark">
            g<span>.</span>
          </span>
          <span>
            gomez<span className="brand-subtitle">GESTÃO AUTOMOTIVA</span>
          </span>
        </a>
        <div className="workspace">
          <span className="workspace-icon">
            <CarFront size={20} />
          </span>
          <div>
            Concessionária Gomez<small>Seu espaço de gestão</small>
          </div>
          <ShieldCheck size={16} />
        </div>
        <span className="nav-caption">PRINCIPAL</span>
        <nav aria-label="Navegação principal">
          <button
            className={`nav-item ${section === 'overview' ? 'selected' : ''}`}
            onClick={() => navigate('overview')}
          >
            <LayoutDashboard size={19} />
            Visão geral
          </button>
          <button
            className={`nav-item ${section === 'estoque' ? 'selected' : ''}`}
            onClick={() => navigate('estoque')}
          >
            <CarFront size={20} />
            Estoque<span className="nav-count">{carros.length}</span>
          </button>
          <div className="nav-item planned" aria-disabled="true">
            <Users size={19} />
            Clientes<span className="soon">Em breve</span>
          </div>
          <div className="nav-item planned" aria-disabled="true">
            <Banknote size={19} />
            Vendas<span className="soon">Em breve</span>
          </div>
        </nav>
        <div className="sidebar-bottom">
          <div className="demo-card">
            <Sparkles size={20} />
            <strong>
              Do primeiro contato
              <br />à próxima conquista.
            </strong>
            <p>
              Uma nova forma de cuidar
              <br />
              do seu estoque.
            </p>
            <span>PROTÓTIPO INTERATIVO</span>
          </div>
          <button className="nav-item help" onClick={() => setInfoOpen(true)}>
            <CircleHelp size={18} />
            Sobre esta demonstração
            <ArrowUpRight size={15} />
          </button>
          <div className="profile">
            <span className="avatar">CG</span>
            <div>
              Concessionária Gomez<small>Ambiente de demonstração</small>
            </div>
            <span className="online-dot" />
          </div>
        </div>
      </aside>

      <div className="main-shell">
        <header className="topbar">
          <div className="breadcrumbs">
            <Button
              variant="ghost"
              size="icon"
              className="mobile-menu"
              aria-label="Abrir navegação"
              onClick={() => setMobileNav(true)}
            >
              <Menu size={21} />
            </Button>
            <span>Workspace</span>
            <ChevronRight size={13} />
            <strong>{section === 'estoque' ? 'Estoque' : 'Visão geral'}</strong>
          </div>
          <div className="topbar-right">
            <span className="demo-pill">
              <span />
              Dados de demonstração
            </span>
            <button
              className="top-avatar"
              onClick={() => setInfoOpen(true)}
              aria-label="Informações do ambiente"
            >
              CG
            </button>
          </div>
        </header>
        <main>
          <div className="page-heading">
            <div>
              <span className="eyebrow">CONCESSIONÁRIA GOMEZ</span>
              <h1>
                {section === 'estoque'
                  ? 'Seu estoque, sob controle.'
                  : 'Uma boa visão do seu negócio.'}
              </h1>
              <p>
                {section === 'estoque'
                  ? 'Cada veículo, uma nova oportunidade. Gerencie tudo por aqui.'
                  : 'Os principais números do estoque, em um só lugar.'}
              </p>
            </div>
            <Button onClick={newVehicle}>
              <Plus size={18} />
              Adicionar veículo
            </Button>
          </div>
          {storageError && (
            <div className="storage-alert" role="alert">
              O navegador não permitiu salvar os dados. As alterações durarão somente enquanto esta
              página estiver aberta.
            </div>
          )}
          <section className="stats-grid" aria-label="Indicadores de todo o estoque">
            <Stat
              label="Veículos no estoque"
              value={String(carros.length).padStart(2, '0')}
              description={`${active.length} ativos · ${carros.length - active.length} inativos`}
              icon={CarFront}
            />
            <Stat
              label="Investido em ativos"
              value={currency(invested)}
              description="Soma dos valores de compra"
              icon={Wallet}
            />
            <Stat
              label="Venda sugerida dos ativos"
              value={currency(invested * 1.1)}
              description="Simulação com acréscimo de 10%"
              icon={ChartNoAxesCombined}
              featured
            />
            <Stat
              label="Veículos zero km"
              value={String(carros.filter((c) => c.quilometragem === 0).length).padStart(2, '0')}
              description="Prontos para a primeira viagem"
              icon={Gauge}
            />
          </section>

          {section === 'overview' ? (
            <section className="overview-grid">
              <div className="panel brand-panel">
                <div className="panel-heading">
                  <div>
                    <h2>O perfil do estoque</h2>
                    <p>Distribuição de veículos por marca</p>
                  </div>
                  <Tag size={20} />
                </div>
                <div className="brand-bars">
                  {brands.length ? (
                    brands.map((marca) => {
                      const count = carros.filter((c) => c.marca === marca).length;
                      return (
                        <div key={marca} className="brand-bar">
                          <div>
                            <strong>{marca}</strong>
                            <span>{count} veículos</span>
                          </div>
                          <div className="bar-track">
                            <span
                              style={{ width: `${(count / Math.max(1, carros.length)) * 100}%` }}
                            />
                          </div>
                        </div>
                      );
                    })
                  ) : (
                    <p className="muted">Adicione veículos para ver a distribuição.</p>
                  )}
                </div>
              </div>
              <div className="overview-callout">
                <span className="callout-icon">
                  <ArrowUpRight size={30} />
                </span>
                <span className="eyebrow">PRÓXIMA PARADA</span>
                <h2>
                  Seu próximo negócio
                  <br />
                  começa no estoque.
                </h2>
                <p>
                  Consulte os veículos disponíveis, atualize informações e prepare cada
                  oportunidade.
                </p>
                <Button onClick={() => navigate('estoque')}>
                  Explorar estoque
                  <ArrowUpRight size={17} />
                </Button>
              </div>
            </section>
          ) : (
            <section className="panel stock-panel" aria-labelledby="inventory-title">
              <div className="panel-heading">
                <div className="inventory-title">
                  <h2 id="inventory-title">Veículos</h2>
                  <span className="count-pill">{results.length}</span>
                </div>
                <div className="view-switch" aria-label="Visualização">
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
                <div className="search-field">
                  <Search size={18} />
                  <input
                    aria-label="Buscar veículos"
                    placeholder="Buscar por marca, modelo ou ano..."
                    value={filters.search}
                    onChange={(event) => changeFilter('search', event.target.value)}
                  />
                  {filters.search && (
                    <button aria-label="Limpar busca" onClick={() => changeFilter('search', '')}>
                      <X size={15} />
                    </button>
                  )}
                </div>
                <label className="select-wrap">
                  <span className="sr-only">Filtrar por marca</span>
                  <select
                    value={filters.marca}
                    onChange={(event) => changeFilter('marca', event.target.value)}
                  >
                    <option value="">Todas as marcas</option>
                    {brands.map((marca) => (
                      <option key={marca}>{marca}</option>
                    ))}
                  </select>
                  <ChevronDown size={14} />
                </label>
                <label className="select-wrap">
                  <span className="sr-only">Filtrar por status</span>
                  <select
                    value={filters.ativo}
                    onChange={(event) => changeFilter('ativo', event.target.value)}
                  >
                    <option value="">Todos os status</option>
                    <option value="true">Ativos</option>
                    <option value="false">Inativos</option>
                  </select>
                  <ChevronDown size={14} />
                </label>
                <Button
                  variant="outline"
                  className={advanced ? 'filter-active' : ''}
                  onClick={() => setAdvanced(!advanced)}
                  aria-expanded={advanced}
                  aria-controls="advanced-filters"
                >
                  <SlidersHorizontal size={16} />
                  Filtros
                  {activeFilterCount > 0 && (
                    <span className="filter-count">{activeFilterCount}</span>
                  )}
                </Button>
              </div>
              {advanced && (
                <div className="advanced-filters" id="advanced-filters">
                  <label>
                    Ano
                    <input
                      aria-label="Filtrar por ano"
                      type="number"
                      min="1886"
                      max={new Date().getFullYear() + 1}
                      placeholder="Todos"
                      value={filters.ano}
                      onChange={(e) => changeFilter('ano', e.target.value)}
                    />
                  </label>
                  <label>
                    Compra mínima (R$)
                    <input
                      type="number"
                      min="0"
                      step="0.01"
                      placeholder="Sem mínimo"
                      value={filters.minValor}
                      onChange={(e) => changeFilter('minValor', e.target.value)}
                    />
                  </label>
                  <label>
                    Compra máxima (R$)
                    <input
                      type="number"
                      min="0"
                      step="0.01"
                      placeholder="Sem máximo"
                      value={filters.maxValor}
                      onChange={(e) => changeFilter('maxValor', e.target.value)}
                    />
                  </label>
                  <label>
                    Km mínimo
                    <input
                      type="number"
                      min="0"
                      placeholder="Sem mínimo"
                      value={filters.minKm}
                      onChange={(e) => changeFilter('minKm', e.target.value)}
                    />
                  </label>
                  <label>
                    Km máximo
                    <input
                      type="number"
                      min="0"
                      placeholder="Sem máximo"
                      value={filters.maxKm}
                      onChange={(e) => changeFilter('maxKm', e.target.value)}
                    />
                  </label>
                  {invalidRange && (
                    <p className="form-error range-error" role="alert">
                      O mínimo deve ser menor ou igual ao máximo.
                    </p>
                  )}
                </div>
              )}
              <div className="results-toolbar">
                <span>
                  {activeFilterCount
                    ? `${results.length} ${results.length === 1 ? 'veículo encontrado' : 'veículos encontrados'}`
                    : 'Uma seleção de novas possibilidades'}
                  {activeFilterCount > 0 && (
                    <button onClick={clearFilters}>
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
                    onChange={(e) => {
                      setSort(e.target.value as Sort);
                      setPage(1);
                    }}
                  >
                    <option value="recentes">Mais recentes</option>
                    <option value="valor-asc">Menor valor de compra</option>
                    <option value="valor-desc">Maior valor de compra</option>
                    <option value="ano-desc">Ano mais novo</option>
                  </select>
                </label>
              </div>
              {!results.length ? (
                <div className="empty-state">
                  <Search size={32} />
                  <h3>{carros.length ? 'Nenhum veículo por aqui.' : 'Seu estoque começa aqui.'}</h3>
                  <p>
                    {carros.length
                      ? 'Experimente outros termos ou ajuste os filtros.'
                      : 'Adicione o primeiro veículo à demonstração.'}
                  </p>
                  <Button variant="outline" onClick={carros.length ? clearFilters : newVehicle}>
                    {carros.length ? 'Limpar filtros' : 'Adicionar veículo'}
                  </Button>
                </div>
              ) : view === 'list' ? (
                <div className="table-scroll">
                  <table>
                    <thead>
                      <tr>
                        <th>VEÍCULO</th>
                        <th>ANO / KM</th>
                        <th>VALOR DE COMPRA</th>
                        <th>STATUS</th>
                        <th>
                          <span className="sr-only">Ações</span>
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {visible.map((vehicle) => (
                        <tr key={vehicle.id}>
                          <td>
                            <div className="vehicle-cell">
                              <div className="vehicle-thumb">
                                <CarIllustration marca={vehicle.marca} />
                              </div>
                              <div>
                                <button
                                  className="vehicle-name"
                                  onClick={() => editVehicle(vehicle)}
                                >
                                  {vehicle.marca} {vehicle.modelo}
                                </button>
                                <span className="vehicle-subtitle">
                                  {vehicle.quilometragem === 0
                                    ? 'Novo · Zero km'
                                    : 'Seminovo / usado'}
                                </span>
                              </div>
                            </div>
                          </td>
                          <td>
                            <strong className="table-year">{vehicle.ano}</strong>
                            <span className="vehicle-subtitle">
                              {number(vehicle.quilometragem)} km
                            </span>
                          </td>
                          <td>
                            <strong className="table-money">{currency(vehicle.compra)}</strong>
                            <span className="vehicle-subtitle">Valor de aquisição</span>
                          </td>
                          <td>
                            <Status active={vehicle.ativo} />
                          </td>
                          <td>
                            <VehicleActions
                              vehicle={vehicle}
                              onEdit={editVehicle}
                              onToggle={toggleVehicle}
                              onDelete={(vehicle) => setConfirm({ type: 'delete', vehicle })}
                            />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="vehicle-grid">
                  {visible.map((vehicle) => (
                    <article className="vehicle-card" key={vehicle.id}>
                      <div className="card-art">
                        <Status active={vehicle.ativo} />
                        <CarIllustration marca={vehicle.marca} large />
                      </div>
                      <div className="card-body">
                        <small>{vehicle.marca}</small>
                        <button className="vehicle-name" onClick={() => editVehicle(vehicle)}>
                          {vehicle.modelo}
                        </button>
                        <p>
                          {vehicle.ano}
                          <span>·</span>
                          {number(vehicle.quilometragem)} km
                          {vehicle.quilometragem === 0 && <span className="zero-km">Zero km</span>}
                        </p>
                        <div className="card-price">
                          <div>
                            <small>Valor de compra</small>
                            <strong>{currency(vehicle.compra)}</strong>
                          </div>
                          <VehicleActions
                            vehicle={vehicle}
                            onEdit={editVehicle}
                            onToggle={toggleVehicle}
                            onDelete={(vehicle) => setConfirm({ type: 'delete', vehicle })}
                          />
                        </div>
                      </div>
                    </article>
                  ))}
                </div>
              )}
              <div className="pagination">
                <span>
                  {results.length
                    ? `Mostrando ${(currentPage - 1) * PAGE_SIZE + 1}–${Math.min(currentPage * PAGE_SIZE, results.length)} de ${results.length} veículos`
                    : '0 veículos'}
                </span>
                <div>
                  <Button
                    variant="outline"
                    size="sm"
                    aria-label="Página anterior"
                    disabled={currentPage === 1}
                    onClick={() => setPage(currentPage - 1)}
                  >
                    <ChevronLeft size={16} />
                  </Button>
                  {Array.from({ length: totalPages }, (_, i) => i + 1)
                    .filter((p) => p === 1 || p === totalPages || Math.abs(p - currentPage) <= 1)
                    .map((p, index, pages) => (
                      <span key={p} className="page-item">
                        {index > 0 && p - pages[index - 1] > 1 && (
                          <span className="page-gap">…</span>
                        )}
                        <Button
                          variant={p === currentPage ? 'default' : 'ghost'}
                          size="sm"
                          aria-label={`Página ${p}`}
                          aria-current={p === currentPage ? 'page' : undefined}
                          onClick={() => setPage(p)}
                        >
                          {p}
                        </Button>
                      </span>
                    ))}
                  <Button
                    variant="outline"
                    size="sm"
                    aria-label="Próxima página"
                    disabled={currentPage === totalPages}
                    onClick={() => setPage(currentPage + 1)}
                  >
                    <ChevronRight size={16} />
                  </Button>
                </div>
              </div>
            </section>
          )}
          <footer className="page-footer">
            <span>
              <ShieldCheck size={14} />
              Demonstração local · sem conexão com a API
            </span>
            <button onClick={() => setConfirm({ type: 'reset' })}>
              <RotateCcw size={13} />
              Restaurar dados de exemplo
            </button>
          </footer>
        </main>
      </div>
      {formOpen && (
        <VehicleForm
          key={editing?.id ?? 'new'}
          vehicle={editing}
          open={formOpen}
          onClose={() => setFormOpen(false)}
          onSave={saveVehicle}
        />
      )}
      <Dialog
        open={confirm !== null}
        onOpenChange={(open) => {
          if (!open) setConfirm(null);
        }}
      >
        <DialogContent className="confirm-dialog">
          <div className="dialog-symbol danger">
            {confirm?.type === 'delete' ? <Trash2 size={24} /> : <RotateCcw size={24} />}
          </div>
          <DialogTitle className="dialog-title">
            {confirm?.type === 'delete' ? 'Excluir este veículo?' : 'Começar de novo?'}
          </DialogTitle>
          <DialogDescription className="muted">
            {confirm?.type === 'delete'
              ? `${confirm.vehicle.marca} ${confirm.vehicle.modelo} será removido dos dados locais. Essa ação não pode ser desfeita.`
              : 'Os cadastros e alterações desta demonstração serão substituídos pelos 14 veículos de exemplo.'}
          </DialogDescription>
          <div className="form-footer">
            <Button variant="outline" onClick={() => setConfirm(null)}>
              Cancelar
            </Button>
            <Button variant="destructive" onClick={confirmAction}>
              {confirm?.type === 'delete' ? 'Excluir veículo' : 'Restaurar exemplos'}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
      <Dialog open={infoOpen} onOpenChange={setInfoOpen}>
        <DialogContent>
          <div className="dialog-symbol">
            <Sparkles size={24} />
          </div>
          <DialogTitle className="dialog-title">Um primeiro olhar para o futuro.</DialogTitle>
          <DialogDescription className="muted">
            Este é um protótipo funcional do painel da Concessionária Gomez.
          </DialogDescription>
          <div className="info-copy">
            <p>
              Você pode buscar, filtrar, cadastrar, editar, ativar, inativar e excluir veículos. Os
              dados ficam apenas neste navegador.
            </p>
            <p>
              Os indicadores consideram todo o estoque. Os valores de venda são simulações; na
              integração definitiva, os cálculos virão do domínio Java.
            </p>
            <p>
              Clientes, vendas e fotos reais ficam para as próximas etapas. As imagens dos veículos
              são ilustrações genéricas.
            </p>
          </div>
          <Button onClick={() => setInfoOpen(false)}>
            Entendi
            <Check size={17} />
          </Button>
        </DialogContent>
      </Dialog>
      {toast && (
        <div className="toast" role="status">
          <span>
            <Check size={17} />
          </span>
          {toast}
          <button aria-label="Dispensar mensagem" onClick={() => setToast('')}>
            <X size={15} />
          </button>
        </div>
      )}
    </div>
  );
}

function Stat({
  label,
  value,
  description,
  icon: Icon,
  featured = false,
}: {
  label: string;
  value: string;
  description: string;
  icon: LucideIcon;
  featured?: boolean;
}) {
  return (
    <article className={`stat-card ${featured ? 'featured' : ''}`}>
      <div className="stat-label">
        <span>{label}</span>
        <Icon size={19} />
      </div>
      <strong className="stat-value">{value}</strong>
      <span className="stat-description">
        {featured ? (
          <ArrowUpRight size={13} />
        ) : label === 'Investido em ativos' ? (
          <ArrowDownLeft size={13} />
        ) : (
          <span className="stat-dot" />
        )}
        {description}
      </span>
    </article>
  );
}
function Status({ active }: { active: boolean }) {
  return (
    <span className={`status-badge ${active ? 'active' : 'inactive'}`}>
      <span />
      {active ? 'Ativo' : 'Inativo'}
    </span>
  );
}
function VehicleActions({
  vehicle,
  onEdit,
  onToggle,
  onDelete,
}: {
  vehicle: Carro;
  onEdit: (vehicle: Carro) => void;
  onToggle: (vehicle: Carro) => void;
  onDelete: (vehicle: Carro) => void;
}) {
  return (
    <div className="vehicle-actions">
      <Button
        size="icon"
        variant="ghost"
        title="Editar veículo"
        aria-label={`Editar ${vehicle.marca} ${vehicle.modelo}`}
        onClick={() => onEdit(vehicle)}
      >
        <Pencil size={15} />
      </Button>
      <Button
        size="icon"
        variant="ghost"
        title={vehicle.ativo ? 'Inativar veículo' : 'Ativar veículo'}
        aria-label={`${vehicle.ativo ? 'Inativar' : 'Ativar'} ${vehicle.marca} ${vehicle.modelo}`}
        onClick={() => onToggle(vehicle)}
      >
        <Power size={15} />
      </Button>
      <Button
        size="icon"
        variant="ghost"
        className="delete-action"
        title="Excluir veículo"
        aria-label={`Excluir ${vehicle.marca} ${vehicle.modelo}`}
        onClick={() => onDelete(vehicle)}
      >
        <Trash2 size={15} />
      </Button>
    </div>
  );
}
