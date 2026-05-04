public class Usuario {

    private String email;
    private String nome;
    private String senha;
    private String fraseSecreta;
    private boolean bloqueado;
    private String role; // "admin" ou "user"
    private int falhasLogin;

    public Usuario(String email, String nome, String senha, String fraseSecreta, String role) {
        this.email = email;
        this.nome = nome;
        this.senha = senha;
        this.fraseSecreta = fraseSecreta;
        this.bloqueado = false;
        this.role = role;
        this.falhasLogin = 0;
    }

    public Usuario(String email, String nome, String senha, String fraseSecreta) {
        this(email, nome, senha, fraseSecreta, "user");
    }

    public String getEmail()        { return email; }
    public String getNome()         { return nome; }
    public String getSenha()        { return senha; }
    public String getFraseSecreta() { return fraseSecreta; }
    public String getRole()         { return role; }
    public boolean isBloqueado()    { return bloqueado; }
    public int getFalhasLogin()     { return falhasLogin; }
    public boolean isAdmin()        { return "admin".equals(role); }

    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }
    public void incrementarFalhas() { falhasLogin++; }
    public void resetarFalhas()     { falhasLogin = 0; }
}
