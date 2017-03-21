package edu.pitt.dbmi.algo.bootstrap;
import edu.cmu.tetrad.bayes.BayesIm;
import edu.cmu.tetrad.bayes.BayesPm;
import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.ContinuousVariable;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataUtils;
import edu.cmu.tetrad.data.DiscreteVariable;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphUtils;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.DagToPag;
import edu.cmu.tetrad.sem.LargeScaleSimulation;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.RandomUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mahdi on 1/24/17.
 */
public class TestBootstrap {
    public static void main(String[] args) throws IOException {
        String algorithm = "gfci";
        for ( int i = 0; i < args.length; i++ ) {
            switch (args[i]) {
                case "-a":
                    algorithm = args[i+1];
            }
        }

        double structurePrior = 1, samplePrior = 1;
        double penaltyDiscount = 4.0;
        int maxPathLength = -1;
        int depth = 5;
        boolean faithfulnessAssumed  = false;
        boolean completeRuleSetUsed = true;
        double alpha = 0.001;

        switch(algorithm){
            case "rfci":
                System.out.println("Run TestBRfci!");
                testBRFCI(alpha, depth, completeRuleSetUsed, maxPathLength);
                break;
            case "fges":
                System.out.println("Run TestBFGes!");
                testBFGES(structurePrior, samplePrior, faithfulnessAssumed);
            case "gfci":
                System.out.println("Run TestBGFci!");
                testBGFci(penaltyDiscount, depth, faithfulnessAssumed, completeRuleSetUsed, maxPathLength);
        }

    }

    public static void testBRFCI(double alpha, int depth, boolean completeRuleSetUsed, int maxPathLength) {

        int numVars = 500;
        int edgesPerNode = 2;
        int LV = 20;
        int numCases = 1000;
        int numBootstrapSamples = 1000;
        boolean verbose = true;

        Graph dag = makeDAG(numVars, edgesPerNode, LV);
        final DagToPag dagToPag = new DagToPag(dag);
        dagToPag.setCompleteRuleSetUsed(true);
        Graph truePag = dagToPag.convert();
        truePag = GraphUtils.replaceNodes(truePag, dag.getNodes());

        // data simulation
        LargeScaleSimulation simulator = new LargeScaleSimulation(dag);
        DataSet data = simulator.simulateDataRecursive(numCases);
        // To remove the columns related to latent variables from dataset
        data = DataUtils.restrictToMeasured(data);
        
        Parameters parameters = new Parameters();
        parameters.set("alpha", alpha);
        parameters.set("depth", depth);
        parameters.set("completeRuleSetUsed", completeRuleSetUsed);
        parameters.set("maxPathLength", maxPathLength);
        parameters.set("verbose", false);
        
        //        Run in parallel mode
        BootstrapExperiment bootstrapExpObj = new BootstrapExperiment(data, BootstrapAlgName.RFCI, numBootstrapSamples);
        bootstrapExpObj.setTrueGraph(truePag);
        bootstrapExpObj.setOut("~/bootstrapsearch/tmp101","testBRFCI_parallel");
        bootstrapExpObj.setParallelMode(true);
        bootstrapExpObj.setParameters(parameters);
        bootstrapExpObj.setVerbose(verbose);
        bootstrapExpObj.runExperiment();

        //        Run in sequential mode
        bootstrapExpObj = new BootstrapExperiment(data, BootstrapAlgName.RFCI, numBootstrapSamples);
        bootstrapExpObj.setTrueGraph(truePag);
        bootstrapExpObj.setOut("~/bootstrapsearch/tmp101","testBRFCI_sequential");
        bootstrapExpObj.setParallelMode(false);
        bootstrapExpObj.setParameters(parameters);
        bootstrapExpObj.setVerbose(verbose);
        bootstrapExpObj.runExperiment();

        System.out.println("Done!");

    }

    public static void testBFGES(double structurePrior, double samplePrior, boolean faithfulnessAssumed){
        int numVars = 200;
        int edgesPerNode = 2;
        int numCases = 1000;
        int numBootstrapSamples = 1000;
        boolean verbose = true;

        Graph dag = makeDAG(numVars, edgesPerNode);


        BayesPm pm = new BayesPm(dag, 2, 3);
        BayesIm im = new MlBayesIm(pm, MlBayesIm.RANDOM);

        System.out.println("Generating data");

        DataSet data = im.simulateData(numCases, 0, false);

        Parameters parameters = new Parameters();
        parameters.set("structurePrior", structurePrior);
        parameters.set("samplePrior", samplePrior);
        parameters.set("faithfulnessAssumed", faithfulnessAssumed);
        parameters.set("numPatternsToStore", 0);
        parameters.set("verbose", false);
        
        //        Run in sequential mode
        BootstrapExperiment bootstrapExpObj = new BootstrapExperiment(data, BootstrapAlgName.FGES, numBootstrapSamples);
        bootstrapExpObj.setTrueGraph(dag);
        bootstrapExpObj.setOut("~/bootstrapsearch/tmp101","testFGES_sequential");
        bootstrapExpObj.setParallelMode(false);   // parallel if true
        bootstrapExpObj.setParameters(parameters);
        bootstrapExpObj.setVerbose(verbose);
        bootstrapExpObj.runExperiment();

        //        Run in parallel mode
        bootstrapExpObj = new BootstrapExperiment(data, BootstrapAlgName.FGES, numBootstrapSamples);
        bootstrapExpObj.setTrueGraph(dag);
        bootstrapExpObj.setOut("~/bootstrapsearch/tmp101","testFGES_parallel");
        bootstrapExpObj.setParallelMode(true);
        bootstrapExpObj.setParameters(parameters);
        bootstrapExpObj.setVerbose(verbose);
        bootstrapExpObj.runExperiment();
        System.out.println("Done!");
    }

