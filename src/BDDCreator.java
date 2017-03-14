import java.io.IOException;
import java.util.*;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.JFactory;

class BDDCreator {
	//Number of propositions (including variables v and v')
	private int propNum = 0;
	private transient BDD initialStateBDD;
	private transient BDD goalBDD;
	private transient BDD constraintBDD;
	private transient BDD excusesBDD = null;
	private transient BDD excusesGoalBDD = null;
	private Vector<Integer> propGoal = new Vector<>();
			
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
	
	BDD getExcusesBDD() {
		return excusesBDD;
	}
	
	BDD getExcusesGoalBDD() {
		return excusesGoalBDD;
	}
	
	/*[input: just the actions] Initializes the BDD variables table with the propositions, propositions primed and actions*/
	void initializeVarTable(String propLine) throws IOException {
		StringTokenizer tknProp = new StringTokenizer(propLine, ",");	
		propNum = tknProp.countTokens(); //Propositions
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
		getAcceptableExcusesStates();
	}
	
	/**Creates a BDD representing the goal **/
	void createGoalBdd(String readLine){
		goalBDD = createAndBdd(readLine);
		getGoalPropositions(readLine);
		getAcceptableExcusesGoalStates();
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
			
	/**Creates a vector of propositions indexes in the goal **/
	void getGoalPropositions(String readLine){
		StringTokenizer tkn = new StringTokenizer(readLine, ",");
		String tknPiece;
		String prop; 
		int index;
	
		while(tkn.hasMoreTokens()) {
			tknPiece = tkn.nextToken().trim();
			if(tknPiece.startsWith("~")){
				prop = tknPiece.substring(1);  // without the signal ~
				index = varTable.get(prop);
			}else{
				index = varTable.get(tknPiece);
			}
			propGoal.add(index);		
		}
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
	BDD createOrBdd(String readLine) {
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
	public BDD createExclusiveOrBdd(String line){
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
	
	
	/************Generating Excuses **************/
	
	
	/** Creates all acceptable excuses goal states **/
	void getAcceptableExcusesGoalStates(){
		excusesGoalBDD = createExcusesGoalState(propGoal.get(0));
		for (int i = 1; i < propGoal.size(); i++) {
			excusesGoalBDD.orWith(createExcusesGoalState(propGoal.get(i)));
		}
		
	}

	/** Creates all acceptable excuses states **/
	void getAcceptableExcusesStates(){
		excusesBDD = createExcusesState(0);
		for(int i = 0; i < propNum; i = i + 1){
			excusesBDD.orWith(createExcusesState(i));
		}
	}
	
	/**Create a BDD which is different from the goal by change the variable value in the index**/
	BDD createExcusesGoalState(int index){
		/*BDD representing the index to be changed**/
		BDD indexBDD = fac.ithVar(index);		
		/*Discovering the index value **/
		BDD teste = goalBDD.and(indexBDD);
		boolean valueIsZero = false;
		if(teste.toString().equals(""))
			valueIsZero = true; //The value of the variable[index] is zero
		
		/**Relaxing the index value**/
		teste = goalBDD.exist(indexBDD);		
		
		/**Construct the excuse state**/
		if(valueIsZero){
			teste.andWith(indexBDD);
		}else{
			teste.andWith(indexBDD.not());
		}
		return teste;
	}
	
	
	/**Create a BDD which is different from the initial state by change the variable value in the index**/
	BDD createExcusesState(int index){
		/**BDD representing the index to be changed**/
		BDD indexBDD = fac.ithVar(index);		
		/**Discovering the index value **/
		BDD teste = initialStateBDD.and(indexBDD);
		boolean valueIsZero = false;
		if(teste.toString().equals(""))
			valueIsZero = true; //The value of the variable[index] is zero
		
		/**Relaxing the index value**/
		teste = initialStateBDD.exist(indexBDD);		
		
		/**Construct the excuse state**/
		if(valueIsZero){
			teste.andWith(indexBDD);
		}else{
			teste.andWith(indexBDD.not());
		}
		return teste;
	}

	
}