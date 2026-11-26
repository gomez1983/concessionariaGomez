// Aplica o tema salvo antes da primeira pintura, para evitar o "piscar" de tema.
// Fica num arquivo próprio (e não inline no index.html) para a CSP aceitar só script-src 'self'.
try {
  document.documentElement.dataset.theme =
    localStorage.getItem('gomez:theme') === 'light' ? 'light' : 'dark';
} catch {
  document.documentElement.dataset.theme = 'dark';
}
