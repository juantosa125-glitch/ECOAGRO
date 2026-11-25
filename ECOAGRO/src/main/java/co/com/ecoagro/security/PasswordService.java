package co.com.ecoagro.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

import java.nio.charset.StandardCharsets;

/** Centraliza la política de longitud y las operaciones bcrypt de registro y acceso. */
public final class PasswordService {
    private static final int COSTE = 12;
    private static final int MAX_BYTES = 72;

    /** Restringe el uso del servicio a sus métodos estáticos. */
    private PasswordService() {
    }

    /** Comprueba que la contraseña ocupe entre 1 y 72 bytes en UTF-8. */
    public static boolean longitudValida(String password) {
        return password != null && !password.isEmpty() && password.length() <= MAX_BYTES
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }

    /** Genera el hash que se guarda al registrar una cuenta. */
    public static String generarHash(String password) {
        if (!longitudValida(password)) {
            throw new IllegalArgumentException("Longitud de contraseña inválida.");
        }
        return BCrypt.with(BCrypt.Version.VERSION_2B).hashToString(COSTE, password.toCharArray());
    }

    /** Comprueba si la contraseña recibida corresponde al hash guardado. */
    public static boolean verificar(String password, String hash) {
        return longitudValida(password) && hash != null && hash.length() == 60
                && BCrypt.verifyer().verify(password.toCharArray(), hash).verified;
    }
}
