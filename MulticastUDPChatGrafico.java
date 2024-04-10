import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class MulticastUDPChatGrafico{
    // Nombre del cliente
    static String nombre;
    static InetAddress grupo;
    static MulticastSocket socket;
    static int puerto;
    static Thread hilo;
    public static JTextArea txtHistorial;
    
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            NombreFrame ventanaNombre = new NombreFrame();
            ventanaNombre.setVisible(true);
        });
    }

    @SuppressWarnings("deprecation")
    public static void unirseGrupo() {
        try {
            puerto = 8080;
            grupo = InetAddress.getByName("224.0.0.0");
            socket = new MulticastSocket(puerto);
            socket.joinGroup(grupo);
            
            hilo = new Thread(new HiloLectura(socket, grupo, puerto));
            hilo.start();        
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        }
    }

    public static void enviarMensaje(String mensaje) {
        try {
            byte[] bufer = new byte[1024];
            mensaje = nombre + mensaje;
            bufer = mensaje.getBytes();
            DatagramPacket datagram = new DatagramPacket(bufer, bufer.length, grupo, puerto);
            socket.send(datagram);
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    public static void salir() {
        try {
            enviarMensaje(" ha abandonado el grupo.");

            socket.leaveGroup(grupo);
            socket.close();
            System.exit(0);
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        }
    }

    static class NombreFrame extends Frame {
        JTextField txtNombre;
        JButton btnAceptar;
        JButton btnCancelar;

        public NombreFrame() {
            super("Ingresar nombre"); 
            setLayout(new BorderLayout());

            JPanel pCenter = new JPanel(new FlowLayout());
            add(pCenter, BorderLayout.CENTER);

            JPanel pSouth = new JPanel(new FlowLayout());
		    add(pSouth, BorderLayout.SOUTH);
            
            txtNombre = new JTextField(20);
            pCenter.add(new Label("Ingresa tu nombre"));
            pCenter.add(txtNombre);
            txtNombre.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    nombre = txtNombre.getText();
                    dispose();
                    unirseGrupo();

                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            new MainFrame().setVisible(true);
                        }
                    });
                }
            });

        
            btnAceptar = new JButton("Aceptar");
            btnAceptar.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    nombre = txtNombre.getText();
                    dispose();
                    unirseGrupo();

                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            new MainFrame().setVisible(true);
                        }
                    });
                }
            });
            pSouth.add(btnAceptar);
        
            btnCancelar = new JButton("Cancelar");
            btnCancelar.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dispose();
                    System.exit(0);
                }
            });
            pSouth.add(btnCancelar);
        
            setSize(500, 140);
            setVisible(true);
            setResizable(false);
        
            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    dispose();
                    System.exit(0);
                }
            });
        }
    }

    static class MainFrame extends Frame {
        JTextField txtMensaje;
        JButton btnEnviar;

        public MainFrame() {
            setLayout(new BorderLayout());

            JPanel pNorth = new JPanel(new FlowLayout());
		    add(pNorth, BorderLayout.NORTH);

            txtHistorial = new JTextArea(20, 40);
            enviarMensaje(" se ha unido al grupo.");
            txtHistorial.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(txtHistorial);
            pNorth.add(scrollPane);

            JPanel pCenter = new JPanel(new FlowLayout(FlowLayout.LEFT));
            add(pCenter, BorderLayout.CENTER);

            pCenter.add(new Label("Usuario: " + nombre));

            JPanel pSouth = new JPanel(new FlowLayout(FlowLayout.CENTER));
		    add(pSouth, BorderLayout.SOUTH);

            txtMensaje = new JTextField(40);
            pSouth.add(txtMensaje);

            txtMensaje.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String mensaje = ": " + txtMensaje.getText();
                    enviarMensaje(mensaje);
                    txtMensaje.setText("");
                }
            });

            btnEnviar = new JButton("Enviar");
            pSouth.add(btnEnviar);

            btnEnviar.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String mensaje = ": " + txtMensaje.getText();
                    enviarMensaje(mensaje);
                    txtMensaje.setText("");
                }
            });

            setSize(550, 450);
            setVisible(true);
            setResizable(false);
        
            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    dispose();
                    salir();
                }
            });
        }
    }

    // Clase de apoyo para leer los mensajes
    static class HiloLectura implements Runnable {
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

            while (!socket.isClosed()) {
                try {
                    // Leemos los mensajes UDP que lleguen
                    DatagramPacket mensajeEntrada = new DatagramPacket(bufer, bufer.length, grupo, port);
                    socket.receive(mensajeEntrada);

                    linea =  new String(bufer, 0, mensajeEntrada.getLength());
                    txtHistorial.setText(txtHistorial.getText() + "\n" + linea);
                } catch (IOException ex) {
                    System.out.println(ex);
                }
            }
        }
    }
}

