import { useState } from 'react';
import type { FormEvent } from 'react';
import { CarFront, Eye, EyeOff, LoaderCircle, LogIn, Moon, Sun } from 'lucide-react';
import { useAuth } from '../lib/auth';
import type { MotivoFimSessao } from '../lib/session';
import { useTheme } from '../lib/useTheme';
import { Button } from './ui/button';

export function LoginScreen({ motivo }: { motivo?: MotivoFimSessao }) {
  const { entrar } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [entrando, setEntrando] = useState(false);
  const [erro, setErro] = useState('');

  async function enviar(event: FormEvent) {
    event.preventDefault();
    if (entrando) return;
    setErro('');
    setEntrando(true);
    try {
      await entrar(email.trim(), senha);
    } catch (error) {
      setErro(error instanceof Error ? error.message : 'Não foi possível entrar.');
      setSenha('');
      setEntrando(false);
    }
  }

  return (
    <main className="login-shell">
      <Button
        variant="ghost"
        size="icon"
        className="login-theme"
        onClick={toggleTheme}
        aria-label={theme === 'dark' ? 'Ativar modo claro' : 'Ativar modo escuro'}
        title={theme === 'dark' ? 'Ativar modo claro' : 'Ativar modo escuro'}
      >
        {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
      </Button>
      <form className="login-card" onSubmit={enviar} noValidate>
        <span className="login-mark">
          <CarFront size={28} />
        </span>
        <h1>Concessionária Gomez</h1>
        <p className="muted">Entre com seu usuário para acessar o painel de gestão.</p>

        {motivo === 'expirada' && !erro && (
          <p className="login-notice" role="status">
            Sua sessão expirou. Entre novamente para continuar.
          </p>
        )}
        {motivo === 'logout' && !erro && (
          <p className="login-notice" role="status">
            Você saiu do painel.
          </p>
        )}

        <label>
          E-mail
          <input
            type="email"
            name="email"
            autoComplete="username"
            inputMode="email"
            autoFocus
            required
            maxLength={100}
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </label>
        <label>
          Senha
          <span className="password-field">
            <input
              type={mostrarSenha ? 'text' : 'password'}
              name="senha"
              autoComplete="current-password"
              required
              maxLength={72}
              value={senha}
              onChange={(event) => setSenha(event.target.value)}
            />
            <button
              type="button"
              className="password-toggle"
              onClick={() => setMostrarSenha((valor) => !valor)}
              aria-label={mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'}
              aria-pressed={mostrarSenha}
              title={mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'}
            >
              {mostrarSenha ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </span>
        </label>

        {erro && (
          <p className="form-error login-error" role="alert">
            {erro}
          </p>
        )}

        <Button type="submit" disabled={entrando || !email.trim() || !senha}>
          {entrando ? <LoaderCircle size={17} className="spin" /> : <LogIn size={17} />}
          {entrando ? 'Entrando…' : 'Entrar'}
        </Button>
        <p className="login-help">
          Esqueceu a senha ou o acesso foi bloqueado? Procure o administrador do painel.
        </p>
      </form>
    </main>
  );
}
