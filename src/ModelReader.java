import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Hashtable;
import java.util.Set;

import net.sf.javabdd.BDD;

/**
 * Read the input file and call the methods of class BDDCreator
 */
class ModelReader {

    private BDDCreator cre;
    private String type;

    /**
     * Read the lines of the fileName.
     *
     * @param fileName  file path
     * @param pType     regression type
     * @param nodenum   number of nodes
     * @param cachesize cache size
     */
    void fileReader(String fileName, String pType, int nodenum, int cachesize) throws Exception {
        type = pType;
        cre = new BDDCreator(nodenum, cachesize);

        BufferedReader in = new BufferedReader(new FileReader(fileName));
        String line,
                propositionsLine,
                initialStateLine,
                constraintsLine,
                goalLine,
                actionName,
                actionPre,
                actionEff;

        while (in.ready()) {
            line = in.readLine();

            // read the lines of the fileName corresponding the variables
            if (line.equals("<predicates>")) {
                line = in.readLine(); //read next line containing the propositions
                propositionsLine = line;
                line = in.readLine(); //read <\predicates>
                cre.initializeVarTable(propositionsLine);
            }

            if (line.equals("<constraints>")) {
                line = in.readLine(); //read next line containing the constraints OR <\constraints>
                while (!line.equals("<\\constraints>")) {
                    constraintsLine = line;
                    cre.createConstraintBDD(constraintsLine);
                    line = in.readLine();
                }
            }

            // read the lines corresponding to the initial state
            if (line.equals("<initial>")) {
                line = in.readLine(); //read next line containing the initial state specification
                initialStateLine = line;
                line = in.readLine(); // read the line <\initial>
                cre.createInitialStateBdd(initialStateLine);
            }

            // read the lines corresponding to the planning goal
            if (line.equals("<goal>")) {
                line = in.readLine(); //read next line
                goalLine = line;
                cre.createGoalBdd(goalLine);
                line = in.readLine();
            }

            // read the lines corresponding to the actions
            if (line.equals("<actionsSet>")) {
                line = in.readLine();
                while (!line.equals("<\\actionsSet>")) {
                    if (line.equals("<action>")) {
                        line = in.readLine(); //<name><\name>
                        actionName = line.substring(line.indexOf(">") + 1, line.indexOf("\\") - 1);

                        line = in.readLine(); //<pre><\pre>
                        actionPre = line.substring(line.indexOf(">") + 1, line.indexOf("\\") - 1);

                        line = in.readLine(); //<pos><\pos>
                        actionEff = line.substring(line.indexOf(">") + 1, line.indexOf("\\") - 1);

                        Action action = new Action(actionName, actionPre, actionEff, cre, pType);
                        cre.addAction(action);
                        in.readLine(); //<\action>
                    }
                    line = in.readLine(); //<action>
                }
            }
        }
        in.close();
    }

    public String getType() {
        return type;
    }

    Hashtable<Integer, String> getVarTable2() {
        return cre.getVarTable2();
    }

    BDD getInitialStateBDD() {
        return cre.getInitiaStateBDD();
    }

    Set<Action> getActionSet() {
        return cre.getActionsSet();
    }

    BDD getGoalSpec() {
        return cre.getGoalBDD();
    }

    BDD getConstraints() {
        return cre.getConstraintBDD();
    }

    BDD getPosAcepExcuses() {
        return cre.getExcusesBDD();
    }

    BDD getPosAcepGoalExcuses() {
        return cre.getExcusesGoalBDD();
    }

}
