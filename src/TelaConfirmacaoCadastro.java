import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.Map;

public class TelaConfirmacaoCadastro {

    private final X509Certificate cert;
    private final Path caminhoChavePrivada;
    private final String frase;
    private final String senha;
    private final String role;
    private final String caminhoCertOriginal;
    private final String caminhoChaveOriginal;

    public TelaConfirmacaoCadastro(X509Certificate cert, Path chaveBin, String frase,
                                   String senha, String role,
                                   String caminhoCertOriginal, String caminhoChaveOriginal) {
        this.cert = cert;
        this.caminhoChavePrivada = chaveBin;
        this.frase = frase;
        this.senha = senha;
        this.role = role;
        this.caminhoCertOriginal = caminhoCertOriginal;
        this.caminhoChaveOriginal = caminhoChaveOriginal;
    }

    public Scene buildScene() {
        VBox root = UI.root();
        VBox cabecalho = UI.cabecalho("COFRE DIGITAL", "Confirmacao dos dados do certificado");

        Map<String, String> campos = CryptoUtils.camposDoCertificado(cert);

        VBox lista = new VBox(6);
        for (Map.Entry<String, String> e : campos.entrySet()) {
            Label nome = new Label(e.getKey() + ":");
            nome.setStyle("-fx-font-size: 11px; -fx-text-fill: " + UI.MUTED + ";");
            Label val = new Label(e.getValue());
            val.setStyle("-fx-font-size: 12px; -fx-text-fill: #f1f5f9; -fx-wrap-text: true;");
            val.setMaxWidth(440);
            lista.getChildren().addAll(nome, val);
        }

        Label lblErro = UI.erro();
        Button btnConfirmar = UI.botao("Confirmar e cadastrar", true);
        Button btnCancelar  = UI.botao("Cancelar", false);

        HBox acoes = new HBox(10, btnConfirmar, btnCancelar);
        acoes.setAlignment(Pos.CENTER);

        btnConfirmar.setOnAction(e -> {
            try {
                byte[] keyEnc = Files.readAllBytes(caminhoChavePrivada);
                AuthService.ResultadoCadastro r = App.auth.efetivarCadastro(cert, keyEnc, senha, role);
                if (!r.ok) {
                    if (r.codigoLog != null) App.db.registrarLog(r.codigoLog);
                    lblErro.setText(r.mensagemErro);
                    return;
                }
                App.db.registrarLog(6008, r.usuario.getUid());

                if ("admin".equals(role)) {
                    Sessao.setFraseSecretaAdmin(frase);
                    Sessao.setChavePrivadaAdmin(
                        CryptoUtils.carregarChavePrivada(caminhoChavePrivada, frase));
                    Sessao.setCertificadoAdmin(cert);
                }

                App.navegar(new TelaTotpSetup(r.usuario, r.totpBase32, role).buildScene());
            } catch (Exception ex) {
                lblErro.setText("Erro ao efetivar cadastro: " + ex.getMessage());
            }
        });

        btnCancelar.setOnAction(e -> {
            App.db.registrarLog(6009);
            if ("admin".equals(role)) {
                App.navegar(new TelaCadastroAdmin().buildSceneComDados(
                    caminhoCertOriginal, caminhoChaveOriginal, frase, senha, senha));
            } else {
                App.navegar(new TelaCadastroUsuario().buildScene());
            }
        });

        VBox card = UI.card(lista, lblErro, acoes);
        root.getChildren().addAll(cabecalho, card);
        return new Scene(root, 540, 620);
    }
}
