# TDD da demonstração local

Os testes foram escritos antes da implementação da persistência JDBC e do servidor HTTP.

- `SqliteRepositoryTest`: persistência após reabrir, isolamento entre contas, e-mail único, chaves estrangeiras, rollback, concorrência e recusa de versão futura sem apagar dados.
- `ServidorWebTest`: cadastro/login/ponto/histórico/logout reais por HTTP, confirmação de saída, escape HTML, cookies, CSRF, origem, limites de requisição e sessão após reiniciar o processo.

As classes inicialmente continham apenas os contratos, com construtores que lançavam `UnsupportedOperationException`. O RED executou 11 testes e produziu 11 falhas esperadas. Depois da implementação, os 11 testes passaram. A suíte foi ampliada com a disputa entre início de intervalo e saída: somente uma transição é confirmada. A suíte ampliada passou com 12 testes.

## Regressão encontrada no navegador real

O roteiro de cadastro do Chrome falhou com “Origem não permitida”. A política `no-referrer` suprimia a origem de formulários enviados pelo navegador, embora os testes do cliente Java passassem. A política passou a `same-origin`, preservando a verificação estrita de origem e o token CSRF. [Comportamento do cabeçalho Origin](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Origin).

O roteiro `scripts/verificar-web.cjs` então passou no Chrome: cadastro, login, entrada, intervalo, retorno, confirmação da saída, histórico, logout e novo login. As capturas ficam em `artifacts/web-*.png`. Esse teste opcional usa Node 20.17+ com `--experimental-websocket`, Chrome headless/CDP na porta 9222 e uma instância separada da demonstração na porta 8081 com banco de teste; nenhuma biblioteca npm é necessária.

Comando da suíte: `gradlew.bat :demo-web:test`.
