# Continuidade do desenvolvimento

Leia `CODEX.MD` antes de alterar o projeto. O Android é a implementação oficial; o desktop demonstra os mesmos serviços Java sem duplicar regras no navegador.

## Decisões do MVP

- Uma entrada por usuário/data, com intervalo opcional único. Não se pode reabrir uma jornada encerrada na mesma data.
- `data_jornada` é a data local da entrada e permanece gravada nas demais batidas. Timestamps representam instantes em milissegundos; a formatação acontece nas interfaces.
- Batidas de mesmo instante são aceitas e ordenadas também pelo ID; relógio anterior ao último ponto é rejeitado. Durações são calculadas por timestamps, inclusive ao atravessar meia-noite.
- Serviços recebem `Clock`; fakes de repositório existem somente nos testes. `PontoRepository.emTransacao` engloba leitura, validação e inserção.
- Estado/ações/cálculos pertencem ao núcleo. Repositórios traduzem falhas SQLite em erros de persistência; interfaces só mostram sucesso após confirmação da gravação.
- Senhas: PBKDF2-HMAC-SHA256, 600 mil iterações, salt aleatório de 16 bytes, hash de 32 bytes. Não registrar credenciais em logs.
- Android mantém ID autenticado em SharedPreferences. Desktop mantém sessão em memória com cookie opaco, HttpOnly/SameSite e token CSRF; reiniciar servidor encerra a sessão.
- Interfaces em português: cadastro, login, início e histórico. Cadastro bem-sucedido retorna ao login. Totais parciais são atualizados ao entrar na tela, registrar ponto ou pressionar Atualizar.
- Esquema SQLite v1: usuários e registros; e-mail único, foreign keys ativas, tipo limitado ao enum, unicidade usuário/data/tipo. Sem migrations destrutivas.

## Como continuar

1. Executar os testes existentes e consultar os relatórios.
2. Escolher uma melhoria pequena; reproduzir o comportamento desejado com teste falhando.
3. Alterar a menor camada responsável e verificar as duas interfaces quando houver mudança de regra.
4. Atualizar a versão do banco para mudanças de esquema, preservando bases existentes.
5. Registrar o comportamento e a validação no README/TDD do módulo correspondente.

Não adicionar servidor remoto, ORM, framework web ou outra camada de arquitetura para funcionalidades que ainda não foram solicitadas.
