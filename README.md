Il progetto consiste nella realizzazione del gioco dell’“Impiccato” in Java utilizzando l’architettura Client-Server.

Tre giocatori (client) si sfidano per indovinare la parola generata dal server che, man mano che vengono inserite le lettere corrette, si rivela progressivamente.

Oltre all’intuito, ai giocatori è richiesta anche una certa rapidità nello scegliere le lettere o le parole da provare. Il timer di 20 secondi per turno non fa sconti a nessuno!

Attenzione anche alle vite: ogni volta che si inserisce una lettera o una parola sbagliata, oppure quando il tempo scade, il giocatore perde una vita.
Ciascun giocatore ne ha a disposizione solo 6: basta poco per essere eliminati!

Per giocare online è sufficiente scaricare i codici presenti nella repository, avviare il Server e successivamente il Client sui vari dispositivi dei giocatori.
È importante ricordare che il server deve essere avviato da un solo giocatore, mentre gli altri client si collegheranno ad esso.

Affinché la connessione funzioni correttamente, i computer dei giocatori devono essere collegati tra loro tramite una rete; e in tal caso, nel codice della classe Client bisognerà inserire
l'ip della macchina su cui gira il server. Se viene istanziato un client nel dispositivo in cui già viene eseguito il server, l'ip assegnato nel codice dovrà essere quello di localhost
(127.0.0.1).

Se invece si desidera giocare in locale, è possibile avviare il server e poi eseguire tre istanze del client sullo stesso dispositivo (in locale), simulando così una partita tra tre giocatori. L'ip in questo caso sarà quello di localhost.

Buon divertimento...e ricordate: basta una lettera per vincere...ma anche per rimanere appesi!

