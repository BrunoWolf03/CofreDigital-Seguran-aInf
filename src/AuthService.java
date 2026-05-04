import java.util.Scanner;

public class AuthService {

    private Database db;
    private Scanner scanner = new Scanner(System.in);

    public AuthService(Database db) {
        this.db = db;
    }

    public void cadastrarAdministrador() {

        System.out.print("Email: ");
        String email = scanner.nextLine();

        System.out.print("Nome: ");
        String nome = scanner.nextLine();

        System.out.print("Senha: ");
        String senha = scanner.nextLine();

        System.out.print("Frase secreta: ");
        String frase = scanner.nextLine();

        Usuario admin = new Usuario(email, nome, senha, frase);

        db.salvarUsuario(admin);

        System.out.println("Administrador cadastrado com sucesso!");
    }

    public boolean validarAdmin() {

        Usuario admin = db.getAdmin();

        System.out.print("Digite a frase secreta do admin: ");
        String frase = scanner.nextLine();

        return admin.getFraseSecreta().equals(frase);
    }

    public Usuario autenticarEmail() {

    System.out.print("Digite seu email: ");
    String email = scanner.nextLine();

    Usuario user = db.buscarPorEmail(email);

    if (user == null) {
        System.out.println("Usuário não encontrado.");
        return null;
    }

    if (user.isBloqueado()) {
        System.out.println("Usuário bloqueado. Tente novamente mais tarde.");
        return null;
    }

    System.out.println("Usuário válido!");
    return user;
}
}

