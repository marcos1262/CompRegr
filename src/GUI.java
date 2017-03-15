/**
 * Implementation of the algorithms that performs backward planning, using regression.
 * The method proposed by [Fourman, 2000], the method proposed by [Rintanen, 2008] and the method seen in [Ramirez and Sardina, 2014]
 */
public class GUI {

    /**
     * This method receives a domain-problem file and does the backward search
     * @param args CLI data
     * @throws Exception at reading model file (FileNotFoundException, IOException)
     */
    public static void main(String[] args) throws Exception{

        //File containing the description of the planning domain-problem
        String fileName = args.length > 0 ? args[0] : "testes/LOGISTICS-6-0-GROUNDED.txt";
        String type = args.length > 1 ? args[1] : "rintanen_rec"; //"rintanen" or "rintanen_rec" or "propplan"
        int nodenum = Integer.parseInt(args.length > 2 ? args[2] : "99999999");
        int cachesize = Integer.parseInt(args.length > 3 ? args[3] : "99999");

        ModelReader model = new ModelReader();
        model.fileReader(fileName, type, nodenum, cachesize);

        System.out.println(fileName.substring(fileName.lastIndexOf("/") + 1, fileName.lastIndexOf(".")));

		/* Performs progressive and regressive search */

        Regression r = new Regression(model);
        System.out.println("\nRegressive search by ["+type+"]");
        r.weakPlan();
        r.strongPlan();

//        Progression p = new Progression(model);
//		System.out.println("\nProgressive search");
//		p.planForward();
    }
}