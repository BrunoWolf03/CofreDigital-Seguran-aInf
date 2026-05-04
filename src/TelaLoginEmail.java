import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class TelaLoginEmail {

    public Scene buildScene() {
        VBox root = UI.root();
        root.setAlignment(Pos.CENTER);

        VBox cabecalho = UI.cabecalho("COFRE DIGITAL", "Autenticação — Etapa 1 de 3");

        Label instrucao = UI.subtitulo("Digite seu endereço de email para continuar.");

        TextField tfEmail = UI.campo("Email");
        Label lblErro = UI.erro();
        Button btnContinuar = UI.botao("Continuar", true);

        btnContinuar.setOnAction(e -> {
            String email = tfEmail.getText().trim();
            if (email.isEmpty()) {
                lblErro.setText("Digite seu email.");
                return;
            }

            Usuario usuario = App.auth.autenticarEmail(email);

            if (usuario == null) {
                lblErro.setText("Email não cadastrado no sistema.");
                return;
            }
            if (usuario.isBloqueado()) {
                lblErro.setText("Conta bloqueada após 3 tentativas inválidas. Aguarde 2 minutos.");
                return;
            }

            App.navegar(new TelaLoginSenha(usuario).buildScene());
        });

        // Permitir Enter no campo de email
        tfEmail.setOnAction(e -> btnContinuar.fire());

        VBox form = UI.card(instrucao, tfEmail, lblErro, btnContinuar);
        root.getChildren().addAll(cabecalho, form);
        return new Scene(root, 450, 360);
    }
}
