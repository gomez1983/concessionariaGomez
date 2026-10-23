// Ilustração vetorial original; não representa uma foto do veículo anunciado.
export function CarIllustration({ marca, large = false }: { marca: string; large?: boolean }) {
  const colors: Record<string, string> = {
    Volkswagen: '#778b9b',
    Ford: '#a7aaa5',
    Mitsubishi: '#597d70',
    Chevrolet: '#b09a7d',
  };
  return (
    <svg className={large ? 'car-art large' : 'car-art'} viewBox="0 0 160 84" aria-hidden="true">
      <ellipse cx="81" cy="68" rx="65" ry="5" fill="#213e3212" />
      <path d="m21 46 16-6 19-21h43l25 21 16 5 5 16H15l2-10z" fill={colors[marca] ?? '#788a7d'} />
      <path d="m43 40 17-17h15v17zm38-17h16l20 17H81z" fill="#dce8e5" />
      <path d="M17 50h17m96 0h11" stroke="#f7f3dc" strokeWidth="5" />
      <path d="M19 60h123M78 43v13" stroke="#253b3440" strokeWidth="2" />
      <path d="M88 45h8" stroke="#d5e1db" strokeWidth="2" />
      {[42, 119].map((cx) => (
        <g key={cx}>
          <circle cx={cx} cy="61" r="13" fill="#293b37" />
          <circle cx={cx} cy="61" r="7" fill="#c8d0cc" />
          <circle cx={cx} cy="61" r="3" fill="#687b73" />
        </g>
      ))}
    </svg>
  );
}
