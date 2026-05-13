import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class Sessao {

    private static Usuario usuarioAtual;
    private static String fraseSecretaAdmin;
    private static PrivateKey chavePrivadaAdmin;
    private static X509Certificate certificadoAdmin;
    private static String senhaPlanaTemp;
    private static int falhasSenhaConsecutivas;
    private static int falhasTotpConsecutivas;

    public static Usuario getUsuarioAtual()                { return usuarioAtual; }
    public static void    setUsuarioAtual(Usuario u)       { usuarioAtual = u; }

    public static String  getFraseSecretaAdmin()           { return fraseSecretaAdmin; }
    public static void    setFraseSecretaAdmin(String f)   { fraseSecretaAdmin = f; }

    public static PrivateKey getChavePrivadaAdmin()        { return chavePrivadaAdmin; }
    public static void       setChavePrivadaAdmin(PrivateKey k) { chavePrivadaAdmin = k; }

    public static X509Certificate getCertificadoAdmin()    { return certificadoAdmin; }
    public static void setCertificadoAdmin(X509Certificate c) { certificadoAdmin = c; }

    public static String  getSenhaPlanaTemp()              { return senhaPlanaTemp; }
    public static void    setSenhaPlanaTemp(String s)      { senhaPlanaTemp = s; }

    public static int  getFalhasSenhaConsecutivas()        { return falhasSenhaConsecutivas; }
    public static void incrementarFalhasSenha()            { falhasSenhaConsecutivas++; }
    public static void resetarFalhasSenha()                { falhasSenhaConsecutivas = 0; }

    public static int  getFalhasTotpConsecutivas()         { return falhasTotpConsecutivas; }
    public static void incrementarFalhasTotp()             { falhasTotpConsecutivas++; }
    public static void resetarFalhasTotp()                 { falhasTotpConsecutivas = 0; }

    public static void encerrarSessao() {
        usuarioAtual = null;
        senhaPlanaTemp = null;
        falhasSenhaConsecutivas = 0;
        falhasTotpConsecutivas = 0;
    }

    public static void limparTudo() {
        encerrarSessao();
        fraseSecretaAdmin = null;
        chavePrivadaAdmin = null;
        certificadoAdmin = null;
    }
}
