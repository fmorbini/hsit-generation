package charniak;

import javax.ws.rs.client.ClientBuilder;

public class Client2 {
	
	public static void main(String[] args) {
		String responseEntity = ClientBuilder.newClient()
				//.target("http://localhost:8080").path("charniak/parse").queryParam("sentence", "The circle creeps up on the big triangle because the circle wants that the big triangle does not see the circle.").queryParam("timeout", "20")
				//.target("http://localhost:8080").path("charniak/parse").queryParam("sentence", "The circle creeps up on the big triangle because the circle wants the big triangle to not see the circle.").queryParam("timeout", "20")
				//.target("http://localhost:8080").path("charniak/parse").queryParam("sentence", "The circle creeps up on the big triangle because it wants the big triangle to not see itself.").queryParam("timeout", "20")
				//.target("http://localhost:8080").path("charniak/parse").queryParam("sentence", "The circle creeps up on the big triangle because it does not want to be seen by the big triangle.").queryParam("timeout", "20")
				//.target("http://localhost:8080").path("charniak/parse").queryParam("sentence", "i eat an apple.").queryParam("timeout", "20")
				.target("http://localhost:8080").path("charniak/reset")
				.request().get(String.class);
		
		System.out.println(System.currentTimeMillis()+": "+responseEntity);
	}
}
