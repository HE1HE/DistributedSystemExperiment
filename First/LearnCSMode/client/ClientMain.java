package client;

public class ClientMain {
	
	public static void main(String[] args) {
		ClientThread clientThread = new ClientThread("127.0.0.1", 999);
		clientThread.setDeadTime(15000);
		clientThread.start();
	}
}
