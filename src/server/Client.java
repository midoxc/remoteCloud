package server;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {
	private static Socket socket;
	
	/*
	 * Fonction isValidIp
	 * Vérification de la validité de l'adresse
	 * retourne un boolean représentant si l'entrée address est une adresse valide, ou non.
	 */
	public static Boolean isValidIp(String address) {
		// string regex, représentant l'ensemble des IP valides.
		String regexNum = "([0-9]|[0-9][0-9]|[0-1][0-9][0-9]|2[0-4][0-9]|25[0-5])";
		String regexIp = "^" + regexNum + "\\." + regexNum + "\\." + regexNum + "\\." + regexNum + "$";
		
		Pattern pattern = Pattern.compile(regexIp, Pattern.CASE_INSENSITIVE);
	    Matcher matcher = pattern.matcher(address);
	    if (matcher.find()) {
	    	return true;
	    }
	    System.out.println(address + " is not a valid ip address.");
	    return false;
	}
	
	/*
	 * Fonction isValidPort
	 * Vérification de la validité du port
	 * retourne un boolean représentant si l'entrée port est un port valide, ou non.
	 */
	public static Boolean isValidPort(String port) {
		int serverPort;
		
		// on test si le port est un nombre et s'il est entre 5000 et 5050.
		try {
			serverPort = Integer.parseInt(port);
		} catch (NumberFormatException e) {
			System.out.println(port + " is not an integer number.");
			return false;
		}
		if (serverPort < 5000 || serverPort > 5050) {
			System.out.println(port + " is not between 5000 and 5050.");
			return false;
		}
		return true;
	}
	
	/*
	 * Fonction isValidCommand
	 * Vérification de la validité de la commande
	 * retourne un boolean représentant si l'entrée port est une commande valide, ou non.
	 */
	public static Boolean isValidCommand(String command) {
		String param[] = command.split(" ");
		
		// string regex, représentant l'ensemble des commandes valides.
		String regexCommand = "^(cd|ls|mkdir|upload|download|exit)$";
		
		
		Pattern pattern = Pattern.compile(regexCommand);
		Matcher matcher = pattern.matcher(param[0]);
		
		// checks if input starts with one of the commands
		if (matcher.find()) {
			// checks number of parameters passed
			if (param.length > 2) {
				System.out.println(param[0] + " was given too many parameters.");
				
				// prints the right error message depending on the command
				if (param[0].equals("mkdir")){
					System.out.println("Try: " + param[0] + " <name of folder to create>");
				}else if (param[0].equals("ls")||param[0].equals("exit")){
					System.out.println("Try: " + param[0]);
				}else {
					System.out.println("Try: " + param[0] + " <target>");
				}
				return false;
			
			}else if (param.length == 2){
				if (param[0].equals("upload")){
					// verification for upload is done by checking if the file is valid
					if(isValidFile(param[1])) {
						return true;
					}
					System.out.println("Cannot find " + param[1] + " in the current repository.");
					return false;
				}else if (param[0].equals("download")){
					// verification for download is done by checking if the file already exists
					if(isValidFile(param[1])) {
						System.out.println(param[1] + " already exists in the current repository.");
						System.out.println("To overwrite the file, delete it first.");
						return false;
					}
					return true;
				}else if (param[0].equals("ls")||param[0].equals("exit")){ 
					// these 2 commands dont take any parameters, so return false
					System.out.println(param[0] + " was given too many parameters.");
					System.out.println("Try: " + param[0]);
					return false;
				}
				// the other commands left are cd, mkdir and download, which are verified by the server
				return true;
			}else if (param.length == 1){
				// if theres only the command, if its not exit or ls, we return false
				if (param[0].equals("ls")||param[0].equals("exit")){
					return true;
				}else if (param[0].equals("mkdir")){
					System.out.println("Try: " + param[0] + " <name of folder to create>");
				}else{
					System.out.println("Try: " + param[0] + " <target>");
				}
				return false;
			}
			
		}
		System.out.println(command + " is not a valid command.");
	    return false;
	}
	
	/*
	 * Fonction isValidFile
	 * Vérification de la validité d'un fichier
	 * retourne un boolean représentant si le fichier existe dans le répertoire.
	 */
	public static Boolean isValidFile(String filename) {
		File f = new File(filename);
		return f.isFile();
	}
	
	/*
	 * Application client
	 */
	public static void main(String[] args) throws Exception {

		// Création d'un scanner pour les entrée �  la console
		Scanner scanner = new Scanner(System.in);

		// Adresse et port du serveur
		String serverAddress;
		String portString;
		int port;

		// On demande une adresse valide
		do {
			System.out.println("Enter a valid Ip address : ");
			serverAddress = scanner.nextLine();
		} while (!isValidIp(serverAddress));

		// On demande un port valide
		do {
			System.out.println("Enter a valid port number : ");
			portString = scanner.nextLine();
		} while (!isValidPort(portString));
		port = Integer.parseInt(portString);

		// Création d'une nouvelle connexion avec le serveur
		socket = new Socket(serverAddress, port);

		System.out.format("The server is running on %s:%d%n", serverAddress, port);

		// Création d'un canal entrant pour recevoir les messages envoyés par le serveur
		DataInputStream in = new DataInputStream(socket.getInputStream());

		// Attente de la réception d'un message envoyé par le serveur sur le canal
		String helloMessageFromServer = in.readUTF();
		System.out.println(helloMessageFromServer);

		// Création d'un canal sortant pour envoyer des messages au client
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());

		while (true) {
			// attente d'une entrée de l'utilisateur
			System.out.printf(">>>");
			String messageForServer = scanner.nextLine();
			while(!isValidCommand(messageForServer)){
				System.out.printf(">>>");
				messageForServer = scanner.nextLine();
			}
			
			// envoi du message au serveur
			out.writeUTF(messageForServer);
			
			// Attente de la réception d'un message envoyé par le serveur sur le canal
			String returnMessageFromServer = in.readUTF();

			// commande de sortie
			if(messageForServer.equals("exit")) {
				System.out.println("Vous avez été déconnecté avec succès");
				break;
			}
			
			// autres commandes
			String command[] = messageForServer.split(" ");
			switch (command[0]) {
				case "cd": {
					// réception du retour de la commande ls sur le serveur
					returnMessageFromServer = in.readUTF();
					System.out.println(returnMessageFromServer);
					break;
				}
				case "ls": {
					// réception du retour de la commande ls sur le serveur
					int count = in.readInt();
					for(; count > 0; count--) {
						returnMessageFromServer = in.readUTF();
						System.out.println(returnMessageFromServer);
					}
					break;
				}
				case "mkdir": {
					// réception du retour de la commande mkdir sur le serveur
					returnMessageFromServer = in.readUTF();
					System.out.println(returnMessageFromServer);
					break;
				}
				case "upload": {
					
					returnMessageFromServer = in.readUTF();
					if(returnMessageFromServer.equals("Error:")) {
						returnMessageFromServer = in.readUTF();
						System.out.println(returnMessageFromServer);
						break;
					}
					System.out.println(returnMessageFromServer);
					
					// isoler le nom du fichier qu'on veut upload
					String nomDuFichier = messageForServer.replaceFirst("upload ", "");
					File file = new File(nomDuFichier);
					
					FileInputStream stream = new FileInputStream(file);
					
					// buffer pour les donnees
					byte[] data = new byte[4096];
					int count;
					
					// tant que on a des donnees a lire, on envoi des messages
					while ((count = stream.read(data)) != -1) {
						out.writeInt(count); // envoi le nombre de byte � envoyer
						out.write(data, 0, count); // envoi les bits de data
					}
					
					// nombre pour dire au client que le download est termin�
					out.writeInt(-1); 
					
					stream.close();
					
					returnMessageFromServer = in.readUTF();
					System.out.println(returnMessageFromServer);
					break;
				}
				case "download": {
					// on trouve le nom du fichier
					String nomDuFichier = messageForServer.replaceFirst("download ", "");
					
					// on prend le ouput, qui peut etre soit download ou un message derreur
					returnMessageFromServer = in.readUTF();
					System.out.println(returnMessageFromServer);
					
					// si c'est download, on commence le traitement
					if(returnMessageFromServer.equals("Downloading ...")) {
						
						// on cr�e le fichier dans lequel on veut �crire
						File file = new File(nomDuFichier);
						file.createNewFile();
						
						// on cr�e un stream d'�criture dans le fichier
						FileOutputStream stream = new FileOutputStream(file);
						
						byte[] data = new byte[4096];
						int count;
						
						// on lit le nombre de bit � recevoir et on �crit ces bits dans le fichier
						while ((count = in.readInt()) != -1) {
							in.read(data, 0, count); // bytes de data
							stream.write(data, 0, count); // �criture des bytes dans le fichier
							stream.flush(); // vide le stream dans le fichier
						}
						stream.close();
						
						// on intercepte le message de fin 
						returnMessageFromServer = in.readUTF();
						System.out.println(returnMessageFromServer);
					}
					break;
				}
			}
		}

		// fermeture de la connexion avec le serveur et du scanner
		scanner.close();
		socket.close();
		System.out.println("Connection with server closed.");
	}
}