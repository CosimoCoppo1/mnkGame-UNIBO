

package mnkgame;

import java.util.Random;
import java.util.ArrayList;
import java.lang.Math.*;


public class ComputerPlayer implements MNKPlayer {
	
	
	private Random rand;
	private MNKBoard B;
	private MNKGameState myWin;
	private MNKGameState yourWin;
	private int TIMEOUT;
	private long start;             


	public ComputerPlayer() {
	}


	public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
		
		rand    = new Random(System.currentTimeMillis()); 
		B       = new MNKBoard(M,N,K);
		myWin   = first ? MNKGameState.WINP1 : MNKGameState.WINP2; 
		yourWin = first ? MNKGameState.WINP2 : MNKGameState.WINP1;
		TIMEOUT = timeout_in_secs;	
	}
	
	
	//alphabeta pruning
	public double alphabeta(MNKBoard board_, boolean myNode, int depth, double alpha, double beta){
		
		double eval;
		MNKCell fc[] = board_.getFreeCells();
		
		if(depth <= 0 || board_.gameState != MNKGameState.OPEN || (System.currentTimeMillis()-start)/1000.0 > TIMEOUT*(99.0/100.0)){
			eval = evaluate(board_, depth);  
						
		}else if(myNode){
			eval = Integer.MAX_VALUE;
			
			for(MNKCell cell : fc){
				board_.markCell(cell.i, cell.j);
				eval = Math.min(eval, alphabeta(board_, false, depth-1, alpha, beta));
				beta = Math.min(eval, beta);
				board_.unmarkCell();
				if(beta <= alpha)
					break;
			}
			
		}else{
			eval = Integer.MIN_VALUE;
			
			for(MNKCell cell : fc){
				board_.markCell(cell.i, cell.j);
				eval = Math.max(eval, alphabeta(board_, true, depth-1, alpha, beta));
				beta = Math.max(eval, alpha);
				board_.unmarkCell();
				if(beta <= alpha)
					break;
			}			
		}
		
		return eval;		
	}
	
	
	/* Funzione che valuta la configurazione in input. Lo score assegnato a configurazioni VITTORIA/SCONFITTA è influezato dalla profondità della
	mossa (in questo caso "d" ricevuto in input):
    -Vittoria: lo score diminuisce con l'aumentare dei livelli. Si favoriscono le vittorie più vicine.
	-Sconfitta: lo score diminuisce in valore assoluto con l'aumentare dei livelli. Si favoriscono le sconfitte più lontane. */
	public double evaluate(MNKBoard b, int d){
		
		double eval;
		
		if(b.gameState == myWin){
			eval = 45;
			eval -= (6 - d);                  //diminuizione score all'aumentare della profondità. Avendo massimo 6 livelli il range è [39, 45]
		}else if(b.gameState == yourWin){
			eval = -45;
			eval += (6 - d);                  //si ritarda la sconfitta assegnando peso maggiore alle configurazioni più lontane da quella attuale. Range è [-45, -39]
		}else if(b.gameState == MNKGameState.DRAW){
			eval = 0;			
		}else{
			eval = evalOpenConfig(b);         //valutazione nodo non foglia
		}
		
		return eval;
	}
	
	
	/* Funzione euristica per valutazione nodi non foglia.
	Segue la seguente logica: valuta le 4 linee (verticale, orizzontale, diagonale ed antidiagonale) che passano per l'ultima cella marcata.
	La valutazione di ogni riga avviene tramite la funzione "evaluateLine()" che riceve in input la linea sotto forma di vettore e 
	ne restituisce il corrisponente score.
    Lo score che restituisce può essere positivo o negativo, a seconda se la configurazione analizzata è favorevole per noi o per l'avversario.
	La seguente funzione si limita a sommare gli score restituiti dalle 4 chiamate a evaluateLine()*/
	public double evalOpenConfig(MNKBoard b_){
		
		double score = 0;
		
		MNKCell[] mc = b_.getMarkedCells();
		MNKCell lastMarkedCell = mc[mc.length-1];  //ultima cella marcata
		int rows = b_.M;              //num righe
		int cols = b_.N;              //num colonne
		int k = b_.K;
		
		MNKCellState[][] bb = new MNKCellState[rows][cols];
		
		//ricreiamo la configurazione della board in modo da poter comodamente ottenerne le varie linee
		for(int i = 0; i < rows; i++){
			for(int j = 0; j < cols; j++){
				bb[i][j] = MNKCellState.FREE;
			}				
		}			
		for (int p = 0; p < mc.length; p++){
			bb[mc[p].i][mc[p].j] = mc[p].state;
		}	
		
		
		//valuto riga contenente ultima cella marcata		
		if(cols >= k){
			MNKCellState[] rowLine;			
			
			rowLine = bb[lastMarkedCell.i];
			score += evaluateLine(rowLine);
			rowLine = null;			
		}
		
		//valuto colonna contenente ultima cella marcata
		if(rows >= k){
			ArrayList<MNKCellState> colLine = new ArrayList<MNKCellState>();
			
			for(int row = 0; row < rows; row++){
				colLine.add(bb[row][lastMarkedCell.j]);
			}
			score += evaluateLine(colLine.toArray(new MNKCellState[colLine.size()]));	
			colLine = null;				
		}
		
		//valuto diagonale contenente ultima cella marcata
		ArrayList<MNKCellState> diagLine = new ArrayList<MNKCellState>();
		
		int x;
		int y;
		
		if(lastMarkedCell.i > lastMarkedCell.j){              //la cella marcata si trova tra quelle al di sotto della diagonale principale
			x = lastMarkedCell.i - lastMarkedCell.j;
			y = 0;
			
			while(x <= rows - 1 && y <= cols - 1){
				diagLine.add(bb[x][y]);
				x++;
				y++;				
			}			
		}else if(lastMarkedCell.i == lastMarkedCell.j){      //la cella marcata si trova tra quelle che compongono la diagonale principale
			x = 0;
			y = 0;
			
			while(x <= rows - 1 && y <= cols -1){
				diagLine.add(bb[x][y]);
				x++;
				y++;				
			}			
		}else{                                          //la cella marcata si trova tra quelle al di sopra della diagonale principale
			x = 0;
			y = lastMarkedCell.j - lastMarkedCell.i;
			
			while(x <= rows -1 && y <= cols -1){
				diagLine.add(bb[x][y]);
				x++;
				y++;				
			}				
		}
		
		if(diagLine.size() >= k){                       //controllo se il num di celle su tale diagonale è sufficiente a contenere k elementi
			score += evaluateLine(diagLine.toArray(new MNKCellState[diagLine.size()]));				
		}
		diagLine = null;
		
		//valuto anti-diagonale contenente ultima cella marcata
		ArrayList<MNKCellState> antiDiagLine = new ArrayList<MNKCellState>();
		
		/* per ottenere l'antidiagonale contenente la cella marcata applichiamo un metodo diverso dal precedente: inizializziamo gli indici di riga e colonna con
		quelli della cella marcata. Successivamente decrementiamo/aumentiamo gli indici in modo da raggiungere la cella iniziale dell'antidiagonale. Poi ripercorriamo tutte le celle
		di tale diagonale per inserirle nella lista (e poi nel vettore) */		
		x = lastMarkedCell.i;
		y = lastMarkedCell.j;
		
		while(x < rows - 1 && y > 0){    //primo ciclo serve a raggiungere la prima cella dell'antidiagonale (dal basso)
			x++;
			y--;
		}
		
		while(x >= 0 && y <= cols - 1){   //secondo ciclo serve per inserire ogni cella nella lista a partire dalla prima indicata
			antiDiagLine.add(bb[x][y]);
			x--;
			y++;
		}
		
		if(antiDiagLine.size() >= k){                       
			score += evaluateLine(antiDiagLine.toArray(new MNKCellState[antiDiagLine.size()]));				
		}
		antiDiagLine = null;	
		
		return score;
		
	}
	
	/* La seguente funzione valuta la linea ricevuta in input seguendo la seguente logica:
	-la valutazione viene effettuata tramite la funzione "maxSubLineOccupiedCells()" che riceve in ingresso il vettore di celle e il tipo di giocatore (P1/P2)
	-tale funzione viene chiamata 2 volte, una per giocatore. Restituisce 2 elementi: 
	 1_numero di celle consecutive (sottoriga) che siano o marcate dal giocatore ricevuto in input oppure libere, indistintamente
	 2_numero di celle libere contenute in tale sottoriga
	-si valuta se la sottoriga presenta numero di celle >= al numero di celle necessarie da marcare consecutivamente per vincere (K), in caso affermativo si somma tale score, altrimenti passiamo alla valutazione della (eventuale) sottoriga dell'avversario
	-lo score è dato dal numero di elementi del sottovettore meno quello di celle libere in esso contenute
	-se la valutazione riguarda il nostro giocatore allora lo score viene sommato, se riguarda il giocatore avversario viene sottratto */
	public double evaluateLine(MNKCellState[] l){
		double score = 0;
		MNKCellState myCell;
		MNKCellState yourCell;		
		
		if(myWin == MNKGameState.WINP1){
			myCell = MNKCellState.P1;
			yourCell = MNKCellState.P2;			
		}else{
			myCell = MNKCellState.P2;
			yourCell = MNKCellState.P1;
		}
		
		int[] myLine = maxSubLineOccupiedCells(l, myCell);
		
		if(myLine[0] >= B.K){
			score += (myLine[0] - myLine[1]);
		}
		
		//passo a valutare (l'eventuale) sottoriga dell'avversario
		int[] yourLine = maxSubLineOccupiedCells(l, yourCell);
		
		if(yourLine[0] >= B.K){
			score -= (yourLine[0] - yourLine[1]);
		}
		
		return score;		
	}	

	
	/* Funzione che calcola il sottovettore di lunghezza massima che presenta sia celle marcate da "player" sia celle libere, indistintamente.
	Ritorna un array di 2 elementi: il primo contiene il numero di elementi del sottovettore di lunghezza massima, il secondo il numero delle corrispondenti celle libere.
	Per risolvere tale problema si adotta la programmazione dinamica */
	int[] maxSubLineOccupiedCells(MNKCellState[] vec, MNKCellState player){
		
		int S[] = new int[vec.length];           //S vettore delle soluzioni -> si inserisce 1 + elemento precedente in S[i] se S[i] == player o a una cella libera. Si inserisce 0 altrimenti
		int nfree = 0, nFreeMax = 0, imax = 0;   //nfree -> conta numero celle libere nel sottovettore corrente. 
		                                         //nFreeMax -> tiene traccia delle celle libere presenti nel sottovettore che risulta massimo fino a quello corrente
												 //imax -> indice dell'elemento di S che contiene il numero di elementi(e quindi segna la fine) del sottovettore massimo
		if(vec[0] == player || vec[0] == MNKCellState.FREE){
			S[0] = 1;
			if(vec[0] == MNKCellState.FREE)
				nfree++;
		}else{
			S[0] = 0;
		}
		
		for(int i = 1; i <= S.length-1; i++){
			if(vec[i] == player || vec[i] == MNKCellState.FREE){
				S[i] = S[i-1] + 1;
				if(vec[i] == MNKCellState.FREE)
					nfree++;
			}else{
				S[i] = 0;
				if(S[i-1] > S[imax]){
					imax = i-1;
					nFreeMax = nfree;
				}
				nfree = 0;
			}
			if(i == S.length-1 && S[i] > S[imax]){
				imax = i;
				nFreeMax = nfree;
			}			
		}
		
		return new int[] {S[imax], nFreeMax};		
	}
	
	
	
	
	

	public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC) {
		start = System.currentTimeMillis();
		
		if(MC.length > 0) {
			MNKCell c = MC[MC.length-1]; // Recover the last move from MC
			B.markCell(c.i,c.j);         // Save the last move in the local MNKBoard
		}
		
		// If there is just one possible move, return immediately
		if(FC.length == 1)
			return FC[0];
		
		if(myWin == MNKGameState.WINP1 && MC.length==0) //se la prima mossa spetta al mio giocatore può essere effettuata randomicamente
		{
			MNKCell c = FC[rand.nextInt(FC.length)];
			B.markCell(c.i,c.j);
			return c;
		}
	
		
		double score, maxEval = Integer.MIN_VALUE;
		int pos   = rand.nextInt(FC.length); 
		MNKCell result = FC[pos]; // random move
		
		for(MNKCell currentCell : FC) {
			
			// If time is running out, return the randomly selected  cell
            if((System.currentTimeMillis()-start)/1000.0 > TIMEOUT*(99.0/100.0)) {
				break;
				
			}else{
				/* A seconda della grandezza della griglia, più precisamente del numero di celle vuote, si chiama la funzione
				"alphabeta()" limitando la ricerca a un numero variabile (ma prefissato) di livelli di profondità in modo da garantire un buon rapporto efficenza computazionale/ottimalità delle mosse.
				Il massimo numero di livelli di profondità raggiungibile è inversalmente proporzionale al numero di celle libere. */
				B.markCell(currentCell.i, currentCell.j);	
				
				if(FC.length > 85)
					score = alphabeta(B, true, 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
				else if((FC.length > 50) && (FC.length <= 85))
					score = alphabeta(B, true, 2, Integer.MIN_VALUE, Integer.MAX_VALUE);
				else if((FC.length > 30) && (FC.length <= 50))
					score = alphabeta(B, true, 3, Integer.MIN_VALUE, Integer.MAX_VALUE);
				else if((FC.length > 20) && (FC.length <= 30))
					score = alphabeta(B, true, 4, Integer.MIN_VALUE, Integer.MAX_VALUE);
				else if((FC.length > 16) && (FC.length <= 20))
					score = alphabeta(B, true, 5, Integer.MIN_VALUE, Integer.MAX_VALUE);	
				else
					score = alphabeta(B, true, 6, Integer.MIN_VALUE, Integer.MAX_VALUE);					
		
				
				if(score > maxEval){
					maxEval = score;
					result = currentCell;
				}
				
				B.unmarkCell();								
			}	
		}
		
		B.markCell(result.i, result.j);		
		return result;
	}

	public String playerName() {
		return "Cosimo++";
	}
}
