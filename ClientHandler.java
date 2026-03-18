package impiccato;


import java.io.*;
import java.net.*;


//La classe ClientHandler per gestire i client


public class ClientHandler implements Runnable {


    private Socket socket;
    private int idGiocatore;
    private BufferedReader in;
    private PrintWriter out;
        
    //questi tre attributi sono volatile perché vengono usati da due thread diversi nello stesso momento: thread run() che li scrive e il thread 
    //server che li legge

    private String  ultimoMessaggio= null;
    private boolean messaggioDisponibile = false;
    private boolean attivo= true;

    //costruttore
    public ClientHandler(Socket socket, int idGiocatore) {
        this.socket= socket;
        this.idGiocatore= idGiocatore;

        try {
        	
        	//nell'attributo in ci va ciò che arriva dalla socket (da byte grezzi -> trasforma byte leggibili -> legge tutta la riga)
            in= new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            //mette nella variabile out cosa mandare sulla socket attraverso funzione PrintWriter(con autoflush attivo ogni volta che viene 
            //scritto qualcosa viene mandato subito)
            out= new PrintWriter(socket.getOutputStream(), true);
            
        } catch (IOException e) {
            System.out.println("Errore apertura flussi giocatore " + idGiocatore);
        }
    }

 
    @Override
    public void run() {
        try {
            while(attivo==true){
            	
            	//si aspetta che il client mandi qualcosa (bloccante)
                String msg = in.readLine();
                
                //se arriva null, vuol dire che il client si è disconnesso
                if (msg == null) {
                    System.out.println("Giocatore " + idGiocatore + " disconnesso.");
                    attivo= false;
                    break;
                }
                
                //blocco sincronizzato (this si riferisce all'oggetto clienthandler corrente)
                //si salva il messaggio arrivato e si sveglia il thread server che era in wait, in attesa
                synchronized (this) {
                    ultimoMessaggio= msg;
                    messaggioDisponibile= true;
                    notify();
                }
            }
            
            //se viene chiamata l'eccezione (crash client) viene disconnesso il client e svegliato il thread server (sennò rimarrebbe in attesa infinita)
        } catch (IOException e) {
            attivo = false;
            System.out.println("Giocatore " + idGiocatore + " disconnesso.");

            synchronized (this) {
                notify();
            }
        }
    }

    
    //metodo per inviare messaggi al client
    public void inviaMessaggio(String messaggio) {
    	
    	//si controlla se non ci sono stati errori nella'pertura del flusso della socket
        if (out!=null) {
            out.println(messaggio);
        }
    }
    
    //metodo per ricevere messaggi dai client
    //è synchronized per fare in modo che il thread server e il thread clienthandler non accedano nello stesso momento agli attributi
    //questo metodo viene chiamato dal thread server, e quindi accede agli attributi che fanno parte del clienthandler
    public synchronized String riceviMessaggio() {
        try {
        	
        	//se non sono arrivati messaggi va in attesa: esce dall'attesa se arriva il messaggio (svegliato dalla notify) o se finiscono i 20 secondi
            if (messaggioDisponibile==false) {
                wait(20000);
            }
            
            //controllo se non è arrivato il messaggio e nel caso ritorno TIMEOUT
            if (messaggioDisponibile==false) {
                System.out.println("Giocatore " + idGiocatore + ": tempo scaduto!");
                inviaMessaggio(MessageFormatter.format("ERRORE", "tempo_scaduto"));
                return "TIMEOUT";
            }
            
            //nel caso in cui è arrivato il messaggio
            String msg= ultimoMessaggio;
            
            //resetto i parametri per i prossimi controlli
            ultimoMessaggio= null;
            messaggioDisponibile= false;
            
            //ritorna il messaggio arrivato
            return msg;

        } catch (InterruptedException e) {
            return null;
        }
    }
    
    
    //metodo per chiudere la connessione
    public void chiudiConnessione() {
        try {
            attivo= false;
            
            //se ci sono stati errori nell'apertura delle socket non si può fare la close perché darebbe errore
            //se ci sono stati errori nell'apertura delle socket non entra nell'if
            if (in!= null) {
            	in.close();
            }
            
          //se ci sono stati errori nell'apertura delle socket non si può fare la close perché darebbe errore
          //se ci sono stati errori nell'apertura delle socket non entra nell'if
            if (out!= null) {
            	out.close();
            }
            
            //se la socket esiste e se non è già stata chiusa, chiude la socket (metodo isClosed predefinito, restituisce true o false)
            if(socket!=null && socket.isClosed()==false) {
            	socket.close();
            }
            
            //eccezioni (potrebbe andare nel catch se la connessione col server è già caduta)
        } catch (IOException e) {
            System.out.println("Errore chiusura giocatore " + idGiocatore);
        }
    }

    
    //getter che restituisce attributo attivo
    public boolean isAttivo() {
        return attivo;
    }
    
    //getter che restituisce attributo idGiocatore
    public int getIdGiocatore() {
        return idGiocatore;
    }
    
}











