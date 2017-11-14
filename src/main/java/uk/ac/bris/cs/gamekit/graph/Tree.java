package uk.ac.bris.cs.gamekit.graph;


public class Tree<T> {
     private Fork<T> root;
     private Fork<T> selectedFork;
     public int depth;

     public Tree(T rootData) {
         root = new Fork<T>(rootData, null);
         selectedFork = root;
         depth = 0;
     }

     public boolean isRoot(){
         if(selectedFork == root){
             return true;
         }
         return false;
     }

     public Fork getSelectedFork(){
         return selectedFork;
     }

     public void selectRoot(){
         selectedFork = root;
     }

     public void selectParent(){
         selectedFork = root.parent();
     }

     public void selectChild(int index){
         if(index > selectedFork.children().size() - 1){
             throw new IndexOutOfBoundsException("Error: Child at selected index does not exist.");
         }
         selectedFork = selectedFork.children().get(index);
     }

     public boolean contains(T value){
        if(root.value().equals(value)){
            return true;
        }else{
            for(Fork<T> f : root.children()){
                if(contains(f, value)) {
                    return true;
                }
            }
        }
        return false;
     }

     public boolean contains(Fork<T> fork, T value){
         if(fork.value().equals(value)){
             return true;
         }else{
             for(Fork<T> f : fork.children()){
                 contains(f, value);
             }
         }
         return false;
     }

     public int depth(T value){
         contains(value);
         int depth = 0;
         while(selectedFork!=root){
             depth ++;
         }
         return depth;
     }

 }