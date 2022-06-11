# scrabble-solver-servlet
A Java servlet for cheating at scrabble =)

## Version History
| Version | Feature |
| ------- | ------- |
| 1       | Port https://github.com/dashih/ScrabbleSolver to a Java servlet |
| 2       | Bootstrap 5 UI overhaul |
| 3       | Security - password authentication |
| 4       | Cancelable operations |
| 5       | Reconnect to running operations |
| 6       | PERFORMANCE - Rewrite of the parallel solver to use Java ForkJoin. Many other optimizations! |
| 7       | PERFORMANCE - Use a Trie to prune the permutation tree |

## Deployment
The project includes a dictionary: `src/main/resources/dictionary.txt`. This file may be replaced with a custom dictionary.

The project expects `src/main/resources/password.txt` to contain the password used to authenticate users submitting a solve operation. This file must be present prior to building the war.

`./gradlew war`

Deploy in any container server (tested on Tomcat 8 and 9).

## Algorithm
Combinations, including blanks, are generated for the input string. These combinations are then permuted, and each permutation is checked against the dictionary to find solutions.

The permutation tree is pruned if it is found that no solutions are possible from that subtree. A Trie data structure is used for the dictionary to optimize the pruning process.

## Performance
As of 2022 (on Amazon EC2 c6a instances - 3rd generation AMD EPYC processors), the most demanding practical parameters for this program can challenge a serial implementation of the above algorithm. It takes more than 10 minute to solve a query for the longest scrabble word with two blanks.

![Alt text](readme-img/15chars-twoblanks_serial.png?raw=true)

## Parallelization
The first step of the algorithm, generating combinations, is done serially. Processing the set of combinations is parallelized by submitting each to a worker pool.

### Parallel permutation
If they are large enough, individual string permutation is parallelized using the following method. Take each character in the permutation range and create a child string that starts with that character. These child strings can be permuted from the next character onwards to produce the same results as permuting the parent string. The child string permutations can be done in parallel, and the permutation range is reduced by one. This process is recursed in a divide-and-conquer manner until the permutation range is small enough to process directly.

How small is small enough? This threshold is chosen based on experimental tuning. A brief test on modern hardware (2019 Macbook Pro, 2.4 GHz Intel Core i9) yielded the following results for serial (single-core) permutation.

| Length | Time (ms) |
| ------ | --------- |
| 5      | 0.008     |
| 6      | 0.03      |
| 7      | 0.2       |
| 8      | 1.1       |
| 9      | 10        |
| 10     | 120       |
| 11     | 1,320     |

Running the full servlet on EC2 C6a instances, a threshold of 6 was found to optimal.

### Java Fork/Join Framework
The worker pool is Java's amazing ForkJoinPool. Each task is a RecursiveAction that either permutes the string directly if it is small enough, or produces subtasks as per above. ForkJoinPool's innate work-stealing does a great job handling the unequal tasks and keeping cores busy. It exceled with a granularity of 100th of a millisecond.

### Historically
Previous (v5 and lower) versions implemented suboptimal parallelization.

Achieving good parallel performance is about breaking up the work just enough to fully utilize all processing cores all the time. How much the work is broken up is referred to as granularity. If the granularity is too coarse, some cores will be stuck with the more expensive operations, and other cores will become idle. If the granularity is too fine, the speedup from using multiple cores will be negated by parallelization overhead (managing task queues, synchronization, work stealing).

#### Status reporting bottleneck
Incrementing a shared AtomicLong for each permutation quickly becomes THE BOTTLENECK. Later v6 versions update the number of done permutations when each task completes.

#### parallelStream() of combinations
The first attempts at parallelization simply used parallelStream() to process the list of combinations. The combinations are, of course, of varying length. So particularly with very large inputs, some workers would be stuck with extremely expensive permutations for a long time after the rest of the problem had been solved.

#### Producer/consumer
Producing the complete set of subtasks (for the standard method of parallelizing permutation of a single string described above) is expensive for large inputs. So we want to begin processing tasks while the set is still being produced. Before learning about Java's Fork/Join Framework, this program used a producer/consumer model where the main thread populated a BlockingQueue, and worker threads consumed from the queue and did the actual permuting.

This works reasonably well, but if the initial tasks are cheap, the consumers will block waiting for the producer. Also, if the level of parallelism is high, contention on the single work queue becomes an issue.

#### Batch processing small strings
I attempted to optimize the current algorithm by batch processing small (6 characters or less) strings. The theory was that there is too much overhead in submitting each of these as a separate task to ForkJoinPool. It didn't help though. ForkJoinPool is too good.

## Benchmarks
**Amazon EC2 c6a 2022 - 3rd generation AMD EPYC processors**

### 11 characters with 2 blanks

| Cores    | Time (s) | Speedup | Efficiency |
| -------- | -------- | ------- | ---------- |
| Serial   | 493      |         |            |
| 8-core   | 68       | 7.3x    | 90.6%      |
| 16-core  | 32       | 15.4x   | 96.3%      |
| 32-core  | 16       | 30.8x   | 96.3%      | 
| 48-core  | 11       | 44.8x   | 93.3%      | 
| 64-core  | 8        | 61.6x   | 96.3%      |
| 96-core  | 6        | 82.2x   | 85.6%      | 

#### Sequential
![Alt text](readme-img/11chars-2blanks_sequential.png?raw=true)


#### Parallel
![Alt text](readme-img/11chars-2blanks_96-core.png?raw=true)


### Longest scrabble word - 15 characters
The initial parallel algorithm required 2 days to find this solution with 96 cores.

#### Solution
![Alt text](readme-img/longest-scrabble-word.png?raw=true)

#### Da CPUs
![Alt text](readme-img/CPU.png?raw=true)
