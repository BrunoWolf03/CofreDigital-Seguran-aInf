import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    static Stage primaryStage;
    static Database db;
    static AuthService auth;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        db   = new Database();
        auth = new AuthService(db);

        stage.setTitle("Cofre Digital");
        stage.setResizable(false);

        db.registrarLog(1001);

        stage.setOnCloseRequest(ev -> {
            db.registrarLog(1002);
            Sessao.limparTudo();
            db.fechar();
        });

        iniciar();
        stage.show();
    }

    static void iniciar() {
        if (!db.existeUsuario()) {
            db.registrarLog(1005);
            navegar(new TelaCadastroAdmin().buildScene());
        } else {
            navegar(new TelaValidacaoAdmin().buildScene());
        }
    }

    static void navegar(Scene cena) {
        primaryStage.setScene(cena);
        primaryStage.sizeToScene();
    }
}
