Simulador de Sistema de Arquivos com Journaling

Este projeto é um simulador simples de sistema de arquivos feito em Java.
A ideia é mostrar como arquivos e pastas podem ser organizados e como o journaling ajuda a não perder dados quando algo dá errado.

Metodologia

O programa foi feito em Java e funciona como se fosse um “mini terminal”.
Cada comando digitado representa uma ação do sistema operacional, como criar arquivos, listar pastas, renomear etc.

Quando alguma ação acontece, o simulador mostra na tela para o usuário saber o que ocorreu.

Parte 1 — Sistema de Arquivos e Journaling
O que é um sistema de arquivos?

É o responsável por guardar e organizar tudo que existe no armazenamento:
arquivos, pastas, nomes, permissões, localização dos dados, etc.

Sem ele, os dados seriam apenas bits jogados no disco sem estrutura.

O que é journaling?

Quando um sistema trava no meio de uma gravação, existe risco de perder arquivos ou deixar o disco “bagunçado”.

O journaling evita isso porque registra as modificações antes de aplicá-las.
Se o sistema cair, ao iniciar de novo ele usa o journal para recuperar o que faltou.

É o que sistemas como NTFS, EXT4 e APFS usam hoje em dia.

Parte 2 — Arquitetura do Simulador

O simulador representa:

Componente	O que faz
Diretório	Guarda arquivos e subpastas
Arquivo	Guarda nome e conteúdo
Sistema de arquivos	Administra tudo e executa os comandos
Journal	Guarda operações pendentes para recuperar em caso de falha

📌 Os caminhos seguem o padrão:
/pasta/arquivo.txt

Quando o usuário digita um comando, ele altera o sistema e registra tudo no journal.

Parte 3 — Implementação em Java

Principais partes do código:

Nome	Função
SimpleFS	Classe principal que roda o simulador
Directory	Estrutura que representa pastas
FileEntry	Estrutura que representa arquivos

O journaling usa dois arquivos:

Arquivo	Função
fs.bin	Estado atual do sistema de arquivos
fs.journal	Lista de operações ainda não aplicadas

Se o programa fechar sem “sair”, ao iniciar novamente ele lê o journal e termina o que ficou pela metade.

Parte 4 — Como executar
Requisitos

Ter o Java instalado na máquina

Compilação

No prompt dentro da pasta do projeto:

javac SimpleFS.java

Execução
java SimpleFS


Vai aparecer um prompt próprio do simulador:

>


A partir daí, é só digitar os comandos.

Exemplos rápidos de uso
criarpasta /docs
criararquivo /docs/nota.txt "Teste no simulador"
listar /docs
mostrar /docs/nota.txt
arvore
sair


Depois que usar sair, os arquivos serão salvos:

fs.bin → onde fica tudo que existe no sistema

fs.journal → vazio quando não há falhas

Se o programa fechar sem sair, ao abrir de novo ele vai avisar que está recuperando alterações.

Esse passo já comprova que o journaling está funcionando.

Conclusão

O simulador demonstra de forma simples como um sistema de arquivos organiza dados e como o journaling ajuda a manter tudo seguro.
Mesmo sendo pequeno, dá para entender a lógica usada em sistemas de arquivos reais.
