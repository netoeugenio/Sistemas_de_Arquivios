# 📌 Simulador de Sistema de Arquivos com Journaling

## 🧰 Metodologia
O projeto foi desenvolvido em Java, simulando operações básicas de um sistema de arquivos.  
Cada comando é executado através de métodos, e o resultado aparece no terminal quando necessário.

Também foi implementado um recurso de **journaling**, que registra operações para evitar perda de dados em caso de falhas.

---

## 📍 Parte 1 — Sistema de Arquivos e Journaling

### ✔️ O que é um sistema de arquivos?
É o componente responsável por armazenar e organizar arquivos no disco.  
Ele define **como os dados são guardados, identificados e encontrados**.

Sem ele, as informações seriam apenas um amontoado de bits sem utilidade.

### ✔️ O que é Journaling?
Antes de escrever no disco, o sistema **guarda a operação em um log (journal)**.  
Se algo der errado, como falta de energia, esse log permite recuperar a ação pendente e **evitar corrupção dos dados**.

Tipos comuns:
- **Write-ahead logging** → primeiro registra no journal, depois aplica no disco
- **Log-structured** → dados organizados como um log contínuo

A ideia principal: **manter o sistema consistente mesmo após falhas inesperadas**.

---

## 🧱 Parte 2 — Arquitetura do Simulador

### 🔹 Estruturas criadas

| Classe | Função |
|--------|--------|
| `File` | Representa um arquivo com nome e conteúdo |
| `Directory` | Representa um diretório com seus itens internos |
| `FileSystemSimulator` | Executa comandos como criar, mover, excluir, listar etc. |
| `Journal` | Registra as operações antes de acontecerem |

### 📁 Persistência em disco
O simulador cria dois arquivos:
- `fs.bin` → guarda o estado do sistema de arquivos
- `fs.journal` → guarda operações pendentes

Ao iniciar:
- Se existirem → tenta recuperar alterações incompletas
- Se não existirem → o sistema começa vazio

---

## 💻 Parte 3 — Implementação em Java

A funcionalidade principal está na classe `SimpleFS.java`.

O journaling funciona assim:
1. Registra a alteração no arquivo `fs.journal`
2. Executa a operação no sistema
3. Marca a conclusão → remove do journal

Se o programa fechar no meio do processo:
→ na próxima execução ele lê o journal e **reaplica as operações pendentes**.

---

## ▶️ Parte 4 — Instalação e Uso

### 📎 Pré-requisitos
- Java JDK 8+ instalado
- Código fonte na mesma pasta

### 🚀 Como compilar e executar

Abra o terminal dentro da pasta do projeto:

**Compilar**
```bash
javac SimpleFS.java
