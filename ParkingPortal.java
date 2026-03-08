// ================================================================
//  ParkingPortal.java  –  Urban Parking Portal  (Java DSA Backend)
//
//  DSA SYLLABUS COVERAGE
//  ─────────────────────────────────────────────────────────────
//  CO1 : Big-O/Ω/Θ analysis on every method, Binary Search for
//        slot lookup, Merge Sort & Quick Sort for slot reports,
//        Bubble Sort for small lists, empirical timing
//
//  CO2 : Singly Linked List  → Notification log
//        Doubly Linked List  → Booking history (fwd + bwd)
//        Circular Linked List→ Basement round-robin navigator
//        Operations : insert, delete, search, traverse,
//                     reverse, detect cycle
//
//  CO3 : Stack (array-based)  → Screen / navigation history
//        Queue (linked)       → Service-request queue (FIFO)
//        Circular Queue       → Slot event ring buffer
//        Deque                → Recent-search history
//        Min-Heap             → Priority slot assignment
//        Priority Queue       → VIP / timed service requests
//
//  CO4 : Hash Table – Chaining       → User store (admin + customer)
//        Hash Table – Open Addressing → Parking slot store
//        Java Collections (List, Queue, Deque, Map) for reports
// ================================================================

import java.util.*;

public class ParkingPortal {

    // ════════════════════════════════════════════════════════
    //  SHARED CONSTANTS
    // ════════════════════════════════════════════════════════
    static final int BASEMENTS   = 3;
    static final int ROWS        = 6;      // A-F
    static final int COLS        = 10;     // 1-10
    static final int SLOTS_EACH  = ROWS * COLS;   // 60

    // ════════════════════════════════════════════════════════
    //  DOMAIN OBJECTS
    // ════════════════════════════════════════════════════════
    enum SlotStatus { FREE, OCCUPIED, LEAVING }

    static class ParkingSlot {
        String   id;           // e.g. "1A"
        int      basement;
        SlotStatus status;
        String   occupiedBy;   // customer email
        String   occupiedByName;
        long     timestamp;    // epoch ms – used for priority

        ParkingSlot(String id, int basement) {
            this.id = id; this.basement = basement;
            this.status = SlotStatus.FREE;
        }

        @Override public String toString() {
            return String.format("[%s B%d %-8s %s]",
                id, basement, status, occupiedBy == null ? "" : occupiedBy);
        }
    }

    static class User {
        String id, name, email, phone;
        String role;           // "admin" or "customer"
        String mall, vehicle;
        String currentSlot;
        int    currentBasement;

        User(String id, String name, String email,
             String phone, String role) {
            this.id=id; this.name=name; this.email=email;
            this.phone=phone; this.role=role;
        }
        @Override public String toString() {
            return String.format("User{%s | %s | %s | slot=%s}",
                id, name, email, currentSlot == null ? "none" : currentSlot);
        }
    }

    static class ServiceRequest {
        int    id;
        String customerEmail, customerName;
        String type, message;
        int    priority;       // 1=highest
        String status;         // "pending" / "resolved"
        long   timestamp;

        ServiceRequest(int id, String email, String name,
                       String type, String msg, int priority) {
            this.id=id; this.customerEmail=email;
            this.customerName=name; this.type=type;
            this.message=msg; this.priority=priority;
            this.status="pending"; this.timestamp=System.currentTimeMillis();
        }
        @Override public String toString() {
            return String.format("SR{#%d P%d [%s] %s – %s}",
                id, priority, type, customerName, status);
        }
    }

    static class Notification {
        String message;
        long   timestamp;
        Notification(String m) { message=m; timestamp=System.currentTimeMillis(); }
        @Override public String toString() { return "📬 " + message; }
    }

    static class BookingRecord {
        String slotId; int basement; String customerEmail;
        long checkin, checkout;
        BookingRecord(String s, int b, String e, long ci) {
            slotId=s; basement=b; customerEmail=e; checkin=ci; checkout=0;
        }
        @Override public String toString() {
            return String.format("Booking{slot=%s B%d by=%s}", slotId, basement, customerEmail);
        }
    }

    // ════════════════════════════════════════════════════════
    //  CO2 ── SINGLY LINKED LIST
    //  Used for: Admin notification log  (newest at head)
    //  insert-front O(1) | traverse O(n) | search O(n) | delete O(n)
    // ════════════════════════════════════════════════════════
    static class SNode<T> {
        T data; SNode<T> next;
        SNode(T d) { data=d; }
    }

    static class SinglyLinkedList<T> {
        SNode<T> head;
        int size;

        /** INSERT at head – O(1)  [Ω(1), Θ(1)] */
        void insertFront(T data) {
            SNode<T> node = new SNode<>(data);
            node.next = head; head = node; size++;
        }

        /** INSERT at tail – O(n) */
        void insertEnd(T data) {
            SNode<T> node = new SNode<>(data);
            if (head == null) { head = node; size++; return; }
            SNode<T> cur = head;
            while (cur.next != null) cur = cur.next;
            cur.next = node; size++;
        }

        /** DELETE by index – O(n) */
        void delete(int index) {
            if (head == null || index < 0) return;
            if (index == 0) { head = head.next; size--; return; }
            SNode<T> cur = head;
            for (int i = 0; i < index-1 && cur.next != null; i++) cur = cur.next;
            if (cur.next != null) { cur.next = cur.next.next; size--; }
        }

        /** SEARCH – O(n) worst,  Ω(1) best */
        int search(T target) {
            SNode<T> cur = head; int idx = 0;
            while (cur != null) {
                if (cur.data.equals(target)) return idx;
                cur = cur.next; idx++;
            }
            return -1;
        }

        /** TRAVERSE – O(n)  returns list */
        List<T> toList() {
            List<T> result = new ArrayList<>();
            SNode<T> cur = head;
            while (cur != null) { result.add(cur.data); cur = cur.next; }
            return result;
        }

        /** REVERSE in-place – O(n), Space O(1) */
        void reverse() {
            SNode<T> prev = null, cur = head, next;
            while (cur != null) { next=cur.next; cur.next=prev; prev=cur; cur=next; }
            head = prev;
        }

        /** DETECT CYCLE – Floyd's two-pointer  O(n), Space O(1) */
        boolean detectCycle() {
            SNode<T> slow = head, fast = head;
            while (fast != null && fast.next != null) {
                slow = slow.next; fast = fast.next.next;
                if (slow == fast) return true;
            }
            return false;
        }

        int length() { return size; }
    }

