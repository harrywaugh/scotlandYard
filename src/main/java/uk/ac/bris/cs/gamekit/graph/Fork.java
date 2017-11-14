package uk.ac.bris.cs.gamekit.graph;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

public class Fork<V>  extends Node implements Serializable{
    private ArrayList<Fork<V>> children;
    private Fork<V> parent;

    public Fork(V value, Fork<V> parent) {
        super(value);
        children = new ArrayList<>();
        this.parent = parent;
    }

    public Fork<V> parent(){
        return this.parent;
    }

    public void addChild(V val){
        children.add(new Fork<V>(val, this));
    }

    public ArrayList<Fork<V>> children(){
        return children;
    }
}
