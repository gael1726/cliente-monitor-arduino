/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/*
 * LectorArduino.java
 * 
 * Esta clase se encarga de LEER DATOS del puerto COM donde está conectado el Arduino.
 * Usa SerialPortDataListener para recibir notificaciones cuando llegan datos.
 * 
 * FUNCIONAMIENTO:
 * 1. Se conecta al puerto COM (ejemplo: COM5)
 * 2. Registra un listener que se activa cuando llegan datos
 * 3. Lee los bytes, construye líneas completas
 * 4. Valida que tengan el formato correcto: "x:#, y:#, z:#"
 * 5. Notifica a quien esté escuchando (mediante el listener)
 */
package mx.unison.clientemonitor;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.nio.charset.StandardCharsets;

/**
 * Clase que lee datos del Arduino mediante puerto serial (COM)
 * Usa eventos para recibir datos de forma asíncrona
 */
public class LectorArduino {

    // INTERFAZ PARA NOTIFICACIONES
    
    /**
     * Interfaz que permite notificar cuando llegan nuevos datos.
     * En nuestro caso, VistaMonitor implementa esta interfaz para recibir los datos.
     */
    public interface DatosListener {
        void onNuevosDatos(String datos);
    }
    
    // VARIABLES DE LA CLASE
    
    // Puerto COM donde está conectado el Arduino
    private SerialPort puerto;
    
    // Objeto que recibirá las notificaciones de nuevos datos
    private DatosListener listener;
    
    // Buffer para construir líneas completas
    // Los datos pueden llegar en fragmentos, este buffer los acumula
    private final StringBuilder lineBuf = new StringBuilder();
    
    // Bandera para controlar si está leyendo o no
    private boolean ejecutando;
    
    // Velocidad de comunicación (debe coincidir con Arduino)
    private static final int BAUD_RATE = 9600;

    // CONSTRUCTOR
    /**
     * Constructor - Inicializa la conexión con el Arduino
     * 
     * @param nombrePuerto Nombre del puerto COM (ejemplo: "COM5")
     * @param listener Objeto que recibirá los datos cuando lleguen
     * @throws Exception Si no puede conectarse al puerto
     */
    public LectorArduino(String nombrePuerto, DatosListener listener) throws Exception {
        this.listener = listener;
        this.ejecutando = false;
        
        try {
            // PASO 1: Obtener el puerto COM por su nombre
            puerto = SerialPort.getCommPort(nombrePuerto);
            
            // PASO 2: Configurar parámetros de comunicación
            puerto.setBaudRate(BAUD_RATE);
            
            // PASO 3: Configurar timeout
            // TIMEOUT_READ_SEMI_BLOCKING con 100ms: espera hasta 100ms por datos
            // Si no llegan datos en 100ms, retorna lo que tenga (puede ser nada)
            puerto.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
            
            System.out.println("[OK] Puerto " + nombrePuerto + " configurado");
            
        } catch (Exception e) {
            System.err.println("[ERROR] Error al configurar Arduino: " + e.getMessage());
            throw e;
        }
    }
    // MÉTODOS DE CONTROL
    
