package br.senac.tads.dsw;

public class GeradorRespostaJson implements GeradorResposta {

	@Override
	public String gerarResposta(String nome, String email) {
		String json = """
            {
                "nome": "%s",
                "email": "%s"
            }
            """.formatted(nome, email);
		return json;
	}
}
