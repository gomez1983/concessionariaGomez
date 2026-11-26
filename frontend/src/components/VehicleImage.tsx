import { useState } from 'react';
import { photoPath } from '../lib/api';
import { useImagemAutenticada } from '../lib/useImagemAutenticada';
import { CarIllustration } from './CarIllustration';
import { referencePhoto } from '../lib/referencePhotos';

/** Foto principal do veículo; sem foto (404) ou com falha, cai na imagem de referência e depois na ilustração. */
export function VehicleImage({
  id,
  marca,
  modelo,
  revision,
  large = false,
}: {
  id: number;
  marca: string;
  modelo: string;
  revision: number;
  large?: boolean;
}) {
  const foto = useImagemAutenticada(photoPath(id, undefined, revision));
  const [failedReference, setFailedReference] = useState('');
  const reference = referencePhoto(marca, modelo);

  if (foto.status === 'pronta') {
    return (
      <img className="vehicle-photo" src={foto.src} alt={`${marca} ${modelo}, foto principal`} />
    );
  }
  if (foto.status === 'carregando') {
    return <span className="vehicle-photo" aria-hidden="true" />;
  }
  const src = reference?.image;
  if (!src || failedReference === src) return <CarIllustration marca={marca} large={large} />;
  return (
    <>
      <img
        className="vehicle-photo"
        src={src}
        alt={`Imagem de referência de ${marca} ${modelo}`}
        loading="lazy"
        onError={() => setFailedReference(src)}
      />
      {large && <span className="reference-badge">Imagem de referência</span>}
    </>
  );
}
