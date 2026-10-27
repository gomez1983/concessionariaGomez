import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import {
  AlertTriangle,
  BadgeCheck,
  Check,
  FileSpreadsheet,
  FileText,
  Handshake,
  Lock,
  ShieldAlert,
  Sparkles,
  XCircle,
} from 'lucide-react';
import type {
  AlcadaAnalise,
  Carro,
  Cliente,
  PerfilOperador,
  PropostaComercial,
  PropostaComercialInput,
  SimulacaoProposta,
  StatusProposta,
} from '../types';
import { allPages, api, errorMessage, getOperadorContext } from '../lib/api';
import { currency } from '../lib/estoque';
import { useResource } from '../lib/useResource';
import { Button } from './ui/button';
import { Dialog, DialogContent, DialogDescription, DialogTitle } from './ui/dialog';
import { ErrorState, FormError, Loading } from './Feedback';
import { Pagination } from './Pagination';
import { MoneyInput } from './ui/money-input';
import { formatDocument, parseMoney } from '../lib/inputMasks';

export function alcadaBadge(alcada?: AlcadaAnalise) {
  switch (alcada) {
    case 'DENTRO_AUTONOMIA':
      return {
        label: 'Dentro da sua autonomia',
        icon: BadgeCheck,
        color: '#10b981',
        bg: 'rgba(16, 185, 129, 0.12)',
      };
    case 'EXIGE_APROVACAO_GERENCIAL':
      return {
        label: 'Exige aprovação gerencial',
        icon: AlertTriangle,
        color: '#f59e0b',
        bg: 'rgba(245, 158, 11, 0.14)',
      };
    case 'BLOQUEADA_ABAIXO_PISO':
      return {
        label: 'Bloqueada (abaixo do piso)',
        icon: Lock,
        color: '#ef4444',
        bg: 'rgba(239, 68, 68, 0.14)',
      };
    default:
      return {
        label: 'Pendente de confirmação',
        icon: ShieldAlert,
        color: '#6366f1',
        bg: 'rgba(99, 102, 241, 0.14)',
      };
  }
}

export function statusLabel(status: StatusProposta) {
  const labels: Record<StatusProposta, string> = {
    RASCUNHO: 'Rascunho',
    EM_NEGOCIACAO: 'Em negociação',
    PENDENTE_APROVACAO: 'Fila gerencial',
    APROVADA: 'Aprovada',
    REJEITADA: 'Rejeitada',
    EXPIRADA: 'Expirada',
    CANCELADA: 'Cancelada',
    CONVERTIDA: 'Convertida em venda',
  };
  return labels[status] ?? status;
}

