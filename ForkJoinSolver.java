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

    protected ConcurrentSkipListSet<Integer> allVisited = new ConcurrentSkipListSet<Integer>();
    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal.
     *
     * @param maze   the maze to be searched
     */
    public ForkJoinSolver(Maze maze)
    {
        super(maze);
        initStructures();
    }

    @Override
    protected void initStructures(){
        predecessor = new HashMap<Integer, Integer>();
        frontier = new Stack<>(); //parallelisera?
    } 

    public ForkJoinSolver(Maze maze, int start)
    {
        this(maze);
        this.start = start;
        initStructures();
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
    // public ForkJoinSolver(Maze maze, int forkAfter)
    // {
    //     this(maze);
    //     this.forkAfter = forkAfter;
    // }

    // How our forkjoinsolver should work:
    // Each fork should know what nodes it has visited
    // & each fork should update the ConcurrentSkipListSet when it visits a node, it is new: fork.

    /*
     * How the logic around how we choose to fork should work:
     * 
     * 1. If two or more neighbouring nodes have not been visited, then fork.
     * 
     * 2. One new fork should be created per non-visited neighbour.
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
        return parallelSearch();
    }

    private List<Integer> parallelSearch()
    {
        int player = maze.newPlayer(start);
        //int visitedNodes = 0;

        frontier.push(start);
        // Loop as long as all nodes have yet to be processed
        while (!frontier.empty()){
            int current = frontier.pop();
            
            if (maze.hasGoal(current)){
                
                maze.move(player, current); //the player moves to the goal
            }
            
            // Add the current node to the visited set
            if(allVisited.add(current)){
                System.out.println("waddup");
                // Move player to the current node
                maze.move(player, current);
                int neighborAmount = maze.neighbors(current).size();
                
                // Check if the current node has 2 or more neighbours, or 1 or more and start == 0
                if(neighborAmount >= 2 || (neighborAmount >= 1 && start == 0)){
                    ArrayList<Integer> nonVisitedNodes = new ArrayList<Integer>();
                    // Loop through the current node's neighbouring nodes and add them to a temporary list
                    for(int nb : maze.neighbors(current)){
                        if(allVisited.add(nb)){
                            nonVisitedNodes.add(nb);
                        }
                    }
                    //if there are 2 or more neighbouring unvisited nodes, fork.
                    if(nonVisitedNodes.size() >= 2){
                        for (Integer node : nonVisitedNodes) {
                            ForkJoinSolver forkJS = new ForkJoinSolver(maze, node);
                            forkJS.fork();
                            //should join forks later
                        }
                    } else { //when we have one unvisited neighbour
                        frontier.push(nonVisitedNodes.get(0));
                    }
                } else {
                    //Dead End
                }

            }
            
        }
        //init new node, returns path
        //init other node, returns path

        //join nodes, join paths

        return null;
    }
}
