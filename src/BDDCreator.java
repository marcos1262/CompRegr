import java.io.IOException;
import java.util.*;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.JFactory;

class BDDCreator {
	private transient BDD initialStateBDD;
	private transient BDD goalBDD;
	private transient BDD constraintBDD;
			
	//Associates the name of the variable to its position in the BDD VariableSet
	private transient Hashtable<String,Integer> varTable = new Hashtable<>();
	private transient Hashtable<Integer,String> varTable2 = new Hashtable<>();
	private Set<Action> actionsSet = new HashSet<>();
	private BDDFactory fac;
	
	BDDCreator(int nodenum, int cachesize){
		//fac = JFactory.init(9000000, 9000000);	
		fac = JFactory.init(nodenum, cachesize);
		constraintBDD = fac.one();
	}
	
	/***** Get Methods *******/

	Hashtable<Integer, String> getVarTable2() {
		return varTable2;
	}
	
	BDD getInitiaStateBDD(){
		return initialStateBDD;
	}
	
	BDD getGoalBDD(){
		return goalBDD;
	}
	
	public Set<Action> getActionsSet() {
		return actionsSet;
	}
	
	BDD getConstraintBDD() {
		return constraintBDD;
	}
	
	/*[input: just the actions] Initializes the BDD variables table with the propositions, propositions primed and actions*/
	void initializeVarTable(String propLine) throws IOException {
		StringTokenizer tknProp = new StringTokenizer(propLine, ",");
		int propNum = tknProp.countTokens();
		fac.setVarNum(propNum);
		
		//Filling the table positions corresponding to the propositions
		String propName;
		for (int i = 0; i < propNum; i++) {
			propName = tknProp.nextToken();
			varTable.put(propName,i);
			varTable2.put(i,propName);
		}
		
	}

	/**Creates a BDD representing the initial state.*/
	void createInitialStateBdd(String readLine){
		initialStateBDD = createAndBdd(readLine);
	}
	
	/**Creates a BDD representing the goal **/
	void createGoalBdd(String readLine){
		goalBDD = createAndBdd(readLine);
	}

	/**
	 * Creates a list of BDDs representing the effects.
	 *
	 * @param readLine effects separated by ';'
	 * @return list of BDDs
	 */
	ArrayList<BDD> createEffectsBdd(String readLine) {
		StringTokenizer effs = new StringTokenizer(readLine, ";");
		String eff;
		ArrayList<BDD> list = new ArrayList<>();

		while(effs.hasMoreTokens()) {
			eff = effs.nextToken();
			list.add(createAndBdd(eff));
		}
		return list;
	}
	
	/** Create a BDD representing the conjunction of the propositions in readLine */
	BDD createAndBdd(String readLine) {
		StringTokenizer tkn = new StringTokenizer(readLine, ",");
		String tknPiece = tkn.nextToken().trim();
		String prop;
		int index;
		BDD bdd;
		
		if(tknPiece.startsWith("~")){
			prop = tknPiece.substring(1); // without the signal ~
			index = varTable.get(prop);
			bdd = fac.nithVar(index);
		}else{
			index = varTable.get(tknPiece);
			bdd = fac.ithVar(index);
		}
		while(tkn.hasMoreTokens()) {
			tknPiece = tkn.nextToken();
			if(tknPiece.startsWith("~")){
				prop = tknPiece.substring(1);  // without the signal ~
				index = varTable.get(prop);
				bdd.andWith(fac.nithVar(index));
			}else{
				index = varTable.get(tknPiece);
				bdd.andWith(fac.ithVar(index));
			}
		}
		return bdd;
	}
	
	/** Create a BDD representing the conjunction of the propositions in readLine */
	ArrayList<ArrayList<BDD>> createEffectsVec(String readLine) {
		StringTokenizer effs = new StringTokenizer(readLine, ";");
		String eff;
		ArrayList<ArrayList<BDD>> list = new ArrayList<>();

		while(effs.hasMoreTokens()) {
			eff = effs.nextToken();

			ArrayList<BDD> bddList = new ArrayList<>();
			String tknPiece;
			String prop;
			int index;
			BDD bdd;
			StringTokenizer tkn = new StringTokenizer(eff, ",");

			while(tkn.hasMoreTokens()){
				tknPiece = tkn.nextToken().trim();
				if(tknPiece.startsWith("~")){
					prop = tknPiece.substring(1); // without the signal ~
					index = varTable.get(prop);
					bdd = fac.nithVar(index);
				}else{
					index = varTable.get(tknPiece);
					bdd = fac.ithVar(index);
				}
				bddList.add(bdd);
			}
			list.add(bddList);
		}
		return list;
	}

	/** Create a BDD representing the conjunction of the propositions in readLine */
	private BDD createOrBdd(String readLine) {
		StringTokenizer tkn = new StringTokenizer(readLine, ",");
		String tknPiece = tkn.nextToken().trim(); 
		int index;
		BDD bdd;
		
		index = varTable.get(tknPiece);
		bdd = fac.ithVar(index);
		
		while(tkn.hasMoreTokens()) {
			tknPiece = tkn.nextToken();
			index = varTable.get(tknPiece);
			bdd.orWith(fac.ithVar(index));
		}
		return bdd;
	}
	
	/*Creates a BDD that represents an exclusive or*/
	private BDD createExclusiveOrBdd(String line){
		StringTokenizer tkn = new StringTokenizer(line, ",");
		String tknPiece; 

		String beforeTkn, afterTkn;
		int init, end, index;
		BDD bdd, aux;
		BDD returnedBdd = null;
		
		while(tkn.hasMoreTokens()){
			tknPiece = tkn.nextToken();
			index = varTable.get(tknPiece);
			bdd = fac.ithVar(index);
			
			init = line.indexOf(tknPiece);
			end = init + tknPiece.length() + 1;
			
			beforeTkn = line.substring(0,init);
			afterTkn = line.substring(end);
			
			if(beforeTkn.equals("")){
				aux = createOrBdd(afterTkn).not();
				bdd.andWith(aux);
			}else if(afterTkn.equals("")){
				aux = createOrBdd(beforeTkn).not();
				bdd.andWith(aux);
			}else {	
				aux = createOrBdd(beforeTkn).not();
				bdd.andWith(aux);
				
				aux = createOrBdd(afterTkn).not();
				bdd.andWith(aux);
				 
			}			
	
			if(returnedBdd == null){
				returnedBdd = bdd;
			}else{
				returnedBdd.orWith(bdd);
			}
		}
		return returnedBdd;
	}
	
	void addAction(Action action){
		actionsSet.add(action);
	}
	
	/*Creates the constraint bdd */
	void createConstraintBDD(String line){
		BDD constr = createExclusiveOrBdd(line);
		if(constr != null){
			constraintBDD.andWith(constr);
		}
	}

}