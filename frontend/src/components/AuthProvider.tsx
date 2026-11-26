import { useCallback, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { AuthContext } from '../lib/auth';
import type { EstadoAutenticacao } from '../lib/auth';
import { entrar, getUsuario, onSessao, sair, verificarSessaoInicial } from '../lib/session';
import { LoginScreen } from './LoginScreen';
import { LoaderCircle } from 'lucide-react';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [estado, setEstado] = useState<EstadoAutenticacao>(() => {
    const usuario = getUsuario();
    return usuario ? { status: 'autenticado', usuario } : { status: 'verificando' };
  });

  useEffect(() => {
    let montado = true;
    const cancelar = onSessao((usuario, motivo) =>
      setEstado(usuario ? { status: 'autenticado', usuario } : { status: 'anonimo', motivo }),
    );
    // Ao recarregar a página o token em memória se perde: tenta recuperar a sessão pelo cookie
    verificarSessaoInicial().then((usuario) => {
      if (!montado) return;
      setEstado((atual) =>
        atual.status !== 'verificando'
          ? atual
          : usuario
            ? { status: 'autenticado', usuario }
            : { status: 'anonimo' },
      );
    });
    return () => {
      montado = false;
      cancelar();
    };
  }, []);

  const entrarNoPainel = useCallback(async (email: string, senha: string) => {
    await entrar(email, senha);
  }, []);

  const valor = useMemo(() => ({ estado, entrar: entrarNoPainel, sair }), [estado, entrarNoPainel]);

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

/** Mostra o painel só para quem está autenticado; caso contrário, a tela de login. */
export function AuthGate({ children }: { children: (email: string) => ReactNode }) {
  return (
    <AuthContext.Consumer>
      {(contexto) => {
        const estado = contexto?.estado;
        if (!estado || estado.status === 'verificando') {
          return (
            <div className="auth-splash" role="status">
              <LoaderCircle size={26} className="spin" />
              Verificando sua sessão…
            </div>
          );
        }
        if (estado.status === 'anonimo') return <LoginScreen motivo={estado.motivo} />;
        return children(estado.usuario.email);
      }}
    </AuthContext.Consumer>
  );
}
