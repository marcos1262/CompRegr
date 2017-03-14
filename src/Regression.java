import java.util.Set;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;

class Regression{
	
	private Set<Action> actionSet;
	private BDD goalState;
	private BDD initialState;
	private BDD constraints;
	private String type;
	private boolean strongPlan;
	private int qtdVar;

	Regression(ModelReader model) {
		this.actionSet = model.getActionSet();
		this.initialState = model.getInitialStateBDD();
		this.goalState = model.getGoalSpec();		
		this.constraints = model.getConstraints();
		this.qtdVar = model.getVarTable2().size();
		this.type = model.getType();
	}

	boolean weakPlan(){
		strongPlan = false;
		if (planBackward()) {
			System.out.println("The problem has a weak solution.");
			return true;
		}
		return false;
	}

	boolean strongPlan(){
		strongPlan = true;
		if (planBackward()) {
			System.out.println("The problem has a strong solution.");
			return true;
		}
		return false;
	}

	/* Backward search from the goal state, towards initial state. */
	boolean planBackward(){
		BDD reached = goalState.id(); //accumulates the reached set of states.
		BDD Z = reached.id(); // Only new states reached	
		BDD aux;	
		int i = 0;
		
		while(!Z.isZero()){
			System.out.println("Iteration: "+i);
			aux = Z.and(initialState.id());

			if (aux.equals(initialState.id())) return true;

			aux.free();
			aux = Z;					
			Z = regression(Z); 
			aux.free();
			
			aux = Z;
			Z = Z.apply(reached, BDDFactory.diff); // The new reachable states in this layer
			aux.free();
			
			aux = reached;
			reached = reached.or(Z); //Union with the new reachable states
			aux.free();			
			
			aux = reached;
			reached = reached.and(constraints);
			aux.free();
			
			i++;				
		}
		
		System.out.println("The problem is unsolvable.");

		return false;
	}
		
	/* Non-Deterministic Regression of a formula by a set of actions */
	private BDD regression(BDD formula){
		BDD reg = formula.getFactory().zero(),
			aux;

		for (Action a : actionSet) {
//			System.out.println("Ação: "+a.getName());

			// The constraints will complete the variables in the returned states
			switch (type) {
				case "ritanen":
					aux = regressionEpc(formula, a);
					break;
				case "propplan":
					aux = regressionQbf(formula, a);
					break;
				default:
					throw new RuntimeException("Invalid regression type! (ritanen or propplan)");
			}
			aux = aux.and(constraints);

			reg.orWith(aux.id());
//			System.out.print("OK: ");reg.printSet();

			aux.free();
		}
		return reg;
	}
	
	/* Propplan regression based on action: Qbf based computation */
	private BDD regressionQbf(BDD Y, Action a) {
		BDD reg, aux;

		BDD effBDD = a.getFactory().zero();
		for (BDD e : a.getEffects()) {
			effBDD = effBDD.or(e);
		}

		if (strongPlan) reg = effBDD.not().or(Y); //(effects(a) --> Y)
		else reg = effBDD.and(Y); //(effects(a) ^ Y)
		
		if(!reg.isZero()){
			aux = reg;

			//qbf computation
			if (strongPlan) reg = reg.forAll(a.getChange());
			else reg = reg.exist(a.getChange());

			aux.free();
				
			aux = reg;
			reg = reg.and(a.getPrecondition()); //precondition(a) ^ reg
			aux.free();
			
			/*if(reg.toString().equals("") == false){
				System.out.println(a.getName());
			}*/
				
			aux = reg;
			reg = reg.and(constraints);
			aux.free();
		}
		return  reg;
 	}
	
	/*Ritanen's regression based on action: epc computation */
	private BDD regressionEpc(BDD formula, Action a){
		BDDFactory factory = a.getFactory();
		Boolean epcP, epcNotP;

		/* formulaR: obtained from formula by replacing every literal l by epc_l(e) v (l ^ epc_~l(e))*/
		BDD formulaR, aux;

		if (strongPlan) formulaR = factory.one();
		else formulaR = factory.zero();

		/* For each effect*/
		for (int i = 0; i < a.getEffects().size(); i++) {
			aux = formula.id();

			/*formulaR computation:
				- For each p, if EPC_p(e) = true then EPC_p(e) v (l ^ EPC_~l(e)) = true
				- For each p, if EPC_~p(e) = true then EPC_p(e) v (l ^ EPC_~l(e)) = false */
			for(int j = 0; j < qtdVar; j++){

				epcP = a.getEpcPTable().get(i).get(j);
				epcNotP = a.getEpcNotPTable().get(i).get(j);

				if(epcP){
					aux.restrictWith(factory.ithVar(j));
				}else if(epcNotP){
					aux.restrictWith(factory.nithVar(j));
				}

				if(aux.equals(factory.zero())) {
					break;
				}
			}

			if (strongPlan) {
				formulaR.andWith(aux);
				if (formulaR.equals(factory.zero())) {
					break;
				}
			}else formulaR.orWith(aux);
		}

		// regression(f,a) = precondition(a) and formula_r
		BDD reg = a.getPrecondition().and(formulaR);
		formulaR.free();
		return reg;
	}
	
}
	