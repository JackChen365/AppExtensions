package jack.android.embedfunction;

import jack.android.embedfunction.api.Decorate;
import java.util.Deque;
import java.util.LinkedList;
import cache.test.LRUCache;

/**
 * Decorate the class that inside the jar file.
 * @param <E>
 */
@Decorate(target = LRUCache.class)
public class LRUCacheDecorate<E> {
    private Deque<E> deque;

    // maximum capacity of cache
    private final int capacity;

    public LRUCacheDecorate(int capacity) {
        this.capacity = capacity;
        deque = new LinkedList<>();
        println("LRUCacheDecorate<init>");
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
        println("push:"+e);
    }

    public E pop(){
        E value = deque.pop();
        println("pop:"+value);
        return value;
    }

    void println(String message){
        System.out.println(message);
    }
}
