import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class TelaCadastroAdmin {

    private static final String ESTILO_ERRO  = "-fx-border-color: #ef4444; -fx-border-radius: 6; -fx-border-width: 1.5;";
    private static final String ESTILO_OK    = "-fx-border-color: #3f3f5f; -fx-border-radius: 6; -fx-border-width: 1;";

    public Scene buildScene() {
        VBox root = UI.root();

        VBox cabecalho = UI.cabecalho("COFRE DIGITAL", "Cadastro do Administrador");

        Label aviso = UI.subtitulo("Primeira execução. Configure o administrador do sistema.");
        aviso.setStyle(aviso.getStyle() + " -fx-background-color: #1e3a5f; -fx-padding: 8 12; -fx-background-radius: 6;");

        TextField     tfEmail  = UI.campo("Ex: admin@empresa.com");
        TextField     tfNome   = UI.campo("Nome completo");
        PasswordField pfSenha    = UI.senha("8 a 10 dígitos numéricos");
        PasswordField pfConfirma = UI.senha("Repita a senha");
        PasswordField pfFrase    = UI.senha("Frase usada para iniciar o sistema");

        Label lblErro = UI.erro();
        Button btnCadastrar = UI.botao("Cadastrar Administrador", true);

        // Limpa destaque de erro ao editar o campo
        tfEmail.textProperty().addListener((o, a, n)   -> resetar(tfEmail,   lblErro));
        tfNome.textProperty().addListener((o, a, n)    -> resetar(tfNome,    lblErro));
        pfSenha.textProperty().addListener((o, a, n)   -> resetar(pfSenha,   lblErro));
        pfConfirma.textProperty().addListener((o, a, n)-> resetar(pfConfirma,lblErro));
        pfFrase.textProperty().addListener((o, a, n)   -> resetar(pfFrase,   lblErro));

        btnCadastrar.setOnAction(e -> {
            String email    = tfEmail.getText().trim();
            String nome     = tfNome.getText().trim();
            String senha    = pfSenha.getText();
            String confirma = pfConfirma.getText();
            String frase    = pfFrase.getText().trim();

            // Validação campo a campo — para no primeiro erro encontrado
            if (email.isEmpty()) {
                erro(tfEmail, lblErro, "Email obrigatório.");
                return;
            }
            if (!email.matches("^[\\w.+\\-]+@[\\w\\-]+\\.[a-zA-Z]{2,}$")) {
                erro(tfEmail, lblErro, "Email inválido. Use o formato: usuario@dominio.com");
                return;
            }
            if (nome.isEmpty()) {
                erro(tfNome, lblErro, "Nome obrigatório.");
                return;
            }
            if (senha.isEmpty()) {
                erro(pfSenha, lblErro, "Senha obrigatória.");
                return;
            }
            if (!senha.matches("\\d+")) {
                erro(pfSenha, lblErro, "Senha deve conter apenas dígitos numéricos (0–9).");
                return;
            }
            if (senha.length() < 8 || senha.length() > 10) {
                erro(pfSenha, lblErro,
                    "Senha deve ter entre 8 e 10 dígitos. Atual: " + senha.length() + " dígito(s).");
                return;
            }
            if (!senha.equals(confirma)) {
                erro(pfConfirma, lblErro, "As senhas não coincidem.");
                return;
            }
            if (frase.isEmpty()) {
                erro(pfFrase, lblErro, "Frase secreta obrigatória.");
                return;
            }
            if (frase.split("\\s+").length < 3) {
                erro(pfFrase, lblErro, "Use pelo menos 3 palavras na frase secreta.");
                return;
            }

            App.auth.cadastrarAdministrador(email, nome, senha, frase);
            App.navegar(new TelaLoginEmail().buildScene());
        });

        VBox form = UI.card(
            rotulo("Email"),          tfEmail,
            rotulo("Nome"),           tfNome,
            rotulo("Senha"),          pfSenha,
            rotulo("Confirmar Senha"), pfConfirma,
            rotulo("Frase Secreta"),   pfFrase,
            lblErro,
            btnCadastrar
        );

        root.getChildren().addAll(cabecalho, aviso, form);
        return new Scene(root, 450, 620);
    }

    private void erro(Control campo, Label lblErro, String mensagem) {
        campo.setStyle(campo.getStyle() + ESTILO_ERRO);
        lblErro.setText(mensagem);
        campo.requestFocus();
    }

    private void resetar(Control campo, Label lblErro) {
        String estilo = campo.getStyle().replaceAll("-fx-border-color:[^;]+;", "").replaceAll("-fx-border-width:[^;]+;", "");
        campo.setStyle(estilo + ESTILO_OK);
        lblErro.setText("");
    }

    private Label rotulo(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: " + UI.MUTED + ";");
        return l;
    }
}
