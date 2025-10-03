import java.io.*;
import java.net.*;
import java.util.Scanner;

public class FileClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5050;

    public static void main(String[] args) {
        try (
                Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                Scanner scanner = new Scanner(System.in);
        ) {
            System.out.println("Conectado ao servidor SiCA");

            while (true) {
                System.out.println("\n===== MENU =====");
                System.out.println("1. Enviar arquivo");
                System.out.println("2. Listar arquivos no servidor");
                System.out.println("3. Baixar arquivo");
                System.out.println("4. Sair");
                System.out.print("Escolha: ");
                int opcao = scanner.nextInt();
                scanner.nextLine(); // limpa buffer

                if (opcao == 1) {
                    System.out.print("Digite o caminho do arquivo: ");
                    String path = scanner.nextLine();
                    sendFile(path, dos);
                } else if (opcao == 2) {
                    listFiles(dis, dos);
                } else if (opcao == 3) {
                    System.out.print("Digite o nome do arquivo: ");
                    String fileName = scanner.nextLine();
                    downloadFile(fileName, dis, dos);
                } else if (opcao == 4) {
                    dos.writeUTF("EXIT");
                    break;
                } else {
                    System.out.println("Opção inválida!");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Envia um arquivo para o servidor
    private static void sendFile(String path, DataOutputStream dos) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("Arquivo não encontrado!");
            return;
        }

        dos.writeUTF("UPLOAD");
        dos.writeUTF(file.getName());
        dos.writeLong(file.length());

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, read);
            }
        }
        System.out.println("Arquivo enviado: " + file.getName());
    }

    // Solicita a lista de arquivos disponíveis no servidor
    private static void listFiles(DataInputStream dis, DataOutputStream dos) throws IOException {
        dos.writeUTF("LIST");
        int count = dis.readInt();

        if (count == 0) {
            System.out.println("Nenhum arquivo no servidor.");
            return;
        }

        System.out.println("Arquivos disponíveis:");
        for (int i = 0; i < count; i++) {
            System.out.println("- " + dis.readUTF());
        }
    }

    // Faz o download de um arquivo do servidor
    private static void downloadFile(String fileName, DataInputStream dis, DataOutputStream dos) throws IOException {
        dos.writeUTF("DOWNLOAD");
        dos.writeUTF(fileName);

        boolean exists = dis.readBoolean();
        if (!exists) {
            System.out.println("Arquivo não encontrado no servidor.");
            return;
        }

        long fileSize = dis.readLong();
        File file = new File("client_" + fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int read;
            long totalRead = 0;

            while (totalRead < fileSize && (read = dis.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
                totalRead += read;
            }
        }

        System.out.println("Arquivo baixado: " + file.getName());
    }
}
