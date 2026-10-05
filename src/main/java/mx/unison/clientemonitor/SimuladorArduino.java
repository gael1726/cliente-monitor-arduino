/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mx.unison.clientemonitor;

import java.util.Random;

/**
 *
 * @author gaelm
 */
public class SimuladorArduino extends Thread {
    
    // Interfaz para notificar cuando hay nuevos datos
    // Esto permite que otras clases (como VistaMonitor) reciban los datos
    public interface DatosListener {
        void onNuevosDatos(String datos);
    }
    
    // Variables de control
    private boolean ejecutando;         // Controla si el simulador está activo
    private DatosListener listener;     // Quien recibirá los datos generados
    private Random random;              // Generador de números aleatorios
    
    // Configuración de simulación
    private static final int VALOR_MIN = 0;      // Valor mínimo (igual que Arduino)
    private static final int VALOR_MAX = 1023;   // Valor máximo (igual que Arduino)
    private static final int INTERVALO_MS = 1000; // 1 segundo entre lecturas
    
    // Variables para generar datos con movimiento suave (más realista)
    private int valorX;
    private int valorY;
    private int valorZ;
    private int variacionMaxima = 50; // Cuánto puede cambiar el valor en cada lectura
    
    /**
     * Constructor del simulador
     * 
     * @param listener El objeto que recibirá las notificaciones de nuevos datos
     */
    public SimuladorArduino(DatosListener listener) {
        this.listener = listener;
        this.random = new Random();
        this.ejecutando = false;
        
        // Inicializar con valores aleatorios en el rango medio
        this.valorX = 500 + random.nextInt(100);
        this.valorY = 500 + random.nextInt(100);
        this.valorZ = 500 + random.nextInt(100);
    }
    
    /**
     * Inicia la simulación
     * Este método arranca el Thread
     */
    public void iniciar() {
        if (!ejecutando) {
            ejecutando = true;
            this.start(); // Inicia el Thread (llama a run())
            System.out.println("Simulador de Arduino iniciado");
        }
    }
    
    /**
     * Detiene la simulación
     */
    public void detener() {
        ejecutando = false;
        System.out.println("Simulador de Arduino detenido");
    }
    
    /**
     * Verifica si el simulador está ejecutándose
     * 
     * @return true si está activo, false si no
     */
    public boolean estaEjecutando() {
        return ejecutando;
    }
    
    /**
     * Método run() del Thread
     * Este método se ejecuta automáticamente cuando se llama a start()
     * Se ejecuta en un hilo separado, no bloquea el programa principal
     */
    @Override
    public void run() {
        System.out.println("Thread del simulador iniciado");
        
        // Bucle infinito mientras ejecutando sea true
        while (ejecutando) {
            try {
                // Generar nuevos valores con variación suave
                valorX = generarNuevoValor(valorX);
                valorY = generarNuevoValor(valorY);
                valorZ = generarNuevoValor(valorZ);
                
                // Formatear datos igual que Arduino: "x:#, y:#, z:#"
                String datos = String.format("x:%d, y:%d, z:%d", valorX, valorY, valorZ);
                
                // Mostrar en consola para debug
                System.out.println("Datos simulados: " + datos);
                
                // Notificar al listener (quien esté escuchando)
                if (listener != null) {
                    listener.onNuevosDatos(datos);
                }
                
                // Esperar 1 segundo antes de generar nuevos datos
                // Thread.sleep() pausa la ejecución del hilo actual
                Thread.sleep(INTERVALO_MS);
                
            } catch (InterruptedException e) {
                // Si el Thread es interrumpido, salir del bucle
                System.out.println("Thread del simulador interrumpido");
                ejecutando = false;
            } catch (Exception e) {
                // Cualquier otro error
                System.err.println("Error en simulador: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("→ Thread del simulador finalizado");
    }
    
    /**
     * Genera un nuevo valor con variación suave a partir del valor anterior
     * Esto hace que los datos se vean más realistas (no saltan bruscamente)
     * 
     * @param valorActual El valor anterior
     * @return El nuevo valor con una pequeña variación
     */
    private int generarNuevoValor(int valorActual) {
        // Generar una variación aleatoria entre -variacionMaxima y +variacionMaxima
        int variacion = random.nextInt(variacionMaxima * 2) - variacionMaxima;
        
        // Aplicar la variación al valor actual
        int nuevoValor = valorActual + variacion;
        
        // Asegurar que el valor esté dentro del rango permitido
        if (nuevoValor < VALOR_MIN) {
            nuevoValor = VALOR_MIN;
        } else if (nuevoValor > VALOR_MAX) {
            nuevoValor = VALOR_MAX;
        }
        
        return nuevoValor;
    }
    
    /**
     * Cambia la variación máxima (qué tan "nerviosos" son los datos)
     * 
     * @param variacion Valor entre 1 y 100
     */
    public void setVariacionMaxima(int variacion) {
        if (variacion >= 1 && variacion <= 100) {
            this.variacionMaxima = variacion;
        }
    }
    
    /**
     * Método de prueba para verificar que el simulador funciona
     */
    public static void main(String[] args) {
        System.out.println("PRUEBA DEL SIMULADOR DE ARDUINO\n");
        
        // Crear un listener para recibir los datos
        DatosListener listener = new DatosListener() {
            @Override
            public void onNuevosDatos(String datos) {
                System.out.println("Datos recibidos: " + datos);
            }
        };
        
        // Crear el simulador
        SimuladorArduino simulador = new SimuladorArduino(listener);
        
        // Iniciar simulación
        simulador.iniciar();
        
        // Dejar que corra por 10 segundos
        try {
            System.out.println("Simulando por 10 segundos\n");
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Detener simulación
        simulador.detener();
        
        System.out.println("\nPRUEBA COMPLETADA");
    }
}