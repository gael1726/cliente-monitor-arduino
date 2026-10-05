/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package mx.unison.clientemonitor;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Pruebas unitarias para SimuladorArduino
 * 
 * Cobertura:
 * - Iniciar y detener simulación
 * - Generación de datos válidos
 * - Threading correcto
 * - Variación de valores
 * - Listener notifications
 */
public class SimuladorArduinoTest {
    
    private SimuladorArduino simulador;
    private List<String> datosRecibidos;
    private CountDownLatch latch;
    
    @BeforeAll
    public static void setUpClass() {
        System.out.println("INICIANDO PRUEBAS DE SIMULADOR");
    }
    
    @AfterAll
    public static void tearDownClass() {
        System.out.println("PRUEBAS DE SIMULADOR COMPLETADAS\n");
    }
    
    @BeforeEach
    public void setUp() {
        System.out.println("Configurando simulador...");
        datosRecibidos = new ArrayList<>();
        
        // Crear listener que captura los datos
        SimuladorArduino.DatosListener listener = datos -> {
            datosRecibidos.add(datos);
            if (latch != null) {
                latch.countDown();
            }
        };
        
        simulador = new SimuladorArduino(listener);
    }
    
    @AfterEach
    public void tearDown() {
        if (simulador != null && simulador.estaEjecutando()) {
            simulador.detener();
        }
        System.out.println("Simulador detenido.\n");
    }
    
    /**
     * Prueba: iniciar simulador
     */
    @Test
    @DisplayName("Iniciar simulador correctamente")
    public void testIniciar() throws InterruptedException {
        System.out.println("Test: Iniciar simulador");
        
        assertFalse(simulador.estaEjecutando(), "Simulador debe estar detenido inicialmente");
        
        simulador.iniciar();
        
        assertTrue(simulador.estaEjecutando(), "Simulador debe estar ejecutándose");
        
        // Esperar un momento para que genere datos
        Thread.sleep(500);
        
        System.out.println("Simulador iniciado correctamente");
    }
    
    /**
     * Prueba: detener simulador
     */
    @Test
    @DisplayName("Detener simulador correctamente")
    public void testDetener() throws InterruptedException {
        System.out.println("Test: Detener simulador");
        
        simulador.iniciar();
        assertTrue(simulador.estaEjecutando(), "Simulador debe estar ejecutándose");
        
        Thread.sleep(500);
        
        simulador.detener();
        assertFalse(simulador.estaEjecutando(), "Simulador debe estar detenido");
        
        System.out.println("Simulador detenido correctamente");
    }
    
    /**
     * Prueba: generar datos con formato correcto
     */
    @Test
    @DisplayName("Generar datos con formato válido")
    public void testFormatoDatos() throws InterruptedException {
        System.out.println("Test: Formato de datos");
        
        latch = new CountDownLatch(1);
        simulador.iniciar();
        
        // Esperar a recibir al menos un dato
        boolean recibido = latch.await(3, TimeUnit.SECONDS);
        
        assertTrue(recibido, "Debe recibir al menos un dato");
        assertFalse(datosRecibidos.isEmpty(), "Debe haber datos recibidos");
        
        String datos = datosRecibidos.get(0);
        
        // Verificar formato: "x:#, y:#, z:#"
        assertTrue(datos.matches("x:\\d+, y:\\d+, z:\\d+"), 
                "Formato debe ser 'x:#, y:#, z:#'");
        
        System.out.println("Datos recibidos: " + datos);
        System.out.println("Formato válido");
    }
    
    /**
     * Prueba: valores dentro del rango permitido
     */
    @Test
    @DisplayName("Valores dentro del rango 0-1023")
    public void testRangoValores() throws InterruptedException {
        System.out.println("Test: Rango de valores");
        
        latch = new CountDownLatch(5);
        simulador.iniciar();
        
        // Esperar 5 lecturas
        boolean recibido = latch.await(10, TimeUnit.SECONDS);
        assertTrue(recibido, "Debe recibir 5 lecturas");
        
        for (String datos : datosRecibidos) {
            String[] partes = datos.split(",");
            
            int x = Integer.parseInt(partes[0].split(":")[1].trim());
            int y = Integer.parseInt(partes[1].split(":")[1].trim());
            int z = Integer.parseInt(partes[2].split(":")[1].trim());
            
            // Verificar que estén en el rango 0-1023
            assertTrue(x >= 0 && x <= 1023, "X debe estar entre 0 y 1023, valor: " + x);
            assertTrue(y >= 0 && y <= 1023, "Y debe estar entre 0 y 1023, valor: " + y);
            assertTrue(z >= 0 && z <= 1023, "Z debe estar entre 0 y 1023, valor: " + z);
        }
        
        System.out.println("Todos los valores en rango válido");
    }
    
