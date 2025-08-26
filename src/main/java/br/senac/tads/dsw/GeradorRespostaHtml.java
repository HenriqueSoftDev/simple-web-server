package br.senac.tads.dsw;

public class GeradorRespostaHtml implements GeradorResposta{

	@Override
	public String gerarResposta(String nome, String email) {
		return """
            <!DOCTYPE html>
            <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Resposta HTML</title>
                </head>
                <body>
                    <h1>Dados Recebidos</h1>
                    <p><strong>Nome:</strong> %s</p>
                    <p><strong>Email:</strong> %s</p>
                </body>
            </html>
            """.formatted(nome, email);
	}
}
