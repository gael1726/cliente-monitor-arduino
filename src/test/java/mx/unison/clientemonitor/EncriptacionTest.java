/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package mx.unison.clientemonitor;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para la clase Encriptacion
 * 
 * Cobertura:
 * - Encriptación y desencriptación básica
 * - Consistencia del cifrado
 * - Manejo de casos especiales (strings vacíos, caracteres especiales)
 * - Validación de excepciones
 */
public class EncriptacionTest {
    
    private static final String TEXTO_SIMPLE = "x:100, y:200, z:300";
    private static final String TEXTO_VACIO = "";
    private static final String TEXTO_ESPECIAL = "x:100, y:200, z:300 ñáéíóú @#$%";
    private static final String TEXTO_LARGO = "x:".repeat(1000) + "100";
    
    @BeforeAll
    public static void setUpClass() {
        System.out.println("INICIANDO PRUEBAS DE ENCRIPTACIÓN");
    }
    
    @AfterAll
    public static void tearDownClass() {
        System.out.println("PRUEBAS DE ENCRIPTACIÓN COMPLETADAS\n");
    }
    
    @BeforeEach
    public void setUp() {
        System.out.println("Ejecutando prueba");
    }
    
    @AfterEach
    public void tearDown() {
        System.out.println("Prueba finalizada.\n");
    }
    
    /**
     * Prueba básica: encriptar y desencriptar texto simple
     */
    @Test
    @DisplayName("Encriptar y desencriptar texto simple")
    public void testEncriptarDesencriptarBasico() throws Exception {
        System.out.println("Test: Encriptar/Desencriptar básico");
        
        // Encriptar
        String encriptado = Encriptacion.encriptar(TEXTO_SIMPLE);
        
        // Verificar que el texto encriptado no es igual al original
        assertNotNull(encriptado, "El texto encriptado no debe ser null");
        assertNotEquals(TEXTO_SIMPLE, encriptado, "El texto encriptado debe ser diferente al original");
        assertTrue(encriptado.length() > 0, "El texto encriptado debe tener contenido");
        
        // Desencriptar
        String desencriptado = Encriptacion.desencriptar(encriptado);
        
        // Verificar que coincide con el original
        assertEquals(TEXTO_SIMPLE, desencriptado, 
                "El texto desencriptado debe ser igual al original");
        
        System.out.println("Texto encriptado y desencriptado correctamente");
    }
    
    /**
     * Prueba: el mismo texto siempre produce el mismo resultado encriptado
     * (porque usamos IV fijo)
     */
    @Test
    @DisplayName("Consistencia del cifrado (mismo input mismo output)")
    public void testConsistenciaCifrado() throws Exception {
        System.out.println("Test: Consistencia del cifrado");
        
        String encriptado1 = Encriptacion.encriptar(TEXTO_SIMPLE);
        String encriptado2 = Encriptacion.encriptar(TEXTO_SIMPLE);
        
        assertEquals(encriptado1, encriptado2, 
                "El mismo texto debe producir el mismo cifrado");
        
        System.out.println("Cifrado consistente verificado");
    }
    
    /**
     * Prueba: manejar string vacío
     */
    @Test
    @DisplayName("Encriptar y desencriptar string vacío")
    public void testEncriptarStringVacio() throws Exception {
        System.out.println("Test: String vacío");
        
        String encriptado = Encriptacion.encriptar(TEXTO_VACIO);
        String desencriptado = Encriptacion.desencriptar(encriptado);
        
        assertEquals(TEXTO_VACIO, desencriptado, 
                "String vacío debe desencriptarse correctamente");
        
        System.out.println("String vacío manejado correctamente");
    }
    
    /**
     * Prueba: caracteres especiales y unicode
     */
    @Test
    @DisplayName("Encriptar texto con caracteres especiales")
    public void testEncriptarCaracteresEspeciales() throws Exception {
        System.out.println("Test: Caracteres especiales");
        
        String encriptado = Encriptacion.encriptar(TEXTO_ESPECIAL);
        String desencriptado = Encriptacion.desencriptar(encriptado);
        
        assertEquals(TEXTO_ESPECIAL, desencriptado, 
                "Los caracteres especiales deben preservarse");
        
        System.out.println("Caracteres especiales manejados correctamente");
    }
    
