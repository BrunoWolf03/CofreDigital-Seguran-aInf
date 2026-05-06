public class Usuario {

    private int    uid;
    private String email;
    private String nome;
    private String senhaHash;     // bcrypt hash armazenado no banco
    private String totpSecret;    // segredo TOTP criptografado (BASE32/AES)
    private String fraseSecreta;  // mantida em memória, nunca vai ao banco
    private boolean bloqueado;
    private String  bloqueadoAte; // datetime ISO até quando está bloqueado
    private String  role;         // "admin" ou "user"
    private int    falhasLogin;
    private int    totalAcessos;

    // Construtor completo — usado ao carregar do banco
    public Usuario(String email, String nome, String senhaHash,
                   String totpSecret, String role) {
        this.email       = email;
        this.nome        = nome;
        this.senhaHash   = senhaHash;
        this.totpSecret  = totpSecret;
        this.role        = role;
        this.bloqueado   = false;
        this.falhasLogin = 0;
        this.totalAcessos = 0;
    }

    // Construtor de cadastro — recebe senha plana antes do bcrypt ser aplicado
    public Usuario(String email, String nome, String senhaHash,
                   String totpSecret, String fraseSecreta, String role) {
        this(email, nome, senhaHash, totpSecret, role);
        this.fraseSecreta = fraseSecreta;
    }

    // Compat: cadastro sem totpSecret (fluxo legado enquanto parceira integra)
    public Usuario(String email, String nome, String senhaHash,
                   String fraseSecreta) {
        this(email, nome, senhaHash, "", fraseSecreta, "user");
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public int     getUid()          { return uid; }
    public String  getEmail()        { return email; }
    public String  getNome()         { return nome; }
    public String  getSenhaHash()    { return senhaHash; }
    public String  getSenha()        { return senhaHash; } // alias para compat
    public String  getTotpSecret()   { return totpSecret; }
    public String  getFraseSecreta() { return fraseSecreta; }
    public String  getRole()         { return role; }
    public boolean isBloqueado()     { return bloqueado; }
    public String  getBloqueadoAte() { return bloqueadoAte; }
    public int     getFalhasLogin()  { return falhasLogin; }
    public int     getTotalAcessos() { return totalAcessos; }
    public boolean isAdmin()         { return "admin".equals(role); }

    // -------------------------------------------------------------------------
    // Setters
    // -------------------------------------------------------------------------

    public void setUid(int uid)                    { this.uid = uid; }
    public void setSenhaHash(String h)             { this.senhaHash = h; }
    public void setTotpSecret(String s)            { this.totpSecret = s; }
    public void setFraseSecreta(String f)          { this.fraseSecreta = f; }
    public void setBloqueado(boolean bloqueado)    { this.bloqueado = bloqueado; }
    public void setBloqueadoAte(String ate)        { this.bloqueadoAte = ate; }
    public void setFalhasLogin(int n)              { this.falhasLogin = n; }
    public void setTotalAcessos(int n)             { this.totalAcessos = n; }

    public void incrementarFalhas() { falhasLogin++; }
    public void resetarFalhas()     { falhasLogin = 0; }
    public void incrementarAcessos() { totalAcessos++; }
}
