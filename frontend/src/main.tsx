import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { AuthGate, AuthProvider } from './components/AuthProvider';
import './styles.css';
import './theme.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AuthProvider>
      {/* A chave pelo e-mail descarta todo o estado da tela quando outro usuário entra */}
      <AuthGate>{(email) => <App key={email} />}</AuthGate>
    </AuthProvider>
  </React.StrictMode>,
);
