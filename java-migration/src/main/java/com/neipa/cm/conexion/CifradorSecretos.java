package com.neipa.cm.conexion;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifra las contrasenas de las conexiones con AES-256-GCM. La clave sale de
 * un secreto de configuracion (SHA-256 del texto): si se cambia el secreto,
 * hay que volver a cargar las contrasenas.
 */
public class CifradorSecretos {

    private static final String PREFIJO = "v1:";
    private static final int IV = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec clave;
    private final SecureRandom azar = new SecureRandom();

    public CifradorSecretos(String secreto) {
        if (secreto == null || secreto.isEmpty()) {
            throw new IllegalArgumentException("Falta el secreto para cifrar las contrasenas");
        }
        try {
            byte[] k = MessageDigest.getInstance("SHA-256").digest(secreto.getBytes(StandardCharsets.UTF_8));
            this.clave = new SecretKeySpec(k, "AES");
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public String cifrar(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV];
            azar.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cifrado = c.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] todo = new byte[IV + cifrado.length];
            System.arraycopy(iv, 0, todo, 0, IV);
            System.arraycopy(cifrado, 0, todo, IV, cifrado.length);
            return PREFIJO + Base64.getEncoder().encodeToString(todo);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo cifrar la contrasena", ex);
        }
    }

    public String descifrar(String guardado) {
        if (guardado == null) {
            return null;
        }
        if (!guardado.startsWith(PREFIJO)) {
            throw new IllegalStateException("Contrasena guardada en un formato desconocido");
        }
        try {
            byte[] todo = Base64.getDecoder().decode(guardado.substring(PREFIJO.length()));
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, todo, 0, IV));
            return new String(c.doFinal(todo, IV, todo.length - IV), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "No se pudo descifrar la contrasena (cambio la clave neipa.cm.clave-conexiones?): volver a cargarla", ex);
        }
    }
}
