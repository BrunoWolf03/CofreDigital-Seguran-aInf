public class Usuario {

    private int     uid;
    private String  email;
    private String  nome;
    private String  senhaHash;
    private String  totpSecret;
    private Integer kid;
    private String  role;
    private boolean bloqueado;
    private String  bloqueadoAte;
    private int     falhasLogin;
    private int     totalAcessos;
    private int     totalConsultas;

    public Usuario(String email, String nome, String senhaHash,
                   String totpSecret, String role) {
        this.email      = email;
        this.nome       = nome;
        this.senhaHash  = senhaHash;
        this.totpSecret = totpSecret;
        this.role       = role;
    }

    public int     getUid()           { return uid; }
    public String  getEmail()         { return email; }
    public String  getNome()          { return nome; }
    public String  getSenhaHash()     { return senhaHash; }
    public String  getTotpSecret()    { return totpSecret; }
    public Integer getKid()           { return kid; }
    public String  getRole()          { return role; }
    public boolean isBloqueado()      { return bloqueado; }
    public String  getBloqueadoAte()  { return bloqueadoAte; }
    public int     getFalhasLogin()   { return falhasLogin; }
    public int     getTotalAcessos()  { return totalAcessos; }
    public int     getTotalConsultas(){ return totalConsultas; }
    public boolean isAdmin()          { return "admin".equals(role); }
    public String  getGrupoLabel()    { return isAdmin() ? "Administrador" : "Usuario"; }

    public void setUid(int uid)                { this.uid = uid; }
    public void setSenhaHash(String h)         { this.senhaHash = h; }
    public void setTotpSecret(String s)        { this.totpSecret = s; }
    public void setKid(Integer k)              { this.kid = k; }
    public void setBloqueado(boolean b)        { this.bloqueado = b; }
    public void setBloqueadoAte(String ate)    { this.bloqueadoAte = ate; }
    public void setFalhasLogin(int n)          { this.falhasLogin = n; }
    public void setTotalAcessos(int n)         { this.totalAcessos = n; }
    public void setTotalConsultas(int n)       { this.totalConsultas = n; }

    public void incrementarFalhas()   { falhasLogin++; }
    public void resetarFalhas()       { falhasLogin = 0; }
    public void incrementarAcessos()  { totalAcessos++; }
    public void incrementarConsultas(){ totalConsultas++; }
}
