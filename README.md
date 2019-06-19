### Raft
#### Workshop on Consensus Algorithms

The goal of this workshop is to implement simple distributed system (log storage) with strong consistency. It will be based on Raft-paper (https://raft.github.io/raft.pdf) and influenced by MIT course on Distributed Systems (https://pdos.csail.mit.edu/6.824/labs/lab-raft.html).

Workshop Plan:

1. Setup provided project template. Template includes:
    1. Protocol described in paper in protobuf
    2. Integration & unit tests
    3. Some helpers
2. Start implementation with leader election:
    1. Implement RequestVote client & server parts. Timer to Election
    2. Handle basic network partitioning cases
3. Continue with agreement tests:
    1. Basic case: Leader broadcast logs to Followers
    2. Healing the node that is far behind Leader
    3. Electing nodes with history that at least up to date to majority
    
To be continued