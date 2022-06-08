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

## Deployment
The project includes a dictionary: `src/main/resources/dictionary.txt`. This file may be replaced with a custom dictionary.

The project expects `src/main/resources/password.txt` to contain the password used to authenticate users submitting a solve operation. This file must be present prior to building the war.

`./gradlew war`

Deploy in any container server (tested on Tomcat 8 and 9).

## Algorithm
Combinations, including blanks, are generated for the input string. These combinations are then permuted, and each permutation is checked against the dictionary to find solutions.

## Performance
A casual player is rarely solving inputs longer than 8 characters. Even including 2 blanks/wildcards, modern hardware has no problem producing the solution in seconds using a serial implementation of the algorithm described above.

A brief test on modern hardware (2019 Macbook Pro, 2.4 GHz Intel Core i9) yielded the following results for serial (single-core) permutation.

| Length | Time (ms) |
| ------ | --------- |
| 3      | 6         |
| 4      | 7         |
| 5      | 8         |
| 6      | 8         |
| 7      | 10        |
| 8      | 15        |
| 9      | 60        |
| 10     | 200       |
| 11     | 1,600     |
| 12     | 15,000    |
| 13     | 200,000   |

Permutation does rapidly become more expensive after 11 characters. So larger inputs become interesting from a parallelization perspective.

## Parallelization
The first step of the algorithm, generating combinations, is still done serially. Processing the set of combinations is parallelized by submitting each to a worker pool.

### Larger strings
Larger strings (11 characters or more) are broken down. Each character is selected to create a child string that starts with that character. These child strings are then permuted in parallel from the second character onwards. This process is recursed until the child strings are permuting 10 characters. As a result, the maximum processing time for a task or subtask is 200 ms.

### Java Fork/Join Framework
The worker pool is Java's amazing ForkJoinPool. Each task is a RecursiveAction that either permutes the string directly if it is small enough, or produces subtasks as per above. ForkJoinPool's innate work-stealing does a great job handling the unequal tasks and keeping cores busy.

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
| 8-core   | 71       | 6.9x    | 86.8%      |
| 16-core  | 37       | 13.3x   | 83.3%      |
| 32-core  | 18       | 27.4x   | 85.6%      | 
| 48-core  | 12       | 41.1x   | 85.6%      | 
| 64-core  | 10       | 49.3x   | 77.0%      |
| 96-core  | 8        | 61.6x   | 64.2%      | 

#### Sequential
![Alt text](readme-img/11chars-2blanks_sequential.png?raw=true)


#### Parallel
![Alt text](readme-img/11chars-2blanks_96-core.png?raw=true)


### The CPUs:
![Alt text](readme-img/the-cpus.png?raw=true)
