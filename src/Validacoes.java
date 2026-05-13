import java.util.regex.Pattern;

public class Validacoes {

    private static final Pattern EMAIL_REGEX = Pattern.compile(
        "^[\\w.+\\-]+@[\\w\\-]+\\.[a-zA-Z]{2,}$");

    public static boolean emailValido(String email) {
        return email != null && EMAIL_REGEX.matcher(email).matches();
    }

    public static String validarSenhaPessoal(String senha) {
        if (senha == null || senha.isEmpty()) return "Senha obrigatoria.";
        if (!senha.matches("\\d+")) return "Senha deve conter apenas digitos numericos (0-9).";
        if (senha.length() < 8 || senha.length() > 10)
            return "Senha deve ter entre 8 e 10 digitos. Atual: " + senha.length() + ".";
        if (Pattern.compile("(.)\\1+").matcher(senha).find())
            return "Senha nao pode ter digitos repetidos em sequencia.";
        return null;
    }
}
