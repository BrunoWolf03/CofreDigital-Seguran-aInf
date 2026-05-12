2212576 - Bruno Wolf
2011478 - Guilherme Senko

# Cofre Digital - INF1416

Sistema em **Java (JDK SE)** que implementa um cofre digital com autenticacao
multifator, criptografia simetrica e assimetrica, assinatura digital, envelope
digital e controle de acesso por usuario.

## Dependencias (em `lib/`)

- `javafx/` — JavaFX SDK 21
- `bcprov-jdk18on-1.78.1.jar` — BouncyCastle (bcrypt, OpenBSDBCrypt)
- `sqlite-jdbc-3.46.1.3.jar` — driver SQLite
- `slf4j-api-*.jar` + `slf4j-simple-*.jar` — logging (dependencia do SQLite driver)

## Compilar

### macOS / Linux

```bash
javac --module-path lib/javafx/lib --add-modules javafx.controls,javafx.fxml \
      -cp "lib/bcprov-jdk18on-1.78.1.jar:lib/sqlite-jdbc-3.46.1.3.jar" \
      -d out src/*.java
```

### Windows

```bat
javac --module-path lib\javafx\lib --add-modules javafx.controls,javafx.fxml ^
      -cp "lib\bcprov-jdk18on-1.78.1.jar;lib\sqlite-jdbc-3.46.1.3.jar" ^
      -d out src\*.java
```

## Executar o Cofre Digital

### macOS / Linux

```bash
java --module-path lib/javafx/lib --add-modules javafx.controls,javafx.fxml \
     -cp "out:lib/bcprov-jdk18on-1.78.1.jar:lib/sqlite-jdbc-3.46.1.3.jar:lib/slf4j-api-2.0.13.jar:lib/slf4j-simple-2.0.13.jar" \
     App
```

### Windows

```bat
java --module-path lib\javafx\lib --add-modules javafx.controls,javafx.fxml ^
     -cp "out;lib\bcprov-jdk18on-1.78.1.jar;lib\sqlite-jdbc-3.46.1.3.jar;lib\slf4j-api-2.0.13.jar;lib\slf4j-simple-2.0.13.jar" ^
     App
```

## Executar o LogView (auditoria - apenas administrador)

```bash
java -cp "out:lib/bcprov-jdk18on-1.78.1.jar:lib/sqlite-jdbc-3.46.1.3.jar:lib/slf4j-api-2.0.13.jar:lib/slf4j-simple-2.0.13.jar" \
     LogView caminho/para/admin-pkcs8-aes.key
```

A frase secreta da chave privada e solicitada via teclado (sem echo).
Em caso de falha de validacao, o programa encerra. Em caso de sucesso,
imprime todos os registros em ordem cronologica.

## Recursos de seguranca

- **Autenticacao multifator (3 etapas)**:
  1. Login (e-mail extraido do certificado digital)
  2. Senha pessoal numerica (8-10 digitos) via teclado virtual sobrecarregado
  3. Token TOTP (Google Authenticator, RFC 6238)
- **Bloqueio de 2 minutos** apos 3 falhas consecutivas na etapa 2 ou 3
- **bcrypt** (versao `2y`, custo `8`) para hash de senha pessoal
- **AES/ECB/PKCS5Padding** (chave de 256 bits derivada via `SHA1PRNG`) para:
  - Arquivos secretos (`.enc`)
  - Chave privada PKCS8 dos usuarios (`.key`)
  - Segredo TOTP armazenado na base
- **RSA/ECB/PKCS1Padding** para envelope digital (`.env`)
- **SHA1withRSA** para assinatura digital (`.asd`)
- **Certificado X.509** lido em formato PEM; e-mail extraido do Subject DN
- **Registro de auditoria** com ~60 mensagens semeadas na tabela `Mensagens`

## Estrutura do banco de dados

- `Usuarios` — UID, login_name, nome, senha_hash, totp_secret, GID, KID, falhas, bloqueio, contadores
- `Chaveiro` — KID, UID, certificado PEM, chave privada binaria
- `Grupos` — GID, nome (1=Administrador, 2=Usuario)
- `Mensagens` — MID, texto
- `Registros` — RID, timestamp, MID, UID, arq_name

## Artefatos de teste

`Pacote-T3/` (fornecido pelo professor):

- `Keys/` — admin + user01 + user02 (X.509 + chave privada AES-encrypted)
- `Files/` — pasta segura ja populada com `index.{enc,env,asd}` e 3 arquivos
