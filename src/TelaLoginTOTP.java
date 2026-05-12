import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

public class TelaLoginTOTP {

    private final Usuario usuario;

    public TelaLoginTOTP(Usuario usuario) {
        this.usuario = usuario;
    }

    public Scene buildScene() {
        App.db.registrarLog(4001, usuario.getUid());

        VBox root = UI.root();
        root.setAlignment(Pos.CENTER);

        VBox cabecalho = UI.cabecalho("COFRE DIGITAL", "Autenticacao - Etapa 3 de 3");

        Label boasVindas = new Label("Bem-vindo(a), " + usuario.getNome());
        boasVindas.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + UI.ACCENT + ";");

        Label instrucao = UI.subtitulo(
            "Abra o Google Authenticator e digite o codigo de 6 digitos.");

        TextField tfTotp = UI.campo("Codigo TOTP (6 digitos)");
        tfTotp.setMaxWidth(200);
        tfTotp.setAlignment(Pos.CENTER);

        Label lblErro = UI.erro();
        Button btnConfirmar = UI.botao("Confirmar", true);

        btnConfirmar.setOnAction(e -> {
            String codigo = tfTotp.getText().trim();
            if (!codigo.matches("\\d{6}")) {
                lblErro.setText("O codigo deve ter exatamente 6 digitos.");
                return;
            }

            if (App.auth.verificarTOTP(usuario, codigo)) {
                App.db.registrarLog(4003, usuario.getUid());
                App.db.registrarLog(4002, usuario.getUid());

                usuario.resetarFalhas();
                usuario.incrementarAcessos();
                App.db.atualizarUsuario(usuario);

                App.db.registrarLog(1003, usuario.getUid());

                Sessao.setUsuarioAtual(usuario);
                Sessao.resetarFalhasTotp();
                Sessao.setSenhaPlanaTemp(null);

                App.navegar(new TelaMenu(usuario).buildScene());
                return;
            }

            Sessao.incrementarFalhasTotp();
            int n = Sessao.getFalhasTotpConsecutivas();
            int midErro = n == 1 ? 4004 : (n == 2 ? 4005 : 4006);
            App.db.registrarLog(midErro, usuario.getUid());

            if (n >= 3) {
                App.auth.bloquearPor2Minutos(usuario);
                App.db.registrarLog(4007, usuario.getUid());
                App.db.registrarLog(4002, usuario.getUid());
                Sessao.resetarFalhasTotp();
                Sessao.setSenhaPlanaTemp(null);
                lblErro.setText("Bloqueio de 2 minutos. Retornando ao login...");
                PauseTransition p = new PauseTransition(Duration.seconds(2));
                p.setOnFinished(ev -> App.navegar(new TelaLoginEmail().buildScene()));
                p.play();
            } else {
                lblErro.setText("Codigo invalido. Tentativas restantes: " + (3 - n));
                tfTotp.clear();
            }
        });

        tfTotp.setOnAction(e -> btnConfirmar.fire());

        VBox form = UI.card(boasVindas, instrucao, tfTotp, lblErro, btnConfirmar);
        root.getChildren().addAll(cabecalho, form);
        return new Scene(root, 460, 420);
    }
}
