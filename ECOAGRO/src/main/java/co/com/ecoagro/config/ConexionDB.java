package co.com.ecoagro.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {
    
    // Credenciales y ruta de la base de datos MySQL
    private static final String URL = "jdbc:mysql://localhost:3306/ecoagro?serverTimezone=UTC";
    private static final String USER = "admin";
    private static final String PASSWORD = "Juan1824125*";

    /**
     * Método estático para obtener la conexión a la base de datos.
     * Al ser estático, no necesitamos crear un objeto ConexionDB para usarlo.
     */
    public static Connection obtenerConexion() {
        Connection conexion = null;
        
        try {
            // 1. Cargar el Driver de MySQL en memoria
            Class.forName("com.mysql.cj.jdbc.Driver");  
            
            // 2. Establecer la conexión usando las credenciales
            conexion = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Conexión exitosa a MySQL");
            
        } catch (ClassNotFoundException e) {
            // Error si falta la librería (conector de MySQL) en el proyecto
            System.out.println("Error: Driver JDBC no encontrado. " + e.getMessage());

        } catch (SQLException e) {
            // Error si las credenciales son incorrectas o el servidor MySQL está apagado
            System.out.println("Error de base de datos: " + e.getMessage());
        }

        // Retorna la conexión lista para usarse, o null si falló
        return conexion;
    }
}