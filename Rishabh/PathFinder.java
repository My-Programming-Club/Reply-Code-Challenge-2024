import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class PathFinder {
    static int w;
    static int h;
    static List<Point> goldenPoints = new ArrayList<>();
    static Map<Point, Integer> silverPoints = new HashMap<>();
    static Map<String, Tile> tiles = new HashMap<>();

    static class Point {
        int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        // Override equals and hashCode for proper functionality in a HashMap or HashSet
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Point point = (Point) o;
            return x == point.x && y == point.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    static class Tile {
        String id;
        int cost;
        List<Point> movements;

        Tile(String id, int cost) {
            this.id = id;
            this.cost = cost;
            this.movements = new ArrayList<>();
        }

        void addMovement(Point p) {
            movements.add(p);
        }

        void setMovementsForTileF() {
            // Define movements for Tile F
            addMovement(new Point(1, 0)); // From left to right
            addMovement(new Point(0, 1)); // From left to down (considering (0,0) at the top-left)
            addMovement(new Point(0, -1)); // From left to up
            addMovement(new Point(0, 1)); // From up to down
            addMovement(new Point(1, 1)); // From down to right
            addMovement(new Point(1, -1)); // From up to right
        }
    }

    static class Node {
        Point position;
        Node parent;
        double gCost;
        double hCost;

        double fCost() {
            return gCost + hCost;
        }

        Node(Point position, Node parent, double gCost, double hCost) {
            this.position = position;
            this.parent = parent;
            this.gCost = gCost;
            this.hCost = hCost;
        }
    }

    // Comparator for the priority queue.
    static class NodeComparator implements Comparator<Node> {
        public int compare(Node a, Node b) {
            return Double.compare(a.fCost(), b.fCost());
        }
    }

    // Heuristic function for A* (Manhattan distance in this case).
    static double heuristic(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    // Function to reconstruct the path from the end node.
    static List<Point> reconstructPath(Node current) {
        List<Point> path = new LinkedList<>();
        while (current != null) {
            path.add(0, current.position); // Add to beginning of list to reverse the path.
            current = current.parent;
        }
        return path;
    }

    // A* algorithm implementation.
    static List<Node> aStar(Point start, Point goal, Map<Point, List<Point>> graph, Map<String, Tile> tiles) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(11, new NodeComparator());
        Map<Point, Node> allNodes = new HashMap<>();

        Node startNode = new Node(start, null, 0, heuristic(start, goal));
        openSet.add(startNode);
        allNodes.put(start, startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.position.equals(goal)) {
                return reconstructPathNodes(current);
            }
            // Here we use the previously declared movements list
            List<Point> movements = graph.getOrDefault(current.position, Collections.emptyList());
            // Iterate through the neighboring nodes using the previously declared movements
            // list
            for (Point movement : movements) {
                int newX = current.position.x + movement.x;
                int newY = current.position.y + movement.y;
                Point newPosition = new Point(newX, newY);

                if (isValidMovement(newX, newY, w, h, goldenPoints)) {
                    double tentativeGCost = current.gCost + getTileCost(tiles, newPosition);
                    if (!allNodes.containsKey(newPosition) || tentativeGCost < allNodes.get(newPosition).gCost) {
                        Node neighborNode = new Node(newPosition, current, tentativeGCost,
                                heuristic(newPosition, goal));
                        allNodes.put(newPosition, neighborNode);
                        openSet.add(neighborNode);
                    }
                }
            }
        }

        return Collections.emptyList(); // Return an empty path if no path is found.
    }

    // Function to reconstruct the path from the end node as a list of Nodes.
    static List<Node> reconstructPathNodes(Node current) {
        List<Node> path = new LinkedList<>();
        while (current != null) {
            path.add(0, current); // Add to beginning of list to reverse the path.
            current = current.parent;
        }
        return path;
    }

    static double getTileCost(Map<String, Tile> tiles, Point position) {
        Tile tile = getTileAtPosition(position, tiles);
        return tile.cost;
    }

    // Logic to calculate the total score for a given path
    static int calculateTotalScore(List<Node> path, Map<Point, Integer> silverPoints, Map<String, Tile> tiles,
            Map<Point, Integer> visitCountMap) {
        int score = 0;
        for (Node node : path) {
            Point position = node.position;
            // If the tile is revisited, double the tile cost
            int visitCount = visitCountMap.getOrDefault(position, 0);
            int tileCost = (int) getTileCost(tiles, position); // Cast the result to int
            score += silverPoints.getOrDefault(position, 0); // add silver point score
            if (visitCount > 0) {
                score -= 2 * tileCost; // subtract double the cost
            } else {
                score -= tileCost; // subtract the cost
            }
            visitCountMap.put(position, visitCount + 1); // update visit count
        }
        return score;
    }

    // Use this method to get the cost of the tile at a given position
    static int getTileCost(Point position, Map<String, Tile> tiles, Map<Point, String> pointToTileIdMap) {
        // Assuming that a pointToTileIdMap exists which maps a position to a Tile ID.
        String tileId = pointToTileIdMap.get(position);
        if (tileId != null && tiles.containsKey(tileId)) {
            return tiles.get(tileId).cost;
        }
        // Default cost if no specific tile is found for the position.
        return 0;
    }

    public static void main(String[] args) {
        // Initialize variables and read inputs as before
        List<Point> goldenPoints = new ArrayList<>();
        Map<Point, Integer> silverPoints = new HashMap<>();
        Map<String, Tile> tiles = new HashMap<>();
        Map<Point, String> pointToTileIdMap = new HashMap<>();

        String inputFilename = "input.txt";
        String outputFilename = "output.txt";
        int w = 0;
        int h = 0;

        try {
            int[] dimensions = readInput(inputFilename, goldenPoints, silverPoints, tiles, pointToTileIdMap);
            w = dimensions[0];
            h = dimensions[1];

            // Ensure graph is correctly initialized with tile movements
            Map<Point, List<Point>> graph = buildGraph(w, h, tiles, goldenPoints, pointToTileIdMap);

            List<Node> bestPath = new ArrayList<>();
            double bestScore = Double.MAX_VALUE;

            // Iterate over all golden points as start and end points
            for (Point start : goldenPoints) {
                for (Point end : goldenPoints) {
                    if (start.equals(end))
                        continue; // Skip if start and end are the same

                    List<Node> path = aStar(start, end, graph, tiles);
                    if (!path.isEmpty()) {
                        double score = calculateTotalScore(path, silverPoints, tiles, new HashMap<>());
                        if (score < bestScore) {
                            bestScore = score;
                            bestPath = path;
                        }
                    }
                }
            }

            if (!bestPath.isEmpty()) {
                List<Point> pathPoints = bestPath.stream().map(node -> node.position).collect(Collectors.toList());
                writeOutput(outputFilename, pathPoints, tiles);
                System.out.println("Path found between the golden points.");
            } else {
                System.out.println("No path found between the golden points.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.printf("Width: %d\nHeight:%d\n", w, h);
            System.out.println("Number of Golden Points: " + goldenPoints.size());
            System.out.println("Number of Silver Points: " + silverPoints.size());
        }
    }

    static void writeOutput(String filename, List<Point> path, Map<String, Tile> tiles) throws IOException {
        // The output format is: TileID X Y (as per the problem statement)
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        for (Point p : path) {
            Tile tile = getTileAtPosition(p, tiles);
            writer.write(tile.id + " " + p.x + " " + p.y);
            writer.newLine();
        }
        writer.close();
    }

    static Tile getTileAtPosition(Point position, Map<String, Tile> tiles) {
        for (Tile tile : tiles.values()) {
            for (Point movement : tile.movements) {
                if (position.x == movement.x && position.y == movement.y) {
                    return tile;
                }
            }
        }
        return new Tile("N", 0); // Return a new tile with id "N" (not found) if no matching tile is found.
    }

    static int[] readInput(String filename, List<Point> goldenPoints, Map<Point, Integer> silverPoints,
            Map<String, Tile> tiles, Map<Point, String> pointToTileIdMap) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line = br.readLine();
        String[] parts = line.split(" ");
        int w = Integer.parseInt(parts[0]);
        int h = Integer.parseInt(parts[1]);
        int gCount = Integer.parseInt(parts[2]);
        int sCount = Integer.parseInt(parts[3]);
        int tCount = Integer.parseInt(parts[4]);

        // Parse Golden Points
        for (int i = 0; i < gCount; i++) {
            line = br.readLine();
            parts = line.split(" ");
            goldenPoints.add(new Point(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
        }
        // Parse Silver Points
        for (int i = 0; i < sCount; i++) {
            line = br.readLine();
            parts = line.split(" ");
            silverPoints.put(new Point(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])),
                    Integer.parseInt(parts[2]));
        }

        // Parse Tiles and their movements
        for (int i = 0; i < tCount; i++) {
            line = br.readLine();
            parts = line.split(" ");
            String tileId = parts[0];
            int cost = Integer.parseInt(parts[1]);

            Tile tile = new Tile(tileId, cost);

            // Based on Tile ID, set the allowed movements
            switch (tileId) {
                case "3":
                    tile.addMovement(new Point(1, 0)); // From left to right
                    break;
                case "5":
                    tile.addMovement(new Point(1, 1)); // From down to right
                    break;
                case "6":
                    tile.addMovement(new Point(0, 1)); // From left to down
                    break;
                case "7":
                    tile.addMovement(new Point(1, 0)); // From left to right
                    tile.addMovement(new Point(0, 1)); // From left to down
                    tile.addMovement(new Point(1, 1)); // From down to right
                    break;
                case "9":
                    tile.addMovement(new Point(1, -1)); // From up to right
                    break;
                case "96":
                    tile.addMovement(new Point(0, 1)); // From left to down
                    tile.addMovement(new Point(1, -1)); // From up to right
                    break;
                case "A":
                    tile.addMovement(new Point(0, -1)); // From left to up
                    break;
                case "A5":
                    tile.addMovement(new Point(0, -1)); // From left to up
                    tile.addMovement(new Point(1, 1)); // From down to right
                    break;
                case "B":
                    tile.addMovement(new Point(1, 0)); // From left to right
                    tile.addMovement(new Point(0, -1)); // From left to up
                    tile.addMovement(new Point(1, -1)); // From up to right
                    break;
                case "C":
                    tile.addMovement(new Point(0, 1)); // From up to down
                    break;
                case "C3":
                    tile.addMovement(new Point(1, 0)); // From left to right
                    tile.addMovement(new Point(0, 1)); // From up to down
                    break;
                case "D":
                    tile.addMovement(new Point(0, 1)); // From up to down
                    tile.addMovement(new Point(1, -1)); // From up to right
                    tile.addMovement(new Point(1, 1)); // From down to right
                    break;
                case "E":
                    tile.addMovement(new Point(0, -1)); // From left to up
                    tile.addMovement(new Point(0, 1)); // From left to down
                    tile.addMovement(new Point(0, 1)); // From up to down
                    break;
                case "F":
                    tile.addMovement(new Point(1, 0)); // From left to right
                    tile.addMovement(new Point(0, 1)); // From left to down
                    tile.addMovement(new Point(0, -1)); // From left to up
                    tile.addMovement(new Point(0, 1)); // From up to down
                    tile.addMovement(new Point(1, 1)); // From down to right
                    tile.addMovement(new Point(1, -1)); // From up to right
                    break;
                default:
                    break;
            }
            List<Point> oppositeMovements = new ArrayList<>();
            for (Point movement : tile.movements) {
                oppositeMovements.add(new Point(-movement.x, -movement.y));
            }
            tile.movements.addAll(oppositeMovements);

            tiles.put(tileId, tile);
        }

        // Initialize the pointToTileIdMap for non-golden points
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                Point p = new Point(x, y);
                if (!goldenPoints.contains(p)) {
                    // Assign a default Tile ID for non-golden points.
                    pointToTileIdMap.put(p, tiles.values().iterator().next().id);
                }
            }
        }

        br.close();
        return new int[] { w, h };
    }

    static boolean isValidMovement(int x, int y, int w, int h, List<Point> goldenPoints) {
        if (x >= 0 && x < w && y >= 0 && y < h) {
            Point newPoint = new Point(x, y);
            // Check if it's not a Golden Point
            return !goldenPoints.contains(newPoint);
        }
        return false; // Out of bounds
    }

    static Map<Point, List<Point>> buildGraph(int w, int h, Map<String, Tile> tiles, List<Point> goldenPoints,
            Map<Point, String> pointToTileIdMap) {
        Map<Point, List<Point>> graph = new HashMap<>();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                Point point = new Point(x, y);
                List<Point> possibleMovements = new ArrayList<>();
                String tileId = pointToTileIdMap.get(point);
                Tile tile = tiles.get(tileId);

                if (tile != null) {
                    for (Point movement : tile.movements) {
                        int newX = x + movement.x;
                        int newY = y + movement.y;
                        if (isValidMovement(newX, newY, w, h, goldenPoints)) {
                            possibleMovements.add(new Point(newX, newY));
                        }
                    }
                }

                // If the point is not a golden point, add the movements to the graph
                if (!goldenPoints.contains(point)) {
                    graph.put(point, possibleMovements);
                }
            }
        }

        // Return the constructed graph
        return graph;
    }
}