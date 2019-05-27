# Git throughput provided by the tool

This document provides a benchmark to show what is the current reachable throughput provided by the tool.

## Hardware

This benchmark has been run on a MacBook Pro with 2.3 GHz Intel Core i5 and 16 GB 2133 MHz LPDDR3

### Results

The tests has been run against Gerrit 3.0, running on localhost and on Gerrithub.

#### Localhost

* Repo size: 100Kb
* Protocol: HTTP

| #Users   |      Command  |  #req/min |
|----------|:-------------:|------:|
| 1   | clone | 174 |
| 10  | clone | 174 |
| 100 | clone | 174 |
| 1   | push  | 150 |
| 10  | push  | 266 |
| 100 | push  | 269 |
| 1   | fetch | 177 |
| 10  | fetch | 183 |
| 100 | fetch | 185 |

#### Gerrithub

* Repo size: 300Kb
* Protocol: SSH

| #Users   |      Command  |  #req/min |
|----------|:-------------:|------:|
| 1   | clone | 61 |
| 10  | clone | 67 |
| 100 | clone | 67 |
| 1   | fetch | 60 |
| 10  | fetch | 61 |
| 100 | fetch | 61|

##### Notes

* The aim of this quick benchmark wasn't to measure the exact throughput of the tool, but it was just meant to give a rough idea
of what is the possible number of requests
* Different protocols and repo size where used for the 2 environment, this doesn't make the results comparable, but, again, this wasn't the
aim of this benchmark
* Gerrithub.io had a peak of 2 git upload pack per second in the last month, but the average was way lower

##### Conclusions

The tool can currently provide 1 git-upload-pack/sec on a small repo, hence it doesn't allow to run a meaningful load test from a single laptop.

The load test need to be distributed on different machines to reach a significant number of requests or it requires to be run on
a much bigger machine than a normal laptop.
 