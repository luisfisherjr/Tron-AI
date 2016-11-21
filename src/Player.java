import java.util.*;

class Node implements Comparable<Node>{
    public char symbol;
    public int x;
    public int y;
    public int value;
    public int cost;
    public int openAdjacent;
    public int g = Integer.MAX_VALUE;
    public Node parent = null;

    public Node(int x, int y, int value) {
        this.x = x;
        this.y = y;
        this.value = value;
        this.cost = 0;
        this.openAdjacent = 0;
        this.symbol = ' ';
    }

    public Node(Node toCopy) {
        this.x = toCopy.x;
        this.y = toCopy.y;
        this.value = toCopy.value;
        this.cost = toCopy.cost;
        this.g = toCopy.g;
        this.parent = toCopy.parent;
        this.openAdjacent = toCopy.openAdjacent;
        this.symbol = toCopy.symbol;
    }

    @Override
    public boolean equals(Object obj) {

        if(obj==null || !(obj instanceof Node)) return false;

        Node other = (Node)obj;
        if (other == this) return true;
        if (this.x != other.x) return false;
        if (this.y != other.y) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return this.y + this.x * 1000;
    }

    @Override
    public String toString() {
        return ("Node(x:" + x + ", y:" + y +", v:" + value + ", c:" + cost + ")");
    }

    @Override
    public int compareTo(Node o) {
        if (this.y != o.y) return this.y - o.y;
        return this.x - o.x;
    }
}

class Player {

    final static int WIDTH = 30;
    final static int HEIGHT = 20;

    static Map<Node, List<Node>> neighbors;
    static List<Node> nodes;

