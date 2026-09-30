# CODEX.md — Bate Aí

## 1. Objetivo

Este documento define as regras obrigatórias de arquitetura, desenvolvimento, testes e manutenção do aplicativo **Bate Aí**.

O Bate Aí é um aplicativo acadêmico de gerenciamento de pontos de trabalho.

O objetivo principal é:

- funcionar corretamente;
- ser simples;
- ser fácil de entender;
- funcionar offline;
- persistir os dados localmente;
- possuir autenticação local;
- ser desenvolvido com TDD;
- evitar dependências externas;
- possuir arquitetura organizada sem overengineering.

Este NÃO é um projeto empresarial e NÃO precisa ser preparado para escala.

Simplicidade tem prioridade sobre abstrações desnecessárias.

---

# 2. Stack obrigatória

O projeto deve utilizar:

- Java;
- Android SDK;
- SQLite;
- SQLiteOpenHelper;
- JUnit para testes;
- APIs nativas do Java e Android sempre que possível.

Evitar bibliotecas externas.

Não adicionar dependências sem necessidade objetiva.

Antes de adicionar qualquer biblioteca, verificar se a funcionalidade pode ser implementada utilizando:

1. Java Standard Library;
2. Android SDK;
3. código simples próprio.

Dependências adicionadas apenas para reduzir algumas linhas de código são proibidas.

---

# 3. Plataforma

A aplicação principal é Android.

O APK Android é a implementação oficial do Bate Aí.

O projeto deve permitir que regras de negócio sejam testadas fora do Android sempre que possível.

O navegador pode ser utilizado como ambiente auxiliar de teste ou demonstração.

Não transformar o projeto em uma aplicação Web complexa apenas para suportar navegador.

Não adicionar frameworks multiplataforma.

A lógica de negócio deve permanecer em Java puro sempre que possível para facilitar testes independentes da interface Android.

---

# 4. Princípios fundamentais

Sempre seguir esta ordem de prioridade:

1. Correção;
2. Simplicidade;
3. Testabilidade;
4. Legibilidade;
5. Baixo acoplamento;
6. Performance suficiente;
7. Reutilização.

Não otimizar prematuramente.

Não criar infraestrutura para problemas que o projeto não possui.

Não construir arquitetura para uma escala futura hipotética.

---

# 5. TDD obrigatório

Todo comportamento relevante deve ser desenvolvido utilizando:

RED → GREEN → REFACTOR.

## RED

Primeiro criar um teste que descreva o comportamento esperado.

O teste deve falhar inicialmente.

## GREEN

Implementar apenas o código necessário para fazer o teste passar.

## REFACTOR

Depois que os testes passarem:

- remover duplicações;
- melhorar nomes;
- simplificar lógica;
- reduzir complexidade;
- preservar o comportamento.

Nunca alterar comportamento durante refatoração.

---

# 6. Regra para novas funcionalidades

Toda nova funcionalidade deve seguir:

1. entender o requisito;
2. identificar regra de negócio;
3. escrever teste;
4. verificar teste falhando;
5. implementar solução mínima;
6. executar testes;
7. refatorar;
8. executar todos os testes novamente.

Não implementar grandes blocos antes dos testes.

---

# 7. Pirâmide de testes

Priorizar:

### 1. Testes unitários

Devem representar a maioria dos testes.

Testar:

- regras de ponto;
- validações;
- cálculos;
- autenticação;
- horários;
- estados;
- regras de usuário.

### 2. Testes de integração

Utilizar apenas onde necessário.

Exemplos:

- Repository + SQLite;
- persistência de usuário;
- persistência de registros de ponto.

### 3. Testes de interface

Utilizar somente para fluxos importantes.

Não testar visualmente cada detalhe da interface.

---

# 8. Regra de ouro para testes

Um teste deve verificar comportamento.

Evitar testes que apenas confirmem implementação interna.

Preferir:

`registrarEntrada_quandoNaoExisteEntrada_criaRegistro()`

em vez de:

`testeMetodo1()`

Os nomes dos testes devem explicar:

- ação;
- condição;
- resultado esperado.

---

# 9. Arquitetura

Utilizar arquitetura simples em camadas.

Estrutura recomendada:

```text
app/
└── src/main/java/.../bateai/
    ├── model/
    ├── repository/
    ├── service/
    ├── ui/
    └── database/
```

Não criar camadas adicionais sem necessidade.

---

# 10. Responsabilidades

## model

Contém objetos do domínio.

Exemplos:

- Usuario;
- RegistroPonto;
- Jornada;
- TipoPonto.

Models NÃO devem depender de:

- Activity;
- Fragment;
- View;
- SQLite;
- contexto Android.

---

## repository

