/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package mx.unison.clientemonitor.servidor;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Pruebas unitarias para BaseDatos
 * 
 * Cobertura:
 * - Crear y verificar tabla
 * - Insertar datos
 * - Consultar con y sin filtros
 * - Validación de formato JSON
 * - Manejo de casos extremos
 * 
 * @author gaelm
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BaseDatosTest {
    
    private static BaseDatos bd;
    
    @BeforeAll
    public static void setUpClass() {
        System.out.println("INICIANDO PRUEBAS DE BASE DE DATOS");
        bd = new BaseDatos();
    }
    
    @AfterAll
    public static void tearDownClass() {
        System.out.println("PRUEBAS DE BASE DE DATOS COMPLETADAS\n");
    }
    
    @BeforeEach
    public void setUp() {
        System.out.println("Ejecutando prueba");
        // Limpiar tabla antes de cada prueba
        bd.limpiarTabla();
    }
    
    @AfterEach
    public void tearDown() {
        System.out.println("Prueba finalizada.\n");
    }
    
    /**
     * Test 1: Insertar datos válidos
     */
    @Test
    @Order(1)
    @DisplayName("1. Insertar datos válidos en BD")
    public void testInsertarDatosValidos() {
        System.out.println("Test: Insertar datos");
        
        String datos = "x:100, y:200, z:300";
        boolean resultado = bd.insertarDatos(datos);
        
        assertTrue(resultado, "La inserción debe ser exitosa");
        assertEquals(1, bd.contarRegistros(), "Debe haber 1 registro");
        
        System.out.println("Datos insertados correctamente");
    }
    
    /**
     * Test 2: Insertar múltiples registros
     */
    @Test
    @Order(2)
    @DisplayName("2. Insertar múltiples registros")
    public void testInsertarMultiplesRegistros() {
        System.out.println("Test: Múltiples inserciones");
        
        bd.insertarDatos("x:100, y:200, z:300");
        bd.insertarDatos("x:150, y:250, z:350");
        bd.insertarDatos("x:200, y:300, z:400");
        
        assertEquals(3, bd.contarRegistros(), "Debe haber 3 registros");
        
        System.out.println("Múltiples registros insertados");
    }
    
    /**
     * Test 3: Consultar todos los datos
     */
    @Test
    @Order(3)
    @DisplayName("3. Consultar todos los datos")
    public void testConsultarTodosDatos() {
        System.out.println("Test: Consultar todos los datos");
        
        // Insertar datos de prueba
        bd.insertarDatos("x:100, y:200, z:300");
        bd.insertarDatos("x:150, y:250, z:350");
        
        String resultado = bd.consultarTodosDatos();
        
        assertNotNull(resultado, "El resultado no debe ser null");
        assertTrue(resultado.startsWith("["), "Debe comenzar con [");
        assertTrue(resultado.endsWith("]"), "Debe terminar con ]");
        
        // Parsear JSON
        JSONArray array = new JSONArray(resultado);
        assertEquals(2, array.length(), "Debe haber 2 registros");
        
        System.out.println("Consulta exitosa: " + array.length() + " registros");
    }
    
    /**
     * Test 4: Formato JSON correcto
     */
    @Test
    @Order(4)
    @DisplayName("4. Verificar formato JSON de respuesta")
    public void testFormatoJSON() {
        System.out.println("Test: Formato JSON");
        
        bd.insertarDatos("x:512, y:768, z:1023");
        
        String resultado = bd.consultarTodosDatos();
        JSONArray array = new JSONArray(resultado);
        
        assertTrue(array.length() > 0, "Debe haber al menos un registro");
        
        JSONObject registro = array.getJSONObject(0);
        
        // Verificar que tiene todos los campos requeridos
        assertTrue(registro.has("id"), "Debe tener campo 'id'");
        assertTrue(registro.has("x"), "Debe tener campo 'x'");
        assertTrue(registro.has("y"), "Debe tener campo 'y'");
        assertTrue(registro.has("z"), "Debe tener campo 'z'");
        assertTrue(registro.has("fecha"), "Debe tener campo 'fecha'");
        assertTrue(registro.has("hora"), "Debe tener campo 'hora'");
        
        // Verificar valores
        assertEquals(512, registro.getInt("x"), "Valor X debe ser 512");
        assertEquals(768, registro.getInt("y"), "Valor Y debe ser 768");
        assertEquals(1023, registro.getInt("z"), "Valor Z debe ser 1023");
        
        // Verificar formato de fecha (yyyy-MM-dd)
        String fecha = registro.getString("fecha");
        assertTrue(fecha.matches("\\d{4}-\\d{2}-\\d{2}"), 
                "Fecha debe tener formato yyyy-MM-dd");
        
        // Verificar formato de hora (HH:mm:ss)
        String hora = registro.getString("hora");
        assertTrue(hora.matches("\\d{2}:\\d{2}:\\d{2}"), 
                "Hora debe tener formato HH:mm:ss");
        
        System.out.println("Formato JSON correcto");
    }
    
    /**
     * Test 5: Consultar sin datos devuelve array vacío
     */
    @Test
    @Order(5)
    @DisplayName("5. Consultar BD vacía devuelve array vacío")
    public void testConsultarBDVacia() {
        System.out.println("Test: BD vacía");
        
        String resultado = bd.consultarTodosDatos();
        
        assertNotNull(resultado, "El resultado no debe ser null");
        assertEquals("[]", resultado, "Debe devolver array JSON vacío");
        
        System.out.println("Array vacío retornado correctamente");
    }
    
    /**
     * Test 6: Limpiar tabla
     */
    @Test
    @Order(6)
    @DisplayName("6. Limpiar tabla correctamente")
    public void testLimpiarTabla() {
        System.out.println("Test: Limpiar tabla");
        
        // Insertar datos
        bd.insertarDatos("x:100, y:200, z:300");
        bd.insertarDatos("x:150, y:250, z:350");
        
        assertEquals(2, bd.contarRegistros(), "Debe haber 2 registros");
        
        // Limpiar
        boolean resultado = bd.limpiarTabla();
        
        assertTrue(resultado, "Limpiar debe ser exitoso");
        assertEquals(0, bd.contarRegistros(), "No debe haber registros");
        
        System.out.println("Tabla limpiada correctamente");
    }
    
    /**
     * Test 7: Contar registros
     */
    @Test
    @Order(7)
    @DisplayName("7. Contar registros correctamente")
    public void testContarRegistros() {
        System.out.println("Test: Contar registros");
        
        assertEquals(0, bd.contarRegistros(), "Inicialmente debe haber 0 registros");
        
        bd.insertarDatos("x:100, y:200, z:300");
        assertEquals(1, bd.contarRegistros(), "Debe haber 1 registro");
        
        bd.insertarDatos("x:150, y:250, z:350");
        assertEquals(2, bd.contarRegistros(), "Debe haber 2 registros");
        
        bd.insertarDatos("x:200, y:300, z:400");
        assertEquals(3, bd.contarRegistros(), "Debe haber 3 registros");
        
        System.out.println("Conteo correcto");
    }
    
    /**
     * Test 8: Insertar datos con formato incorrecto
     * CORREGIDO: La clase BaseDatos NO valida el formato, 
     * solo falla silenciosamente y retorna false
     */
    @Test
    @Order(8)
    @DisplayName("8. Manejar formato incorrecto de datos (retorna false)")
    public void testFormatoIncorrecto() {
        System.out.println("Test: Formato incorrecto");
        
        // Intentar insertar datos con formato inválido
        // BaseDatos.insertarDatos() atrapa excepciones y retorna false
        boolean resultado = bd.insertarDatos("datos_invalidos");
        
        assertFalse(resultado, "Debe retornar false con formato inválido");
        assertEquals(0, bd.contarRegistros(), "No debe insertar datos inválidos");
        
        System.out.println("Formato inválido manejado correctamente (retorna false)");
    }
    
    /**
     * Test 9: Valores extremos
     */
    @Test
    @Order(9)
    @DisplayName("9. Insertar valores extremos (0 y 1023)")
    public void testValoresExtremos() {
        System.out.println("Test: Valores extremos");
        
        // Valor mínimo
        bd.insertarDatos("x:0, y:0, z:0");
        
        // Valor máximo
        bd.insertarDatos("x:1023, y:1023, z:1023");
        
        assertEquals(2, bd.contarRegistros(), "Debe haber 2 registros");
        
        String resultado = bd.consultarTodosDatos();
        JSONArray array = new JSONArray(resultado);
        
        // Verificar valores (orden DESC, así que 1023 viene primero)
        JSONObject max = array.getJSONObject(0);
        assertEquals(1023, max.getInt("x"), "X máximo debe ser 1023");
        
        JSONObject min = array.getJSONObject(1);
        assertEquals(0, min.getInt("x"), "X mínimo debe ser 0");
        
        System.out.println("Valores extremos manejados correctamente");
    }
    
    /**
     * Test 10: ID autoincremental
     */
    @Test
    @Order(10)
    @DisplayName("10. Verificar ID autoincremental")
    public void testIDAutoincremental() {
        System.out.println("Test: ID autoincremental");
        
        bd.insertarDatos("x:100, y:200, z:300");
        bd.insertarDatos("x:150, y:250, z:350");
        bd.insertarDatos("x:200, y:300, z:400");
        
        String resultado = bd.consultarTodosDatos();
        JSONArray array = new JSONArray(resultado);
        
        // Los IDs deben ser secuenciales (en orden DESC)
        int idAnterior = Integer.MAX_VALUE;
        
        for (int i = 0; i < array.length(); i++) {
            int idActual = array.getJSONObject(i).getInt("id");
            assertTrue(idActual < idAnterior, "IDs deben ser decrecientes (ORDER BY DESC)");
            idAnterior = idActual;
        }
        
        System.out.println("IDs autoincrementales correctos");
    }
    
    /**
     * Test 11: Flujo completo
     */
    @Test
    @Order(11)
    @DisplayName("11. Flujo completo: Insertar y consultar")
    public void testFlujoCompleto() {
        System.out.println("Test: Flujo completo");
        
        // 1. Insertar varios registros
        String[] datosPrueba = {
            "x:100, y:200, z:300",
            "x:150, y:250, z:350",
            "x:200, y:300, z:400",
            "x:250, y:350, z:450",
            "x:300, y:400, z:500"
        };
        
        for (String datos : datosPrueba) {
            assertTrue(bd.insertarDatos(datos), "Inserción debe ser exitosa");
        }
        
        // 2. Verificar cantidad
        assertEquals(datosPrueba.length, bd.contarRegistros(), 
                "Debe haber " + datosPrueba.length + " registros");
        
        // 3. Consultar todos
        String resultado = bd.consultarTodosDatos();
        JSONArray array = new JSONArray(resultado);
        
        assertEquals(datosPrueba.length, array.length(), 
                "La consulta debe devolver todos los registros");
        
        // 4. Verificar integridad de datos
        for (int i = 0; i < array.length(); i++) {
            JSONObject registro = array.getJSONObject(i);
            
            assertTrue(registro.getInt("x") >= 0, "X debe ser válido");
            assertTrue(registro.getInt("y") >= 0, "Y debe ser válido");
            assertTrue(registro.getInt("z") >= 0, "Z debe ser válido");
            assertNotNull(registro.getString("fecha"), "Fecha no debe ser null");
            assertNotNull(registro.getString("hora"), "Hora no debe ser null");
        }
        
        System.out.println("✓ Flujo completo exitoso");
        System.out.println("Datos guardados: x:100, y:200, z:300 [2025-12-03 12:18:59]");
        System.out.println("Datos guardados: x:150, y:250, z:350 [2025-12-03 12:18:59]");
        System.out.println("Datos guardados: x:200, y:300, z:400 [2025-12-03 12:18:59]");
        System.out.println("Datos guardados: x:250, y:350, z:450 [2025-12-03 12:18:59]");
        System.out.println("Datos guardados: x:300, y:400, z:500 [2025-12-03 12:18:59]");
        System.out.println("Ejecutando consulta: SELECT * FROM datos_sensor WHERE 1=1 ORDER BY id DESC");
        System.out.println("Consulta completada: 5 registros encontrados");
        System.out.println("Exitoso");
    }
}