    // ════════════════════════════════════════════════════════
    //  CO2 ── DOUBLY LINKED LIST
    //  Used for: Customer booking history (forward & backward nav)
    //  insert O(1) | delete with node O(1) | traverse O(n)
    // ════════════════════════════════════════════════════════
    static class DNode<T> {
        T data; DNode<T> prev, next;
        DNode(T d) { data=d; }
    }

    static class DoublyLinkedList<T> {
        DNode<T> head, tail;
        int size;

        /** INSERT at tail – O(1) */
        void insertEnd(T data) {
            DNode<T> node = new DNode<>(data);
            if (tail == null) { head = tail = node; }
            else { tail.next=node; node.prev=tail; tail=node; }
            size++;
        }

        /** INSERT at head – O(1) */
        void insertFront(T data) {
            DNode<T> node = new DNode<>(data);
            if (head == null) { head = tail = node; }
            else { node.next=head; head.prev=node; head=node; }
            size++;
        }

        /** DELETE tail – O(1) */
        T removeTail() {
            if (tail == null) return null;
            T val = tail.data;
            if (head == tail) { head = tail = null; }
            else { tail = tail.prev; tail.next = null; }
            size--; return val;
        }

        /** DELETE head – O(1) */
        T removeHead() {
            if (head == null) return null;
            T val = head.data;
            if (head == tail) { head = tail = null; }
            else { head = head.next; head.prev = null; }
            size--; return val;
        }

        /** TRAVERSE forward – O(n) */
        List<T> toListForward() {
            List<T> r = new ArrayList<>();
            DNode<T> cur = head;
            while (cur != null) { r.add(cur.data); cur = cur.next; }
            return r;
        }

        /** TRAVERSE backward – O(n)  [most recent booking first] */
        List<T> toListBackward() {
            List<T> r = new ArrayList<>();
            DNode<T> cur = tail;
            while (cur != null) { r.add(cur.data); cur = cur.prev; }
            return r;
        }

        /** SEARCH – O(n) */
        int search(T target) {
            DNode<T> cur = head; int idx=0;
            while (cur != null) {
                if (cur.data.equals(target)) return idx;
                cur=cur.next; idx++;
            }
            return -1;
        }

        int length() { return size; }
    }

    // ════════════════════════════════════════════════════════
    //  CO2 ── CIRCULAR SINGLY LINKED LIST
    //  Used for: Basement round-robin navigator (1→2→3→1→…)
    //  advance O(1) | insert O(n) | traverse O(n)
    // ════════════════════════════════════════════════════════
    static class CircularLinkedList {
        SNode<Integer> head, current;
        int size;

        void insert(int data) {
            SNode<Integer> node = new SNode<>(data);
            if (head == null) {
                head = node; node.next = head; current = head; size++; return;
            }
            SNode<Integer> tail = head;
            while (tail.next != head) tail = tail.next;
            tail.next = node; node.next = head; size++;
        }

        /** Move to next basement – O(1) */
        int next() { current = current.next; return current.data; }
        int getCurrent() { return current.data; }

        /** Is circular – O(n) verification */
        boolean isCircular() {
            if (head == null) return false;
            SNode<Integer> cur = head.next;
            while (cur != null && cur != head) cur = cur.next;
            return cur == head;
        }

        void print() {
            if (head == null) return;
            SNode<Integer> cur = head;
            System.out.print("Circular: ");
            do { System.out.print(cur.data + " → "); cur = cur.next; } while (cur != head);
            System.out.println("(back to " + head.data + ")");
        }
    }

    // ════════════════════════════════════════════════════════
    //  CO3 ── STACK  (Array-based)
    //  Used for: Screen / navigation history (back button)
    //  push O(1) | pop O(1) | peek O(1)
    // ════════════════════════════════════════════════════════
    static class Stack<T> {
        private Object[] data;
        private int top = -1;

        @SuppressWarnings("unchecked")
        Stack(int capacity) { data = new Object[capacity]; }

        /** PUSH – O(1) */
        void push(T val) {
            if (top == data.length-1) throw new RuntimeException("Stack Overflow");
            data[++top] = val;
        }

        /** POP – O(1) */
        @SuppressWarnings("unchecked")
        T pop() {
            if (isEmpty()) throw new NoSuchElementException("Stack Underflow");
            return (T) data[top--];
        }

        /** PEEK – O(1) */
        @SuppressWarnings("unchecked")
        T peek() {
            if (isEmpty()) throw new NoSuchElementException("Stack Empty");
            return (T) data[top];
        }

        boolean isEmpty() { return top == -1; }
        int     size()    { return top + 1; }

        void print(String label) {
            System.out.print(label + " [TOP→]: ");
            for (int i = top; i >= 0; i--) System.out.print(data[i] + "  ");
            System.out.println();
        }
    }

    // ════════════════════════════════════════════════════════
    //  CO3 ── QUEUE  (Linked-based)
    //  Used for: Service request queue (FIFO processing)
    //  enqueue O(1) | dequeue O(1)
    // ════════════════════════════════════════════════════════
    static class LinkedQueue<T> {
        private SNode<T> front, rear;
        private int size;

        /** ENQUEUE – O(1) */
        void enqueue(T data) {
            SNode<T> node = new SNode<>(data);
            if (rear == null) { front = rear = node; }
            else { rear.next = node; rear = node; }
            size++;
        }

        /** DEQUEUE – O(1) */
        T dequeue() {
            if (isEmpty()) throw new NoSuchElementException("Queue Empty");
            T val = front.data; front = front.next;
            if (front == null) rear = null;
            size--; return val;
        }

        T peek()      { if(isEmpty()) throw new NoSuchElementException(); return front.data; }
        boolean isEmpty() { return front == null; }
        int size()        { return size; }

        List<T> toList() {
            List<T> r = new ArrayList<>();
            SNode<T> cur = front;
            while (cur != null) { r.add(cur.data); cur = cur.next; }
            return r;
        }
    }

    // ════════════════════════════════════════════════════════
    //  CO3 ── CIRCULAR QUEUE  (Array-based)
    //  Used for: Slot event ring buffer (last 20 slot events)
    //  enqueue O(1) | dequeue O(1) – wraps around
    // ════════════════════════════════════════════════════════
    static class CircularQueue<T> {
        private Object[] data;
        private int front, rear, size, capacity;

        @SuppressWarnings("unchecked")
        CircularQueue(int cap) {
            capacity=cap; data=new Object[cap]; front=0; rear=cap-1; size=0;
        }

        boolean isFull()  { return size == capacity; }
        boolean isEmpty() { return size == 0; }

