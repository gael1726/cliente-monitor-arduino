/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mx.unison.clientemonitor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 *
 * @author gaelm
 */
public class ConexionServidor {

    // Configuración de conexión
    private static final String HOST = "localhost"; // IP del servidor (localhost = misma máquina)
    private static final int PUERTO = 5000;         // Puerto donde escucha el servidor

    // Objetos de conexión
    private Socket socket;              // El socket de conexión
    private PrintWriter salida;         // Para enviar datos al servidor
    private BufferedReader entrada;     // Para recibir datos del servidor

    /**
     * Constructor - Inicializa la conexión con el servidor
     *
     * @throws Exception Si no puede conectarse al servidor
     */
    public ConexionServidor() throws Exception {
        try {
            // Crear el socket y conectar al servidor
            // Socket(host, puerto) intenta conectarse al servidor
            socket = new Socket(HOST, PUERTO);

            // Crear el flujo de SALIDA (para enviar datos)
            // PrintWriter permite escribir Strings
            // true = autoFlush (envía datos inmediatamente)
            salida = new PrintWriter(socket.getOutputStream(), true);

            // Crear el flujo de ENTRADA (para recibir datos)
            // BufferedReader permite leer líneas de texto
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Conectado al servidor en " + HOST + ":" + PUERTO);

        } catch (Exception e) {
            System.err.println("Error al conectar con el servidor: " + e.getMessage());
            throw e; // Re-lanzar la excepción para que el llamador la maneje
        }
    }

    /**
     * Envía datos del sensor al servidor (ya encriptados)
     *
     * Flujo: Arduino → Cliente → Encriptar → Servidor
     *
     * @param datosEncriptados Los datos ya encriptados en Base64
     * @return true si se enviaron correctamente, false si hubo error
     */
    public boolean enviarDatosSensor(String datosEncriptados) {
        try {
            // Verificar que la conexión esté activa
            if (socket == null || socket.isClosed()) {
                System.err.println("Error: Socket no está conectado");
                return false;
            }

            // Enviar comando al servidor para indicar que vienen datos de sensor
            // Protocolo: "DATOS:" + datos_encriptados
            salida.println("DATOS:" + datosEncriptados);

            // Esperar respuesta del servidor
            String respuesta = entrada.readLine();

            if (respuesta != null && respuesta.equals("OK")) {
                System.out.println("Datos enviados y guardados correctamente");
                return true;
            } else {
                System.err.println("Error: Respuesta inesperada del servidor: " + respuesta);
                return false;
            }

        } catch (Exception e) {
            System.err.println("Error al enviar datos: " + e.getMessage());
            return false;
        }
    }

    /**
     * Solicita datos históricos al servidor con filtros
     *
     * Flujo: Cliente solicita → Servidor consulta BD → Encripta → Envía al
     * Cliente
     *
     * @param fechaInicio Fecha de inicio del filtro (formato: yyyy-MM-dd) o
     * null para sin filtro
     * @param fechaFin Fecha de fin del filtro (formato: yyyy-MM-dd) o null para
     * sin filtro
     * @param horaInicio Hora de inicio del filtro (formato: HH:mm:ss) o null
     * para sin filtro
     * @param horaFin Hora de fin del filtro (formato: HH:mm:ss) o null para sin
     * filtro
     * @return String con los datos encriptados recibidos del servidor (o null
     * si hay error)
     */
    public String solicitarDatosHistoricos(String fechaInicio, String fechaFin,
            String horaInicio, String horaFin) {
        try {
            // Verificar que la conexión esté activa
            if (socket == null || socket.isClosed()) {
                System.err.println("Error: Socket no está conectado");
                return null;
            }

            // Construir el comando de consulta
            // Protocolo: "CONSULTA:fechaInicio|fechaFin|horaInicio|horaFin"
            // Si algún filtro es null, se envía "null"
            StringBuilder comando = new StringBuilder("CONSULTA:");
            comando.append(fechaInicio != null ? fechaInicio : "null").append("|");
            comando.append(fechaFin != null ? fechaFin : "null").append("|");
            comando.append(horaInicio != null ? horaInicio : "null").append("|");
            comando.append(horaFin != null ? horaFin : "null");

            System.out.println("Enviando consulta: " + comando);

            // Enviar comando al servidor
            salida.println(comando.toString());

            // Esperar respuesta del servidor
            // El servidor enviará los datos en formato: "DATOS_ENCRIPTADOS:..."
            String respuesta = entrada.readLine();

            if (respuesta != null && respuesta.startsWith("DATOS_ENCRIPTADOS:")) {
                // Extraer los datos encriptados (después de "DATOS_ENCRIPTADOS:")
                String datosEncriptados = respuesta.substring("DATOS_ENCRIPTADOS:".length());
                System.out.println("Datos históricos recibidos");
                return datosEncriptados;

            } else if (respuesta != null && respuesta.equals("SIN_DATOS")) {
                System.out.println("No hay datos para los filtros especificados");
                return "SIN_DATOS";

            } else {
                System.err.println("Error: Respuesta inesperada del servidor: " + respuesta);
                return null;
            }

        } catch (Exception e) {
            System.err.println("Error al solicitar datos históricos: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Solicita TODOS los datos históricos sin filtros
     *
     * @return String con los datos encriptados recibidos del servidor (o null
     * si hay error)
     */
    public String solicitarTodosDatos() {
        return solicitarDatosHistoricos(null, null, null, null);
    }

    /**
     * Verifica si la conexión está activa
     *
     * @return true si está conectado, false si no
     */
    public boolean estaConectado() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    /**
     * Cierra la conexión con el servidor Importante: Siempre cerrar las
     * conexiones para liberar recursos
     */
    public void cerrarConexion() {
        try {
            if (entrada != null) {
                entrada.close();
            }
            if (salida != null) {
                salida.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("Conexión cerrada");
        } catch (Exception e) {
            System.err.println("Error al cerrar conexión: " + e.getMessage());
        }
    }

    /**
     * Método de prueba para verificar la conexión NOTA: Para probarlo, primero
     * debes tener el Servidor ejecutándose
     */
    public static void main(String[] args) {
        try {
            System.out.println("PRUEBA DE CONEXIÓN AL SERVIDOR");
            System.out.println("IMPORTANTE: Asegúrate de que el Servidor esté ejecutándose\n");

            // Crear conexión
            ConexionServidor conexion = new ConexionServidor();

            // Probar envío de datos
            System.out.println("\nPrueba 1: Enviar datos");
            String datosEjemplo = "x:100, y:200, z:300";
            String datosEncriptados = Encriptacion.encriptar(datosEjemplo);
            boolean enviado = conexion.enviarDatosSensor(datosEncriptados);
            System.out.println(enviado ? "Datos enviados" : "Error al enviar");

            // Probar consulta de datos
            System.out.println("\nPrueba 2: Consultar datos");
            String datosRecibidos = conexion.solicitarTodosDatos();
            if (datosRecibidos != null && !datosRecibidos.equals("SIN_DATOS")) {
                System.out.println("Datos recibidos encriptados");
                // Desencriptar para ver el contenido
                String datosDesencriptados = Encriptacion.desencriptar(datosRecibidos);
                System.out.println("Datos desencriptados: " + datosDesencriptados);
            }

            // Cerrar conexión
            conexion.cerrarConexion();

        } catch (Exception e) {
            System.err.println("Error en la prueba: " + e.getMessage());
            System.err.println("\n¿El servidor está ejecutándose en localhost:5000?");
        }
    }
}
