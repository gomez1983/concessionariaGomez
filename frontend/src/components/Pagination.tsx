import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from './ui/button';
export function Pagination({
  page,
  total,
  size,
  onChange,
}: {
  page: number;
  total: number;
  size: number;
  onChange: (page: number) => void;
}) {
  const pages = Math.max(1, Math.ceil(total / size));
  return (
    <div className="pagination">
      <span>
        {total
          ? `Mostrando ${page * size + 1}–${Math.min((page + 1) * size, total)} de ${total} registros`
          : 'Nenhum registro'}
      </span>
      <div>
        <Button
          variant="outline"
          size="sm"
          aria-label="Página anterior"
          disabled={page === 0}
          onClick={() => onChange(page - 1)}
        >
          <ChevronLeft size={16} />
        </Button>
        <span className="page-caption">
          {page + 1} de {pages}
        </span>
        <Button
          variant="outline"
          size="sm"
          aria-label="Próxima página"
          disabled={page + 1 >= pages}
          onClick={() => onChange(page + 1)}
        >
          <ChevronRight size={16} />
        </Button>
      </div>
    </div>
  );
}
