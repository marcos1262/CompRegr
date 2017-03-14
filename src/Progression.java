import java.util.Set;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;

class Progression{
	private Set<Action> actionSet;
	private BDD goal;
	private BDD initialState;
	private BDD constraints;

	/* Constructor */
	Progression(ModelReader model) {
		this.actionSet = model.getActionSet();
		this.initialState = model.getInitialStateBDD();
		this.goal = model.getGoalSpec();		
		this.constraints = model.getConstraints();
	}
	
	/* Foward search from the initial state, towards a goal state. */
	boolean planForward(){
		BDD reached = initialState.id(); //accumulates the reached set of states.
		BDD Z = reached.id(); // Only new states reached	
		BDD aux;	
		int i = 0;

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
		
	/* Non-Deterministic Progression of a formula by a set of actions */
	private BDD progression(BDD formula){
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
	private BDD progressionQbf(BDD Y, Action a) {
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
	