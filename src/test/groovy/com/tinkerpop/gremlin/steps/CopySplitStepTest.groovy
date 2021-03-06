package com.tinkerpop.gremlin.steps

import com.tinkerpop.blueprints.pgm.Graph
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraphFactory
import com.tinkerpop.gremlin.Gremlin
import com.tinkerpop.pipes.Pipe
import junit.framework.TestCase

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class CopySplitStepTest extends TestCase {

    public void testIsPipe() throws Exception {
        Gremlin.load();
        Graph g = TinkerGraphFactory.createTinkerGraph();
        assertTrue(g.V.copySplit(_(), _()).fairMerge() instanceof Pipe)
        assertTrue(g.V.copySplit(_(), _()).fairMerge instanceof Pipe)
        assertTrue(g.V.copySplit(_(), _()).exhaustMerge() instanceof Pipe)
        assertTrue(g.V.copySplit(_(), _()).exhaustMerge instanceof Pipe)
    }

    public void testFairMerge() {
        Gremlin.load();
        Graph g = TinkerGraphFactory.createTinkerGraph();
        int c = 0;
        g.V.copySplit(_().outE.inV, _().outE.inV).fairMerge.sideEffect {c++} >> -1
        assertEquals(c, g.E.count() * 2);

        def results = [];
        g.v(1)._().copySplit(_().outE('knows').label, _().outE('created').label).fairMerge >> results;
        assertEquals(results[0], 'knows');
        assertEquals(results[1], 'created');
        assertEquals(results[2], 'knows');
        assertEquals(results.size(), 3);
    }

    public void testExhaustiveMerge() {
        Gremlin.load();
        Graph g = TinkerGraphFactory.createTinkerGraph();
        int c = 0;
        g.V.copySplit(_().outE.inV, _().outE.inV).exhaustMerge.sideEffect {c++} >> -1
        assertEquals(c, g.E.count() * 2);

        def results = [];
        g.v(1)._.copySplit(_().outE('knows').label, _().outE('created').label).exhaustMerge >> results;
        assertEquals(results[0], 'knows');
        assertEquals(results[1], 'knows');
        assertEquals(results[2], 'created');
        assertEquals(results.size(), 3);
    }
}