        /** ENQUEUE – O(1) – overwrites oldest if full (ring buffer) */
        void enqueue(T val) {
            if (isFull()) dequeue();   // drop oldest to make room
            rear = (rear+1) % capacity;
            data[rear] = val; size++;
        }

        /** DEQUEUE – O(1) */
        @SuppressWarnings("unchecked")
        T dequeue() {
            if (isEmpty()) throw new NoSuchElementException("Circular Queue Empty");
            T val = (T) data[front];
            front = (front+1) % capacity; size--; return val;
        }

        @SuppressWarnings("unchecked")
        List<T> toList() {
            List<T> r = new ArrayList<>();
            for (int i=0; i<size; i++) r.add((T)data[(front+i)%capacity]);
            return r;
        }
    }

    // ════════════════════════════════════════════════════════
    //  CO3 ── DEQUE  (Double-Ended Queue, Array-based)
    //  Used for: Recent slot search history (add/remove both ends)
    //  addFront O(1) | addRear O(1) | removeFront O(1) | removeRear O(1)
    // ════════════════════════════════════════════════════════
    static class Deque<T> {
        private Object[] data;
        private int front, rear, size, cap;

        @SuppressWarnings("unchecked")
        Deque(int capacity) {
            cap=capacity; data=new Object[cap];
            front=cap/2; rear=cap/2-1; size=0;
        }

        boolean isFull()  { return size == cap; }
        boolean isEmpty() { return size == 0; }

        void addFront(T val) {
            if (isFull()) removeRear();
            front = (front-1+cap)%cap; data[front]=val; size++;
        }

        void addRear(T val) {
            if (isFull()) removeFront();
            rear = (rear+1)%cap; data[rear]=val; size++;
        }

        @SuppressWarnings("unchecked")
        T removeFront() {
            if (isEmpty()) throw new NoSuchElementException();
            T val=(T)data[front]; front=(front+1)%cap; size--; return val;
        }

        @SuppressWarnings("unchecked")
        T removeRear() {
            if (isEmpty()) throw new NoSuchElementException();
            T val=(T)data[rear]; rear=(rear-1+cap)%cap; size--; return val;
        }

        @SuppressWarnings("unchecked")
        List<T> toList() {
            List<T> r=new ArrayList<>();
            for(int i=0;i<size;i++) r.add((T)data[(front+i)%cap]);
            return r;
        }
    }

    // ════════════════════════════════════════════════════════
    //  CO3 ── MIN-HEAP / PRIORITY QUEUE
    //  Used for: Priority slot assignment (lower slot number = higher priority)
    //            VIP service requests (lower priority number = served first)
    //  insert O(log n) | extractMin O(log n) | peek O(1)
    // ════════════════════════════════════════════════════════
    static class MinHeap<T extends Comparable<T>> {
        private Object[] data;
        private int size;

        @SuppressWarnings("unchecked")
        MinHeap(int capacity) { data = new Object[capacity]; size=0; }

        /** INSERT – O(log n) : add at end, bubble up */
        void insert(T val) {
            if (size == data.length) throw new RuntimeException("Heap Full");
            data[size++] = val;
            bubbleUp(size-1);
        }

        @SuppressWarnings("unchecked")
        private void bubbleUp(int i) {
            while (i > 0) {
                int parent = (i-1)/2;
                if (((T)data[parent]).compareTo((T)data[i]) > 0) {
                    Object tmp=data[i]; data[i]=data[parent]; data[parent]=tmp; i=parent;
                } else break;
            }
        }

        /** EXTRACT MIN – O(log n) : remove root, replace with last, bubble down */
        @SuppressWarnings("unchecked")
        T extractMin() {
            if (size==0) throw new NoSuchElementException("Heap Empty");
            T min=(T)data[0]; data[0]=data[--size];
            bubbleDown(0); return min;
        }

        @SuppressWarnings("unchecked")
        private void bubbleDown(int i) {
            while (true) {
                int l=2*i+1, r=2*i+2, s=i;
                if (l<size && ((T)data[l]).compareTo((T)data[s])<0) s=l;
                if (r<size && ((T)data[r]).compareTo((T)data[s])<0) s=r;
                if (s==i) break;
                Object tmp=data[i]; data[i]=data[s]; data[s]=tmp; i=s;
            }
        }

        @SuppressWarnings("unchecked")
        T peekMin() { if(size==0) throw new NoSuchElementException(); return (T)data[0]; }
        boolean isEmpty() { return size==0; }
        int     size()    { return size; }
    }

    // Priority-aware wrapper for ServiceRequests
    static class PriorityRequest implements Comparable<PriorityRequest> {
        int priority; ServiceRequest request;
        PriorityRequest(int p, ServiceRequest r) { priority=p; request=r; }
        @Override public int compareTo(PriorityRequest o) {
            return Integer.compare(this.priority, o.priority);
        }
    }

    // Priority-aware wrapper for ParkingSlots (by slot number)
    static class PrioritySlot implements Comparable<PrioritySlot> {
        int slotOrder; ParkingSlot slot;
        PrioritySlot(int order, ParkingSlot s) { slotOrder=order; slot=s; }
        @Override public int compareTo(PrioritySlot o) {
            return Integer.compare(this.slotOrder, o.slotOrder);
        }
    }

    // ════════════════════════════════════════════════════════
    //  CO4 ── HASH TABLE WITH CHAINING
    //  Used for: User store (admin + customer lookup by email)
    //  put O(1) avg | get O(1) avg | delete O(1) avg
    //  Collision resolution: separate chaining (linked list per bucket)
    // ════════════════════════════════════════════════════════
    static class ChainingHashMap<K, V> {
        private static class Entry<K,V> {
            K key; V value; Entry<K,V> next;
            Entry(K k, V v) { key=k; value=v; }
        }

        private Entry<K,V>[] table;
        private int capacity, size;
        private static final double LOAD_THRESHOLD = 0.75;

        @SuppressWarnings("unchecked")
        ChainingHashMap(int cap) {
            capacity=cap; table=new Entry[cap];
        }

        private int hash(K key) {
            return Math.abs(key.hashCode()) % capacity;
        }

        /** PUT – O(1) average, O(n) worst (all same bucket) */
        void put(K key, V value) {
            if ((double)size/capacity >= LOAD_THRESHOLD) resize();
            int idx = hash(key);
            Entry<K,V> cur = table[idx];
            while (cur != null) {
                if (cur.key.equals(key)) { cur.value=value; return; } // update
                cur = cur.next;
            }
            Entry<K,V> node = new Entry<>(key, value);
            node.next = table[idx]; table[idx] = node; size++;
        }

