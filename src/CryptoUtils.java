import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CryptoUtils {

    private static final String AES_TRANSFORM = "AES/ECB/PKCS5Padding";
    private static final String RSA_TRANSFORM = "RSA/ECB/PKCS1Padding";
    private static final String SIG_ALGORITHM = "SHA1withRSA";

    public static SecretKey deriveAesKey(String segredo) {
        return deriveAesKey(segredo.getBytes());
    }

    public static SecretKey deriveAesKey(byte[] seed) {
        try {
            SecureRandom rnd = SecureRandom.getInstance("SHA1PRNG");
            rnd.setSeed(seed);
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(256, rnd);
            return kg.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao derivar chave AES", e);
        }
    }

    public static byte[] aesEncrypt(byte[] plain, SecretKey k) {
        try {
            Cipher c = Cipher.getInstance(AES_TRANSFORM);
            c.init(Cipher.ENCRYPT_MODE, k);
            return c.doFinal(plain);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao cifrar AES", e);
        }
    }

    public static byte[] aesDecrypt(byte[] cipherText, SecretKey k) throws Exception {
        Cipher c = Cipher.getInstance(AES_TRANSFORM);
        c.init(Cipher.DECRYPT_MODE, k);
        return c.doFinal(cipherText);
    }

    public static byte[] rsaEncrypt(byte[] data, PublicKey pk) {
        try {
            Cipher c = Cipher.getInstance(RSA_TRANSFORM);
            c.init(Cipher.ENCRYPT_MODE, pk);
            return c.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao cifrar RSA", e);
        }
    }

    public static byte[] rsaDecrypt(byte[] data, PrivateKey pk) throws Exception {
        Cipher c = Cipher.getInstance(RSA_TRANSFORM);
        c.init(Cipher.DECRYPT_MODE, pk);
        return c.doFinal(data);
    }

    public static byte[] assinar(byte[] dados, PrivateKey priv) {
        try {
            Signature sig = Signature.getInstance(SIG_ALGORITHM);
            sig.initSign(priv);
            sig.update(dados);
            return sig.sign();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao assinar", e);
        }
    }

    public static boolean verificar(byte[] dados, byte[] assinatura, PublicKey pub) {
        try {
            Signature sig = Signature.getInstance(SIG_ALGORITHM);
            sig.initVerify(pub);
            sig.update(dados);
            return sig.verify(assinatura);
        } catch (Exception e) {
            return false;
        }
    }

    public static X509Certificate carregarCertificado(Path arquivo) throws Exception {
        byte[] bytes = Files.readAllBytes(arquivo);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(bytes));
    }

    public static String certificadoParaPem(X509Certificate cert) throws Exception {
        String b64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(cert.getEncoded());
        return "-----BEGIN CERTIFICATE-----\n" + b64 + "\n-----END CERTIFICATE-----\n";
    }

    private static final Pattern PEM_PRIVATE_KEY = Pattern.compile(
        "-----BEGIN PRIVATE KEY-----(.*?)-----END PRIVATE KEY-----",
        Pattern.DOTALL);

    public static PrivateKey carregarChavePrivada(Path arquivoCifrado, String frase) throws Exception {
        byte[] cifrado = Files.readAllBytes(arquivoCifrado);
        SecretKey k = deriveAesKey(frase);
        byte[] pemBytes = aesDecrypt(cifrado, k);
        String pem = new String(pemBytes);

        Matcher m = PEM_PRIVATE_KEY.matcher(pem);
        if (!m.find()) throw new Exception("Conteudo nao esta no formato PKCS8 PEM esperado");

        // getMimeDecoder (e nao getDecoder) por causa das quebras de linha do PEM
        byte[] pkcs8 = Base64.getMimeDecoder().decode(m.group(1));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pkcs8);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    public static Map<String, String> camposDoCertificado(X509Certificate c) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Versao",            String.valueOf(c.getVersion()));
        m.put("Serie",             c.getSerialNumber().toString());
        m.put("Validade",          c.getNotBefore() + "  ate  " + c.getNotAfter());
        m.put("Tipo de Assinatura", c.getSigAlgName());
        m.put("Emissor",           c.getIssuerX500Principal().getName());
        m.put("Sujeito",           extrairCN(c.getSubjectX500Principal().getName()));
        m.put("Email",             extrairEmail(c));
        return m;
    }

    public static String extrairCN(String dn) {
        Matcher m = Pattern.compile("CN=([^,]+)").matcher(dn);
        return m.find() ? m.group(1).trim() : dn;
    }

    public static String extrairEmail(X509Certificate c) {
        String subject = c.getSubjectX500Principal().getName(
            javax.security.auth.x500.X500Principal.RFC2253);
        Matcher m = Pattern.compile("(?:1\\.2\\.840\\.113549\\.1\\.9\\.1|EMAILADDRESS|EmailAddress|E)=#?([^,]+)").matcher(subject);
        if (m.find()) {
            String v = m.group(1).trim();
            // alguns certs (incluindo os do Pacote-T3) trazem o email em IA5 hex
            if (v.startsWith("16")) return decodeIA5(v);
            return v;
        }
        try {
            if (c.getSubjectAlternativeNames() != null) {
                for (java.util.List<?> san : c.getSubjectAlternativeNames()) {
                    if (san.size() >= 2 && Integer.valueOf(1).equals(san.get(0))) {
                        return san.get(1).toString();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static String decodeIA5(String hexLike) {
        try {
            String hex = hexLike;
            if (hex.length() >= 4 && hex.startsWith("16")) hex = hex.substring(4);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i + 1 < hex.length(); i += 2) {
                sb.append((char) Integer.parseInt(hex.substring(i, i + 2), 16));
            }
            return sb.toString();
        } catch (Exception e) {
            return hexLike;
        }
    }

    public static String bcryptHash(String senha) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return org.bouncycastle.crypto.generators.OpenBSDBCrypt.generate(
            "2y", senha.toCharArray(), salt, 8);
    }

    public static boolean bcryptCheck(String senha, String hash) {
        try {
            return org.bouncycastle.crypto.generators.OpenBSDBCrypt.checkPassword(
                hash, senha.toCharArray());
        } catch (Exception e) {
            return false;
        }
    }

    public static String cifrarTotpSecret(String base32, String senhaPlana) {
        SecretKey k = deriveAesKey(senhaPlana);
        byte[] enc = aesEncrypt(base32.getBytes(), k);
        return Base64.getEncoder().encodeToString(enc);
    }

    public static String decifrarTotpSecret(String armazenado, String senhaPlana) throws Exception {
        SecretKey k = deriveAesKey(senhaPlana);
        byte[] cifr = Base64.getDecoder().decode(armazenado);
        return new String(aesDecrypt(cifr, k));
    }

    public static String gerarTotpSecretBase32() {
        byte[] secret = new byte[20];
        new SecureRandom().nextBytes(secret);
        return new Base32(Base32.Alphabet.BASE32, false, false).toString(secret);
    }
}
