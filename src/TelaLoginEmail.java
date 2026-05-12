import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class TelaLoginEmail {

    public Scene buildScene() {
        App.db.registrarLog(2001);

        VBox root = UI.root();
        root.setAlignment(Pos.CENTER);

        VBox cabecalho = UI.cabecalho("COFRE DIGITAL", "Autenticacao - Etapa 1 de 3");

        Label instrucao = UI.subtitulo("Digite seu e-mail para continuar.");

        TextField tfEmail = UI.campo("E-mail");
        Label lblErro = UI.erro();
        Button btnContinuar = UI.botao("Continuar", true);

        btnContinuar.setOnAction(e -> {
            String email = tfEmail.getText().trim();
            if (email.isEmpty()) {
                lblErro.setText("Digite seu e-mail.");
                return;
            }
            if (!Validacoes.emailValido(email)) {
                lblErro.setText("E-mail invalido.");
                return;
            }

            Usuario usuario = App.auth.identificar(email);

            if (usuario == null) {
                App.db.registrarLog(2005);
                lblErro.setText("E-mail nao cadastrado no sistema.");
                return;
            }
            if (usuario.isBloqueado()) {
                App.db.registrarLog(2004, usuario.getUid());
                lblErro.setText("Conta bloqueada. Aguarde 2 minutos.");
                return;
            }

            App.db.registrarLog(2003, usuario.getUid());
            App.db.registrarLog(2002);
            App.navegar(new TelaLoginSenha(usuario).buildScene());
        });

        tfEmail.setOnAction(e -> btnContinuar.fire());

        VBox form = UI.card(instrucao, tfEmail, lblErro, btnContinuar);
        root.getChildren().addAll(cabecalho, form);
        return new Scene(root, 460, 380);
    }
}
