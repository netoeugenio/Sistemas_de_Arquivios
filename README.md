# Simulador de Sistema de Arquivos com Journaling

## Metodologia
O projeto foi desenvolvido em Java, simulando operações básicas de um sistema de arquivos.  
Cada comando é executado através de métodos, e o resultado aparece no terminal sempre que necessário.

O sistema conta com journaling, que registra operações para evitar perda de dados caso ocorra alguma falha durante a execução.

---

## Parte 1 — Sistema de Arquivos e Journaling

### O que é um sistema de arquivos?
É o componente responsável por organizar e armazenar arquivos dentro de um dispositivo.  
Ele define como os dados são guardados, nomeados e recuperados pelo sistema operacional.

### O que é Journaling?
É uma técnica utilizada para garantir a integridade do sistema de arquivos.  
Antes de realizar alterações no disco, a operação é registrada em um log (journal). Assim, caso o computador desligue inesperadamente, o sistema pode recuperar ou finalizar as ações pendentes sem corromper os dados.

Principais ideias:
- Registrar a operação primeiro
- Aplicar no armazenamento depois
- Permitir recuperação após falhas

---

## Parte 2 — Arquitetura do Simulador

### Estruturas utilizadas

| Classe | Função |
|--------|--------|
| `File` | Representa um arquivo com nome e conteúdo |
| `Directory` | Gerencia diretórios e seus itens internos |
| `SimpleFS` | Implementa os comandos do sistema de arquivos |
| `Journal` | Armazena operações antes de executá-las |

### Arquivos gerados
O sistema utiliza dois arquivos reais:
- `fs.bin`: armazena o estado completo do sistema de arquivos
- `fs.journal`: guarda as operações pendentes

Quando o simulador é iniciado:
- Se os arquivos existirem, ele carrega o estado anterior e aplica o que estava pendente
- Se não existirem, o sistema começa vazio

---

## Parte 3 — Implementação em Java
O simulador foi programado na classe `SimpleFS.java`.

Funcionamento do journaling:
1. A operação é registrada no `fs.journal`
2. A tarefa é executada no sistema de arquivos
3. Após concluir com sucesso, o registro é removido do journal

Com isso, se o programa for encerrado bruscamente, as operações restantes serão reaplicadas automaticamente ao iniciar novamente.

---

## Parte 4 — Instalação e Funcionamento

### Requisitos
- Java JDK instalado
- Arquivos do projeto na mesma pasta

### Como compilar e executar

No terminal, dentro da pasta onde está o arquivo `.java`:

Compilar:
```bash
javac SimpleFS.java
