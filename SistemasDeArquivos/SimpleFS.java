/*
 SimpleFS.java
 Simulador simples de sistema de arquivos com Journaling (modo terminal).
 Linguagem: Java (sem bibliotecas externas).

 Este programa cria dois arquivos na pasta onde é executado:
 - fs.bin      : guarda o estado do sistema de arquivos
 - fs.journal  : guarda operações pendentes para recuperar depois de falhas

 Comandos do terminal (exemplos):
 - criarpasta /meusarquivos
 - criarpasta /meusarquivos/textos
 - criararquivo /meusarquivos/textos/oi.txt "Olá pessoal!"
 - mostrar /meusarquivos/textos/oi.txt
 - listar /meusarquivos
 - copiar /meusarquivos/textos/oi.txt /meusarquivos/textos/copia.txt
 - mover /meusarquivos/textos/copia.txt /meusarquivos/textos/novo_nome.txt
 - apagararquivo /meusarquivos/textos/oi.txt
 - apagarpasta /meusarquivos/textos
 - sair
*/

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class SimpleFS {

    // Representação básica das coisas do sistema de arquivos
    static abstract class FSNode implements Serializable {
        String nome;
        FSDirectory pai;
        FSNode(String nome) { this.nome = nome; }
        String caminho() {
            if (pai == null) return "/";
            String p = pai.caminho();
            if (p.equals("/")) return "/" + nome;
            return p + "/" + nome;
        }
    }

    static class FSFile extends FSNode implements Serializable {
        StringBuilder conteudo = new StringBuilder();
        FSFile(String nome) { super(nome); }
    }

    static class FSDirectory extends FSNode implements Serializable {
        Map<String, FSNode> filhos = new LinkedHashMap<>();
        FSDirectory(String nome) { super(nome); }
    }

    static class FileSystem implements Serializable {
        FSDirectory raiz = new FSDirectory("");
        transient File arqDados;
        transient File arqJournal;

        FileSystem(File arqDados, File arqJournal) {
            raiz.pai = null;
            this.arqDados = arqDados;
            this.arqJournal = arqJournal;
        }

        FSNode localizar(String caminho) {
            if (caminho.equals("/")) return raiz;
            String[] partes = caminho.split("/");
            FSDirectory atual = raiz;
            for (int i = 1; i < partes.length; i++) {
                if (partes[i].isEmpty()) continue;
                FSNode n = atual.filhos.get(partes[i]);
                if (n == null) return null;
                if (i == partes.length - 1) return n;
                if (!(n instanceof FSDirectory)) return null;
                atual = (FSDirectory) n;
            }
            return atual;
        }

        FSDirectory diretorioPai(String caminho) throws Exception {
            if (caminho.equals("/") || caminho.isEmpty()) throw new Exception("Caminho inválido");
            int idx = caminho.lastIndexOf('/');
            String p = (idx == 0) ? "/" : caminho.substring(0, idx);
            FSNode node = localizar(p);
            if (node == null || !(node instanceof FSDirectory)) throw new Exception("Diretório pai não existe: " + p);
            return (FSDirectory) node;
        }

        String nomeBase(String caminho) {
            int idx = caminho.lastIndexOf('/');
            return caminho.substring(idx+1);
        }

        void salvar() throws IOException {
            try (FileOutputStream fos = new FileOutputStream(arqDados);
                 ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(this);
            }
        }

        static FileSystem carregar(File arqDados, File arqJournal) {
            if (!arqDados.exists()) {
                FileSystem fs = new FileSystem(arqDados, arqJournal);
                fs.raiz.pai = null;
                return fs;
            }
            try (FileInputStream fis = new FileInputStream(arqDados);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                FileSystem fs = (FileSystem) ois.readObject();
                fs.arqDados = arqDados;
                fs.arqJournal = arqJournal;
                fs.ajustarPais(fs.raiz, null);
                return fs;
            } catch (Exception e) {
                System.out.println("Falha ao carregar sistema, criando um novo.");
                FileSystem fs = new FileSystem(arqDados, arqJournal);
                fs.raiz.pai = null;
                return fs;
            }
        }

        void ajustarPais(FSNode node, FSDirectory parent) {
            node.pai = parent;
            if (node instanceof FSDirectory) {
                FSDirectory d = (FSDirectory) node;
                for (FSNode c : d.filhos.values()) ajustarPais(c, d);
            }
        }

        void registrarJournal(String linha) throws IOException {
            try (FileWriter fw = new FileWriter(arqJournal, true);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(linha);
                bw.newLine();
            }
        }

        void limparJournal() throws IOException {
            new FileOutputStream(arqJournal).close();
        }

        void replayJournal() throws Exception {
            if (!arqJournal.exists()) return;
            List<String> linhas = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(arqJournal))) {
                String l;
                while ((l = br.readLine()) != null) {
                    if (!l.trim().isEmpty()) linhas.add(l.trim());
                }
            }
            if (linhas.isEmpty()) return;

            System.out.println("Recuperando alterações pendentes...");
            for (String cmd : linhas) {
                aplicarOperacao(cmd, true);
            }
            salvar();
            limparJournal();
            System.out.println("Pronto.");
        }

        boolean ehMutacao(String op) {
            return Arrays.asList("criarpasta","apagarpasta","criararquivo","apagararquivo","mover","copiar").contains(op);
        }

        void aplicarOperacao(String linha, boolean doJournal) throws Exception {
            List<String> partes = separarArgs(linha);
            if (partes.isEmpty()) return;
            String op = partes.get(0);

            if (!doJournal && ehMutacao(op)) {
                registrarJournal(linha);
            }

            switch (op) {
                case "criarpasta": op_criarpasta(partes); break;
                case "apagarpasta": op_apagarpasta(partes); break;
                case "criararquivo": op_criararquivo(partes); break;
                case "apagararquivo": op_apagararquivo(partes); break;
                case "mover": op_mover(partes); break;
                case "copiar": op_copiar(partes); break;
                default: throw new Exception("Operação desconhecida: " + op);
            }

            if (!doJournal) {
                salvar();
                limparJournal();
            }
        }

        void op_criarpasta(List<String> p) throws Exception {
            if (p.size() < 2) throw new Exception("Use: criarpasta /caminho");
            String caminho = p.get(1);
            FSDirectory pai = diretorioPai(caminho);
            String nome = nomeBase(caminho);
            if (pai.filhos.containsKey(nome)) throw new Exception("Já existe: " + caminho);
            FSDirectory d = new FSDirectory(nome);
            d.pai = pai;
            pai.filhos.put(nome, d);
        }

        void op_apagarpasta(List<String> p) throws Exception {
            if (p.size() < 2) throw new Exception("Use: apagarpasta /caminho");
            FSNode node = localizar(p.get(1));
            if (node == null) throw new Exception("Não existe.");
            if (!(node instanceof FSDirectory)) throw new Exception("Não é diretório.");
            FSDirectory d = (FSDirectory) node;
            if (!d.filhos.isEmpty()) throw new Exception("Pasta não está vazia.");
            d.pai.filhos.remove(d.nome);
        }

        void op_criararquivo(List<String> p) throws Exception {
            if (p.size() < 3) throw new Exception("Use: criararquivo /caminho \"texto\"");
            String caminho = p.get(1);
            String texto = p.get(2);
            FSDirectory pai = diretorioPai(caminho);
            String nome = nomeBase(caminho);
            FSFile f;
            if (pai.filhos.containsKey(nome)) {
                FSNode n = pai.filhos.get(nome);
                if (!(n instanceof FSFile)) throw new Exception("Já existe uma pasta com esse nome.");
                f = (FSFile) n;
                f.conteudo.setLength(0);
                f.conteudo.append(texto);
            } else {
                f = new FSFile(nome);
                f.pai = pai;
                f.conteudo.append(texto);
                pai.filhos.put(nome, f);
            }
        }

        void op_apagararquivo(List<String> p) throws Exception {
            if (p.size() < 2) throw new Exception("Use: apagararquivo /caminho");
            FSNode node = localizar(p.get(1));
            if (node == null) throw new Exception("Não existe.");
            if (node instanceof FSDirectory) throw new Exception("Isso é uma pasta, use apagarpasta.");
            node.pai.filhos.remove(node.nome);
        }

        void op_mover(List<String> p) throws Exception {
            if (p.size() < 3) throw new Exception("Use: mover /origem /destino");
            String de = p.get(1);
            String para = p.get(2);
            FSNode n = localizar(de);
            if (n == null) throw new Exception("Origem não encontrada.");
            FSDirectory novoPai = diretorioPai(para);
            String novoNome = nomeBase(para);
            if (novoPai.filhos.containsKey(novoNome)) throw new Exception("Destino já existe.");
            n.pai.filhos.remove(n.nome);
            n.nome = novoNome;
            n.pai = novoPai;
            novoPai.filhos.put(novoNome, n);
        }

        void op_copiar(List<String> p) throws Exception {
            if (p.size() < 3) throw new Exception("Use: copiar /origem /destino");
            FSNode n = localizar(p.get(1));
            if (n == null) throw new Exception("Origem não encontrada.");
            if (n instanceof FSDirectory) throw new Exception("Só copia arquivos por enquanto.");
            FSFile arqOrig = (FSFile) n;
            FSDirectory pai = diretorioPai(p.get(2));
            String novoNome = nomeBase(p.get(2));
            if (pai.filhos.containsKey(novoNome)) throw new Exception("Destino já existe.");
            FSFile copia = new FSFile(novoNome);
            copia.conteudo.append(arqOrig.conteudo.toString());
            copia.pai = pai;
            pai.filhos.put(novoNome, copia);
        }

        // Parte só para leitura, não altera nada:
        void cmd_listar(String caminho) throws Exception {
            FSNode n = localizar(caminho);
            if (n == null) throw new Exception("Não existe.");
            if (!(n instanceof FSDirectory)) throw new Exception("Isto não é uma pasta.");
            FSDirectory d = (FSDirectory) n;
            for (FSNode c : d.filhos.values()) {
                String tipo = (c instanceof FSDirectory) ? "pasta" : "arq";
                System.out.printf("%s\t%s\n", tipo, c.nome);
            }
        }

        void cmd_mostrar(String caminho) throws Exception {
            FSNode n = localizar(caminho);
            if (n == null) throw new Exception("Não existe.");
            if (!(n instanceof FSFile)) throw new Exception("Isto não é um arquivo.");
            FSFile f = (FSFile) n;
            System.out.println(f.conteudo.toString());
        }

        void cmd_arvore(FSDirectory dir, String prefixo) {
            for (FSNode c : dir.filhos.values()) {
                System.out.println(prefixo + c.nome + (c instanceof FSDirectory ? "/" : ""));
                if (c instanceof FSDirectory) cmd_arvore((FSDirectory)c, prefixo + "  ");
            }
        }
        
        static List<String> separarArgs(String linha) {
            List<String> out = new ArrayList<>();
            boolean entreAspas = false;
            StringBuilder cur = new StringBuilder();
            for (char c : linha.toCharArray()) {
                if (c == '"') {
                    entreAspas = !entreAspas;
                    continue;
                }
                if (c == ' ' && !entreAspas) {
                    if (cur.length() > 0) { out.add(cur.toString()); cur.setLength(0); }
                } else cur.append(c);
            }
            if (cur.length() > 0) out.add(cur.toString());
            return out;
        }
    }

    public static void main(String[] args) throws Exception {
        File arqDados = new File("fs.bin");
        File arqJournal = new File("fs.journal");
        FileSystem fs = FileSystem.carregar(arqDados, arqJournal);

        fs.replayJournal();

        System.out.println("=== Sistema de Arquivos Simples ===");
        System.out.println("Digite 'ajuda' para instruções.");

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String linha = sc.nextLine().trim();
            if (linha.isEmpty()) continue;

            if (linha.equals("sair")) break;

            if (linha.equals("ajuda")) {
                System.out.println("Comandos: criarpasta, apagarpasta, criararquivo, apagararquivo, mover, copiar, listar, mostrar, arvore, ajuda, sair");
                continue;
            }

            try {
                if      (linha.startsWith("criarpasta "))   { fs.aplicarOperacao(linha, false); System.out.println("Pasta criada."); }
                else if (linha.startsWith("apagarpasta ")) { fs.aplicarOperacao(linha, false); System.out.println("Pasta removida."); }
                else if (linha.startsWith("criararquivo ")) { fs.aplicarOperacao(linha, false); System.out.println("Arquivo salvo."); }
                else if (linha.startsWith("apagararquivo ")) { fs.aplicarOperacao(linha, false); System.out.println("Arquivo removido."); }
                else if (linha.startsWith("mover ")) { fs.aplicarOperacao(linha, false); System.out.println("Feito."); }
                else if (linha.startsWith("copiar ")) { fs.aplicarOperacao(linha, false); System.out.println("Arquivo copiado."); }
                else if (linha.startsWith("listar ")) { fs.cmd_listar(linha.split(" ",2)[1]); }
                else if (linha.startsWith("mostrar ")) { fs.cmd_mostrar(linha.split(" ",2)[1]); }
                else if (linha.equals("arvore")) { fs.cmd_arvore(fs.raiz,""); }
                else System.out.println("Comando não reconhecido. Digite 'ajuda'.");
            } catch (Exception e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }

        sc.close();
        System.out.println("Saindo e salvando estado em fs.bin");
    }
}
