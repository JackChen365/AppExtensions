package jack.android.embedfunction;

import android.util.Log;
import cache.test.LRUCache;
import jack.android.embedfunction.api.Decorate;
import java.util.Deque;
import java.util.LinkedList;

@Decorate(target = LRUCache.class)
public class LRUCache_Decorate<E> {
    private static final String TAG = "LRUCache_Decorate";
    private Deque<E> deque;
    private OnMessageListener listener;

    // maximum capacity of cache
    private final int capacity;

    public LRUCache_Decorate(int capacity) {
        this.capacity = capacity;
        deque = new LinkedList<>();
        println("LRUCache_Decorate<init>");
        setMessageListener(new OnMessageListener() {
            @Override public void onMessage(final String message) {
                Log.i(TAG,"onMessage:"+message);
            }
        });
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
        if(null != listener){
            listener.onMessage(message);
        }
    }

    public void setMessageListener(OnMessageListener listener){
        this.listener = listener;
    }

    public interface OnMessageListener {
        void onMessage(String message);
    }
}
