/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gaffer.accumulostore.operation.spark.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import gaffer.commonutil.CommonConstants;
import gaffer.data.element.Edge;
import gaffer.data.element.Element;
import gaffer.data.element.Entity;
import gaffer.data.elementdefinition.view.View;
import gaffer.graph.Graph;
import gaffer.operation.GetOperation;
import gaffer.operation.OperationException;
import gaffer.operation.data.EdgeSeed;
import gaffer.operation.data.EntitySeed;
import gaffer.operation.impl.add.AddElements;
import gaffer.operation.simple.spark.GetJavaRDDOfElements;
import gaffer.user.User;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.junit.Test;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GetJavaRDDOfElementsHandlerTest {

    private final static String ENTITY_GROUP = "BasicEntity";
    private final static String EDGE_GROUP = "BasicEdge";

    @Test
    public void checkGetCorrectElementsInJavaRDDForEntitySeed() throws OperationException, IOException {
        final Graph graph1 = new Graph.Builder()
                .addSchema(getClass().getResourceAsStream("/schema/dataSchema.json"))
                .addSchema(getClass().getResourceAsStream("/schema/dataTypes.json"))
                .addSchema(getClass().getResourceAsStream("/schema/storeTypes.json"))
                .storeProperties(getClass().getResourceAsStream("/store.properties"))
                .build();

        final List<Element> elements = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final Entity entity = new Entity(ENTITY_GROUP);
            entity.setVertex("" + i);

            final Edge edge1 = new Edge(EDGE_GROUP);
            edge1.setSource("" + i);
            edge1.setDestination("B");
            edge1.setDirected(false);
            edge1.putProperty("count", 2);

            final Edge edge2 = new Edge(EDGE_GROUP);
            edge2.setSource("" + i);
            edge2.setDestination("C");
            edge2.setDirected(false);
            edge2.putProperty("count", 4);

            elements.add(edge1);
            elements.add(edge2);
            elements.add(entity);
        }
        final User user = new User();
        graph1.execute(new AddElements(elements), user);

        final SparkConf sparkConf = new SparkConf()
                .setMaster("local")
                .setAppName("testCheckGetCorrectElementsInJavaRDDForEntitySeed")
                .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .set("spark.kryo.registrator", "gaffer.serialisation.kryo.Registrator")
                .set("spark.driver.allowMultipleContexts", "true");
        final JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);

        // Create Hadoop configuration and serialise to a string
        final Configuration configuration = new Configuration();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        configuration.write(new DataOutputStream(baos));
        final String configurationString = new String(baos.toByteArray(), CommonConstants.UTF_8);

        // Check get correct edges for "1"
        GetJavaRDDOfElements<EntitySeed> rddQuery = new GetJavaRDDOfElements.Builder<EntitySeed>()
                .javaSparkContext(sparkContext)
                .seeds(Collections.singleton(new EntitySeed("1")))
                .build();
        rddQuery.addOption(AbstractGetRDDOperationHandler.HADOOP_CONFIGURATION_KEY, configurationString);
        JavaRDD<Element> rdd = graph1.execute(rddQuery, user);
        if (rdd == null) {
            fail("No RDD returned");
        }
        final Set<Element> results = new HashSet<>(rdd.collect());
        final Set<Element> expectedElements = new HashSet<>();
        final Entity entity1 = new Entity(ENTITY_GROUP);
        entity1.setVertex("1");
        final Edge edge1B = new Edge(EDGE_GROUP);
        edge1B.setSource("1");
        edge1B.setDestination("B");
        edge1B.setDirected(false);
        edge1B.putProperty("count", 2);
        final Edge edge1C = new Edge(EDGE_GROUP);
        edge1C.setSource("1");
        edge1C.setDestination("C");
        edge1C.setDirected(false);
        edge1C.putProperty("count", 4);
        expectedElements.add(entity1);
        expectedElements.add(edge1B);
        expectedElements.add(edge1C);
        assertEquals(expectedElements, results);

        // Check get correct edges for "1" when specify entities only
        rddQuery = new GetJavaRDDOfElements.Builder<EntitySeed>()
                .javaSparkContext(sparkContext)
                .seeds(Collections.singleton(new EntitySeed("1")))
                .view(new View.Builder()
                        .entity(ENTITY_GROUP)
                        .build())
                .build();
        rddQuery.addOption(AbstractGetRDDOperationHandler.HADOOP_CONFIGURATION_KEY, configurationString);
        rdd = graph1.execute(rddQuery, user);
        if (rdd == null) {
            fail("No RDD returned");
        }
        results.clear();
        results.addAll(rdd.collect());
        expectedElements.clear();
        expectedElements.add(entity1);
        assertEquals(expectedElements, results);

        // Check get correct edges for "1" when specify edges only
        rddQuery = new GetJavaRDDOfElements.Builder<EntitySeed>()
                .javaSparkContext(sparkContext)
                .seeds(Collections.singleton(new EntitySeed("1")))
                .view(new View.Builder()
                        .edge(EDGE_GROUP)
                        .build())
                .build();
        rddQuery.addOption(AbstractGetRDDOperationHandler.HADOOP_CONFIGURATION_KEY, configurationString);
        rdd = graph1.execute(rddQuery, user);
        if (rdd == null) {
            fail("No RDD returned");
        }
        results.clear();
        results.addAll(rdd.collect());
        expectedElements.clear();
        expectedElements.add(edge1B);
        expectedElements.add(edge1C);
        assertEquals(expectedElements, results);

        // Check get correct edges for "1" and "5"
        Set<EntitySeed> seeds = new HashSet<>();
        seeds.add(new EntitySeed("1"));
        seeds.add(new EntitySeed("5"));
        rddQuery = new GetJavaRDDOfElements.Builder<EntitySeed>()
                .javaSparkContext(sparkContext)
                .seeds(seeds)
                .build();
        rddQuery.addOption(AbstractGetRDDOperationHandler.HADOOP_CONFIGURATION_KEY, configurationString);
        rdd = graph1.execute(rddQuery, user);
        if (rdd == null) {
            fail("No RDD returned");
        }
        results.clear();
        results.addAll(rdd.collect());
        final Entity entity5 = new Entity(ENTITY_GROUP);
        entity5.setVertex("5");
        final Edge edge5B = new Edge(EDGE_GROUP);
        edge5B.setSource("5");
        edge5B.setDestination("B");
        edge5B.setDirected(false);
        edge5B.putProperty("count", 2);
        final Edge edge5C = new Edge(EDGE_GROUP);
        edge5C.setSource("5");
        edge5C.setDestination("C");
        edge5C.setDirected(false);
        edge5C.putProperty("count", 4);
        expectedElements.clear();
        expectedElements.add(entity1);
        expectedElements.add(edge1B);
        expectedElements.add(edge1C);
        expectedElements.add(entity5);
        expectedElements.add(edge5B);
        expectedElements.add(edge5C);
        assertEquals(expectedElements, results);

        sparkContext.stop();
    }

    @Test
    public void checkGetCorrectElementsInRDDForEdgeSeed() throws OperationException, IOException {
        final Graph graph1 = new Graph.Builder()
                .addSchema(getClass().getResourceAsStream("/schema/dataSchema.json"))
                .addSchema(getClass().getResourceAsStream("/schema/dataTypes.json"))
                .addSchema(getClass().getResourceAsStream("/schema/storeTypes.json"))
                .storeProperties(getClass().getResourceAsStream("/store.properties"))
                .build();

        final List<Element> elements = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final Entity entity = new Entity(ENTITY_GROUP);
            entity.setVertex("" + i);

            final Edge edge1 = new Edge(EDGE_GROUP);
            edge1.setSource("" + i);
            edge1.setDestination("B");
            edge1.setDirected(false);
            edge1.putProperty("count", 2);

            final Edge edge2 = new Edge(EDGE_GROUP);
            edge2.setSource("" + i);
            edge2.setDestination("C");
            edge2.setDirected(false);
            edge2.putProperty("count", 4);

            elements.add(edge1);
            elements.add(edge2);
            elements.add(entity);
        }
        final User user = new User();
        graph1.execute(new AddElements(elements), user);

        final SparkConf sparkConf = new SparkConf()
                .setMaster("local")
                .setAppName("testCheckGetCorrectElementsInJavaRDDForEdgeSeed")
                .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .set("spark.kryo.registrator", "gaffer.serialisation.kryo.Registrator")
                .set("spark.driver.allowMultipleContexts", "true");
        final JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);

        // Create Hadoop configuration and serialise to a string
        final Configuration configuration = new Configuration();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        configuration.write(new DataOutputStream(baos));
        final String configurationString = new String(baos.toByteArray(), CommonConstants.UTF_8);

        // Check get correct edges for EdgeSeed 1 -> B
        GetJavaRDDOfElements<EdgeSeed> rddQuery = new GetJavaRDDOfElements.Builder<EdgeSeed>()
                .javaSparkContext(sparkContext)
                .seeds(Collections.singleton(new EdgeSeed("1", "B", false)))
                .setIncludeEdges(GetOperation.IncludeEdgeType.ALL)
                .setIncludeEntities(false)
                .build();
        rddQuery.addOption(AbstractGetRDDOperationHandler.HADOOP_CONFIGURATION_KEY, configurationString);
        JavaRDD<Element> rdd = graph1.execute(rddQuery, user);
        if (rdd == null) {
            fail("No RDD returned");
        }
        final Set<Element> results = new HashSet<>();
        results.addAll(rdd.collect());
        final Set<Element> expectedElements = new HashSet<>();
        final Edge edge1B = new Edge(EDGE_GROUP);
        edge1B.setSource("1");
        edge1B.setDestination("B");
        edge1B.setDirected(false);
        edge1B.putProperty("count", 2);
        expectedElements.add(edge1B);
        assertEquals(expectedElements, results);

        // Check get entity for 1 when query for 1 -> B and specify entities only
        rddQuery = new GetJavaRDDOfElements.Builder<EdgeSeed>()
                .javaSparkContext(sparkContext)
                .seeds(Collections.singleton(new EdgeSeed("1", "B", false)))
                .setIncludeEntities(true)
                .setIncludeEdges(GetOperation.IncludeEdgeType.NONE)
                .build();
        rddQuery.addOption(AbstractGetRDDOperationHandler.HADOOP_CONFIGURATION_KEY, configurationString);
        rdd = graph1.execute(rddQuery, user);
        if (rdd == null) {
            fail("No RDD returned");
        }
        results.clear();
        results.addAll(rdd.collect());
        expectedElements.clear();
        final Entity entity1 = new Entity(ENTITY_GROUP);
        entity1.setVertex("1");
        expectedElements.add(entity1);
        assertEquals(expectedElements, results);

        // Check get correct edges for 1 -> B when specify edges only
        rddQuery = new GetJavaRDDOfElements.Builder<EdgeSeed>()
                .javaSparkContext(sparkContext)
                .seeds(Collections.singleton(new EdgeSeed("1", "B", false)))
                .view(new View.Builder()
                        .edge(EDGE_GROUP)
                        .build())
                .setIncludeEntities(false)
                .setIncludeEdges(GetOperation.IncludeEdgeType.ALL)
                .build();
        rddQuery.addOption(AbstractGetRDDOperationHandler.HADOOP_CONFIGURATION_KEY, configurationString);
        rdd = graph1.execute(rddQuery, user);
        if (rdd == null) {
            fail("No RDD returned");
        }
        results.clear();
        results.addAll(rdd.collect());
        expectedElements.clear();
        expectedElements.add(edge1B);
        assertEquals(expectedElements, results);

        // Check get correct edges for 1 -> B and 5 -> C
        Set<EdgeSeed> seeds = new HashSet<>();
        seeds.add(new EdgeSeed("1", "B", false));
        seeds.add(new EdgeSeed("5", "C", false));
        rddQuery = new GetJavaRDDOfElements.Builder<EdgeSeed>()
                .javaSparkContext(sparkContext)
                .setIncludeEntities(false)
                .seeds(seeds)
                .build();
        rddQuery.addOption(AbstractGetRDDOperationHandler.HADOOP_CONFIGURATION_KEY, configurationString);
        rdd = graph1.execute(rddQuery, user);
        if (rdd == null) {
            fail("No RDD returned");
        }
        results.clear();
        results.addAll(rdd.collect());
        final Edge edge5C = new Edge(EDGE_GROUP);
        edge5C.setSource("5");
        edge5C.setDestination("C");
        edge5C.setDirected(false);
        edge5C.putProperty("count", 4);
        expectedElements.clear();
        expectedElements.add(edge1B);
        expectedElements.add(edge5C);
        assertEquals(expectedElements, results);

        sparkContext.stop();
    }
}
