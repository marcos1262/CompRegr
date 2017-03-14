/**
 * Implementation of the algorithms that performs backward planning, using regression.
 * The method proposed by [Fourmann, 2000] and the method proposed  by [Ritanen, 2008]
 */
public class GUI {

    /**
     * This method receives a domain-problem file and does the backward search
     * @param args CLI data
     * @throws Exception at reading model file (FileNotFoundException, IOException)
     */
    public static void main(String[] args) throws Exception{

        //File containing the description of the planning domain-problem
        String fileName = args.length > 0 ? args[0] : "testes/climber-GROUNDED-1.txt";
        String type = args.length > 1 ? args[1] : "propplan"; //"ritanen" or "propplan"
        int nodenum = Integer.parseInt(args.length > 2 ? args[2] : "999999");
        int cachesize = Integer.parseInt(args.length > 3 ? args[3] : "9999");

        ModelReader model = new ModelReader();
        model.fileReader(fileName, type, nodenum, cachesize);

        System.out.println(fileName.substring(fileName.lastIndexOf("/") + 1, fileName.lastIndexOf(".")));

		/*Performs progressive and regressive search*/

        Regression r = new Regression(model);
        System.out.println("\nRegressive search by ["+type+"]");
        r.weakPlan();

        Progression p = new Progression(model);
		System.out.println("\nProgressive search");
		p.planForward();
    }
}