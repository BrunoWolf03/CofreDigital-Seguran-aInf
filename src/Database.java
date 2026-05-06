import java.sql.*;

public class Database {

    private static final String DB_FILE = "cofre.db";
    private Connection conn;

    public Database() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
            conn.createStatement().execute("PRAGMA foreign_keys = ON");
            criarTabelas();
            popularGrupos();
            popularMensagens();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao inicializar banco de dados: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Schema
    // -------------------------------------------------------------------------

    private void criarTabelas() throws SQLException {
        Statement st = conn.createStatement();

        st.execute("""
            CREATE TABLE IF NOT EXISTS Grupos (
                GID   INTEGER PRIMARY KEY,
                nome  TEXT    NOT NULL UNIQUE
            )""");

        st.execute("""
            CREATE TABLE IF NOT EXISTS Usuarios (
                UID           INTEGER PRIMARY KEY AUTOINCREMENT,
                login_name    TEXT    NOT NULL UNIQUE,
                nome          TEXT    NOT NULL,
                senha_hash    TEXT    NOT NULL,
                totp_secret   TEXT    NOT NULL,
                GID           INTEGER NOT NULL REFERENCES Grupos(GID),
                KID           INTEGER,
                bloqueado     INTEGER NOT NULL DEFAULT 0,
                falhas_login  INTEGER NOT NULL DEFAULT 0,
                total_acessos INTEGER NOT NULL DEFAULT 0,
                bloqueado_ate DATETIME
            )""");

        st.execute("""
            CREATE TABLE IF NOT EXISTS Chaveiro (
                KID          INTEGER PRIMARY KEY AUTOINCREMENT,
                UID          INTEGER NOT NULL REFERENCES Usuarios(UID),
                certificado  TEXT    NOT NULL,
                chave_privada BLOB   NOT NULL
            )""");

        st.execute("""
            CREATE TABLE IF NOT EXISTS Mensagens (
                MID   INTEGER PRIMARY KEY,
                texto TEXT    NOT NULL
            )""");

        st.execute("""
            CREATE TABLE IF NOT EXISTS Registros (
                RID       INTEGER  PRIMARY KEY AUTOINCREMENT,
                timestamp DATETIME NOT NULL DEFAULT (datetime('now','localtime')),
                MID       INTEGER  NOT NULL REFERENCES Mensagens(MID),
                UID       INTEGER  REFERENCES Usuarios(UID),
                arq_name  TEXT
            )""");
    }

    // -------------------------------------------------------------------------
    // Seed: Grupos
    // -------------------------------------------------------------------------

    private void popularGrupos() throws SQLException {
        conn.createStatement().execute("""
            INSERT OR IGNORE INTO Grupos (GID, nome) VALUES
                (1, 'Administrador'),
                (2, 'Usuario')
            """);
    }

    // -------------------------------------------------------------------------
    // Seed: Mensagens (Tabela de Mensagens de Registro do spec)
    // -------------------------------------------------------------------------

