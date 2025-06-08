package org.jbinder.graph;

import java.util.*;

public class DAG<T> {
    // Underlying DAG map
    // T can be mapped to multiple T
    Map<T, Collection<T>> dag = new HashMap<>();

    // There's a single root to our tree, always.
    T root;

    public void DAG() {
    }

    public void saveRoot(T root, Collection<T> children) {
        this.root = root;
        children.forEach(child -> put(root, child));
    }

    public void put(T source, T target) {
        dag.computeIfAbsent(source, k -> new HashSet<>()).add(target);
    }

    public List<T> getIncoming(T node) {
        var entrySet = dag.entrySet();
        return entrySet.stream()
            .filter(e -> e.getValue().contains(node))
            .map(Map.Entry::getKey)
            .toList();
    }

    public T getRoot() {
        return root;
    }

    // Kahn's
    public List<T> sort() {
        List<T> sorted = new ArrayList<>();

        // Keep track of in-degrees
        Map<T, Integer> inDegrees = new HashMap<>();

        for (Collection<T> neighbour: dag.values()) {
            neighbour.forEach(n -> inDegrees.merge(n, 1, Integer::sum));
        }

        // Start from root
        Queue<T> queue = new ArrayDeque<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            T node = queue.poll();
            sorted.add(node);

            Collection<T> neighbours = dag.get(node);
            if (neighbours != null) {
                for (T neighbour: neighbours) {
                    if (inDegrees.merge(neighbour, -1, Integer::sum) == 0) {
                        queue.offer(neighbour);
                    }
                }
            }
        }

        return sorted;
    }

    @Override
    public String toString() {
        return dag.toString();
    }
}