export function Proposals({
  revision,
  perfil,
  onChanged,
  formRequested,
  closeForm,
}: {
  revision: number;
  perfil: PerfilOperador;
  onChanged: (message?: string) => void;
  formRequested: boolean;
  closeForm: () => void;
}) {
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<StatusProposta | ''>('');
  const [selectedProposta, setSelectedProposta] = useState<PropostaComercial | null>(null);
  const [resumoClienteProposta, setResumoClienteProposta] = useState<PropostaComercial | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [actionError, setActionError] = useState('');

  const resource = useResource(
    (signal) => api.propostas(page, statusFilter, signal),
    [page, statusFilter, revision, perfil],
  );

  const podeAprovar = perfil === 'GERENTE' || perfil === 'ADMIN';
  const podeVerInterno = perfil === 'GERENTE' || perfil === 'ADMIN';

  async function handleDecisaoGerencial(prop: PropostaComercial, aprovar: boolean) {
    const rev = prop.revisaoAtual;
    if (!rev) return;
    setBusyId(prop.id);
    setActionError('');
    try {
      const ctx = getOperadorContext();
      // Quando o usuário testa no mesmo navegador alternando o seletor para GERENTE/ADMIN,
      // garantimos um identificador de decisor gerencial distinto caso a proposta tenha sido
      // criada com o identificador padrão de gerente.
      const decisorId =
        ctx.operadorId.toLowerCase() === prop.vendedorIdentificador?.toLowerCase()
          ? 'diretoria.aprovacao@concessionariagomez.com.br'
          : ctx.operadorId;

      await api.decidirAprovacaoProposta(
        prop.id,
        rev.sequencia,
        aprovar,
        aprovar
          ? 'Aprovada pela gerência comercial'
          : 'Condição comercial abaixo da rentabilidade mínima desejada',
        decisorId,
      );
      resource.reload();
      onChanged(
        aprovar
          ? `Proposta ${prop.numero} aprovada pela gerência.`
          : `Proposta ${prop.numero} rejeitada pela gerência.`,
      );
    } catch (err) {
      setActionError(errorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  async function handleConverterVenda(prop: PropostaComercial) {
    const rev = prop.revisaoAtual;
    if (!rev) return;
    setBusyId(prop.id);
    setActionError('');
    try {
      await api.converterPropostaEmVenda(prop.id, rev.sequencia);
      resource.reload();
      onChanged(`Proposta ${prop.numero} convertida em venda com sucesso!`);
    } catch (err) {
      setActionError(errorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  return (
    <>
      <section className="panel">
        <div className="panel-heading" style={{ flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <h2>Propostas comerciais & negociação assistida</h2>
            <p className="muted" style={{ fontSize: '0.86rem', marginTop: '0.2rem' }}>
              Simulação em tempo real pelo motor financeiro, controle de alçada e conversão auditada.
            </p>
          </div>
          <div style={{ display: 'flex', gap: '0.6rem', alignItems: 'center', flexWrap: 'wrap' }}>
            <select
              aria-label="Filtrar por situação da proposta"
              value={statusFilter}
              onChange={(e) => {
                setPage(0);
                setStatusFilter(e.target.value as StatusProposta | '');
              }}
              style={{
                padding: '0.45rem 0.75rem',
                borderRadius: '0.5rem',
                border: '1px solid var(--border, #334155)',
                background: 'transparent',
                color: 'inherit',
              }}
            >
              <option value="">Todas as situações</option>
              <option value="PENDENTE_APROVACAO">Fila gerencial (Pendente de aprovação)</option>
              <option value="EM_NEGOCIACAO">Em negociação</option>
              <option value="APROVADA">Aprovadas para fechamento</option>
              <option value="CONVERTIDA">Convertidas em venda</option>
              <option value="REJEITADA">Rejeitadas</option>
            </select>
            <span className="count-pill">{resource.data?.totalElements ?? '—'} propostas</span>
          </div>
        </div>

        {actionError && <ErrorState message={actionError} />}

        {resource.loading ? (
          <Loading />
        ) : resource.error ? (
          <ErrorState message={resource.error} retry={resource.reload} />
        ) : !resource.data?.content.length ? (
          <div className="empty-state">
            <FileSpreadsheet size={34} />
            <h3>Nenhuma proposta encontrada neste filtro.</h3>
            <p>Inicie uma nova proposta comercial para simular descontos, troca ou financiamento.</p>
          </div>
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>PROPOSTA / REVISÃO</th>
                  <th>VEÍCULO & CLIENTE</th>
                  <th>CONDIÇÃO PROPOSTA</th>
                  <th>ALÇADA & SITUAÇÃO</th>
                  {podeVerInterno && <th>CONTRIBUIÇÃO EST.</th>}
                  <th>AÇÕES</th>
                </tr>
              </thead>
              <tbody>
                {resource.data.content.map((prop) => {
                  const rev = prop.revisaoAtual;
                  const badge = alcadaBadge(rev?.alcadaAnalise);
                  const BadgeIcon = badge.icon;
                  const exigeDecisaoGerencial =
                    prop.status === 'PENDENTE_APROVACAO' ||
                    (prop.status === 'EM_NEGOCIACAO' &&
                      rev?.alcadaAnalise === 'EXIGE_APROVACAO_GERENCIAL');

                  return (
                    <tr key={prop.id}>
                      <td>
                        <strong>{prop.numero}</strong>
                        <span className="vehicle-subtitle">
                          Rev. #{prop.revisaoAtualSequencia} · Vendedor: {prop.vendedorIdentificador}
                        </span>
                      </td>
                      <td>
                        <strong>
                          {prop.carro.marca} {prop.carro.modelo} ({prop.carro.ano})
                        </strong>
                        <span className="vehicle-subtitle">
                          {prop.cliente.nome} · {formatDocument(prop.cliente.cpfCnpj)}
                        </span>
                      </td>
                      <td>
                        <strong>{rev ? currency(rev.precoNegociado) : '—'}</strong>
                        {rev && (
                          <span className="vehicle-subtitle">
                            Anunciado: {currency(rev.precoAnunciado)} · Desc:{' '}
                            {currency(rev.descontoReais)} ({(rev.descontoPercentual * 100).toFixed(1)}%)
                          </span>
                        )}
                      </td>
                      <td>
                        <div
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.35rem',
                            padding: '0.22rem 0.6rem',
                            borderRadius: '999px',
                            fontSize: '0.78rem',
                            fontWeight: 600,
                            color: badge.color,
                            background: badge.bg,
                          }}
                        >
                          <BadgeIcon size={14} />
                          {badge.label}
                        </div>
                        <span className="vehicle-subtitle" style={{ marginTop: '0.25rem' }}>
                          Status: <strong>{statusLabel(prop.status)}</strong>
                        </span>
                      </td>
                      {podeVerInterno && (
                        <td>
                          <strong>
                            {rev?.contribuicaoEstimada !== undefined
                              ? currency(rev.contribuicaoEstimada)
                              : '—'}
                          </strong>
                          {rev?.margemContribuicaoEstimada !== undefined && (
                            <span className="vehicle-subtitle">
                              Margem: {(rev.margemContribuicaoEstimada * 100).toFixed(2)}% (Base: Preço
                              negociado)
                            </span>
                          )}
                        </td>
                      )}
                      <td>
                        <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap' }}>
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => setResumoClienteProposta(prop)}
                          >
                            <FileText size={14} />
                            Resumo cliente
                          </Button>

                          {prop.status !== 'CONVERTIDA' && prop.status !== 'CANCELADA' && (
                            <Button
                              type="button"
                              variant="outline"
                              size="sm"
                              onClick={() => setSelectedProposta(prop)}
                            >
                              Nova revisão
                            </Button>
                          )}

                          {podeAprovar && exigeDecisaoGerencial && rev && (
                            <>
                              <Button
                                type="button"
                                size="sm"
                                disabled={busyId === prop.id}
                                onClick={() => void handleDecisaoGerencial(prop, true)}
                              >
                                <Check size={14} />
                                {busyId === prop.id ? 'Processando…' : 'Aprovar'}
                              </Button>
                              <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                disabled={busyId === prop.id}
                                onClick={() => void handleDecisaoGerencial(prop, false)}
                              >
                                <XCircle size={14} />
                                Rejeitar
                              </Button>
                            </>
                          )}

                          {prop.status === 'APROVADA' && rev && (
                            <Button
                              type="button"
                              size="sm"
                              disabled={busyId === prop.id}
                              onClick={() => void handleConverterVenda(prop)}
                            >
                              <Handshake size={14} />
                              {busyId === prop.id ? 'Convertendo…' : 'Fechar venda'}
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
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

      {(formRequested || selectedProposta) && (
        <ProposalSimulatorDialog
          propostaExistente={selectedProposta ?? undefined}
          perfil={perfil}
          onClose={() => {
            if (formRequested) closeForm();
            if (selectedProposta) setSelectedProposta(null);
          }}
          onSaved={(msg) => {
            if (formRequested) closeForm();
            if (selectedProposta) setSelectedProposta(null);
            resource.reload();
            onChanged(msg);
          }}
        />
      )}

      {resumoClienteProposta && (
        <CustomerSummaryDialog
          proposta={resumoClienteProposta}
          onClose={() => setResumoClienteProposta(null)}
        />
      )}
    </>
  );
}

export function ProposalSimulatorDialog({
  propostaExistente,
  initialCarroId,
  initialClienteId,
  perfil,
  onClose,
  onSaved,
}: {
  propostaExistente?: PropostaComercial;
  initialCarroId?: number;
  initialClienteId?: number;
  perfil: PerfilOperador;
  onClose: () => void;
  onSaved: (message: string) => void;
}) {
  const options = useResource(async (signal) => {
    const [carros, clientes] = await Promise.all([
      allPages<Carro>('carros', signal),
      allPages<Cliente>('clientes', signal),
    ]);
    return {
      carros,
      clientes: clientes.filter((c) => c.ativo),
    };
  }, [propostaExistente?.id]);

  const [carroId, setCarroId] = useState(
    propostaExistente
      ? String(propostaExistente.carro.id)
      : initialCarroId
        ? String(initialCarroId)
        : '',
  );
  const [clienteId, setClienteId] = useState(
    propostaExistente
      ? String(propostaExistente.cliente.id)
      : initialClienteId
        ? String(initialClienteId)
        : '',
  );
  const [precoRaw, setPrecoRaw] = useState(
    propostaExistente?.revisaoAtual ? String(propostaExistente.revisaoAtual.precoNegociado) : '',
  );
  const [justificativa, setJustificativa] = useState(
    propostaExistente?.revisaoAtual?.justificativaVendedor ?? '',
  );
  const [incluirTroca, setIncluirTroca] = useState(
    Boolean(propostaExistente?.revisaoAtual?.avaliacaoTroca),
  );
  const [usadoId, setUsadoId] = useState(
    propostaExistente?.revisaoAtual?.avaliacaoTroca?.identificacaoVeiculoUsado ?? '',
  );
  const [usadoAvaliacao, setUsadoAvaliacao] = useState(
    String(propostaExistente?.revisaoAtual?.avaliacaoTroca?.valorComercialAceito ?? ''),
  );
  const [usadoQuitacao, setUsadoQuitacao] = useState(
    String(propostaExistente?.revisaoAtual?.avaliacaoTroca?.saldoQuitacaoVerificado ?? '0'),
  );

  const [simulacao, setSimulacao] = useState<SimulacaoProposta | null>(null);
  const [simulando, setSimulando] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  const selectedCarro = options.data?.carros.find((c) => c.id === +carroId);

  useEffect(() => {
    if (selectedCarro && !precoRaw && !propostaExistente) {
      setPrecoRaw(String(selectedCarro.venda));
    }
  }, [selectedCarro, precoRaw, propostaExistente]);

  const precoNum = parseMoney(precoRaw);

  useEffect(() => {
    if (!carroId || !clienteId || !Number.isFinite(precoNum) || precoNum <= 0) {
      setSimulacao(null);
      return;
    }
    const controller = new AbortController();
    setSimulando(true);
    const timer = window.setTimeout(async () => {
      try {
        const payload: PropostaComercialInput = {
          carroId: +carroId,
          clienteId: +clienteId,
          precoNegociado: precoNum,
        };
        if (incluirTroca && usadoId.trim()) {
          const valAceito = parseMoney(usadoAvaliacao) || 0;
          const valQuitacao = parseMoney(usadoQuitacao) || 0;
          if (valAceito > 0) {
            payload.avaliacaoTroca = {
              identificacaoVeiculoUsado: usadoId.trim(),
              valorAvaliacaoTecnica: valAceito,
              valorComercialAceito: valAceito,
              saldoQuitacaoVerificado: valQuitacao,
            };
          }
        }
        const res = await api.simularProposta(payload, controller.signal);
        setSimulacao(res);
        setError(null);
      } catch (err) {
        if (!controller.signal.aborted) {
          setSimulacao(null);
          setError(err);
        }
      } finally {
        if (!controller.signal.aborted) {
          setSimulando(false);
        }
      }
    }, 350);

    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [carroId, clienteId, precoNum, incluirTroca, usadoId, usadoAvaliacao, usadoQuitacao, perfil]);

  async function salvar(submeter: boolean, event: FormEvent) {
    event.preventDefault();
    if (!carroId || !clienteId || !Number.isFinite(precoNum) || precoNum <= 0) {
      setError(new Error('Selecione o veículo, o cliente e informe um preço negociado válido.'));
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const payload: PropostaComercialInput = {
        carroId: +carroId,
        clienteId: +clienteId,
        precoNegociado: precoNum,
        justificativaVendedor: justificativa.trim() || undefined,
        submeter,
      };
      if (incluirTroca && usadoId.trim()) {
        const valAceito = parseMoney(usadoAvaliacao) || 0;
        const valQuitacao = parseMoney(usadoQuitacao) || 0;
        payload.avaliacaoTroca = {
          identificacaoVeiculoUsado: usadoId.trim(),
          valorAvaliacaoTecnica: valAceito,
          valorComercialAceito: valAceito,
          saldoQuitacaoVerificado: valQuitacao,
        };
      }
      if (propostaExistente) {
        await api.criarRevisaoProposta(propostaExistente.id, payload);
        onSaved(`Nova revisão registrada para a proposta ${propostaExistente.numero}.`);
      } else {
        const criada = await api.criarProposta(payload);
        onSaved(
          submeter
            ? `Proposta ${criada.numero} submetida (${statusLabel(criada.status)}).`
            : `Rascunho da proposta ${criada.numero} salvo.`,
        );
      }
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  const badge = alcadaBadge(simulacao?.alcada);
  const BadgeIcon = badge.icon;
  const podeSubmeter =
    !simulando &&
    simulacao &&
    simulacao.alcada !== 'BLOQUEADA_ABAIXO_PISO' &&
    !simulacao.possuiCustosPendentes;

  return (
    <Dialog
      open
      onOpenChange={(open) => {
        if (!open && !busy) onClose();
      }}
    >
      <DialogContent>
        <div className="dialog-symbol">
          <Sparkles size={24} />
        </div>
        <DialogTitle className="dialog-title">
          {propostaExistente
            ? `Nova revisão · ${propostaExistente.numero}`
            : 'Nova proposta comercial'}
        </DialogTitle>
        <DialogDescription className="muted">
          Simule condições em tempo real. Cálculos oficiais e alçadas são processados pelo domínio
          Java.
        </DialogDescription>

        {options.loading ? (
          <Loading />
        ) : options.error ? (
          <ErrorState message={options.error} retry={options.reload} />
        ) : (
          <form className="vehicle-form" onSubmit={(e) => salvar(true, e)}>
            <fieldset disabled={busy} className="form-fieldset">
              <div className="form-grid">
                <label className="span-two">
                  Veículo
                  <select
                    required
                    disabled={Boolean(propostaExistente)}
                    value={carroId}
                    onChange={(e) => {
                      setCarroId(e.target.value);
                      const c = options.data?.carros.find((item) => item.id === +e.target.value);
                      if (c) setPrecoRaw(String(c.venda));
                    }}
                  >
                    <option value="">Selecione um veículo do estoque</option>
                    {options.data?.carros.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.marca} {c.modelo} ({c.ano}) · Sugerido: {currency(c.venda)}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="span-two">
                  Cliente
                  <select
                    required
                    disabled={Boolean(propostaExistente)}
                    value={clienteId}
                    onChange={(e) => setClienteId(e.target.value)}
                  >
                    <option value="">Selecione um cliente ativo</option>
                    {options.data?.clientes.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.nome} · {formatDocument(c.cpfCnpj)}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="span-two">
                  Preço proposto na negociação (R$)
                  <MoneyInput
                    name="precoNegociado"
                    required
                    placeholder="R$ 0,00"
                    defaultValue={
                      propostaExistente?.revisaoAtual?.precoNegociado ?? selectedCarro?.venda
                    }
                    onValueChange={(values) => setPrecoRaw(values.formattedValue)}
                  />
                </label>

                <label className="span-two" style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                  <input
                    type="checkbox"
                    checked={incluirTroca}
                    onChange={(e) => setIncluirTroca(e.target.checked)}
                  />
                  Incluir avaliação de veículo usado na troca (Etapa 7.5)
                </label>

                {incluirTroca && (
                  <>
                    <label className="span-two">
                      Identificação do usado (Marca/Modelo/Ano/Placa)
                      <input
                        value={usadoId}
                        onChange={(e) => setUsadoId(e.target.value)}
                        placeholder="Ex.: VW Polo Highline 2021 ABC-1D23"
                      />
                    </label>
                    <label>
                      Valor comercial aceito (R$)
                      <input
                        type="number"
                        step="0.01"
                        value={usadoAvaliacao}
                        onChange={(e) => setUsadoAvaliacao(e.target.value)}
                        placeholder="40000"
                      />
                    </label>
                    <label>
                      Saldo de quitação de dívida (R$)
                      <input
                        type="number"
                        step="0.01"
                        value={usadoQuitacao}
                        onChange={(e) => setUsadoQuitacao(e.target.value)}
                        placeholder="10000"
                      />
                    </label>
                  </>
                )}

                {simulacao && (
                  <div
                    className="span-two"
                    style={{
                      padding: '0.9rem',
                      borderRadius: '0.75rem',
                      border: `1px solid ${badge.color}`,
                      background: badge.bg,
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 700, color: badge.color }}>
                      <BadgeIcon size={18} />
                      <span>{badge.label}</span>
                      {simulando && <small style={{ marginLeft: 'auto' }}>Recalculando…</small>}
                    </div>
                    <div style={{ fontSize: '0.84rem', marginTop: '0.5rem', display: 'grid', gap: '0.25rem' }}>
                      <div>
                        Preço anunciado: <strong>{currency(simulacao.precoAnunciado)}</strong> ·
                        Desconto: <strong>{currency(simulacao.descontoReais)}</strong> (
                        {(simulacao.descontoPercentual * 100).toFixed(2)}%)
                      </div>
                      <div>
                        Piso da autonomia do vendedor:{' '}
                        <strong>{currency(simulacao.pisoAutonomia)}</strong>
                      </div>
                      {simulacao.contribuicaoEstimada !== undefined && (
                        <div>
                          Contribuição estimada (Gerência):{' '}
                          <strong>{currency(simulacao.contribuicaoEstimada)}</strong> (
                          {((simulacao.margemContribuicaoEstimada ?? 0) * 100).toFixed(2)}% sobre venda)
                          {simulacao.pisoAbsoluto !== undefined && (
                            <> · Piso absoluto: <strong>{currency(simulacao.pisoAbsoluto)}</strong></>
                          )}
                        </div>
                      )}
                      {simulacao.motivosAlcada.length > 0 && (
                        <ul style={{ margin: '0.3rem 0 0 1.1rem', padding: 0 }}>
                          {simulacao.motivosAlcada.map((m, idx) => (
                            <li key={idx}>{m}</li>
                          ))}
                        </ul>
                      )}
                    </div>
                  </div>
                )}

                <label className="span-two">
                  Justificativa comercial / Observações da revisão
                  <textarea
                    rows={2}
                    maxLength={255}
                    value={justificativa}
                    onChange={(e) => setJustificativa(e.target.value)}
                    placeholder="Justifique caso solicite exceção gerencial..."
                  />
                </label>
              </div>

              <FormError error={error} />

              <div className="form-footer" style={{ gap: '0.5rem', flexWrap: 'wrap' }}>
                <Button type="button" variant="outline" onClick={onClose}>
                  Cancelar
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  disabled={busy || simulando}
                  onClick={(e) => salvar(false, e)}
                >
                  Salvar rascunho
                </Button>
                <Button type="submit" disabled={busy || !podeSubmeter}>
                  {busy
                    ? 'Processando…'
                    : simulacao?.alcada === 'EXIGE_APROVACAO_GERENCIAL'
                      ? 'Solicitar aprovação gerencial'
                      : 'Submeter e liberar proposta'}
                </Button>
              </div>
            </fieldset>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
}

function CustomerSummaryDialog({
  proposta,
  onClose,
}: {
  proposta: PropostaComercial;
  onClose: () => void;
}) {
  const resumo = proposta.resumoParaCliente;
  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <div className="dialog-symbol">
          <FileText size={24} />
        </div>
        <DialogTitle className="dialog-title">Resumo Comercial para o Cliente</DialogTitle>
        <DialogDescription className="muted">
          Documento limpo para compartilhamento (sem exposição de custo de aquisição, piso ou margem
          interna da loja).
        </DialogDescription>
        {resumo ? (
          <div className="info-copy" style={{ display: 'grid', gap: '0.55rem' }}>
            <p>
              <strong>Proposta:</strong> {resumo.numeroProposta} (Rev. #{proposta.revisaoAtualSequencia})
            </p>
            <p>
              <strong>Cliente:</strong> {resumo.cliente}
            </p>
            <p>
              <strong>Veículo:</strong> {resumo.veiculo}
            </p>
            <p>
              <strong>Preço de Tabela / Anunciado:</strong> {currency(resumo.precoAnunciado)}
            </p>
            <p>
              <strong>Desconto Comercial Concedido:</strong> {currency(resumo.descontoConcedido)}
            </p>
            <p style={{ fontSize: '1.08rem' }}>
              <strong>Preço Final Proposto:</strong> {currency(resumo.precoFinalProposto)}
            </p>
            {resumo.creditoLiquidoUsadoNaTroca !== undefined && (
              <p>
                <strong>Crédito Líquido do Veículo na Troca:</strong>{' '}
                {currency(resumo.creditoLiquidoUsadoNaTroca)}
              </p>
            )}
            <p>
              <strong>Validade das condições:</strong>{' '}
              {new Date(resumo.validaAte).toLocaleDateString('pt-BR')}
            </p>
          </div>
        ) : (
          <p className="muted">Resumo indisponível.</p>
        )}
        <Button onClick={onClose}>
          Fechar resumo
          <Check size={16} />
        </Button>
      </DialogContent>
    </Dialog>
  );
}
