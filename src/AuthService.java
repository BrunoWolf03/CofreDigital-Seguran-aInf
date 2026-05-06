import java.util.List;

public class AuthService {

    private Database db;

    public AuthService(Database db) {
        this.db = db;
    }

    public void cadastrarAdministrador(String email, String nome, String senha, String frase) {
        // totpSecret vazio por enquanto — será preenchido pela parceira no cadastro real
        Usuario admin = new Usuario(email, nome, senha, "", frase, "admin");
        db.salvarUsuario(admin);
    }

    public boolean validarAdmin(String frase) {
        Usuario admin = db.getAdmin();
        if (admin == null) return false;
        return admin.getFraseSecreta().equals(frase);
    }

    // Retorna o usuário se encontrado (mesmo bloqueado). Null se não existir.
    public Usuario autenticarEmail(String email) {
        return db.buscarPorEmail(email);
    }

    // Valida a senha via teclado virtual: cada clique é um par de dígitos possíveis.
    public boolean autenticarSenha(Usuario usuario, List<int[]> cliques) {
        String senha = usuario.getSenha();
        if (senha.length() != cliques.size()) {
            registrarFalha(usuario);
            return false;
        }
        for (int i = 0; i < senha.length(); i++) {
            int digito = Character.getNumericValue(senha.charAt(i));
            int[] par = cliques.get(i);
            if (digito != par[0] && digito != par[1]) {
                registrarFalha(usuario);
                return false;
            }
        }
        usuario.resetarFalhas();
        return true;
    }

    // Stub: aceita qualquer código de 6 dígitos até o TOTP ser implementado.
    public boolean autenticarTOTP(Usuario usuario, String totpCode) {
        return totpCode.matches("\\d{6}");
    }

    private void registrarFalha(Usuario usuario) {
        usuario.incrementarFalhas();
        if (usuario.getFalhasLogin() >= 3) {
            usuario.setBloqueado(true);
        }
    }
}
