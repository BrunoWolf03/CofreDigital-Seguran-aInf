2212576 - Bruno Wolf
??? - Guilherme Senko

# 🔐 Cofre Digital

Sistema desenvolvido em **Java (JDK SE)** para a disciplina **INF1416 – Segurança da Informação (PUC-Rio)**.

O projeto implementa um **cofre digital seguro**, utilizando autenticação multifator, criptografia simétrica e assimétrica, assinatura digital e controle de acesso para proteção de arquivos secretos.

---

## 📚 Sobre o Projeto

O Cofre Digital é uma aplicação responsável por proteger uma pasta contendo arquivos sigilosos. O sistema permite:

- Cadastro de usuários
- Login com autenticação em 3 etapas
- Controle de grupos (Administrador / Usuário)
- Criptografia e descriptografia de arquivos
- Verificação de integridade e autenticidade
- Registro de logs de auditoria
- Consulta segura de arquivos secretos

---

## 🔒 Recursos de Segurança Implementados

### ✅ Autenticação Multifator (MFA)

O login ocorre em 3 etapas:

1. **Login (E-mail válido)**
2. **Senha pessoal numérica via teclado virtual**
3. **Token TOTP (Google Authenticator)**

---

### 🔐 Criptografia

#### Arquivos Secretos

- AES/ECB/PKCS5Padding

#### Chaves Privadas

- Armazenadas criptografadas

#### Tokens TOTP

- Segredo de 160 bits codificado em Base32

---

### ✍️ Assinatura Digital

Utilizada para garantir:

- Integridade
- Autenticidade

Arquivos `.asd`

---

### 🔑 Hash de Senhas

As senhas são armazenadas com:

- **bcrypt**
- versão `2y`
- custo `08`

---

## 🧱 Tecnologias Utilizadas

- Java SE
- SQLite / MySQL
- JDBC
- BouncyCastle
- Google Authenticator
- SHA1PRNG
- AES
- RSA
- X.509 Certificates

---

## 🗄️ Estrutura do Banco de Dados

O sistema utiliza 5 tabelas:

| Tabela     | Função                          |
|------------|---------------------------------|
| Usuarios   | Dados dos usuários              |
| Chaveiro   | Certificados e chaves privadas  |
| Grupos     | Perfis do sistema               |
| Mensagens  | Códigos de logs                 |
| Registros  | Auditoria                       |

---

## 👤 Perfis de Usuário

### Administrador

Pode:

- Cadastrar usuários
- Consultar arquivos
- Visualizar logs via `logView`

### Usuário

Pode:

- Consultar arquivos autorizados
- Acessar apenas arquivos próprios

---

## 📂 Estrutura dos Arquivos Seguros

| Arquivo      | Função                  |
|--------------|--------------------------|
| `index.enc`  | Índice criptografado     |
| `index.env`  | Envelope digital         |
| `index.asd`  | Assinatura digital       |
| `arquivo.enc`| Conteúdo criptografado   |
| `arquivo.env`| Chave protegida          |
| `arquivo.asd`| Assinatura               |

---

## ▶️ Como Executar

### Compilar

```bash
javac *.java