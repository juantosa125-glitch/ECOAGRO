package co.com.ecoagro.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {
    
    private static final String URL = "jdbc:mysql://localhost:3306/ecoagro?serverTimezone=UTC";
    private static final String USER = "admin";
    private static final String PASSWORD = "Juan1251824*";

    public static Connection obtenerConexion() {
        Connection conexion = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");  
            
            conexion = DriverManager.getConnection(URL, USER, PASSWORD);

            System.out.println("Conexión exitosa a MySQL");
            
        } catch (ClassNotFoundException e) {

            System.out.println(
                "Error: Driver JDBC no encontrado. "
                + e.getMessage()
            );

        } catch (SQLException e) {

            System.out.println(
                "Error de base de datos: "
                + e.getMessage()
            );
        }

        return conexion;
    }
}