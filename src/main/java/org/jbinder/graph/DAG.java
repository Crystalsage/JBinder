package org.jbinder.graph;

import java.util.*;

public class DAG<T> {
    // Underlying DAG map
    // T can be mapped to multiple T
    Map<T, Collection<T>> dag = new HashMap<>();

    public void DAG() {
    }

    public void put(T source, T target) {
        dag.computeIfAbsent(source, k -> new HashSet<>()).add(target);
    }

    public Collection<T> getIncoming(T node) {
        var entrySet = dag.entrySet();
        return entrySet.stream()
                .filter(e -> e.getValue().contains(node))
                .map(Map.Entry::getKey)
                .toList();
    }

    @Override
    public String toString() {
        return dag.toString();
    }
}
