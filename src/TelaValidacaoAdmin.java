import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

public class TelaValidacaoAdmin {

    public Scene buildScene() {
        VBox root = UI.root();
        root.setAlignment(Pos.CENTER);

        VBox cabecalho = UI.cabecalho("COFRE DIGITAL", "Partida do sistema");

        Label instrucao = UI.subtitulo(
            "Digite a frase secreta da chave privada do administrador.");

        PasswordField pfFrase = UI.senha("Frase secreta");
        Label lblErro = UI.erro();
        Button btnConfirmar = UI.botao("Validar", true);

        btnConfirmar.setOnAction(e -> {
            String frase = pfFrase.getText();
            if (frase.isEmpty()) {
                lblErro.setText("Digite a frase secreta.");
                return;
            }
            if (App.auth.validarChavePrivadaAdmin(frase)) {
                App.db.registrarLog(1006);
                App.navegar(new TelaLoginEmail().buildScene());
            } else {
                lblErro.setText("Validacao da chave privada negativa. Encerrando...");
                btnConfirmar.setDisable(true);
                PauseTransition p = new PauseTransition(Duration.seconds(2));
                p.setOnFinished(ev -> {
                    App.db.registrarLog(1002);
                    Sessao.limparTudo();
                    App.db.fechar();
                    App.primaryStage.close();
                });
                p.play();
            }
        });

        VBox form = UI.card(instrucao, pfFrase, lblErro, btnConfirmar);
        root.getChildren().addAll(cabecalho, form);
        return new Scene(root, 460, 380);
    }
}
