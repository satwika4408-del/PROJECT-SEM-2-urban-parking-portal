import java.util.*;

public class ParkingPortal {

    // ============================
    // SLOT CLASS
    // ============================

    static class Slot {
        String id;
        boolean occupied;

        Slot(String id) {
            this.id = id;
            this.occupied = false;
        }

        public String toString() {
            return id + " : " + (occupied ? "Occupied" : "Free");
        }
    }

    // ============================
    // CO2 – SINGLY LINKED LIST
    // ============================

    static class Node {
        String data;
        Node next;

        Node(String data) {
            this.data = data;
        }
    }

    static class SinglyLinkedList {

        Node head;

        void insert(String data) {
            Node newNode = new Node(data);

            if (head == null) {
                head = newNode;
                return;
            }

            Node temp = head;
            while (temp.next != null)
                temp = temp.next;

            temp.next = newNode;
        }

        void display() {
            Node temp = head;
            while (temp != null) {
                System.out.println(temp.data);
                temp = temp.next;
            }
        }
    }

    // ============================
    // CO3 – STACK
    // ============================

    static Stack<String> navigationStack = new Stack<>();

    // ============================
    // CO3 – QUEUE
    // ============================

    static Queue<String> serviceQueue = new LinkedList<>();


    // ============================
    // CO3 – PRIORITY QUEUE (HEAP)
    // ============================

    static PriorityQueue<String> vipQueue = new PriorityQueue<>();


    // ============================
    // CO4 – HASH TABLE
    // ============================

    static HashMap<String, String> customers = new HashMap<>();


    // ============================
    // CO1 – BUBBLE SORT
    // ============================

    static void bubbleSort(List<Slot> slots) {

        for (int i = 0; i < slots.size() - 1; i++) {

            for (int j = 0; j < slots.size() - i - 1; j++) {

                if (slots.get(j).id.compareTo(slots.get(j + 1).id) > 0) {

                    Slot temp = slots.get(j);
                    slots.set(j, slots.get(j + 1));
                    slots.set(j + 1, temp);

                }
            }
        }
    }


    // ============================
    // CO1 – BINARY SEARCH
    // ============================

    static Slot binarySearch(List<Slot> slots, String target) {

        int low = 0;
        int high = slots.size() - 1;

        while (low <= high) {

            int mid = (low + high) / 2;

            int cmp = slots.get(mid).id.compareTo(target);

            if (cmp == 0)
                return slots.get(mid);

            else if (cmp < 0)
                low = mid + 1;

            else
                high = mid - 1;

        }

        return null;
    }

    // ============================
    // MAIN APPLICATION
    // ============================

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        List<Slot> slots = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            slots.add(new Slot("S" + i));
        }

        SinglyLinkedList notifications = new SinglyLinkedList();

        System.out.println("===== URBAN PARKING PORTAL =====");

        // Register Customer
        System.out.print("Enter Customer Name: ");
        String name = sc.nextLine();

        System.out.print("Enter Email: ");
        String email = sc.nextLine();

        customers.put(email, name);

        navigationStack.push("Dashboard");

        notifications.insert("Customer Registered: " + name);

        // Book Slot
        System.out.print("Enter Slot ID to Book (S1-S10): ");
        String slotId = sc.nextLine();

        for (Slot s : slots) {

            if (s.id.equals(slotId) && !s.occupied) {

                s.occupied = true;

                notifications.insert("Slot Booked: " + slotId);

                System.out.println("Slot booked successfully!");

            }

        }

        // Add service request
        serviceQueue.add("Cleaning Request");

        vipQueue.add("VIP Complaint");

        // Sort slots
        bubbleSort(slots);

        // Binary search slot
        System.out.print("Search Slot: ");
        String search = sc.nextLine();

        Slot result = binarySearch(slots, search);

        if (result != null)
            System.out.println("Slot Found: " + result);
        else
            System.out.println("Slot Not Found");


        // Display Notifications (Linked List)
        System.out.println("\nNotifications:");
        notifications.display();


        // Process Queue
        System.out.println("\nProcessing Service Request:");
        System.out.println(serviceQueue.poll());


        // VIP Priority Queue
        System.out.println("\nVIP Request:");
        System.out.println(vipQueue.poll());


        // Stack Navigation
        System.out.println("\nNavigation Stack:");
        System.out.println(navigationStack);

    }
}
