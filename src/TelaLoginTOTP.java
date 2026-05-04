import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class TelaLoginTOTP {

    private final Usuario usuario;

    public TelaLoginTOTP(Usuario usuario) {
        this.usuario = usuario;
    }

    public Scene buildScene() {
        VBox root = UI.root();
        root.setAlignment(Pos.CENTER);

        VBox cabecalho = UI.cabecalho("COFRE DIGITAL", "Autenticação — Etapa 3 de 3");

        Label boasVindas = new Label("Bem-vindo(a), " + usuario.getNome());
        boasVindas.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + UI.ACCENT + ";");

        Label instrucao = UI.subtitulo(
            "Abra o Google Authenticator e digite o código de 6 dígitos gerado para este sistema.");

        Label aviso = new Label("⚠  TOTP em desenvolvimento — qualquer código de 6 dígitos é aceito");
        aviso.setStyle("-fx-font-size: 11px; -fx-text-fill: #f59e0b; -fx-wrap-text: true;");
        aviso.setMaxWidth(360);

        TextField tfTotp = UI.campo("Código TOTP (6 dígitos)");
        tfTotp.setMaxWidth(200);
        tfTotp.setAlignment(Pos.CENTER);

        Label lblErro = UI.erro();
        Button btnConfirmar = UI.botao("Confirmar", true);

        btnConfirmar.setOnAction(e -> {
            String codigo = tfTotp.getText().trim();
            if (!codigo.matches("\\d{6}")) {
                lblErro.setText("O código deve ter exatamente 6 dígitos numéricos.");
                return;
            }
            if (App.auth.autenticarTOTP(usuario, codigo)) {
                App.navegar(new TelaMenu(usuario).buildScene());
            } else {
                lblErro.setText("Código TOTP inválido. Tente novamente.");
                tfTotp.clear();
            }
        });

        tfTotp.setOnAction(e -> btnConfirmar.fire());

        VBox form = UI.card(boasVindas, instrucao, aviso, tfTotp, lblErro, btnConfirmar);
        root.getChildren().addAll(cabecalho, form);
        return new Scene(root, 450, 440);
    }
}
