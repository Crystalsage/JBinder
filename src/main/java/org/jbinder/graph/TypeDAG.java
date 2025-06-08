package org.jbinder.graph;

import org.jbinder.xsd.XsdElement;

import java.util.ArrayList;
import java.util.List;

public class TypeDAG {
    DAG<XsdElement> xsdDag = new DAG<>();

    public TypeDAG() {}

    public void put(XsdElement source, XsdElement target) {
        xsdDag.put(source, target);
    }

    public void saveRoot(XsdElement root) {
        xsdDag.saveRoot(root, new ArrayList<>());
    }

    public List<XsdElement> sort() {
        // Dependants first
        return xsdDag.sort().reversed();
    }
}
