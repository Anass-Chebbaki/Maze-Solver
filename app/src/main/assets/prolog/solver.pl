rows(14).
cols(14).

valid_cell(R, C) :-
    rows(MaxR), cols(MaxC),
    R >= 0, R < MaxR,
    C >= 0, C < MaxC.

neighbor(R, C, NR, C) :- NR is R - 1, valid_cell(NR, C).
neighbor(R, C, NR, C) :- NR is R + 1, valid_cell(NR, C).
neighbor(R, C, R, NC)  :- NC is C - 1, valid_cell(R, NC).
neighbor(R, C, R, NC)  :- NC is C + 1, valid_cell(R, NC).

heuristic(R1, C1, R2, C2, H) :-
    H is abs(R2 - R1) + abs(C2 - C1).

% A*
astar(Walls, SR, SC, GR, GC, Path, Visited) :-
    heuristic(SR, SC, GR, GC, H),
    astar_loop(Walls, GR, GC,
               [[H, 0, SR, SC, [r(SR,SC)]]],
               [], [], RevVisited, RevPath),
    reverse(RevPath, Path),
    reverse(RevVisited, Visited).

astar_loop(_, GR, GC, [[_, _, GR, GC, P]|_], _, Vis, Vis, P) :- !.
astar_loop(Walls, GR, GC, [[_, G, R, C, P]|Rest], Closed, VisAcc, Vis, Path) :-
    \+ member(r(R,C), Closed),
    findall(
        [F1, G1, NR, NC, [r(NR,NC)|P]],
        (   neighbor(R, C, NR, NC),
            \+ member(r(NR,NC), Closed),
            \+ member(wall(NR,NC), Walls),
            G1 is G + 1,
            heuristic(NR, NC, GR, GC, H1),
            F1 is G1 + H1
        ),
        Successors
    ),
    append(Rest, Successors, OpenRaw),
    sort(OpenRaw, Open),
    astar_loop(Walls, GR, GC, Open,
               [r(R,C)|Closed], [r(R,C)|VisAcc], Vis, Path).
astar_loop(Walls, GR, GC, [[_, _, R, C, _]|Rest], Closed, VisAcc, Vis, Path) :-
    member(r(R,C), Closed),
    astar_loop(Walls, GR, GC, Rest, Closed, VisAcc, Vis, Path).

% BFS
bfs(Walls, SR, SC, GR, GC, Path, Visited) :-
    bfs_loop(Walls, GR, GC,
             [[SR, SC, [r(SR,SC)]]],
             [], [], RevVisited, RevPath),
    reverse(RevPath, Path),
    reverse(RevVisited, Visited).

bfs_loop(_, GR, GC, [[GR, GC, P]|_], _, Vis, Vis, P) :- !.
bfs_loop(Walls, GR, GC, [[R, C, P]|Queue], Closed, VisAcc, Vis, Path) :-
    \+ member(r(R,C), Closed),
    findall(
        [NR, NC, [r(NR,NC)|P]],
        (   neighbor(R, C, NR, NC),
            \+ member(r(NR,NC), Closed),
            \+ member(wall(NR,NC), Walls)
        ),
        Successors
    ),
    append(Queue, Successors, NewQueue),
    bfs_loop(Walls, GR, GC, NewQueue,
             [r(R,C)|Closed], [r(R,C)|VisAcc], Vis, Path).
bfs_loop(Walls, GR, GC, [[R, C, _]|Queue], Closed, VisAcc, Vis, Path) :-
    member(r(R,C), Closed),
    bfs_loop(Walls, GR, GC, Queue, Closed, VisAcc, Vis, Path).

% DFS
dfs(Walls, SR, SC, GR, GC, Path, Visited) :-
    dfs_loop(Walls, GR, GC,
             [[SR, SC, [r(SR,SC)]]],
             [], [], RevVisited, RevPath),
    reverse(RevPath, Path),
    reverse(RevVisited, Visited).

dfs_loop(_, GR, GC, [[GR, GC, P]|_], _, Vis, Vis, P) :- !.
dfs_loop(Walls, GR, GC, [[R, C, P]|Stack], Closed, VisAcc, Vis, Path) :-
    \+ member(r(R,C), Closed),
    findall(
        [NR, NC, [r(NR,NC)|P]],
        (   neighbor(R, C, NR, NC),
            \+ member(r(NR,NC), Closed),
            \+ member(wall(NR,NC), Walls)
        ),
        Successors
    ),
    append(Successors, Stack, NewStack),
    dfs_loop(Walls, GR, GC, NewStack,
             [r(R,C)|Closed], [r(R,C)|VisAcc], Vis, Path).
dfs_loop(Walls, GR, GC, [[R, C, _]|Stack], Closed, VisAcc, Vis, Path) :-
    member(r(R,C), Closed),
    dfs_loop(Walls, GR, GC, Stack, Closed, VisAcc, Vis, Path).

% Greedy
greedy(Walls, SR, SC, GR, GC, Path, Visited) :-
    heuristic(SR, SC, GR, GC, H),
    greedy_loop(Walls, GR, GC,
                [[H, SR, SC, [r(SR,SC)]]],
                [], [], RevVisited, RevPath),
    reverse(RevPath, Path),
    reverse(RevVisited, Visited).

greedy_loop(_, GR, GC, [[_, GR, GC, P]|_], _, Vis, Vis, P) :- !.
greedy_loop(Walls, GR, GC, [[_, R, C, P]|Rest], Closed, VisAcc, Vis, Path) :-
    \+ member(r(R,C), Closed),
    findall(
        [H1, NR, NC, [r(NR,NC)|P]],
        (   neighbor(R, C, NR, NC),
            \+ member(r(NR,NC), Closed),
            \+ member(wall(NR,NC), Walls),
            heuristic(NR, NC, GR, GC, H1)
        ),
        Successors
    ),
    append(Rest, Successors, OpenRaw),
    sort(OpenRaw, Open),
    greedy_loop(Walls, GR, GC, Open,
                [r(R,C)|Closed], [r(R,C)|VisAcc], Vis, Path).
greedy_loop(Walls, GR, GC, [[_, R, C, _]|Rest], Closed, VisAcc, Vis, Path) :-
    member(r(R,C), Closed),
    greedy_loop(Walls, GR, GC, Rest, Closed, VisAcc, Vis, Path).