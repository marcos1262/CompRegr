import java.util.Set;
import java.util.Vector;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;

public class Progression{
	private Set<Action> actionSet;
	private BDD goal;
	private BDD initialState;
	private BDD constraints;
	private BDD acepExcuses;
	
	/* Constructor */
	public Progression(ModelReader model) {
		this.actionSet = model.getActionSet();
		this.initialState = model.getInitialStateBDD();
		this.goal = model.getGoalSpec();		
		this.constraints = model.getConstraints();
		this.acepExcuses = model.getPosAcepGoalExcuses();
	}	
	
	/* Foward search from the initial state, towards a goal state. */
	public boolean planForward(){
		BDD reached = initialState.id(); //accumulates the reached set of states.
		BDD Z = reached.id(); // Only new states reached	
		BDD aux;	
		int i = 0;
		Vector<BDD> excusesVec = new Vector<>();
		
		while(!Z.isZero()){
			System.out.println("Iteration: "+i);
			aux = Z.and(goal.id()).and(constraints);

			if (!aux.toString().equals("")) {
				System.out.println("The problem is solvable.");
				return true;
			}

			aux.free();
			aux = Z;
			Z = progression(Z);
			aux.free();
			
			aux = Z;
			Z = Z.apply(reached, BDDFactory.diff); // The new reachable states in this layer
			excusesVec.add(i,Z.id());
			aux.free();
			
			aux = reached;
			reached = reached.or(Z); //Union with the new reachable states
			aux.free();
			
			aux = reached;
			reached = reached.and(constraints);
			aux.free();
			
			i++;
		}
		
		BDD AExcuses = acepExcuses.and(reached);
		System.out.println("The problem is unsolvable.");
		
		if(AExcuses.toString().equals("")){
			System.out.println("There are not found modifications for the goal.");
			return false;
		}else{
			System.out.println("The goal is:");
			goal.printSet();
			System.out.println("(Metric M5) The possible modifications for the goal are:");
			AExcuses.printSet();
		}
		
		/*Finding good excuses*/
		BDD GExcuses;
		for (int j = i-1; j >= 0; j--) { //more distance from the initial state.
			GExcuses = excusesVec.get(j).and(AExcuses); 
			if(!GExcuses.toString().equals("")){
				System.out.println("(Metric M61) The possible modifications for the goal are reached in layer: " + j); 
				GExcuses.printSet();
				break;
			}
		}
		
		for (int layer = 0; layer < i; layer++) { //nearest from the initial state.
			GExcuses = excusesVec.get(layer).and(AExcuses);
			if(!GExcuses.toString().equals("")){
				System.out.println("(Metric M62) The possible modifications for the goal are reached in layer: " + layer); 
				GExcuses.printSet();
				return false;
			}
		}
		
		return false;
	}
		
	/* Non-Deterministic Progression of a formula by a set of actions */
	BDD progression(BDD formula){
		BDD prog = formula.getFactory().zero();
		BDD test;

		for (Action a : actionSet) {
//			System.out.println("Ação: "+a.getName());

			test = progressionQbf(formula, a).and(constraints);

			prog.orWith(test.id());
		}

		return prog;
	}
	
	/* Propplan progression based on action: Qbf based computation */
	BDD progressionQbf(BDD Y, Action a) {
		BDD prog, aux;
		prog = a.getPrecondition().and(Y); //(precondition(a) ^ Y)

		if(!prog.isZero()){
			aux = prog;
			prog = prog.exist(a.getChange()); //qbf computation
			aux.free();

			BDD effBDD = a.getFactory().zero();
			for (BDD e : a.getEffects()) {
				effBDD = effBDD.or(e);
			}

			aux = prog;
			prog = prog.and(effBDD); //prog ^ effects(a)
			aux.free();
				
			aux = prog;
			prog = prog.and(constraints);
			aux.free();
		}
		return  prog;
 	}
}
	