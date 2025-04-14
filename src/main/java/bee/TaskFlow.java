/*
 * Copyright (C) 2025 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import kiss.Managed;

@Managed(OnProject.class)
class TaskFlow {

    /** The current processing task name holder. */
    static final InheritableThreadLocal<String> current = new InheritableThreadLocal<>();

    /** Internal class to hold flow context for each task node. */
    private static class Flow {
        final List<String> children = new ArrayList<>();

        volatile Status status = Status.RUNNING;

        volatile Thread thread;
    }

    // Unified map holding the flow context for each task node
    private final Map<String, Flow> flows = new ConcurrentHashMap<>();

    static {
        current.set("");
    }

    public TaskFlow() {
        flows.computeIfAbsent("", k -> new Flow());
    }

    void stepInto(String child) {
        String parentName = current.get();
        Flow childFlow = flows.computeIfAbsent(child, k -> new Flow());
        childFlow.thread = Thread.currentThread();

        flows.computeIfPresent(parentName, (key, parentFlow) -> {
            parentFlow.children.add(child);
            return parentFlow;
        });

        current.set(child);
    }

    void status(String name, Status status) {
        flows.computeIfPresent(name, (key, flow) -> {
            Status currentStatus = flow.status;
            if (currentStatus != Status.SUCCESS && currentStatus != Status.FAILURE && currentStatus != Status.ABORT) {
                flow.status = status;
            }
            return flow;
        });
    }

    /**
     * Aborts the specified task and all its descendant tasks recursively.
     * Sets the status of tasks currently in RUNNING state to ABORT.
     * Attempts to interrupt the threads associated with the affected tasks.
     * Tasks already in SUCCESS, FAILURE, or ABORT state are ignored.
     *
     * @param name The name of the task to start the abort process from.
     */
    void abort(String name) {
        Flow flow = flows.get(name);
        if (flow != null) {
            flow.status = Status.ABORT;

            // I.signal(flow).recurseMap(signal -> signal.flatIterable(x -> x.children));
            abortRecursive(name, flow);
        }
        // Removed: System.err.println("Abort target task not found: " + name);
    }

    /**
     * Recursive helper method to abort a task and its descendants.
     * Only processes tasks that are currently RUNNING.
     *
     * @param nodeName The name of the current node being processed.
     * @param nodeFlow The flow context object of the current node.
     */
    private void abortRecursive(String nodeName, Flow nodeFlow) {
        Status currentStatus = nodeFlow.status;

        // Only proceed if the task is currently RUNNING
        if (currentStatus == Status.RUNNING) {
            // 1. Update the status to ABORT
            nodeFlow.status = Status.ABORT;
            // Removed: Debugging logs

            // 2. Attempt to interrupt the associated thread
            Thread nodeThread = nodeFlow.thread;
            if (nodeThread != null && nodeThread.isAlive()) {
                try {
                    nodeThread.interrupt();
                } catch (SecurityException e) {
                    // Removed: Error log for inability to interrupt
                    // Optionally, rethrow as an unchecked exception or log using a proper logging
                    // framework
                }
            }

            // 3. Recursively call for children
            List<String> children = nodeFlow.children;
            if (children != null) {
                for (String childName : new ArrayList<>(children)) { // Use snapshot
                    Flow childFlow = flows.get(childName);
                    if (childFlow != null) {
                        abortRecursive(childName, childFlow);
                    }
                }
            }
        }
        // Removed: Debugging log for skipping abort
    }

    /**
     * Generates an ASCII art representation of the task tree structure using tabs for indentation.
     *
     * @return A string containing the ASCII tree diagram with tab indentation.
     */
    public String visualizeTaskTree() {
        StringBuilder builder = new StringBuilder();
        Flow rootFlow = flows.get("");
        if (rootFlow == null) {
            // Consider throwing an exception or returning a more informative error string
            // if the root context is critical and should always exist.
            return "[Error: Root flow ('') is missing]";
        }

        List<String> rootChildren = rootFlow.children;

        for (int i = 0; i < rootChildren.size(); i++) {
            String childName = rootChildren.get(i);
            boolean isLastChild = (i == rootChildren.size() - 1);
            buildTreeStringRecursive(childName, builder, "", isLastChild, true);
        }

        if (builder.length() > 0 && builder.charAt(builder.length() - 1) == '\n') {
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }

    /**
     * Recursive helper method to build the ASCII tree string with tab indentation.
     *
     * @param nodeName The name of the current node to process.
     * @param builder The StringBuilder to append the output to.
     * @param indent The indentation string prefix for the current level's children.
     * @param isLast A boolean indicating if this node is the last child of its parent.
     * @param isTopLevel A boolean indicating if this is a direct child of the implicit root ("").
     */
    private void buildTreeStringRecursive(String nodeName, StringBuilder builder, String indent, boolean isLast, boolean isTopLevel) {
        Flow nodeFlow = flows.get(nodeName);

        if (nodeFlow == null) {
            String prefix = isTopLevel ? "" : (indent + (isLast ? "└─ " : "├─ "));
            // Return an error indication, or skip the node silently
            builder.append(prefix).append(nodeName).append(" [Flow Missing!]\n");
            return;
        }

        String currentIndent;
        String childIndent;
        String nodeDisplay;

        if (isTopLevel) {
            currentIndent = "";
            childIndent = "";
            nodeDisplay = nodeName;
        } else {
            currentIndent = indent;
            if (isLast) {
                nodeDisplay = currentIndent + "└─ " + nodeName;
                childIndent = indent + "\t";
            } else {
                nodeDisplay = currentIndent + "├─ " + nodeName;
                childIndent = indent + "│\t";
            }
        }

        builder.append(nodeDisplay).append("  ").append(nodeFlow.status).append("\n");

        List<String> children = nodeFlow.children;
        List<String> childrenToIterate = (children != null) ? children : Collections.emptyList();

        for (int i = 0; i < childrenToIterate.size(); i++) {
            String childName = childrenToIterate.get(i);
            boolean isLastChild = (i == childrenToIterate.size() - 1);
            buildTreeStringRecursive(childName, builder, childIndent, isLastChild, false);
        }
    }
}