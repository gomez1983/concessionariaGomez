import { useState } from 'react';
import { Trash2 } from 'lucide-react';
import { Dialog, DialogContent, DialogDescription, DialogTitle } from './ui/dialog';
import { Button } from './ui/button';
import { FormError } from './Feedback';
export interface Confirmation {
  title: string;
  description: string;
  label: string;
  action: () => Promise<void>;
}
export function ConfirmAction({
  confirmation,
  onClose,
}: {
  confirmation: Confirmation | null;
  onClose: () => void;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  return (
    <Dialog
      open={!!confirmation}
      onOpenChange={(open) => {
        if (!open && !busy) {
          setError(null);
          onClose();
        }
      }}
    >
      <DialogContent className="confirm-dialog">
        <div className="dialog-symbol danger">
          <Trash2 size={24} />
        </div>
        <DialogTitle className="dialog-title">{confirmation?.title}</DialogTitle>
        <DialogDescription className="muted">{confirmation?.description}</DialogDescription>
        <FormError error={error} />
        <div className="form-footer">
          <Button variant="outline" disabled={busy} onClick={onClose}>
            Cancelar
          </Button>
          <Button
            variant="destructive"
            disabled={busy}
            onClick={async () => {
              setBusy(true);
              setError(null);
              try {
                await confirmation?.action();
                onClose();
              } catch (error) {
                setError(error);
              } finally {
                setBusy(false);
              }
            }}
          >
            {busy ? 'Aguarde…' : confirmation?.label}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
