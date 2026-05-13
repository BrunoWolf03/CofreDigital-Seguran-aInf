import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AuthService {

    private static final int   BLOQUEIO_MINUTOS = 2;
    private static final int   CHALLENGE_BYTES_ADMIN = 9216;
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Database db;

    public AuthService(Database db) {
        this.db = db;
    }

    // ============================================================
    // Validação da chave privada do administrador (partida normal)
    //   - decifra a chave privada com a frase
    //   - assina array aleatório de 9216 bytes
    //   - verifica com a chave pública do certificado armazenado
    // ============================================================

    public boolean validarChavePrivadaAdmin(String frase) {
        Usuario admin = db.getAdmin();
        if (admin == null || admin.getKid() == null) return false;

        Database.Chaveiro ch = db.buscarChaveiroPorUid(admin.getUid());
        if (ch == null) return false;

        try {
            byte[] keyBytes = ch.chavePrivadaEnc;
            java.nio.file.Path tmp = java.nio.file.Files.createTempFile("admkey", ".bin");
            java.nio.file.Files.write(tmp, keyBytes);
            PrivateKey priv;
            try {
                priv = CryptoUtils.carregarChavePrivada(tmp, frase);
            } finally {
                java.nio.file.Files.deleteIfExists(tmp);
            }

            X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new java.io.ByteArrayInputStream(ch.certificadoPem.getBytes()));
            PublicKey pub = cert.getPublicKey();

            byte[] desafio = new byte[CHALLENGE_BYTES_ADMIN];
            new SecureRandom().nextBytes(desafio);
            byte[] assinatura = CryptoUtils.assinar(desafio, priv);
            if (!CryptoUtils.verificar(desafio, assinatura, pub)) return false;

            Sessao.setFraseSecretaAdmin(frase);
            Sessao.setChavePrivadaAdmin(priv);
            Sessao.setCertificadoAdmin(cert);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ============================================================
    // Etapa 1 — identificação por e-mail
    //   Faz auto-desbloqueio se o tempo de bloqueio passou.
    // ============================================================

    public Usuario identificar(String email) {
        Usuario u = db.buscarPorEmail(email);
        if (u == null) return null;

        if (u.isBloqueado() && u.getBloqueadoAte() != null) {
            try {
                LocalDateTime ate = LocalDateTime.parse(u.getBloqueadoAte(), DT);
                if (LocalDateTime.now().isAfter(ate)) {
                    u.setBloqueado(false);
                    u.setFalhasLogin(0);
                    u.setBloqueadoAte(null);
                    db.atualizarUsuario(u);
                }
            } catch (Exception ignored) {}
        }
        return u;
    }

    // ============================================================
    // Etapa 2 — verificação da senha pessoal (brute-force tree)
    //   cliques: lista de pares [a,b] (cada botão tem 2 dígitos)
    //   Retorna true se alguma combinação 2^N passar no bcrypt.
    //   Guarda a senha plana descoberta em Sessao para a etapa 3.
    // ============================================================

    public boolean verificarSenha(Usuario u, List<int[]> cliques) {
        String hash = u.getSenhaHash();
        char[] atual = new char[cliques.size()];
        String achada = buscarRecursivo(cliques, 0, atual, hash);
        if (achada != null) {
            Sessao.setSenhaPlanaTemp(achada);
            u.resetarFalhas();
            return true;
        }
        return false;
    }

    private String buscarRecursivo(List<int[]> cliques, int i, char[] buf, String hash) {
        if (i == cliques.size()) {
            String tentativa = new String(buf);
            return CryptoUtils.bcryptCheck(tentativa, hash) ? tentativa : null;
        }
        for (int dig : cliques.get(i)) {
            buf[i] = (char) ('0' + dig);
            String r = buscarRecursivo(cliques, i + 1, buf, hash);
            if (r != null) return r;
        }
        return null;
    }

    // ============================================================
    // Etapa 3 — verificação do TOTP
    //   Decifra totp_secret com K_AES derivado da senha plana
    //   guardada na sessão; instancia TOTP e valida (±30s).
    // ============================================================

    public boolean verificarTOTP(Usuario u, String codigo) {
        try {
            String senha = Sessao.getSenhaPlanaTemp();
            if (senha == null) return false;
            String base32 = CryptoUtils.decifrarTotpSecret(u.getTotpSecret(), senha);
            TOTP totp = new TOTP(base32, 30);
            return totp.validateCode(codigo);
        } catch (Exception e) {
            return false;
        }
    }

    // ============================================================
    // Bloqueio temporal
    // ============================================================

    public void registrarFalhaSenha(Usuario u) {
        u.incrementarFalhas();
        db.atualizarUsuario(u);
    }

    public void registrarFalhaTotp(Usuario u) {
        u.incrementarFalhas();
        db.atualizarUsuario(u);
    }

    public void bloquearPor2Minutos(Usuario u) {
        u.setBloqueado(true);
        u.setBloqueadoAte(LocalDateTime.now().plusMinutes(BLOQUEIO_MINUTOS).format(DT));
        u.setFalhasLogin(0);
        db.atualizarUsuario(u);
    }

    // ============================================================
    // Cadastro de usuário (admin ou comum)
    //   Recebe os caminhos do cert e da chave; faz toda a validação
    //   e persiste tudo. Retorna o Usuario salvo ou null em erro.
    // ============================================================

    public static class ResultadoCadastro {
        public final boolean ok;
        public final String  mensagemErro;
        public final Integer codigoLog;   // MID a registrar (ex: 6004..6007)
        public final Usuario usuario;
        public final X509Certificate certificado;
        public final String  totpBase32;

        public ResultadoCadastro(boolean ok, String erro, Integer mid,
                                 Usuario u, X509Certificate c, String b32) {
            this.ok = ok; this.mensagemErro = erro; this.codigoLog = mid;
            this.usuario = u; this.certificado = c; this.totpBase32 = b32;
        }

        public static ResultadoCadastro erro(int mid, String msg) {
            return new ResultadoCadastro(false, msg, mid, null, null, null);
        }

        public static ResultadoCadastro sucesso(Usuario u, X509Certificate c, String b32) {
            return new ResultadoCadastro(true, null, null, u, c, b32);
        }
    }

    public ResultadoCadastro carregarECertVerificar(Path caminhoCert, Path caminhoKey, String frase) {
        if (caminhoCert == null || !caminhoCert.toFile().exists()) {
            return ResultadoCadastro.erro(6004, "Caminho do certificado invalido.");
        }
        X509Certificate cert;
        try {
            cert = CryptoUtils.carregarCertificado(caminhoCert);
        } catch (Exception e) {
            return ResultadoCadastro.erro(6004, "Certificado nao pode ser carregado: " + e.getMessage());
        }

        if (caminhoKey == null || !caminhoKey.toFile().exists()) {
            return ResultadoCadastro.erro(6005, "Caminho da chave privada invalido.");
        }
        PrivateKey priv;
        try {
            priv = CryptoUtils.carregarChavePrivada(caminhoKey, frase);
        } catch (javax.crypto.BadPaddingException | javax.crypto.IllegalBlockSizeException bad) {
            return ResultadoCadastro.erro(6006, "Frase secreta invalida.");
        } catch (Exception e) {
            String m = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (m.contains("padding") || m.contains("block") || m.contains("pkcs8"))
                return ResultadoCadastro.erro(6006, "Frase secreta invalida.");
            return ResultadoCadastro.erro(6005, "Erro ao carregar chave privada: " + e.getMessage());
        }

        byte[] desafio = new byte[CHALLENGE_BYTES_ADMIN];
        new SecureRandom().nextBytes(desafio);
        byte[] sig = CryptoUtils.assinar(desafio, priv);
        if (!CryptoUtils.verificar(desafio, sig, cert.getPublicKey())) {
            return ResultadoCadastro.erro(6007, "Assinatura digital nao confere.");
        }

        return ResultadoCadastro.sucesso(null, cert, null);
    }

    public ResultadoCadastro efetivarCadastro(X509Certificate cert, byte[] chavePrivadaEnc,
                                              String senha, String role) {
        String nome  = CryptoUtils.extrairCN(cert.getSubjectX500Principal().getName());
        String email = CryptoUtils.extrairEmail(cert);

        if (email == null || email.isEmpty()) {
            return ResultadoCadastro.erro(6004, "Certificado nao contem e-mail.");
        }
        if (db.buscarPorEmail(email) != null) {
            return ResultadoCadastro.erro(6004, "E-mail ja cadastrado: " + email);
        }

        String totpBase32  = CryptoUtils.gerarTotpSecretBase32();
        String totpSecret  = CryptoUtils.cifrarTotpSecret(totpBase32, senha);
        String senhaHash   = CryptoUtils.bcryptHash(senha);

        Usuario u = new Usuario(email, nome, senhaHash, totpSecret, role);
        db.salvarUsuario(u);

        String certPem;
        try {
            certPem = CryptoUtils.certificadoParaPem(cert);
        } catch (Exception e) {
            return ResultadoCadastro.erro(6004, "Erro ao serializar certificado: " + e.getMessage());
        }
        int kid = db.salvarChaveiro(u.getUid(), certPem, chavePrivadaEnc);
        u.setKid(kid);

        return ResultadoCadastro.sucesso(u, cert, totpBase32);
    }
}