Responsável por acesso e persistência de dados.

Exemplos:

- UsuarioRepository;
- PontoRepository.

A interface do aplicativo nunca deve executar SQL diretamente.

---

## database

Contém infraestrutura SQLite.

Exemplos:

- DatabaseHelper;
- criação das tabelas;
- upgrades;
- consultas básicas.

Utilizar `SQLiteOpenHelper`.

Não utilizar ORM.

Room, Hibernate e similares não devem ser utilizados.

---

## service

Contém regras de negócio.

Exemplos:

- AuthService;
- PontoService;
- JornadaService.

Services não devem conhecer componentes visuais.

---

## ui

Contém componentes Android.

Exemplos:

- Activity;
- Fragment;
- Adapter;
- componentes visuais.

A UI deve:

- capturar ações;
- chamar services;
- exibir resultados.

A UI NÃO deve conter regras de negócio importantes.

---

# 11. Dependências entre camadas

Fluxo preferencial:

```text
UI
 ↓
Service
 ↓
Repository
 ↓
SQLite
```

Models podem circular entre as camadas.

Evitar dependências circulares.

Repository não conhece UI.

Service não conhece Activity.

Model não conhece Android.

---

# 12. Funcionalidades principais

O Bate Aí deve oferecer, no mínimo:

- cadastro de usuário;
- login;
- logout;
- registro de entrada;
- registro de saída;
- registro de início de intervalo;
- registro de fim de intervalo;
- histórico de pontos;
- visualização da jornada diária;
- funcionamento offline;
- persistência após fechar o aplicativo.

Funcionalidades adicionais são opcionais.

---

# 13. Registro de ponto

Os principais tipos de ponto são:

```text
ENTRADA
INICIO_INTERVALO
FIM_INTERVALO
SAIDA
```

Preferir `enum`.

Exemplo:

```java
public enum TipoPonto {
    ENTRADA,
    INICIO_INTERVALO,
    FIM_INTERVALO,
    SAIDA
}
```

Evitar strings soltas como:

```java
"entrada"
"saida"
```

---

# 14. Regras básicas do ponto

As regras devem ser validadas pelo domínio/service.

Exemplo de sequência válida:

```text
ENTRADA
→ INICIO_INTERVALO
→ FIM_INTERVALO
→ SAIDA
```

Não permitir:

- saída sem entrada;
- segundo registro de entrada aberto;
- finalizar intervalo sem iniciá-lo;
- iniciar dois intervalos simultaneamente;
- registrar ponto para usuário inexistente.

As regras exatas devem possuir testes.

---

# 15. Horários

Armazenar datas e horários de maneira consistente.

Preferir timestamps.

O banco deve armazenar um valor que não dependa de formatação visual.

Formatação deve acontecer apenas na UI.

Nunca salvar:

```text
"30 de setembro às 08:10"
```

como valor principal no banco.

Salvar timestamp e formatar posteriormente.

---

# 16. SQLite

Utilizar SQLite local como fonte persistente oficial.

Não utilizar servidor remoto.

Não exigir internet.

Não sincronizar com nuvem.

Não criar mecanismos de sincronização.

---

# 17. Banco de dados inicial

Estrutura mínima recomendada:

```text
usuarios
---------
id
nome
email
senha_hash
salt
criado_em
```

```text
registros_ponto
---------------
id
usuario_id
tipo
timestamp
```

Adicionar campos somente quando houver requisito real.

---

# 18. Chaves

Utilizar identificadores numéricos locais.

Preferir:

```text
INTEGER PRIMARY KEY AUTOINCREMENT
```

Não utilizar UUID sem necessidade.

---

# 19. Autenticação

A autenticação é local.

Não utilizar:

- Firebase;
- OAuth;
- Auth0;
- APIs externas.

O aplicativo deve funcionar completamente offline.

---

# 20. Senhas

Nunca armazenar senha em texto puro.

Utilizar recursos nativos do Java para gerar hash seguro.

Preferir:

- PBKDF2;
- salt aleatório;
- APIs de `javax.crypto`.

Cada usuário deve possuir seu próprio salt.

Não inventar algoritmo criptográfico próprio.

---

# 21. Sessão

A sessão atual pode utilizar armazenamento local simples.

Preferir `SharedPreferences` apenas para informações pequenas de sessão.

Exemplo:

```text
usuario_logado_id
```

Dados principais continuam no SQLite.

---

# 22. Offline first

Todas as funcionalidades principais devem funcionar sem internet.

O usuário deve conseguir:

- abrir o aplicativo;
- entrar;
- bater ponto;
- consultar histórico;
- fechar;
- reabrir;

sem conexão de rede.

Internet não pode ser requisito para o fluxo principal.