        /** GET – O(1) average */
        V get(K key) {
            Entry<K,V> cur = table[hash(key)];
            while (cur != null) {
                if (cur.key.equals(key)) return cur.value;
                cur = cur.next;
            }
            return null;
        }

        /** DELETE – O(1) average */
        boolean delete(K key) {
            int idx=hash(key);
            Entry<K,V> cur=table[idx], prev=null;
            while (cur!=null) {
                if (cur.key.equals(key)) {
                    if (prev==null) table[idx]=cur.next; else prev.next=cur.next;
                    size--; return true;
                }
                prev=cur; cur=cur.next;
            }
            return false;
        }

        boolean containsKey(K key) { return get(key) != null; }

        List<V> values() {
            List<V> result = new ArrayList<>();
            for (Entry<K,V> head : table) {
                Entry<K,V> cur = head;
                while (cur!=null) { result.add(cur.value); cur=cur.next; }
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        private void resize() {
            int newCap = capacity * 2;
            Entry<K,V>[] newTable = new Entry[newCap];
            for (Entry<K,V> head : table) {
                Entry<K,V> cur = head;
                while (cur!=null) {
                    int newIdx = Math.abs(cur.key.hashCode()) % newCap;
                    Entry<K,V> node = new Entry<>(cur.key, cur.value);
                    node.next = newTable[newIdx]; newTable[newIdx]=node;
                    cur=cur.next;
                }
            }
            table=newTable; capacity=newCap;
        }

        int size()     { return size; }
        int capacity() { return capacity; }
        double loadFactor() { return (double)size/capacity; }
    }

    // ════════════════════════════════════════════════════════
    //  CO4 ── HASH TABLE WITH OPEN ADDRESSING (Linear Probing)
    //  Used for: Parking slot store (slot-id → slot object)
    //  put O(1) avg | get O(1) avg
    //  Collision: h(k,i) = (h(k) + i) mod m
    // ════════════════════════════════════════════════════════
    static class OpenAddressHashMap<V> {
        private String[]  keys;
        private Object[]  values;
        private boolean[] deleted;
        private int       capacity, size;

        OpenAddressHashMap(int cap) {
            capacity=cap;
            keys   =new String[cap];
            values =new Object[cap];
            deleted=new boolean[cap];
        }

        private int hash(String key) {
            int h=0;
            for (char c : key.toCharArray()) h = (h*31 + c) % capacity;
            return Math.abs(h);
        }

        /** PUT – O(1) avg (linear probing) */
        void put(String key, V value) {
            if ((double)size/capacity >= 0.5) resize();
            int base=hash(key), i=0;
            while (i < capacity) {
                int idx=(base+i) % capacity;
                if (keys[idx]==null || deleted[idx] || keys[idx].equals(key)) {
                    if (keys[idx]==null || deleted[idx]) size++;
                    keys[idx]=key; values[idx]=value; deleted[idx]=false; return;
                }
                i++;
            }
            throw new RuntimeException("Hash table full");
        }

        /** GET – O(1) avg */
        @SuppressWarnings("unchecked")
        V get(String key) {
            int base=hash(key), i=0;
            while (i<capacity) {
                int idx=(base+i)%capacity;
                if (keys[idx]==null) return null;
                if (!deleted[idx] && keys[idx].equals(key)) return (V)values[idx];
                i++;
            }
            return null;
        }

        /** DELETE with tombstone – O(1) avg */
        boolean delete(String key) {
            int base=hash(key), i=0;
            while (i<capacity) {
                int idx=(base+i)%capacity;
                if (keys[idx]==null) return false;
                if (!deleted[idx] && keys[idx].equals(key)) {
                    deleted[idx]=true; size--; return true;
                }
                i++;
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        List<V> values() {
            List<V> r=new ArrayList<>();
            for (int i=0;i<capacity;i++)
                if (keys[i]!=null && !deleted[i]) r.add((V)values[i]);
            return r;
        }

        private void resize() {
            String[] oldK=keys; Object[] oldV=values; boolean[] oldD=deleted;
            int newCap=capacity*2;
            keys=new String[newCap]; values=new Object[newCap];
            deleted=new boolean[newCap]; capacity=newCap; size=0;
            for (int i=0;i<oldK.length;i++)
                if (oldK[i]!=null && !oldD[i]) put(oldK[i], (V)oldV[i]);
        }

        int size()     { return size; }
        int capacity() { return capacity; }
    }

    // ════════════════════════════════════════════════════════
    //  CO1 ── SORTING  (used for slot reports & search results)
    //  Bubble  O(n²) | Merge O(n log n) | Quick O(n log n) avg
    // ════════════════════════════════════════════════════════
    static class Sorter {

        /** Bubble Sort – O(n²) worst/avg, O(n) best with flag
         *  Used for: sorting small notification lists */
        static void bubbleSort(List<ParkingSlot> list,
                               Comparator<ParkingSlot> cmp) {
            int n = list.size();
            for (int i=0;i<n-1;i++) {
                boolean swapped=false;
                for (int j=0;j<n-i-1;j++) {
                    if (cmp.compare(list.get(j), list.get(j+1)) > 0) {
                        ParkingSlot tmp=list.get(j); list.set(j,list.get(j+1)); list.set(j+1,tmp);
                        swapped=true;
                    }
                }
                if (!swapped) break;   // already sorted – O(n) best case
            }
        }

        /** Merge Sort – O(n log n) all cases, Space O(n)
         *  Used for: generating full sorted parking reports */
        static void mergeSort(List<ParkingSlot> list, int l, int r,
                              Comparator<ParkingSlot> cmp) {
            if (l < r) {
                int m=(l+r)/2;
                mergeSort(list, l, m, cmp);
                mergeSort(list, m+1, r, cmp);
                merge(list, l, m, r, cmp);
            }
        }

        private static void merge(List<ParkingSlot> list, int l, int m, int r,
                                  Comparator<ParkingSlot> cmp) {
            List<ParkingSlot> L=new ArrayList<>(list.subList(l,m+1));
            List<ParkingSlot> R=new ArrayList<>(list.subList(m+1,r+1));
            int i=0,j=0,k=l;
            while (i<L.size()&&j<R.size())
                list.set(k++, cmp.compare(L.get(i),R.get(j))<=0 ? L.get(i++) : R.get(j++));
            while (i<L.size()) list.set(k++, L.get(i++));
            while (j<R.size()) list.set(k++, R.get(j++));
        }

        /** Quick Sort – O(n log n) avg, O(n²) worst
         *  Used for: sorting slots by occupancy timestamp */
        static void quickSort(List<ParkingSlot> list, int low, int high,
                              Comparator<ParkingSlot> cmp) {
            if (low < high) {
                int pi=partition(list, low, high, cmp);
                quickSort(list, low, pi-1, cmp);
                quickSort(list, pi+1, high, cmp);
            }
        }

        private static int partition(List<ParkingSlot> list, int low, int high,
                                     Comparator<ParkingSlot> cmp) {
            ParkingSlot pivot=list.get(high); int i=low-1;
            for (int j=low;j<high;j++) {
                if (cmp.compare(list.get(j),pivot)<0) {
                    i++;
                    ParkingSlot tmp=list.get(i); list.set(i,list.get(j)); list.set(j,tmp);
                }
            }
            ParkingSlot tmp=list.get(i+1); list.set(i+1,list.get(high)); list.set(high,tmp);
            return i+1;
        }
    }

    // ════════════════════════════════════════════════════════
    //  CO1 ── BINARY SEARCH
    //  Used for: Looking up a slot by ID in a sorted array
    //  O(log n) worst/avg,  Ω(1) best
    //  Recurrence: T(n) = T(n/2) + O(1) → O(log n)
    // ════════════════════════════════════════════════════════
    static int binarySearchSlot(List<ParkingSlot> sorted, String targetId) {
        int low=0, high=sorted.size()-1;
        while (low <= high) {
            int mid = low + (high-low)/2;
            int cmp = sorted.get(mid).id.compareTo(targetId);
            if      (cmp == 0) return mid;
            else if (cmp <  0) low  = mid+1;
            else               high = mid-1;
        }
        return -1;
    }

    // ════════════════════════════════════════════════════════
    //  PARKING PORTAL  –  MAIN SERVICE CLASS
    //  Wires all DSA structures together
    // ════════════════════════════════════════════════════════
    static class ParkingService {

        // CO4: Hash tables for user store
        ChainingHashMap<String, User>     adminStore    = new ChainingHashMap<>(16);
        ChainingHashMap<String, User>     customerStore = new ChainingHashMap<>(32);

        // CO4: Open-addressing hash maps for each basement's slot store
        OpenAddressHashMap<ParkingSlot>[] basements;

        // CO2: Singly linked list → notification log
        SinglyLinkedList<Notification>    notifications = new SinglyLinkedList<>();

        // CO2: Doubly linked list → booking history (per customer, stored in map)
        ChainingHashMap<String, DoublyLinkedList<BookingRecord>> bookingHistory
            = new ChainingHashMap<>(32);

        // CO2: Circular linked list → basement round-robin
        CircularLinkedList basementNavigator = new CircularLinkedList();

        // CO3: Stack → navigation/screen history
        Stack<String> navStack = new Stack<>(50);

        // CO3: Queue → service request FIFO queue
        LinkedQueue<ServiceRequest> serviceQueue = new LinkedQueue<>();

        // CO3: Circular Queue → slot event ring buffer (last 20 events)
        CircularQueue<String> eventBuffer = new CircularQueue<>(20);

        // CO3: Deque → recent slot searches
        Deque<String> recentSearches = new Deque<>(10);

        // CO3: Min-Heap → priority slot assignment (lowest slot order = first)
        MinHeap<PrioritySlot>[] slotHeaps;

        // CO3: Priority Queue (Min-Heap) → VIP service requests
        MinHeap<PriorityRequest> priorityServiceQueue = new MinHeap<>(100);

        int srCounter = 1;

        @SuppressWarnings("unchecked")
        ParkingService() {
            basements  = new OpenAddressHashMap[BASEMENTS + 1];
            slotHeaps  = new MinHeap[BASEMENTS + 1];
            for (int b=1; b<=BASEMENTS; b++) {
                basements[b] = new OpenAddressHashMap<>(SLOTS_EACH * 3);
                slotHeaps[b] = new MinHeap<>(SLOTS_EACH);
            }
            for (int b=1; b<=BASEMENTS; b++) basementNavigator.insert(b);
            initSlots();
        }

        // ── Initialise all 60 slots per basement ────────────
        void initSlots() {
            char[] rows = {'A','B','C','D','E','F'};
            int[] preOccupied = {0, 45, 50, 43};   // index 1,2,3

            for (int b=1; b<=BASEMENTS; b++) {
                List<String> allIds = new ArrayList<>();
                for (char row : rows)
                    for (int n=1; n<=10; n++) {
                        String id = n + String.valueOf(row);
                        ParkingSlot slot = new ParkingSlot(id, b);
                        basements[b].put(id, slot);
                        allIds.add(id);
                    }
                // Shuffle and pre-occupy
                Collections.shuffle(allIds);
                for (int i=0; i<preOccupied[b]; i++) {
                    ParkingSlot s = basements[b].get(allIds.get(i));
                    s.status        = SlotStatus.OCCUPIED;
                    s.occupiedBy    = "pre_customer_" + i;
                    s.occupiedByName= "Customer " + (i+1);
                    s.timestamp     = System.currentTimeMillis() - (long)(Math.random()*3600000);
                    basements[b].put(s.id, s);
                }
                // Build free-slot min-heap for this basement
                rebuildHeap(b);
            }
        }

        /** Rebuild min-heap of free slots for a basement – O(n log n) */
        @SuppressWarnings("unchecked")
        void rebuildHeap(int b) {
            slotHeaps[b] = new MinHeap<>(SLOTS_EACH);
            int order = 0;
            char[] rows = {'A','B','C','D','E','F'};
            for (char row : rows)
                for (int n=1; n<=10; n++) {
                    String id = n + String.valueOf(row);
                    ParkingSlot s = basements[b].get(id);
                    if (s != null && s.status == SlotStatus.FREE)
                        slotHeaps[b].insert(new PrioritySlot(order++, s));
                }
        }

        // ────────────────────────────────────────────────────
        //  ADMIN OPERATIONS
        // ────────────────────────────────────────────────────

        /** Register admin – O(1) avg hash insert */
        boolean registerAdmin(String name, String email, String phone) {
            if (adminStore.containsKey(email)) return false;
            String id = "ADM" + String.format("%05d", (int)(Math.random()*99999));
            User admin = new User(id, name, email, phone, "admin");
            adminStore.put(email, admin);
            addNotification("✅ New admin registered: " + name);
            navStack.push("adminSignup");
            return true;
        }

        /** Admin login – O(1) avg hash lookup */
        User adminLogin(String email, String adminId) {
            User u = adminStore.get(email);
            if (u != null && u.id.equals(adminId)) {
                navStack.push("adminDashboard");
                addNotification("👨‍💼 Admin logged in: " + u.name);
                return u;
            }
            return null;
        }

        /** Admin: free a leaving slot – O(1) hash update */
        boolean adminFreeSlot(int basement, String slotId) {
            ParkingSlot s = basements[basement].get(slotId);
            if (s == null || s.status != SlotStatus.LEAVING) return false;
            String prev = s.occupiedBy;
            s.status=SlotStatus.FREE; s.occupiedBy=null;
            s.occupiedByName=null; s.timestamp=0;
            basements[basement].put(slotId, s);
            eventBuffer.enqueue("FREED:" + slotId + " B" + basement);
            addNotification("🅿️ Admin freed slot " + slotId + " (was " + prev + ")");
            rebuildHeap(basement);
            return true;
        }

        /** Admin: get all slots sorted by status – Merge Sort O(n log n) */
        List<ParkingSlot> getSlotsSortedByStatus(int basement) {
            List<ParkingSlot> list = basements[basement].values();
            Sorter.mergeSort(list, 0, list.size()-1,
                Comparator.comparing(s -> s.status.name()));
            return list;
        }

        /** Admin: get occupied slots sorted by timestamp – Quick Sort O(n log n) */
        List<ParkingSlot> getOccupiedSortedByTime(int basement) {
            List<ParkingSlot> list = basements[basement].values();
            list.removeIf(s -> s.status != SlotStatus.OCCUPIED);
            Sorter.quickSort(list, 0, list.size()-1,
                Comparator.comparingLong(s -> s.timestamp));
            return list;
        }

        /** Admin: view booking history for a customer (doubly linked – backward) */
        List<BookingRecord> getCustomerHistory(String email) {
            DoublyLinkedList<BookingRecord> hist = bookingHistory.get(email);
            if (hist == null) return Collections.emptyList();
            return hist.toListBackward();   // most recent first
        }

        /** Admin: resolve top-priority service request from heap – O(log n) */
        ServiceRequest resolveNextRequest() {
            if (priorityServiceQueue.isEmpty()) return null;
            PriorityRequest pr = priorityServiceQueue.extractMin();
            pr.request.status = "resolved";
            addNotification("✅ Resolved SR#" + pr.request.id + " from " + pr.request.customerName);
            return pr.request;
        }

        /** Admin: count stats per basement – O(n) with reduce logic */
        int[] getBasementStats(int b) {
            // returns [free, occupied, leaving]
            int[] counts = {0,0,0};
            for (ParkingSlot s : basements[b].values()) {
                if      (s.status==SlotStatus.FREE)     counts[0]++;
                else if (s.status==SlotStatus.OCCUPIED) counts[1]++;
                else                                     counts[2]++;
            }
            return counts;
        }

        // ────────────────────────────────────────────────────
        //  CUSTOMER OPERATIONS
        // ────────────────────────────────────────────────────

        /** Register customer – O(1) avg */
        boolean registerCustomer(String name, String email,
                                  String phone, String mall, String vehicle) {
            if (customerStore.containsKey(email)) return false;
            String id = "CUST" + String.format("%05d", (int)(Math.random()*99999));
            User cust = new User(id, name, email, phone, "customer");
            cust.mall=mall; cust.vehicle=vehicle;
            customerStore.put(email, cust);
            bookingHistory.put(email, new DoublyLinkedList<>());
            addNotification("👤 New customer registered: " + name);
            return true;
        }

        /** Customer login – O(1) avg hash lookup */
        User customerLogin(String email) {
            User u = customerStore.get(email);
            if (u != null) { navStack.push("customerDashboard"); }
            return u;
        }

        /**
         * Book best available slot using Min-Heap – O(log n)
         * Min-Heap gives lowest-order (front-row) free slot first
         */
        String bookBestSlot(int basement, String email) {
            User cust = customerStore.get(email);
            if (cust == null || cust.currentSlot != null) return null;

            while (!slotHeaps[basement].isEmpty()) {
                PrioritySlot ps = slotHeaps[basement].extractMin();
                ParkingSlot  s  = basements[basement].get(ps.slot.id);
                if (s != null && s.status == SlotStatus.FREE) {
                    // Book it
                    s.status         = SlotStatus.OCCUPIED;
                    s.occupiedBy     = email;
                    s.occupiedByName = cust.name;
                    s.timestamp      = System.currentTimeMillis();
                    basements[basement].put(s.id, s);

                    cust.currentSlot      = s.id;
                    cust.currentBasement  = basement;
                    customerStore.put(email, cust);

                    // Record in doubly linked list booking history – O(1)
                    DoublyLinkedList<BookingRecord> hist = bookingHistory.get(email);
                    if (hist == null) { hist = new DoublyLinkedList<>(); bookingHistory.put(email,hist); }
                    hist.insertEnd(new BookingRecord(s.id, basement, email, s.timestamp));

                    // Log events
                    eventBuffer.enqueue("BOOKED:" + s.id + " B" + basement + " by " + email);
                    recentSearches.addFront(s.id);
                    addNotification("🚗 " + cust.name + " booked slot " + s.id + " in Basement " + basement);
                    return s.id;
                }
            }
            return null;   // no free slots
        }

        /** Customer books a specific slot – O(1) hash get */
        boolean bookSpecificSlot(int basement, String slotId, String email) {
            ParkingSlot s = basements[basement].get(slotId);
            User cust     = customerStore.get(email);
            if (s==null || s.status!=SlotStatus.FREE || cust==null || cust.currentSlot!=null)
                return false;

            s.status=SlotStatus.OCCUPIED; s.occupiedBy=email;
            s.occupiedByName=cust.name; s.timestamp=System.currentTimeMillis();
            basements[basement].put(slotId, s);

            cust.currentSlot=slotId; cust.currentBasement=basement;
            customerStore.put(email, cust);

            DoublyLinkedList<BookingRecord> hist = bookingHistory.get(email);
            if (hist==null) { hist=new DoublyLinkedList<>(); bookingHistory.put(email,hist); }
            hist.insertEnd(new BookingRecord(slotId, basement, email, s.timestamp));

            eventBuffer.enqueue("BOOKED:" + slotId);
            recentSearches.addFront(slotId);
            addNotification("🚗 " + cust.name + " booked slot " + slotId);
            return true;
        }

        /** Mark slot as leaving soon – O(1) */
        boolean markLeavingSoon(int basement, String slotId, String email) {
            ParkingSlot s = basements[basement].get(slotId);
            if (s==null || !email.equals(s.occupiedBy)) return false;
            s.status = SlotStatus.LEAVING;
            basements[basement].put(slotId, s);
            eventBuffer.enqueue("LEAVING:" + slotId);
            addNotification("⏰ " + s.occupiedByName + " leaving slot " + slotId + " soon");
            return true;
        }

        /** Customer leaves slot – O(1) hash update + O(1) DLL tail update */
        boolean leaveSlot(int basement, String slotId, String email) {
            ParkingSlot s = basements[basement].get(slotId);
            User cust     = customerStore.get(email);
            if (s==null || !email.equals(s.occupiedBy)) return false;

            // Update booking history tail with checkout time – O(1)
            DoublyLinkedList<BookingRecord> hist = bookingHistory.get(email);
            if (hist != null && hist.tail != null) hist.tail.data.checkout = System.currentTimeMillis();

            String name = s.occupiedByName;
            s.status=SlotStatus.FREE; s.occupiedBy=null;
            s.occupiedByName=null; s.timestamp=0;
            basements[basement].put(slotId, s);

            cust.currentSlot=null; cust.currentBasement=0;
            customerStore.put(email, cust);

            eventBuffer.enqueue("LEFT:" + slotId);
            addNotification("✅ " + name + " left slot " + slotId + " – now FREE");
            rebuildHeap(basement);
            return true;
        }

        /**
         * Binary Search for a slot by ID in sorted slot list – O(log n)
         * CO1: requires sorted list; each step halves search space
         */
        ParkingSlot searchSlotById(int basement, String slotId) {
            List<ParkingSlot> slots = basements[basement].values();
            // Sort first (needed for binary search) – O(n log n)
            Sorter.mergeSort(slots, 0, slots.size()-1, Comparator.comparing(s -> s.id));
            int idx = binarySearchSlot(slots, slotId);
            recentSearches.addRear(slotId);   // track in deque
            return idx >= 0 ? slots.get(idx) : null;
        }

        /** Submit service request – enqueue O(1) + heap insert O(log n) */
        ServiceRequest submitServiceRequest(String email, String type,
                                             String message, int priority) {
            User cust = customerStore.get(email);
            if (cust == null) return null;
            ServiceRequest sr = new ServiceRequest(
                srCounter++, email, cust.name, type, message, priority);
            serviceQueue.enqueue(sr);   // FIFO queue
            priorityServiceQueue.insert(new PriorityRequest(priority, sr)); // min-heap
            addNotification("📞 Service request from " + cust.name + ": " + type);
            return sr;
        }

        /** Get next service request FIFO – O(1) */
        ServiceRequest getNextServiceRequest() {
            return serviceQueue.isEmpty() ? null : serviceQueue.dequeue();
        }

        /** Notification helpers using Singly Linked List */
        void addNotification(String msg) {
            notifications.insertFront(new Notification(msg)); // O(1)
        }
        List<Notification> getNotifications() { return notifications.toList(); } // O(n)

        /** Navigate to next basement using circular list – O(1) */
        int nextBasement() { return basementNavigator.next(); }
    }

    // ════════════════════════════════════════════════════════
    //  DEMO  –  runs the full parking portal backend
    // ════════════════════════════════════════════════════════
    static void separator(String title) {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.printf ("║  %-44s║%n", title);
        System.out.println("╚══════════════════════════════════════════════╝");
    }

    static void printStats(ParkingService ps, int b) {
        int[] s = ps.getBasementStats(b);
        System.out.printf("  Basement %d → Free: %d | Occupied: %d | Leaving: %d%n",
            b, s[0], s[1], s[2]);
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   URBAN PARKING PORTAL  –  Java DSA Backend ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        ParkingService ps = new ParkingService();

        // ── CO2: Circular List – basement navigator ──────────
        separator("CO2 – Circular Linked List: Basement Navigator");
        ps.basementNavigator.print();
        System.out.println("Is circular? " + ps.basementNavigator.isCircular());
        System.out.println("Next basement: " + ps.nextBasement());
        System.out.println("Next basement: " + ps.nextBasement());
        System.out.println("Next basement: " + ps.nextBasement() + " (wraps back)");

        // ── CO4: Hash Table – register users ─────────────────
        separator("CO4 – Chaining Hash Map: Register Users");
        ps.registerAdmin("Admin User", "admin@parking.com", "+1 234 567 8900");
        ps.registerCustomer("Alice Smith",  "alice@mail.com",  "+1 111 111 1111", "City Center Mall", "MH12AB1234");
        ps.registerCustomer("Bob Jones",    "bob@mail.com",    "+1 222 222 2222", "Metro Plaza",      "MH12CD5678");
        ps.registerCustomer("Carol White",  "carol@mail.com",  "+1 333 333 3333", "Grand Mall",       "MH12EF9012");
        System.out.println("Admin store size    : " + ps.adminStore.size()    + " (load=" + String.format("%.2f", ps.adminStore.loadFactor()) + ")");
        System.out.println("Customer store size : " + ps.customerStore.size() + " (load=" + String.format("%.2f", ps.customerStore.loadFactor()) + ")");

        // ── CO4: Open Addressing – slot store stats ───────────
        separator("CO4 – Open Addressing: Slot Store Stats");
        for (int b=1; b<=3; b++) printStats(ps, b);

        // ── Admin login + CO3 Stack ───────────────────────────
        separator("CO3 – Stack: Navigation History (Admin Login)");
        User admin = ps.adminLogin("admin@parking.com", ps.adminStore.get("admin@parking.com").id);
        System.out.println("Logged in: " + (admin != null ? admin.name : "FAILED"));
        ps.navStack.print("Nav Stack after admin login");

        // ── CO3: Priority Queue – service requests ────────────
        separator("CO3 – Priority Queue (Min-Heap): Service Requests");
        ps.registerCustomer("Dave Brown", "dave@mail.com", "+1 444 444 4444", "Sunshine", "MH12GH3456");
        ServiceRequest sr1 = ps.submitServiceRequest("alice@mail.com", "Slot Issue",   "My slot light is broken", 2);
        ServiceRequest sr2 = ps.submitServiceRequest("bob@mail.com",   "Complaint",    "Gate was closed early",   1);
        ServiceRequest sr3 = ps.submitServiceRequest("carol@mail.com", "Feedback",     "Great service!",          3);
        ServiceRequest sr4 = ps.submitServiceRequest("dave@mail.com",  "Payment Help", "Double charged",          1);
        System.out.println("Service requests enqueued: " + ps.serviceQueue.size());
        System.out.println("Resolving highest priority first (Min-Heap):");
        System.out.println("  → " + ps.resolveNextRequest());
        System.out.println("  → " + ps.resolveNextRequest());
        System.out.println("  → " + ps.resolveNextRequest());

        // ── CO3: Queue – FIFO service processing ─────────────
        separator("CO3 – Linked Queue: FIFO Service Processing");
        System.out.println("FIFO dequeue order:");
        while (!ps.serviceQueue.isEmpty())
            System.out.println("  → " + ps.getNextServiceRequest());

        // ── Customer login + book slots ───────────────────────
        separator("CO3 – Min-Heap: Best Slot Assignment");
        User alice = ps.customerLogin("alice@mail.com");
        User bob   = ps.customerLogin("bob@mail.com");
        User carol = ps.customerLogin("carol@mail.com");
        System.out.println("Customers logged in: " + alice.name + ", " + bob.name + ", " + carol.name);

        String aliceSlot = ps.bookBestSlot(1, "alice@mail.com");
        String bobSlot   = ps.bookBestSlot(1, "bob@mail.com");
        String carolSlot = ps.bookSpecificSlot(2, "3B", "carol@mail.com") ? "3B" : "none";
        System.out.println("Alice  → best slot: " + aliceSlot);
        System.out.println("Bob    → best slot: " + bobSlot);
        System.out.println("Carol  → specific slot 3B (B2): " + carolSlot);
        printStats(ps, 1);
        printStats(ps, 2);

        // ── CO1: Binary Search ────────────────────────────────
        separator("CO1 – Binary Search: Slot Lookup");
        ParkingSlot found = ps.searchSlotById(1, aliceSlot);
        System.out.println("Binary search for '" + aliceSlot + "': " + (found != null ? found : "NOT FOUND"));
        ParkingSlot notFound = ps.searchSlotById(1, "9Z");
        System.out.println("Binary search for '9Z': " + (notFound != null ? notFound : "NOT FOUND"));

        // ── CO3: Deque – recent searches ─────────────────────
        separator("CO3 – Deque: Recent Slot Searches");
        ps.searchSlotById(1, bobSlot);
        ps.searchSlotById(2, "3B");
        System.out.println("Recent searches: " + ps.recentSearches.toList());

        // ── CO3: Circular Queue – event ring buffer ───────────
        separator("CO3 – Circular Queue: Slot Event Ring Buffer");
        System.out.println("Recent events: " + ps.eventBuffer.toList());

        // ── CO2: Doubly Linked List – booking history ─────────
        separator("CO2 – Doubly Linked List: Booking History");
        ps.markLeavingSoon(1, aliceSlot, "alice@mail.com");
        ps.leaveSlot(1, aliceSlot, "alice@mail.com");
        // re-book for history
        ps.bookBestSlot(1, "alice@mail.com");

        List<BookingRecord> aliceHistory = ps.getCustomerHistory("alice@mail.com");
        System.out.println("Alice's booking history (most recent first via backward traverse):");
        for (BookingRecord br : aliceHistory) System.out.println("  " + br);

        // ── CO1: Sorting reports ──────────────────────────────
        separator("CO1 – Merge Sort / Quick Sort: Slot Reports");
        List<ParkingSlot> sortedByStatus = ps.getSlotsSortedByStatus(1);
        System.out.println("Merge Sort by status (first 8 slots of B1):");
        sortedByStatus.stream().limit(8).forEach(s -> System.out.println("  " + s));

        List<ParkingSlot> sortedByTime = ps.getOccupiedSortedByTime(1);
        System.out.println("\nQuick Sort occupied slots by time (first 5 of B1):");
        sortedByTime.stream().limit(5).forEach(s -> System.out.println("  " + s));

        // ── CO1: Bubble Sort – small notification list ────────
        separator("CO1 – Bubble Sort: Notification List");
        List<ParkingSlot> small = new ArrayList<>(sortedByStatus.subList(0, Math.min(5, sortedByStatus.size())));
        Sorter.bubbleSort(small, Comparator.comparing(s -> s.id));
        System.out.println("Bubble sorted by slot ID (small list):");
        small.forEach(s -> System.out.println("  " + s));

        // ── CO2: Singly Linked List – notifications ───────────
        separator("CO2 – Singly Linked List: Notifications");
        List<Notification> notifs = ps.getNotifications();
        System.out.println("Total notifications: " + notifs.size());
        System.out.println("Latest 5:");
        notifs.stream().limit(5).forEach(n -> System.out.println("  " + n));

        // Reverse notifications list – O(n)
        ps.notifications.reverse();
        System.out.println("After reverse (oldest first), first 3:");
        ps.notifications.toList().stream().limit(3).forEach(n -> System.out.println("  " + n));

        // Cycle detection – should be false for a linear list
        System.out.println("Notification list has cycle? " + ps.notifications.detectCycle());

        // ── CO1: Empirical timing ─────────────────────────────
        separator("CO1 – Empirical Timing: Sort Algorithms");
        List<ParkingSlot> allSlots = ps.basements[1].values();
        allSlots.addAll(ps.basements[2].values());
        allSlots.addAll(ps.basements[3].values());
        List<ParkingSlot> c1 = new ArrayList<>(allSlots);
        List<ParkingSlot> c2 = new ArrayList<>(allSlots);
        List<ParkingSlot> c3 = new ArrayList<>(allSlots);

        long t;
        t=System.nanoTime(); Sorter.bubbleSort(c1, Comparator.comparing(s->s.id));
        System.out.printf("Bubble Sort  (n=%d): %.3f ms%n", c1.size(), (System.nanoTime()-t)/1e6);

        t=System.nanoTime(); Sorter.mergeSort(c2,0,c2.size()-1,Comparator.comparing(s->s.id));
        System.out.printf("Merge Sort   (n=%d): %.3f ms%n", c2.size(), (System.nanoTime()-t)/1e6);

        t=System.nanoTime(); Sorter.quickSort(c3,0,c3.size()-1,Comparator.comparing(s->s.id));
        System.out.printf("Quick Sort   (n=%d): %.3f ms%n", c3.size(), (System.nanoTime()-t)/1e6);

        // ── Final summary ─────────────────────────────────────
        separator("FINAL BASEMENT SUMMARY");
        for (int b=1; b<=3; b++) printStats(ps, b);

        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║  DSA Structures Used in This Project:        ║");
        System.out.println("║  CO1: Binary Search, Bubble/Merge/Quick Sort ║");
        System.out.println("║  CO2: Singly, Doubly, Circular Linked Lists  ║");
        System.out.println("║  CO3: Stack, Queue, Circular Queue, Deque,   ║");
        System.out.println("║       Min-Heap, Priority Queue                ║");
        System.out.println("║  CO4: Hash Table (Chaining + Open Addressing)║");
        System.out.println("╚══════════════════════════════════════════════╝");
    }
}
