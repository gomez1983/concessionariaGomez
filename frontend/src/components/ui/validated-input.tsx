import { useId, useState } from 'react';
import type { InputHTMLAttributes } from 'react';

/** Validação imediata com mensagens em português, sem duplicar regras do domínio. */
export function ValidatedInput(props: InputHTMLAttributes<HTMLInputElement>) {
  const id = useId();
  const [touched, setTouched] = useState(false);
  const [message, setMessage] = useState('');
  function validate(input: HTMLInputElement, show: boolean) {
    input.setCustomValidity('');
    let error = '';
    if (input.required && !input.value.trim()) error = 'Preencha este campo.';
    else if (input.validity.typeMismatch) error = 'Informe um e-mail válido.';
    else if (input.validity.badInput) error = 'Informe um número válido.';
    else if (input.validity.rangeUnderflow) error = `O mínimo permitido é ${input.min}.`;
    else if (input.validity.rangeOverflow) error = `O máximo permitido é ${input.max}.`;
    else if (input.validity.stepMismatch) error = 'Informe um número inteiro.';
    else if (input.validity.patternMismatch) error = 'Confira o formato informado.';
    input.setCustomValidity(error);
    if (show) setMessage(error);
  }
  return (
    <>
      <input
        {...props}
        aria-invalid={!!message}
        aria-describedby={message ? `${id}-error` : undefined}
        onBlur={(event) => {
          setTouched(true);
          validate(event.currentTarget, true);
          props.onBlur?.(event);
        }}
        onInput={(event) => {
          validate(event.currentTarget, touched);
          props.onInput?.(event);
        }}
        onInvalid={(event) => {
          setTouched(true);
          validate(event.currentTarget, true);
        }}
      />
      {message && (
        <span className="field-validation" id={`${id}-error`} aria-live="polite">
          {message}
        </span>
      )}
    </>
  );
}
