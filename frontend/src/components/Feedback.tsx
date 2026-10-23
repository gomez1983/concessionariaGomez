import { AlertCircle, LoaderCircle } from 'lucide-react';
import { ApiError } from '../lib/api';
import { Button } from './ui/button';
export function Loading() {
  return (
    <div className="loading-state" role="status">
      <LoaderCircle size={23} className="spin" />
      Carregando dados da API…
    </div>
  );
}
export function ErrorState({ message, retry }: { message: string; retry?: () => void }) {
  return (
    <div className="error-state" role="alert">
      <AlertCircle size={23} />
      <p>{message}</p>
      {retry && (
        <Button variant="outline" onClick={retry}>
          Tentar novamente
        </Button>
      )}
    </div>
  );
}
export function FormError({ error }: { error: unknown }) {
  if (!error) return null;
  return (
    <div className="form-error" role="alert">
      <p>{error instanceof Error ? error.message : 'Não foi possível salvar.'}</p>
      {error instanceof ApiError && Object.entries(error.fields).length > 0 && (
        <ul>
          {Object.entries(error.fields).map(([field, message]) => (
            <li key={field}>{message}</li>
          ))}
        </ul>
      )}
    </div>
  );
}
