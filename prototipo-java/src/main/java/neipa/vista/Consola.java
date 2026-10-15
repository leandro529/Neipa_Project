package neipa.vista;

import java.io.InputStream;
import java.util.Scanner;

/**
 * Entrada y salida por consola. Centraliza la lectura validada: cada
 * metodo repite la pregunta hasta recibir un dato correcto.
 */
public class Consola {

    private final Scanner teclado;

    public Consola(InputStream entrada) {
        this.teclado = new Scanner(entrada);
    }

    public void mostrar(String texto) {
        System.out.println(texto);
    }

    /** Muestra un texto de varias lineas que ya termina con salto de linea. */
    public void mostrarBloque(String texto) {
        System.out.print(texto);
    }

    public void mostrarTitulo(String titulo) {
        System.out.println();
        System.out.println("=== " + titulo + " ===");
    }

    /** Lee un texto no vacio. */
    public String leerTexto(String mensaje) {
        while (true) {
            System.out.print(mensaje + ": ");
            String texto = teclado.nextLine().trim();
            if (!texto.isEmpty()) {
                return texto;
            }
            System.out.println("  El dato es obligatorio.");
        }
    }

    /** Lee un entero entre minimo y maximo (ambos incluidos). */
    public int leerEntero(String mensaje, int minimo, int maximo) {
        while (true) {
            System.out.print(mensaje + ": ");
            String texto = teclado.nextLine().trim();
            try {
                int numero = Integer.parseInt(texto);
                if (numero >= minimo && numero <= maximo) {
                    return numero;
                }
                System.out.println("  Ingrese un numero entre " + minimo + " y " + maximo + ".");
            } catch (NumberFormatException e) {
                System.out.println("  '" + texto + "' no es un numero entero valido.");
            }
        }
    }

    /** Lee un numero decimal mayor que cero (acepta coma o punto). */
    public double leerDecimalPositivo(String mensaje) {
        while (true) {
            System.out.print(mensaje + ": ");
            String texto = teclado.nextLine().trim().replace(',', '.');
            try {
                double numero = Double.parseDouble(texto);
                if (numero > 0) {
                    return numero;
                }
                System.out.println("  Ingrese un numero mayor que cero.");
            } catch (NumberFormatException e) {
                System.out.println("  '" + texto + "' no es un numero valido.");
            }
        }
    }

    /** Pregunta de si o no. */
    public boolean confirmar(String mensaje) {
        while (true) {
            System.out.print(mensaje + " (S/N): ");
            String texto = teclado.nextLine().trim();
            if (texto.equalsIgnoreCase("S")) {
                return true;
            }
            if (texto.equalsIgnoreCase("N")) {
                return false;
            }
            System.out.println("  Responda S o N.");
        }
    }
}
