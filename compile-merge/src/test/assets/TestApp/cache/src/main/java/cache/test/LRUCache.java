package cache.test;

import java.util.Deque;
import java.util.LinkedList;

public class LRUCache<E> {
    private Deque<E> deque;
  
    // maximum capacity of cache  
    private final int capacity;
  
    public LRUCache(int capacity) {
        this.capacity = capacity;
        deque = new LinkedList<>();
    }

    /**
     * Put a new element into the collection.
     * @param e
     */
    public void push(E e) {
        if(deque.contains(e)){
            deque.remove(e);
        } else {
            if(deque.size() == capacity){
                deque.removeLast();
            }
        }
        deque.push(e);
    }

    public E pop(){
        return deque.pop();
    }
}  