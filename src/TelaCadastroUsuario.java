import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.nio.file.Path;

public class TelaCadastroUsuario {

    public Scene buildScene() {
        return buildSceneComDados("", "", "", "Usuario", "", "");
    }

    public Scene buildSceneComDados(String cert, String chave, String frase,
                                    String grupo, String senha, String confirma) {
        Usuario admin = Sessao.getUsuarioAtual();
        App.db.registrarLog(6001, admin.getUid());

        VBox root = UI.root();

        VBox cabecalho = cabecalhoAdmin(admin);
        VBox corpo1 = corpo1TotalUsuarios();

        Label titulo = new Label("Formulario de Cadastro:");
        titulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + UI.ACCENT + ";");

        TextField     tfCert     = UI.campo("Caminho do certificado (.crt)");
        TextField     tfChave    = UI.campo("Caminho da chave privada (.key)");
        PasswordField pfFrase    = UI.senha("Frase secreta da chave privada");
        ComboBox<String> cbGrupo = new ComboBox<>();
        cbGrupo.getItems().addAll("Administrador", "Usuario");
        cbGrupo.setValue(grupo);
        cbGrupo.setStyle("-fx-background-color: #1e1e2e; -fx-text-fill: #f1f5f9; -fx-padding: 4 8;");
        PasswordField pfSenha    = UI.senha("Senha pessoal (8-10 digitos)");
        PasswordField pfConfirma = UI.senha("Confirmar senha pessoal");

        tfCert.setText(cert);
        tfChave.setText(chave);
        pfFrase.setText(frase);
        pfSenha.setText(senha);
        pfConfirma.setText(confirma);

        Button btnEscolherCert  = UI.botao("...", false);
        Button btnEscolherChave = UI.botao("...", false);
        btnEscolherCert.setMaxWidth(50);
        btnEscolherChave.setMaxWidth(50);
        btnEscolherCert.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            java.io.File f = fc.showOpenDialog(App.primaryStage);
            if (f != null) tfCert.setText(f.getAbsolutePath());
        });
        btnEscolherChave.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            java.io.File f = fc.showOpenDialog(App.primaryStage);
            if (f != null) tfChave.setText(f.getAbsolutePath());
        });

        HBox boxCert  = new HBox(6, tfCert,  btnEscolherCert);
        HBox boxChave = new HBox(6, tfChave, btnEscolherChave);

        Label lblErro = UI.erro();
        Button btnCadastrar = UI.botao("Cadastrar", true);
        Button btnVoltar    = UI.botao("Voltar", false);

        HBox acoes = new HBox(10, btnCadastrar, btnVoltar);
        acoes.setAlignment(Pos.CENTER);

        btnVoltar.setOnAction(e -> {
            App.db.registrarLog(6010, admin.getUid());
            App.navegar(new TelaMenu(admin).buildScene());
        });

        btnCadastrar.setOnAction(e -> {
            String pathCert  = tfCert.getText().trim();
            String pathChave = tfChave.getText().trim();
            String frasep    = pfFrase.getText();
            String grupoSel  = cbGrupo.getValue();
            String senhap    = pfSenha.getText();
            String confirmap = pfConfirma.getText();

            App.db.registrarLog(6002, admin.getUid());

            if (pathCert.isEmpty() || pathChave.isEmpty() || frasep.isEmpty()) {
                lblErro.setText("Preencha os caminhos e a frase secreta.");
                return;
            }
            String erroSenha = Validacoes.validarSenhaPessoal(senhap);
            if (erroSenha != null) {
                App.db.registrarLog(6003, admin.getUid());
                lblErro.setText(erroSenha);
                return;
            }
            if (!senhap.equals(confirmap)) {
                lblErro.setText("As senhas nao coincidem.");
                return;
            }

            AuthService.ResultadoCadastro r = App.auth.carregarECertVerificar(
                Path.of(pathCert), Path.of(pathChave), frasep);
            if (!r.ok) {
                if (r.codigoLog != null) App.db.registrarLog(r.codigoLog, admin.getUid());
                lblErro.setText(r.mensagemErro);
                return;
            }

            String role = "Administrador".equals(grupoSel) ? "admin" : "user";
            App.navegar(new TelaConfirmacaoCadastro(
                r.certificado, Path.of(pathChave), frasep, senhap, role,
                pathCert, pathChave
            ).buildScene());
        });

        VBox form = UI.card(
            titulo,
            rotulo("Certificado digital"), boxCert,
            rotulo("Chave privada"),       boxChave,
            rotulo("Frase secreta"),       pfFrase,
            rotulo("Grupo"),               cbGrupo,
            rotulo("Senha pessoal"),       pfSenha,
            rotulo("Confirmacao da senha"), pfConfirma,
            lblErro, acoes
        );

        root.getChildren().addAll(cabecalho, corpo1, form);
        return new Scene(root, 540, 760);
    }

    private VBox cabecalhoAdmin(Usuario admin) {
        Label l1 = info("Login", admin.getEmail());
        Label l2 = info("Grupo", admin.getGrupoLabel());
        Label l3 = info("Nome",  admin.getNome());
        VBox v = new VBox(2, l1, l2, l3);
        return v;
    }

    private VBox corpo1TotalUsuarios() {
        Label l = new Label("Total de usuarios do sistema: " + App.db.totalUsuarios());
        l.setStyle("-fx-font-size: 13px; -fx-text-fill: #f1f5f9;");
        return UI.card(l);
    }

    private Label info(String chave, String valor) {
        Label l = new Label(chave + ": " + valor);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #f1f5f9;");
        return l;
    }

    private Label rotulo(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: " + UI.MUTED + ";");
        return l;
    }
}
