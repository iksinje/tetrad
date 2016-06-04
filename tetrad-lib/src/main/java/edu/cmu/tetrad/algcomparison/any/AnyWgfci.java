package edu.cmu.tetrad.algcomparison.any;

import edu.cmu.tetrad.algcomparison.ComparisonAlgorithm;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.DagToPag;
import edu.cmu.tetrad.search.WGfci;

import java.util.Map;

/**
 * Created by jdramsey on 6/4/16.
 */
public class AnyWgfci implements ComparisonAlgorithm {
    public Graph search(DataSet dataSet, Map<String, Number> parameters) {
        WGfci fgs = new WGfci(dataSet);
        fgs.setPenaltyDiscount(parameters.get("penaltyDiscount").doubleValue());
        return fgs.search();
    }

    public String getName() {
        return "WGFCI";
    }

    @Override
    public Graph getComparisonGraph(Graph dag) {
        return new DagToPag(dag).convert();
    }
}
