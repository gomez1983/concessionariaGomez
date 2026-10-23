# Gomez — painel do funcionário

React + TypeScript + Vite, Tailwind e componentes locais no padrão shadcn/ui. O painel usa a API Spring Boot para estoque, clientes, vendas, indicadores e fotos. Não utiliza dados fictícios nem salva cadastros no localStorage.

## Abrir no IntelliJ

1. Inicie o MySQL pelo Docker Compose do projeto.
2. Recompile e reinicie a API Java pelo IntelliJ. O Flyway aplicará a migration **V009**, que adiciona a tabela das fotos complementares sem modificar as fotos principais existentes.
3. No terminal da pasta `frontend`, execute:

```powershell
npm ci
npm run dev
```

Abra **http://127.0.0.1:5173**. O Vite atualiza a tela quando você salva os arquivos.

A API deve estar em **http://127.0.0.1:8080**. O Vite encaminha `/api` para ela, incluindo imagens e uploads. Para outro endereço, copie `.env.example` para `.env.local`, ajuste `API_TARGET` e reinicie o Vite.

Em produção, sirva o build com um proxy reverso que encaminhe `/api` para a API. `npm run preview` serve apenas o build estático e não deve ser usado como servidor de produção da integração. Não é necessário ampliar as origens CORS para usar o proxy na mesma origem.

## Recursos

- Dashboard de todo o estoque: quantidade, ativos/inativos, zero km, investimento e venda sugerida dos ativos. Os cálculos financeiros ficam no domínio Java.
- Catálogo em cards e lista, com fotos reais, compra, venda sugerida e status.
- Filtros e paginação executados no servidor. A busca textual corresponde ao **modelo**; a marca tem um filtro próprio.
- Cadastro, edição, ativação, inativação e exclusão de veículos e clientes.
- Cadastro de clientes com contatos e endereço opcional.
- Registro e cancelamento de vendas; a API inativa/reativa o veículo.
- Validação imediata dos formulários e mensagens de Problem Details em português.
- Indicadores de carregamento, erros de conexão, nova tentativa e confirmação de exclusões.

## Galeria e upload

Clique na foto do card ou no botão de câmera do veículo.

- Selecione várias imagens ou arraste-as para a área de envio (até 20 arquivos por seleção).
- Cada arquivo pode ter sua descrição: frente, traseira, lateral, interior, painel, bancos, motor etc.
- A primeira imagem enviada se torna a **foto principal**, caso o veículo ainda não tenha uma. As demais são fotos complementares.
- Navegue pelas miniaturas ou setas. Use **Substituir** para trocar a imagem selecionada e o botão de lixeira para removê-la após confirmação.
- JPEG ou PNG, até **10 MB por imagem**, no máximo 12.000 pixels por lado e 40 megapixels. A API também verifica o formato real do arquivo.
- O envio é feito por arquivo. Se um deles falhar, os anteriores permanecem salvos e a seleção mantém somente os arquivos ainda não enviados para permitir uma nova tentativa.

### Onde ficam as fotos?

Os metadados ficam no MySQL (`foto_carro` para a principal e `foto_galeria_carro` para as complementares). Os arquivos ficam no **disco da máquina onde a API roda**, em `catalogo-fotos/`, relativo ao diretório de execução da API. Esse diretório é ignorado pelo Git.

É possível configurar outro caminho com `concessionaria.storage.local.diretorio-fotos` (variável de ambiente `CONCESSIONARIA_STORAGE_LOCAL_DIRETORIO_FOTOS`). O Docker Compose atual contém o MySQL; ele não guarda os arquivos da API. Ao containerizar a API, monte também um volume para as fotos. Faça backup do banco e do diretório de arquivos.

Os arquivos antigos só são removidos após o commit da transação. Em rollback, o novo arquivo é removido e o anterior é preservado. Falhas de limpeza física são registradas no log para recuperação.

## Contratos de integração