    /**
     * Inicia la lectura de datos
     * Abre el puerto y registra el listener de eventos
     */
    public void iniciar() {
        if (ejecutando) {
            System.out.println("[WARN] Ya está ejecutándose");
            return;
        }
        
        // PASO 1: Abrir el puerto
        if (!puerto.openPort()) {
            System.err.println("[ERROR] No se pudo abrir el puerto");
            return;
        }
        
        System.out.println("[OK] Puerto " + puerto.getSystemPortName() + " abierto correctamente");
        
        // PASO 2: Esperar que Arduino se inicialice
        // Arduino se resetea al abrir el puerto serial
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // PASO 3: Registrar el listener de datos
        // Este listener se activará AUTOMÁTICAMENTE cuando lleguen datos
        puerto.addDataListener(new SerialPortDataListener() {
            
            /**
             * Indica qué eventos queremos escuchar
             * LISTENING_EVENT_DATA_AVAILABLE = cuando hay datos disponibles
             */
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }
            
            /**
             * Este método se llama AUTOMÁTICAMENTE cuando llegan datos
             * Es como un "callback" - el sistema operativo nos avisa
             */
            @Override
            public void serialEvent(SerialPortEvent event) {
                // Verificar que sea el evento correcto
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                    return;
                }
                
                // PASO 1: LEER BYTES DISPONIBLES
                
                // Verificar cuántos bytes hay disponibles
                int bytesDisponibles = puerto.bytesAvailable();
                if (bytesDisponibles <= 0) {
                    return; // No hay datos, salir
                }
                
                // Crear buffer para los bytes
                byte[] buffer = new byte[bytesDisponibles];
                
                // Leer los bytes del puerto
                int bytesLeidos = puerto.readBytes(buffer, buffer.length);
                
                if (bytesLeidos <= 0) {
                    return; // No se leyó nada
                }
                
                System.out.println("[DEBUG] Leídos " + bytesLeidos + " bytes");

                // PASO 2: CONVERTIR BYTES A STRING
                
                // Convertir bytes a String usando codificación US_ASCII
                // Arduino envía texto ASCII simple
                String fragmento = new String(buffer, 0, bytesLeidos, StandardCharsets.US_ASCII);
                
                System.out.println("[DEBUG] Fragmento: [" + fragmento + "]");

                // PASO 3: CONSTRUIR LÍNEAS COMPLETAS
                
                // Los datos pueden llegar en fragmentos:
                // Fragmento 1: "x:10"
                // Fragmento 2: "0, y:200"
                // Fragmento 3: ", z:300\n"
                // 
                // El buffer acumula estos fragmentos hasta encontrar \n
                
                synchronized (lineBuf) {
                    // Agregar el nuevo fragmento al buffer
                    lineBuf.append(fragmento);
                    
                    // Buscar saltos de línea (\n o \r)
                    int indiceSalto;
                    
                    // Procesar TODAS las líneas completas que haya en el buffer
                    while ((indiceSalto = indexOfLineBreak(lineBuf)) >= 0) {
                        
                        // Extraer la línea completa (sin el salto de línea)
                        String linea = lineBuf.substring(0, indiceSalto).trim();
                        
                        // Calcular cuántos caracteres eliminar del buffer
                        // Si es \r\n o \n\r (Windows), eliminar 2 caracteres
                        // Si es solo \n o \r, eliminar 1 carácter
                        int caracteresAEliminar = esParSaltoLinea(lineBuf, indiceSalto) ? 2 : 1;
                        
                        // Eliminar la línea procesada del buffer
                        lineBuf.delete(0, indiceSalto + caracteresAEliminar);
                        
                        // Procesar la línea si no está vacía
                        if (!linea.isEmpty()) {
                            procesarLinea(linea);
                        }
                    }
                }
            }
        });
        
        ejecutando = true;
        System.out.println("[OK] Lectura de Arduino iniciada");
    }
    
    /**
     * Detiene la lectura de datos
     */
    public void detener() {
        if (!ejecutando) {
            return;
        }
        
        ejecutando = false;
        
        if (puerto != null && puerto.isOpen()) {
            // Remover el listener antes de cerrar
            puerto.removeDataListener();
            puerto.closePort();
            System.out.println("[OK] Puerto cerrado");
        }
        
        System.out.println("[OK] Lectura de Arduino detenida");
    }
    
    /**
     * Verifica si está leyendo datos actualmente
     */
    public boolean estaEjecutando() {
        return ejecutando;
    }
    
    /**
     * Cierra el puerto serial
     */
    public void cerrarPuerto() {
        detener();
    }
    
    // MÉTODOS AUXILIARES
    
    /**
     * Busca la posición del primer salto de línea (\n o \r)
     * 
     * @param s Secuencia de caracteres donde buscar
     * @return Índice del salto de línea, o -1 si no hay
     */
    private int indexOfLineBreak(CharSequence s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r') {
                return i;
            }
        }
        return -1; // No se encontró
    }
    
    /**
     * Verifica si hay un par de saltos de línea (\r\n o \n\r)
     * Windows usa \r\n, algunos sistemas usan \n\r
     * 
     * @param s Secuencia de caracteres
     * @param i Índice donde está el primer salto
     * @return true si es un par, false si es solo uno
     */
    private boolean esParSaltoLinea(CharSequence s, int i) {
        // Verificar que haya un siguiente carácter
        if (i + 1 >= s.length()) {
            return false;
        }
        
        char a = s.charAt(i);
        char b = s.charAt(i + 1);
        
        // Es par si es \r\n o \n\r
        return (a == '\r' && b == '\n') || (a == '\n' && b == '\r');
    }
    
    /**
     * Procesa una línea completa de datos
     * 
     * @param linea Línea recibida del Arduino
     */
    private void procesarLinea(String linea) {
        System.out.println("[RAW] Datos crudos: [" + linea + "]");
        
        // Limpiar la línea
        linea = linea.trim();
        
        if (linea.isEmpty()) {
            System.out.println("[SKIP] Línea vacía ignorada");
            return;
        }
        
        System.out.println("[LIMPIO] Datos limpios: [" + linea + "]");
        
        // Validar formato
        if (validarFormato(linea)) {
            System.out.println("[DATOS] Datos Arduino validados: " + linea);
            
            // Notificar al listener
            if (listener != null) {
                listener.onNuevosDatos(linea);
            }
        } else {
            System.err.println("[WARN] Formato inválido: " + linea);
            System.err.println("       Esperado: 'x:###, y:###, z:###'");
            System.err.println("       Contiene 'x:'? " + linea.toLowerCase().contains("x:"));
            System.err.println("       Contiene 'y:'? " + linea.toLowerCase().contains("y:"));
            System.err.println("       Contiene 'z:'? " + linea.toLowerCase().contains("z:"));
        }
    }
    
    /**
     * Valida que los datos tengan el formato correcto
     * Formato esperado: "x:###, y:###, z:###" o "x:###,y:###,z:###"
     */
    private boolean validarFormato(String datos) {
        String datosLower = datos.toLowerCase();
        
        boolean tieneX = datosLower.contains("x:");
        boolean tieneY = datosLower.contains("y:");
        boolean tieneZ = datosLower.contains("z:");
        
        return tieneX && tieneY && tieneZ;
    }

    // MÉTODOS ESTÁTICOS ÚTILES
    
    /**
     * Obtiene lista de todos los puertos COM disponibles
     */
    public static String[] obtenerPuertosDisponibles() {
        SerialPort[] puertos = SerialPort.getCommPorts();
        String[] nombresPuertos = new String[puertos.length];
        
        for (int i = 0; i < puertos.length; i++) {
            nombresPuertos[i] = puertos[i].getSystemPortName();
        }
        
        return nombresPuertos;
    }

    // MÉTODO MAIN - PARA PRUEBAS
    
    public static void main(String[] args) {
        System.out.println("=== PRUEBA DEL LECTOR DE ARDUINO ===\n");
        
        String[] puertos = obtenerPuertosDisponibles();
        System.out.println("Puertos COM disponibles:");
        for (String puerto : puertos) {
            System.out.println("  - " + puerto);
        }
        
        if (puertos.length == 0) {
            System.out.println("[ERROR] No se encontraron puertos COM");
            return;
        }
        
        try {
            DatosListener listener = new DatosListener() {
                @Override
                public void onNuevosDatos(String datos) {
                    System.out.println("[OK] DATOS PROCESADOS: " + datos);
                }
            };
            
            String puertoArduino = "COM5"; // Cambiar según tu puerto
            System.out.println("\n[->] Intentando conectar a " + puertoArduino + "...");
            
            LectorArduino lector = new LectorArduino(puertoArduino, listener);
            lector.iniciar();
            
            System.out.println("[->] Leyendo datos por 30 segundos...\n");
            Thread.sleep(30000);
            
            lector.detener();
            
            System.out.println("\nPRUEBA COMPLETADA");
            
        } catch (Exception e) {
            System.err.println("[ERROR] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}