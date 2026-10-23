import { useState } from 'react';
import { photoUrl } from '../lib/api';
import { CarIllustration } from './CarIllustration';
import { referencePhoto } from '../lib/referencePhotos';
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
  const [failed, setFailed] = useState('');
  const [failedReference, setFailedReference] = useState('');
  const url = photoUrl(id, undefined, revision);
  const reference = referencePhoto(marca, modelo);
  const isReference = failed === url;
  const src = isReference ? reference?.image : url;
  return !src || (isReference && failedReference === src) ? (
    <CarIllustration marca={marca} large={large} />
  ) : (
    <>
      <img
        className="vehicle-photo"
        src={src}
        alt={
          isReference
            ? `Imagem de referência de ${marca} ${modelo}`
            : `${marca} ${modelo}, foto principal`
        }
        loading="lazy"
        onError={() => (isReference ? setFailedReference(src) : setFailed(url))}
      />
      {isReference && large && <span className="reference-badge">Imagem de referência</span>}
    </>
  );
}
