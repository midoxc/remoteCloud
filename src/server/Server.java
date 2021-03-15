package server;

import java.io.DataOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server {
	private static ServerSocket listener;

	/*
	 * Fonction isValidIp
	 * V√©rification de la validit√© de l'adresse
	 * retourne un boolean repr√©sentant si l'entr√©e address est une adresse valide, ou non.
	 */
	public static Boolean isValidIp(String address) {
		// string regex, repr√©sentant l'ensemble des IP valides.
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
	 * V√©rification de la validit√© du port
	 * retourne un boolean repr√©sentant si l'entr√©e port est un port valide, ou non.
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
	 * Application Serveur
	 */
	public static void main(String[] args) throws Exception {
		// Compteur incremente chaque connexion d'un client au serveur
		int clientNumber = 0;

		// Creation d'un scanner pour les entrees a la console
		Scanner scanner = new Scanner(System.in);

		// Adresse et port du serveur
		String serverAddress;
		String portString;
		int serverPort;

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
		serverPort = Integer.parseInt(portString);
		
		// Fermeture du scanner
		scanner.close();
		
		// CrÔøΩation de la connexion pour communiquer avec les clients
		listener = new ServerSocket();
		listener.setReuseAddress(true);
		InetAddress serverIP = InetAddress.getByName(serverAddress);

		// Association de l'adresse et du port a la connexion
		listener.bind(new InetSocketAddress(serverIP, serverPort));

		System.out.format("The server is running on %s:%d%n", serverAddress, serverPort);

		try {
			/*
			 * ÔøΩ chaque fois qu'un nouveau client se connecte, on exÔøΩcute la fonction Run()
			 * de l'objet ClientHandler.
			 */
			while (true) {
				// Important : la fonction accept() est bloquante : attend qu'un prochain client
				// se connecte
				// Une nouvelle connection : on incrÔøΩnte le compteur clientNumber
				new ClientHandler(listener.accept(), clientNumber++).start();
			}
		} finally {
			// Fermeture de la connexion
			listener.close();
		}
	}

	/*
	 * Une thread qui se charge de traiter la demande de chaque client sur un socket
	 * particulier
	 */
	private static class ClientHandler extends Thread {
		private Socket socket;
		private int clientNumber;

		public ClientHandler(Socket socket, int clientNumber) {
			this.socket = socket;
			this.clientNumber = clientNumber;
			System.out.println("New connection with client#" + clientNumber + " at " + socket);
		}

		/*
		 * Une thread qui se charge d'envoyer au client un message de bienvenue
		 */
		public void run() {
			try {
				// CrÔøΩation d'un canal sortant pour envoyer des messages au client
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());

				// Envoi d'un message au client
				out.writeUTF("Hello from server - you are client#" + clientNumber + ".");

				// CrÔøΩation d'un canal entrant pour recevoir les messages envoyÔøΩs par le client
				DataInputStream in = new DataInputStream(socket.getInputStream());
				String currentDir = System.getProperty("user.dir");
				while (true) {
					// Attente de la rÔøΩception d'un message envoyÔøΩ par le serveur sur le canal et
					// log du message
					String messageFromClient = in.readUTF();
					LocalDateTime date = LocalDateTime.now();
					DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd@HH:mm:ss");
					System.out.println("[" + socket.getInetAddress() + ":" + socket.getPort() + " - "
							+ date.format(dateFormat) + "]: " + messageFromClient);
					

					// Envoi d'un message au client
					out.writeUTF("You wrote " + messageFromClient + ".");

					// commande de sortie
					if (messageFromClient.equals("exit")) {
						break;
					}
					
					// autres commandes
					String command[] = messageFromClient.split(" ");
					
					switch (command[0]) {
						case "cd": {
		                    if(command[1].equals("..")) {
		                        int i;
		                        for(i=currentDir.length() - 1; i>=0; i--) {
		                            if(currentDir.charAt(i) == '/') {
		                                break;
		                            }
		                        }
		                        String tempDir = currentDir.substring(0, i);
		
		                        File file = new File(tempDir);
		
		                        if(file.isDirectory()) {
		                        	currentDir = tempDir;
		                            out.writeUTF("Vous Ítes dans le dossier " + currentDir);
		                        } else {
		                            out.writeUTF("Pas possible de revenir en arriËre");
		                        }
		                    }
		                    else {
		                    	String tempDir = currentDir + '/' + command[1];
		                        File file = new File(tempDir);
		                        if(file.isDirectory()) {
		                        	currentDir = tempDir;
		                            out.writeUTF("Vous Ítes dans le dossier " + currentDir);
		                        } else {
		                            out.writeUTF(tempDir + " n'est pas un dossier valide");
		                        }
		                    }
		                    break;
		                }
						case "ls": {
							// r√©ception du retour de la commande ls sur le serveur
							String[] nomsDeFichiers;
							
							File file = new File(currentDir);
							nomsDeFichiers = file.list();
							
							out.writeInt(nomsDeFichiers.length);
							for(String nomDeFichier: nomsDeFichiers) {
								//System.out.println(pathname);
								String newPath = currentDir + '/' + nomDeFichier;
								File sousFichier = new File(newPath);
								if(sousFichier.isDirectory()) {
									out.writeUTF("[Folder]" + nomDeFichier);
								} else {
									out.writeUTF("[File]" + nomDeFichier);
								}
								
							}
							break;
						}
						case "mkdir": {
                            //isoler le nom du fichier qu'on ve creer
                            String nomDuFichier = messageFromClient.replaceFirst("mkdir ", "");
                      
                            //creation du fichier
                            String fullNomFichier = currentDir + '/' + nomDuFichier;
                            File file = new File(fullNomFichier);
                            
                            //creation du directory
                            boolean isCreated = file.mkdir();
                            if(isCreated){
                               out.writeUTF("Le dossier " + nomDuFichier + " a √©t√© cr√©√©");
                            }else{
                               out.writeUTF("Le dossier " + nomDuFichier + " n'as pas pu √™tre cr√©√©");
                            }
                            
							break;
						}
						case "upload": {
							// isoler le nom du fichier qu'on veut upload
							String nomDuFichier = messageFromClient.replaceFirst("upload ", "");
							
							// on crÈe le fichier dans lequel on veut Ècrire
							String fullNomFichier = currentDir + '/' + nomDuFichier;
							File file = new File(fullNomFichier);
							
							// si le fichier existe, on le dÈtruit et on en crÈe un nouveau
							if(file.isFile()) {
								out.writeUTF("Error:");
								out.writeUTF("The file " + nomDuFichier + " already exists in the directory");
								break;
							}else {
								out.writeUTF("Uploading " + nomDuFichier + " ...");
							}
							file.createNewFile();
							
							// on crÈe un stream d'Ècriture dans le fichier
							FileOutputStream stream = new FileOutputStream(file);
							
							byte[] data = new byte[4096];
							int count;
							
							// on lit le nombre de bit ‡ recevoir et on Ècrit ces bits dans le fichier
							while ((count = in.readInt()) != -1) {
								in.read(data, 0, count); // bytes de data
								stream.write(data, 0, count); // Ècriture des bytes dans le fichier
								stream.flush(); // vide le stream dans le fichier
							}
							stream.close();
							out.writeUTF("Upload done");
							break;
						}
						case "download": {
							// isoler le nom du fichier qu'on veut upload
							String nomDuFichier = messageFromClient.replaceFirst("download ", "");
							
							String fullNomFichier = currentDir + '/' + nomDuFichier;
							File file = new File(fullNomFichier);
							
							// on vÈrifie que le fichier existe
							if (file.isFile()) {
								// creation du stream de lecture
								
								FileInputStream stream = new FileInputStream(file);
								
								// buffer pour les donnees
								byte[] data = new byte[4096];
								int count;
								
								// on Ècrit au client que le download commence
								out.writeUTF("Downloading ...");
								
								// tant que on a des donnees a lire, on envoi des messages
								while ((count = stream.read(data)) != -1) {
									out.writeInt(count); // envoi le nombre de byte ‡ envoyer
									out.write(data, 0, count); // envoi les bits de data
								}
								
								// nombre pour dire au client que le download est terminÈ
								out.writeInt(-1); 
								
								// message de fin au client
								out.writeUTF(nomDuFichier + " downloaded successfully");
								stream.close();
							} else {
								out.writeUTF(nomDuFichier  + " is not a valid file name");
							}
							break;
						}
					}
					//out.writeUTF("end");
                    

				}
			} catch (IOException e) {
				System.out.println("Error handling client# " + clientNumber + ": " + e);
			} finally {
				try {
					// Fermeture de la connexion avec le client
					socket.close();
				} catch (IOException e) {
					System.out.println("Couldn't close a socket, what's going on?");
				}
				System.out.println("Connection with client# " + clientNumber + " closed");
			}
		}
	}
}