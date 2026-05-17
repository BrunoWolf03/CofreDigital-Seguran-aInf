# Cofre Digital - INF1416

Trabalho 3 da disciplina INF1416 (Seguranca da Informacao), 2026.1.

Bruno Wolf - 2212576
Guilherme Senko - 2011478

## Dependencias

Tudo em `lib/` (gitignored). Voce precisa baixar:

- JavaFX SDK 21 -> `lib/javafx/`
- `bcprov-jdk18on-1.78.1.jar` (BouncyCastle)
- `sqlite-jdbc-3.46.1.3.jar`
- `slf4j-api-2.0.13.jar` + `slf4j-simple-2.0.13.jar`

## Compilar

macOS / Linux:

```bash
javac --module-path lib/javafx/lib --add-modules javafx.controls,javafx.fxml \
      -cp "lib/bcprov-jdk18on-1.78.1.jar:lib/sqlite-jdbc-3.46.1.3.jar" \
      -d out src/*.java
```

Windows: trocar `:` por `;` e `/` por `\`.

## Rodar o cofre

```bash
java --module-path lib/javafx/lib --add-modules javafx.controls,javafx.fxml \
     -cp "out:lib/bcprov-jdk18on-1.78.1.jar:lib/sqlite-jdbc-3.46.1.3.jar:lib/slf4j-api-2.0.13.jar:lib/slf4j-simple-2.0.13.jar" \
     App
```

Na primeira execucao aparece a tela de cadastro do administrador. Da segunda em diante, pede a frase secreta da chave privada do admin.

## Rodar o LogView (auditoria)

Em paralelo com o cofre (banco aberto em modo read-only):

```bash
java -cp "out:lib/bcprov-jdk18on-1.78.1.jar:lib/sqlite-jdbc-3.46.1.3.jar:lib/slf4j-api-2.0.13.jar:lib/slf4j-simple-2.0.13.jar" \
     LogView caminho/para/chave-privada.key
```

Pede a frase secreta sem echo. Se a chave nao for do administrador, encerra.

## Pacote de teste do professor

A pasta `Pacote-T3/` (do enunciado) tem 3 identidades prontas e uma pasta segura ja populada.

| Usuario | Certificado | Chave | Frase |
|---------|-------------|-------|-------|
| admin   | `Pacote-T3/Keys/admin-x509.crt`  | `Pacote-T3/Keys/admin-pkcs8-aes.key`  | `admin`  |
| user01  | `Pacote-T3/Keys/user01-x509.crt` | `Pacote-T3/Keys/user01-pkcs8-aes.key` | `user01` |
| user02  | `Pacote-T3/Keys/user02-x509.crt` | `Pacote-T3/Keys/user02-pkcs8-aes.key` | `user02` |

Pasta segura: `Pacote-T3/Files/` (3 arquivos secretos cifrados, um pra cada usuario).

Senhas pessoais sugeridas pros testes (qualquer 8 a 10 digitos sem repeticao consecutiva serve): `13572468`, `84629137`, `20485139`.
