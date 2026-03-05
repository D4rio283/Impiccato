package impiccato;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
	
	//ip del server e la porta. sono statici così da essere condivisi tra tutti i client (senza ripetizioni)
	//la porta è registrata ma non assegnata a nessun protocollo, quindi utilizzabile
    static final String IP_SERVER= "25.18.244.222";
    static final int PORTA= 12345;

    
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private int idGiocatore;
    private boolean partitaFinita;

    
    public Client() {
        partitaFinita = false;
    }

    //viene stabilita la connessione con il server
    public void connetti() {
        try {
            socket= new Socket(IP_SERVER, PORTA);
            reader= new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer= new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Connesso al server");
            
            //IOException riguarda problemi di input / output con flusso dati in rete
        } catch (IOException e) {
            System.out.println("Errore di connessione: " + e.getMessage());
        }
    }

    //invia una lettera al server nel formato LETTERA|a
    public void inviaLettera(char c) {
        writer.println("LETTERA|" +c);
    }

    // Invia una parola intera al server nel formato PAROLA_INTERA|parola
    public void inviaParola(String parola) {
        writer.println("PAROLA_INTERA|" +parola);
    }

    // Ciclo principale: legge i messaggi dal server e li gestisce
    public void avviaGioco() {
        Scanner scanner = new Scanner(System.in);

        try {
            String messaggioRicevuto;

            while (partitaFinita==false && (messaggioRicevuto=reader.readLine())!=null) {
                gestisciMessaggio(messaggioRicevuto, scanner);
            }

        } catch (IOException e) {
            if (partitaFinita==false) {
                System.out.println("Connessione al server persa.");
            }
        }

        chiudi();
    }

    // Interpreta ogni messaggio ricevuto dal server e reagisce
    private void gestisciMessaggio(String messaggio, Scanner scanner) {

        // Divido il messaggio in TIPO e contenuto usando "|"
        String[] parti   = messaggio.split("\\|", 2);
        String tipo    = parti[0];
        
        //gestisco il caso in cui il protocollo non viene rispettato (esempio TIMEOUT)
        String corpo;
        if (parti.length > 1) {
            corpo = parti[1];
        } else {
            corpo = "";
        }

        switch(tipo){

            case "BENVENUTO":
            	
            	//in idGiocatore ci va il corpo del BENVENUTO (da stringa a intero)
                idGiocatore = Integer.parseInt(corpo);
                System.out.println("========================================");
                System.out.println("  Benvenuto! Sei il Giocatore " + idGiocatore+ ", attendi gli altri giocatori...");
                System.out.println("========================================");
                break;

            case "INIZIO":
                System.out.println("\n========================================");
                System.out.println("  PARTITA INIZIATA");
                System.out.println("  Parola: " + corpo);
                System.out.println("========================================\n");
                break;

            case "TURNO":
                System.out.println("\n--- E' IL TUO TURNO ---");
                System.out.println("Scrivi una lettera oppure la parola intera: ");
                
                //trim rimuove spazi all'inizio e fine della parola/lettera; e toLowerCase porta tutto in minuscolo
                String input = scanner.nextLine().trim().toLowerCase();
                
                //controlla se è una lettera e se fa parte dell'alfabeto tramite Character.isLetter (predefinita)
                if (input.length()==1 && Character.isLetter(input.charAt(0))) {
                    //bisogna rimettere charat(0) perché la funzione inviaLettera accetta un char (input è una stringa)
                    inviaLettera(input.charAt(0));
                } else if (input.length() > 1) {
                    // Ha inserito una parola intera
                    inviaParola(input);
                } else {
                    // Input non valido: mando lo stesso qualcosa al server
                    // altrimenti il server rimane bloccato ad aspettare
                    System.out.println("Input non valido. Perdi una vita.");
                    
                    // serve per far scattare la penalità al giocatore in  maniera automatica dato che quello che è estato inserito è sbagliato 
                    writer.println("LETTERA|?");
                }
                break;

            case "ASPETTA":
                System.out.println("Sta giocando il Giocatore " + corpo + ". Aspetta il tuo turno...");
                break;

            case "CORRETTO":
                System.out.println("Lettera CORRETTA!");
                System.out.println("Parola aggiornata: " + corpo);
                break;

            case "SBAGLIATO":
                System.out.println("Lettera SBAGLIATA! Vite rimaste: " + corpo);
                break;

            case "PAROLA":
                // Aggiornamento parola per i giocatori non di turno
                System.out.println("Parola aggiornata: " + corpo);
                break;

            case "LETTERE_SBAGLIATE":
                System.out.println("Lettere gia' usate: " + corpo);
                break;

            case "ELIMINATO":
                if (corpo.equals(String.valueOf(idGiocatore))) {
                    System.out.println("\nSei stato ELIMINATO! Hai esaurito le vite.");
                } else {
                    System.out.println("Il Giocatore " + corpo + " e' stato eliminato!");
                }
                break;

            case "PAROLA_CORRETTA":
                partitaFinita = true;
                if (corpo.equals(String.valueOf(idGiocatore))) {
                    
                    System.out.println("║ HAI INDOVINATO LA PAROLA! ║");
                } else {
                    System.out.println("\nIl Giocatore " + corpo + " ha indovinato la parola!");
                }
                break;

            case "PAROLA_SBAGLIATA":
                System.out.println("Parola SBAGLIATA! Vite rimaste: " + corpo);
                break;

            case "WIN":
                partitaFinita = true;
                if (corpo.equals(String.valueOf(idGiocatore))) {
                    System.out.println("║ HAI VINTO! Complimenti ║");
                } else {
                    System.out.println("\nHa vinto il Giocatore " + corpo + ". Meglio la prossima volta!");
                }
                break;

            case "LOSE":
                partitaFinita = true;
                System.out.println("║ PARTITA PERSA! ║");
                System.out.println("La parola era: " + corpo);
                break;

            case "ERRORE":
                System.out.println("Errore: " + corpo);
                break;
        }
    }


    // Chiude tutti i flussi e il socket
    public void chiudi() {
        try {
            if (reader != null) {
            	reader.close();
            }
            if (writer != null) {
            	writer.close();
            }
            if (socket != null && !socket.isClosed()) {
            	socket.close();
            }
            System.out.println("Connessione chiusa. Arrivederci!");
        } catch (IOException e) {
            System.out.println("Errore nella chiusura: " + e.getMessage());
        }
    }


    // Il main crea l'oggetto Client e avvia il gioco
    public static void main(String[] args) {
        Client client = new Client();
        client.connetti();

        if (client.socket != null && client.socket.isConnected()==true) {
            client.avviaGioco();
        }
    }
}
