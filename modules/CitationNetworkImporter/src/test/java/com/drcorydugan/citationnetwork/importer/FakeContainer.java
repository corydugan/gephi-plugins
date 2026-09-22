/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.importer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.gephi.io.importer.api.ContainerLoader;
import org.gephi.io.importer.api.EdgeDraft;
import org.gephi.io.importer.api.ElementDraft;
import org.gephi.io.importer.api.NodeDraft;

/**
 * A container loader that records what was written to it.
 *
 * <p>The interfaces involved carry far more methods than this needs, so the
 * drafts are dynamic proxies: every call is recorded, and anything else
 * returns a harmless default.</p>
 */
final class FakeContainer {

    final Map<String, Map<String, Object>> nodeValues = new LinkedHashMap<>();
    final Map<String, String> nodeLabels = new LinkedHashMap<>();
    final Set<String> nodeColumns = new LinkedHashSet<>();
    final List<String[]> edges = new ArrayList<>();
    private final Map<String, NodeDraft> nodesById = new LinkedHashMap<>();
    private final Map<Object, String> idsByDraft = new LinkedHashMap<>();

    ContainerLoader loader() {
        return (ContainerLoader) proxy(ContainerLoader.class, (proxy, method, args) -> {
            switch (method.getName()) {
                case "factory":
                    return factory();
                case "addNodeColumn":
                    nodeColumns.add((String) args[0]);
                    return null;
                case "addNode":
                    return null;
                case "getNode":
                    return nodesById.get((String) args[0]);
                case "addEdge":
                    return null;
                default:
                    return null;
            }
        });
    }

    private ElementDraft.Factory factory() {
        return (ElementDraft.Factory) proxy(ElementDraft.Factory.class, (proxy, method, args) -> {
            String id = args != null && args.length > 0 ? (String) args[0] : "";
            if (method.getName().equals("newNodeDraft")) {
                NodeDraft node = (NodeDraft) proxy(NodeDraft.class, nodeHandler(id));
                nodesById.put(id, node);
                idsByDraft.put(node, id);
                nodeValues.put(id, new LinkedHashMap<>());
                return node;
            }
            return proxy(EdgeDraft.class, edgeHandler());
        });
    }

    private InvocationHandler nodeHandler(String id) {
        return (proxy, method, args) -> {
            if (method.getName().equals("setLabel")) {
                nodeLabels.put(id, (String) args[0]);
            } else if (method.getName().equals("setValue")) {
                // The real container throws on a null value, so this one does too.
                if (args[1] == null) {
                    throw new NullPointerException(
                            "Value for key '" + args[0] + "' can't be null");
                }
                nodeValues.get(id).put((String) args[0], args[1]);
            } else if (method.getName().equals("getId")) {
                return id;
            }
            return defaultFor(method.getReturnType());
        };
    }

    private InvocationHandler edgeHandler() {
        String[] ends = new String[2];
        edges.add(ends);
        return (proxy, method, args) -> {
            if (method.getName().equals("setSource")) {
                ends[0] = idsByDraft.get(args[0]);
            } else if (method.getName().equals("setTarget")) {
                ends[1] = idsByDraft.get(args[0]);
            }
            return defaultFor(method.getReturnType());
        };
    }

    private static Object proxy(Class<?> type, InvocationHandler handler) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (p, method, args) -> {
                    if (method.getName().equals("equals")) {
                        return p == args[0];
                    }
                    if (method.getName().equals("hashCode")) {
                        return System.identityHashCode(p);
                    }
                    if (method.getName().equals("toString")) {
                        return type.getSimpleName() + "@stub";
                    }
                    return handler.invoke(p, method, args);
                });
    }

    private static Object defaultFor(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == float.class) {
            return 0f;
        }
        if (type == double.class) {
            return 0d;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == void.class) {
            return null;
        }
        return 0;
    }
}
