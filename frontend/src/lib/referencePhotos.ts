import photos from '../data/referencePhotos.json';

const normalize = (text: string) =>
  text
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '');

export function referencePhoto(marca: string, modelo: string) {
  return photos.find(
    (photo) =>
      normalize(photo.marca) === normalize(marca) && normalize(photo.modelo) === normalize(modelo),
  );
}
