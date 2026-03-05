package impiccato;

//classe che contiene il metodo parse; è static così da non dover creare l'oggetto (si fa MessageParser.parse)
//restituisce un array di stringhe
class MessageParser {
public static String[] parse(String messaggio) {
  if (messaggio == null || messaggio.contains("|")==false) {
  	return null;
  }
  
  //splitto in due parti il messaggio: lo splitto in tipo e contenuto
  //il carattere separatore è "|" (c'è bisogno di "\\" per far capire a java che non si riferisce all'OR)
  String[] campi = messaggio.split("\\|", 2);
  if (campi.length!=2) {
  	return null;
  }
  return campi;
}
}
