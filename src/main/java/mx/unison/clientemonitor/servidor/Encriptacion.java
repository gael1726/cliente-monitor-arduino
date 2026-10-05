
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mx.unison.clientemonitor.servidor;

import mx.unison.clientemonitor.*;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 *
 * @author gaelm
 */
public class Encriptacion {
// CLAVE SECRETA de 32 bytes (256 bits) - DEBE SER LA MISMA EN CLIENTE Y SERVIDOR
    // En producción, esta clave debería estar en un archivo de configuración seguro

    private static final String CLAVE_SECRETA = "ClaveSecreta12345678901234567890"; // 32 caracteres

    // IV (Initialization Vector) de 16 bytes - Para modo CBC
    // El IV agrega aleatoriedad al cifrado, haciendo que el mismo texto produzca diferentes cifrados
    private static final String IV = "1234567890123456"; // 16 caracteres

    // Algoritmo a usar: AES con modo CBC y padding PKCS5
    private static final String ALGORITMO = "AES/CBC/PKCS5Padding";

    /**
     * Encripta un texto usando AES-256 CBC
     *
     * @param textoPlano El texto que queremos encriptar (ejemplo: "x:10, y:20,
     * z:30")
     * @return El texto encriptado en Base64 (para poder enviarlo como String)
     * @throws Exception Si ocurre algún error en el proceso de encriptación
     */
    public static String encriptar(String textoPlano) throws Exception {

        // Paso 1: Crear la clave secreta desde el String
        // getBytes() convierte el String a bytes
        SecretKeySpec claveSecreta = new SecretKeySpec(CLAVE_SECRETA.getBytes("UTF-8"), "AES");

        // Paso 2: Crear el IV (Initialization Vector)
        IvParameterSpec ivParametro = new IvParameterSpec(IV.getBytes("UTF-8"));

        // Paso 3: Crear el objeto Cipher (el que hace el cifrado)
        Cipher cipher = Cipher.getInstance(ALGORITMO);

        // Paso 4: Inicializar el cipher en modo ENCRYPT (encriptar)
        cipher.init(Cipher.ENCRYPT_MODE, claveSecreta, ivParametro);

        // Paso 5: Encriptar el texto
        // doFinal() realiza la operación de cifrado completa
        byte[] encriptado = cipher.doFinal(textoPlano.getBytes("UTF-8"));

        // Paso 6: Convertir los bytes encriptados a Base64
        // Base64 permite representar datos binarios como texto ASCII
        // Esto es necesario para enviar los datos por Socket como String
        return Base64.getEncoder().encodeToString(encriptado);
    }

    /**
     * Desencripta un texto que fue encriptado con el método encriptar()
     *
     * @param textoEncriptado El texto encriptado en Base64
     * @return El texto original desencriptado
     * @throws Exception Si ocurre algún error en el proceso de desencriptación
     */
    public static String desencriptar(String textoEncriptado) throws Exception {

        // Paso 1: Crear la clave secreta (DEBE SER LA MISMA que en encriptar)
        SecretKeySpec claveSecreta = new SecretKeySpec(CLAVE_SECRETA.getBytes("UTF-8"), "AES");

        // Paso 2: Crear el IV (DEBE SER EL MISMO que en encriptar)
        IvParameterSpec ivParametro = new IvParameterSpec(IV.getBytes("UTF-8"));

        // Paso 3: Crear el objeto Cipher
        Cipher cipher = Cipher.getInstance(ALGORITMO);

        // Paso 4: Inicializar el cipher en modo DECRYPT (desencriptar)
        cipher.init(Cipher.DECRYPT_MODE, claveSecreta, ivParametro);

        // Paso 5: Convertir el texto Base64 a bytes
        byte[] bytesEncriptados = Base64.getDecoder().decode(textoEncriptado);

        // Paso 6: Desencriptar los bytes
        byte[] desencriptado = cipher.doFinal(bytesEncriptados);

        // Paso 7: Convertir los bytes desencriptados a String
        return new String(desencriptado, "UTF-8");
    }

    /**
     * Método de prueba para verificar que la encriptación funciona
     * correctamente Puedes ejecutar este main para probar la clase
     */
    public static void main(String[] args) {
        try {
            // Texto de ejemplo (formato de datos del Arduino)
            String textoOriginal = "x:100, y:200, z:300";
            System.out.println("Texto original: " + textoOriginal);

            // Encriptar
            String encriptado = encriptar(textoOriginal);
            System.out.println("Texto encriptado: " + encriptado);

            // Desencriptar
            String desencriptado = desencriptar(encriptado);
            System.out.println("Texto desencriptado: " + desencriptado);

            // Verificar que son iguales
            if (textoOriginal.equals(desencriptado)) {
                System.out.println("Encriptación funcionando correctamente");
            } else {
                System.out.println("Error en la encriptación");
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
