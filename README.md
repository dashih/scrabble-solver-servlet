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

## Performance
For 7 letters including 2 blanks, modern hardware has no problem producing a complete solution in seconds. But for larger inputs, the problem becomes interesting from a performance/parallelism perspective.

### Parallelization strategy
The solver generates all combinations including blanks, then uses parallelStream() to permute them.

Additionally, if a given combination is large enough, it further uses parallelStream() to parallelize the actual permuting. Specifically, the algorithm considers each character in the string and swaps it to the front to create a starting point. Then it parallelize permuting these starting points, leaving the first character alone.

### Benchmarks
TODO
