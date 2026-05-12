import java.io.Console;
import java.io.IOException;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

public class LogView {

    private static final int CHALLENGE_BYTES = 2048;

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

        Database db = new Database();
        try {
            Usuario admin = db.getAdmin();
            if (admin == null || admin.getKid() == null) {
                System.err.println("Nao existe administrador cadastrado no sistema.");
                System.exit(1);
            }
            Database.Chaveiro ch = db.buscarChaveiroPorUid(admin.getUid());
            if (ch == null) {
                System.err.println("Chaveiro do administrador nao encontrado.");
                System.exit(1);
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
                .generateCertificate(new java.io.ByteArrayInputStream(ch.certificadoPem.getBytes()));
            PublicKey pub = cert.getPublicKey();

            byte[] desafio = new byte[CHALLENGE_BYTES];
            new SecureRandom().nextBytes(desafio);
            byte[] sig = CryptoUtils.assinar(desafio, priv);
            if (!CryptoUtils.verificar(desafio, sig, pub)) {
                System.err.println("Verificacao da chave privada falhou. Encerrando.");
                System.exit(1);
            }

            List<Database.RegistroLinha> registros = db.listarRegistrosOrdenados();
            for (Database.RegistroLinha r : registros) {
                String texto = r.texto;
                if (r.loginName != null) texto = texto.replace("<login_name>", r.loginName);
                if (r.arqName  != null)  texto = texto.replace("<arq_name>",   r.arqName);
                System.out.printf("%s  [%04d]  %s%n", r.timestamp, r.mid, texto);
            }
        } finally {
            db.fechar();
        }
    }

    private static String lerFraseSecreta() {
        Console c = System.console();
        if (c != null) {
            char[] pass = c.readPassword("Frase secreta da chave privada: ");
            if (pass == null) return "";
            return new String(pass);
        }
        // Fallback para execucao em IDEs sem TTY
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