    private void popularMensagens() throws SQLException {
        String sql = "INSERT OR IGNORE INTO Mensagens (MID, texto) VALUES (?, ?)";
        PreparedStatement ps = conn.prepareStatement(sql);

        Object[][] msgs = {
            {1001, "Sistema iniciado."},
            {1002, "Sistema encerrado."},
            {1003, "Sessao iniciada para <login_name>."},
            {1004, "Sessao encerrada para <login_name>."},
            {1005, "Partida do sistema iniciada para cadastro do administrador."},
            {1006, "Partida do sistema iniciada para operacao normal pelos usuarios."},
            {2001, "Autenticacao etapa 1 iniciada."},
            {2002, "Autenticacao etapa 1 encerrada."},
            {2003, "Login name <login_name> identificado com acesso liberado."},
            {2004, "Login name <login_name> identificado com acesso bloqueado."},
            {2005, "Login name <login_name> nao identificado."},
            {3001, "Autenticacao etapa 2 iniciada para <login_name>."},
            {3002, "Autenticacao etapa 2 encerrada para <login_name>."},
            {3003, "Senha pessoal verificada positivamente para <login_name>."},
            {3004, "Primeiro erro da senha pessoal contabilizado para <login_name>."},
            {3005, "Segundo erro da senha pessoal contabilizado para <login_name>."},
            {3006, "Terceiro erro da senha pessoal contabilizado para <login_name>."},
            {3007, "Acesso do usuario <login_name> bloqueado pela autenticacao etapa 2."},
            {4001, "Autenticacao etapa 3 iniciada para <login_name>."},
            {4002, "Autenticacao etapa 3 encerrada para <login_name>."},
            {4003, "Token verificado positivamente para <login_name>."},
            {4004, "Primeiro erro de token contabilizado para <login_name>."},
            {4005, "Segundo erro de token contabilizado para <login_name>."},
            {4006, "Terceiro erro de token contabilizado para <login_name>."},
            {4007, "Acesso do usuario <login_name> bloqueado pela autenticacao etapa 3."},
            {5001, "Tela principal apresentada para <login_name>."},
            {5002, "Opcao 1 do menu principal selecionada por <login_name>."},
            {5003, "Opcao 2 do menu principal selecionada por <login_name>."},
            {5004, "Opcao 3 do menu principal selecionada por <login_name>."},
            {6001, "Tela de cadastro apresentada para <login_name>."},
            {6002, "Botao cadastrar pressionado por <login_name>."},
            {6003, "Senha pessoal invalida fornecida por <login_name>."},
            {6004, "Caminho do certificado digital invalido fornecido por <login_name>."},
            {6005, "Chave privada verificada negativamente para <login_name> (caminho invalido)."},
            {6006, "Chave privada verificada negativamente para <login_name> (frase secreta invalida)."},
            {6007, "Chave privada verificada negativamente para <login_name> (assinatura digital invalida)."},
            {6008, "Confirmacao de dados aceita por <login_name>."},
            {6009, "Confirmacao de dados rejeitada por <login_name>."},
            {6010, "Botao voltar de cadastro para o menu principal pressionado por <login_name>."},
            {7001, "Tela de consulta de arquivos secretos apresentada para <login_name>."},
            {7002, "Botao voltar de consulta para o menu principal pressionado por <login_name>."},
            {7003, "Botao Listar de consulta pressionado por <login_name>."},
            {7004, "Caminho de pasta invalido fornecido por <login_name>."},
            {7005, "Arquivo de indice decriptado com sucesso para <login_name>."},
            {7006, "Arquivo de indice verificado (integridade e autenticidade) com sucesso para <login_name>."},
            {7007, "Falha na decriptacao do arquivo de indice para <login_name>."},
            {7008, "Falha na verificacao (integridade e autenticidade) do arquivo de indice para <login_name>."},
            {7009, "Lista de arquivos presentes no indice apresentada para <login_name>."},
            {7010, "Arquivo <arq_name> selecionado por <login_name> para decriptacao."},
            {7011, "Acesso permitido ao arquivo <arq_name> para <login_name>."},
            {7012, "Acesso negado ao arquivo <arq_name> para <login_name>."},
            {7013, "Arquivo <arq_name> decriptado com sucesso para <login_name>."},
            {7014, "Arquivo <arq_name> verificado (integridade e autenticidade) com sucesso para <login_name>."},
            {7015, "Falha na decriptacao do arquivo <arq_name> para <login_name>."},
            {7016, "Falha na verificacao (integridade e autenticidade) do arquivo <arq_name> para <login_name>."},
            {8001, "Tela de saida apresentada para <login_name>."},
            {8002, "Botao encerrar sessao pressionado por <login_name>."},
            {8003, "Botao encerrar sistema pressionado por <login_name>."},
            {8004, "Botao voltar de sair para o menu principal pressionado por <login_name>."},
        };

        for (Object[] row : msgs) {
            ps.setInt(1, (int) row[0]);
            ps.setString(2, (String) row[1]);
            ps.addBatch();
        }
        ps.executeBatch();
    }

    // -------------------------------------------------------------------------
    // Usuarios
    // -------------------------------------------------------------------------

