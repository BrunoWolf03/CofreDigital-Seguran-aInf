import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.util.*;

public class TelaLoginSenha {

    private final Usuario usuario;
    private final List<int[]> cliques = new ArrayList<>();
    private int[][] teclado;
    private Label lblIndicador;
    private Label lblErro;
    private Button[] botoes;

    public TelaLoginSenha(Usuario usuario) {
        this.usuario = usuario;
        this.teclado = gerarTeclado();
    }

    public Scene buildScene() {
        VBox root = UI.root();

        VBox cabecalho = UI.cabecalho("COFRE DIGITAL", "Autenticação — Etapa 2 de 3");

        Label boasVindas = new Label("Bem-vindo(a), " + usuario.getNome());
        boasVindas.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + UI.ACCENT + ";");

        Label instrucao = UI.subtitulo(
            "Use o teclado virtual abaixo para digitar sua senha. " +
            "Cada botão representa dois dígitos possíveis.");

        // Indicador de dígitos digitados
        lblIndicador = new Label("─");
        lblIndicador.setStyle("-fx-font-size: 22px; -fx-text-fill: #4a5568; -fx-letter-spacing: 4;");

        // Teclado virtual: 5 botões com 2 dígitos cada
        botoes = new Button[5];
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            botoes[i] = new Button(teclado[i][0] + "   |   " + teclado[i][1]);
            botoes[i].setStyle(estiloTecla());
            botoes[i].setPrefSize(130, 55);
            botoes[i].setOnAction(e -> registrarClique(idx));
        }

        HBox linha1 = new HBox(8, botoes[0], botoes[1], botoes[2]);
        HBox linha2 = new HBox(8, botoes[3], botoes[4]);
        linha1.setAlignment(Pos.CENTER);
        linha2.setAlignment(Pos.CENTER);
        VBox tecladoView = new VBox(8, linha1, linha2);
        tecladoView.setAlignment(Pos.CENTER);
        tecladoView.setPadding(new Insets(8, 0, 8, 0));

        lblErro = UI.erro();

        Button btnLimpar   = UI.botao("Limpar", false);
        Button btnConfirmar = UI.botao("Confirmar", true);
        btnLimpar.setMaxWidth(150);
        btnConfirmar.setMaxWidth(200);

        HBox acoes = new HBox(10, btnLimpar, btnConfirmar);
        acoes.setAlignment(Pos.CENTER);

        btnLimpar.setOnAction(e -> App.navegar(new TelaLoginSenha(usuario).buildScene()));
        btnConfirmar.setOnAction(e -> confirmar());

        VBox conteudo = UI.card(boasVindas, instrucao, lblIndicador, tecladoView, lblErro, acoes);
        root.getChildren().addAll(cabecalho, conteudo);
        return new Scene(root, 450, 560);
    }

    private void registrarClique(int idx) {
        cliques.add(teclado[idx]);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cliques.size(); i++) sb.append("● ");
        lblIndicador.setText(sb.toString().trim());
        lblErro.setText("");

        teclado = gerarTeclado();
        for (int i = 0; i < 5; i++) {
            botoes[i].setText(teclado[i][0] + "   |   " + teclado[i][1]);
        }
    }

    private void confirmar() {
        if (cliques.isEmpty()) {
            lblErro.setText("Digite sua senha usando o teclado virtual.");
            return;
        }

        if (App.auth.autenticarSenha(usuario, cliques)) {
            App.navegar(new TelaLoginTOTP(usuario).buildScene());
        } else if (usuario.isBloqueado()) {
            lblErro.setText("Conta bloqueada após 3 tentativas. Retornando ao login...");
            PauseTransition p = new PauseTransition(Duration.seconds(2));
            p.setOnFinished(e -> App.navegar(new TelaLoginEmail().buildScene()));
            p.play();
        } else {
            int restantes = 3 - usuario.getFalhasLogin();
            lblErro.setText("Senha incorreta. Tentativas restantes: " + restantes);
            PauseTransition p = new PauseTransition(Duration.seconds(1.5));
            p.setOnFinished(e -> App.navegar(new TelaLoginSenha(usuario).buildScene()));
            p.play();
        }
    }

    private int[][] gerarTeclado() {
        List<Integer> digitos = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        Collections.shuffle(digitos);
        int[][] pares = new int[5][2];
        for (int i = 0; i < 5; i++) {
            pares[i][0] = digitos.get(i * 2);
            pares[i][1] = digitos.get(i * 2 + 1);
        }
        return pares;
    }

    private String estiloTecla() {
        return "-fx-background-color: #3a3a5c; -fx-text-fill: #f1f5f9; " +
               "-fx-font-size: 15px; -fx-font-weight: bold; " +
               "-fx-background-radius: 8; -fx-cursor: hand; " +
               "-fx-border-color: #5f5f8f; -fx-border-radius: 8; -fx-border-width: 1;";
    }
}
