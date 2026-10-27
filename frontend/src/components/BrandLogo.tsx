import { CarFront } from 'lucide-react';
import type { CSSProperties } from 'react';

const brands: Record<string, [string, string]> = {
  chevrolet: ['chevrolet', '#e2bc64'],
  ford: ['ford', '#62a5ef'],
  mitsubishi: ['mitsubishi', '#ed6b70'],
  volkswagen: ['volkswagen', '#83b5e7'],
  ferrari: ['ferrari', '#e9d36d'],
  audi: ['audi', '#b9c7d6'],
  bmw: ['bmw', '#78baff'],
  mercedesbenz: ['mercedes', '#bccddf'],
  mercedes: ['mercedes', '#bccddf'],
  porsche: ['porsche', '#d9b76b'],
  toyota: ['toyota', '#f17c81'],
  honda: ['honda', '#ed7c7c'],
  hyundai: ['hyundai', '#79acd5'],
  byd: ['byd', '#e05d64'],
  bid: ['byd', '#e05d64'],
  bugatti: ['bugatti', '#4da9ff'],
};
export function BrandLogo({ marca }: { marca: string }) {
  const brand = brands[marca.toLowerCase().replace(/[^a-z]/g, '')];
  if (!brand) return <CarFront className="brand-logo-fallback" size={19} aria-hidden="true" />;
  return (
    <span
      className="automaker-logo"
      title={marca}
      aria-hidden="true"
      style={
        {
          '--logo-image': `url('/brand-logos/${brand[0]}.svg')`,
          '--brand-color': brand[1],
        } as CSSProperties
      }
    />
  );
}
