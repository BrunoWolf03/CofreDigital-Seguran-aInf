import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import javax.crypto.SecretKey;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class TelaConsultaPasta {

    public static class LinhaIndice {
        private final SimpleStringProperty nomeCodigo  = new SimpleStringProperty();
        private final SimpleStringProperty nomeSecreto = new SimpleStringProperty();
        private final SimpleStringProperty dono        = new SimpleStringProperty();
        private final SimpleStringProperty grupo       = new SimpleStringProperty();

        public LinhaIndice(String nc, String ns, String d, String g) {
            nomeCodigo.set(nc);  nomeSecreto.set(ns);
            dono.set(d);          grupo.set(g);
        }
        public StringProperty nomeCodigoProperty()  { return nomeCodigo; }
        public StringProperty nomeSecretoProperty() { return nomeSecreto; }
        public StringProperty donoProperty()        { return dono; }
        public StringProperty grupoProperty()       { return grupo; }
        public String getNomeCodigo()  { return nomeCodigo.get(); }
        public String getNomeSecreto() { return nomeSecreto.get(); }
        public String getDono()        { return dono.get(); }
        public String getGrupo()       { return grupo.get(); }
    }

    public Scene buildScene() {
        Usuario u = Sessao.getUsuarioAtual();
        App.db.registrarLog(7001, u.getUid());

        VBox root = UI.root();

        VBox cabecalho = new VBox(2,
            info("Login", u.getEmail()),
            info("Grupo", u.getGrupoLabel()),
            info("Nome",  u.getNome()));

        Label totalC = new Label("Total de consultas do usuario: " + u.getTotalConsultas());
        totalC.setStyle("-fx-font-size: 13px; -fx-text-fill: #f1f5f9;");
        VBox corpo1 = UI.card(totalC);

        TextField tfPasta = UI.campo("Caminho da pasta");
        PasswordField pfFrase = UI.senha("Frase secreta do usuario");

        Button btnEscolher = UI.botao("...", false);
        btnEscolher.setMaxWidth(50);
        btnEscolher.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(App.primaryStage);
            if (f != null) tfPasta.setText(f.getAbsolutePath());
        });
        HBox boxPasta = new HBox(6, tfPasta, btnEscolher);

        TableView<LinhaIndice> tabela = new TableView<>();
        TableColumn<LinhaIndice, String> cCod   = col("Nome codigo",  "nomeCodigo");
        TableColumn<LinhaIndice, String> cSec   = col("Nome",         "nomeSecreto");
        TableColumn<LinhaIndice, String> cDono  = col("Dono",         "dono");
        TableColumn<LinhaIndice, String> cGrupo = col("Grupo",        "grupo");
        tabela.getColumns().addAll(cCod, cSec, cDono, cGrupo);
        tabela.setPrefHeight(220);
        tabela.setStyle("-fx-control-inner-background: #1e1e2e; -fx-text-fill: #f1f5f9;");

        Label lblErro = UI.erro();
        Button btnListar = UI.botao("Listar", true);
        Button btnAbrir  = UI.botao("Decifrar selecionado", true);
        Button btnVoltar = UI.botao("Voltar", false);

        HBox acoes = new HBox(10, btnListar, btnAbrir, btnVoltar);
        acoes.setAlignment(Pos.CENTER);

        btnVoltar.setOnAction(e -> {
            App.db.registrarLog(7002, u.getUid());
            App.navegar(new TelaMenu(u).buildScene());
        });

        btnListar.setOnAction(e -> {
            App.db.registrarLog(7003, u.getUid());
            String caminho = tfPasta.getText().trim();
            String frase   = pfFrase.getText();
            tabela.getItems().clear();

            if (caminho.isEmpty() || !new File(caminho).isDirectory()) {
                App.db.registrarLog(7004, u.getUid());
                lblErro.setText("Caminho da pasta invalido.");
                return;
            }
            if (frase.isEmpty()) {
                lblErro.setText("Forneca a frase secreta.");
                return;
            }

            try {
                Database.Chaveiro ch = App.db.buscarChaveiroPorUid(u.getUid());
                Path tmp = Files.createTempFile("uk", ".bin");
                Files.write(tmp, ch.chavePrivadaEnc);
                try {
                    CryptoUtils.carregarChavePrivada(tmp, frase);
                } finally {
                    Files.deleteIfExists(tmp);
                }
            } catch (Exception ex) {
                lblErro.setText("Frase secreta invalida.");
                return;
            }

            try {
                List<LinhaIndice> linhas = lerIndice(Path.of(caminho), u);
                App.db.registrarLog(7005, u.getUid());
                App.db.registrarLog(7006, u.getUid());

                List<LinhaIndice> filtradas = new ArrayList<>();
                String grupoLower = u.getGrupoLabel().toLowerCase();
                for (LinhaIndice l : linhas) {
                    if (u.isAdmin()
                        || l.getDono().equalsIgnoreCase(u.getEmail())
                        || l.getGrupo().toLowerCase().contains(grupoLower)) {
                        filtradas.add(l);
                    }
                }
                tabela.setItems(FXCollections.observableArrayList(filtradas));
                App.db.registrarLog(7009, u.getUid());

                u.incrementarConsultas();
                App.db.atualizarUsuario(u);
                lblErro.setText("");
            } catch (Exception ex) {
                String m = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
                if (ex instanceof SecurityException) {
                    App.db.registrarLog(7008, u.getUid());
                    lblErro.setText("Conteudo do indice invalido.");
                    UI.alertaIntegridade(
                        "Indice corrompido ou adulterado",
                        "Nome de codigo invalido detectado no indice. " +
                        "O arquivo pode ter sido modificado por um atacante.\n\n" +
                        "Detalhes: " + ex.getMessage());
                } else if (m.contains("signature") || m.contains("verif")) {
                    App.db.registrarLog(7008, u.getUid());
                    lblErro.setText("Falha na verificacao do indice.");
                    UI.alertaIntegridade(
                        "Assinatura do indice INVALIDA",
                        "A assinatura digital do arquivo index.asd nao confere " +
                        "com a chave publica do administrador.\n\n" +
                        "Isso significa que o arquivo index.enc ou index.asd " +
                        "foi alterado depois de assinado, ou nao foi assinado " +
                        "pelo administrador correto.");
                } else {
                    App.db.registrarLog(7007, u.getUid());
                    lblErro.setText("Falha na decriptacao do indice.");
                    UI.alertaIntegridade(
                        "Falha ao decifrar o indice",
                        "O arquivo index.enc nao pode ser decifrado. " +
                        "Possiveis causas: arquivo corrompido, chave do envelope " +
                        "errada, ou tentativa de adulteracao.\n\n" +
                        "Detalhes: " + ex.getMessage());
                }
            }
        });

        btnAbrir.setOnAction(e -> {
            LinhaIndice sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) {
                lblErro.setText("Selecione um arquivo da lista.");
                return;
            }
            App.db.registrarLog(7010, u.getUid(), sel.getNomeCodigo());

            if (!u.isAdmin() && !sel.getDono().equals(u.getEmail())) {
                App.db.registrarLog(7012, u.getUid(), sel.getNomeCodigo());
                lblErro.setText("Acesso negado: voce nao e dono do arquivo.");
                return;
            }
            App.db.registrarLog(7011, u.getUid(), sel.getNomeCodigo());

            try {
                Path pasta = Path.of(tfPasta.getText().trim());
                String frase = pfFrase.getText();

                Usuario dono = App.db.buscarPorEmail(sel.getDono());
                Database.Chaveiro chDono = App.db.buscarChaveiroPorUid(dono.getUid());

                PrivateKey priv;
                if (dono.getUid() == u.getUid()) {
                    Path tmp = Files.createTempFile("uk", ".bin");
                    Files.write(tmp, chDono.chavePrivadaEnc);
                    try { priv = CryptoUtils.carregarChavePrivada(tmp, frase); }
                    finally { Files.deleteIfExists(tmp); }
                } else {
                    lblErro.setText("Apenas o dono pode decifrar (administrador nao tem a frase do dono).");
                    return;
                }

                X509Certificate certDono = (X509Certificate)
                    java.security.cert.CertificateFactory.getInstance("X.509")
                    .generateCertificate(new java.io.ByteArrayInputStream(chDono.certificadoPem.getBytes()));

                byte[] env = Files.readAllBytes(pasta.resolve(sel.getNomeCodigo() + ".env"));
                byte[] semente = CryptoUtils.rsaDecrypt(env, priv);
                SecretKey kAes = CryptoUtils.deriveAesKey(semente);

                byte[] cifrado = Files.readAllBytes(pasta.resolve(sel.getNomeCodigo() + ".enc"));
                byte[] plano;
                try {
                    plano = CryptoUtils.aesDecrypt(cifrado, kAes);
                } catch (Exception decEx) {
                    App.db.registrarLog(7015, u.getUid(), sel.getNomeCodigo());
                    lblErro.setText("Falha na decriptacao do arquivo.");
                    UI.alertaIntegridade(
                        "Falha ao decifrar " + sel.getNomeCodigo() + ".enc",
                        "O arquivo nao pode ser decifrado com a chave do envelope. " +
                        "Possiveis causas: arquivo .enc corrompido, .env adulterado, " +
                        "ou tentativa de adulteracao deliberada.\n\n" +
                        "Detalhes: " + decEx.getMessage());
                    return;
                }
                App.db.registrarLog(7013, u.getUid(), sel.getNomeCodigo());

                byte[] sig = Files.readAllBytes(pasta.resolve(sel.getNomeCodigo() + ".asd"));
                if (!CryptoUtils.verificar(plano, sig, certDono.getPublicKey())) {
                    App.db.registrarLog(7016, u.getUid(), sel.getNomeCodigo());
                    lblErro.setText("Falha na verificacao de assinatura.");
                    UI.alertaIntegridade(
                        "Assinatura de " + sel.getNomeCodigo() + ".asd INVALIDA",
                        "A assinatura digital do arquivo nao confere com a chave " +
                        "publica do dono (" + sel.getDono() + ").\n\n" +
                        "Isso significa que o arquivo .enc ou .asd foi adulterado, " +
                        "ou nao foi assinado pelo dono legitimo.");
                    return;
                }
                App.db.registrarLog(7014, u.getUid(), sel.getNomeCodigo());

                FileChooser fc = new FileChooser();
                fc.setTitle("Salvar arquivo decifrado");
                fc.setInitialFileName(sel.getNomeSecreto());
                File destino = fc.showSaveDialog(App.primaryStage);
                if (destino != null) {
                    Files.write(destino.toPath(), plano);
                    lblErro.setText("Arquivo salvo em: " + destino.getAbsolutePath());
                }
            } catch (Exception ex) {
                lblErro.setText("Erro ao decifrar: " + ex.getMessage());
            }
        });

        VBox corpo2 = UI.card(
            rotulo("Caminho da pasta"), boxPasta,
            rotulo("Frase secreta"),    pfFrase,
            tabela,
            lblErro,
            acoes
        );

        root.getChildren().addAll(cabecalho, corpo1, corpo2);
        return new Scene(root, 720, 720);
    }

    private List<LinhaIndice> lerIndice(Path pasta, Usuario corrente) throws Exception {
        Path indexEnv = pasta.resolve("index.env");
        Path indexEnc = pasta.resolve("index.enc");
        Path indexAsd = pasta.resolve("index.asd");

        PrivateKey privAdmin = Sessao.getChavePrivadaAdmin();
        if (privAdmin == null) {
            throw new Exception("Chave privada do administrador nao disponivel na sessao.");
        }
        PublicKey pubAdmin;
        if (Sessao.getCertificadoAdmin() != null) {
            pubAdmin = Sessao.getCertificadoAdmin().getPublicKey();
        } else {
            Usuario admin = App.db.getAdmin();
            Database.Chaveiro ch = App.db.buscarChaveiroPorUid(admin.getUid());
            X509Certificate cert = (X509Certificate)
                java.security.cert.CertificateFactory.getInstance("X.509")
                .generateCertificate(new java.io.ByteArrayInputStream(ch.certificadoPem.getBytes()));
            pubAdmin = cert.getPublicKey();
        }

        byte[] env = Files.readAllBytes(indexEnv);
        byte[] semente = CryptoUtils.rsaDecrypt(env, privAdmin);
        SecretKey kAes = CryptoUtils.deriveAesKey(semente);

        byte[] cif = Files.readAllBytes(indexEnc);
        byte[] plano = CryptoUtils.aesDecrypt(cif, kAes);

        byte[] sig = Files.readAllBytes(indexAsd);
        if (!CryptoUtils.verificar(plano, sig, pubAdmin)) {
            throw new Exception("Signature verification failed");
        }

        List<LinhaIndice> out = new ArrayList<>();
        String[] linhas = new String(plano).split("\\R");
        for (String l : linhas) {
            if (l.trim().isEmpty()) continue;
            String[] partes = l.trim().split("\\s+");
            if (partes.length < 4) continue;
            // spec exige alfanumerico; tambem evita path traversal
            if (!partes[0].matches("[A-Za-z0-9]+")) {
                throw new SecurityException(
                    "Nome de codigo invalido no indice: '" + partes[0] + "'");
            }
            out.add(new LinhaIndice(partes[0], partes[1], partes[2], partes[3]));
        }
        return out;
    }

    private TableColumn<LinhaIndice, String> col(String titulo, String prop) {
        TableColumn<LinhaIndice, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(150);
        return c;
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
