import { NumericFormat } from 'react-number-format';
import type { NumericFormatProps } from 'react-number-format';

export function MoneyInput(props: NumericFormatProps) {
  return (
    <NumericFormat
      {...props}
      type="text"
      inputMode="decimal"
      thousandSeparator="."
      decimalSeparator=","
      allowedDecimalSeparators={[',']}
      decimalScale={2}
      fixedDecimalScale
      allowNegative={false}
      prefix="R$ "
      isAllowed={(values) => values.floatValue === undefined || values.floatValue <= 99999999.99}
    />
  );
}
