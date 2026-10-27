import { useEffect, useRef, useState } from 'react';
import {
  Camera,
  ChevronLeft,
  ChevronRight,
  ImagePlus,
  Pencil,
  Trash2,
  Upload,
  X,
} from 'lucide-react';
import type { Carro, Foto } from '../types';
import { api, photoUrl } from '../lib/api';
import { useResource } from '../lib/useResource';
import { Dialog, DialogContent, DialogDescription, DialogTitle } from './ui/dialog';
import { Button } from './ui/button';
import { ErrorState, FormError, Loading } from './Feedback';
import { ConfirmAction, type Confirmation } from './ConfirmAction';
import { referencePhoto } from '../lib/referencePhotos';

type PendingFile = { file: File; url: string; descricao: string };
export function VehicleGallery({
  vehicle,
  onClose,
  onChanged,
}: {
  vehicle: Carro;
  onClose: () => void;
  onChanged: () => void;
}) {
  const photos = useResource((signal) => api.fotos(vehicle.id, signal), [vehicle.id]);
  const [selected, setSelected] = useState(0);
  const [pending, setPending] = useState<PendingFile[]>([]);
  const [target, setTarget] = useState<Foto>();
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [confirmation, setConfirmation] = useState<Confirmation | null>(null);
  const [revision, setRevision] = useState(Date.now());
  const [success, setSuccess] = useState('');
  const previewUrls = useRef<string[]>([]);
  const all = photos.data ?? [];
  const photo = all[Math.min(selected, Math.max(0, all.length - 1))];
  const reference = referencePhoto(vehicle.marca, vehicle.modelo);
  useEffect(() => () => previewUrls.current.forEach((url) => URL.revokeObjectURL(url)), []);
  function preview(file: File) {
    const url = URL.createObjectURL(file);
    previewUrls.current.push(url);
    return url;
  }
  function refresh() {
    photos.reload();
    onChanged();
    setRevision(Date.now());
  }
  async function selectFiles(files: FileList | File[] | null, replace?: Foto) {
    if (!files) return;
    setError(null);
    setSuccess('');
    const list = Array.from(files);
    if (replace && list.length > 1) {
      setError(new Error('Escolha uma imagem para substituir a foto selecionada.'));
      return;
    }
    if (list.length > 20) {
      setError(new Error('Selecione até 20 imagens por envio.'));
      return;
    }
    if (
      list.some(
        (file) =>
          !['image/jpeg', 'image/png', 'image/webp'].includes(file.type) ||
          file.size === 0 ||
          file.size > 10 * 1024 * 1024,
      )
    ) {
      setError(new Error('Selecione apenas imagens JPEG, PNG ou WebP de até 10 MB cada.'));
      return;
    }
    const selected: PendingFile[] = [];
    try {
      for (const file of list) {
        const bitmap = await createImageBitmap(file);
        const valid =
          bitmap.width <= 12000 &&
          bitmap.height <= 12000 &&
          bitmap.width * bitmap.height <= 40000000;
        bitmap.close();
        if (!valid)
          throw new Error('Cada imagem deve ter no máximo 12.000 pixels por lado e 40 megapixels.');
        selected.push({ file, url: preview(file), descricao: replace?.descricao || 'Frente' });
      }
      setTarget(replace);
      setPending(selected);
    } catch (error) {
      selected.forEach((item) => URL.revokeObjectURL(item.url));
      setError(
        error instanceof Error ? error : new Error('Não foi possível abrir uma das imagens.'),
      );
    }
  }
  async function upload() {
    setUploading(true);
    setError(null);
    setSuccess('');
    let completed = 0;
    let hasPrincipal = all.some((photo) => photo.principal);
    try {
      for (const item of pending) {
        const destination = target ?? (!hasPrincipal ? ({ principal: true } as Foto) : undefined);
        await api.upload(
          vehicle.id,
          item.file,
          item.descricao.trim() || 'Detalhes do veículo',
          destination,
        );
        hasPrincipal = true;
        completed++;
      }
      setPending([]);
      setTarget(undefined);
      setSelected(0);
      setSuccess(`${completed} ${completed === 1 ? 'imagem salva' : 'imagens salvas'} no veículo.`);
    } catch (error) {
      // Mantém somente as imagens ainda não enviadas para não duplicar uploads ao tentar novamente.
      setPending((previous) => previous.slice(completed));
      setError(error);
    } finally {
      refresh();
      setUploading(false);
    }
  }
  return (
    <Dialog
      open
      onOpenChange={(open) => {
        if (!open && !uploading) onClose();
      }}
    >
      <DialogContent className="gallery-dialog">
        <div className="gallery-header">
          <div>
            <span className="eyebrow">GALERIA DO VEÍCULO</span>
            <DialogTitle className="dialog-title">
              {vehicle.marca} {vehicle.modelo}
            </DialogTitle>
            <DialogDescription className="muted">
              Fotos externas, internas e detalhes. JPEG, PNG ou WebP · até 10 MB por imagem.
            </DialogDescription>
          </div>
          <Camera size={28} />
        </div>
        {photos.loading ? (
          <Loading />
        ) : photos.error ? (
          <ErrorState message={photos.error} retry={photos.reload} />
        ) : (
          <>
            {photo ? (
              <>
                <div className="gallery-stage">
                  <img src={photoUrl(vehicle.id, photo, revision)} alt={photo.descricao} />
                  <Button
                    variant="outline"
                    size="icon"
                    className="gallery-prev"
                    aria-label="Foto anterior"
                    disabled={all.length < 2}
                    onClick={() => setSelected((selected - 1 + all.length) % all.length)}
                  >
                    <ChevronLeft size={19} />
                  </Button>
                  <Button
                    variant="outline"
                    size="icon"
                    className="gallery-next"
                    aria-label="Próxima foto"
                    disabled={all.length < 2}
                    onClick={() => setSelected((selected + 1) % all.length)}
                  >
                    <ChevronRight size={19} />
                  </Button>
                  <span className="gallery-counter">
                    {Math.min(selected + 1, all.length)} / {all.length}
                  </span>
                </div>
                <div className="gallery-caption">
                  <div>
                    <strong>{photo.descricao}</strong>
                    {photo.principal && <span className="count-pill">Foto principal</span>}
                  </div>
                  <div className="gallery-tools">
                    <label className={`button button-outline ${uploading ? 'disabled-label' : ''}`}>
                      <Pencil size={14} />
                      Substituir
                      <input
                        aria-label="Substituir foto selecionada"
                        type="file"
                        accept="image/jpeg,image/png,image/webp"
                        disabled={uploading}
                        onChange={(event) => {
                          void selectFiles(event.target.files, photo);
                          event.target.value = '';
                        }}
                      />
                    </label>
                    <Button
                      variant="ghost"
                      disabled={uploading}
                      aria-label="Remover foto selecionada"
                      onClick={() =>
                        setConfirmation({
                          title: 'Remover esta foto?',
                          description:
                            'A imagem será removida da galeria e do armazenamento do servidor.',
                          label: 'Remover foto',
                          action: async () => {
                            await api.deleteFoto(vehicle.id, photo);
                            setSelected(0);
                            refresh();
                          },
                        })
                      }
                    >
                      <Trash2 size={16} />
                    </Button>
                  </div>
                </div>
                <div className="gallery-thumbs">
                  {all.map((foto, index) => (
                    <button
                      key={foto.id ?? 'principal'}
                      aria-label={`Ver foto ${index + 1}: ${foto.descricao}`}
                      aria-pressed={index === selected}
                      onClick={() => setSelected(index)}
                    >
                      <img src={photoUrl(vehicle.id, foto, revision)} alt="" />
                      <span>{foto.descricao}</span>
                    </button>
                  ))}
                </div>
              </>
            ) : reference ? (
              <>
                <div className="gallery-stage">
                  <img
                    src={reference.image}
                    alt={`Imagem de referência de ${vehicle.marca} ${vehicle.modelo}`}
                  />
                </div>
                <div className="reference-caption">
                  <strong>Imagem de referência do modelo</strong>
                  <p>Ano, versão e cor podem variar. Envie fotos para mostrar este veículo.</p>
                  <span>
                    Foto: {reference.author} ·{' '}
                    {reference.licenseUrl ? (
                      <a href={reference.licenseUrl} target="_blank" rel="noreferrer">
                        {reference.license}
                      </a>
                    ) : (
                      reference.license
                    )}{' '}
                    ·{' '}
                    <a href={reference.sourceUrl} target="_blank" rel="noreferrer">
                      Fonte
                    </a>
                  </span>
                </div>
              </>
            ) : (
              <div className="gallery-empty">
                <Camera size={38} />
                <h3>Mostre cada detalhe.</h3>
                <p>Adicione a primeira foto para apresentar este veículo.</p>
              </div>
            )}
            <label
              className={`upload-dropzone ${uploading ? 'disabled-label' : ''}`}
              onDragOver={(event) => event.preventDefault()}
              onDrop={(event) => {
                event.preventDefault();
                if (!uploading) void selectFiles(event.dataTransfer.files);
              }}
            >
              <ImagePlus size={27} />
              <strong>Escolha imagens ou arraste para cá</strong>
              <span>
                Envie vários ângulos. A primeira imagem vira a principal se ainda não houver uma.
              </span>
              <input
                type="file"
                aria-label="Adicionar fotos do veículo"
                multiple
                accept="image/jpeg,image/png,image/webp"
                disabled={uploading}
                onChange={(event) => {
                  void selectFiles(event.target.files);
                  event.target.value = '';
                }}
              />
            </label>
            {pending.length > 0 && (
              <div className="pending-uploads">
                <h3>{target ? 'Substituição de imagem' : 'Imagens prontas para enviar'}</h3>
                {pending.map((item, index) => (
                  <div className="pending-row" key={`${item.file.name}-${index}`}>
                    <img src={item.url} alt="Prévia do envio" />
                    <div>
                      <span>{item.file.name}</span>
                      <label>
                        Ângulo / descrição
                        <input
                          aria-label={`Descrição da imagem ${index + 1}`}
                          list="photo-angles"
                          maxLength={150}
                          value={item.descricao}
                          disabled={uploading}
                          onChange={(event) =>
                            setPending((previous) =>
                              previous.map((row, i) =>
                                i === index ? { ...row, descricao: event.target.value } : row,
                              ),
                            )
                          }
                        />
                      </label>
                    </div>
                  </div>
                ))}
                <datalist id="photo-angles">
                  {[
                    'Frente',
                    'Traseira',
                    'Lateral esquerda',
                    'Lateral direita',
                    'Interior',
                    'Painel',
                    'Bancos',
                    'Porta-malas',
                    'Motor',
                    'Detalhes',
                  ].map((angle) => (
                    <option key={angle} value={angle} />
                  ))}
                </datalist>
                <div className="form-footer">
                  <Button
                    variant="outline"
                    disabled={uploading}
                    onClick={() => {
                      setPending([]);
                      setTarget(undefined);
                    }}
                  >
                    Cancelar envio
                  </Button>
                  <Button disabled={uploading} onClick={upload}>
                    <Upload size={16} />
                    {uploading
                      ? 'Enviando imagens…'
                      : `Enviar ${pending.length} ${pending.length === 1 ? 'imagem' : 'imagens'}`}
                  </Button>
                </div>
              </div>
            )}
          </>
        )}
        {success && (
          <p className="success-message" role="status">
            {success}
          </p>
        )}
        <FormError error={error} />
        {confirmation && (
          <ConfirmAction confirmation={confirmation} onClose={() => setConfirmation(null)} />
        )}
      </DialogContent>
    </Dialog>
  );
}
