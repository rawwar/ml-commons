package org.opensearch.ml.engine.clustering;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.ml.common.dataframe.ColumnMeta;
import org.opensearch.ml.common.dataframe.ColumnType;
import org.opensearch.ml.common.dataframe.DataFrame;
import org.opensearch.ml.common.dataframe.DataFrameBuilder;
import org.opensearch.ml.common.parameter.MLParameter;
import org.opensearch.ml.common.parameter.MLParameterBuilder;
import org.opensearch.ml.engine.Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class KMeansTest {
    private List<MLParameter> parameters = new ArrayList<>();
    private KMeans kMeans;
    private DataFrame trainDataFrame;
    private DataFrame predictionDataFrame;
    private int trainSize = 100;
    private int predictionSize = 10;

    @Before
    public void setUp() {
        parameters.add(MLParameterBuilder.parameter("seed", 1));
        parameters.add(MLParameterBuilder.parameter("num_threads", 1));
        parameters.add(MLParameterBuilder.parameter("distance_type", 0));
        parameters.add(MLParameterBuilder.parameter("iterations", 10));
        parameters.add(MLParameterBuilder.parameter("k", 2));

        kMeans = new KMeans(parameters);
        constructKMeansTrainDataFrame();
        constructKMeansPredictionDataFrame();
    }

    @Test
    public void predict() {
        Model model = kMeans.train(trainDataFrame);
        DataFrame predictions = kMeans.predict(predictionDataFrame, model);
        Assert.assertEquals(predictionSize, predictions.size());
        predictions.forEach(row -> Assert.assertTrue(row.getValue(0).intValue() == 0 || row.getValue(0).intValue() == 1));
    }

    @Test
    public void train() {
        Model model = kMeans.train(trainDataFrame);
        Assert.assertEquals("KMeans", model.getName());
        Assert.assertEquals(1, model.getVersion());
        Assert.assertNotNull(model.getContent());
    }

    private void constructKMeansPredictionDataFrame() {
        predictionDataFrame = constructKMeansDataFrame(predictionSize);
    }

    private void constructKMeansTrainDataFrame() {
        trainDataFrame = constructKMeansDataFrame(trainSize);
    }

    private DataFrame constructKMeansDataFrame(int size) {
        ColumnMeta[] columnMetas = new ColumnMeta[]{new ColumnMeta("f1", ColumnType.DOUBLE), new ColumnMeta("f2", ColumnType.DOUBLE)};
        DataFrame dataFrame = DataFrameBuilder.emptyDataFrame(columnMetas);

        Random random = new Random(1);
        MultivariateNormalDistribution g1 = new MultivariateNormalDistribution(new JDKRandomGenerator(random.nextInt()),
                new double[]{0.0, 0.0}, new double[][]{{2.0, 1.0}, {1.0, 2.0}});
        MultivariateNormalDistribution g2 = new MultivariateNormalDistribution(new JDKRandomGenerator(random.nextInt()),
                new double[]{10.0, 10.0}, new double[][]{{2.0, 1.0}, {1.0, 2.0}});
        MultivariateNormalDistribution[] normalDistributions = new MultivariateNormalDistribution[]{g1, g2};
        for (int i = 0; i < size; ++i) {
            int id = 0;
            if (Math.random() < 0.5) {
                id = 1;
            }
            double[] sample = normalDistributions[id].sample();
            dataFrame.appendRow(Arrays.stream(sample).boxed().toArray(Double[]::new));
        }

        return dataFrame;
    }
}