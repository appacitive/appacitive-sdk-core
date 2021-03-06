package com.appacitive.core.model;

import com.appacitive.core.AppacitiveConnection;
import com.appacitive.core.AppacitiveObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by sathley.
 */
public class AppacitiveGraphNode implements Serializable {

    public AppacitiveObjectBase object = null;

    public AppacitiveConnection connection = null;

    public Map<String, List<AppacitiveGraphNode>> children = new ConcurrentHashMap<String, List<AppacitiveGraphNode>>();

    public AppacitiveGraphNode parent = null;

    public List<AppacitiveGraphNode> getChildren(String name) {
        if (this.children.containsKey(name))
            return this.children.get(name);
        return new ArrayList<AppacitiveGraphNode>();

    }

    public void addChildNode(String name, AppacitiveGraphNode node) {
        if (this.children.containsKey(name) && this.children.get(name) != null)
            this.children.get(name).add(node);
        else {
            this.children.put(name, new ArrayList<AppacitiveGraphNode>());
            this.children.get(name).add(node);
        }
    }

}
