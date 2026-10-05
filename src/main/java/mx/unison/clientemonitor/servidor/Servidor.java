/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mx.unison.clientemonitor.servidor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author gaelm
 */
public class Servidor {

    // Puerto en el que escucha el servidor
    private static final int PUERTO = 5000;

    // Base de datos
    private BaseDatos baseDatos;

    // Formato para mostrar timestamps
    private static final SimpleDateFormat FORMATO_TIMESTAMP
            = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Constructor del servidor
     */
    public Servidor() {
        // Inicializar la base de datos
        baseDatos = new BaseDatos();
        System.out.println("Base de datos inicializada");
    }

    /**
     * Inicia el servidor y escucha conexiones Este método corre indefinidamente
     */
    public void iniciar() {
        

        // ServerSocket: Escucha conexiones en el puerto especificado
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {

            System.out.println("Servidor iniciado en puerto " + PUERTO);
            System.out.println("Esperando conexiones de clientes...");
            

            int numeroCliente = 1;

            // Bucle infinito: espera conexiones continuamente
            while (true) {
                try {
                    // accept() BLOQUEA hasta que un cliente se conecte
                    // Cuando se conecta, retorna un Socket para comunicarse
                    Socket socketCliente = serverSocket.accept();

                    // Obtener información del cliente
                    String ipCliente = socketCliente.getInetAddress().getHostAddress();
                    String timestamp = FORMATO_TIMESTAMP.format(new Date());

                    System.out.println("[" + timestamp + "] NUEVA CONEXIÓN");
                    System.out.println("Cliente #" + numeroCliente);
                    System.out.println("IP: " + ipCliente);
                   

                    //Crear una copia final de la variable
                    final int numCliente = numeroCliente;

                    // Atender al cliente en un Thread separado
                    // Esto permite que el servidor pueda atender múltiples clientes
                    Thread hiloCliente = new Thread(() -> {
                        atenderCliente(socketCliente, numCliente);
                    });
                    hiloCliente.start();

                    numeroCliente++;

                } catch (Exception e) {
                    System.err.println("Error al aceptar conexión: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Error al iniciar servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Atiende a un cliente conectado Se ejecuta en un Thread separado por cada
     * cliente
     *
     * @param socket Socket del cliente
     * @param numeroCliente Número identificador del cliente
     */
    private void atenderCliente(Socket socket, int numeroCliente) {
        String timestamp = FORMATO_TIMESTAMP.format(new Date());

        try (
                // Flujo de ENTRADA: para recibir datos del cliente
                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                ); // Flujo de SALIDA: para enviar datos al cliente
                 PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);) {

            String linea;

            // Leer comandos del cliente hasta que se desconecte
            while ((linea = entrada.readLine()) != null) {

                timestamp = FORMATO_TIMESTAMP.format(new Date());
                System.out.println("  [" + timestamp + "] Cliente #" + numeroCliente + " → "
                        + linea.substring(0, Math.min(50, linea.length())) + "...");

                // Procesar el comando
                String respuesta = procesarComando(linea, numeroCliente);

                // Enviar respuesta al cliente
                salida.println(respuesta);
            }

        } catch (Exception e) {
            System.err.println("Error con Cliente #" + numeroCliente + ": " + e.getMessage());
        } finally {
            try {
                socket.close();
                timestamp = FORMATO_TIMESTAMP.format(new Date());
                System.out.println("  [" + timestamp + "] Cliente #" + numeroCliente + " DESCONECTADO");
                System.out.println();
            } catch (Exception e) {
                // Ignorar errores al cerrar
            }
        }
    }

    /**
     * Procesa un comando recibido del cliente
     *
     * Protocolo: "DATOS:datos_encriptados" Guardar en BD 
     * "CONSULTA:fechaInicio/fechaFin horaInicio/horaFin"  Consultar BD
     *
     * @param comando Comando recibido
     * @param numeroCliente Número del cliente (para logs)
     * @return Respuesta para enviar al cliente
     */
    private String procesarComando(String comando, int numeroCliente) {
        try {

            //COMANDO: DATOS (Guardar en BD)
            if (comando.startsWith("DATOS:")) {

                // Extraer los datos encriptados
                String datosEncriptados = comando.substring("DATOS:".length());

                // Desencriptar los datos
                String datosDesencriptados = Encriptacion.desencriptar(datosEncriptados);

                System.out.println("Datos desencriptados: " + datosDesencriptados);

                // Guardar en la base de datos
                boolean guardado = baseDatos.insertarDatos(datosDesencriptados);

                if (guardado) {
                    System.out.println("Datos guardados en BD");
                    return "OK";
                } else {
                    System.err.println("Error al guardar en BD");
                    return "ERROR";
                }
            } // ** COMANDO: CONSULTA (Leer de BD) **
            else if (comando.startsWith("CONSULTA:")) {

                // Extraer los filtros
                String filtros = comando.substring("CONSULTA:".length());
                String[] partes = filtros.split("\\|");

                // Parsear filtros (pueden ser "null")
                String fechaInicio = partes.length > 0 && !partes[0].equals("null") ? partes[0] : null;
                String fechaFin = partes.length > 1 && !partes[1].equals("null") ? partes[1] : null;
                String horaInicio = partes.length > 2 && !partes[2].equals("null") ? partes[2] : null;
                String horaFin = partes.length > 3 && !partes[3].equals("null") ? partes[3] : null;

                System.out.println("Consultando BD con filtros:");
                System.out.println("Fecha: " + fechaInicio + " a " + fechaFin);
                System.out.println("Hora: " + horaInicio + " a " + horaFin);

                // Consultar la base de datos
                String datosJSON = baseDatos.consultarDatos(fechaInicio, fechaFin, horaInicio, horaFin);

                if (datosJSON != null && !datosJSON.equals("[]")) {
                    // Encriptar los datos antes de enviar
                    String datosEncriptados = Encriptacion.encriptar(datosJSON);
                    System.out.println("Datos consultados y encriptados");
                    return "DATOS_ENCRIPTADOS:" + datosEncriptados;
                } else {
                    System.out.println("No se encontraron datos");
                    return "SIN_DATOS";
                }
            } //COMANDO DESCONOCIDO
            else {
                System.err.println("Comando desconocido: " + comando);
                return "ERROR:COMANDO_DESCONOCIDO";
            }

        } catch (Exception e) {
            System.err.println("Error al procesar comando: " + e.getMessage());
            e.printStackTrace();
            return "ERROR:" + e.getMessage();
        }
    }

    
     //Método principal Punto de entrada del servidor
     
    public static void main(String[] args) {
        // Crear e iniciar el servidor
        Servidor servidor = new Servidor();
        servidor.iniciar();
    }
}
