import { useState } from 'react';
import { Trash2 } from 'lucide-react';
import { Dialog, DialogContent, DialogDescription, DialogTitle } from './ui/dialog';
import { Button } from './ui/button';
import { FormError } from './Feedback';
export interface Confirmation {
  title: string;
  description: string;
  label: string;
  /** Quando informado, exige uma justificativa (ex.: motivo do cancelamento auditado). */
  reason?: { label: string; placeholder?: string; maxLength?: number };
  action: (reason?: string) => Promise<void>;
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
  const [reason, setReason] = useState('');
  const reasonMissing = !!confirmation?.reason && reason.trim() === '';
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
        {confirmation?.reason && (
          <label className="confirm-reason">
            {confirmation.reason.label}
            <textarea
              required
              rows={3}
              maxLength={confirmation.reason.maxLength ?? 255}
              placeholder={confirmation.reason.placeholder}
              value={reason}
              disabled={busy}
              onChange={(event) => setReason(event.target.value)}
            />
          </label>
        )}
        <FormError error={error} />
        <div className="form-footer">
          <Button variant="outline" disabled={busy} onClick={onClose}>
            Cancelar
          </Button>
          <Button
            variant="destructive"
            disabled={busy || reasonMissing}
            onClick={async () => {
              setBusy(true);
              setError(null);
              try {
                await confirmation?.action(confirmation.reason ? reason.trim() : undefined);
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
