# scrabble-solver-servlet
A Java servlet for helping at Scrabble/Words with Friends =)

## Deployment
The project includes a dictionary: `src/main/resources/dictionary.txt`. This file may be replaced with a custom dictionary.

The project expects `src/main/resources/password.txt` to contain the password used to authenticate users submitting a solve operation. This file must be present prior to building the war.

`./gradlew war`

Deploy in any container server (tested on Tomcat 8 and 9).

## Algorithm
Combinations, including blanks, are generated for the input string. These combinations are then permuted, and each permutation is checked against the dictionary to find solutions.

The permutation tree is pruned if it is found that no solutions are possible from that subtree. A Trie data structure is used to optimize this.

## Performance
As of 2022 on Amazon C6a EC2 instances (3rd generation AMD EPYC processors, turbo frequency of 3.6 GHz), the most demanding practical parameters for this program can still challenge a serial implementation of the above algorithm. A 15-character input with 2 blanks (181,286,001,075,825 permutations) requires more than 10 minutes to solve. As such, this problem is interesting from a parallelization standpoint.

## Parallelization
The first step of the algorithm, generating combinations, is done serially. Processing the set of combinations is parallelized by submitting each to a Java ForkJoinPool. The innate work-stealing of the Fork/Join framework works quite well for this scenario.

## Benchmarks
15-characters, 2 blanks

### Physical server - Intel Xeon E3-1220 v6 @ 3.00 GHz
| Cores    | Time (s) | Speedup | Efficiency |
| -------- | -------- | ------- | ---------- |
| Serial   | 600      |         |            |
| 4-core   | 202      | 2.97x   | 74.2%      |

### Amazon C6a EC2 instances (3rd generation AMD EPYC processors, turbo frequency of 3.6 GHz)
| Cores    | Time (s) | Speedup | Efficiency |
| -------- | -------- | ------- | ---------- |
| Serial   | 643      |         |            |
| 8-core   | 125      | 5.1x    | 64.3%      |
| 16-core  | 58       | 11.1    | 69.3%      |
| 32-core  | 28       | 23.0x   | 71.8%      | 
| 48-core  | 18       | 35.7x   | 74.4%      | 
| 64-core  | 13       | 49.5x   | 77.3%      |
| 96-core  | 10       | 64.3x   | 67.0%      | 

## Version History

| Version | Feature |
| ------- | ------- |
| 1       | Port https://github.com/dashih/ScrabbleSolver to a Java servlet |
| 2       | Bootstrap 5 UI overhaul |
| 3       | Security - password authentication |
| 4       | Cancelable operations |
| 5       | Reconnect to running operations |
| 6       | PERFORMANCE - Rewrite of the parallel solver to use Java ForkJoin. |
| 7       | PERFORMANCE - Use a Trie to prune the permutation tree |

### Version 1
This program began as a college project right when I learned how to generate combinations and permutations for a string. This project is archived at https://github.com/dashih/ScrabbleSolver.

Version 1 of this GitHub project is the port of the original program to a Java servlet. The UI was basic HTML/javascript.

### Version 2
This was the aesthetics release. Bootstrap 5 was used to make the UI pretty.

### Version 3
Security. I don't want everyone to be able to launch an operation that saturates CPU and brings my personal server to its knees.

### Version 4
Being able to cancel running operations requires some thinking and plumbing.

### Version 5
Implemented basic in-memory sessions for solve operations, so you can close the browser and see the solution for your operation later.

### Version 6
The parallel solver in Versions 5 and lower used parallelStream() to parallelize processing of the set of combinations. For long strings where permutation cost increases quickly, some cores could be stuck with long pole work long after others finished and became idle. Also, the paralllel solver updated a numProcessed AtomicLong for every permutation, which was a bottleneck with killed performance.

This version implemented a divide-and-conquer approach using Java's Fork/Join framwork. If strings are large enough, individual string permutation is parallelized using the following method. Take each character in the permutation range and create a child string that starts with that character. These child strings can be permuted from the next character onwards to produce the same results as permuting the parent string. The child string permutations can be done in parallel, and the permutation range is reduced by one. This process is recursed in a divide-and-conquer manner until the permutation range is small enough to process directly.

Parallelization efficiency was 99% with this version for large inputs. The sheer amount of work outweighed any overhead.

### Version 7
I finally became wise to the optimization that matters most. Given that the dictionary of valid words is extremely small compared to the number of permutations for a large input, there's a massive opportunity to prune the permutation tree. The algorithm can bail whenever the current permutation point can never result in a valid word. For example, if the current permutation point is "VC...", the we can stop permuting even if there are 100 following characters, because no valid word begins with "VC". For a large input, a prune early on like this can skip literally hours of work. A Trie is the perfect data structure for efficiently computing whether a string is a beginning point for any word.

I found that 99.99% of permutations can be pruned for a typical large input. At this rate, no individual operation is ever long enough to require parallelizing permutation of individual strings as was done in version 6. So that algorithm was removed.

With operations being so quick, the overhead of submitting tasks for every combination became the bottleneck. This version implements batch processing of the combinations. Still using the Fork/Join framework, the set of combinations is continually divided in half until the batch size is manageable.

The performance improvement in this version is astounding. For a 15-character 0-blanks input, the original parallel solver (versions 5 and below) required 2 days to produce a solution; the serial algorithm was not even worth running. The parallel solver in version 6 required 40 minutes. The **serial** solver in this version requires seconds.

As such, this version brought about a new benchmark, 15-characters with 2-blanks. Parallelization gains are good. Efficiency is in the 65-80% range, which is respectable.

## Eye candy

![Alt text](readme-img/15chars-2blanks_96cores.png?raw=true)

![Alt text](readme-img/CPU.png?raw=true)
