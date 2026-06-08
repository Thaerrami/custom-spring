# Java Interview Guide — Tricky Parts, Loops, Data Structures & Multithreading

A practical guide for Java interviews, with connections to **this project's code** where it helps you remember concepts.

---

## How to use this guide

| Section | Interview focus |
|---------|-----------------|
| [Tricky Java fundamentals](#1-tricky-java-fundamentals) | `==` vs `equals`, pass-by-value, `static`, boxing |
| [Loops — traps & patterns](#2-loops--traps--patterns) | Off-by-one, concurrent modification, infinite loops |
| [Data structures](#3-data-structures) | List, Set, Map, complexity, `equals`/`hashCode` |
| [Multithreading](#4-multithreading) | Threads, synchronization, pools, deadlocks |
| [Framework connections](#5-connection-to-this-project) | How Custom Spring uses these concepts |
| [Top 30 interview Q&A](#6-top-30-interview-qa) | Quick-fire questions with answers |

---

## 1. Tricky Java fundamentals

### 1.1 `==` vs `.equals()`

```java
String a = new String("hello");
String b = new String("hello");

a == b       // false — compares memory addresses (references)
a.equals(b)  // true  — compares content
```

| Type | Use `==` for | Use `.equals()` for |
|------|--------------|---------------------|
| Objects | Same instance in memory? | Same logical value? |
| Primitives (`int`, `boolean`) | Value comparison | N/A (no `.equals()`) |

**String pool trap:**

```java
String x = "hello";          // string pool
String y = "hello";          // same pool entry
String z = new String("hello"); // heap

x == y   // true  (same pool reference)
x == z   // false (different objects)
x.equals(z) // true
```

**Interview answer:** `==` checks reference identity. `.equals()` checks semantic equality (override in your classes for Map keys, Set membership, comparisons).

**In this project:** `Container` uses `HashMap<Class<?>, Object>` — keys use `Class` identity (`==` works for Class objects loaded once). Custom entity keys need proper `equals`/`hashCode`.

---

### 1.2 Java is always pass-by-value

```java
void swap(int[] arr, int x) {
    arr[0] = 99;  // mutates the object the reference points to — visible to caller
    x = 42;       // reassigns local copy of x — NOT visible to caller
}
```

| What you pass | What is copied | Can caller see changes? |
|---------------|----------------|-------------------------|
| `int x` | value of x | No (for reassignment) |
| `User u` | reference to User | Yes, if you mutate the object |
| `User u` | reference to User | No, if you reassign `u = new User()` |

**Interview answer:** Java passes copies of values. For objects, the copied value is a reference — mutating the object is visible; reassigning the parameter is not.

---

### 1.3 `static` vs instance

```java
public class Counter {
    static int global = 0;   // one copy per class — shared by all instances
    int local = 0;           // one copy per instance

    static void staticMethod() { /* no `this` */ }
    void instanceMethod() { /* has `this` */ }
}
```

| | `static` | instance |
|--|----------|----------|
| Belongs to | Class | Object |
| Memory | Method area / metaspace | Heap (inside object) |
| Access | `ClassName.field` | `object.field` |
| Use for | Utilities, constants, factories | Per-object state |

**Trap:** Non-static inner classes hold an implicit reference to outer instance → memory leak if not careful.

**In this project:** `JdbcTemplate.ResultRow` is a `static` nested class — it doesn't need a `JdbcTemplate` instance to exist.

---

### 1.4 `final` — three meanings

```java
final int x = 1;              // variable — assign once
final class User { }          // class — cannot extend
void method(final User u) { } // parameter — cannot reassign u (object still mutable!)
```

---

### 1.5 Autoboxing traps

```java
Integer a = 127;
Integer b = 127;
a == b  // true (cached -128 to 127)

Integer c = 128;
Integer d = 128;
c == d  // false (new objects on heap)

// Null trap
Integer n = null;
n + 1   // NullPointerException (unboxing null)
```

**Rule:** Never use `==` on boxed types. Use `.equals()` or primitives.

---

### 1.6 Exception hierarchy

```
Throwable
├── Error (OutOfMemoryError — don't catch)
└── Exception
    ├── RuntimeException (unchecked — NPE, IllegalArgumentException)
    └── IOException, SQLException (checked — must handle or declare)
```

| | Checked | Unchecked |
|--|---------|-----------|
| Must compile? | Handle or `throws` | Optional |
| Examples | `SQLException`, `IOException` | `NullPointerException`, `IllegalStateException` |
| When to use | Recoverable external failures | Programming bugs, optional handling |

**In this project:** `JdbcTemplate` methods `throw SQLException` (checked). `Container.getBean()` throws `IllegalStateException` (unchecked).

---

## 2. Loops — traps & patterns

### 2.1 Off-by-one (most common bug)

```java
// WRONG — IndexOutOfBoundsException on last iteration
for (int i = 0; i <= array.length; i++)

// RIGHT
for (int i = 0; i < array.length; i++)

// Also correct
for (int i = 0; i < array.length; i++)   // classic
for (int i : array)                        // enhanced for-each (no index)
IntStream.range(0, array.length)           // streams
```

**Memory trick:** `length` is exclusive upper bound → use `<`, not `<=`.

---

### 2.2 Enhanced for-each vs indexed loop

```java
// for-each — clean, no index, cannot modify collection structure
for (User user : users) {
    System.out.println(user.getName());
}

// indexed — need position, or modify by index
for (int i = 0; i < users.size(); i++) {
    if (shouldRemove(users.get(i))) {
        users.remove(i);  // TRAP: shifts elements, skip next one!
        i--;              // fix: decrement after remove
    }
}
```

**Interview tip:** Prefer **Iterator.remove()** when removing during iteration:

```java
Iterator<User> it = users.iterator();
while (it.hasNext()) {
    if (shouldRemove(it.next())) {
        it.remove();  // safe
    }
}
```

---

### 2.3 ConcurrentModificationException

```java
List<String> list = new ArrayList<>(List.of("a", "b", "c"));

for (String s : list) {
    if (s.equals("b")) {
        list.remove(s);  // 💥 ConcurrentModificationException
    }
}
```

**Why:** for-each uses Iterator internally. Structural change (add/remove) without Iterator invalidates it.

**Fixes:**
1. `iterator.remove()`
2. `list.removeIf(s -> s.equals("b"))`
3. Copy list first: `new ArrayList<>(list)` and iterate copy
4. Iterate backwards (index-based lists only)

**In this project:** `Container.injectDependencies()` copies `beanRegistry.values()` into `new HashSet<>()` before iterating — avoids concurrent modification if injection somehow triggers registry changes.

---

### 2.4 Infinite loop traps

```java
// Trap 1: never increments
int i = 0;
while (i < 10) {
    System.out.println(i);
    // forgot i++
}

// Trap 2: integer overflow (rare but asked)
byte b = 127;
while (b++ < 127) { }  // eventually wraps to -128, loop exits

// Trap 3: float comparison
double x = 0.1;
while (x != 1.0) {
    x += 0.1;  // never exactly 1.0 due to floating point
}
// Fix: use x < 1.0 or Math.abs(x - 1.0) < epsilon
```

---

### 2.5 `break` vs `continue` vs `return`

```java
outer:
for (int i = 0; i < 3; i++) {
    for (int j = 0; j < 3; j++) {
        if (j == 1) continue;      // skip rest of inner loop body
        if (i == 1 && j == 2) break outer;  // exit both loops (labeled)
    }
}
```

**Interview question:** Difference between `break` and `continue`?
- `break` — exit the loop entirely
- `continue` — skip to next iteration

---

### 2.6 Loop complexity (Big-O cheat sheet)

| Loop pattern | Complexity | Example |
|--------------|------------|---------|
| Single loop 0..n | O(n) | `for (i = 0; i < n; i++)` |
| Nested loops | O(n²) | matrix traversal |
| Halving each step | O(log n) | binary search |
| Loop + `HashMap.get` | O(n) avg | lookup is O(1) per iteration |

```java
// O(n²) — classic interview question
for (int i = 0; i < n; i++)
    for (int j = 0; j < n; j++)

// O(n) — looks nested but inner is O(1)
for (int i = 0; i < n; i++)
    map.get(arr[i]);  // HashMap get = O(1) average
```

---

## 3. Data structures

### 3.1 Java Collections hierarchy (know this diagram)

```
Iterable
└── Collection
    ├── List (ordered, duplicates OK)
    │   ├── ArrayList    — array-backed, O(1) get, O(n) insert middle
    │   └── LinkedList   — nodes, O(n) get, O(1) insert at ends
    ├── Set (no duplicates)
    │   ├── HashSet      — O(1) add/contains, no order
    │   ├── LinkedHashSet — insertion order
    │   └── TreeSet      — sorted, O(log n)
    └── Queue / Deque
        └── ArrayDeque, PriorityQueue

Map (not a Collection!)
├── HashMap       — O(1) avg get/put, no order
├── LinkedHashMap — insertion order
└── TreeMap       — sorted by key, O(log n)
```

---

### 3.2 When to use what (interview gold)

| Need | Use | Why |
|------|-----|-----|
| Fast lookup by key | `HashMap` | O(1) average |
| Sorted keys | `TreeMap` | Red-black tree |
| Preserve insert order | `LinkedHashMap` | HashMap + linked list |
| No duplicates | `HashSet` | Backed by HashMap |
| Index access, mostly read | `ArrayList` | Cache-friendly |
| Frequent insert/delete at ends | `LinkedList` or `ArrayDeque` | O(1) at ends |
| Thread-safe map (legacy) | `ConcurrentHashMap` | Fine-grained locks |
| Thread-safe list (rare) | `CopyOnWriteArrayList` | Read-heavy |

**In this project:** `Container.beanRegistry` is `HashMap<Class<?>, Object>` — O(1) bean lookup by type.

---

### 3.3 `equals()` and `hashCode()` contract

**Rule:** If two objects are equal (`a.equals(b)` is true), they **must** have the same hash code.

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof User user)) return false;
    return id == user.id;
}

@Override
public int hashCode() {
    return Long.hashCode(id);
}
```

**What breaks if you violate it:**

```java
User u1 = new User(1, "Ada", "ada@example.com");
User u2 = new User(1, "Grace", "grace@example.com"); // same id

Set<User> set = new HashSet<>();
set.add(u1);
set.contains(u2);  // false if hashCode/equals wrong!
```

**Interview answer:**
- `equals()` — logical equality
- `hashCode()` — bucket index for HashMap/HashSet
- Override both together, or neither
- Use same fields in both

**Our `User` class:** Does NOT override `equals`/`hashCode` — fine for demo, wrong if used as HashMap key or in HashSet.

---

### 3.4 `ArrayList` vs `LinkedList` (classic question)

| Operation | ArrayList | LinkedList |
|-----------|-----------|------------|
| get(i) | O(1) | O(n) |
| add at end | O(1) amortized | O(1) |
| add at middle | O(n) | O(n) to find + O(1) insert |
| memory | contiguous array | node overhead per element |

**Real world:** Use `ArrayList` by default. LinkedList rarely wins in practice on modern JVMs.

---

### 3.5 Immutable collections (Java 9+)

```java
List<String> fixed = List.of("a", "b", "c");  // cannot add/remove
fixed.add("d");  // UnsupportedOperationException

// Mutable copy
List<String> mutable = new ArrayList<>(List.of("a", "b"));
```

---

### 3.6 Comparable vs Comparator

```java
// Natural order — built into class
class User implements Comparable<User> {
    public int compareTo(User other) { return Long.compare(this.id, other.id); }
}

// External order — flexible, multiple sort strategies
users.sort(Comparator.comparing(User::getName));
users.sort(Comparator.comparing(User::getName).reversed());
```

---

### 3.7 Stack vs Queue vs Deque

| ADT | Java | Use |
|-----|------|-----|
| Stack (LIFO) | `ArrayDeque` (not legacy `Stack`) | undo, DFS |
| Queue (FIFO) | `ArrayDeque`, `LinkedList` | BFS, task queues |
| Priority queue | `PriorityQueue` | scheduling, top-K |

```java
Deque<Integer> stack = new ArrayDeque<>();
stack.push(1);
stack.pop();

Queue<String> queue = new ArrayDeque<>();
queue.offer("first");
queue.poll();
```

---

## 4. Multithreading

### 4.1 Thread basics

```java
// Way 1: extend Thread (avoid — limits inheritance)
class MyThread extends Thread {
    public void run() { System.out.println("running"); }
}

// Way 2: implement Runnable (preferred)
Thread t = new Thread(() -> System.out.println("running"));
t.start();   // begins execution
// t.run();  // TRAP: runs in CURRENT thread, not new thread
```

**Interview:** Never call `run()` directly — call `start()` to spawn a new thread.

---

### 4.2 Thread lifecycle

```
NEW → RUNNABLE → RUNNING → TERMINATED
              ↘ BLOCKED (waiting for lock)
              ↘ WAITING (wait(), join())
              ↘ TIMED_WAITING (sleep(), timed join())
```

---

### 4.3 Race condition (the core problem)

```java
public class Counter {
    private int count = 0;

    public void increment() {
        count++;  // NOT atomic! read → add → write — three steps
    }
}
```

Two threads both read `count = 5`, both write `6` — expected `7`, got `6`.

**Fixes:**
```java
// 1. synchronized method
public synchronized void increment() { count++; }

// 2. synchronized block
synchronized (this) { count++; }

// 3. AtomicInteger
private AtomicInteger count = new AtomicInteger(0);
count.incrementAndGet();

// 4. Lock
private final ReentrantLock lock = new ReentrantLock();
lock.lock();
try { count++; } finally { lock.unlock(); }
```

---

### 4.4 `synchronized` vs `volatile`

| | `synchronized` | `volatile` |
|--|----------------|------------|
| Visibility | Yes | Yes |
| Atomicity | Yes (mutual exclusion) | No (single read/write only) |
| Use for | Compound operations (count++) | simple flag, status |

```java
private volatile boolean running = true;  // OK — single write/read

private volatile int count = 0;
count++;  // NOT safe — still read-modify-write
```

**Interview answer:** `volatile` ensures all threads see latest value, but doesn't make `count++` atomic.

---

### 4.5 Deadlock (classic interview scenario)

```java
// Thread 1: lock A → lock B
// Thread 2: lock B → lock A
// Both hold one, wait for other → deadlock forever
```

**Four conditions (Coffman):**
1. Mutual exclusion
2. Hold and wait
3. No preemption
4. Circular wait

**Prevention:** Always acquire locks in same global order. Use `tryLock()` with timeout. Prefer higher-level concurrency utilities.

---

### 4.6 `wait()` vs `sleep()` vs `yield()`

| Method | Where | Releases lock? | Use |
|--------|-------|----------------|-----|
| `Thread.sleep(ms)` | anywhere | No | pause current thread |
| `object.wait()` | inside synchronized | Yes | wait for signal |
| `object.notify()` | inside synchronized | — | wake one waiter |
| `Thread.yield()` | anywhere | No | hint to scheduler (rarely used) |

**Trap:** `sleep()` does NOT release locks. `wait()` must be in `synchronized` block.

---

### 4.7 Thread-safe collections

| Class | Thread-safe? | Strategy |
|-------|--------------|----------|
| `HashMap` | No | — |
| `ConcurrentHashMap` | Yes | segment/CAS locking |
| `Collections.synchronizedMap()` | Yes | lock entire map (slow) |
| `CopyOnWriteArrayList` | Yes | copy on write (read-heavy) |
| `ArrayList` | No | — |

**Interview:** Why not synchronize every method on HashMap?
- Coarse lock = one writer blocks all readers
- `ConcurrentHashMap` allows concurrent reads + limited concurrent writes

---

### 4.8 ExecutorService (production pattern)

```java
ExecutorService pool = Executors.newFixedThreadPool(4);

pool.submit(() -> processRequest());  // fire task
pool.shutdown();                       // no new tasks
pool.awaitTermination(30, TimeUnit.SECONDS);
```

| Pool type | Use |
|-----------|-----|
| `newFixedThreadPool(n)` | Bounded workers — web servers |
| `newCachedThreadPool()` | Unbounded — short tasks (careful!) |
| `newSingleThreadExecutor()` | Sequential tasks |

**Spring MVC:** One thread per HTTP request from Tomcat's pool — your singleton beans must be thread-safe (stateless).

---

### 4.9 Virtual threads (Java 21+ — modern interview topic)

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> handleRequest());  // cheap — millions possible
}
```

| Platform threads | Virtual threads |
|------------------|-----------------|
| ~1 MB stack each | ~KB each |
| OS-scheduled | JVM-scheduled |
| Expensive to create | Cheap to create |
| Good for CPU-bound | Good for I/O-bound (DB, HTTP) |

**Spring Boot 3.2+:** `@EnableVirtualThreads` — same DI model, different thread scheduling.

---

### 4.10 Singleton thread safety (Spring connection)

Spring beans are **singletons by default** — one instance shared by all request threads.

```java
@Service
public class UserService {
    @Inject
    private UserRepository repo;  // singleton — must be stateless

    // WRONG — shared mutable state
    private int requestCount = 0;  // race condition across threads!

    // RIGHT — no instance fields that change per request
    public User find(long id) { return repo.findById(id); }
}
```

**Rule:** Singleton beans = stateless. Per-request state goes in method locals or request-scoped beans.

---

## 5. Connection to this project

| Concept | Where in Custom Spring |
|---------|------------------------|
| `HashMap` O(1) lookup | `Container.beanRegistry` |
| `HashSet` copy before iterate | `injectDependencies()`, `invokeLifecycle()` |
| Reflection | `Container.registerBean()`, field injection |
| ClassLoader / classpath | `Container.scanPackage()` |
| Checked exceptions | `JdbcTemplate`, `SQLException` |
| Singleton pattern | One bean instance per class in registry |
| try-with-resources | `JdbcTemplate` closes Connection/Statement |
| Layered architecture | Service → Repository → JDBC |

**Threading note:** This demo is single-threaded. In a web app, **multiple threads** would share the same `UserService` singleton — that's why services must not store per-user state in fields.

---

## 6. Top 30 interview Q&A

### Language

**Q1: Is Java pass-by-reference?**
No. Pass-by-value always. Object references are copied; mutating the object is visible, reassigning the parameter is not.

**Q2: Difference between `String`, `StringBuilder`, `StringBuffer`?**
- `String` — immutable
- `StringBuilder` — mutable, not thread-safe, faster
- `StringBuffer` — mutable, synchronized, slower

**Q3: Can you override static methods?**
No — static methods are hidden, not overridden. Resolved at compile time by reference type.

**Q4: What is method overloading vs overriding?**
- Overload: same name, different params, compile-time dispatch
- Override: same signature in subclass, runtime dispatch (virtual method)

**Q5: What are default methods in interfaces?**
Java 8+ — interface can have implementation. Enables evolving APIs without breaking implementors.

---

### Collections

**Q6: How does HashMap work internally?**
Array of buckets. `hashCode()` → bucket index. Collisions: linked list (Java 7) or tree (Java 8+, when bucket > 8). `equals()` resolves collision.

**Q7: What happens if two keys have same hashCode?**
Collision — both stored in same bucket, distinguished by `equals()`.

**Q8: Can you use mutable object as HashMap key?**
Bad idea — changing key after insert breaks lookup (lost in wrong bucket).

**Q9: ArrayList vs HashSet vs HashMap?**
List = ordered sequence. Set = unique elements. Map = key-value pairs.

**Q10: Time complexity of HashMap get/put?**
O(1) average, O(n) worst case (all collisions).

---

### Loops & algorithms

**Q11: How to remove elements while iterating a List?**
Use `Iterator.remove()`, `removeIf()`, or iterate backwards with index.

**Q12: What is ConcurrentModificationException?**
Structural modification during iteration without using Iterator's remove method.

**Q13: Binary search complexity?**
O(log n) — requires sorted array.

**Q14: Reverse a linked list — approach?**
Iterative: three pointers (prev, curr, next). Or recursive.

**Q15: Detect cycle in linked list?**
Floyd's tortoise and hare — slow and fast pointer meet if cycle exists.

---

### Multithreading

**Q16: Difference between process and thread?**
Process: own memory space. Thread: shares process memory, lighter, faster context switch.

**Q17: What is a race condition?**
Outcome depends on timing of unsynchronized concurrent access.

**Q18: synchronized vs ReentrantLock?**
Both mutual exclusion. Lock: tryLock, timeouts, fair ordering, separate conditions.

**Q19: What is thread pool and why use it?**
Reuse threads — avoid creation cost, bound concurrency, queue overflow tasks.

**Q20: What makes a class thread-safe?**
All shared mutable state accessed with proper synchronization or confinement.

---

### JVM & Spring (senior questions)

**Q21: Heap vs stack?**
Stack: method frames, local vars per thread. Heap: all objects, shared across threads (needs sync).

**Q22: What is garbage collection?**
Automatic reclamation of unreachable heap objects. Generations: young (Eden, Survivor), old.

**Q23: What is ClassLoader?**
Loads `.class` bytes into JVM. Hierarchy: Bootstrap → Platform → Application.

**Q24: What is DI / IoC?**
Don't `new` dependencies — container injects them. Inversion: framework controls object graph.

**Q25: Why constructor injection over field injection?**
Immutable, testable without reflection, required dependencies explicit, no partial construction.

---

### Tricky code output questions

**Q26: What prints?**
```java
System.out.println(025);  // 21 (octal 25 = decimal 21)
```

**Q27: What prints?**
```java
int i = 0;
i = i++;  // i stays 0! (read 0, increment temp, write 0 back)
```

**Q28: What prints?**
```java
try {
    return 1;
} finally {
    return 2;  // finally wins — returns 2
}
```

**Q29: What prints?**
```java
List<Integer> list = Arrays.asList(1, 2, 3);
list.add(4);  // UnsupportedOperationException — fixed-size list
```

**Q30: How many objects created?**
```java
String s = new String("hello");
// "hello" in pool (maybe) + new String object on heap = 1 or 2 objects
```

---

## 7. Practice problems (do these on paper)

1. Implement LRU cache using `LinkedHashMap` or `HashMap` + doubly linked list
2. Find first non-repeating character in a string using `LinkedHashMap`
3. Two-sum problem using `HashMap` (complement lookup)
4. Print numbers 1–100 with FizzBuzz
5. Producer-consumer with `BlockingQueue`
6. Print odd/even in order with two threads
7. Implement thread-safe singleton (double-checked locking or enum)
8. Reverse words in a string
9. Check if parentheses are balanced using `ArrayDeque`
10. Find duplicate in array of n+1 integers ( Floyd or HashSet)

---

## 8. Study plan (1 week)

| Day | Focus | Action |
|-----|-------|--------|
| 1 | `==`, equals, hashCode | Override in `User`, test in HashSet |
| 2 | Collections | Reimplement bean registry with `ConcurrentHashMap` |
| 3 | Loops & iterators | Do FizzBuzz + iterator remove exercises |
| 4 | Threads basics | Write 2 threads incrementing shared counter — see race |
| 5 | synchronized / Atomic | Fix the counter three different ways |
| 6 | ExecutorService | Submit 10 tasks, observe pool behavior |
| 7 | Mock interview | Answer Q1–30 without looking |

---

## Related docs in this repo

- [Java Internals for Framework Builders](./java-internals.md) — classloading, reflection, proxies
- [Architecture](./architecture.md) — HashMap registry, lifecycle
- [Exercises](./EXERCISES.md) — hands-on framework exercises
- [Spring 6 Internals](./spring-6-internals.md) — thread-per-request, virtual threads

---

Back to [Learning Guide](./README.md)
