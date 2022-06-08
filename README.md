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
| 6       | PERFORMANCE - Rewrite of the parallel solver to use Java ForkJoin. Many other optimizations |

## Deployment
The project includes a dictionary: `src/main/resources/dictionary.txt`. This file may be replaced with a custom dictionary.

The project expects `src/main/resources/password.txt` to contain the password used to authenticate users submitting a solve operation. This file must be present prior to building the war.

`./gradlew war`

Deploy in any container server (tested on Tomcat 8 and 9).

## Algorithm
Combinations, including blanks, are generated for the input string. These combinations are then permuted, and each permutation is checked against the dictionary to find solutions.

## Performance
Permuting strings is the potential bottleneck of the operation. Modern hardware (2019 Macbook Pro, 2.4 GHz Intel Core i9) permutation performance:
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

### Practically
For 8 letters including 2 blanks, modern hardware has no problem producing the solution in seconds without optimization. Realistically, this is as much as anyone needs for a Scrabble helper application. Academically however, larger inputs become interesting from a parallelization perspective.

### Parallelization strategy
For small strings of 8 characters or less, it is inefficient to process with individual tasks. So these strings are processed in bulk. The collection is chunked so that each chunk requires around 200 ms to process.

For large strings, permutation can quickly become very expensive. The standard method to parallelize permutation of a large string is to take each character and produce strings that start with those characters. These strings can then be permuted in parallel from the second character onwards. This process can be recursed until the desired permutation range is attained. This program breaks down strings until they are 11 characters, so each subtask (permutation of a 10-character string) requires about 200 ms.

### Historically
Previous (v5 and lower) version of this program attempted suboptimal parallelization.

#### parallelStream() of combinations
The first attempt at parallelization simply used parallelStream() to process the list of combinations. The combinations are, of course, of varying length. So with large inputs, some workers would be stuck with extremely expensive permutations, and that quickly became the bottleneck.

Additionally, small permutations are processed individually.

#### Consumer/producer
Using the standard parallelization strategy for permutations, the main thread populated a BlockingQueue with strings whose substrings could be permuted. Worker pool tasks then consumed this BlockingQueue, performing the actual permutations.

It is hard to balance the producer/consumer dynamic, avoiding blocking the producer (if consumers weren't processing fast enough) or blocking the consumers (if the producer wasn't producing fast enough). Also, all workers shared a single source of work.

#### Java Fork/Join
Java's Fork/Join framework is perfect for this problem. It allows the solution to be expressed in a divide-and-conqueror fashion, with individual worker queuing and work-stealing taken care of automatically. v6 introduced usage of this framework.

However, initial v6 versions only used Fork/Join for parallelization of large strings. Small strings were also processed as individual tasks, and the coordination overhead became a bottleneck. Chunking was eventually implemented for small strings.

### Benchmarks - Amazon EC2 c6a 2022 - 3rd generation AMD EPYC processors
This program achieves 7x speedup on 8-core and 13x speedup on 16-core for an 11-character/2-blank input.

Sequential

![Alt text](readme-img/11chars-2blanks_sequential.png?raw=true)


8-core

![Alt text](readme-img/11chars-2blanks_8core.png?raw=true)


16-core

![Alt text](readme-img/11chars-2blanks_16core.png?raw=true)


The CPUs:
![Alt text](readme-img/cpus.png?raw=true)
