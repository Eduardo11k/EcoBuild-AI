# Plano de Implementação - Integração com API Parceira

Configurar a camada de rede e integrar as rotas de Upload e Análise da API externa no fluxo do aplicativo.

## Alterações Propostas

### Dependências & Build
- Adicionar Retrofit, OkHttp Logging e Kotlinx Serialization ao `libs.versions.toml` e `build.gradle.kts`.
- Aplicar o plugin `plugin.serialization` no Gradle.

### Modelos de Dados (DTOs)
- **[NEW] DTOs da API**: Criar classes para `OrganizationResponse`, `PlanResponse`, `AnalysisResponse` e `MaterialResponse` compatíveis com a especificação OpenAPI.

### Camada de Rede
- **[NEW] ApiService.kt**: Interface Retrofit com:
    - `POST /plans/{organization_id}` (Multipart).
    - `POST /analyses/` (Início da análise).
    - `GET /analyses/{plan_id}` (Busca de resultados).
- **[NEW] NetworkModule.kt**: Singleton para fornecer a instância do Retrofit configurada com o IP `10.0.2.2` (para emulador) ou o IP real fornecido.

### Integração UI/UX
- **UploadScreen**: Atualizar o botão "Start Analysis" para realizar o upload real via API antes de navegar.
- **AnalysisScreen**: Consumir o endpoint de status da análise para mostrar progresso real.

## Plano de Verificação

### Testes de Integração
- Validar a conexão com o Root (`/`) para garantir que a API está acessível do dispositivo.
- Monitorar logs via `HttpLoggingInterceptor` para verificar payloads de upload.

### Manual
- Selecionar um PDF no Upload e verificar se o `plan_id` é retornado pela API.
- Navegar para a tela de análise e validar a transição de estados ("pending" -> "ready").
