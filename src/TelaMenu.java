import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class TelaMenu {

    private final Usuario usuario;

    public TelaMenu(Usuario usuario) {
        this.usuario = usuario;
    }

    public Scene buildScene() {
        VBox root = UI.root();

        String papel = usuario.isAdmin() ? "Administrador" : "Usuário";
        VBox cabecalho = UI.cabecalho(
            "COFRE DIGITAL",
            "Autenticado como: " + usuario.getNome() + " (" + papel + ")"
        );

        VBox menu = usuario.isAdmin() ? menuAdmin() : menuUsuario();
        root.getChildren().addAll(cabecalho, menu);
        return new Scene(root, 450, usuario.isAdmin() ? 460 : 360);
    }

    private VBox menuAdmin() {
        Label titulo = UI.subtitulo("Menu do Administrador");

        Button btnCadastrar = UI.botao("Cadastrar Novo Usuário", true);
        Button btnLogs      = UI.botao("Visualizar Registros de Acesso", false);
        Button btnArquivo   = UI.botao("Acessar Arquivo Secreto", false);
        Button btnSair      = botaoSair();

        btnCadastrar.setOnAction(e -> emBreve("Cadastro de usuário"));
        btnLogs.setOnAction(e -> emBreve("Visualização de registros"));
        btnArquivo.setOnAction(e -> emBreve("Acesso ao arquivo secreto"));

        return UI.card(titulo, btnCadastrar, btnLogs, btnArquivo, separador(), btnSair);
    }

    private VBox menuUsuario() {
        Label titulo = UI.subtitulo("Menu Principal");

        Button btnArquivo = UI.botao("Acessar Arquivo Secreto", true);
        Button btnSair    = botaoSair();

        btnArquivo.setOnAction(e -> emBreve("Acesso ao arquivo secreto"));

        return UI.card(titulo, btnArquivo, separador(), btnSair);
    }

    private Button botaoSair() {
        Button btn = UI.botao("Sair / Logout", false);
        btn.setStyle("-fx-background-color: #7f1d1d; -fx-text-fill: #fca5a5; " +
                     "-fx-font-size: 13px; -fx-background-radius: 8; " +
                     "-fx-padding: 10 20; -fx-cursor: hand;");
        btn.setOnAction(e -> App.navegar(new TelaLoginEmail().buildScene()));
        return btn;
    }

    private Label separador() {
        Label l = new Label("");
        l.setMinHeight(4);
        return l;
    }

    private void emBreve(String funcao) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Em desenvolvimento");
        alert.setHeaderText(funcao);
        alert.setContentText("Esta funcionalidade será implementada nas próximas etapas do trabalho.");
        alert.getDialogPane().setStyle("-fx-background-color: " + UI.CARD + ";");
        alert.showAndWait();
    }
}
