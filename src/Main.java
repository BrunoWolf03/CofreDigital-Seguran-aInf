import java.util.*;

public class Main {

    public static void main(String[] args) {

        Database db = new Database();
        AuthService auth = new AuthService(db);

        System.out.println("=== COFRE DIGITAL ===");

        // Primeira execução → cadastra admin
        if (!db.existeUsuario()) {
            System.out.println("Primeira execução do sistema.");
            auth.cadastrarAdministrador();
        } else {
            // Validação da frase secreta do admin
            System.out.println("Validação do administrador necessária.");

            boolean valido = auth.validarAdmin();

            if (!valido) {
                System.out.println("Frase secreta inválida. Encerrando sistema...");
                return;
            }
        }

        // Etapa 1 - Login (email)
        Usuario usuario = auth.loginEtapa1();

        if (usuario == null) {
            System.out.println("Falha na autenticação (etapa 1). Encerrando...");
            return;
        }

        System.out.println("Login etapa 1 concluída com sucesso!");

        // Próximas etapas (ainda não implementadas)
        System.out.println("Indo para etapa 2 (senha)...");
        // auth.loginEtapa2(usuario);

        System.out.println("Indo para etapa 3 (TOTP)...");
        // auth.loginEtapa3(usuario);

        System.out.println("Sistema pronto (parcialmente implementado).");
    }
}