# Validação do MVP

Validação realizada em 30/09/2026. O APK debug e a distribuição desktop foram gerados a partir do código deste workspace.

## Resultados

| Verificação | Resultado |
|---|---|
| Domínio Java (`core`) | 29 testes, nenhuma falha |
| SQLite JDBC e HTTP real (`demo-web`) | 12 testes, nenhuma falha |
| SQLite, sessão e interface Android | 9 testes instrumentados, nenhuma falha |
| Android Lint | `No issues found.` |
| APK debug | Compilado e assinatura v2 verificada |
| Inicialização pelo script documentado | Página de login em `127.0.0.1:8080` respondeu HTTP 200 |
| Navegador real | Fluxo completo aprovado no Chrome, com requisições externas bloqueadas |

Total: **50 testes automatizados aprovados**, além do roteiro de navegador. Os relatórios gerados estão em:

- [Domínio](../core/build/reports/tests/test/index.html).
- [SQLite e HTTP desktop](../demo-web/build/reports/tests/test/index.html).
- [Android instrumentado](../app/build/reports/androidTests/connected/debug/index.html).
- [Android Lint](../app/build/reports/lint-results-debug.html).

## Cobertura e ambiente

As suítes verificam cadastro, e-mail normalizado/duplicado, autenticação, isolamento entre contas, sequências de ponto, intervalo opcional, rejeição de batidas inválidas, cálculos, meia-noite, relógio regressivo, concorrência e rollback. A persistência é verificada fechando e reabrindo bancos SQLite reais nas duas plataformas. A sessão Android é recuperada, e o logout preserva os pontos.

Os testes Android foram executados no AVD local `BateAi_API36`, Android 16/API 36, com modo avião ativado. O fluxo de interface cobre cadastro, login, entrada, recriação da Activity, histórico, logout, novo login, intervalo, retorno e confirmação de saída. Há regressões para sessão inválida, reabertura da Activity e mudança de fuso com o processo aberto.

Uma verificação adicional por toques no emulador confirmou cadastro/login, entrada e histórico. Após `am force-stop br.com.bateai` e nova abertura pelo launcher, a sessão e a jornada em andamento foram restauradas, ainda em modo avião.

O navegador percorreu cadastro, login, entrada, intervalo, retorno, confirmação de saída, histórico, logout e novo login. A validação usou uma instância separada na porta 8081 com banco de teste, bloqueando todo acesso externo. Também verificou a apresentação em largura estreita, incluindo acesso ao logout. O código reproduzível está em [verificar-web.cjs](../scripts/verificar-web.cjs); requer Chrome com CDP na porta 9222 e Node com WebSocket nativo (`node --experimental-websocket scripts/verificar-web.cjs` no Node 20.17).

Compilação: JDK Corretto 25.0.2, Gradle 9.3.1, AGP 9.1.1, SDK 37 e Build Tools 36.0.0. O APK não declara permissões, inclusive `INTERNET`, e não contém bibliotecas externas de runtime. AndroidX Test fica restrito à instrumentação. A base desktop é independente da base Android.

## Reproduzir

Na raiz do projeto:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/testar.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/iniciar-emulador.ps1
# Aguarde o Android terminar de iniciar.
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/testar-android.ps1
```

Para gerar também a distribuição desktop, use um PowerShell com os scripts liberados para esse processo:

```powershell
. .\scripts\ambiente.ps1
.\gradlew.bat :demo-web:installDist :demo-web:distZip --offline --no-daemon
```

O modo `--offline` exige o cache já preparado pelo primeiro build. Os comandos de testes, instalação e uso completo estão no [README](../README.md).

## Artefatos e capturas

- [APK debug instalável](../app/build/outputs/apk/debug/app-debug.apk).
- [Distribuição desktop ZIP](../demo-web/build/distributions/demo-web.zip).
- [Login Android](../artifacts/android-login.png).
- [Jornada em andamento no Android](../artifacts/android-inicio.png).
- [Histórico Android após reabrir o processo](../artifacts/android-historico.png).
- [Início no navegador](../artifacts/web-inicio.png).
- [Histórico completo no navegador](../artifacts/web-historico-completo.png).
- [Apresentação em largura estreita](../artifacts/web-mobile.png).

SHA-256 do APK validado:

```text
A32D89D0908ACD6352682F36B3AE2E7A40F47E73C611DE101C3DF69DE268EA4E
```

Os artefatos, capturas e relatórios são arquivos locais ignorados pelo Git; são regenerados pelos comandos de build/teste e pelo roteiro visual. Não confundir os bancos sintéticos de teste com contas de demonstração prontas para uso: o usuário deve criar sua própria conta.

## Limites desta validação

Não houve teste em aparelho físico nem execução em Android API 26; a compatibilidade mínima foi configurada e verificada pelo Lint, enquanto a execução instrumentada usou API 36. Não foi realizado reinício completo do dispositivo. O APK é debug, destinado a avaliação do MVP. Publicação, assinatura de distribuição e funcionalidades futuras permanecem fora desta entrega.

Os ciclos RED/GREEN e as correções observadas estão documentados nos arquivos [core/TDD.md](../core/TDD.md), [app/TDD.md](../app/TDD.md) e [demo-web/TDD.md](../demo-web/TDD.md).
