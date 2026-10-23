import { NumberFormatBase } from 'react-number-format';
import { digits } from '../../lib/inputMasks';

export function MaskedInput({
  format,
  value,
  onValueChange,
  name,
  required = false,
  placeholder,
  autoComplete,
  onBlur,
}: {
  format: (value: string) => string;
  value: string;
  onValueChange: (value: string) => void;
  name: string;
  required?: boolean;
  placeholder?: string;
  autoComplete?: string;
  onBlur?: () => void;
}) {
  return (
    <NumberFormatBase
      name={name}
      value={value}
      valueIsNumericString
      format={format}
      removeFormatting={digits}
      onValueChange={(values) => onValueChange(values.value)}
      type="text"
      inputMode="numeric"
      required={required}
      placeholder={placeholder}
      autoComplete={autoComplete}
      onBlur={onBlur}
    />
  );
}
