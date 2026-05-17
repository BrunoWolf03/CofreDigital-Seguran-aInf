import java.io.Console;
import java.io.IOException;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class LogView {

    private static final int CHALLENGE_BYTES = 2048;
    private static final String DB_URL = "jdbc:sqlite:file:cofre.db?mode=ro&immutable=0&cache=shared";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Uso: java LogView <caminho-da-chave-privada-do-admin>");
            System.exit(1);
        }
        Path keyPath = Path.of(args[0]);
        if (!keyPath.toFile().exists()) {
            System.err.println("Arquivo da chave nao encontrado: " + keyPath);
            System.exit(1);
        }

        String frase = lerFraseSecreta();

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String certPem;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                    "SELECT c.certificado FROM Usuarios u " +
                    "JOIN Chaveiro c ON c.UID = u.UID " +
                    "WHERE u.GID = 1 LIMIT 1")) {
                if (!rs.next()) {
                    System.err.println("Nao existe administrador cadastrado no sistema.");
                    System.exit(1); return;
                }
                certPem = rs.getString(1);
            }

            PrivateKey priv;
            try {
                priv = CryptoUtils.carregarChavePrivada(keyPath, frase);
            } catch (Exception e) {
                System.err.println("Falha ao carregar a chave privada (frase incorreta?).");
                System.exit(1); return;
            }

            X509Certificate cert = (X509Certificate)
                CertificateFactory.getInstance("X.509")
                .generateCertificate(new java.io.ByteArrayInputStream(certPem.getBytes()));
            PublicKey pub = cert.getPublicKey();

            byte[] desafio = new byte[CHALLENGE_BYTES];
            new SecureRandom().nextBytes(desafio);
            byte[] sig = CryptoUtils.assinar(desafio, priv);
            if (!CryptoUtils.verificar(desafio, sig, pub)) {
                System.err.println("Verificacao da chave privada falhou. Encerrando.");
                System.exit(1);
            }

            String sql = """
                SELECT r.timestamp, r.MID, m.texto, u.login_name, r.arq_name
                FROM Registros r
                JOIN Mensagens m ON r.MID = m.MID
                LEFT JOIN Usuarios u ON r.UID = u.UID
                ORDER BY r.timestamp ASC, r.RID ASC
                """;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    String ts        = rs.getString(1);
                    int    mid       = rs.getInt(2);
                    String texto     = rs.getString(3);
                    String loginName = rs.getString(4);
                    String arqName   = rs.getString(5);

                    if (loginName != null) texto = texto.replace("<login_name>", loginName);
                    if (arqName   != null) texto = texto.replace("<arq_name>",   arqName);
                    System.out.printf("%s  [%04d]  %s%n", ts, mid, texto);
                }
            }
        }
    }

    private static String lerFraseSecreta() {
        Console c = System.console();
        if (c != null) {
            char[] pass = c.readPassword("Frase secreta da chave privada: ");
            if (pass == null) return "";
            return new String(pass);
        }
        System.out.print("Frase secreta (visivel - sem TTY): ");
        try {
            byte[] buf = new byte[1024];
            int n = System.in.read(buf);
            if (n <= 0) return "";
            String s = new String(buf, 0, n);
            return s.endsWith("\n") ? s.substring(0, s.length()-1) : s;
        } catch (IOException e) {
            return "";
        }
    }
}
