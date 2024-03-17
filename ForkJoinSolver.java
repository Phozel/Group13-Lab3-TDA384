package amazed.solver;

import amazed.maze.Maze;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
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

    /**
     * An Atomic boolean to keep track of if the object in question is the initial one
     * that is created on startup or not.
     */
    AtomicBoolean first = new AtomicBoolean(false);
    
    /**
     * A set to keep track of all visited nodes
     */
    protected ConcurrentSkipListSet<Integer> visited;
    
    /**
     * A list to keep track of the created forks
     */
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
        first.set(true); 
    }
    @Override
    protected void initStructures(){
        visited = new ConcurrentSkipListSet<Integer>();
        predecessor = new HashMap<Integer, Integer>();
        frontier = new Stack<>();
        forks = new ArrayList<ForkJoinSolver>();
    }  
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
        List<Integer> parallelSearch = parallelSearch();
        return parallelSearch;
    }

    private List<Integer> parallelSearch()
    {   
        /**
         * Each time a new instance of ForkJoinSolver is run, create a new player to 
         * represent it and push it's start node to the frontier.
         */
        int player = maze.newPlayer(start);
        frontier.push(start);
        
        /**
         * Loop as long as all nodes in the frontier have yet to be processed
         * and the goal hasn't been found yet.
         */
        while (!frontier.empty() && !GoalStatus.getGoalIsFound()){

            int current = frontier.pop();
            
            /**
             * If the current node has the goal, move the player to it.
             * 
             * Then add the goal-node to the visited set and change the goalstatus
             * so that other forks get notified that it has been found.
             * 
             * Then return the path from the start to the goal-node.
             */
            if (maze.hasGoal(current)){
                maze.move(player, current);
                visited.add(current);
                GoalStatus.setGoalIsFound();
                return pathFromTo(start, current);
            }
            
            /**
             * Add the "spawn point"/the initial node visited to visited.
             * 
             * This only happens once.
             * This is because in all other cases the current node has already been added to the visited list.
             */
            if(first.get() == true){
                visited.add(current);
                first.set(false);
            }
            
            /**
             * Move the player to the current node, and get the number of neighbors to said node.
             */
            maze.move(player, current);
            int neighborAmount = maze.neighbors(current).size();

            /**
             * Check whether or not the node has 1 or more neighbors.
             * 
             * If this happens to be false, then the player has nowhere to go.
             */
            if(neighborAmount >= 1){

                /**
                 * Create a list to store unvisited nodes in.
                 */
                ArrayList<Integer> nonVisitedNodes = new ArrayList<Integer>();
                
                /**
                 * Loop through the neighbors and if they haven't been visited yet, add them to
                 * the visited set. 
                 * 
                 * After this, add the nodes to the unvisited nodes list and also add them
                 * to the predecessor hashmap to store the way one got to said node.
                 */
                for(int nb : maze.neighbors(current)){
                    if(visited.add(nb)){
                        nonVisitedNodes.add(nb);
                        predecessor.put(nb, current);
                    }
                }
                /**
                 * If there are 2 or more neighboring unvisited nodes we want to fork.
                 */
                if(nonVisitedNodes.size() >= 2){

                    /**
                     * Loop through the unvisited nodes list.
                     */
                    for (Integer node : nonVisitedNodes) {
                        
                        /**
                         * Create a new fork that explores in the direction of the given neighbor node.
                         */
                        ForkJoinSolver forkJS = new ForkJoinSolver(maze, node, visited);
                        forks.add(forkJS);
                        forkJS.fork();
                        
                    }

                    /**
                     * Loop through the forks and join each of them, unless one returns something that isn't null.
                     * 
                     * If one returns a path (which it does if the path leads to a goal),
                     * we immediately return the path and don't care about the other forks.
                     */
                    for(ForkJoinSolver processes : forks){
                        List<Integer> path = processes.join();
                        if(path != null){
                            List<Integer> tempList = pathFromTo(start, current);
                            tempList.addAll(path);
                            return tempList;
                        }
                    }

                } 
                /**
                 * When we only have one unvisited neighbor we don't want to create a new fork.
                 * Instead we just add the neighbor to the frontier if the neighboring
                 * node is unvisited.
                 */
                else { 
                    if(nonVisitedNodes.size() != 0){
                        frontier.push(nonVisitedNodes.get(0));
                    }
                }
            }
        }

        /**
         * Only return null if every possible node has been visited without finding a goal.
         */
        return null;
    }
    
    /**
    * GoalStatus
    * 
    * Keeps track of whether or not the goal has been found or not.
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
}