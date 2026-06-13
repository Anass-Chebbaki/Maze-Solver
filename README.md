<div align="center">

![Maze Solver](docs/banner.png)

# Maze Solver

**An Android app that solves hand-drawn mazes with four classic search algorithms — modeled in first-order logic with Prolog.**

[![License: MIT](https://img.shields.io/badge/license-MIT-4F46E5.svg)](LICENSE)
![Platform](https://img.shields.io/badge/platform-Android-3DDC84.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF.svg?logo=kotlin&logoColor=white)
![SWI-Prolog](https://img.shields.io/badge/reasoning-SWI--Prolog-E05A2B.svg)

</div>

---

## Overview

Maze Solver lets you draw a maze on a 14×14 touch grid, pick one of four search
algorithms, and watch the exploration of the state space and the resulting path
animate in real time on screen.

What makes it different from a typical pathfinding demo is *where the thinking
happens*: the algorithms are **not** written in Kotlin. They are modeled
declaratively in **first-order logic** and executed by an **SWI-Prolog** engine.
The Android app is purely the front-end; it draws the grid, sends the problem to
a Prolog reasoner over HTTP, and animates whatever comes back.

The project was built for a university Artificial Intelligence course, following
the state-space search formulation in Russell & Norvig's *Artificial
Intelligence: A Modern Approach*.

> **Scope of this repository.** This repo contains the **Android client only**.
> The reasoning backend (a small Flask server that wraps SWI-Prolog) is a
> separate component — see [Backend](#backend) for the contract the app expects.

## Features

- **Interactive grid** — draw walls and move the start/goal cells with touch.
- **Four algorithms** — A\*, BFS, DFS and Greedy Best-First Search, selectable at runtime.
- **Two-phase animation** — first the explored cells (the *visited* set), then the final path.
- **Live statistics** — visited-node count, path length, and round-trip time per run.
- **Non-blocking UI** — the network call runs on a Kotlin coroutine, so the interface stays responsive while Prolog computes.

## How it works

The maze is treated as a classic state-space search problem: every free cell is a
state, the four cardinal moves are the actions, the transition model returns an
adjacent non-wall cell, and the cost of each step is uniform (1). The grid maps
to an undirected graph `G = (V, E)`; walls remove nodes, and a *closed list*
prevents cycles.

```
  ┌──────────────────┐   HTTP POST /solve   ┌──────────────┐   subprocess   ┌──────────────┐
  │  Android (Kotlin) │ ───────────────────▶ │ Flask (Python)│ ─────────────▶ │  SWI-Prolog  │
  │  grid · touch · UI│                      │    bridge     │                │  solver.pl   │
  │                   │ ◀─────────────────── │               │ ◀───────────── │              │
  └──────────────────┘     JSON  {path,      └──────────────┘     stdout      └──────────────┘
                                  visited}
```

1. The user draws walls and presses **Solve**.
2. The app sends a JSON payload with the grid, start, goal and chosen algorithm.
3. The server builds a Prolog query and launches `swipl`.
4. Prolog runs the algorithm and prints the `PATH` and `VISITED` lists.
5. The server parses the output and replies with JSON.
6. The app animates the exploration, then the final path.

### The four algorithms

All four share the same search skeleton in Prolog — only the management of the
*open list* differs.

| Algorithm | Complete | Optimal | Open list | Heuristic |
|-----------|:--------:|:-------:|-----------|-----------|
| **BFS**    | ✅ | ✅ | FIFO queue | — |
| **DFS**    | ✅ | ❌ | LIFO stack | — |
| **A\***    | ✅ | ✅ | priority by `f = g + h` | Manhattan |
| **Greedy** | ✅ | ❌ | priority by `h` | Manhattan |

*Completeness holds because the grid is a finite graph searched with a closed
list. A\* is optimal because the Manhattan heuristic is admissible and consistent
on a 4-connected, unit-cost grid.*

## Tech stack

- **Android / Kotlin** — custom `View` rendering, touch handling, coroutines.
- **Python / Flask** — lightweight HTTP bridge (not included here, see below).
- **SWI-Prolog** — the inference engine running the search logic.

## Project structure

```
app/
└── src/main/
    ├── java/com/example/mazesolver/
    │   ├── MainActivity.kt   # orchestration, coroutines, UI wiring
    │   ├── MazeView.kt       # custom View: rendering, touch, animation
    │   └── PrologEngine.kt    # HTTP client → Prolog backend
    └── res/                  # layouts, themes, launcher icons
```

| File | Responsibility |
|------|----------------|
| `MazeView.kt` | `onDraw` renders the grid; `onTouchEvent` paints walls/start/goal; `animateResult` runs the two-phase animation. |
| `PrologEngine.kt` | Builds the request, POSTs it via `HttpURLConnection`, parses the JSON response into `List<Cell>`. |
| `MainActivity.kt` | Wires up buttons and launches the solve on `Dispatchers.IO`. |

## Getting started

### Prerequisites

- Android Studio (Koala or newer) with the Android SDK.
- A reasoning backend reachable over the network — see [Backend](#backend).

### Build and run

1. Clone the repository and open it in Android Studio.
2. Start the backend and note the host machine's IP address.
3. Set the server address (see [Configuration](#configuration)).
4. Make sure the phone and the backend are on the **same local network**.
5. Run the app on a device or emulator, draw a maze, and press **Solve**.

### Configuration

The backend address lives in `PrologEngine.kt`:

```kotlin
private val SERVER_URL = "http://172.20.10.4:5000/solve"
```

Replace the IP with your backend's address. For a cleaner setup, consider moving
this into a `BuildConfig` field or a settings screen instead of hard-coding it —
the current value is just a development default and changes with the network.

> The address shown is a **private LAN address** (the subnet an iPhone Personal
> Hotspot hands out). It is not reachable from the public internet, so committing
> it carries no security risk; it is kept only as a placeholder.

## Backend

The Prolog reasoning server is a separate component and is **not** included in
this repository. The app talks to it over plain HTTP and expects the following
contract.

**`POST /solve`** — request:

```json
{
  "algorithm": "astar",
  "start": { "row": 2, "col": 2 },
  "goal":  { "row": 11, "col": 11 },
  "walls": [ { "row": 3, "col": 4 } ]
}
```

Response:

```json
{
  "path":    [ { "row": 2, "col": 2 } ],
  "visited": [ { "row": 2, "col": 2 } ]
}
```

`algorithm` is one of `astar`, `bfs`, `dfs`, `greedy`. A `GET /ping` endpoint is
also used as a health check.

## Results

Benchmark on a 14×14 grid, no extra walls, start `(2,2)` → goal `(11,11)`:

| Algorithm | Visited nodes | Path length | Optimal |
|-----------|:-------------:|:-----------:|:-------:|
| A\*       | 99  | 19  | ✅ |
| BFS       | 183 | 19  | ✅ |
| DFS       | 156 | 133 | ❌ |
| Greedy    | 18  | 19  | ✅ |

A\* and BFS both find the optimal 19-cell path, but A\* expands ~46% fewer nodes
thanks to the heuristic. Greedy explores the fewest nodes on an open grid because
the heuristic guides it straight to the goal — though it can lose optimality once
complex walls are added. The wall-clock time is dominated by process startup and
network latency rather than the search itself, so **visited nodes** is the
meaningful metric here.

## Limitations

- **Network dependency** — the app needs the backend reachable on the same Wi-Fi network; it is not self-contained.
- **Latency** — each request pays the cost of an HTTP round-trip plus spawning a fresh `swipl` process.
- The backend ships with Flask's development server, which is not meant for production use.

## Roadmap

- [ ] Native Prolog via the Android NDK — fully offline, no server.
- [ ] Procedural maze generation (e.g. randomized DFS) with a guaranteed solution.
- [ ] Step-by-step mode showing the open and closed lists per step.
- [ ] Diagonal movement (8-connected grid) with an updated heuristic.

## Academic context

Developed for the **Artificial Intelligence** course (A.Y. 2025–2026),
Università Politecnica delle Marche, under prof. Aldo Franco Dragoni. The
search formulation follows Russell & Norvig, *Artificial Intelligence: A Modern
Approach*.

## License

Released under the [MIT License](LICENSE).

## Author

**Anass Chebbaki** — [@Anass-Chebbaki](https://github.com/Anass-Chebbaki)
