package amazed.solver;

import amazed.maze.Maze;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * <code>ForkJoinSolver</code> implements a solver for
 * <code>Maze</code> objects using a fork/join multi-thread
 * depth-first search.
 * <p>
 * Instances of <code>ForkJoinSolver</code> should be run by a
 * <code>ForkJoinPool</code> object.
 */

public class ForkJoinSolver
    extends SequentialSolver
{

    protected ConcurrentSkipListSet<Integer> visited;
    protected ArrayList<ForkJoinSolver> forks;
    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal.
     *
     * @param maze   the maze to be searched
     */
    public ForkJoinSolver(Maze maze)
    {
        super(maze);
    }

    public ForkJoinSolver(Maze maze, int start, ConcurrentSkipListSet<Integer> visit)
    {
        this(maze);
        this.start = start;
        visited = visit;
    }

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal, forking after a given number of visited
     * nodes.
     *
     * @param maze        the maze to be searched
     * @param forkAfter   the number of steps (visited nodes) after
     *                    which a parallel task is forked; if
     *                    <code>forkAfter &lt;= 0</code> the solver never
     *                    forks new tasks
     */
    public ForkJoinSolver(Maze maze, int forkAfter)
    {
        this(maze);
    }
    @Override
    protected void initStructures(){
        visited = new ConcurrentSkipListSet<Integer>();;
        predecessor = new HashMap<Integer, Integer>();
        frontier = new Stack<>(); //parallelisera?
        forks = new ArrayList<ForkJoinSolver>();
    }  

    // How our forkjoinsolver should work:
    // Each fork should know what nodes it has visited
    // & each fork should update the ConcurrentSkipListSet when it visits a node, it is new: fork.

    /*
     * How the logic around how we choose to fork should work:
     * 
     * 1. If two or more neighbouring nodes have not been visited, then fork.
     * 
     * 2. One new fork should be created per non-visited neighbour if neighbour count is >= 2 (no need to
     * fork if there only is one way to go).
     * 
     * 3. If all neighbours have been visited, then join.
     * 
     * 4. 
     */

    /**
     * Searches for and returns the path, as a list of node
     * identifiers, that goes from the start node to a goal node in
     * the maze. If such a path cannot be found (because there are no
     * goals, or all goals are unreacheable), the method returns
     * <code>null</code>.
     *
     * @return   the list of node identifiers from the start node to a
     *           goal node in the maze; <code>null</code> if such a path cannot
     *           be found.
     */
    @Override
    public List<Integer> compute()
    {
        List<Integer> paralSearch = parallelSearch();
        return paralSearch;
    }

    private List<Integer> parallelSearch()
    {
        int player = maze.newPlayer(start);
        frontier.push(start);
        // Loop as long as all nodes have yet to be processed
        while (!frontier.empty() && !GoalStatus.getGoalIsFound()){

            int current = frontier.pop();
            
            if (maze.hasGoal(current)){
                maze.move(player, current); //the player moves to the goal
                visited.add(current);
                GoalStatus.setGoalIsFound();
                return pathFromTo(start, current);
            }
            
            // Add the current node to the visited set
            if(visited.add(current)){
                
                // Move player to the current node
                maze.move(player, current);
              
                int neighborAmount = maze.neighbors(current).size();
                // int unvisitedNeighbors = 0;
                
                if(neighborAmount >= 1){
                    ArrayList<Integer> nonVisitedNodes = new ArrayList<Integer>();
                    // Loop through the current node's neighbouring nodes and add them to a temporary list
                    for(int nb : maze.neighbors(current)){
                        if(!visited.contains(nb)){
                            nonVisitedNodes.add(nb);
                            predecessor.put(nb, current);
                            // unvisitedNeighbors++;
                        }
                    }
                    //if there are 2 or more neighbouring unvisited nodes, fork.
                    if(nonVisitedNodes.size() >= 2){
                        for (Integer node : nonVisitedNodes) {

                            //double-check that node is still available
                            if(!visited.contains(node)){
                                ForkJoinSolver forkJS = new ForkJoinSolver(maze, node, visited);
                                forks.add(forkJS);
                                forkJS.fork();
                            }
                        }
                        List<Integer> workingPath = null;
                        for(ForkJoinSolver processes : forks){
                            List<Integer> path = processes.join();
                            if(path != null){
                                workingPath = path;
                            }
                        }
                        if(workingPath != null) {
                            List<Integer> tempList = pathFromTo(start, current);
                            tempList.addAll(workingPath);
                            return tempList;
                        }
                    } else { //when we have one unvisited neighbour
                        if(nonVisitedNodes.size() != 0){
                            frontier.push(nonVisitedNodes.get(0));
                        }
                    }
                }
            }
        }

        // Only return null if every possible node has been visited without finding aa goal
        return null;
    }
    
    /**
    * InnerForkJoinSolver
    */
    public static class GoalStatus {
        static boolean goalIsFound = false;
        
        public static boolean getGoalIsFound(){
            return goalIsFound;
        }

        public static void setGoalIsFound(){
            goalIsFound = true;
        }
    }

    public static class DataHandler{
        static protected ConcurrentSkipListSet<Integer> visited = new ConcurrentSkipListSet<>();

        public static ConcurrentSkipListSet<Integer> getVisited(){
            return visited;
        }
        
        public static void addToSet(Integer visitedNode){
            visited.add(visitedNode);
        }
    }
}


