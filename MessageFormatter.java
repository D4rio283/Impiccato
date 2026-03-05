package impiccato;

//classe che contiene il metodo format; è static così da non dover creare l'oggetto (si fa MessageFormatter.format)
//format prende il tipo del messaggio e il messaggio e li concatena
class MessageFormatter {
public static String format(String tipo, String contenuto) {
  return tipo + "|" + contenuto;
}
}

