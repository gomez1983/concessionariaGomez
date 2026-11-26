import { useEffect, useRef, useState } from 'react';
import { ChevronDown, KeyRound, LogOut } from 'lucide-react';
import { iniciais, NOME_PERFIL, useAuth, useUsuario } from '../lib/auth';

/** Chip do usuário logado com o menu "Trocar senha" e "Sair" (seção 8.2 do design da Etapa 3). */
export function UserMenu({ onTrocarSenha }: { onTrocarSenha: () => void }) {
  const usuario = useUsuario();
  const { sair } = useAuth();
  const [aberto, setAberto] = useState(false);
  const [saindo, setSaindo] = useState(false);
  const container = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!aberto) return;
    function cliqueFora(event: MouseEvent) {
      if (container.current && !container.current.contains(event.target as Node)) setAberto(false);
    }
    function tecla(event: KeyboardEvent) {
      if (event.key === 'Escape') setAberto(false);
    }
    document.addEventListener('mousedown', cliqueFora);
    document.addEventListener('keydown', tecla);
    return () => {
      document.removeEventListener('mousedown', cliqueFora);
      document.removeEventListener('keydown', tecla);
    };
  }, [aberto]);

  return (
    <div className="user-menu" ref={container}>
      <button
        type="button"
        className="user-chip"
        title={usuario.email}
        aria-haspopup="menu"
        aria-expanded={aberto}
        aria-label={`Menu de ${usuario.nome}`}
        onClick={() => setAberto((valor) => !valor)}
      >
        <span className="top-avatar" aria-hidden="true">
          {iniciais(usuario.nome)}
        </span>
        <span className="user-chip-text">
          <strong>{usuario.nome}</strong>
          <span className={`perfil-badge perfil-${usuario.perfil.toLowerCase()}`}>
            {NOME_PERFIL[usuario.perfil]}
          </span>
        </span>
        <ChevronDown size={15} aria-hidden="true" />
      </button>
      {aberto && (
        <div className="user-menu-popover" role="menu">
          <span className="user-menu-email">{usuario.email}</span>
          <button
            type="button"
            role="menuitem"
            onClick={() => {
              setAberto(false);
              onTrocarSenha();
            }}
          >
            <KeyRound size={15} />
            Trocar senha
          </button>
          <button
            type="button"
            role="menuitem"
            disabled={saindo}
            onClick={() => {
              setSaindo(true);
              void sair();
            }}
          >
            <LogOut size={15} />
            {saindo ? 'Saindo…' : 'Sair'}
          </button>
        </div>
      )}
    </div>
  );
}
