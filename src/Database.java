import java.util.ArrayList;
import java.util.List;

public class Database {

    private List<Usuario> usuarios = new ArrayList<>();

    public boolean existeUsuario() {
        return !usuarios.isEmpty();
    }

    public void salvarUsuario(Usuario u) {
        usuarios.add(u);
    }

    public Usuario getAdmin() {
        if (usuarios.isEmpty()) return null;
        return usuarios.get(0);
    }

    public Usuario buscarPorEmail(String email) {
        for (Usuario u : usuarios) {
            if (u.getEmail().equals(email)) {
                return u;
            }
        }
        return null;
    }
}