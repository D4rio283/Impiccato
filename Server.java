package impiccato;


import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

	 //costanti del server (static perchè permette di condividee l'attributo, final purchè non vengono modificate)
     static final int PORTA= 12345;
     static final int NUM_GIOCATORI = 3;
     static final int VITE_INIZIALI= 6;
     
    //array parole da indovinare
     static final String[] PAROLE = {
    		    "cane", "gatto", "casa", "sole", "mare",
    		    "luna", "fiore", "albero", "strada", "porta",
    		    "finestra", "tavolo", "sedia", "pane", "latte",
    		    "acqua", "fuoco", "vento", "neve", "pioggia",
    		    "campo", "collina", "montagna", "fiume", "lago",
    		    "barca", "treno", "auto", "bici", "aereo",
    		    "penna", "libro", "quaderno", "zaino", "scuola",
    		    "amico", "famiglia", "gioco", "palla", "sogno",
    		    "tempo", "giorno", "notte", "estate", "inverno",
    		    "primavera", "autunno", "strada", "piazza", "giardino"
    		};

    
    //attributi
    private ClientHandler[] handlers;        // Thread che gestiscono giocatori
    private int[] vite;            // Vite di ogni giocatore
    private boolean[] attivo;          // Se il giocatore e' ancora in gioco
    private String parola;          // La parola da indovinare
    private char[] parolaNascosta;  // La parola con i trattini
    private List<Character> lettereSbagliate;	//lista dinamica che contiene lettere sbagliate
    private int[] ordineTurni;

    
    //costruttore
    public Server() {
        handlers= new ClientHandler[NUM_GIOCATORI];
        vite= new int[NUM_GIOCATORI];
        attivo= new boolean[NUM_GIOCATORI];
        parola= "";
        lettereSbagliate= new ArrayList<>();
    }

    
    //avviaPartita() contiene la logica del gioco
    public void avviaPartita() {

        ServerSocket serverSocket= null;

        try {
        	
        	//mette il server in ascolto sulla porta
            serverSocket = new ServerSocket(PORTA);
            
        } catch (IOException e) {
        	
        	//stampa l'errore accaduto
            System.out.println("Errore avvio server: " + e.getMessage());
            return;
        }

        System.out.println("Server avviato. In attesa di " + NUM_GIOCATORI + " giocatori...");

        //accettazione connessioni e avvio thread
        for (int i= 0; i < NUM_GIOCATORI; i++) {
            try {
            	
            	//si aspetta che il giocatore si connetta (istruzione bloccante)
                Socket socket = serverSocket.accept();
                
                //stampo il num del giocatore e il suo ip
                System.out.println("Giocatore " + (i + 1) + " connesso da: " + socket.getInetAddress());
                
                //creo oggetto clientHandler passandogli socket (con quel client) e il num del giocatore da gestire
                handlers[i] = new ClientHandler(socket, i + 1);
                
                //creo e avvio il thread
                new Thread(handlers[i]).start();
                
                //inizializzo le vite di un client
                vite[i]= VITE_INIZIALI;
                
                //segno  attivo il client
                attivo[i]= true;
                
                //chiamo inviamessaggio sull'oggetto handler passando il risultato da format di MessageFormatter
                //passandogli a sua volta il tipo del messaggio e il num del giocatore in stringa
                handlers[i].inviaMessaggio(MessageFormatter.format("BENVENUTO", String.valueOf(i + 1)));

            } catch (IOException e) {
                System.out.println("Errore connessione giocatore " + (i + 1));
            }
        }
        
        //alla fine del ciclo sono tutti connessi
        System.out.println("Tutti connessi! Inizio partita.");

        //estraggo la parola casuale e la metto nell'attributo parola
        Random random = new Random();
        parola = PAROLE[random.nextInt(PAROLE.length)];
        System.out.println("Parola scelta: " + parola);
        
        //creo array di N celle, N= num lettere parola
        parolaNascosta= new char[parola.length()];
        
        //la prima lettera viene messa nel primo spazio dell'array (l'iniziale deve essere visibile)
        parolaNascosta[0] = parola.charAt(0);
        
        //le altre celle dell'array della parola nascosta vengono censurate
        for(int i = 1; i < parola.length(); i++) {
            parolaNascosta[i] = '_';
        }

        //Algoritmo per mischiare l'ordine con cui giocheranno i client (ordine dei turni)
        ordineTurni = new int[]{0, 1, 2};
        for (int i = 0; i < ordineTurni.length; i++) {
        	int j   = random.nextInt(ordineTurni.length);  
        	int tmp = ordineTurni[i];
        	ordineTurni[i] = ordineTurni[j];
        	ordineTurni[j] = tmp;
        }
        
        
        //a tutti i giocatori viene inviato il messaggio di inizio, formattato dal format e gli viene 
        //mostrata la parola nascosta
        inviaATutti(MessageFormatter.format("INIZIO", costruisciStringaParola()));

        
        boolean partitaFinita= false;

        while (partitaFinita==false) {

            for (int t = 0; t < NUM_GIOCATORI; t++) {
            	
            	//prendo l'id del giocatore a cui sta il turno
                int id = ordineTurni[t];
                
                //controllo se è disattivo e nel caso passa al prossimo giocatore andando alla prossima iterazione del for
                if (handlers[id].isAttivo()==false) {
                	attivo[id] = false;
                	continue;
                }
                
                //se è rimasto un solo giocatore, controlla chi è, manda il messaggio di vittoria e mette che è finita la partita 
                if (giocatoriAttivi() == 1) {
                    int vincitore = trovaUnicoAttivo();
                    inviaATutti(MessageFormatter.format("WIN", String.valueOf(vincitore + 1)));
                    partitaFinita = true;
                    break;
                }

                //informo il giocatore singolo che è il suo turno
                handlers[id].inviaMessaggio(MessageFormatter.format("TURNO", String.valueOf(id + 1)));
                
                //informo gli altri del turno di un altro giocatore (sempre che siano attivi; non mando il messaggio al giocatore di cui è il turno)
                for (int i = 0; i < NUM_GIOCATORI; i++) {
                    if (i != id && attivo[i]) {
                        handlers[i].inviaMessaggio(MessageFormatter.format("ASPETTA", String.valueOf(id + 1)));
                    }
                }
                
               
                //ricezione della risposta del client a cui era il turno
                String msgRicevuto = handlers[id].riceviMessaggio();

                //Controllo disconnessione: se non arriva nulla o se il client è disattivo, metto il flag a disattivo (elimino il client), invio
                //a tutti dell'eliminazione del client e si passa al prossimo giocatore.
                if (msgRicevuto == null || handlers[id].isAttivo()==false) {
                    attivo[id] = false;
                    inviaATutti(MessageFormatter.format("ELIMINATO", String.valueOf(id + 1)));
                    continue;
                }

                //se sono passati più di 20 secondi viene tolta la vita
                if (msgRicevuto.equals("TIMEOUT")) {
                    vite[id]--;
                    if (vite[id] <= 0) {
                        attivo[id] = false;
                        inviaATutti(MessageFormatter.format("ELIMINATO", String.valueOf(id + 1)));
                    } else {
                        handlers[id].inviaMessaggio(MessageFormatter.format("SBAGLIATO", String.valueOf(vite[id])));
                    }
                    continue;
                }
                
                //in campi ci va il messaggio splittato
                String[] campi = MessageParser.parse(msgRicevuto);
                
                
                if (campi == null) {
                    handlers[id].inviaMessaggio(MessageFormatter.format("ERRORE", "input_non_valido"));
                    vite[id]--;
                    if (vite[id] <= 0) {
                        attivo[id] = false;
                        inviaATutti(MessageFormatter.format("ELIMINATO", String.valueOf(id + 1)));
                    }
                    continue;
                }

                //caso in cui il giocatore tenta la parola intera
                if (campi[0].equals("PAROLA_INTERA")) {
                	
                	//assegno a tentativo la parola che arriva dal client (campi contiene lo split del messaggio)
                    String tentativo = campi[1];
                    
                    //se il tentativo del client è uguale alla parola allora finisce la partita
                    if (tentativo.equals(parola)) {
                        inviaATutti(MessageFormatter.format("PAROLA_CORRETTA", String.valueOf(id + 1)));
                        partitaFinita = true;
                        break;
                        
                        //se la parola è sbagliata tolgo la vita e nel caso ha finito le vite lo elimino
                    } else {
                        vite[id]--;
                        System.out.println("Giocatore " + (id+1) + " ha tentato: " + tentativo + " -> SBAGLIATA");
                        if (vite[id] <= 0) {
                            attivo[id] = false;
                            inviaATutti(MessageFormatter.format("ELIMINATO", String.valueOf(id + 1)));
                        } else {
                            handlers[id].inviaMessaggio(MessageFormatter.format("PAROLA_SBAGLIATA", String.valueOf(vite[id])));
                        }
                        continue;
                    }
                }

               
                
                //in lettera ci va la lettera di campi[1] trasformandolo quindi da stringa in char
                char lettera = campi[1].charAt(0);
                
                //controllo che una lettera minuscola facente parte dell'alfabeto (tutto il resto viene scartato)
                if (lettera < 'a' || lettera > 'z') {
                    handlers[id].inviaMessaggio(MessageFormatter.format("ERRORE", "input_non_valido"));
                    vite[id]--;
                    if (vite[id] <= 0) {
                        attivo[id] = false;
                        inviaATutti(MessageFormatter.format("ELIMINATO", String.valueOf(id + 1)));
                    }
                    continue;
                }
                
                
                //controlla che la lettera inserita fosse già stata inserita precedentemente
                if (lettereSbagliate.contains(lettera) || letteraGiaRivelata(lettera)) {
                    handlers[id].inviaMessaggio(MessageFormatter.format("ERRORE", "lettera_gia_usata"));
                    vite[id]--;
                    if (vite[id] <= 0) {
                        attivo[id] = false;
                        inviaATutti(MessageFormatter.format("ELIMINATO", String.valueOf(id + 1)));
                    }
                    continue;
                }

                //controllo se la lettera e' nella parola
                boolean trovata = false;
                for (int i = 0; i < parola.length(); i++) {
                    if (parola.charAt(i)==lettera) {
                        parolaNascosta[i] = lettera;
                        trovata = true;
                    }
                }
                
                //riprendo la parola aggiornata e conosciuta attualmente
                String parolaAggiornata = costruisciStringaParola();
                
                //se la lettera era giusta
                if (trovata==true) {
                    System.out.println("Giocatore " + (id + 1) + " ha indovinato: " + lettera);
                    
                    //comunica a tutti la parola aggiornata (con le lettere nuove)
                    for (int i = 0; i < NUM_GIOCATORI; i++) {
                        if (attivo[i]==true && handlers[i].isAttivo()==true) {   
                            handlers[i].inviaMessaggio(MessageFormatter.format("PAROLA", parolaAggiornata));
                        }
                    }
                    
                    //se la parola è completa invia messaggio vincita e finisce partita
                    if (parolaCompleta()==true) {
                    	inviaATutti(MessageFormatter.format("WIN", String.valueOf(id + 1)));
                        partitaFinita = true;
                        break;
                    }
                    
                    //altrimenti
                	} else {
                		
                	//aggiungo la lettera provata a quelle sbagliate
                    lettereSbagliate.add(lettera);
                    vite[id]--;
                    System.out.println("Giocatore " + (id + 1) + " sbagliato: " + lettera + ". Vite: " + vite[id]);

                    handlers[id].inviaMessaggio(MessageFormatter.format("SBAGLIATO", String.valueOf(vite[id])));
                    inviaATutti(MessageFormatter.format("LETTERE_SBAGLIATE", lettereSbagliateToString()));

                    if (vite[id] <= 0) {
                        attivo[id] = false;
                        inviaATutti(MessageFormatter.format("ELIMINATO", String.valueOf(id + 1)));
                    }
                }
                
                //se rimane un solo giocatore vince e la partita finisce
                if (giocatoriAttivi() == 1) {
                    int vincitore = trovaUnicoAttivo();
                    inviaATutti(MessageFormatter.format("WIN", String.valueOf(vincitore + 1)));
                    partitaFinita = true;
                    break;
                }
                
                //se non ci sono più giocatori la partita finisce
                if (giocatoriAttivi() == 0) {
                    inviaATutti(MessageFormatter.format("LOSE", parola));
                    partitaFinita = true;
                    break;
                }
            }
        }

        //chiude la connessione con i client
        System.out.println("Partita terminata.");
        for (int i = 0; i < NUM_GIOCATORI; i++) {
            handlers[i].chiudiConnessione();
        }
        
        //chiude la socket
        try { serverSocket.close(); } 
        catch (IOException e) { }
    }

    
    //invia a tutti i giocatori ATTIVI il messaggio
    private void inviaATutti(String messaggio) {
        for (int i = 0; i < NUM_GIOCATORI; i++) {
            if (attivo[i]==true && handlers[i].isAttivo()==true) {  // controlla entrambi
                handlers[i].inviaMessaggio(messaggio);
            }
        }
    }
    
    
    //metodo per contare quanti giocatori attivi ci sono al momento
    private int giocatoriAttivi() {
        int count = 0;
        for (int i = 0; i < NUM_GIOCATORI; i++) {
            if (attivo[i]) {
            	count++;
            }
        }
        return count;
    }
    
    
    //metodo per trovare l'unico giocatore attivo
    private int trovaUnicoAttivo() {
        for (int i = 0; i < NUM_GIOCATORI; i++) {
            if (attivo[i]) {
            	return i;
            }
        }
        return -1;
    }
    
    //controlla se ci sono spazi vuoti (se la lettera non è stata indovinata)
    private boolean parolaCompleta() {
    	for (int i = 0; i < parolaNascosta.length; i++) {
    	    if (parolaNascosta[i] == '_') {
    	    	return false;
    	    }
    	}
    	return true;
    }
    
    
    //metodo per controllare se una lettera è gia stata indovinata
    private boolean letteraGiaRivelata(char lettera) {
    	for (int i = 0; i < parolaNascosta.length; i++) {
    	    if (parolaNascosta[i] == lettera) {
    	    	return true;
    	    }
    	}
    	return false;
    }
    
    //metodo per avere dentro una variabile la parola attualmente conosciuta ai giocatori
    private String costruisciStringaParola() {
        String risultato = "";
        
        //copio la parola nascosta in risultato, aggiungengdo uno spazio ogni volta tranne che alla fine
        for (int i = 0; i < parolaNascosta.length; i++) {
            risultato += parolaNascosta[i];
            if (i < parolaNascosta.length - 1) {
            	risultato += " ";
            }
        }
        return risultato;
    }
    
    
    //in risultato ci va l'insieme delle lettere sbagliate
    //risultato è una stringa che contiene le lettere che fanno parte anche della lista lettereSbagliate
    private String lettereSbagliateToString() {
        String risultato = "";
        
        //size e get per le liste
        for (int i = 0; i < lettereSbagliate.size(); i++) {
            risultato += lettereSbagliate.get(i);
            if (i < lettereSbagliate.size() - 1) {
            	risultato += ",";
            }
        }
        return risultato;
    }


    public static void main(String[] args) {
        Server server = new Server();   // creo l'oggetto
        server.avviaPartita();          // avvio il gioco
    }
}