    /**
     * Prueba: texto largo
     */
    @Test
    @DisplayName("Encriptar texto largo")
    public void testEncriptarTextoLargo() throws Exception {
        System.out.println("Test: Texto largo");
        
        String encriptado = Encriptacion.encriptar(TEXTO_LARGO);
        String desencriptado = Encriptacion.desencriptar(encriptado);
        
        assertEquals(TEXTO_LARGO, desencriptado, 
                "Textos largos deben encriptarse correctamente");
        assertEquals(TEXTO_LARGO.length(), desencriptado.length(), 
                "La longitud debe preservarse");
        
        System.out.println("Texto largo manejado correctamente");
    }
    
    /**
     * Prueba: formato Base64 válido
     */
    @Test
    @DisplayName("Verificar formato Base64 del texto encriptado")
    public void testFormatoBase64() throws Exception {
        System.out.println("Test: Formato Base64");
        
        String encriptado = Encriptacion.encriptar(TEXTO_SIMPLE);
        
        // Verificar que es Base64 válido (solo contiene caracteres válidos)
        assertTrue(encriptado.matches("^[A-Za-z0-9+/]*={0,2}$"), 
                "El texto encriptado debe estar en formato Base64 válido");
        
        System.out.println("Formato Base64 válido");
    }
    
    /**
     * Prueba: desencriptar texto inválido debe lanzar excepción
     */
    @Test
    @DisplayName("Desencriptar texto inválido lanza excepción")
    public void testDesencriptarTextoInvalido() {
        System.out.println("Test: Texto inválido");
        
        assertThrows(Exception.class, () -> {
            Encriptacion.desencriptar("texto_invalido_no_base64!@#$");
        }, "Desencriptar texto inválido debe lanzar excepción");
        
        System.out.println("Excepción lanzada correctamente");
    }
    
    /**
     * Prueba: desencriptar Base64 válido pero no cifrado correctamente
     */
    @Test
    @DisplayName("Desencriptar Base64 válido pero contenido incorrecto")
    public void testDesencriptarBase64Invalido() {
        System.out.println("Test: Base64 con contenido incorrecto");
        
        // Base64 válido pero no es un texto cifrado con nuestra clave
        String base64Invalido = "SGVsbG8gV29ybGQ="; // "Hello World" en Base64
        
        assertThrows(Exception.class, () -> {
            Encriptacion.desencriptar(base64Invalido);
        }, "Debe lanzar excepción al intentar desencriptar contenido incorrecto");
        
        System.out.println("Contenido incorrecto detectado");
    }
    
    /**
     * Prueba: null input debe lanzar excepción
     */
    @Test
    @DisplayName("Encriptar null lanza NullPointerException")
    public void testEncriptarNull() {
        System.out.println("Test: Input null");
        
        assertThrows(Exception.class, () -> {
            Encriptacion.encriptar(null);
        }, "Encriptar null debe lanzar excepción");
        
        System.out.println("Null manejado correctamente");
    }
    
    /**
     * Prueba: diferentes textos producen diferentes cifrados
     */
    @Test
    @DisplayName("Textos diferentes producen cifrados diferentes")
    public void testTextosDiferentesCifradosDiferentes() throws Exception {
        System.out.println("Test: Cifrados únicos");
        
        String texto1 = "x:100, y:200, z:300";
        String texto2 = "x:100, y:200, z:301"; // Un carácter diferente
        
        String encriptado1 = Encriptacion.encriptar(texto1);
        String encriptado2 = Encriptacion.encriptar(texto2);
        
        assertNotEquals(encriptado1, encriptado2, 
                "Textos diferentes deben producir cifrados diferentes");
        
        System.out.println("Cifrados únicos verificados");
    }
    
    /**
     * Prueba de integración: simular flujo completo del sistema
     */
    @Test
    @DisplayName("Flujo completo: Cliente → Servidor")
    public void testFlujoCompletoSistema() throws Exception {
        System.out.println("Test: Flujo completo del sistema");
        
        // 1. Cliente genera datos del sensor
        String datosOriginales = "x:512, y:768, z:1023";
        
        // 2. Cliente encripta antes de enviar
        String datosEncriptados = Encriptacion.encriptar(datosOriginales);
        
        // 3. Servidor recibe y desencripta
        String datosRecibidos = Encriptacion.desencriptar(datosEncriptados);
        
        // 4. Verificar integridad
        assertEquals(datosOriginales, datosRecibidos, 
                "Los datos deben mantener integridad en todo el flujo");
        
        System.out.println("Flujo completo exitoso");
    }
}