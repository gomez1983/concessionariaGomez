import { useEffect, useState } from 'react';
import {
  ArrowUpRight,
  Banknote,
  Camera,
  CarFront,
  ChartNoAxesCombined,
  Check,
  ChevronRight,
  CircleHelp,
  FileSpreadsheet,
  Gauge,
  LayoutDashboard,
  Menu,
  Plus,
  RefreshCw,
  ShieldCheck,
  Sparkles,
  Tag,
  Users,
  Wallet,
  X,
  Sun,
  Moon,
  Search,
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { api, getOperadorContext, setOperadorContext } from './lib/api';
import { currency } from './lib/estoque';
import { useResource } from './lib/useResource';
import { Button } from './components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogTitle } from './components/ui/dialog';
import { ErrorState, Loading } from './components/Feedback';
import { Stock } from './components/Stock';
import { Customers } from './components/Customers';
import { Proposals } from './components/Proposals';
import { Sales } from './components/Sales';
import { useTheme } from './lib/useTheme';
import type { PerfilOperador } from './types';

type Section = 'estoque' | 'overview' | 'clientes' | 'propostas' | 'vendas';
const sections: Record<
  Section,
  { label: string; title: string; description: string; action: string; icon: LucideIcon }
> = {
  overview: {
    label: 'Visão geral',
    title: 'Uma boa visão do seu negócio.',
    description: 'Os principais números do estoque e do funil comercial, em um só lugar.',
    action: 'Adicionar veículo',
    icon: LayoutDashboard,
  },
  estoque: {
    label: 'Estoque',
    title: 'Seu estoque, sob controle.',
    description: 'Cada veículo, uma nova oportunidade. Gerencie tudo por aqui.',
    action: 'Adicionar veículo',
    icon: CarFront,
  },
  clientes: {
    label: 'Clientes',
    title: 'Bons negócios começam com pessoas.',
    description: 'Contatos, cadastros e relacionamentos da sua concessionária.',
    action: 'Adicionar cliente',
    icon: Users,
  },
  propostas: {
    label: 'Propostas',
    title: 'Negociações estruturadas, margem protegida.',
    description: 'Simule descontos, avalie trocas, aprove alçadas e converta propostas em vendas.',
    action: 'Nova proposta',
    icon: FileSpreadsheet,
  },
  vendas: {
    label: 'Vendas',
    title: 'Cada venda, uma nova conquista.',
    description: 'Registre negócios e acompanhe as vendas realizadas.',
    action: 'Registrar venda',
    icon: Banknote,
  },
};
export default function App() {
  const { theme, toggleTheme } = useTheme();
  const [search, setSearch] = useState('');
  const [section, setSection] = useState<Section>('estoque');
  const [revision, setRevision] = useState(0);
  const [formRequested, setFormRequested] = useState(false);
  const [mobileNav, setMobileNav] = useState(false);
  const [infoOpen, setInfoOpen] = useState(false);
  const [toast, setToast] = useState('');
  const [perfil, setPerfil] = useState<PerfilOperador>(() => getOperadorContext().perfil);
  const dashboard = useResource(api.dashboard, [revision, perfil]);
  const summary = dashboard.data;
  const config = sections[section];
  function changed(message?: string) {
    setRevision((value) => value + 1);
    if (message) setToast(message);
  }
  function handlePerfilChange(nextPerfil: PerfilOperador) {
    setOperadorContext(nextPerfil);
    setPerfil(nextPerfil);
    setRevision((value) => value + 1);
  }
  function navigate(next: Section) {
    setSection(next);
    setFormRequested(false);
    setMobileNav(false);
  }
  useEffect(() => {
    document.title = `${config.label} · Gomez`;
  }, [config.label]);
  useEffect(() => {
    if (!toast) return;
    const timeout = window.setTimeout(() => setToast(''), 5000);
    return () => clearTimeout(timeout);
  }, [toast]);
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
          aria-label="Gomez, ir para estoque"
          onClick={(event) => {
            event.preventDefault();
            navigate('estoque');
          }}
        >
          <span className="brand-mark">
            <CarFront size={27} />
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
          {(Object.keys(sections) as Section[]).map((key) => {
            const Icon = sections[key].icon;
            return (
              <button
                key={key}
                className={`nav-item ${section === key ? 'selected' : ''}`}
                aria-current={section === key ? 'page' : undefined}
                aria-label={sections[key].label}
                title={sections[key].label}
                onClick={() => navigate(key)}
              >
                <Icon size={19} />
                <span className="nav-label">{sections[key].label}</span>
                {key === 'estoque' && summary && (
                  <span className="nav-count">{summary.totalVeiculos}</span>
                )}
                {key === 'propostas' &&
                  summary &&
                  (summary.propostasPendentesAprovacao ?? 0) > 0 && (
                    <span className="nav-count">{summary.propostasPendentesAprovacao}</span>
                  )}
              </button>
            );
          })}
        </nav>
        <div className="sidebar-bottom">
          <div className="demo-card">
            <Camera size={22} />
            <strong>
              Cada ângulo conta
              <br />
              uma história.
            </strong>
            <p>
              Organize fotos e destaque
              <br />o melhor de cada veículo.
            </p>
            <span>CATÁLOGO CONECTADO</span>
          </div>
          <button
            className="nav-item help"
            aria-label="Sobre o painel"
            title="Sobre o painel"
            onClick={() => setInfoOpen(true)}
          >
            <CircleHelp size={18} />
            <span className="nav-label">Sobre o painel</span>
            <ArrowUpRight size={15} />
          </button>
          <div className="profile">
            <span className="avatar">CG</span>
            <div>
              Concessionária Gomez<small>Perfil: {perfil}</small>
            </div>
            <span className="online-dot" />
          </div>
        </div>
      </aside>
      <div className="main-shell">
        <header className="topbar">
          <a
            className="topbar-brand"
            href="#estoque"
            onClick={(event) => {
              event.preventDefault();
              navigate('estoque');
            }}
          >
            <CarFront size={34} />
            <span>
              Concessionária<strong>Gomez</strong>
            </span>
          </a>
          <div className="breadcrumbs">
            <Button
              className="mobile-menu"
              variant="ghost"
              size="icon"
              aria-label="Abrir navegação"
              onClick={() => setMobileNav(true)}
            >
              <Menu size={21} />
            </Button>
            <span>Workspace</span>
            <ChevronRight size={13} />
            <strong>{config.label}</strong>
          </div>
          <div className="global-search">
            <Search size={19} />
            <input
              aria-label="Buscar veículo por modelo"
              value={search}
              placeholder="Buscar um veículo..."
              onChange={(event) => {
                if (section !== 'estoque') navigate('estoque');
                setSearch(event.target.value);
              }}
            />
            {search && (
              <button aria-label="Limpar busca" onClick={() => setSearch('')}>
                <X size={15} />
              </button>
            )}
          </div>
          <div className="topbar-right">
            <label
              style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 12 }}
              title="Alternar perfil operacional (Alçada Comercial)"
            >
              <span className="muted">Perfil:</span>
              <select
                aria-label="Perfil do operador comercial"
                value={perfil}
                onChange={(e) => handlePerfilChange(e.target.value as PerfilOperador)}
                style={{
                  padding: '4px 8px',
                  borderRadius: 6,
                  border: '1px solid var(--border, #334155)',
                  background: 'transparent',
                  color: 'inherit',
                  fontSize: 12,
                  fontWeight: 600,
                }}
              >
                <option value="VENDEDOR">Vendedor</option>
                <option value="GERENTE">Gerente</option>
                <option value="ADMIN">Admin</option>
              </select>
            </label>
            <Button
              variant="ghost"
              size="icon"
              className="theme-toggle"
              onClick={toggleTheme}
              title={theme === 'dark' ? 'Ativar modo claro' : 'Ativar modo escuro'}
              aria-label={theme === 'dark' ? 'Ativar modo claro' : 'Ativar modo escuro'}
            >
              {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
            </Button>
            <span className={`connection-pill ${dashboard.error ? 'offline' : ''}`}>
              <span />
              {dashboard.loading
                ? 'Atualizando…'
                : dashboard.error
                  ? 'API indisponível'
                  : 'Conectado à API'}
            </span>
            <Button
              size="icon"
              variant="ghost"
              aria-label="Atualizar dados"
              title="Atualizar dados"
              onClick={() => changed()}
            >
              <RefreshCw size={16} />
            </Button>
            <button
              className="top-avatar"
              onClick={() => setInfoOpen(true)}
              aria-label="Sobre o painel"
            >
              CG
            </button>
          </div>
        </header>
        <main>
          <div className="page-heading">
            <div>
              <span className="eyebrow">CONCESSIONÁRIA GOMEZ</span>
              <h1>{config.title}</h1>
              <p>{config.description}</p>
            </div>
            <Button
              onClick={() => {
                if (section === 'overview') setSection('estoque');
                setFormRequested(true);
              }}
            >
              <Plus size={18} />
              {config.action}
            </Button>
          </div>
          {(section === 'estoque' || section === 'overview') && (
            <>
              {dashboard.error ? (
                <div className="dashboard-error">
                  <ErrorState message={dashboard.error} retry={dashboard.reload} />
                </div>
              ) : (
                <section className="stats-grid" aria-label="Indicadores de todo o estoque">
                  <Stat
                    label="Veículos no estoque"
                    value={summary ? String(summary.totalVeiculos).padStart(2, '0') : '—'}
                    description={
                      summary
                        ? `${summary.ativos} ativos · ${summary.totalVeiculos - summary.ativos} inativos`
                        : 'Consultando estoque'
                    }
                    icon={CarFront}
                  />
                  <Stat
                    label="Investido em ativos"
                    value={
                      summary
                        ? summary.valorInvestido != null
                          ? currency(summary.valorInvestido)
                          : 'Restrito'
                        : '—'
                    }
                    description={
                      summary?.valorInvestido != null
                        ? 'Soma dos valores de compra'
                        : 'Visível para Gerente/Admin'
                    }
                    icon={Wallet}
                  />
                  <Stat
                    label="Venda sugerida dos ativos"
                    value={summary ? currency(summary.vendaSugerida) : '—'}
                    description="Estimativa calculada pelo domínio"
                    icon={ChartNoAxesCombined}
                    featured
                  />
                  <Stat
                    label="Margem prevista"
                    value={
                      summary
                        ? summary.margemPrevista != null
                          ? currency(summary.margemPrevista)
                          : 'Restrito'
                        : '—'
                    }
                    description={
                      summary?.margemPrevista != null
                        ? 'Projeção sobre os veículos ativos'
                        : 'Visível para Gerente/Admin'
                    }
                    icon={Gauge}
                  />
                </section>
              )}
            </>
          )}
          {section === 'overview' &&
            (dashboard.loading ? (
              <Loading />
            ) : (
              summary &&
              !dashboard.error && (
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
                      {Object.entries(summary.marcas).map(([marca, count]) => (
                        <div className="brand-bar" key={marca}>
                          <div>
                            <strong>{marca}</strong>
                            <span>{count} veículos</span>
                          </div>
                          <div className="bar-track">
                            <span
                              style={{
                                width: `${(count / Math.max(1, summary.totalVeiculos)) * 100}%`,
                              }}
                            />
                          </div>
                        </div>
                      ))}
                      {!summary.totalVeiculos && (
                        <p className="muted">Cadastre veículos para visualizar a distribuição.</p>
                      )}
                    </div>
                  </div>
                  <div className="overview-callout">
                    <span className="callout-icon">
                      <ArrowUpRight size={30} />
                    </span>
                    <span className="eyebrow">FUNIL COMERCIAL</span>
                    <h2>
                      {summary.propostasEmNegociacao ?? 0} em negociação ·{' '}
                      {summary.propostasConvertidas ?? 0} convertidas
                    </h2>
                    <p>
                      Aguardando aprovação gerencial: {summary.propostasPendentesAprovacao ?? 0}.
                      Faturamento realizado: {currency(summary.faturamentoRealizado ?? 0)}.
                    </p>
                    <Button onClick={() => navigate('propostas')}>
                      Abrir mesa de propostas
                      <ArrowUpRight size={17} />
                    </Button>
                  </div>
                </section>
              )
            ))}
          {section === 'estoque' && (
            <Stock
              revision={revision}
              search={search}
              onSearchChange={setSearch}
              brands={Object.keys(summary?.marcas ?? {})}
              onChanged={changed}
              formRequested={formRequested}
              closeForm={() => setFormRequested(false)}
            />
          )}
          {section === 'clientes' && (
            <Customers
              revision={revision}
              onChanged={changed}
              formRequested={formRequested}
              closeForm={() => setFormRequested(false)}
            />
          )}
          {section === 'propostas' && (
            <Proposals
              revision={revision}
              perfil={perfil}
              onChanged={changed}
              formRequested={formRequested}
              closeForm={() => setFormRequested(false)}
            />
          )}
          {section === 'vendas' && (
            <Sales
              revision={revision}
              onChanged={changed}
              formRequested={formRequested}
              closeForm={() => setFormRequested(false)}
            />
          )}
          <footer className="page-footer">
            <span>
              <ShieldCheck size={14} />
              Dados persistidos pela API · Concessionária Gomez
            </span>
            <a href="/creditos-fotos.html" target="_blank" rel="noreferrer">
              Créditos das fotos de referência
            </a>
          </footer>
        </main>
      </div>
      <Dialog open={infoOpen} onOpenChange={setInfoOpen}>
        <DialogContent>
          <div className="dialog-symbol">
            <Sparkles size={24} />
          </div>
          <DialogTitle className="dialog-title">Seu negócio, conectado.</DialogTitle>
          <DialogDescription className="muted">
            Painel de gestão da Concessionária Gomez.
          </DialogDescription>
          <div className="info-copy">
            <p>
              Estoque, clientes, propostas comerciais e vendas são consultados e gravados na API. O
              banco de dados mantém os cadastros, as revisões imutáveis e os arquivos das fotos.
            </p>
            <p>
              Abra a mesa de propostas para simular descontos, avaliar trocas com quitação, submeter
              para alçada gerencial e converter em venda com reconciliação financeira completa.
            </p>
            <p>
              O preço sugerido e os indicadores financeiros vêm do domínio Java. Custos e margens
              internas são protegidos conforme o perfil selecionado (Vendedor, Gerente ou Admin).
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
        <span className="stat-dot" />
        {description}
      </span>
    </article>
  );
}
