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

## Deployment
The project includes a dictionary: `src/main/resources/dictionary.txt`. This file may be replaced with a custom dictionary.

The project expects `src/main/resources/password.txt` to contain the password used to authenticate users submitting a solve operation. This file must be present prior to building the war.

`./gradlew war`

Deploy in any container server (tested on Tomcat 8 and 9).

## Performance
For 7 letters including 2 blanks, modern hardware has no problem producing a complete solution in seconds. But for larger inputs, the problem becomes interesting from a performance/parallelism perspective.

### Parallelization strategy
The solver generates all combinations including blanks, then uses parallelStream() to permute them.

Additionally, if a given combination is large enough, it further uses parallelStream() to parallelize the actual permuting. Specifically, the algorithm considers each character in the string and swaps it to the front to create a starting point. Then it parallelize permuting these starting points, leaving the first character alone.

### Benchmarks
The longest scrabble word (15 characters):

![Alt text](readme-img/longest-scrabble-word.png?raw=true)

2-blank 11 characters:

![Alt text](readme-img/11chars-2blanks.png?raw=true)

The CPUs:

![Alt text](readme-img/cpu-max.png?raw=true)
