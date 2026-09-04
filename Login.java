import java.util.Scanner;
public class Login {
    void main(String[] args) {
        String email;
        String password;
        String emailUser = "Juan";
        String passwordUser = "123455";
        boolean correctEmail;
        int correctLogin;
        Scanner lector = new Scanner(System.in);
        for (int attemps = 1; attemps <= 5; attemps++) {
            System.out.println("este es el intento " + attemps + " de 5");
            email = lector.nextLine();
            password = lector.nextLine();
            correctEmail = (email.toUpperCase()).equals(emailUser.toUpperCase());
            if (true == correctEmail && password.equals(passwordUser)) {
                System.out.println("has accedido correctamente");
                correctLogin = 1;
                break;
            } else {
                System.out.println("la contraseña o el correo son incorrectos");
            }
            if (attemps == 5){
                System.out.println("has acabado los intentos");
            }
        }
    }
}