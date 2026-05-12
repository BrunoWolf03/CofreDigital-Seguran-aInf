import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class TelaMenu {

    private final Usuario usuario;

    public TelaMenu(Usuario usuario) {
        this.usuario = usuario;
    }

    public Scene buildScene() {
        App.db.registrarLog(5001, usuario.getUid());

        VBox root = UI.root();
        root.getChildren().add(cabecalho());
        root.getChildren().add(corpo1());
        root.getChildren().add(corpo2());
        return new Scene(root, 500, usuario.isAdmin() ? 580 : 480);
    }

    private VBox cabecalho() {
        Label login  = linhaInfo("Login", usuario.getEmail());
        Label grupo  = linhaInfo("Grupo", usuario.getGrupoLabel());
        Label nome   = linhaInfo("Nome",  usuario.getNome());
        VBox v = new VBox(2, login, grupo, nome);
        v.setAlignment(Pos.CENTER_LEFT);
        v.setStyle("-fx-padding: 4 0 8 0;");
        return v;
    }

    private VBox corpo1() {
        Label total = new Label("Total de acessos do usuario: " + usuario.getTotalAcessos());
        total.setStyle("-fx-font-size: 13px; -fx-text-fill: #f1f5f9;");
        return UI.card(total);
    }

    private VBox corpo2() {
        Label titulo = new Label("Menu Principal:");
        titulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + UI.ACCENT + ";");

        VBox card = UI.card(titulo);

        if (usuario.isAdmin()) {
            Button b1 = UI.botao("1 - Cadastrar um novo usuario", true);
            b1.setOnAction(e -> {
                App.db.registrarLog(5002, usuario.getUid());
                App.navegar(new TelaCadastroUsuario().buildScene());
            });
            card.getChildren().add(b1);
        }

        Button b2 = UI.botao("2 - Consultar pasta de arquivos secretos do usuario", true);
        b2.setOnAction(e -> {
            App.db.registrarLog(5003, usuario.getUid());
            App.navegar(new TelaConsultaPasta().buildScene());
        });
        card.getChildren().add(b2);

        Button b3 = UI.botao("3 - Sair do sistema", false);
        b3.setOnAction(e -> {
            App.db.registrarLog(5004, usuario.getUid());
            App.navegar(new TelaSaida().buildScene());
        });
        card.getChildren().add(b3);

        return card;
    }

    private Label linhaInfo(String chave, String valor) {
        Label l = new Label(chave + ": " + valor);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #f1f5f9;");
        return l;
    }
}
