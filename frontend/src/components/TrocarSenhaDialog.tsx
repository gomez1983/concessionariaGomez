import { useState } from 'react';
import type { FormEvent } from 'react';
import { Eye, EyeOff, KeyRound, LoaderCircle } from 'lucide-react';
import { api } from '../lib/api';
import { Button } from './ui/button';
import { Dialog, DialogContent, DialogDescription, DialogTitle } from './ui/dialog';
import { FormError } from './Feedback';
import { SENHA_MAXIMO as MAXIMO, SENHA_MINIMO as MINIMO, validarNovaSenha } from '../lib/senha';

export function TrocarSenhaDialog({
  open,
  onClose,
  onDone,
}: {
  open: boolean;
  onClose: () => void;
  onDone: (mensagem: string) => void;
}) {
  const [atual, setAtual] = useState('');
  const [nova, setNova] = useState('');
  const [confirmacao, setConfirmacao] = useState('');
  const [mostrar, setMostrar] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState<unknown>(null);

  function fechar() {
    if (salvando) return;
    setAtual('');
    setNova('');
    setConfirmacao('');
    setErro(null);
    setMostrar(false);
    onClose();
  }

  async function enviar(event: FormEvent) {
    event.preventDefault();
    const problema = validarNovaSenha(atual, nova, confirmacao);
    if (problema) {
      setErro(new Error(problema));
      return;
    }
    setErro(null);
    setSalvando(true);
    try {
      await api.trocarSenha(atual, nova);
      setSalvando(false);
      setAtual('');
      setNova('');
      setConfirmacao('');
      onDone('Senha alterada. As sessões abertas em outros dispositivos foram encerradas.');
    } catch (error) {
      setErro(error);
      setSalvando(false);
    }
  }

  const tipo = mostrar ? 'text' : 'password';
  return (
    <Dialog open={open} onOpenChange={(aberto) => !aberto && fechar()}>
      <DialogContent className="confirm-dialog">
        <div className="dialog-symbol">
          <KeyRound size={24} />
        </div>
        <DialogTitle className="dialog-title">Trocar senha</DialogTitle>
        <DialogDescription className="muted">
          Use de {MINIMO} a {MAXIMO} caracteres. Ao trocar, as sessões abertas em outros
          dispositivos são encerradas.
        </DialogDescription>
        <form className="password-form" onSubmit={enviar} noValidate>
          <label>
            Senha atual
            <input
              type={tipo}
              autoComplete="current-password"
              maxLength={MAXIMO}
              required
              value={atual}
              onChange={(event) => setAtual(event.target.value)}
            />
          </label>
          <label>
            Nova senha
            <input
              type={tipo}
              autoComplete="new-password"
              maxLength={MAXIMO}
              required
              value={nova}
              onChange={(event) => setNova(event.target.value)}
            />
          </label>
          <label>
            Confirme a nova senha
            <input
              type={tipo}
              autoComplete="new-password"
              maxLength={MAXIMO}
              required
              value={confirmacao}
              onChange={(event) => setConfirmacao(event.target.value)}
            />
          </label>
          <button
            type="button"
            className="password-visibility"
            onClick={() => setMostrar((valor) => !valor)}
            aria-pressed={mostrar}
          >
            {mostrar ? <EyeOff size={15} /> : <Eye size={15} />}
            {mostrar ? 'Ocultar senhas' : 'Mostrar senhas'}
          </button>
          <FormError error={erro} />
          <div className="form-footer">
            <Button type="button" variant="outline" onClick={fechar} disabled={salvando}>
              Cancelar
            </Button>
            <Button type="submit" disabled={salvando || !atual || !nova || !confirmacao}>
              {salvando && <LoaderCircle size={16} className="spin" />}
              {salvando ? 'Salvando…' : 'Trocar senha'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
