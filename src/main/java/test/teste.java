package test;

import Server.MainServer;

public class teste {
	public static void main (String [] args) {
		MainServer server = new MainServer();
		new Thread(server).start();	
	}
}