    public boolean existeUsuario() {
        try {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM Usuarios");
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void salvarUsuario(Usuario u) {
        try {
            PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO Usuarios
                    (login_name, nome, senha_hash, totp_secret, GID, bloqueado, falhas_login)
                VALUES (?, ?, ?, ?, ?, 0, 0)
                """);
            ps.setString(1, u.getEmail());
            ps.setString(2, u.getNome());
            ps.setString(3, u.getSenhaHash());
            ps.setString(4, u.getTotpSecret());
            ps.setInt(5, u.isAdmin() ? 1 : 2);
            ps.executeUpdate();

            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT last_insert_rowid()");
            u.setUid(rs.getInt(1));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Usuario getAdmin() {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM Usuarios WHERE GID = 1 LIMIT 1");
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            return mapUsuario(rs);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Usuario buscarPorEmail(String email) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM Usuarios WHERE login_name = ?");
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            return mapUsuario(rs);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void atualizarUsuario(Usuario u) {
        try {
            PreparedStatement ps = conn.prepareStatement("""
                UPDATE Usuarios SET
                    bloqueado     = ?,
                    falhas_login  = ?,
                    total_acessos = ?,
                    bloqueado_ate = ?
                WHERE UID = ?
                """);
            ps.setInt(1, u.isBloqueado() ? 1 : 0);
            ps.setInt(2, u.getFalhasLogin());
            ps.setInt(3, u.getTotalAcessos());
            ps.setString(4, u.getBloqueadoAte());
            ps.setInt(5, u.getUid());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int totalUsuarios() {
        try {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM Usuarios");
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------------------------
    // Chaveiro
    // -------------------------------------------------------------------------

    public int salvarChaveiro(int uid, String certificadoPem, byte[] chavePrivadaEnc) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Chaveiro (UID, certificado, chave_privada) VALUES (?, ?, ?)");
            ps.setInt(1, uid);
            ps.setString(2, certificadoPem);
            ps.setBytes(3, chavePrivadaEnc);
            ps.executeUpdate();

            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT last_insert_rowid()");
            int kid = rs.getInt(1);

            conn.createStatement().execute(
                "UPDATE Usuarios SET KID = " + kid + " WHERE UID = " + uid);
            return kid;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------------------------
    // Registros (log)
    // -------------------------------------------------------------------------

    public void registrarLog(int mid) {
        registrarLog(mid, null, null);
    }

    public void registrarLog(int mid, Integer uid) {
        registrarLog(mid, uid, null);
    }

    public void registrarLog(int mid, Integer uid, String arqName) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Registros (MID, UID, arq_name) VALUES (?, ?, ?)");
            ps.setInt(1, mid);
            if (uid != null) ps.setInt(2, uid); else ps.setNull(2, Types.INTEGER);
            if (arqName != null) ps.setString(3, arqName); else ps.setNull(3, Types.VARCHAR);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ResultSet buscarRegistrosOrdenados() throws SQLException {
        return conn.createStatement().executeQuery("""
            SELECT r.timestamp, r.MID, m.texto, r.UID, u.login_name, r.arq_name
            FROM Registros r
            JOIN Mensagens m ON r.MID = m.MID
            LEFT JOIN Usuarios u ON r.UID = u.UID
            ORDER BY r.timestamp ASC
            """);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Usuario mapUsuario(ResultSet rs) throws SQLException {
        Usuario u = new Usuario(
            rs.getString("login_name"),
            rs.getString("nome"),
            rs.getString("senha_hash"),
            rs.getString("totp_secret"),
            rs.getInt("GID") == 1 ? "admin" : "user"
        );
        u.setUid(rs.getInt("UID"));
        u.setBloqueado(rs.getInt("bloqueado") == 1);
        u.setFalhasLogin(rs.getInt("falhas_login"));
        u.setTotalAcessos(rs.getInt("total_acessos"));
        u.setBloqueadoAte(rs.getString("bloqueado_ate"));
        return u;
    }

    public void fechar() {
        try {
            if (conn != null) conn.close();
        } catch (SQLException ignored) {}
    }
}
