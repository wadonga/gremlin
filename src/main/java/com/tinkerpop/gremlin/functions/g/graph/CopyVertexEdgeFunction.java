package com.tinkerpop.gremlin.functions.g.graph;

import com.tinkerpop.blueprints.pgm.*;
import com.tinkerpop.gremlin.compiler.context.GremlinScriptContext;
import com.tinkerpop.gremlin.compiler.operations.Operation;
import com.tinkerpop.gremlin.compiler.types.Atom;
import com.tinkerpop.gremlin.functions.AbstractFunction;
import com.tinkerpop.gremlin.functions.FunctionHelper;
import com.tinkerpop.pipes.PipeHelper;

import java.util.List;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class CopyVertexEdgeFunction extends AbstractFunction<Object> {

    private final static String FUNCTION_NAME = "copy-ve";

    // g:copy-ve(graph?,element,string?)

    public Atom<Object> compute(final List<Operation> arguments, final GremlinScriptContext context) throws RuntimeException {
        final int size = arguments.size();
        if (size > 4 || size == 0)
            throw new RuntimeException(this.createUnsupportedArgumentMessage());

        final Graph graph;
        List<Object> objects = FunctionHelper.generateObjects(arguments);
        if (objects.get(0) instanceof Graph)
            graph = (Graph) objects.get(0);
        else
            graph = FunctionHelper.getGlobalGraph(context);

        final Element element;
        final String vertexIndexName;
        final String uniqueProperty;

        if (size == 1) {
            element = (Element) objects.get(0);
            uniqueProperty = "_id";
            vertexIndexName = Index.VERTICES;
        } else if (size == 2) {
            // todo fix
            Object object = objects.get(0);
            if (object instanceof Graph) {
                element = (Element) objects.get(1);
                uniqueProperty = "_id";
                vertexIndexName = Index.VERTICES;
            } else {
                throw new RuntimeException(this.createUnsupportedArgumentMessage("When providing an index, provide a property key to use"));
            }
        } else if (size == 3) {
            if (objects.get(0) instanceof Graph) {
                throw new RuntimeException(this.createUnsupportedArgumentMessage("When providing an index, provide a property key to use"));
            } else {
                element = (Element) objects.get(0);
                vertexIndexName = (String) objects.get(1);
                uniqueProperty = (String) objects.get(2);
            }
        } else {
            element = (Element) objects.get(1);
            vertexIndexName = (String) objects.get(2);
            uniqueProperty = (String) objects.get(3);
        }
        if (element instanceof Vertex) {
            Vertex v1 = (Vertex) element;
            Vertex v2 = CopyVertexEdgeFunction.getVertex(graph, vertexIndexName, v1, uniqueProperty);
            if (null == v2) {
                v2 = graph.addVertex(v1.getId());
                FunctionHelper.copyElementProperties(v1, v2);
            }

        } else if (element instanceof Edge) {
            Edge e1 = (Edge) element;
            Vertex v1Out = e1.getOutVertex();
            Vertex v1In = e1.getInVertex();
            Vertex v2Out = CopyVertexEdgeFunction.getVertex(graph, vertexIndexName, v1Out, uniqueProperty);
            Vertex v2In = CopyVertexEdgeFunction.getVertex(graph, vertexIndexName, v1In, uniqueProperty);
            if (null != v2Out && null != v2In) {
                for (Edge e : v2Out.getOutEdges()) {
                    if (e.getInVertex().equals(v2In) && e.getLabel().equals(e1.getLabel())) {
                        return new Atom<Object>(null);
                    }
                }
            }

            if (null == v2Out) {
                v2Out = graph.addVertex(v1Out.getId());
                FunctionHelper.copyElementProperties(v1Out, v2Out);
            }
            if (null == v2In) {
                v2In = graph.addVertex(v1In.getId());
                FunctionHelper.copyElementProperties(v1In, v2In);
            }

            Edge e2 = graph.addEdge(e1.getId(), v2Out, v2In, e1.getLabel());
            FunctionHelper.copyElementProperties(e1, e2);
        }

        return new Atom<Object>(null);

    }

    private static Vertex getVertex(Graph graph, String vertexIndexName, Vertex v1, String uniqueProperty) {
        Vertex v2 = null;
        if (uniqueProperty.equals("_id")) {
            v2 = graph.getVertex(v1.getId());
        } else {
            Iterable<Vertex> results = ((IndexableGraph) graph).getIndex(vertexIndexName, Vertex.class).get(uniqueProperty, v1.getProperty(uniqueProperty));
            long count = PipeHelper.counter(results.iterator());
            if (count > 1) {
                throw new RuntimeException("There is more than one vertex in target graph that has that unique property value");
            } else if (count != 0) {
                v2 = results.iterator().next();
            }
        }
        return v2;
    }

    public String getFunctionName() {
        return FUNCTION_NAME;
    }
}