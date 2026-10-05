/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mx.unison.clientemonitor.servidor;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author gaelm
 */
public class BaseDatos {
    // Nombre de la base de datos

    private static final String NOMBRE_BD = "monitorBD.db";

    // URL de conexión JDBC para SQLite
    // jdbc:sqlite: indica que es SQLite
    // El archivo se crea en la carpeta del proyecto
    private static final String URL_CONEXION = "jdbc:sqlite:" + NOMBRE_BD;

    // Formatos de fecha y hora
    private static final SimpleDateFormat FORMATO_FECHA = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat FORMATO_HORA = new SimpleDateFormat("HH:mm:ss");

    /**
     * Constructor - Inicializa la base de datos Crea la tabla si no existe
     */
    public BaseDatos() {
        crearTabla();
    }

    /**
     * Obtiene una conexión a la base de datos
     *
     * @return Connection a la base de datos
     * @throws SQLException Si hay error de conexión
     */
    private Connection obtenerConexion() throws SQLException {
        // DriverManager.getConnection() crea la conexión
        // Si el archivo no existe, SQLite lo crea automáticamente
        return DriverManager.getConnection(URL_CONEXION);
    }

    /**
     * Crea la tabla datos_sensor si no existe
     *
     * Estructura según las instrucciones: - id: INTEGER PRIMARY KEY
     * AUTOINCREMENT NOT NULL - x: INTEGER NOT NULL - y: INTEGER NOT NULL - z:
     * INTEGER NOT NULL - fecha_de_captura: TEXT NOT NULL - hora_de_captura:
     * TEXT NOT NULL
     */
    private void crearTabla() {
        // SQL para crear la tabla
        String sql = "CREATE TABLE IF NOT EXISTS datos_sensor ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "x INTEGER NOT NULL, "
                + "y INTEGER NOT NULL, "
                + "z INTEGER NOT NULL, "
                + "fecha_de_captura TEXT NOT NULL, "
                + "hora_de_captura TEXT NOT NULL"
                + ")";

        // try-with-resources: cierra automáticamente la conexión
        try (Connection conn = obtenerConexion(); Statement stmt = conn.createStatement()) {

            // Ejecutar la creación de la tabla
            stmt.execute(sql);
            System.out.println("Tabla 'datos_sensor' verificada/creada");

        } catch (SQLException e) {
            System.err.println("Error al crear tabla: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Inserta un registro de datos del sensor
     *
     * @param datos String con formato "x:#, y:#, z:#"
     * @return true si se insertó correctamente, false si hubo error
     */
    public boolean insertarDatos(String datos) {
        try {
            // Parsear los datos (formato: "x:100, y:200, z:300")
            String[] partes = datos.split(",");
            int x = Integer.parseInt(partes[0].split(":")[1].trim());
            int y = Integer.parseInt(partes[1].split(":")[1].trim());
            int z = Integer.parseInt(partes[2].split(":")[1].trim());

            // Obtener fecha y hora actual
            Date ahora = new Date();
            String fecha = FORMATO_FECHA.format(ahora);
            String hora = FORMATO_HORA.format(ahora);

            // SQL con parámetros (?) para evitar SQL Injection
            String sql = "INSERT INTO datos_sensor (x, y, z, fecha_de_captura, hora_de_captura) "
                    + "VALUES (?, ?, ?, ?, ?)";

            try (Connection conn = obtenerConexion(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

                // Asignar valores a los parámetros
                pstmt.setInt(1, x);      // Primer ? = x
                pstmt.setInt(2, y);      // Segundo ? = y
                pstmt.setInt(3, z);      // Tercer ? = z
                pstmt.setString(4, fecha); // Cuarto ? = fecha
                pstmt.setString(5, hora);  // Quinto ? = hora

                // Ejecutar la inserción
                int filasAfectadas = pstmt.executeUpdate();

                if (filasAfectadas > 0) {
                    System.out.println("Datos guardados: " + datos + " [" + fecha + " " + hora + "]");
                    return true;
                }

            }

        } catch (Exception e) {
            System.err.println("Error al insertar datos: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Consulta datos con filtros opcionales
     *
     * @param fechaInicio Fecha de inicio (formato: yyyy-MM-dd) o null
     * @param fechaFin Fecha de fin (formato: yyyy-MM-dd) o null
     * @param horaInicio Hora de inicio (formato: HH:mm:ss) o null
     * @param horaFin Hora de fin (formato: HH:mm:ss) o null
     * @return String con los datos en formato JSON-like
     */
    public String consultarDatos(String fechaInicio, String fechaFin,
            String horaInicio, String horaFin) {

        StringBuilder resultado = new StringBuilder();
        resultado.append("["); // Inicio de array

        try {
            // Construir la consulta SQL dinámicamente según los filtros
            StringBuilder sql = new StringBuilder("SELECT * FROM datos_sensor WHERE 1=1");

            // Lista para los parámetros
            List<String> parametros = new ArrayList<>();

            // Agregar filtros si existen
            if (fechaInicio != null && !fechaInicio.equals("null")) {
                sql.append(" AND fecha_de_captura >= ?");
                parametros.add(fechaInicio);
            }

            if (fechaFin != null && !fechaFin.equals("null")) {
                sql.append(" AND fecha_de_captura <= ?");
                parametros.add(fechaFin);
            }

            if (horaInicio != null && !horaInicio.equals("null")) {
                sql.append(" AND hora_de_captura >= ?");
                parametros.add(horaInicio);
            }

            if (horaFin != null && !horaFin.equals("null")) {
                sql.append(" AND hora_de_captura <= ?");
                parametros.add(horaFin);
            }

            // Ordenar por ID (más reciente primero)
            sql.append(" ORDER BY id DESC");

            System.out.println("Ejecutando consulta: " + sql);

            try (Connection conn = obtenerConexion(); PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

                // Asignar parámetros
                for (int i = 0; i < parametros.size(); i++) {
                    pstmt.setString(i + 1, parametros.get(i));
                }

                // Ejecutar consulta
                ResultSet rs = pstmt.executeQuery();

                boolean primero = true;
                int contador = 0;

                // Procesar resultados
                while (rs.next()) {
                    if (!primero) {
                        resultado.append(",");
                    }
                    primero = false;

                    // Formato: {id:1,x:100,y:200,z:300,fecha:"2024-01-01",hora:"12:00:00"}
                    resultado.append("{")
                            .append("\"id\":").append(rs.getInt("id")).append(",")
                            .append("\"x\":").append(rs.getInt("x")).append(",")
                            .append("\"y\":").append(rs.getInt("y")).append(",")
                            .append("\"z\":").append(rs.getInt("z")).append(",")
                            .append("\"fecha\":\"").append(rs.getString("fecha_de_captura")).append("\",")
                            .append("\"hora\":\"").append(rs.getString("hora_de_captura")).append("\"")
                            .append("}");

                    contador++;
                }

                System.out.println("Consulta completada: " + contador + " registros encontrados");
            }

        } catch (SQLException e) {
            System.err.println("Error en consulta: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        resultado.append("]"); // Fin de array

        return resultado.toString();
    }

    /**
     * Consulta TODOS los datos sin filtros
     *
     * @return String con todos los datos
     */
    public String consultarTodosDatos() {
        return consultarDatos(null, null, null, null);
    }

    /**
     * Cuenta el total de registros en la base de datos
     *
     * @return Número total de registros
     */
    public int contarRegistros() {
        String sql = "SELECT COUNT(*) as total FROM datos_sensor";

        try (Connection conn = obtenerConexion(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (SQLException e) {
            System.err.println("Error al contar registros: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Elimina todos los registros (para pruebas)
     *
     * @return true si se eliminaron correctamente
     */
    public boolean limpiarTabla() {
        String sql = "DELETE FROM datos_sensor";

        try (Connection conn = obtenerConexion(); Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(sql);
            System.out.println("Tabla limpiada");
            return true;

        } catch (SQLException e) {
            System.err.println("Error al limpiar tabla: " + e.getMessage());
            return false;
        }
    }

    /**
     * Método de prueba
     */
    public static void main(String[] args) {
        System.out.println("PRUEBA DE BASE DE DATOS\n");

        BaseDatos bd = new BaseDatos();

        // Insertar datos de prueba
        System.out.println("Insertando datos");
        bd.insertarDatos("x:100, y:200, z:300");
        bd.insertarDatos("x:150, y:250, z:350");
        bd.insertarDatos("x:200, y:300, z:400");

        // Contar registros
        System.out.println("\nTotal de registros: " + bd.contarRegistros());

        // Consultar todos los datos
        System.out.println("\nConsultando todos los datos");
        String datos = bd.consultarTodosDatos();
        System.out.println(datos);

        System.out.println("\nPRUEBA COMPLETADA");
    }
}
