# Bate Aí

Aplicativo acadêmico de ponto: **Android nativo em Java**, com uma demonstração funcional no navegador desktop. Cadastro, autenticação, batidas e histórico funcionam offline e persistem em SQLite. As duas interfaces usam as mesmas regras Java.

## Experimentar no computador

Na raiz do projeto, abra `iniciar-demo.cmd` ou execute:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\iniciar-demo.ps1
```

Depois abra **http://127.0.0.1:8080**. Mantenha o terminal aberto durante o uso; `Ctrl+C` encerra a demonstração. A liberação de scripts vale somente para esse processo PowerShell e não altera a política do Windows.

Crie uma conta e faça login. Não existem credenciais predefinidas. Os dados ficam em `data/bateai-demo.db`, fora das pastas de compilação. Reiniciar o servidor exige novo login, preservando os cadastros e pontos.

**O computador e o celular têm bancos independentes, sem sincronização.** A demonstração escuta somente no próprio computador e não é um site publicado na internet.

## Gerar e instalar o APK

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\gerar-apk.ps1
```

O arquivo será gerado em `app/build/outputs/apk/debug/app-debug.apk`. Copie-o para um celular com **Android 8.0 ou mais recente** e abra-o, autorizando a instalação pela origem utilizada quando o Android solicitar. É um APK debug para teste acadêmico.

Com um dispositivo em depuração USB ou emulador conectado:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r .\app\build\outputs\apk\debug\app-debug.apk
```

A sessão Android é restaurada ao reabrir. Sair da conta preserva o histórico e qualquer jornada pendente. Desinstalar o aplicativo ou limpar seus dados apaga a base local; não há backup em nuvem.

## Preparação do ambiente

- JDK 25 recomendado. `scripts/ambiente.ps1` encontra o Corretto 25 ou o runtime do Android Studio; para outra instalação, configure `BATEAI_JAVA_HOME` com um JDK 17 a 25.
- Android SDK Platform 37 e Build Tools 36.0.0 para compilar o APK. O SDK é localizado em `ANDROID_HOME` ou `%LOCALAPPDATA%\Android\Sdk`.
- Gradle Wrapper 9.3.1 e Android Gradle Plugin 9.1.1 estão fixados. O wrapper verifica o checksum da distribuição.
- O primeiro build precisa de internet para baixar ferramentas e dependências. O aplicativo e a demonstração não dependem de rede depois de preparados. O cache Gradle usado pelos scripts fica em `.gradle-user-home`.
- Não é necessário Node, npm, banco externo nem servidor remoto para executar o projeto.

Para importar no Android Studio, abra a pasta raiz, selecione JDK 25 em **Gradle JDK** e confirme a localização do SDK. Os scripts PowerShell configuram o próprio ambiente; essa configuração não altera o JDK global do computador.

### Emulador de teste

Com o Android Emulator instalado pelo Android Studio e virtualização habilitada:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\preparar-emulador.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\iniciar-emulador.ps1
```

A preparação baixa aproximadamente 844 MB de uma imagem oficial API 36 e verifica seu checksum. Imagem e dispositivo virtual ficam em `.tools`, dentro do projeto. Para testar sem janela, acrescente `-SemJanela` ao último comando. Esse AVD é local ao projeto, não aparece automaticamente no Device Manager do Android Studio.

## Testes

Regras Java, integração SQLite/HTTP da demonstração, Android Lint e geração do APK:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\testar.ps1
```

Com o emulador iniciado ou um aparelho conectado, execute os testes Android de banco, sessão e fluxos principais:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\testar-android.ps1
```

Relatórios HTML ficam em `core/build/reports/tests/test`, `demo-web/build/reports/tests/test` e `app/build/reports/androidTests/connected`. Os testes usam bases isoladas e não alteram a base da demonstração.

### Roteiro de demonstração

1. Crie uma conta, entre e confira o estado sem jornada.
2. Registre a entrada; o status muda para **Trabalhando**.
3. Inicie o intervalo; o tempo de trabalho para de acumular. Use **Atualizar** para consultar os totais.
4. Finalize o intervalo e confirme o encerramento da jornada.
5. Abra o histórico e confira as quatro batidas e o desconto do intervalo.
6. Saia, entre novamente e feche/reabra o aplicativo ou servidor: os registros permanecem.
7. Crie outra conta para testar entrada/saída sem intervalo e isolamento de dados.
8. Repita o uso com a conexão de internet desligada; no desktop, mantenha o processo Java em execução.

Os horários de exemplo de 8h e as jornadas noturnas são verificados com relógio controlado nos testes. A interface registra sempre a hora real, sem campo de alteração manual.

## Regras e organização

Uma jornada iniciada por dia, com no máximo um intervalo opcional. Durante intervalo aberto, é preciso retornar antes de sair. Uma jornada iniciada antes da meia-noite continua vinculada à data de entrada; depois de encerrá-la, pode-se iniciar a jornada do novo dia. Não existe fechamento automático.

| Módulo | Conteúdo |
|---|---|
| `core` | Domínio Java, autenticação PBKDF2, estados/cálculos e contratos de repositório. |
| `app` | Android Java/XML, SQLiteOpenHelper e sessão SharedPreferences. |
| `demo-web` | HTTP local, HTML/CSS e SQLite JDBC. |

Não há dependências de runtime externas no núcleo ou APK. SQLite JDBC está restrito à demonstração; JUnit e AndroidX Test são usados somente em testes. O banco está na versão 1. Versões incompatíveis são recusadas sem apagar dados.

Validação da entrega: [50 testes aprovados, artefatos e capturas](docs/VALIDACAO.md). Decisões e continuidade: [docs/DESENVOLVIMENTO.md](docs/DESENVOLVIMENTO.md). Evidências TDD ficam nos arquivos `TDD.md` dos módulos.

Ficam para versões futuras: edição de batidas, recuperação de senha, metas, banco de horas, exportação, sincronização e publicação online.
