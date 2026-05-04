public class Usuario {

    private String email;
    private String nome;
    private String senha;
    private String fraseSecreta;
    private boolean bloqueado;

    public Usuario(String email, String nome, String senha, String fraseSecreta) {
        this.email = email;
        this.nome = nome;
        this.senha = senha;
        this.fraseSecreta = fraseSecreta;
        this.bloqueado = false;
    }

    public String getEmail() {
        return email;
    }

    public String getSenha() {
        return senha;
    }

    public String getFraseSecreta() {
        return fraseSecreta;
    }

    public boolean isBloqueado() {
        return bloqueado;
    }

    public void setBloqueado(boolean bloqueado) {
        this.bloqueado = bloqueado;
    }
}