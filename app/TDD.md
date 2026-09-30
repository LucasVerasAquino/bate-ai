# Validação Android

## Primeiro ciclo: persistência e sessão

`PersistenciaAndroidTest` foi escrito antes de `DatabaseHelper`, dos repositórios Android e de `SessaoLocal`. O primeiro comando de compilação não pôde executar os testes: o Gradle Wrapper ainda estava sendo preparado pelo trabalho de infraestrutura. Portanto, essa tentativa é um bloqueio de ambiente, **não** evidência de um teste RED executado.

Os testes usam SQLite real e SharedPreferences no Android e verificam:

- persistência após fechar/reabrir o banco e isolamento entre contas;
- rollback de uma transação que falha após inserir;
- unicidade de e-mail/ponto e rejeição de usuário inexistente;
- restauração de sessão sem apagar a jornada no logout;
- duas entradas simultâneas, aceitando somente uma batida.

## Fluxo importante da interface

`FluxoAndroidTest` cobre cadastro → login → entrada → recriação da Activity → histórico → logout → novo login → intervalo → retorno → saída confirmada. Também verifica a recuperação segura de uma sessão cujo usuário não existe mais. As verificações usam APIs nativas da instrumentação Android e AndroidX Test apenas como executor JUnit.

O teste de cadastro foi atualizado antes da correção de navegação para exigir retorno ao login, e-mail preenchido e sessão ainda encerrada. A execução e o resultado desse ciclo estão registrados abaixo.

## Ambiente e comandos

Usar o JDK 25, SDK Android configurado e um emulador/dispositivo autorizado. Neste workspace, a preparação fornece `scripts/ambiente.ps1` e um emulador API 36. Os diretórios de cache e configuração Android ficam dentro do projeto.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/testar-android.ps1
```

Para diagnóstico, as tarefas são `:app:connectedDebugAndroidTest`, `:app:assembleDebug` e `:app:lintDebug`. A compilação inicial das classes Java, XML e DEX passou; a assinatura precisou do diretório Android configurado no workspace para evitar o bloqueio de escrita da chave debug no perfil de usuário.

## Resultados finais

Os cinco testes de persistência passaram em SQLite real no emulador API 36. O teste de cadastro inicialmente falhou ao esperar a tela de login: a implementação abria a Home diretamente. A navegação foi corrigida depois dessa falha observada.

Um teste adicional reproduziu o fuso incorreto com o processo aberto: esperava 01/10, mas a entrada recebia 30/09. O relógio Android passou a consultar o fuso atual sem alterar as datas das batidas anteriores. Também há cobertura de reabertura da Activity pelo launcher.

A primeira execução completa após essas correções passou por cadastro, login, entrada, recriação, histórico, logout, novo login e intervalo. A confirmação final exigiu corrigir o seletor do teste para o identificador nativo do botão de diálogo, pois a apresentação em maiúsculas depende do tema. O teste continua exigindo confirmação explícita e estado encerrado após o clique.

Na execução final, os **9 testes instrumentados passaram**, com modo avião ativado no emulador API 36. Android Lint terminou com `No issues found.`; a compatibilidade do tema API 26 e a navegação de retorno nativa foram corrigidas. O registro consolidado final fica em [docs/VALIDACAO.md](../docs/VALIDACAO.md).
