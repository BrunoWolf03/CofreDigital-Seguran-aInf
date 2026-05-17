import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

class UI {
    static final String BG    = "#1e1e2e";
    static final String CARD  = "#282840";
    static final String ACCENT = "#7c3aed";
    static final String MUTED  = "#94a3b8";
    static final String DANGER = "#ef4444";

    static Label titulo(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT + ";");
        return l;
    }

    static Label subtitulo(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size: 13px; -fx-text-fill: " + MUTED + "; -fx-wrap-text: true;");
        l.setMaxWidth(360);
        return l;
    }

    static Label erro() {
        Label l = new Label("");
        l.setStyle("-fx-text-fill: " + DANGER + "; -fx-font-size: 12px; -fx-wrap-text: true;");
        l.setMaxWidth(360);
        return l;
    }

    static TextField campo(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle(estiloInput());
        return tf;
    }

    static PasswordField senha(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setStyle(estiloInput());
        return pf;
    }

    static Button botao(String texto, boolean primario) {
        Button b = new Button(texto);
        b.setStyle(primario ? estiloPrimario() : estiloSecundario());
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    static VBox card(javafx.scene.Node... nodes) {
        VBox v = new VBox(10);
        v.setPadding(new Insets(20));
        v.setStyle("-fx-background-color: " + CARD + "; -fx-background-radius: 12;");
        v.getChildren().addAll(nodes);
        return v;
    }

    static VBox root() {
        VBox v = new VBox(20);
        v.setPadding(new Insets(30, 40, 30, 40));
        v.setAlignment(Pos.TOP_CENTER);
        v.setStyle("-fx-background-color: " + BG + ";");
        return v;
    }

    static VBox cabecalho(String tituloTxt, String subTxt) {
        VBox h = new VBox(4, titulo(tituloTxt), subtitulo(subTxt));
        h.setAlignment(Pos.CENTER);
        return h;
    }

    static void alertaIntegridade(String tituloTexto, String mensagem) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("ALERTA DE SEGURANCA");
        a.setHeaderText(tituloTexto);
        a.setContentText(mensagem);
        a.getDialogPane().setStyle(
            "-fx-background-color: #2a0a0a; " +
            "-fx-border-color: " + DANGER + "; -fx-border-width: 2; " +
            "-fx-text-fill: #f1f5f9;");
        a.getDialogPane().lookupAll(".label").forEach(n ->
            n.setStyle("-fx-text-fill: #f1f5f9; -fx-font-weight: bold;"));
        a.showAndWait();
    }

    private static String estiloInput() {
        return "-fx-background-color: #1e1e2e; -fx-text-fill: #f1f5f9; " +
               "-fx-prompt-text-fill: #475569; -fx-border-color: #3f3f5f; " +
               "-fx-border-radius: 6; -fx-background-radius: 6; " +
               "-fx-padding: 8 12; -fx-font-size: 13px;";
    }

    private static String estiloPrimario() {
        return "-fx-background-color: #7c3aed; -fx-text-fill: white; " +
               "-fx-font-size: 13px; -fx-font-weight: bold; " +
               "-fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;";
    }

    private static String estiloSecundario() {
        return "-fx-background-color: #374151; -fx-text-fill: #f1f5f9; " +
               "-fx-font-size: 13px; " +
               "-fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;";
    }
}