---

# 23. Persistência

Dados não podem desaparecer ao:

- fechar Activity;
- fechar aplicativo;
- reiniciar dispositivo.

SQLite é responsável pelos dados permanentes.

Estado temporário da UI não substitui persistência.

---

# 24. Interface

A interface deve ser simples.

Não criar design system complexo.

Priorizar:

- clareza;
- botões grandes;
- fluxo direto;
- feedback visual;
- poucas telas.

---

# 25. Telas mínimas

O projeto pode utilizar:

```text
Login
Cadastro
Home
Bater Ponto
Histórico
```

Evitar criar telas sem necessidade.

---

# 26. Home

A Home deve apresentar informações relevantes de forma direta.

Exemplo:

```text
Olá, Lucas

Status:
Trabalhando

Último ponto:
Entrada - 08:03

[BATER PONTO]

[HISTÓRICO]

[SAIR]
```

---

# 27. Feedback

Toda operação importante deve informar sucesso ou erro.

Exemplos:

```text
Entrada registrada às 08:03.
```

```text
Você precisa registrar uma entrada primeiro.
```

Nunca falhar silenciosamente.

---

# 28. Tratamento de erros

Não utilizar:

```java
catch (Exception e) {
}
```

Todo erro deve:

- ser tratado;
- retornar estado conhecido;
- ou ser registrado durante desenvolvimento.

Não utilizar exceptions como fluxo normal de negócio.

---

# 29. Null

Evitar `null` quando possível.

Métodos devem ter contratos previsíveis.

Não retornar `null` silenciosamente quando:

- lista vazia;
- Optional;
- resultado conhecido;

for mais apropriado.

Não adicionar abstrações apenas para evitar todo uso de `null`.

---

# 30. Métodos

Métodos devem ser pequenos e possuir responsabilidade clara.

Preferir:

```java
registrarEntrada(usuarioId)
```

a métodos genéricos como:

```java
processar(int x, String y, boolean z)
```

Evitar parâmetros booleanos misteriosos.

---

# 31. Classes

Uma classe deve possuir uma responsabilidade principal.

Não criar `Utils` gigantes.

Não criar `Manager` responsável por todo o aplicativo.

Não criar abstrações sem uso concreto.

---

# 32. Interfaces

Não criar interface para toda classe automaticamente.

Criar interfaces quando facilitarem:

- testes;
- substituição de persistência;
- separação clara de responsabilidade.

Repository é um bom candidato.

Models simples não precisam de interfaces.

---

# 33. SOLID

Aplicar SOLID com pragmatismo.

SOLID é ferramenta, não objetivo.

Se uma abstração torna o código mais difícil de entender sem benefício concreto, não utilizá-la.

O projeto deve continuar pequeno.

---

# 34. DRY

Evitar duplicação relevante.

Não abstrair código apenas porque duas linhas são parecidas.

Duplicação pequena pode ser preferível a uma abstração ruim.

---

# 35. KISS

Manter tudo tão simples quanto possível.

Se existem duas soluções corretas, escolher a menor e mais fácil de entender.

---

# 36. YAGNI

Não implementar funcionalidades futuras hipotéticas.

Não criar:

- sistema de plugins;
- microservices;
- sincronização;
- cloud;
- cache distribuído;
- event bus;
- arquitetura multi-module complexa;
- abstrações para múltiplos bancos.

O aplicativo é acadêmico.

---

# 37. Dependências proibidas por padrão

Evitar:

- Firebase;
- Retrofit;
- OkHttp;
- Room;
- Dagger;
- Hilt;
- RxJava;
- Realm;
- Hibernate;
- Spring;
- bibliotecas de autenticação;
- frameworks multiplataforma.

Uma exceção exige necessidade concreta.

---

# 38. Concorrência

Não introduzir concorrência complexa sem necessidade.

Operações SQLite devem ser simples e previsíveis.

Evitar:

- threads manuais desnecessárias;
- filas;
- schedulers;
- event buses.

---

# 39. Logging

Logs são permitidos durante desenvolvimento.

Nunca registrar:

- senha;
- hash completo;
- credenciais;
- dados sensíveis desnecessários.

Remover logs de depuração inúteis.

---

# 40. Browser / modo de demonstração

Android continua sendo a plataforma principal.

Regras de negócio devem permanecer desacopladas do Android para permitir testes em Java puro.

Se for necessário demonstrar comportamento no navegador, criar apenas uma camada auxiliar mínima.

Não duplicar regras de negócio no navegador.

Não criar uma segunda aplicação completa.

Não adicionar frameworks Web grandes.

O modo browser existe para teste/demonstração, não como produto oficial.

---

# 41. Testabilidade

