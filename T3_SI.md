# Aula: Cofre Digital — Trabalho 3 de Segurança da Informação

Documento didático completo sobre o que construímos no Trabalho 3 da disciplina **INF1416 — Segurança da Informação** (PUC-Rio). O objetivo aqui é que qualquer pessoa, mesmo sem fundo técnico em criptografia, consiga entender **o quê** foi feito, **por quê** e **como** cada peça funciona.

---

## Sumário

1. [O que é o Cofre Digital](#1-o-que-é-o-cofre-digital)
2. [Os quatro pilares de segurança](#2-os-quatro-pilares-de-segurança)
3. [Tipos de criptografia — panorama geral](#3-tipos-de-criptografia--panorama-geral)
4. [Criptografia simétrica: AES](#4-criptografia-simétrica-aes)
5. [Criptografia assimétrica: RSA](#5-criptografia-assimétrica-rsa)
6. [Funções de hash e bcrypt](#6-funções-de-hash-e-bcrypt)
7. [Geradores pseudoaleatórios: SHA1PRNG](#7-geradores-pseudoaleatórios-sha1prng)
8. [Certificado digital X.509](#8-certificado-digital-x509)
9. [Como uma chave privada é armazenada e restaurada](#9-como-uma-chave-privada-é-armazenada-e-restaurada)
10. [Envelope digital — o melhor dos dois mundos](#10-envelope-digital--o-melhor-dos-dois-mundos)
11. [Assinatura digital — SHA1withRSA](#11-assinatura-digital--sha1withrsa)
12. [Autenticação multifator (3 etapas)](#12-autenticação-multifator-3-etapas)
13. [TOTP — Time-based One-Time Password](#13-totp--time-based-one-time-password)
14. [Como o cofre funciona — fluxo completo](#14-como-o-cofre-funciona--fluxo-completo)
15. [Banco de dados e auditoria](#15-banco-de-dados-e-auditoria)
16. [Mapa do código — onde está cada coisa](#16-mapa-do-código--onde-está-cada-coisa)
17. [Como rodar e testar](#17-como-rodar-e-testar)

---

## 1. O que é o Cofre Digital

O **Cofre Digital** é uma aplicação em Java que protege uma pasta de arquivos sigilosos. Apenas pessoas devidamente autenticadas podem acessar os arquivos, e apenas o **dono** de cada arquivo (ou um administrador, em casos específicos) pode decifrá-lo.

### Propósito acadêmico

Foi proposto como Trabalho 3 da disciplina **INF1416 — Segurança da Informação** (PUC-Rio, 2026.1, Prof. Anderson Oliveira da Silva). O objetivo é exercitar, num único projeto, praticamente todos os conceitos centrais da disciplina:

- Criptografia simétrica e assimétrica
- Hash de senhas
- Assinatura digital
- Envelope digital
- Certificados digitais X.509
- Autenticação multifator (3 etapas)
- Controle de acesso por usuário e grupo
- Auditoria (logging) de operações

### Propósito prático

Sistemas semelhantes existem no mundo real:

- **Gerenciadores de senhas:** 1Password, Bitwarden, LastPass — guardam credenciais cifradas com chave derivada de uma senha-mestre.
- **Cofres corporativos:** HashiCorp Vault, AWS KMS — armazenam segredos (chaves de API, certificados, senhas de banco) com controle de acesso fino.
- **Pastas criptografadas:** Veracrypt, FileVault, BitLocker — protegem dados em repouso no disco.

O Cofre Digital é uma versão didática que junta as ideias principais.

### Garantias que o sistema oferece

O cofre oferece quatro propriedades, descritas em detalhe na próxima seção:

- **Sigilo** (ninguém de fora consegue ler)
- **Integridade** (ninguém consegue alterar sem ser detectado)
- **Autenticidade** (sabe-se quem produziu o conteúdo)
- **Não-repúdio** (o autor não pode negar depois)

Mais um quinto pilar operacional: **controle de acesso** (cada usuário só vê o que pode ver).

---

## 2. Os quatro pilares de segurança

Antes de mergulhar em algoritmos, é importante separar **o que** estamos tentando garantir. Toda decisão técnica do projeto serve a um destes quatro objetivos:

### Sigilo (ou confidencialidade)

A informação só pode ser lida por quem está autorizado.

**Exemplo no projeto:** o arquivo `index.enc` na pasta segura é um bloco de bytes ilegíveis. Sem a chave AES (que só quem tem a chave privada do administrador consegue obter), o conteúdo é indecifrável.

### Integridade

Qualquer alteração indevida no conteúdo deve ser detectada.

**Exemplo no projeto:** se um atacante mudar um único bit do arquivo `XXYYZZ00.enc`, ao tentar abrir, a verificação da assinatura digital `XXYYZZ00.asd` vai falhar e o sistema avisa o usuário.

### Autenticidade

É possível comprovar quem produziu o conteúdo.

**Exemplo no projeto:** o `index.asd` é uma assinatura digital feita com a chave privada do administrador. Quando o sistema verifica essa assinatura usando a chave pública do administrador (que está no certificado), ele tem certeza de que **foi o administrador** que assinou aquele arquivo.

### Não-repúdio

O autor não consegue, depois, alegar que não foi ele.

**Exemplo no projeto:** como apenas o administrador possui a chave privada dele, **só ele** pode ter produzido a assinatura `index.asd`. Ele não pode dizer "não fui eu" — a única forma de aquela assinatura existir é com a posse da chave.

---

## 3. Tipos de criptografia — panorama geral

| Tipo | Mesma chave cifra e decifra? | Velocidade | Tamanho de dados | Para que serve no projeto |
|------|---|---|---|---|
| **Simétrica** (AES) | Sim | Muito rápida | Qualquer tamanho | Cifrar arquivos, chave privada no disco, segredo TOTP no banco |
| **Assimétrica** (RSA) | Não — pública cifra, privada decifra | Lenta | Limitado (~245 bytes em RSA 2048) | Envelope digital, assinatura digital |
| **Hash** (SHA-1, bcrypt) | Não cifra; produz "impressão digital" | Rápida (SHA-1) ou propositalmente lenta (bcrypt) | Saída de tamanho fixo | Armazenar senha, verificar integridade |

**Detalhe importante:** simétrica e assimétrica **não são alternativas concorrentes**, são **complementares**. A maior parte dos sistemas modernos usa as duas juntas (e o cofre digital também) — veja a [seção 10](#10-envelope-digital--o-melhor-dos-dois-mundos).

---

## 4. Criptografia simétrica: AES

### O que é

**AES** (Advanced Encryption Standard) é o algoritmo simétrico mais usado no mundo. Foi padronizado pelo NIST em 2001 e é considerado seguro até hoje.

Ele recebe três coisas:

1. **Chave** (no nosso caso, 256 bits)
2. **Texto em claro** (qualquer sequência de bytes)
3. **Modo de operação** (no nosso caso, ECB)

E produz o **texto cifrado** — uma sequência de bytes que parece aleatória. Com a mesma chave, é possível inverter o processo e recuperar o texto original.

### Como funciona internamente (visão geral)

AES é uma **cifra de bloco**: ele processa dados em blocos de **16 bytes** por vez. Dentro de cada bloco, faz uma série de operações matemáticas (substituições, permutações, mistura de linhas e colunas) repetidas 14 vezes (no AES-256). Cada repetição embaralha mais os bits.

### Modos de operação

Como AES opera em blocos de 16 bytes, precisa-se decidir como tratar arquivos maiores. Isso é o **modo de operação**:

- **ECB** (Electronic Codebook): cada bloco é cifrado independentemente. Simples, mas vulnerável a análise de padrões em dados muito repetitivos.
- **CBC** (Cipher Block Chaining): cada bloco é misturado (XOR) com o cifrado anterior antes de ser cifrado. Mais seguro, mas precisa de IV (vetor de inicialização).
- **GCM, CTR**, etc. — modos modernos que também oferecem integridade.

**No projeto usamos ECB** porque o enunciado pediu (`AES/ECB/PKCS5Padding`). Em sistemas de produção, normalmente se prefere CBC ou GCM.

### Padding

Se o arquivo não for múltiplo exato de 16 bytes, precisa-se completar o último bloco. O **PKCS5Padding** (que na prática é PKCS7 para AES) preenche com bytes cujo valor é a quantidade de bytes que faltam:

```
Faltam 5 bytes  →  preenche com [05][05][05][05][05]
Faltam 1 byte   →  preenche com [01]
Faltam 0 bytes  →  acrescenta um bloco inteiro de [10][10]...[10]
```

### AES-256

A força bruta para uma chave de 256 bits exigiria 2^256 tentativas. Mesmo com toda a computação do planeta trabalhando junto, levaria um tempo absurdamente maior que a idade do universo. Por isso AES-256 é considerado **virtualmente inquebrável**.

### Onde está no código

[src/CryptoUtils.java](src/CryptoUtils.java) — métodos:

```java
public static byte[] aesEncrypt(byte[] plain, SecretKey k)
public static byte[] aesDecrypt(byte[] cipherText, SecretKey k)
```

Usado para:

- Cifrar e decifrar arquivos `.enc` (índice e arquivos secretos da pasta)
- Cifrar e decifrar a chave privada `.key` armazenada no disco
- Cifrar o segredo TOTP de cada usuário antes de salvar no banco

---

## 5. Criptografia assimétrica: RSA

### O que é

**RSA** (de Rivest-Shamir-Adleman, os criadores) é o algoritmo assimétrico mais clássico. Foi publicado em 1977 e ainda é amplamente usado.

A grande diferença em relação ao AES é que existem **duas chaves diferentes**:

- **Chave pública** — pode (e deve) ser distribuída livremente
- **Chave privada** — fica em segredo absoluto com o dono

E elas têm uma propriedade matemática que se complementa: **o que uma cifra, só a outra decifra**.

### Os dois usos

#### Uso 1: Cifrar para alguém

Se eu quero mandar uma mensagem secreta para você:

1. Pego sua **chave pública** (que está disponível)
2. Cifro a mensagem com ela
3. Você recebe o texto cifrado
4. Você decifra com sua **chave privada** (que só você tem)

Resultado: ninguém mais consegue ler, nem mesmo eu (porque eu só tenho a sua pública).

#### Uso 2: Assinar (provar autoria)

Se eu quero provar para o mundo que **eu** escrevi uma mensagem:

1. Cifro a mensagem (na prática: um hash dela) com minha **chave privada**
2. Junto a mensagem original com esse "cifrado", que vira a **assinatura**
3. Qualquer pessoa com minha **chave pública** pode pegar a assinatura, decifrar com a pública e comparar com o hash da mensagem original
4. Se bater, foi eu — porque só eu tenho a privada

Esses dois usos cobrem **envelope digital** (uso 1) e **assinatura digital** (uso 2).

### Limitação prática

RSA é **lento** comparado a AES (cerca de 1000 vezes mais lento) e **limitado em tamanho**: com chave de 2048 bits, só consegue cifrar até ~245 bytes por operação. Por isso ninguém cifra um arquivo grande com RSA direto — usa-se **envelope digital** (combinação RSA + AES).

### Onde está no código

[src/CryptoUtils.java](src/CryptoUtils.java) — métodos:

```java
public static byte[] rsaEncrypt(byte[] data, PublicKey pk)
public static byte[] rsaDecrypt(byte[] data, PrivateKey pk) throws Exception
```

Algoritmo: `RSA/ECB/PKCS1Padding`.

---

## 6. Funções de hash e bcrypt

### O que é hash

Uma **função de hash** transforma uma entrada de qualquer tamanho em uma saída de tamanho fixo (por exemplo, 160 bits no SHA-1, 256 bits no SHA-256).

Propriedades importantes:

- **One-way:** dado o hash, é praticamente impossível recuperar a entrada
- **Determinístico:** mesma entrada → sempre o mesmo hash
- **Sensível:** mudar 1 bit da entrada muda drasticamente o hash (efeito avalanche)
- **Resistente a colisão:** é impraticável encontrar duas entradas com o mesmo hash

### Para que serve

- **Verificar integridade:** se você baixou um arquivo, calcula o hash e compara com o publicado. Se bater, o arquivo não foi alterado.
- **Verificar assinatura:** em vez de assinar um arquivo grande, assina-se o hash dele.
- **Armazenar senha:** salvar o hash da senha em vez da senha em si.

### Por que não basta SHA-256 para senhas

Imagine que um atacante rouba a tabela de senhas hasheadas. Ele pode:

1. Pegar listas gigantes de senhas comuns ("123456", "qwerty", etc.)
2. Calcular o hash SHA-256 de cada uma
3. Comparar com os hashes vazados

SHA-256 é **rápido demais** — milhões de comparações por segundo. Em poucas horas, descobre todas as senhas fracas.

Pior: senhas iguais geram hashes iguais. Se duas pessoas usam "senha123", os dois hashes são idênticos no banco — um sinal claro de senha repetida.

### bcrypt: a solução

**bcrypt** é uma função especificamente projetada para armazenar senhas. Ela tem duas características que mudam o jogo:

#### Salt aleatório

Antes de aplicar o hash, bcrypt junta a senha com um **salt** aleatório (uma string única por usuário). Mesmo que duas pessoas usem a mesma senha, os hashes resultantes serão diferentes — porque os salts são diferentes.

O salt é armazenado **junto** com o hash (em texto claro), então o sistema pode reusá-lo quando for verificar a senha.

#### Custo configurável

bcrypt é **propositalmente lento**. O parâmetro de custo determina quantas iterações ele faz: custo `8` = 2^8 = 256 iterações. Custo `12` = 4096 iterações. Quanto maior, mais lento — para o usuário legítimo, alguns milissegundos; para um atacante tentando bilhões de senhas, anos.

No projeto, usamos custo `8` (definido pelo enunciado).

### Formato bcrypt no projeto

```
$2y$08$ajDZdV0XFoYiLRE0ZrSx6OmRbhhB5qM.Ap6c.0LqRu2cIY1jpytuO
│  │  │                    │
│  │  │                    └── Hash da senha (parte BASE64)
│  │  └─── Salt aleatório (parte BASE64)
│  └────── Custo: 2^8 iterações
└───────── Versão do algoritmo (2y é uma das versões padrão)
```

Tudo numa string só de 60 caracteres. O sistema não precisa guardar salt em coluna separada.

### Onde está no código

[src/CryptoUtils.java](src/CryptoUtils.java) — métodos:

```java
public static String bcryptHash(String senha)
public static boolean bcryptCheck(String senha, String hash)
```

Internamente usa a classe `OpenBSDBCrypt` do **BouncyCastle**.

---

## 7. Geradores pseudoaleatórios: SHA1PRNG

### O que é PRNG

Computadores não geram **aleatoriedade verdadeira** (a menos que tenham hardware específico). Eles usam **geradores pseudoaleatórios** (PRNG, *Pseudo-Random Number Generator*): algoritmos determinísticos que, dada uma **semente** (*seed*), produzem uma sequência de bytes que **parece** aleatória.

A propriedade-chave é: **mesma seed → mesma sequência**. Sempre.

### SHA1PRNG no Java

`SHA1PRNG` é o PRNG padrão do Java baseado em SHA-1. É invocado assim:

```java
SecureRandom rnd = SecureRandom.getInstance("SHA1PRNG");
rnd.setSeed(semente);  // bytes que servem de seed
// rnd agora gera bytes pseudo-aleatórios determinísticos a partir dessa seed
```

### Para que usamos no projeto

Para **derivar a chave AES** a partir de algo: pode ser uma frase secreta digitada pelo usuário ou uma semente aleatória. O fluxo é:

```
seed (bytes) ─→ SHA1PRNG.setSeed() ─→ KeyGenerator AES ─→ K_AES (256 bits)
```

Duas aplicações importantes:

1. **Derivar K_AES da frase secreta** (quando vamos decifrar a chave privada `.key`)
2. **Derivar K_AES da semente do envelope digital** (quando vamos decifrar um arquivo `.enc`)

Por essa propriedade determinística, basta cifrar a **semente** com RSA — quem decifrar a semente consegue regenerar exatamente a mesma chave AES. É isso que faz o envelope digital funcionar (próxima seção).

### Onde está no código

[src/CryptoUtils.java](src/CryptoUtils.java) — método:

```java
public static SecretKey deriveAesKey(String segredo)
public static SecretKey deriveAesKey(byte[] seed)
```

---

## 8. Certificado digital X.509

### Problema que ele resolve

Imagine que alguém me dá uma chave pública e diz "essa é a chave pública do Bruno". Como eu sei que é mesmo do Bruno, e não de alguém se passando por ele?

A solução é um **certificado digital**: um documento que **liga uma identidade (nome, e-mail) a uma chave pública**, e que é **assinado por uma autoridade confiável**.

### O que tem dentro de um certificado X.509

X.509 é o padrão internacional. Os campos principais:

| Campo | O que é |
|-------|---------|
| **Version** | Versão do padrão X.509 (v3 é o comum) |
| **Serial Number** | Identificador único do certificado dado pela AC |
| **Validity** | Datas de início e fim de validade |
| **Issuer** | Quem emitiu (a AC) — em formato DN (`CN=AC INF1416, O=PUC...`) |
| **Subject** | Para quem foi emitido — também em formato DN (`CN=Administrator, emailAddress=admin@inf1416.puc-rio.br`) |
| **Subject Public Key** | A chave pública do titular |
| **Signature Algorithm** | Algoritmo da assinatura da AC (no nosso caso: `sha256WithRSAEncryption`) |
| **Signature** | A assinatura da AC sobre todos os campos acima |

### Exemplo real do projeto

No arquivo `Pacote-T3/Keys/admin-x509.crt`, ao rodar `openssl x509 -in admin-x509.crt -noout -text`:

```
Subject: C=BR, ST=RJ, O=PUC, OU=INF1416, CN=Administrator,
         emailAddress=admin@inf1416.puc-rio.br
Issuer:  C=BR, ST=RJ, L=Rio, O=PUC, OU=INF1416, CN=AC INF1416,
         emailAddress=ca@grad.inf.puc-rio.br
Validity:
    Not Before: Apr 23 15:01:50 2026 GMT
    Not After : Apr 23 15:01:50 2027 GMT
Serial Number: 23
Public Key Algorithm: rsaEncryption (2048 bit)
```

O **e-mail** do usuário está no Subject, no campo `emailAddress`. Foi com base nesse campo que o cofre extrai a identidade na hora do cadastro.

### Formato PEM

X.509 pode ser armazenado em dois formatos:

- **DER** — binário puro
- **PEM** — BASE64 do DER, delimitado por linhas:

```
-----BEGIN CERTIFICATE-----
MIIEQzCCAyugAwIBAgIBFzANBgkqhkiG9w0BAQsFADCBhDELMAkGA1UEBhMCQlIx
... (várias linhas de base64) ...
-----END CERTIFICATE-----
```

No projeto, os arquivos `.crt` são PEM. É o formato que a classe `CertificateFactory` do Java consome diretamente.

### Onde está no código

[src/CryptoUtils.java](src/CryptoUtils.java) — métodos:

```java
public static X509Certificate carregarCertificado(Path arquivo)
public static String certificadoParaPem(X509Certificate cert)
public static Map<String, String> camposDoCertificado(X509Certificate c)
public static String extrairEmail(X509Certificate c)
```

O `extrairEmail` é especialmente delicado porque o e-mail no Subject DN pode vir codificado em formato IA5 (hex). Tem uma sub-rotina `decodeIA5` para lidar com isso.

---

## 9. Como uma chave privada é armazenada e restaurada

### Problema

A chave privada é o ativo mais sensível do usuário. Se ela for guardada em texto claro num arquivo, qualquer um que copiar o arquivo tem acesso a tudo (pode assinar como se fosse o usuário, pode abrir envelopes destinados a ele etc.).

### Solução

Cifrar a chave privada com **AES**, usando uma **frase secreta** que só o dono conhece. O arquivo no disco fica em formato:

```
[bytes binários cifrados com AES/ECB/PKCS5]
```

Quando o dono precisa usar a chave, ele digita a frase, o sistema deriva K_AES, decifra os bytes, e dentro encontra a chave privada propriamente dita.

### Pipeline completo (conforme `Trabalho3-Detalhamento.pdf` slide 8)

```
┌─────────────────────────┐
│ admin-pkcs8-aes.key     │   arquivo binário no disco
│ (bytes cifrados)        │
└───────────┬─────────────┘
            │
            │  AES/ECB/PKCS5 decrypt
            │  (chave derivada da frase "admin" via SHA1PRNG)
            ▼
┌─────────────────────────┐
│ -----BEGIN PRIVATE KEY---│   PEM ASCII (texto)
│ MIIEvgIBADANBgkqhkiG9w0B │
│ ...                      │
│ -----END PRIVATE KEY-----│
└───────────┬─────────────┘
            │
            │  extrai miolo BASE64
            │  Base64.getMimeDecoder().decode()
            ▼
┌─────────────────────────┐
│ bytes PKCS8             │   formato padrão para chaves
└───────────┬─────────────┘
            │
            │  PKCS8EncodedKeySpec
            │  KeyFactory.getInstance("RSA").generatePrivate()
            ▼
┌─────────────────────────┐
│ PrivateKey (objeto Java)│   pronto para uso
└─────────────────────────┘
```

### Por que a frase secreta é tão importante

Sem a frase, é impossível decifrar o arquivo. Mesmo o administrador do sistema **não tem como** recuperar a chave privada de um usuário se ele esquecer a frase — a única solução nesse caso seria emitir um novo certificado e regerar tudo.

### Onde está no código

[src/CryptoUtils.java](src/CryptoUtils.java) — método:

```java
public static PrivateKey carregarChavePrivada(Path arquivoCifrado, String frase)
```

---

## 10. Envelope digital — o melhor dos dois mundos

### Problema

Quero cifrar um arquivo `documento.docx` de 50 KB para que **só o Bruno** consiga ler.

Tentativas ingênuas:

- **Só com AES?** Eu precisaria de uma chave compartilhada com o Bruno. Como entrego essa chave para ele sem alguém interceptar? Esse é o "problema da troca de chaves".
- **Só com RSA?** RSA é lento e só cifra ~245 bytes por vez. Um arquivo de 50 KB precisaria ser quebrado em 200+ pedaços e cifrado separadamente — funcional, mas extremamente ineficiente.

### Solução: envelope digital

Combina-se **AES (rápido, para o conteúdo)** com **RSA (lento, mas pequeno, para a chave)**.

#### Cifragem (lado do emissor)

```
1. Gera uma semente aleatória pequena (no projeto, 5 bytes)
2. Deriva uma chave AES da semente: SHA1PRNG(semente) → K_AES
3. Cifra o arquivo grande com AES usando K_AES → arquivo.enc
4. Cifra APENAS a semente com a chave PÚBLICA do destinatário → arquivo.env
```

Resultado: dois arquivos. O `.enc` é o conteúdo cifrado; o `.env` é o "envelope" com a semente protegida.

#### Decifragem (lado do destinatário)

```
1. Decifra arquivo.env com a chave PRIVADA → recupera a semente
2. Aplica SHA1PRNG na semente → K_AES (mesma chave de antes!)
3. Decifra arquivo.enc com K_AES → conteúdo original
```

Funciona porque SHA1PRNG é determinístico — mesma semente, mesma K_AES.

### Por que se chama "envelope"

Pensa numa carta:

- O **conteúdo** (o arquivo) está dentro de um envelope cifrado com AES — barato, mesmo se for grande
- A **chave do envelope** (a semente) está protegida por uma cifra RSA — cara, mas só precisa cifrar poucos bytes

### No projeto

A pasta segura do `Pacote-T3/Files/` tem essa estrutura para cada arquivo protegido:

```
index.enc    ← conteúdo do índice cifrado com AES
index.env    ← semente cifrada com chave pública do administrador
index.asd    ← assinatura digital (próxima seção)
```

E para cada arquivo individual (`XXYYZZ00`, `XXYYZZ11`, `XXYYZZ22`), o mesmo padrão. A diferença é **de quem é a chave pública** usada no envelope:

- Para o índice: chave pública do **administrador** (porque o índice é dele)
- Para `XXYYZZ11.env`: chave pública do **user01** (porque o dono é user01)

### Onde está no código

Pipeline de decifragem do índice em [src/TelaConsultaPasta.java](src/TelaConsultaPasta.java) (método `lerIndice`, linha ~228 em diante):

```java
byte[] env = Files.readAllBytes(indexEnv);
byte[] semente = CryptoUtils.rsaDecrypt(env, privAdmin);    // RSA
SecretKey kAes = CryptoUtils.deriveAesKey(semente);          // SHA1PRNG → K_AES
byte[] cif = Files.readAllBytes(indexEnc);
byte[] plano = CryptoUtils.aesDecrypt(cif, kAes);            // AES decrypt
```

---

## 11. Assinatura digital — SHA1withRSA

### O que ela garante

Três coisas ao mesmo tempo:

- **Integridade:** o conteúdo não foi alterado
- **Autenticidade:** sabemos quem assinou
- **Não-repúdio:** o autor não pode negar depois

### Como funciona

#### Assinar (com chave privada)

```
1. Calcula o hash SHA-1 do conteúdo (160 bits)
2. Cifra esse hash com a chave PRIVADA do autor
3. O resultado é a "assinatura" (bytes)
4. Geralmente é distribuída junto com o conteúdo
```

#### Verificar (com chave pública)

```
1. Recebe o conteúdo + assinatura
2. Calcula o hash SHA-1 do conteúdo recebido
3. Decifra a assinatura com a chave PÚBLICA do suposto autor
4. Compara: se os dois hashes baterem, a assinatura é válida
```

Se alguém alterou o conteúdo, o hash recalculado no passo 2 será diferente do que sai da assinatura no passo 3, e a verificação falha.

### Por que SHA-1 (e não SHA-256)?

SHA-1 já foi considerado quebrado para alguns usos críticos (colisões foram demonstradas em 2017). Em sistemas de produção, recomenda-se hoje SHA-256 ou SHA-3.

**No nosso projeto, usamos SHA-1 porque o enunciado pediu** (confirmado no `Trabalho3-Detalhamento.pdf` slide 6 e 7: "A chave privada é utilizada para produzir a assinatura digital **(SHA1-RSA)**"). Para fins didáticos é OK, mas se você adaptar esse código para algo real, troque para `SHA256withRSA`.

### Arquivos `.asd` no projeto

A pasta segura tem `index.asd`, `XXYYZZ00.asd`, etc. Cada um é a representação binária da assinatura do conteúdo correspondente.

- `index.asd` é assinado com a chave privada do **administrador**
- `XXYYZZ11.asd` é assinado com a chave privada do **user01** (o dono)

### Desafio criptográfico (autenticação por chave)

A assinatura digital também serve para **provar que você tem uma chave privada sem revelá-la**. O cofre faz isso em três pontos:

1. **Cadastro de usuário:** o sistema gera 9216 bytes aleatórios, assina com a chave privada do candidato e verifica com a pública do certificado. Se passar, prova que a chave realmente corresponde ao certificado e a frase secreta digitada está correta.
2. **Validação do admin na partida:** mesmo procedimento com 9216 bytes.
3. **LogView:** desafio de 2048 bytes para autenticar o administrador antes de exibir os logs.

### Onde está no código

[src/CryptoUtils.java](src/CryptoUtils.java) — métodos:

```java
public static byte[] assinar(byte[] dados, PrivateKey priv)
public static boolean verificar(byte[] dados, byte[] assinatura, PublicKey pub)
```

Algoritmo: `Signature.getInstance("SHA1withRSA")`.

---

## 12. Autenticação multifator (3 etapas)

### Conceito de MFA

Autenticação multifator se baseia em três categorias de "fatores":

1. **Algo que você sabe** (senha, frase secreta, PIN)
2. **Algo que você tem** (token físico, smartphone, smartcard)
3. **Algo que você é** (impressão digital, íris, face)

Quando exigimos **mais de uma categoria**, dificulta drasticamente um ataque: roubar uma senha não basta, é preciso também roubar o telefone, por exemplo.

### As 3 etapas do cofre digital

#### Etapa 1 — Identificação (e-mail)

O usuário fornece o e-mail. O sistema:

- Verifica se o e-mail existe no banco
- Verifica se a conta não está bloqueada (após 3 erros consecutivos, a conta fica bloqueada por 2 minutos)
- Se ok, passa para a etapa 2

**Nota:** essa etapa **não autentica**, apenas identifica. Qualquer um que saiba o e-mail pode chegar até aqui. As etapas 2 e 3 é que realmente autenticam.

**Arquivo:** [src/TelaLoginEmail.java](src/TelaLoginEmail.java)

#### Etapa 2 — Senha pessoal via teclado virtual

Algo que **você sabe**: a senha pessoal numérica de 8 a 10 dígitos.

O detalhe interessante é o **teclado virtual sobrecarregado**: em vez de digitar a senha no teclado físico, o usuário clica em botões na tela. Cada botão exibe **dois dígitos**, e os dígitos são **redistribuídos aleatoriamente entre os 5 botões a cada clique**.

```
Exemplo: 5 botões com pares
┌─────┬─────┬─────┐
│ 1 9 │ 8 7 │ 3 5 │
├─────┼─────┘     │
│ 6 2 │ 0 4 │     │
└─────┴─────┘
```

**Para que serve essa sobrecarga?**

- **Contra keyloggers:** não há tecla pressionada
- **Contra observação visual:** mesmo se alguém vê o usuário clicar nos botões, não consegue determinar a senha (porque cada botão tem 2 dígitos possíveis, e cada clique embaralha as posições)

**Como o sistema valida?**

O servidor não sabe a senha em texto plano — só tem o hash bcrypt. Quando o usuário clica em 8 botões, o servidor recebe 8 **pares de dígitos** (e não sabe qual dos dois cada clique representou).

Solução: **força bruta restrita** sobre o espaço de combinações possíveis. Para 8 dígitos, são 2^8 = 256 combinações. Para 10 dígitos, 1024. O sistema testa cada uma com `bcrypt.checkPassword` até encontrar a correta.

Esse é o "brute-force tree" mostrado no slide 13 do detalhamento.

**Bloqueio temporal:** 3 erros consecutivos → conta bloqueada por 2 minutos. Outros usuários podem usar o sistema durante esse tempo. Quando o tempo passa, o desbloqueio é automático.

**Arquivos:**
- [src/TelaLoginSenha.java](src/TelaLoginSenha.java) (interface do teclado virtual)
- [src/AuthService.java](src/AuthService.java) método `verificarSenha` (lógica do brute-force tree)

#### Etapa 3 — Token TOTP

Algo que **você tem**: o smartphone com Google Authenticator. Veja seção dedicada a seguir.

**Arquivos:**
- [src/TelaLoginTOTP.java](src/TelaLoginTOTP.java) (interface)
- [src/TOTP.java](src/TOTP.java) (algoritmo)

---

## 13. TOTP — Time-based One-Time Password

### O que é

**TOTP** (Time-based One-Time Password) é um padrão para gerar códigos curtos (6 dígitos) que mudam a cada 30 segundos. É o que o **Google Authenticator** faz no seu celular.

Definido por três RFCs:

- **RFC 4226** — HOTP (HMAC-based One-Time Password, base do algoritmo)
- **RFC 6238** — TOTP (versão baseada em tempo)
- **RFC 4648** — codificações BASE16, BASE32, BASE64

### Como funciona

Servidor e cliente (Google Authenticator) compartilham:

1. Uma **chave secreta** de 20 bytes (160 bits), gerada no cadastro
2. Um **contador de tempo** baseado no horário Unix:
   ```
   T = floor(unix_time / 30)
   ```
   `T` muda exatamente uma vez a cada 30 segundos.

A cada momento, ambos calculam:

```
hash = HMAC-SHA1(chave_secreta, T)         (resultado: 20 bytes)
otp  = truncar(hash) mod 10^6              (resultado: número de 6 dígitos)
```

Como ambos têm a mesma chave e o mesmo tempo, ambos chegam ao mesmo número. **Sem precisar trocar nenhuma mensagem.**

### Truncamento dinâmico (a parte engenhosa)

Reduzir um hash de 20 bytes para 6 dígitos decimais não é trivial — quer-se uma distribuição uniforme. O RFC 4226 define:

```
offset = último byte do hash & 0x0F            (valor entre 0 e 15)
binary = (hash[offset+0] & 0x7F) << 24 |
         (hash[offset+1] & 0xFF) << 16 |
         (hash[offset+2] & 0xFF) << 8  |
         (hash[offset+3] & 0xFF)
otp = binary mod 10^6
```

O `& 0x7F` no primeiro byte garante que `binary` seja positivo (limpa o bit de sinal). O `mod 10^6` corta para 6 dígitos. Se der menos que 6 dígitos, completa com zeros à esquerda.

### Janela de tolerância

Relógios podem estar levemente dessincronizados. Por isso, o validador testa **três valores**:

- `T - 1` (30 segundos atrás)
- `T` (agora)
- `T + 1` (30 segundos no futuro)

Se o código bate com qualquer um dos três, aceita.

### Cadastro no Google Authenticator

Para configurar, o usuário precisa transferir a chave secreta do servidor para o celular. Duas formas:

1. **Digitar a chave em BASE32** — exemplo: `JXXMGK33L7S3H3JOY5GMUXC7G7ASJHTD`. BASE32 é uma codificação que usa só letras e dígitos legíveis (sem caracteres ambíguos como `0`/`O`, `1`/`I`).

2. **Escanear um QR code** que codifica uma URI no formato:
   ```
   otpauth://totp/Cofre%20Digital:admin@inf1416.puc-rio.br
            ?secret=JXXMGK33L7S3H3JOY5GMUXC7G7ASJHTD
            &issuer=Cofre%20Digital
   ```

No nosso cofre, exibimos as duas opções (texto + URI) na [src/TelaTotpSetup.java](src/TelaTotpSetup.java).

### Detalhe importante: o segredo é cifrado no banco

A chave secreta TOTP **não fica em texto plano** no banco. Ela é cifrada com AES, usando uma chave derivada da **senha pessoal** do usuário.

Consequência: na etapa 3 do login, o sistema só consegue decifrar o segredo TOTP **se** o usuário acabou de passar pela etapa 2 com a senha certa. Isso amarra as duas etapas e impede que um atacante que de alguma forma roubasse o banco pudesse usar o segredo TOTP isoladamente.

### Restrição do enunciado

O enunciado é explícito: a classe TOTP **só pode usar** `javax.crypto.Mac`, `javax.crypto.spec.SecretKeySpec`, `java.util.Date` e a classe `Base32` fornecida. **Nenhuma biblioteca de terceiros para gerar TOTP é permitida.**

Por isso a implementação em [src/TOTP.java](src/TOTP.java) é "manual" — fizemos o HMAC-SHA1, o truncamento e a conversão para 6 dígitos do zero.

---

## 14. Como o cofre funciona — fluxo completo

Junta tudo o que vimos até agora num passo a passo.

### 14.1 Partida do sistema

O sistema verifica se já existe administrador cadastrado.

#### Primeira execução (banco vazio)
- Abre a **tela de cadastro do administrador**
- Pede: certificado, chave privada, frase secreta, senha pessoal, confirmação da senha
- Veja seção 14.2 para o fluxo de cadastro

#### Execução normal (já há admin)
- Abre a **tela de validação do administrador**
- Pede apenas a frase secreta da chave privada do admin
- O sistema:
  1. Lê o certificado e a chave privada cifrada do banco (tabela `Chaveiro`)
  2. Decifra a chave privada com a frase
  3. Gera 9216 bytes aleatórios
  4. Assina esses bytes com a chave privada
  5. Verifica a assinatura com a chave pública do certificado
- **Se a verificação passar:** frase + chave privada + certificado do admin ficam **em memória** (objeto `Sessao`) até o sistema encerrar. Vai para a tela de login.
- **Se falhar:** o sistema encerra (sem persistir nada).

### 14.2 Cadastro de novo usuário

Tanto na primeira execução (admin) quanto depois (usuários comuns, via opção 1 do menu, quando o admin está logado).

```
┌────────────────────────────────────────┐
│ 1. Usuário fornece os 5 campos:        │
│    • caminho do certificado            │
│    • caminho da chave privada cifrada  │
│    • frase secreta                     │
│    • senha pessoal                     │
│    • confirmação da senha              │
│    • (cadastro de usuário): grupo      │
└────────────────────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────┐
│ 2. Validação de senha:                 │
│    • 8 a 10 dígitos                    │
│    • apenas 0-9                        │
│    • sem dígitos repetidos consecutivos│
│      (regra: regex (.)\1+)             │
│    • confirmação bate                  │
└────────────────────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────┐
│ 3. Carrega cert + chave privada;       │
│    assina 9216 bytes aleatórios;       │
│    verifica assinatura com chave       │
│    pública do certificado.             │
│    (Erros 6004-6007 conforme o caso.)  │
└────────────────────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────┐
│ 4. Tela de confirmação dos dados       │
│    do certificado, mostrando:          │
│    Versão, Série, Validade, Tipo de    │
│    Assinatura, Emissor, Sujeito, Email │
└────────────────────────────────────────┘
                  │
        ┌─────────┴──────────┐
        │ Confirmar          │
        ▼                    
┌────────────────────────────────────────┐
│ 5. Efetivar cadastro:                  │
│    • extrai e-mail (login) e nome      │
│      do certificado                    │
│    • verifica unicidade do e-mail      │
│    • gera segredo TOTP (20 bytes)      │
│      em BASE32                         │
│    • cifra TOTP com K_AES derivado     │
│      da senha pessoal                  │
│    • gera hash bcrypt da senha         │
│    • salva nas tabelas Usuarios e      │
│      Chaveiro                          │
└────────────────────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────┐
│ 6. Mostra BASE32 + URI otpauth para o  │
│    usuário cadastrar no Google         │
│    Authenticator.                      │
└────────────────────────────────────────┘
```

### 14.3 Login (3 etapas)

```
┌─────────────────────┐
│ Etapa 1: E-mail     │
│ (identificação)     │
└──────────┬──────────┘
           │ válido + não bloqueado
           ▼
┌─────────────────────┐       ┌─────────────────────┐
│ Etapa 2: Senha      │ erro  │ 3º erro → bloqueia  │
│ (teclado virtual    │──────▶│ 2 min e volta etapa │
│ + brute-force tree) │  3x   │ 1                   │
└──────────┬──────────┘       └─────────────────────┘
           │ bcrypt.checkPassword bate
           │ (guarda senha plana em Sessao para usar
           │  no decrypt do segredo TOTP)
           ▼
┌─────────────────────┐       ┌─────────────────────┐
│ Etapa 3: TOTP       │ erro  │ 3º erro → bloqueia  │
│ (validar código com │──────▶│ 2 min e volta etapa │
│ janela ±30s)        │  3x   │ 1                   │
└──────────┬──────────┘       └─────────────────────┘
           │ código válido
           ▼
┌─────────────────────┐
│ Menu Principal      │
│ (log 1003: sessão   │
│  iniciada)          │
└─────────────────────┘
```

### 14.4 Consulta da pasta segura (opção 2 do menu)

A operação mais complexa do sistema, mas que junta praticamente tudo:

```
Usuário informa:
  • caminho da pasta segura
  • frase secreta dele mesmo
                  │
                  ▼
1. Valida a frase carregando a chave privada do usuário corrente.
                  │
                  ▼
2. Abre o índice da pasta:
   ┌─────────────────────────────────────────────────────┐
   │ index.env  → rsaDecrypt com Kpriv DO ADMIN          │
   │             (o índice é dele, foi assinado por ele) │
   │             → semente (~5 bytes)                    │
   │                                                     │
   │ semente    → SHA1PRNG → K_AES                       │
   │                                                     │
   │ index.enc  → aesDecrypt com K_AES                   │
   │             → texto do índice (linhas)              │
   │                                                     │
   │ index.asd  → verifica com Kpub do admin             │
   │             (integridade + autenticidade)           │
   └─────────────────────────────────────────────────────┘
                  │
                  ▼
3. Filtra linhas do índice:
   • Admin: vê todas
   • Usuário comum: vê apenas linhas com DONO=ele
                  │
                  ▼
4. Apresenta lista de arquivos disponíveis ao usuário.
                  │
                  ▼
5. Usuário seleciona um arquivo (ex: XXYYZZ11).
                  │
                  ▼
6. Controle de acesso (ACL):
   • Se DONO ≠ usuário → 7012 (negado) e para
   • Se DONO = usuário → continua
                  │
                  ▼
7. Decifra o arquivo individual:
   ┌─────────────────────────────────────────────────────┐
   │ XXYYZZ11.env → rsaDecrypt com Kpriv do DONO         │
   │              → semente                              │
   │ semente     → SHA1PRNG → K_AES                      │
   │ XXYYZZ11.enc → aesDecrypt com K_AES → conteúdo      │
   │ XXYYZZ11.asd → verifica com Kpub do DONO            │
   └─────────────────────────────────────────────────────┘
                  │
                  ▼
8. Salva conteúdo com o nome secreto original (teste01.docx).
```

Veja [src/TelaConsultaPasta.java](src/TelaConsultaPasta.java) — toda essa lógica está lá.

### 14.5 Encerramento

- Limpa `Sessao` (frase do admin, chave privada, certificado, senha plana, contadores)
- Fecha conexão com banco
- Registra log 1002 (sistema encerrado)

---

## 15. Banco de dados e auditoria

### Tabelas

O banco é SQLite (arquivo `cofre.db` na raiz). Cinco tabelas:

| Tabela | Conteúdo | Campos importantes |
|--------|----------|--------------------|
| **Usuarios** | Cada usuário do sistema | UID, login_name (e-mail), nome, senha_hash (bcrypt), totp_secret (cifrado), GID, KID, contadores, flag e timestamp de bloqueio |
| **Chaveiro** | Certificados e chaves privadas cifradas | KID, UID, certificado (PEM), chave_privada (BLOB binário cifrado) |
| **Grupos** | Grupos do sistema | GID (1=Administrador, 2=Usuario), nome |
| **Mensagens** | Catálogo de mensagens de log | MID, texto |
| **Registros** | Eventos de auditoria | RID, timestamp, MID, UID, arq_name |

A separação Mensagens × Registros segue o princípio do enunciado: **"Não é permitido armazenar o texto das mensagens dos registros nessa tabela"**. Em Registros guarda-se apenas o `MID` (identificador da mensagem); o texto vem da tabela Mensagens via JOIN.

### Mensagens de log

A tabela Mensagens é populada na criação do banco com **60 mensagens predefinidas**, com códigos como:

- **1001-1006:** sistema (iniciado, encerrado, partida)
- **2001-2005:** etapa 1 do login
- **3001-3007:** etapa 2 do login
- **4001-4007:** etapa 3 do login
- **5001-5004:** menu principal
- **6001-6010:** cadastro
- **7001-7016:** consulta de pasta
- **8001-8004:** saída

As mensagens podem conter os placeholders `<login_name>` e `<arq_name>`, que são substituídos pelos valores reais na hora de exibir.

### Programa de auditoria — `LogView`

O enunciado é claro: o programa que mostra os logs **não pode fazer parte do cofre**. Por quê? Porque o administrador (a única pessoa autorizada a ver os logs) precisa poder auditar sem precisar autenticar-se no cofre completo — e o auditor pode até ser uma pessoa diferente do administrador operacional.

**Arquivo separado:** [src/LogView.java](src/LogView.java)

**Como ele funciona:**

1. Recebe o caminho da chave privada do administrador como argumento de linha de comando:
   ```bash
   java LogView Pacote-T3/Keys/admin-pkcs8-aes.key
   ```
2. Pede a frase secreta via `Console.readPassword()` — **sem echo** no terminal, ou seja, os caracteres digitados não aparecem
3. Carrega a chave privada do arquivo informado
4. Lê o certificado correspondente do banco
5. Gera um array aleatório de **2048 bytes**
6. Assina com a chave privada, verifica com a pública do certificado
7. Se passar, mostra todos os registros em ordem cronológica com `<login_name>` e `<arq_name>` substituídos
8. Se falhar, encerra

**Detalhe técnico:** o LogView abre o banco em modo **read-only** (`jdbc:sqlite:file:cofre.db?mode=ro`), permitindo rodar em paralelo com o cofre principal sem causar lock.

---

## 16. Mapa do código — onde está cada coisa

Referência rápida para navegar no projeto.

### Camada de criptografia

| Arquivo | Responsabilidade |
|---------|------------------|
| [src/CryptoUtils.java](src/CryptoUtils.java) | Todas as primitivas: AES, RSA, assinatura, certificado, chave privada, bcrypt, derivação de chave |
| [src/TOTP.java](src/TOTP.java) | Implementação RFC 6238 (HMAC-SHA1 + truncamento dinâmico) |
| [src/Base32.java](src/Base32.java) | Utilitário fornecido pelo enunciado |

### Modelo e persistência

| Arquivo | Responsabilidade |
|---------|------------------|
| [src/Usuario.java](src/Usuario.java) | Entidade de usuário (login, nome, contadores, bloqueio) |
| [src/Database.java](src/Database.java) | Schema SQLite, queries, populamento de Grupos e Mensagens |
| [src/Sessao.java](src/Sessao.java) | Estado em memória durante execução (frase admin, chave privada admin, cert admin, senha plana temporária, contadores de falha) |

### Lógica de negócio

| Arquivo | Responsabilidade |
|---------|------------------|
| [src/AuthService.java](src/AuthService.java) | Validação da chave admin, brute-force tree de senha, bloqueio temporal, cadastro |
| [src/Validacoes.java](src/Validacoes.java) | Regras de senha (8-10 dígitos, sem repetição) e formato de e-mail |

### Interface (JavaFX)

| Arquivo | Responsabilidade |
|---------|------------------|
| [src/App.java](src/App.java) | Ponto de entrada, lifecycle, navegação entre telas |
| [src/Main.java](src/Main.java) | Bootstrapper que chama `App.main` |
| [src/UI.java](src/UI.java) | Primitivas visuais (cards, botões, campos, cores) |
| [src/TelaValidacaoAdmin.java](src/TelaValidacaoAdmin.java) | Tela de partida (segunda execução em diante): frase secreta do admin |
| [src/TelaCadastroAdmin.java](src/TelaCadastroAdmin.java) | Primeira execução: cadastro do administrador |
| [src/TelaCadastroUsuario.java](src/TelaCadastroUsuario.java) | Admin cadastrando outros usuários |
| [src/TelaConfirmacaoCadastro.java](src/TelaConfirmacaoCadastro.java) | Confirmação dos campos do certificado |
| [src/TelaTotpSetup.java](src/TelaTotpSetup.java) | Exibe BASE32 e URI otpauth para Google Authenticator |
| [src/TelaLoginEmail.java](src/TelaLoginEmail.java) | Etapa 1 do login |
| [src/TelaLoginSenha.java](src/TelaLoginSenha.java) | Etapa 2 do login (teclado virtual sobrecarregado) |
| [src/TelaLoginTOTP.java](src/TelaLoginTOTP.java) | Etapa 3 do login (TOTP) |
| [src/TelaMenu.java](src/TelaMenu.java) | Menu principal (filtrado por grupo) |
| [src/TelaConsultaPasta.java](src/TelaConsultaPasta.java) | Opção 2 do menu: envelope, índice, listagem com ACL, decifragem de arquivo |
| [src/TelaSaida.java](src/TelaSaida.java) | Opção 3 do menu: três botões (Encerrar Sessão, Encerrar Sistema, Voltar) |

### Programa separado

| Arquivo | Responsabilidade |
|---------|------------------|
| [src/LogView.java](src/LogView.java) | Auditoria CLI: lista registros do banco em ordem cronológica |

---

## 17. Como rodar e testar

### Dependências

Em [lib/](lib/) (já gitignored):

- `javafx/` — JavaFX SDK 21 (para a interface gráfica)
- `bcprov-jdk18on-1.78.1.jar` — BouncyCastle (para bcrypt)
- `sqlite-jdbc-3.46.1.3.jar` — driver SQLite
- `slf4j-api-2.0.13.jar` + `slf4j-simple-2.0.13.jar` — logging (dependência do SQLite)

### Compilar

```bash
javac --module-path lib/javafx/lib --add-modules javafx.controls,javafx.fxml \
      -cp "lib/bcprov-jdk18on-1.78.1.jar:lib/sqlite-jdbc-3.46.1.3.jar" \
      -d out src/*.java
```

### Rodar o cofre

```bash
java --module-path lib/javafx/lib --add-modules javafx.controls,javafx.fxml \
     -cp "out:lib/bcprov-jdk18on-1.78.1.jar:lib/sqlite-jdbc-3.46.1.3.jar:lib/slf4j-api-2.0.13.jar:lib/slf4j-simple-2.0.13.jar" \
     App
```

### Rodar o LogView (em paralelo ou separado)

```bash
java -cp "out:lib/bcprov-jdk18on-1.78.1.jar:lib/sqlite-jdbc-3.46.1.3.jar:lib/slf4j-api-2.0.13.jar:lib/slf4j-simple-2.0.13.jar" \
     LogView Pacote-T3/Keys/admin-pkcs8-aes.key
```

### Testar com os artefatos do professor

A pasta `Pacote-T3/` contém todos os arquivos necessários para um teste end-to-end:

**Identidades:**

| Usuário | Cert | Chave privada | Frase secreta |
|---------|------|---------------|---------------|
| admin | `Pacote-T3/Keys/admin-x509.crt` | `Pacote-T3/Keys/admin-pkcs8-aes.key` | `admin` |
| user01 | `Pacote-T3/Keys/user01-x509.crt` | `Pacote-T3/Keys/user01-pkcs8-aes.key` | `user01` |
| user02 | `Pacote-T3/Keys/user02-x509.crt` | `Pacote-T3/Keys/user02-pkcs8-aes.key` | `user02` |

**Pasta segura pronta:** `Pacote-T3/Files/` contém `index.{enc,env,asd}` e três arquivos secretos `XXYYZZ00`, `XXYYZZ11`, `XXYYZZ22` já cifrados, envelopados e assinados.

**Roteiro de teste end-to-end:**

1. Apagar `cofre.db` (se existir) para começar do zero
2. Rodar o cofre → tela de cadastro do administrador aparece
3. Cadastrar admin com os arquivos do `Pacote-T3/Keys/`, frase `admin`, senha `13572468`
4. Confirmar dados do certificado
5. Cadastrar TOTP no Google Authenticator (BASE32 exibido)
6. Login completo (e-mail → senha → TOTP)
7. Opção 1 do menu: cadastrar user01 e user02
8. Opção 2 do menu: caminho `Pacote-T3/Files`, frase `admin` → lista 3 arquivos
9. Decifrar um arquivo → grava `teste00.docx` (ou outro) decifrado
10. Sair, fazer login como user01 → opção 2 vê só o arquivo dele
11. Rodar `LogView` para auditar a sequência completa

---

Quaisquer dúvidas ou pontos a expandir, abra o arquivo correspondente do código e veja os comentários no método específico.
