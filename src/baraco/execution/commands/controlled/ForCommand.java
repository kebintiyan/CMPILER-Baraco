package baraco.execution.commands.controlled;

import baraco.antlr.parser.BaracoParser;
import baraco.builder.BuildChecker;
import baraco.builder.ErrorRepository;
import baraco.execution.ExecutionManager;
import baraco.execution.ExecutionMonitor;
import baraco.execution.commands.ICommand;
import baraco.execution.commands.evaluation.AssignmentCommand;
import baraco.execution.commands.evaluation.MappingCommand;
import baraco.execution.commands.simple.IncDecCommand;
import baraco.execution.commands.utils.ConditionEvaluator;
import baraco.representations.BaracoValue;
import baraco.semantics.analyzers.LocalVariableAnalyzer;
import baraco.semantics.mapping.IValueMapper;
import baraco.semantics.mapping.IdentifierMapper;
import baraco.semantics.searching.VariableSearcher;
import baraco.semantics.utils.LocalVarTracker;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

public class ForCommand implements IControlledCommand {

    private final static String TAG = "MobiProg_ForCommand";

    private List<ICommand> commandSequences;

    private BaracoParser.LocalVariableDeclarationContext localVarDecCtx; //a local variable ctx that is evaluated at the start of the for loop
    private BaracoParser.ExpressionContext conditionalExpr; //the condition to satisfy
    private ICommand updateCommand; //the update command aftery ever iteration

    private String modifiedConditionExpr;

    private ArrayList<String> localVars = new ArrayList<>();

    public ForCommand(BaracoParser.LocalVariableDeclarationContext localVarDecCtx, BaracoParser.ExpressionContext conditionalExpr, ICommand updateCommand) {
        this.localVarDecCtx = localVarDecCtx;
        this.conditionalExpr = conditionalExpr;
        this.updateCommand = updateCommand;

        this.commandSequences = new ArrayList<ICommand>();
    }

    /* (non-Javadoc)
     * @see com.neildg.mobiprog.execution.commands.ICommand#execute()
     */
    @Override
    public void execute() {
        //this.evaluateLocalVariable();
        this.identifyVariables();

        ExecutionMonitor executionMonitor = ExecutionManager.getInstance().getExecutionMonitor();

        LocalVarTracker.resetLocalVars(localVars);

        try {
            //evaluate the given condition
            while(ConditionEvaluator.evaluateCondition(this.conditionalExpr)) {
                for(ICommand command : this.commandSequences) {
                    executionMonitor.tryExecution();
                    command.execute();

                    LocalVarTracker.getInstance().populateLocalVars(command);

                    if (ExecutionManager.getInstance().isAborted())
                        break;
                }

                if (ExecutionManager.getInstance().isAborted())
                    break;

                executionMonitor.tryExecution();
                this.updateCommand.execute(); //execute the update command
                this.identifyVariables(); //identify variables again to detect changes to such variables used.
            }

        } catch(InterruptedException e) {
            System.out.println(TAG + ": " + "Monitor block interrupted! " +e.getMessage());
        }
    }

    private void evaluateLocalVariable() {
        if(this.localVarDecCtx != null) {
            LocalVariableAnalyzer localVarAnalyzer = new LocalVariableAnalyzer();
            localVarAnalyzer.markImmediateExecution();
            localVarAnalyzer.analyze(this.localVarDecCtx);
        }
    }

    private void identifyVariables() {
        IValueMapper identifierMapper = new IdentifierMapper(this.conditionalExpr.getText());
        identifierMapper.analyze(this.conditionalExpr);

        this.modifiedConditionExpr = identifierMapper.getModifiedExp();
    }

    /* (non-Javadoc)
     * @see com.neildg.mobiprog.execution.commands.controlled.IControlledCommand#getControlType()
     */
    @Override
    public ControlTypeEnum getControlType() {
        return ControlTypeEnum.FOR_CONTROL;
    }

    /* (non-Javadoc)
     * @see com.neildg.mobiprog.execution.commands.controlled.IControlledCommand#addCommand(com.neildg.mobiprog.execution.commands.ICommand)
     */
    @Override
    public void addCommand(ICommand command) {

        System.out.println("		Added command to FOR");
        this.commandSequences.add(command);
    }

    public int getCommandCount() {
        return this.commandSequences.size();
    }

    public ArrayList<String> getLocalVars() {
        return localVars;
    }
}