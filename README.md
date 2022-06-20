# scrabble-solver-servlet
A Java servlet for helping at Scrabble/Words with Friends =)

## Deployment
The easiest way to deploy this application is Docker.

`docker run --name scrabble-solver --detach --publish 8081:8080 spacechip/scrabble-solver`

Navigate to `http://localhost:8081`. The port can be changed as needed.

### Configuration
| Environment variable                        | Default | Description |
| ------------------------------------------- | -------- | ------- |
| SCRABBLE_SOLVER_PASSWORD_FILE               |          | Setting this causes the UI to show a password field. A user must input the password that matches the value read from this file.
| SCRABBLE_SOLVER_PASSWORD                    |          | Same as SCRABBLE_SOLVER_PASSWORD_FILE except the password is stored directly in the variable |
| SCABBLE_SOLVER_MAX_CONCURRENT_OPERATIONS    | 4        | How many solve operations can execute concurrently? This only matters for concurrent sequential solver operations, because a single parallel solver operation will saturate all CPUs. |
| SCABBLE_SOLVER_PERMUTATION_BATCH_THRESHOLD  | 200      | At what point (numer of strings to permute) does the parallel solver stop breaking up work? Useful for tuning on different environments. |

### Configuring docker deployment
The image is published at https://hub.docker.com/r/spacechip/scrabble-solver. The application runs on the default tomcat port 8080. Simply exposing this port, like in the `docker run` example above, works if password authentication is disabled. But this application can quickly saturate all processors, so it's recommended to set a password if you're running the application on a production server. Unfortunately, in most browsers, the application's client javascript that processes the password will only run on a secure connection (TLS). Therefore, it's additionally recommended to deploy the application container behind a reverse proxy that configures TLS.

This project contains a sample docker-compose that deploys an nginx reverse proxy with a self-signed TLS in front of the application container. It also enables password authentication and overrides some configuration.

```
git clone https://github.com/dashih/scrabble-solver-servlet
cd scrabble-solver-servlet/src/docker
docker-compose up --detach --build
```

Access `https://localhost:44300`.

### Building and manual deployment
This section is really only for contributing to development of this project.

#### Building the application war
A default dictionary is included: `src/main/resources/dictionary.txt`. This file may be replaced with a custom dictionary but it must be included prior to building the war.

```
./gradlew build
./gradlew war
```

Deploy in any container server (tested on Tomcat 9).

#### Building the application docker image
This publishes the image to the local docker daemon:

`./gradlew jibDockerBuild`

To publish to an official docker repository, credentials must be set up in advance.

`./gradlew jib`

## Algorithm
Combinations, including blanks, are generated for the input string. These combinations are then permuted, and each permutation is checked against the dictionary to find solutions.

The permutation tree is pruned if it is found that no solutions are possible from that subtree. A Trie data structure is used to optimize this.

## Performance
As of 2022 on Amazon C6a EC2 instances (3rd generation AMD EPYC processors, turbo frequency of 3.6 GHz), the most demanding practical parameters for this program can still challenge a serial implementation of the above algorithm. A 15-character input with 2 blanks (181,286,001,075,825 permutations) requires more than 10 minutes to solve. As such, this problem is interesting from a parallelization standpoint.

## Parallelization
The first step of the algorithm, generating combinations, is done serially. Processing the set of combinations is parallelized by submitting each to a Java ForkJoinPool. The innate work-stealing of the Fork/Join framework works quite well for this scenario.

## Benchmarks
15-characters, 2 blanks (over 181 trillion permutations)

### MacBook Pro (2019) - Intel Core i9 2.4 GHz
| Cores    | Time (s) | Speedup | Efficiency |
| -------- | -------- | ------- | ---------- |
| Serial   | 472      |         |            |
| 8-core   | 79       | 5.97x   | 74.7%      |

### Ubuntu 20.04 server - Intel Xeon E3-1220 v6 @ 3.00 GHz
| Cores    | Time (s) | Speedup | Efficiency |
| -------- | -------- | ------- | ---------- |
| Serial   | 600      |         |            |
| 4-core   | 180      | 3.33x   | 83.3%      |

### Amazon C6a EC2 instances - Amazon Linux 2 - 3rd generation AMD EPYC processors, turbo frequency of 3.6 GHz
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
| 8       | Docker! |

### Version 1
This project began in college right when I learned the basic algorithm for generating string permutations. The original implemention was in C for no good reason; it was complex, only supported descrambling fixed-length strings, and did not support blanks. The next iteration was a Java command-line application.

Version 1 of this GitHub project is the port of the original program to a Java servlet. The UI was basic HTML/javascript.

### Version 2
This was the aesthetics release. Bootstrap 5 makes the UI pretty.

### Version 3
Security. I don't want everyone to be able to launch an operation that saturates CPU and brings my personal server to its knees.

### Version 4
The ability to cancel operations is mainly a plumbing exercise.

### Version 5
Implemented basic in-memory sessions for solve operations, so solutions can be viewed later after closing a browser tab.

### Version 6
The parallel solver in versions 5 and lower used parallelStream() to parallelize processing of the set of combinations. For long strings where permutation cost increases quickly, some cores could be stuck with long pole work long after others finished and became idle. Also, the paralllel solver updated a numProcessed AtomicLong for every permutation, which was a bottleneck that killed performance.

This version implemented a divide-and-conquer approach using Java's Fork/Join framwork. If strings are large enough, individual string permutation is parallelized using the following method. Take each character in the permutation range and create a child string that starts with that character. These child strings can be permuted from the next character onwards to produce the same results as permuting the parent string. The child string permutations can be done in parallel, and the permutation range is reduced by one. This process is recursed in a divide-and-conquer manner until the permutation range is small enough to process directly.

Parallelization efficiency was 99% with this version for large inputs. The sheer amount of work outweighed any overhead.

### Version 7
I finally became wise to the optimization that matters most. Given that the dictionary of valid words is extremely small compared to the number of permutations for a large input, there's a massive opportunity to prune the permutation tree. The algorithm can bail whenever the current permutation point can never result in a valid word. For example, if the current permutation point is "VC...", the we can stop permuting even if there are 100 following characters, because no valid word begins with "VC". For a large input, a prune early on like this can skip literally hours of work. A Trie is the perfect data structure for efficiently computing whether a string is a beginning point for any word.

I found that 99.99% of permutations can be pruned for a typical large input. At this rate, no individual operation is ever long enough to require parallelizing permutation of individual strings as was done in version 6. So that algorithm was removed.

With operations being so quick, the overhead of submitting tasks for every combination became the bottleneck. This version implements batch processing of the combinations. Still using the Fork/Join framework, the set of combinations is continually divided in half until the batch size is manageable.

The performance improvement in this version is astounding. For a 15-character 0-blanks input, the original parallel solver (versions 5 and below) required 2 days to produce a solution; the serial algorithm was not even worth running. The parallel solver in version 6 required 40 minutes. The **serial** solver in this version requires seconds.

As such, this version brought about a new benchmark, 15-characters with 2-blanks. Parallelization gains are good. Efficiency is in the 65-80% range, which is respectable.

### Version 8
It's 2022 and everything is a docker container. Adds a dependency on Google's jib to easily produce a docker image for this project. Makes a number of things configurable via environment variables.

## Eye candy

![Alt text](readme-img/15chars-2blanks_96cores.png?raw=true)

![Alt text](readme-img/CPU.png?raw=true)