    public static void main(String args[]) {

        nodes = new ArrayList<Node>();
        neighbors = new HashMap<Node, List<Node>>();

        Node current; // current node position
        List currNeighbors; // list of node neighbors

        // adds the nodes to a list in correct order
        for(int index = 0; index < WIDTH * HEIGHT; index++ ) nodes.add(new Node(index % WIDTH, index / WIDTH, -1));

        // the game board is an adjacency list using a map to represent it Map<Node, List<Node>>
        for(int index = 0; index < WIDTH * HEIGHT; index++ ) {

            current = nodes.get(index);
            currNeighbors = new ArrayList<Node>();

            if (index / WIDTH > 0) currNeighbors.add(nodes.get(index - WIDTH)); // add top neighbor
            if (index / WIDTH < HEIGHT - 1) currNeighbors.add(nodes.get(index + WIDTH)); // add bottom neighbor
            if (index % WIDTH > 0)  currNeighbors.add(nodes.get(index - 1)); // adds left neighbor
            if (index % WIDTH < WIDTH - 1) currNeighbors.add(nodes.get(index + 1)); // adds right neighbor

            current.openAdjacent = currNeighbors.size();
            neighbors.put(current, currNeighbors);
        }

        Scanner in = new Scanner(System.in);

        long start; // start time of each loop
        long end; // end time of each loop

        String bestMove = ""; // UP DOWN LEFT RIGHT

        List<Node> evaluated = null;

        Node player = null; // current player
        Map<Node, List<Node>> playerGraph = null;

        List<Node> enemies = null;
        List<Map<Node, List<Node>>> enemyGraphs = null;

        Set<Integer> wallsToErase = new HashSet<Integer>();
        int wallsToErasePreviousSize = wallsToErase.size();

        // game loop
        while (true) {

            start = System.currentTimeMillis();

            enemies = new ArrayList<Node>();

            int N = in.nextInt(); // total number of players (2 to 4).
            int P = in.nextInt(); // your player number (0 to 3).

            for (int i = 0; i < N; i++) {

                int X0 = in.nextInt(); // starting X coordinate of lightcycle (or -1)
                int Y0 = in.nextInt(); // starting Y coordinate of lightcycle (or -1)
                int X1 = in.nextInt(); // starting X coordinate of lightcycle (can be the same as X0 if you play before this player)
                int Y1 = in.nextInt(); // starting Y coordinate of lightcycle (can be the same as Y0 if you play before this player)

                if (X0 == -1) {
                    wallsToErase.add(i);
                    continue;
                }

                current = nodes.get(X1 + Y1 * WIDTH); // gets node with coordinates X1,Y1
                current.value = i; // updates node with correct wall
                current.openAdjacent = 0; // walls have no moves
                current.cost = 0;

                for (Node n : neighbors.get(current)) if (n.value == -1) n.openAdjacent--; // updates neighbors of wall

                if (i == P) player = current;
                else enemies.add(current);
            }

            // if there has been a change in wallsToErase
            // resets walls of enemy that died to open space and refreshes openAdjacent values
            // checked against all entries that have been deleted in case more than 1 enemy dies in same turn
            if (wallsToErasePreviousSize != wallsToErase.size()) {
                for (Node node : neighbors.keySet()) {
                    if (wallsToErase.contains(node.value)) {
                        node.value = -1;
                    }
                }
                for (Node node : neighbors.keySet()) {
                    if (node.value == -1) {
                        node.openAdjacent = 4;
                        for (Node neighbor : neighbors.get(node)) if (neighbor.value != -1) node.openAdjacent--;
                    } else node.openAdjacent = 0;
                }
                wallsToErasePreviousSize = wallsToErase.size();
            }

            playerGraph = newGraph(player, enemies);

            List<Node> reachableEnemies = new ArrayList<Node>();

            Node closestEnemy = null;
            int closestDistance = Integer.MAX_VALUE;
            for (Node n : playerGraph.keySet()) {
                if (enemies.contains(n)) {
                    reachableEnemies.add(n);
                    if (n.g < closestDistance) {
                        closestEnemy = n;
                        closestDistance = n.g;
                    }
                }
            }

            System.err.println("MOVES REMAINING: " + (playerGraph.size() - 1 - reachableEnemies.size()));

            if (reachableEnemies.size() > 0) {

                for (Node n : reachableEnemies) System.err.println("DISTANCE FROM ENEMY-" + n.value + ": " + n.g + "\nENEMY-POS: " + n);
                System.err.println("CLOSEST ENEMY-" + closestEnemy.value + ":" + closestEnemy);

                List<Node> playerList = new ArrayList<Node>(); // list to pass player as second argument
                playerList.add(player);

                enemyGraphs = new ArrayList<Map<Node, List<Node>>>();
                for (Node n : reachableEnemies) enemyGraphs.add(newGraph(n, playerList));// creates graph to nodes that player can reach

                evaluated = new ArrayList<Node>();

                voronoiDiagramLayer(playerGraph, player, enemyGraphs, reachableEnemies, new ArrayList<Node>());

                //showVoronoiDiagram(); // used to display voronoi diagram layer

                setCost(playerGraph, player, enemyGraphs, reachableEnemies, 2, evaluated);
                //alphabeta(playerGraph, player, 3, Integer.MIN_VALUE, Integer.MAX_VALUE, true);

                System.err.println("CURRENT-POS:" + player);
                for (Node e: evaluated)  System.err.println("EVALUATED-POS" + e);
                bestMove = nextMoves(playerGraph, player, enemies);

                //System.err.println("PLAYER-POS: " + player + " " + P);
                //for (Node n : playerGraph.get(player)) System.err.println("POSSIBLE-MOVES: " + n);



                //showFreeSpaces(); // show how many open spaces are around node
                //showSpaceValue(); // show if node is empty or a players wall
                showCost(); // show cost given to all nodes
            }
            else {

                System.err.println("ENEMY UNREACHABLE");

                /* must create a fill method */
                alphabeta(playerGraph, player, 3, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
                bestMove = nextMoves(playerGraph, player, new ArrayList<Node>());

                System.err.println("PLAYER-POS: " + player);
                for (Node n : playerGraph.get(player)) if (!enemies.contains(n)) System.err.println("POSSIBLE-MOVES: " + n);
            }

            System.out.println(bestMove); // output

            end = System.currentTimeMillis();
            System.err.println("TURN MILLIS: " + (end - start));
        }
    }

    // picks next best move if player can reach any enemy
    // this function is working as intended
    public static String nextMoves(Map<Node, List<Node>> playerGraph, Node player, List<Node> enemies) {

        boolean isPlayerFirst = true;
        Node nextBestMove = null;
        int highestCost = Integer.MIN_VALUE;

        System.err.println("current Pos: " + player);

        for (Node node: playerGraph.get(player)) {

//            for (Node e: enemies) {
//                if (neighbors.get(e).contains(node) && (e.value < player.value)) {
//                    isPlayerFirst = false;
//                    break;
//                }
//            }
//
//            if (!isPlayerFirst) {
//                isPlayerFirst = true;
//                continue;
//            }

            System.err.println("possible Move: " + node);

            if (node.cost > highestCost) { // finds non enemy nodes with highest cost
                highestCost = node.cost;
                nextBestMove = node;
            }
        }

        System.err.println("picked Move: " + nextBestMove);

        if (player.y - 1 == nextBestMove.y) return "UP";
        else if (player.x + 1 == nextBestMove.x) return "RIGHT";
        else if (player.y + 1 == nextBestMove.y) return "DOWN";
        else if (player.x - 1 == nextBestMove.x) return "LEFT";
        return "@.@";
    }

    // creates a tree using the graph, deep copy of nodes     maybe change?
    public static Map<Node, List<Node>>newGraph(Node player, List<Node> enemies) {

        Map<Node, Integer> enemyValues = new HashMap<Node, Integer>();

        for(Node e: enemies) {
            Node n = nodes.get(e.x + e.y * WIDTH);
            enemyValues.put(n,e.value);
            n.value = -1;
        }

        Map<Node, List<Node>> graphOut = new HashMap<Node, List<Node>>();

        Queue<Node> frontier = new LinkedList<Node>();
        List<Node> explored = new ArrayList<Node>();

        for(Node n: nodes) n.g = Integer.MAX_VALUE;
        player = nodes.get(player.x + player.y * WIDTH);
        player.g = 0;

        frontier.offer(new Node(player));

        List<Node> children;
        Node parent;
        Node temp;

        while (!frontier.isEmpty()) {

            parent = frontier.poll();
            children = new ArrayList<Node>();

            explored.add(parent);

            if (enemies.contains(parent)) {
                graphOut.put(parent, children);
                continue;
            }

            Integer value = null;
            for (Node child : neighbors.get(nodes.get(parent.x + parent.y * WIDTH))) {

                if (explored.contains(child)) continue;
                else if (child.value != -1) continue;
                if (!frontier.contains(child)) {

                    child.g = parent.g + 1;
                    child.parent = parent;

                    value = enemyValues.get(child);
                    if (value != null) child.value = value;

                    temp = new Node(child);
                    children.add(temp);
                    frontier.add(temp);
                }
            }
            graphOut.put(parent, children);
        }

        for(Node n: enemyValues.keySet()) {
            n.value = enemyValues.get(n);
        }

        return  graphOut;
    }

    // fixing to work with multiple graphs
    // sets cost of move in a graph with voronoi DiagramLayer & voronoiDistanceLayer recursive
    public static void setCost(Map<Node, List<Node>> playerGraph, Node player,
                               List<Map<Node, List<Node>>> enemyGraphs, List<Node> enemies, int depth, List<Node> evaluated) {

        List<Node> children = playerGraph.get(player);

        if ((depth == 0) || (children.size() == 0)) return;

        Map<Node, List<Node>> childGraph;

        int nodesCloserToPlayer = 0;
        int divider = 1;
        int playerValue = player.value;

        for(Node child : children) {

            if ((child.value != -1) && evaluated.contains(child)) continue;
            child.value = playerValue;

            evaluated.add(child);
            childGraph = newGraph(child, enemies);

            for(Node e: enemies) if  (childGraph.keySet().contains(e)) divider++;

            voronoiDiagramLayer(childGraph, child, enemyGraphs, enemies, evaluated);

            nodesCloserToPlayer = 0;
            for (Node node: neighbors.keySet()) if (node.symbol == '+') nodesCloserToPlayer++;
            //for (Node node: neighbors.keySet()) if (node.symbol == 'P' || node.symbol == '@') pCount++;

            child.cost = nodesCloserToPlayer + (childGraph.size() - divider) / divider;
            Node temp = nodes.get(child.x + child.y * WIDTH);
            temp.cost = child.cost;

            setCost(childGraph, child, enemyGraphs, enemies, depth - 1, evaluated);
            temp.value = -1;
            child.value = -1;
        }
    }

    // used in voronoiDiagramLayer to calculate distance from player/enemy
    public static void voronoiDistanceLayer(Map<Node, List<Node>> playerGraph,
                                            Map<Node, List<Node>> enemyGraph, List<Node> evaluated) {

        if (enemyGraph == null) {

            for (Node n: nodes) if (n.value == -1) n.value = -1;
            for (Node n: playerGraph.keySet()) nodes.get(n.x + n.y * WIDTH).value = n.g;
        }
        else {

            // put in sets into list and then sorts
            Node[] playerNodes = new Node[playerGraph.size()];
            playerGraph.keySet().toArray(playerNodes);
            Arrays.sort(playerNodes);

            // put in sets into list and then sorts
            Node[] enemyNodes = new Node[enemyGraph.size()];
            enemyGraph.keySet().toArray(enemyNodes);
            Arrays.sort(enemyNodes);

            int i1 = 0;
            int i2 = 0;
            int compareResult = 0;
            int costResult = 0;

            while ((i1 < playerNodes.length) || (i2 < enemyNodes.length)) {

                if (i1 >= playerNodes.length) {
                    i2++;
                    continue;
                } else if (i2 >= enemyNodes.length) {
                    i1++;
                    continue;
                }

                compareResult = playerNodes[i1].compareTo(enemyNodes[i2]);
                if (compareResult == 0) {
                    i1++;
                    i2++;

                    for (Node n : evaluated) if (n.equals(playerNodes[i1 - 1])) continue;

                    costResult = enemyNodes[i2 - 1].g - playerNodes[i1 - 1].g;

                    if (costResult < playerNodes[i1 - 1].cost)
                        playerNodes[i1 - 1].cost = costResult;
                    nodes.get(playerNodes[i1 - 1].x + playerNodes[i1 - 1].y * WIDTH).cost = costResult;
                } else if (compareResult > 0) i2++;
                else i1++;
            }
        }
    }


    //gives symbols to each node and creates a voronoi diagram
    public static void voronoiDiagramLayer(Map<Node, List<Node>> playerGraph, Node player,
                                           List<Map<Node, List<Node>>> enemyGraphs,List<Node> enemies, List<Node> evaluated) {

        Node closestEnemy = null;
        int closestDistance = Integer.MAX_VALUE;
        for (Node n : playerGraph.keySet()) {
            if (enemies.contains(n)) {
                if (n.g < closestDistance) {
                    closestEnemy = n;
                    closestDistance = n.g;
                }
            }
        }

        Map<Node, List<Node>> closestEnemyGraph = null;

        for(int index = 0; index < enemies.size(); index++) {
            if (enemies.get(index).equals(closestEnemy)) closestEnemyGraph = enemyGraphs.get(index);
        }

        voronoiDistanceLayer(playerGraph, closestEnemyGraph, evaluated);

        Set unreachable =  new HashSet<Node>();
        Set reachablePlayerNodes = new HashSet<Node>();
        List<Set<Node>> reachableEnemyNodes = new ArrayList<Set<Node>>();

        reachablePlayerNodes.addAll(playerGraph.keySet());

        Set<Node> temp;

        for(Map<Node, List<Node>> graph: enemyGraphs) {
            temp = new HashSet<Node>();
            temp.addAll(graph.keySet());
            reachableEnemyNodes.add(temp);
        }

        unreachable.addAll(neighbors.keySet());
        unreachable.removeAll(reachablePlayerNodes);
        for(Set<Node> enemyNodes: reachableEnemyNodes) unreachable.removeAll(enemyNodes);

        for(Node node: neighbors.keySet()) {

            if (node.value == -1) {
                if (unreachable.contains(node)) node.symbol = '#';
                else if (node.cost < 0) node.symbol = '-'; // nodes that enemy can reach first, currently all are 1 symbol
                else if (node.cost > 0) node.symbol = '+'; // nodes that player can reach first
                else node.symbol = '?'; // battle front where enemy can possibly reach before player
            }
            else {
                if (player.equals(node)) node.symbol = 'X';
                else if (enemies.contains(node)) node.symbol = (char) (64 + node.value);
                else {
                    node.symbol = '#';
                }
            }
        }
    }

    private static Node alphabeta(Map<Node, List<Node>> playerGraph, Node player, Integer depth,
                                  Integer alpha, Integer beta, Boolean maximizingPlayer) {

        List<Node> children = playerGraph.get(player);

        if ((depth == 0) || (children.size() == 0)) return player;

        int bestValue = 0;
        int value = 0;

        if (maximizingPlayer) {

            bestValue = Integer.MIN_VALUE; // negative infinite

            for (Node node : children) {

                alphabeta(playerGraph, node, depth - 1, alpha, beta, false);
                value = node.cost;

                bestValue = Math.max(bestValue, value);
                alpha = Math.max(alpha, bestValue);
                player.cost = bestValue;

                if (beta <= alpha) break;
            }
            return player;
        }
        else {
            bestValue = Integer.MAX_VALUE; // positive infinite

            for (Node node : children) {

                alphabeta(playerGraph, node, depth - 1, alpha, beta, true);
                value = node.cost;

                bestValue = Math.min(bestValue, value);
                alpha = Math.min(alpha, bestValue);
                player.cost = bestValue;

                if (beta <= alpha) break;
            }
            return player;
        }
    }

    public static void showCost() {
        for(int index = 0; index < nodes.size(); index++) {
            System.err.print(nodes.get(index % WIDTH + (index / WIDTH) * WIDTH).cost + " ");
            if (index % WIDTH == WIDTH - 1) System.err.println();
        }
    }

    public static void showFreeSpaces() {
        for(int index = 0; index < nodes.size(); index++) {
            System.err.print(nodes.get(index % WIDTH + (index / WIDTH) * WIDTH).openAdjacent + " ");
            if (index % WIDTH == WIDTH - 1) System.err.println();
        }
    }

    public static void showVoronoiDiagram() {
        for(int index = 0; index < nodes.size(); index++) {
            System.err.print(nodes.get(index % WIDTH + (index / WIDTH) * WIDTH).symbol + " ");
            if (index % WIDTH == WIDTH - 1) System.err.println();
        }
    }

    public static void showSpaceValue() {
        int value = 0;
        for(int index = 0; index < nodes.size(); index++) {
            value = nodes.get(index % WIDTH + (index / WIDTH) * WIDTH).value;
            if (value == -1) System.err.print(value);
            else System.err.print(" " + value);
            if (index % WIDTH == WIDTH - 1) System.err.println();
        }
    }
}