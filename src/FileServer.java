import java.io.*;
import java.net.*;
import java.util.Arrays;

public class FileServer {
    // Porta na qual o servidor vai escutar as conexões
    private static final int PORT = 5050;
    // Diretório onde os arquivos serão armazenados
    private static final String SERVER_DIRECTORY = "server_files";

    public static void main(String[] args) {
        try {
            // Cria o diretório do servidor, se não existir
            File dir = new File(SERVER_DIRECTORY);
            if (!dir.exists()) {
                dir.mkdir();
            }

            // Cria o servidor na porta definida
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Servidor iniciado na porta " + PORT);

            // Fica escutando por conexões indefinidamente
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Cliente conectado: " + socket.getInetAddress());
                // Cria uma thread para lidar com o cliente
                new ClientHandler(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Classe interna para lidar com cada cliente
    private static class ClientHandler extends Thread {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            ) {
                while (true) {
                    // Recebe o comando do cliente
                    String command = dis.readUTF();

                    if (command.equalsIgnoreCase("UPLOAD")) {
                        receiveFile(dis);
                    } else if (command.equalsIgnoreCase("LIST")) {
                        sendFileList(dos);
                    } else if (command.equalsIgnoreCase("DOWNLOAD")) {
                        sendFile(dis, dos);
                    } else if (command.equalsIgnoreCase("EXIT")) {
                        System.out.println("Cliente desconectado.");
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Erro com cliente: " + e.getMessage());
            }
        }

        // Método para receber arquivo do cliente
        private void receiveFile(DataInputStream dis) throws IOException {
            String fileName = dis.readUTF();
            long fileSize = dis.readLong();

            File file = new File(SERVER_DIRECTORY, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int read;
                long totalRead = 0;

                while (totalRead < fileSize && (read = dis.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                    totalRead += read;
                }
            }
            System.out.println("Arquivo recebido: " + fileName);
        }

        // Método para enviar lista de arquivos
        private void sendFileList(DataOutputStream dos) throws IOException {
            File dir = new File(SERVER_DIRECTORY);
            String[] files = dir.list();
            if (files == null || files.length == 0) {
                dos.writeInt(0);
            } else {
                dos.writeInt(files.length);
                for (String file : files) {
                    dos.writeUTF(file);
                }
            }
        }

        // Método para enviar arquivo solicitado
        private void sendFile(DataInputStream dis, DataOutputStream dos) throws IOException {
            String fileName = dis.readUTF();
            File file = new File(SERVER_DIRECTORY, fileName);

            if (!file.exists()) {
                dos.writeBoolean(false);
                return;
            }

            dos.writeBoolean(true);
            dos.writeLong(file.length());

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, read);
                }
            }
            System.out.println("Arquivo enviado: " + fileName);
        }
    }
}
