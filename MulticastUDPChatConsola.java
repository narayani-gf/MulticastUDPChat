import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.Scanner;


public class MulticastUDPChatConsola {
    // Nombre del cliente
    static String nombre;

    // Bandera que indica que hay que dejar de leer mensajes
    static volatile boolean terminado = false;

    @SuppressWarnings("deprecation")

    public static void main(String[] args) {
        try {
            // Establecemos un puerto acordado para multicast
            int puerto = 8080;

            // Establecemos la IP para multicast
            InetAddress grupo = InetAddress.getByName("224.0.0.0");

            // Creamos el socket multicast
            MulticastSocket socket = new MulticastSocket(puerto);

            // Solicitamos el nombre del cliente
            Scanner scanner = new Scanner(System.in);
            System.out.println("Ingresa tu nombre: ");
            nombre = scanner.nextLine();

            // Se une al grupo
            socket.joinGroup(grupo);

            // Tenemos que crear un hilo para que reciba los mensajes de los otros
            Thread hilo = new Thread(new HiloLectura(socket, grupo, puerto));
            hilo.start();

            // Le decimos al cliente que ya no puede enviar mensajes
            System.out.println("Puede comenzar a escribir mensajes al grupo...\n");

            // Esperamos la respuesta no mayor a 1024 bytes
            byte[] bufer = new byte[1024];
            String linea;

            // Se queda a la espera de escritura de este cliente,
            // hasta que escriba "Adios"
            while (true) {
                linea = scanner.nextLine();

                if (linea.equalsIgnoreCase("Adios")) {
                    terminado = true;

                    // Avisamos que nos vamos
                    linea = nombre + ": Ha terminado la conexion.";
                    bufer = linea.getBytes();
                    DatagramPacket mensajeSalida = new DatagramPacket(bufer, bufer.length, grupo, puerto);
                    socket.send(mensajeSalida);

                    // Cerramos las conexiones
                    socket.leaveGroup(grupo);
                    socket.close();
                    break;
                }

                // Enviamos al grupo el mensaje escrito
                linea = nombre + ": " + linea;
                bufer = linea.getBytes();
                DatagramPacket datagram = new DatagramPacket(bufer, bufer.length, grupo, puerto);
                socket.send(datagram);
            }

            scanner.close();
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        }
    }
}

// Clase de apoyo para leer los mensajes
class HiloLectura implements Runnable {
    private MulticastSocket socket;
    private InetAddress grupo;
    private int port;

    HiloLectura(MulticastSocket socket, InetAddress grupo, int port) {
        this.socket = socket;
        this.grupo = grupo;
        this.port = port;
    }

    @Override
    public void run() {
        // Esperamos la respuesta no mayor a 1024 bytes
        byte[] bufer = new byte[1024];
        String linea;

        while (! MulticastUDPChatConsola.terminado) {
            try {
                // Leemos los mensajes UDP que lleguen
                DatagramPacket mensajeEntrada = new DatagramPacket(bufer, bufer.length, grupo, port);
                socket.receive(mensajeEntrada);

                // Convierte el mensaje a cadena
                linea =  new String(bufer, 0, mensajeEntrada.getLength());

                // No imprimimos los mensajes que yo mismo envío al grupo
                if (!linea.startsWith(MulticastUDPChatConsola.nombre))
                    System.out.println(linea);
            } catch (IOException e) {
                System.out.println("Comunicacion y socket cerrados.");
            }
        }
    }

    
}