Sempre que possível, código de domínio deve funcionar sem:

- Context;
- Activity;
- View;
- banco real;
- dispositivo Android.

Exemplo:

```java
PontoService service =
    new PontoService(fakeRepository);
```

Isso permite testar regras rapidamente.

---

# 42. Fake repositories

Para testes unitários, repositories podem possuir implementação em memória.

Exemplo:

```text
FakeUsuarioRepository
FakePontoRepository
```

Eles devem existir apenas em código de teste.

Não adicionar frameworks de mocking se uma implementação fake simples resolver.

---

# 43. Mocking

Evitar bibliotecas de mocking.

Preferir objetos fake escritos em Java.

Exemplo:

```java
class FakePontoRepository implements PontoRepository {
    ...
}
```

Para este projeto isso é suficiente.

---

# 44. Organização dos testes

Estrutura recomendada:

```text
src/test/java/.../bateai/
    service/
    repository/
    model/
```

Espelhar aproximadamente a estrutura do código principal.

---

# 45. Testes obrigatórios mínimos

Antes de considerar o projeto concluído, testar:

- cadastro válido;
- cadastro duplicado;
- login correto;
- senha incorreta;
- usuário inexistente;
- registro de entrada;
- entrada duplicada;
- início de intervalo;
- fim de intervalo;
- saída;
- saída sem entrada;
- histórico;
- persistência SQLite;
- recuperação de sessão.

---

# 46. Alterações no banco

Toda alteração de schema deve atualizar a versão do SQLite.

Não apagar dados silenciosamente.

Durante desenvolvimento acadêmico, migrations podem ser simples.

Não construir framework próprio de migrations.

---

# 47. Código morto

Remover:

- classes não utilizadas;
- métodos não utilizados;
- imports não utilizados;
- comentários obsoletos;
- código comentado.

Git deve preservar versões antigas.

O código-fonte atual deve representar somente a solução atual.

---

# 48. Comentários

Comentários devem explicar POR QUÊ.

Não comentar o óbvio.

Ruim:

```java
// incrementa i
i++;
```

Bom:

```java
// Mantemos o timestamp original para preservar a ordem real dos pontos.
```

---

# 49. Nomenclatura

Código deve utilizar nomes claros.

Preferir português ou inglês de forma consistente.

Neste projeto, preferir nomes de domínio em português.

Exemplos:

```java
Usuario
RegistroPonto
PontoService
UsuarioRepository
registrarEntrada()
buscarHistorico()
```

Não misturar idiomas sem necessidade.

---

# 50. Commits e alterações

Cada alteração deve ser pequena e focada.

Evitar modificar arquivos não relacionados.

Não realizar grandes refatorações durante correção de bugs.

Ao corrigir bug:

1. criar teste que reproduz o bug;
2. verificar falha;
3. corrigir;
4. verificar sucesso;
5. executar suíte completa.

---

# 51. Regra para agentes de IA

Qualquer agente trabalhando neste repositório deve:

1. ler este `CODEX.md`;
2. entender a estrutura existente antes de editar;
3. preservar arquitetura simples;
4. escrever ou atualizar testes primeiro;
5. evitar novas dependências;
6. modificar o menor número possível de arquivos;
7. executar testes após mudanças;
8. não reescrever código funcional sem motivo;
9. não adicionar infraestrutura futura;
10. manter compatibilidade com Android Java.

---

# 52. Antes de criar uma classe

Perguntar:

```text
Essa classe é realmente necessária?
```

Se a resposta for não, não criar.

---

# 53. Antes de adicionar uma dependência

Perguntar:

```text
Isso pode ser feito com Java ou Android SDK em poucas linhas?
```

Se sim, não adicionar a dependência.

---

# 54. Antes de criar abstração

Perguntar:

```text
Existe atualmente mais de uma implementação ou um ganho real de teste?
```

Se não, manter simples.

---

# 55. Definition of Done

Uma funcionalidade só está concluída quando:

- requisito funciona;
- teste existe;
- teste passa;
- suíte existente continua passando;
- dados persistem quando necessário;
- erros possuem tratamento;
- não adicionou dependência desnecessária;
- código permanece simples;
- arquitetura definida neste documento foi respeitada.

---

# 56. Filosofia final

O Bate Aí deve ser:

```text
simples
+
testável
+
offline
+
persistente
+
previsível
```

e não:

```text
complexo
+
genérico
+
superarquitetado
+
dependente de frameworks
```

Quando houver dúvida entre uma solução sofisticada e uma solução simples que atende integralmente ao requisito, utilizar a solução simples.

**Fazer funcionar corretamente é mais importante do que preparar o projeto para uma escala que ele nunca terá.**