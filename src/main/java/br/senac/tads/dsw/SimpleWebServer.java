package br.senac.tads.dsw;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleWebServer {

	private final int port;
	private static final int THREAD_POOL_SIZE = 10;

	public SimpleWebServer(int port) {
		this.port = port;
	}

	public void start() throws IOException {
		ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			System.out.println("Server started na porta " + port);
			while (true) {
				Socket clientSocket = serverSocket.accept();
				threadPool.execute(() -> {
					try {
						handleClient(clientSocket);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
			}
		}
	}

	private void handleClient(Socket clientSocket) throws IOException {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			 BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

			String requestLine = "";
			StringBuilder requestHeaders = new StringBuilder();
			boolean collectingRequestLine = true;

			int contentLength = 0;
			String requestInputLine;
			while ((requestInputLine = in.readLine()) != null) {
				if (collectingRequestLine) {
					requestLine = requestInputLine;
					collectingRequestLine = false;
					continue;
				}
				if (requestInputLine.isEmpty()) {
					break;
				}
				requestHeaders.append(requestInputLine).append("\r\n");
				if (requestInputLine.toLowerCase().startsWith("content-length:")) {
					contentLength = Integer.parseInt(requestInputLine.split(":")[1].trim());
				}
			}
			String header = requestHeaders.toString();

			String body = "";
			if (contentLength > 0) {
				char[] bodyChars = new char[contentLength];
				in.read(bodyChars, 0, contentLength);
				body = new String(bodyChars);
			}

			// Extrai o caminho da URL (não usado para decidir tipo de resposta)
			String path = "/";
			if (!requestLine.isEmpty()) {
				String[] parts = requestLine.split(" ");
				if (parts.length >= 2) {
					path = parts[1];
				}
			}

			// Extrai Content-Type do header (se existir)
			String contentTypeHeader = "";
			for (String line : header.split("\r\n")) {
				if (line.toLowerCase().startsWith("content-type:")) {
					contentTypeHeader = line.split(":")[1].trim().toLowerCase();
					break;
				}
			}

			// Dados padrão
			String nome = "Henrique";
			String email = "henrisilva2003@gmail.com";

			// Se tiver body JSON, tenta extrair nome e email
			if (body != null && body.contains("{")) {
				String[] pares = body.replace("{", "").replace("}", "").replace("\"", "").split(",");
				for (String par : pares) {
					String[] chaveValor = par.split(":");
					if (chaveValor.length == 2) {
						if (chaveValor[0].trim().equalsIgnoreCase("nome")) {
							nome = chaveValor[1].trim();
						} else if (chaveValor[0].trim().equalsIgnoreCase("email")) {
							email = chaveValor[1].trim();
						}
					}
				}
			}

			GeradorResposta gerador;
			String contentType;

			// Decide gerador e content-type baseado no header Content-Type da requisição
			if ("application/json".equals(contentTypeHeader)) {
				gerador = new GeradorRespostaJson();
				contentType = "application/json";
			} else {
				gerador = new GeradorRespostaHtml();
				contentType = "text/html";
			}

			String responseBody = gerador.gerarResposta(nome, email);

			ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

			int length = responseBody.getBytes().length;

			// Envia resposta HTTP
			out.write("HTTP/1.1 200 OK\r\n");
			out.write("Date: " + formatter.format(now) + "\r\n");
			out.write("Server: Custom Server\r\n");
			out.write("Content-Type: " + contentType + "\r\n");
			out.write("Content-Length: " + length + "\r\n");
			out.write("\r\n");
			out.write(responseBody);
			out.flush();

		} catch (IOException ex) {
			System.err.println("Erro ao lidar com cliente: " + ex.getMessage());
		} finally {
			if (clientSocket != null) {
				try {
					clientSocket.close();
				} catch (IOException ex) {
					System.err.println("Erro ao fechar conexão: " + ex.getMessage());
				}
			}
		}
	}

	public static void main(String[] args) throws IOException {
		int port = 8080;
		SimpleWebServer server = new SimpleWebServer(port);
		server.start();
	}
}
