import type { CSSProperties, ReactNode } from 'react';
import { useImagemAutenticada } from '../lib/useImagemAutenticada';

/** <img> para fotos protegidas pela API. Sem foto (ou com falha), mostra o fallback. */
export function FotoAutenticada({
  path,
  alt,
  className,
  style,
  fallback = null,
}: {
  path: string;
  alt: string;
  className?: string;
  style?: CSSProperties;
  fallback?: ReactNode;
}) {
  const foto = useImagemAutenticada(path);
  if (foto.status === 'pronta') {
    return <img className={className} src={foto.src} alt={alt} style={style} />;
  }
  if (foto.status === 'carregando') {
    const acessibilidade = alt ? { role: 'img', 'aria-label': alt } : { 'aria-hidden': true };
    return <span className={className} style={style} aria-busy="true" {...acessibilidade} />;
  }
  return <>{fallback}</>;
}
