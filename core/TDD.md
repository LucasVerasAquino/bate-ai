# Evidências de TDD — domínio Java

Os testes foram escritos antes das regras. Os serviços inicialmente retornavam
erro ou listas vazias para permitir a compilação dos contratos compartilhados.
Os repositórios em memória e o relógio controlado existem somente nos testes.

## RED

Em 30/09/2026 às 22:49:39 UTC, `:core:test` executou 29 testes contra os serviços
sem implementação e produziu 23 falhas:

| Classe | Testes | Falhas iniciais |
| --- | ---: | ---: |
| AuthServiceTest | 8 | 5 |
| PontoServiceTest | 11 | 9 |
| JornadaServiceTest | 10 | 9 |

Falhas incluíram cadastro/login válidos, sequência de ponto, jornada noturna,
estados, histórico e totais. Os demais casos já aceitavam o resultado vazio ou
de erro dos serviços provisórios.

## GREEN e revisão

- Implementação de autenticação: os 8 testes de `AuthServiceTest` passaram
  usando PBKDF2-HMAC-SHA256 real com 600 mil iterações, sem substituir a função
  criptográfica por um fake.
- Implementação de pontos: os 11 testes de `PontoServiceTest` passaram com
  validação e gravação na mesma transação, incluindo duas entradas concorrentes.
- Implementação de jornadas: a suíte completa passou com **29 testes**, incluindo
  os 10 cenários de cálculo/histórico. Esta verificação usou `javac --release 17`
  e `org.junit.runner.JUnitCore` com os mesmos JARs JUnit 4.13.2 do cache Gradle,
  em `core/build/manual-test`, para não disputar o build Android simultâneo.
- Revisão: simplificação do relógio de teste e ajuste de concordância da mensagem
  de sucesso para todos os tipos de ponto; contratos públicos preservados.
  Após a revisão, a suíte foi compilada novamente e terminou com `OK (29 tests)`.
- Na validação integrada final, `:core:test` também passou pelo Gradle com os
  mesmos 29 testes, sem falhas ou erros, gerando os relatórios XML e HTML.

## Execução reproduzível

Na raiz, execute `powershell -ExecutionPolicy Bypass -File scripts/testar.ps1`.
Relatórios Gradle ficam em `core/build/reports/tests/test/index.html`.

Os testes SQLite reais, restauração de sessão e fluxos das interfaces ficam nos
módulos Android e demo; o core testa regras, isolamento e o contrato transacional.