    public static void testBGFci(double penaltyDiscount, int maxDegree, boolean faithfulnessAssumed, boolean completeRuleSetUsed, int maxPathLength){
        System.out.println("Seed = " + RandomUtil.getInstance().getSeed());

//        double alpha = .1;
//        int depth = -1;
//        double penaltyDiscount = 4.0;
//        int maxPathLength = -1;

        double coefLow = .3;
        double coefHigh = 1.5;
        int numLatentConfounders = 50;
        int numVars = 200;
        int edgesPerNode = 2;

        int numCases = 1000;
        int numBootstrapSamples = 5;
        boolean verbose = true;

        List<Node> vars = new ArrayList<>();
        for (int i = 0; i < numVars; i++) {
            vars.add(new ContinuousVariable("X" + i));
        }
        Graph dag = GraphUtils.randomGraphRandomForwardEdges(vars, numLatentConfounders, (int) (numVars * edgesPerNode),
                10, 10, 10, false, false);
        LargeScaleSimulation simulator = new LargeScaleSimulation(dag);
        simulator.setCoefRange(coefLow, coefHigh);
        DataSet data = simulator.simulateDataRecursive(numCases);
        data = DataUtils.restrictToMeasured(data);

        Parameters parameters = new Parameters();
        parameters.set("penaltyDiscount", penaltyDiscount);
        parameters.set("maxDegree", maxDegree);
        parameters.set("faithfulnessAssumed", faithfulnessAssumed);
        parameters.set("completeRuleSetUsed", completeRuleSetUsed);
        parameters.set("maxPathLength", maxPathLength);
        parameters.set("verbose", false);
        
        //        Run in sequential mode
        BootstrapExperiment bootstrapExpObj = new BootstrapExperiment(data, BootstrapAlgName.GFCI, numBootstrapSamples);
        bootstrapExpObj.setTrueGraph(dag);
        bootstrapExpObj.setOut("~/bootstrapsearch/tmp101","testBGFci_sequential");
        bootstrapExpObj.setParallelMode(false);
        bootstrapExpObj.setParameters(parameters);
        bootstrapExpObj.setVerbose(verbose);
        bootstrapExpObj.runExperiment();

        //        Run in parallel mode
        bootstrapExpObj = new BootstrapExperiment(data, BootstrapAlgName.GFCI, numBootstrapSamples);
        bootstrapExpObj.setTrueGraph(dag);
        bootstrapExpObj.setOut("~/bootstrapsearch/tmp101","testBGFci_parallel");
        bootstrapExpObj.setParallelMode(true);
        bootstrapExpObj.setParameters(parameters);
        bootstrapExpObj.setVerbose(verbose);
        bootstrapExpObj.runExperiment();
        System.out.println("Done!");
    }

    private static Graph makeDAG(int numVars, double edgesPerNode, int numLatentConfounders){
        final int numEdges = (int) (numVars * edgesPerNode);
        System.out.println("Making list of vars");
        List<Node> vars = new ArrayList<>();
        for (int i = 0; i < numVars; i++) {
            vars.add(new ContinuousVariable(Integer.toString(i)));
            //			vars.add(new DiscreteVariable(Integer.toString(i)));

        }
        System.out.println("Making dag");
        return GraphUtils.randomGraphRandomForwardEdges(vars, numLatentConfounders, numEdges, 30, 15, 15, false, true);//randomGraphRandomForwardEdges(vars, 0,numEdges);
    }

    private static Graph makeDAG(int numVars, double edgesPerNode){
        final int numEdges = (int) (numVars * edgesPerNode);

        System.out.println("Making list of vars");

        List<Node> vars = new ArrayList<>();

        for (int i = 0; i < numVars; i++) {
            vars.add(new DiscreteVariable(Integer.toString(i)));
        }

        System.out.println("Making dag");
        return GraphUtils.randomGraph(vars, 0,numEdges, 30, 15, 15, false);//randomGraphRandomForwardEdges(vars, 0,numEdges);
    }
}