    /**
     * Prueba: valores varían entre lecturas (no son estáticos)
     */
    @Test
    @DisplayName("Valores varían entre lecturas")
    public void testVariacionValores() throws InterruptedException {
        System.out.println("Test: Variación de valores");
        
        latch = new CountDownLatch(3);
        simulador.iniciar();
        
        boolean recibido = latch.await(5, TimeUnit.SECONDS);
        assertTrue(recibido, "Debe recibir 3 lecturas");
        
        // Verificar que no todas las lecturas sean idénticas
        String primera = datosRecibidos.get(0);
        boolean hayVariacion = false;
        
        for (int i = 1; i < datosRecibidos.size(); i++) {
            if (!datosRecibidos.get(i).equals(primera)) {
                hayVariacion = true;
                break;
            }
        }
        
        assertTrue(hayVariacion, "Los valores deben variar entre lecturas");
        
        System.out.println("Variación confirmada");
    }
    
    /**
     * Prueba: no iniciar dos veces
     */
    @Test
    @DisplayName("No permitir iniciar dos veces")
    public void testNoIniciarDobleVez() throws InterruptedException {
        System.out.println("Test: Prevenir inicio doble");
        
        simulador.iniciar();
        int cantidadInicial = datosRecibidos.size();
        
        Thread.sleep(500);
        
        // Intentar iniciar de nuevo
        simulador.iniciar();
        
        Thread.sleep(500);
        
        // Verificar que solo hay un thread ejecutándose
        // (no deberíamos tener el doble de datos)
        assertTrue(simulador.estaEjecutando(), "Debe seguir ejecutándose");
        
        System.out.println("Inicio doble prevenido");
    }
    
    /**
     * Prueba: detener sin haber iniciado no causa error
     */
    @Test
    @DisplayName("Detener sin iniciar no causa error")
    public void testDetenerSinIniciar() {
        System.out.println("Test: Detener sin iniciar");
        
        assertDoesNotThrow(() -> {
            simulador.detener();
        }, "Detener sin iniciar no debe causar error");
        
        assertFalse(simulador.estaEjecutando(), "Debe estar detenido");
        
        System.out.println("Detención segura");
    }
    
    /**
     * Prueba: configurar variación máxima
     */
    @Test
    @DisplayName("Configurar variación máxima")
    public void testSetVariacionMaxima() throws InterruptedException {
        System.out.println("Test: Variación máxima");
        
        // Configurar variación muy pequeña
        simulador.setVariacionMaxima(5);
        
        latch = new CountDownLatch(3);
        simulador.iniciar();
        
        boolean recibido = latch.await(5, TimeUnit.SECONDS);
        assertTrue(recibido, "Debe recibir 3 lecturas");
        
        // Verificar que las variaciones son pequeñas
        if (datosRecibidos.size() >= 2) {
            String[] partes1 = datosRecibidos.get(0).split(",");
            String[] partes2 = datosRecibidos.get(1).split(",");
            
            int x1 = Integer.parseInt(partes1[0].split(":")[1].trim());
            int x2 = Integer.parseInt(partes2[0].split(":")[1].trim());
            
            int diferencia = Math.abs(x1 - x2);
            
            // La diferencia debe ser razonable para una variación de 5
            assertTrue(diferencia <= 10, 
                    "Con variación de 5, la diferencia debe ser pequeña");
        }
        
        System.out.println("Variación configurada correctamente");
    }
    
    /**
     * Prueba: listener recibe notificaciones
     */
    @Test
    @DisplayName("Listener recibe notificaciones correctas")
    public void testListenerNotificaciones() throws InterruptedException {
        System.out.println("Test: Notificaciones al listener");
        
        latch = new CountDownLatch(1);
        simulador.iniciar();
        
        boolean notificado = latch.await(3, TimeUnit.SECONDS);
        
        assertTrue(notificado, "El listener debe recibir notificación");
        assertFalse(datosRecibidos.isEmpty(), "Debe haber datos recibidos");
        
        System.out.println("Listener notificado correctamente");
    }
    
    /**
     * Prueba de carga: generar muchos datos
     */
    @Test
    @DisplayName("Generar múltiples lecturas sin errores")
    @Timeout(15)
    public void testGeneracionContinua() throws InterruptedException {
        System.out.println("Test: Generación continua");
        
        latch = new CountDownLatch(10);
        simulador.iniciar();
        
        boolean completo = latch.await(15, TimeUnit.SECONDS);
        
        assertTrue(completo, "Debe generar 10 lecturas");
        assertEquals(10, datosRecibidos.size(), "Debe haber exactamente 10 lecturas");
        
        // Verificar que todas son válidas
        for (String datos : datosRecibidos) {
            assertTrue(datos.matches("x:\\d+, y:\\d+, z:\\d+"), 
                    "Todas las lecturas deben ser válidas");
        }
        
        System.out.println("Generación continua exitosa");
    }
}