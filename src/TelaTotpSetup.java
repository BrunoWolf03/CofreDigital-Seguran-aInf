import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TelaTotpSetup {

    private final Usuario usuario;
    private final String base32;
    private final String role;

    public TelaTotpSetup(Usuario u, String base32, String role) {
        this.usuario = u;
        this.base32  = base32;
        this.role    = role;
    }

    public Scene buildScene() {
        VBox root = UI.root();
        VBox cabecalho = UI.cabecalho(
            "COFRE DIGITAL", "Cadastro no Google Authenticator");

        Label info = UI.subtitulo(
            "Cadastre o segredo TOTP abaixo no aplicativo Google Authenticator " +
            "para o usuario " + usuario.getEmail() + ".");

        Label lblSeg = new Label("Segredo BASE32:");
        lblSeg.setStyle("-fx-font-size: 11px; -fx-text-fill: " + UI.MUTED + ";");

        TextField tfSeg = new TextField(base32);
        tfSeg.setEditable(false);
        tfSeg.setStyle("-fx-background-color: #1e1e2e; -fx-text-fill: #f1f5f9; " +
                       "-fx-border-color: #3f3f5f; -fx-padding: 8 12; " +
                       "-fx-font-family: monospace; -fx-font-size: 13px;");

        String uri = "otpauth://totp/Cofre%20Digital:" +
                     URLEncoder.encode(usuario.getEmail(), StandardCharsets.UTF_8) +
                     "?secret=" + base32 +
                     "&issuer=Cofre%20Digital";

        Label lblUri = new Label("URI (otpauth):");
        lblUri.setStyle("-fx-font-size: 11px; -fx-text-fill: " + UI.MUTED + ";");
        TextField tfUri = new TextField(uri);
        tfUri.setEditable(false);
        tfUri.setStyle("-fx-background-color: #1e1e2e; -fx-text-fill: #f1f5f9; " +
                       "-fx-border-color: #3f3f5f; -fx-padding: 8 12; -fx-font-size: 11px;");

        Button btnContinuar = UI.botao("Concluir", true);
        btnContinuar.setOnAction(e -> {
            if ("admin".equals(role) && Sessao.getUsuarioAtual() == null) {
                App.navegar(new TelaLoginEmail().buildScene());
            } else if (Sessao.getUsuarioAtual() != null) {
                App.navegar(new TelaMenu(Sessao.getUsuarioAtual()).buildScene());
            } else {
                App.navegar(new TelaLoginEmail().buildScene());
            }
        });

        VBox card = UI.card(info, lblSeg, tfSeg, lblUri, tfUri, btnContinuar);
        root.getChildren().addAll(cabecalho, card);
        return new Scene(root, 580, 480);
    }
}
