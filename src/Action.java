import java.util.*;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;

class Action {
	private String name;
	private BDD precondition;
	private int qtdVar;
	private ArrayList<BDD> effects;

	/*For PropPlan Regression */
	private BDD change;

	/*For Ritanen's regression */
	private ArrayList<ArrayList<BDD>> effectVec;
	private ArrayList<ArrayList<Boolean>> epcPTable = new ArrayList<>();
	private ArrayList<ArrayList<Boolean>> epcNotPTable = new ArrayList<>();

	/**
	 * Constructor
	 *
	 * @param actionName name
	 * @param preCond precondition
	 * @param eff effect
	 * @param cre BDD construction class
	 * @param pType regression type
	 */
	Action(String actionName, String preCond, String eff, BDDCreator cre, String pType){
		name = actionName;
		qtdVar = cre.getVarTable2().size();
		
		precondition = cre.createAndBdd(preCond);
		effects = cre.createEffectsBdd(eff);

		change = getFactory().one();
		Set<String> changeSet = createChangeSet(eff);
		
		if(pType.equals("ritanen")){ //computes epc
			effectVec = cre.createEffectsVec(eff);
			fillEpcTable();		
		}else if(pType.equals("propplan")){ //compute change set
			for (String s : changeSet) {
				change.andWith(cre.createAndBdd(s));
			}
		}
	}

	/**
	 * Creates the change set which is the union of all propositions involved in the effect list, without negation.
	 *
	 * @param effs effects separated by ';'
	 * @return change set
	 */
	private Set<String> createChangeSet(String effs){
		Set<String> changeSet = new HashSet<>();
		StringTokenizer tknEffs = new StringTokenizer(effs, ";");
		String eff;

		while (tknEffs.hasMoreTokens()) {
			eff = tknEffs.nextToken();
			StringTokenizer tknEff = new StringTokenizer(eff, ",");
			String tknPiece;

			/* Adding the effect propositions in the change set*/
			while (tknEff.hasMoreTokens()) {
				tknPiece = tknEff.nextToken();
				if(tknPiece.startsWith("~")){
					tknPiece = tknPiece.substring(1); // deletes the signal ~
				}
				changeSet.add(tknPiece);
			}
		}

		return changeSet;
	}
	
	/* The condition (E) is defined for each action and proposition */
	private void fillEpcTable(){
		BDDFactory factory = precondition.getFactory();
		boolean epcP, epcNotP;
		BDD prop, negprop;

		/*For each propositional value, computes the EPC.*/
		/*and for each effect*/
		for (int i = 0; i < effects.size(); i++) {
			epcPTable.add(new ArrayList<>());
			epcNotPTable.add(new ArrayList<>());

			for (int j = 0; j < qtdVar; j++) {
				prop = factory.ithVar(j);	//BDD for proposition
				negprop = factory.nithVar(j);	//BDD for negation of the proposition

				epcP = epc(prop, effectVec.get(i));
				epcNotP = epc(negprop, effectVec.get(i));

				epcPTable.get(i).add(epcP);
				epcNotPTable.get(i).add(epcNotP);
			}
		}
	}
		
	ArrayList<ArrayList<Boolean>> getEpcNotPTable() {
		return epcNotPTable;
	}
	
	ArrayList<ArrayList<Boolean>> getEpcPTable() {
		return epcPTable;
	}

	/* EPC_literal(effect) when the effect is a literal */
	private boolean epc(BDD literal, BDD effect) {
		return literal.equals(effect);
	}

	/* EPC_literal(effect) when the effect is a conjunction */
	private boolean epc(BDD literal, ArrayList<BDD> effect) {
		for (BDD e : effect) {
			if (epc(literal, e)) {
				return true;
			}
		}
		return false;
	}
		
	public String getName() {
		return name;
	}
	
	BDD getChange() {
		return change;
	}
	
	BDD getPrecondition() {
		return precondition;
	}
	
	ArrayList<BDD> getEffects() {
		return effects;
	}

	public BDDFactory getFactory(){
		return effects.get(0).getFactory();
	}
}
