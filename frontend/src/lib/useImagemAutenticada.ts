import { useEffect, useState } from 'react';
import { imagem } from './api';

export type EstadoImagem =
  | { status: 'carregando'; src: null }
  | { status: 'pronta'; src: string }
  | { status: 'ausente'; src: null }
  | { status: 'erro'; src: null };

/**
 * Baixa uma foto protegida com o token e a expõe como blob URL para o <img>.
 * "ausente" (404) permite cair na imagem de referência do modelo; a URL é revogada ao trocar ou desmontar.
 */
export function useImagemAutenticada(path: string | null): EstadoImagem {
  const [estado, setEstado] = useState<EstadoImagem>(
    path ? { status: 'carregando', src: null } : { status: 'ausente', src: null },
  );

  useEffect(() => {
    if (!path) {
      setEstado({ status: 'ausente', src: null });
      return;
    }
    const controller = new AbortController();
    let blobUrl: string | null = null;
    setEstado({ status: 'carregando', src: null });

    imagem(path, controller.signal)
      .then((blob) => {
        if (controller.signal.aborted) return;
        if (!blob) {
          setEstado({ status: 'ausente', src: null });
          return;
        }
        blobUrl = URL.createObjectURL(blob);
        setEstado({ status: 'pronta', src: blobUrl });
      })
      .catch(() => {
        if (!controller.signal.aborted) setEstado({ status: 'erro', src: null });
      });

    return () => {
      controller.abort();
      if (blobUrl) URL.revokeObjectURL(blobUrl);
    };
  }, [path]);

  return estado;
}