| Rota | Uso |
|---|---|
| `GET /dashboard` | Indicadores e distribuição por marca de todo o estoque |
| `/carros`, `/clientes`, `/vendas` | Contratos existentes de gestão |
| `PUT /carros/{id}/foto` | Adicionar/substituir a principal (contrato anterior preservado) |
| `GET /carros/{id}/foto` | Metadados JSON ou imagem, conforme o Accept |
| `DELETE /carros/{id}/foto` | Excluir a principal |
| `GET /carros/{id}/fotos` | Listar fotos complementares |
| `POST /carros/{id}/fotos` | Adicionar uma imagem multipart (`arquivo`, `descricao`) |
| `PUT /carros/{id}/fotos/{fotoId}` | Substituir uma imagem complementar |
| `GET /carros/{id}/fotos/{fotoId}/conteudo` | Ler o binário |
| `DELETE /carros/{id}/fotos/{fotoId}` | Excluir a imagem complementar |

## Visual e preenchimento dos formulários

- O painel segue a referência `concessionaria-dashboard-ui.png`: cores escuras, destaque ciano, navegação lateral compacta e logotipos das fabricantes. O botão de sol/lua no topo alterna entre modo claro e escuro e salva somente essa preferência no navegador (`gomez:theme`). O tema padrão é escuro.
- Campos de compra, venda e filtros monetários formatam reais durante a digitação. Digite `1500000` para obter `R$ 1.500.000,00`; use vírgula para os centavos. A API recebe o valor numérico, sem a máscara.
- CPF/CNPJ, telefone com DDD e CEP recebem pontuação automaticamente. Telefone aceita fixo com oito dígitos ou celular com nove, além do DDD.
- Ao completar os oito dígitos do CEP, o cadastro consulta o [ViaCEP](https://viacep.com.br/). Somente o CEP é enviado ao serviço. Rua, bairro, cidade e UF são preenchidos; número e complemento permanecem para preenchimento. País é exibido como Brasil, sem alterar o contrato de endereço da API.
- Endereços continuam editáveis. CEP inexistente, endereço geral sem rua/bairro ou indisponibilidade da consulta permitem preenchimento manual; o botão de busca permite repetir a consulta. Requisições antigas são canceladas ao trocar o CEP ou fechar o formulário.
- Documentos já cadastrados sem pontuação continuam reconhecidos pela verificação de duplicidade da API.

### Comandos de validação

```powershell
npm run build
npm test
npm run format
```

Arquivos principais: `src/App.tsx` (navegação e dashboard), `src/components/Stock.tsx`, `Customers.tsx`, `Sales.tsx`, `VehicleGallery.tsx`, `VehicleForm.tsx`, `src/lib/api.ts` (HTTP e Problem Details), `src/types.ts` (contratos) e `src/styles.css` (visual responsivo).

A carga destrutiva `afterMigrate.sql` está apenas em `src/test/resources/db/testdata`: ela não roda na API normal. Os testes Java podem usar outro banco terminando em `test` por meio da variável `TEST_DB_URL`. Nunca aponte os testes para o banco principal.

## Fotos padrão de referência

O catálogo inclui 14 fotos dos modelos cadastrados, baixadas do Wikimedia Commons em `public/images/vehicles/`. Elas aparecem somente quando o veículo não tem uma foto principal enviada pelo usuário. A imagem padrão é identificada como referência: ano, versão e cor podem variar. Os arquivos do usuário e os registros do banco não são substituídos.

Nos cards, a imagem fica limitada a uma moldura com recorte central; o título e os dados ficam fora dessa moldura. Na galeria, a imagem é exibida inteira e preserva sua proporção (`object-fit: contain`).

Fontes, autores e licenças estão em `public/creditos-fotos.html`, acessível pelo rodapé do painel. O mapeamento por marca/modelo está em `src/data/referencePhotos.json`. O script `scripts/download-reference-photos.py`, executado a partir da raiz do repositório, permite recuperar os arquivos e os créditos sem escrever no banco de dados.
