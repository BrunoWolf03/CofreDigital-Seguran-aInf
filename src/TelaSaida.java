import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class TelaSaida {

    public Scene buildScene() {
        Usuario u = Sessao.getUsuarioAtual();
        App.db.registrarLog(8001, u.getUid());

        VBox root = UI.root();

        VBox cabecalho = new VBox(2,
            info("Login", u.getEmail()),
            info("Grupo", u.getGrupoLabel()),
            info("Nome",  u.getNome()));

        Label totalAcessos = new Label("Total de acessos do usuario: " + u.getTotalAcessos());
        totalAcessos.setStyle("-fx-font-size: 13px; -fx-text-fill: #f1f5f9;");
        VBox corpo1 = UI.card(totalAcessos);

        Label titulo = new Label("Saida do sistema:");
        titulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + UI.ACCENT + ";");

        Label msg = UI.subtitulo(
            "Pressione o botao Encerrar Sessao ou o botao Encerrar Sistema para confirmar.");

        Button btnSessao   = UI.botao("Encerrar Sessao",  true);
        Button btnSistema  = UI.botao("Encerrar Sistema", true);
        Button btnVoltar   = UI.botao("Voltar",           false);

        btnSessao.setOnAction(e -> {
            App.db.registrarLog(8002, u.getUid());
            App.db.registrarLog(1004, u.getUid());
            Sessao.encerrarSessao();
            App.navegar(new TelaLoginEmail().buildScene());
        });

        btnSistema.setOnAction(e -> {
            App.db.registrarLog(8003, u.getUid());
            App.db.registrarLog(1004, u.getUid());
            App.db.registrarLog(1002);
            Sessao.limparTudo();
            App.db.fechar();
            Platform.exit();
        });

        btnVoltar.setOnAction(e -> {
            App.db.registrarLog(8004, u.getUid());
            App.navegar(new TelaMenu(u).buildScene());
        });

        HBox acoes = new HBox(10, btnSessao, btnSistema);
        acoes.setAlignment(Pos.CENTER);

        VBox corpo2 = UI.card(titulo, msg, acoes, btnVoltar);

        root.getChildren().addAll(cabecalho, corpo1, corpo2);
        return new Scene(root, 540, 520);
    }

    private Label info(String chave, String valor) {
        Label l = new Label(chave + ": " + valor);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #f1f5f9;");
        return l;
    }
}
