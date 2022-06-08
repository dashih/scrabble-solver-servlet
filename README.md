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
Permuting strings is the potential bottleneck of the algorithm. A brief test on modern hardware (2019 Macbook Pro, 2.4 GHz Intel Core i9) yielded the following results for serial (single-core) permutation.

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

A casual player is rarely solving inputs longer than 8 characters. Even including 2 blanks/wildcards, modern hardware has no problem producing the solution in seconds using a serial implementation of the algorithm described above.

Permutation does rapidly become expensive after 11 characters. So academically, these larger inputs become interesting from a parallelization perspective.

## Parallelization strategy
Achieving good parallel performance is about breaking up the work just enough to fully utilize all processing cores all the time. How much the work is broken up is referred to as granularity. If the granularity is too coarse, some cores will be stuck with the more expensive operations, and other cores will become idle. If the granularity is too fine, the speedup from using multiple cores will be negated by parallelization overhead (managing task queues, synchronization, work stealing).

On ~2022 hardware (AWS EC2 8-core/16-core) and software (Java 11 Fork/Join Framework), I found that a granularity of around 200 ms per task strikes a good balance.

### Batch processing of small strings
Small strings, 8 characters or less, require ~10 ms to permute. And there are a great many of them in this scenario. So they are processed in batches where each batch requires around 200 ms to process.

### Large strings
Permuting large strings can quickly become very expensive. The standard method to parallelize permutation of a single string is to take each character and produce strings that start with those characters. These strings can then be permuted in parallel from the second character onwards. This process can be recursed until the desired permutation range is attained. This program breaks down strings until they are 11 characters, so each subtask (permutation of a 10-character string) requires about 200 ms.

### Historically
Previous (v5 and lower) versions implemented suboptimal parallelization.

#### Status reporting bottleneck
Incrementing a shared AtomicLong for each permutation quickly becomes THE BOTTLENECK. Later v6 versions update the number of done permutations when each task completes.

#### parallelStream() of combinations
The first attempts at parallelization simply used parallelStream() to process the list of combinations. The combinations are, of course, of varying length. So particularly with very large inputs, some workers would be stuck with extremely expensive permutations for a long time after the rest of the problem had been solved. Additionally, individually processing the vast number of small combinations produced too much overhead.

#### Producer/consumer
Producing the complete set of subtasks (for the standard method of parallelizing permutation of a single string described above) is expensive for large inputs. So we want to begin processing tasks while the set is still being produced. Before learning about Java's Fork/Join Framework, this program used a producer/consumer model where the main thread populated a BlockingQueue, and worker threads consumed from the queue and did the actual permuting.

This works reasonably well, but if the initial tasks are cheap, the consumers will block waiting for the producer. Also, if the level of parallelism is high, contention on the single work queue becomes an issue.

#### Pure Java Fork/Join
Java's Fork/Join framework is perfect for this problem. It allows the solution to be expressed elegantly in a divide-and-conqueror fashion, with individual worker queues and work-stealing taken care of automatically.

Initial v6 versions only implemented the standard method of parallelizing permutation of a single string. Small strings were also processed as individual tasks, and the overhead became a bottleneck. Later v6 versions implemented batching for small strings.

### Benchmarks
**Amazon EC2 c6a 2022 - 3rd generation AMD EPYC processors**

This program achieves 7x speedup on 8-core and 13x speedup on 16-core for an 11-character/2-blank input.

#### Sequential
![Alt text](readme-img/11chars-2blanks_sequential.png?raw=true)


#### 8-core
![Alt text](readme-img/11chars-2blanks_8core.png?raw=true)


#### 16-core
![Alt text](readme-img/11chars-2blanks_16core.png?raw=true)


#### The CPUs:
![Alt text](readme-img/cpus.png?raw=true)
